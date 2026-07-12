package com.nurba.java.profiling;

/**
 * TEMPORARY per-request profiling holder (diagnostic instrumentation).
 * <p>
 * A single MVC request runs entirely on one Tomcat worker thread (security filters →
 * DispatcherServlet → controller → service/JDBC → Jackson serialization), so a ThreadLocal
 * captures the whole timeline safely. Remove this package once the ~4s latency is localized.
 * <p>
 * All timestamps are {@link System#nanoTime()} readings; {@code getConnectionNanos} is an
 * accumulator because a request may acquire the JDBC connection more than once.
 */
public final class RequestTimings {

    private static final ThreadLocal<RequestTimings> TL = new ThreadLocal<>();

    long requestReceivedNanos;   // (1) outermost filter entry
    long securityDoneNanos;      // (2) first checkpoint after the Spring Security chain
    long handlerStartNanos;      // (3) interceptor preHandle (controller about to run)
    long handlerEndNanos;        // (6) interceptor postHandle (controller returned, before serialization)
    long completedNanos;         // (8) interceptor afterCompletion (response written)

    long getConnectionNanos;     // (4) accumulated DataSource.getConnection() wall time
    int  getConnectionCount;

    long sqlNanos;               // (5) accumulated JDBC Statement.execute*() wall time
    int  sqlCount;

    long resultSetNanos;         // (5b) accumulated ResultSet.next() wall time (fetch/iteration)
    int  resultSetRows;

    public static void begin() {
        RequestTimings t = new RequestTimings();
        t.requestReceivedNanos = System.nanoTime();
        TL.set(t);
    }

    public static RequestTimings current() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }

    public void addGetConnection(long nanos) {
        this.getConnectionNanos += nanos;
        this.getConnectionCount++;
    }

    public void addSql(long nanos) {
        this.sqlNanos += nanos;
        this.sqlCount++;
    }

    public void addResultSet(long nanos, boolean advanced) {
        this.resultSetNanos += nanos;
        if (advanced) this.resultSetRows++;
    }
}
