package com.nurba.java.profiling;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * TEMPORARY DataSource wrapper that times {@link #getConnection()}.
 * <p>
 * This is the key instrument for the ~4s investigation: it isolates the time a Tomcat worker
 * spends waiting for a free HikariCP connection (pool starvation) from the time spent running SQL.
 * Hibernate obtains its JDBC connection through this wrapper → Hikari, so the measurement is exact.
 */
public class TimingDataSource extends DelegatingDataSource {

    public TimingDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        long start = System.nanoTime();
        Connection c = super.getConnection();
        record(System.nanoTime() - start);
        return JdbcTiming.wrap(c);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        long start = System.nanoTime();
        Connection c = super.getConnection(username, password);
        record(System.nanoTime() - start);
        return JdbcTiming.wrap(c);
    }

    private void record(long nanos) {
        try {
            RequestTimings t = RequestTimings.current();
            if (t != null) {
                t.addGetConnection(nanos);
            }
        } catch (Throwable ignored) {
            // Bookkeeping must never affect connection acquisition (or leak the acquired connection).
        }
    }
}
