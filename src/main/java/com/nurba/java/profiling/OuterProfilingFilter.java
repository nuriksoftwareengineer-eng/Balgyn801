package com.nurba.java.profiling;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TEMPORARY outermost profiling filter — registered BEFORE the Spring Security chain, applied to
 * EVERY request. Starts the per-request timer on entry and, in a finally block, logs a one-line
 * per-stage breakdown for any request whose total wall time exceeds the configured threshold
 * (default 100 ms). Fast requests add only a few nanoTime reads and no log output.
 */
public class OuterProfilingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("PROFILE");

    private final long thresholdMs;
    private final ProfilingMetrics metrics;

    public OuterProfilingFilter(long thresholdMs, ProfilingMetrics metrics) {
        this.thresholdMs = thresholdMs;
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RequestTimings.begin();
        boolean thrown = false;
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            thrown = true;
            throw t;
        } finally {
            // The profiler must never affect the request and must never leak the ThreadLocal on a
            // pooled worker: finish() is fully isolated, and clear() runs unconditionally.
            try {
                finish(request, response, thrown);
            } catch (Throwable t) {
                log.debug("profiler failed (ignored)", t);
            } finally {
                RequestTimings.clear();
            }
        }
    }

    private void finish(HttpServletRequest request, HttpServletResponse response, boolean thrown) {
        RequestTimings t = RequestTimings.current();
        if (t == null) return;

        long end = System.nanoTime();
        long totalMs = (end - t.requestReceivedNanos) / 1_000_000;
        // Fast path: below threshold there is NO flush, NO metric collection, NO log, and therefore
        // no change to response-commit behaviour for the overwhelming majority of requests.
        if (totalMs < thresholdMs) return;

        // Slow request only: flush the response BEFORE the (potentially blocking) metric collection so
        // the profiler can never add to client-visible latency. Skipped on the error path (the
        // container still owns error rendering) and when async is in progress / already committed.
        if (!thrown && !request.isAsyncStarted() && !response.isCommitted()) {
            try {
                response.flushBuffer();
            } catch (Throwable ignored) { /* best-effort */ }
        }

        long security  = ms(t.requestReceivedNanos, t.securityDoneNanos);
        long dispatch  = ms(t.securityDoneNanos, t.handlerStartNanos);
        long getConn   = t.getConnectionNanos / 1_000_000;
        long sql       = t.sqlNanos / 1_000_000;
        long rsRead    = t.resultSetNanos / 1_000_000;
        long handler   = ms(t.handlerStartNanos, t.handlerEndNanos);
        long mapping   = (handler < 0) ? -1 : Math.max(0, handler - getConn - sql - rsRead);
        long serialize = ms(t.handlerEndNanos, t.completedNanos);

        log.warn("""
                [SLOW {}ms] {} {} -> {}
                  stages : security={} dispatch={} getConn={}({}) sql={}({}) rsRead={}({} rows) mapping={} serialize={} total={}
                  hikari : {}
                  jvm    : {}
                  pg     : {}""",
                totalMs, request.getMethod(), safeUri(request), response.getStatus(),
                fmt(security), fmt(dispatch),
                getConn, t.getConnectionCount,
                sql, t.sqlCount,
                rsRead, t.resultSetRows,
                fmt(mapping), fmt(serialize), totalMs,
                metrics.hikari(),
                metrics.jvm(),
                metrics.postgres());
    }

    /** Nanos delta in ms, or -1 if a checkpoint was never reached (e.g. request short-circuited). */
    private static long ms(long fromNanos, long toNanos) {
        if (fromNanos == 0 || toNanos == 0) return -1;
        return (toNanos - fromNanos) / 1_000_000;
    }

    private static String fmt(long ms) {
        return ms < 0 ? "n/a" : Long.toString(ms);
    }

    /**
     * The request PATH only — never the query string (which may carry tokens/PII), and never any
     * header, cookie or body. getRequestURI() is not URL-decoded so it cannot contain raw newlines,
     * but CR/LF are stripped and the value capped to defend against log forging / flooding.
     */
    private static String safeUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return "";
        uri = uri.replace('\n', '_').replace('\r', '_');
        return uri.length() > 512 ? uri.substring(0, 512) + "…" : uri;
    }
}
