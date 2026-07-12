package com.nurba.java.profiling;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * APM-grade context collected ONLY for requests that exceed the profiling threshold.
 * <p>
 * Hikari pool state and JVM memory come from in-process MXBeans (no I/O, microsecond reads).
 * The last GC pause is captured passively via a GC notification listener (event-driven, no polling).
 * PostgreSQL stats require a query, so they are (a) gated behind {@code app.profiling.pg-stats-enabled},
 * (b) throttled to at most once per {@code app.profiling.pg-stats-throttle-ms}, and (c) fetched over a
 * short-lived direct JDBC connection — NOT the Hikari pool — so the probe can never compete for the
 * very connections whose starvation it is meant to reveal. Everything is best-effort: any failure
 * yields "n/a" and never affects the request.
 * <p>
 * Instantiated only via {@link ProfilingConfig} (flag-gated), so nothing here — no GC listener, no
 * Hikari unwrap — exists when profiling is disabled.
 */
class ProfilingMetrics {

    private static final Logger log = LoggerFactory.getLogger("PROFILE");

    private final DataSource dataSource;
    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

    private volatile HikariDataSource hikari;
    private volatile int hikariMax = -1;
    private volatile long lastGcPauseMs = -1;
    private volatile String lastGcName = "";

    // GC listeners registered by this bean, tracked so @PreDestroy can remove them (no dangling
    // listeners after a context refresh).
    private final List<GcListener> gcListeners = new ArrayList<>();
    private record GcListener(NotificationEmitter emitter, NotificationListener listener) {}

    // Postgres probe state
    private final Properties pgProps = new Properties();
    private final AtomicBoolean pgCollecting = new AtomicBoolean(false);
    private volatile String pgCache = "n/a";
    private volatile long pgCacheAt = 0;

    @Value("${spring.datasource.url:}")          private String jdbcUrl;
    @Value("${spring.datasource.username:}")      private String jdbcUser;
    @Value("${spring.datasource.password:}")      private String jdbcPassword;
    @Value("${app.profiling.pg-stats-enabled:true}")     private boolean pgEnabled;
    @Value("${app.profiling.pg-stats-throttle-ms:1000}") private long pgThrottleMs;

    ProfilingMetrics(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    void init() {
        // Resolve the underlying HikariDataSource (the bean is wrapped by TimingDataSource). The
        // pool MXBean is fetched lazily in hikari() because the pool starts on first getConnection().
        try {
            this.hikari = dataSource.unwrap(HikariDataSource.class);
            if (this.hikari != null) {
                this.hikariMax = this.hikari.getMaximumPoolSize();
            }
        } catch (Exception e) {
            log.debug("HikariDataSource unwrap failed: {}", e.toString());
        }
        // Passive last-GC-pause capture.
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc instanceof NotificationEmitter emitter) {
                NotificationListener listener = (notification, handback) -> {
                    if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
                            .equals(notification.getType())) {
                        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo
                                .from((CompositeData) notification.getUserData());
                        lastGcPauseMs = info.getGcInfo().getDuration();
                        lastGcName = info.getGcName();
                    }
                };
                emitter.addNotificationListener(listener, null, null);
                gcListeners.add(new GcListener(emitter, listener));
            }
        }
        // Tight timeouts so the Postgres probe can never hang a worker thread (bounded to ~1s).
        pgProps.setProperty("user", jdbcUser);
        pgProps.setProperty("password", jdbcPassword);
        pgProps.setProperty("connectTimeout", "1");
        pgProps.setProperty("socketTimeout", "1");
        pgProps.setProperty("loginTimeout", "1");
        pgProps.setProperty("ApplicationName", "balgyn-profiler");
    }

    @PreDestroy
    void shutdown() {
        for (GcListener gl : gcListeners) {
            try {
                gl.emitter().removeNotificationListener(gl.listener());
            } catch (Exception ignored) { /* listener already gone */ }
        }
        gcListeners.clear();
    }

    String hikari() {
        try {
            HikariDataSource h = hikari;
            if (h == null) return "n/a";
            HikariPoolMXBean p = h.getHikariPoolMXBean(); // non-null once the pool has started
            if (p == null) return "starting";
            return "active=" + p.getActiveConnections()
                    + " idle=" + p.getIdleConnections()
                    + " pending=" + p.getThreadsAwaitingConnection()
                    + " total=" + p.getTotalConnections()
                    + " max=" + hikariMax;
        } catch (Throwable t) {
            return "n/a";
        }
    }

    String jvm() {
        try {
            return jvmInternal();
        } catch (Throwable t) {
            return "n/a";
        }
    }

    private String jvmInternal() {
        long usedMb = memory.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxMb = memory.getHeapMemoryUsage().getMax() / (1024 * 1024);
        String gc = lastGcPauseMs < 0 ? "none" : (lastGcPauseMs + "ms/" + lastGcName);
        return "heapUsed=" + usedMb + "MB heapMax=" + maxMb + "MB lastGcPause=" + gc;
    }

    String postgres() {
        if (!pgEnabled) return "disabled";
        long now = System.currentTimeMillis();
        if (now - pgCacheAt < pgThrottleMs) return pgCache;          // serve throttled snapshot
        if (!pgCollecting.compareAndSet(false, true)) return pgCache; // another thread is collecting
        try {
            pgCache = collectPg();
            pgCacheAt = System.currentTimeMillis();
        } catch (Exception e) {
            pgCache = "n/a(" + e.getClass().getSimpleName() + ")";
            pgCacheAt = System.currentTimeMillis();
        } finally {
            pgCollecting.set(false);
        }
        return pgCache;
    }

    private String collectPg() throws Exception {
        String sql = """
                SELECT
                  (SELECT count(*) FROM pg_stat_activity
                     WHERE datname = current_database() AND state = 'active')            AS active,
                  (SELECT count(*) FROM pg_stat_activity
                     WHERE datname = current_database() AND wait_event_type = 'Lock')    AS waiting,
                  (SELECT count(*) FROM pg_locks WHERE NOT granted)                      AS lock_waits
                """;
        try (Connection c = DriverManager.getConnection(jdbcUrl, pgProps);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return "active=" + rs.getInt("active")
                        + " waiting=" + rs.getInt("waiting")
                        + " lockWaits=" + rs.getInt("lock_waits");
            }
            return "n/a";
        }
    }
}
