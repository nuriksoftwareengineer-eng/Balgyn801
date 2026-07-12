package com.nurba.java.profiling;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * TEMPORARY JDBC timing proxies used only when {@code app.profiling.enabled=true}.
 * <p>
 * Wraps a {@link Connection} so that every {@code Statement.execute*()} call is timed and added to
 * the current {@link RequestTimings}. This isolates raw SQL execution from entity hydration + DTO
 * mapping (which becomes "handler − getConnection − sql").
 * <p>
 * Only statement <em>execution</em> is timed, not per-row {@code ResultSet.next()}: the PostgreSQL
 * driver with the default fetch size (no cursor) materialises the whole result during
 * {@code executeQuery()}, so execute time already includes the fetch. Skipping row-level proxying
 * keeps overhead to one nanoTime pair per statement. HikariCP already hands out a proxy Connection,
 * so layering this java.sql.Connection proxy on top is safe — callers reach the driver via
 * {@code unwrap()}, which is delegated normally.
 */
final class JdbcTiming {

    private JdbcTiming() {}

    static Connection wrap(Connection target) {
        try {
            return (Connection) Proxy.newProxyInstance(
                    JdbcTiming.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    new ConnectionHandler(target));
        } catch (Throwable t) {
            return target; // never break connection acquisition — degrade to the raw connection
        }
    }

    private static Object invokeRaw(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // Surface the real SQLException, not the reflection wrapper. getCause() is non-null for a
            // target exception, but guard against a null cause so we never "throw null".
            Throwable cause = e.getCause();
            throw (cause != null) ? cause : e;
        }
    }

    private record ConnectionHandler(Connection target) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = invokeRaw(target, method, args); // business result/exception preserved
            return switch (method.getName()) {
                case "createStatement"  -> proxyStatement(result, Statement.class);
                case "prepareStatement" -> proxyStatement(result, PreparedStatement.class);
                case "prepareCall"      -> proxyStatement(result, CallableStatement.class);
                default -> result;
            };
        }

        /** Never let a proxy failure break a real statement — fall back to the raw statement. */
        private Object proxyStatement(Object stmt, Class<?> iface) {
            if (stmt == null) return null;
            try {
                return Proxy.newProxyInstance(
                        JdbcTiming.class.getClassLoader(),
                        new Class<?>[]{iface},
                        new StatementHandler(stmt));
            } catch (Throwable t) {
                return stmt;
            }
        }
    }

    private record StatementHandler(Object target) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().startsWith("execute")) {
                long start = System.nanoTime();
                Object result = invokeRaw(target, method, args); // may throw business SQLException
                record(System.nanoTime() - start);
                return maybeProxyResultSet(result);
            }
            return maybeProxyResultSet(invokeRaw(target, method, args)); // e.g. getResultSet()
        }

        private void record(long nanos) {
            try {
                RequestTimings t = RequestTimings.current();
                if (t != null) t.addSql(nanos);
            } catch (Throwable ignored) { /* bookkeeping must never affect the query */ }
        }

        private Object maybeProxyResultSet(Object result) {
            if (!(result instanceof ResultSet rs)) return result;
            try {
                return Proxy.newProxyInstance(
                        JdbcTiming.class.getClassLoader(),
                        new Class<?>[]{ResultSet.class},
                        new ResultSetHandler(rs));
            } catch (Throwable t) {
                return rs; // fall back to the raw ResultSet
            }
        }
    }

    /** Times {@code ResultSet.next()} — fetch/iteration cost, separate from statement execution. */
    private record ResultSetHandler(ResultSet target) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("next")) {
                long start = System.nanoTime();
                Object advanced = invokeRaw(target, method, args); // business result/exception preserved
                try {
                    RequestTimings t = RequestTimings.current();
                    if (t != null) t.addResultSet(System.nanoTime() - start, Boolean.TRUE.equals(advanced));
                } catch (Throwable ignored) { /* never affect iteration */ }
                return advanced;
            }
            return invokeRaw(target, method, args);
        }
    }
}
