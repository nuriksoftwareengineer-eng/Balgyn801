package com.nurba.java.profiling;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the production profiler filter: threshold gating, per-request ThreadLocal cleanup,
 * resilience to exceptions (both from the request and from the profiler itself), concurrency
 * isolation, and async requests. No Spring context or database required.
 */
class OuterProfilingFilterTest {

    private ProfilingMetrics metrics;
    private Logger profileLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        metrics = mock(ProfilingMetrics.class);
        when(metrics.hikari()).thenReturn("hikari-ok");
        when(metrics.jvm()).thenReturn("jvm-ok");
        when(metrics.postgres()).thenReturn("pg-ok");

        profileLogger = (Logger) LoggerFactory.getLogger("PROFILE");
        profileLogger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        profileLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        profileLogger.detachAppender(appender);
        RequestTimings.clear(); // defensive — no test should leave a dangling ThreadLocal
    }

    private long slowLines() {
        return appender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("[SLOW"))
                .count();
    }

    @Test
    void requestBelowThreshold_doesNotLog_andClearsThreadLocal() throws Exception {
        OuterProfilingFilter filter = new OuterProfilingFilter(100_000, metrics); // 100s threshold
        FilterChain chain = (req, res) -> { /* fast */ };

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/catalog/groups"),
                new MockHttpServletResponse(), chain);

        assertThat(slowLines()).isZero();
        assertThat(RequestTimings.current()).isNull();
    }

    @Test
    void requestAboveThreshold_logsBreakdown_andClearsThreadLocal() throws Exception {
        OuterProfilingFilter filter = new OuterProfilingFilter(0, metrics); // everything is "slow"
        FilterChain chain = (req, res) -> {
            RequestTimings t = RequestTimings.current();   // simulate downstream work
            t.addGetConnection(5_000_000);
            t.addSql(3_000_000);
            ((MockHttpServletResponse) res).setStatus(200);
        };

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/catalog/groups"),
                new MockHttpServletResponse(), chain);

        assertThat(slowLines()).isEqualTo(1);
        String msg = appender.list.get(appender.list.size() - 1).getFormattedMessage();
        assertThat(msg).contains("GET", "/api/v1/catalog/groups", "-> 200");
        assertThat(msg).contains("getConn=5", "sql=3", "hikari-ok", "jvm-ok", "pg-ok");
        assertThat(RequestTimings.current()).isNull();
    }

    @Test
    void fastRequest_doesNotTouchResponse() throws Exception {
        // Regression: the profiler must not change response-commit behaviour for sub-threshold
        // requests (no early flush for the overwhelming majority of traffic).
        OuterProfilingFilter filter = new OuterProfilingFilter(100_000, metrics);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/x"), response, (req, res) -> {});

        assertThat(response.isCommitted()).isFalse();
        assertThat(slowLines()).isZero();
        assertThat(RequestTimings.current()).isNull();
    }

    @Test
    void slowRequest_flushesResponseBeforeMetrics() throws Exception {
        // Slow path flushes the response first so metric collection can't add client-visible latency.
        OuterProfilingFilter filter = new OuterProfilingFilter(0, metrics);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/x"), response, (req, res) -> {});

        assertThat(response.isCommitted()).isTrue();
        assertThat(slowLines()).isEqualTo(1);
        assertThat(RequestTimings.current()).isNull();
    }

    @Test
    void exceptionDuringRequest_propagates_andClearsThreadLocal() {
        OuterProfilingFilter filter = new OuterProfilingFilter(0, metrics);
        FilterChain chain = (req, res) -> { throw new ServletException("boom from business logic"); };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest("GET", "/x"),
                new MockHttpServletResponse(), chain))
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("boom from business logic");

        assertThat(RequestTimings.current()).isNull(); // no leak even on the exception path
    }

    @Test
    void exceptionInsideProfiler_isSwallowed_requestUnaffected_andClearsThreadLocal() throws Exception {
        when(metrics.hikari()).thenThrow(new RuntimeException("profiler metric blew up"));
        OuterProfilingFilter filter = new OuterProfilingFilter(0, metrics);
        boolean[] chainRan = {false};
        FilterChain chain = (req, res) -> chainRan[0] = true;

        // Must NOT throw: a profiler failure can never break the request.
        filter.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), chain);

        assertThat(chainRan[0]).isTrue();
        assertThat(slowLines()).isZero();            // report aborted before emitting
        assertThat(RequestTimings.current()).isNull(); // still cleaned up
    }

    @Test
    void concurrentRequests_haveIsolatedThreadLocals() throws Exception {
        int threads = 16;
        OuterProfilingFilter filter = new OuterProfilingFilter(100_000, metrics); // never logs; test state only
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        Set<RequestTimings> seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
        boolean[] contaminated = {false};

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                FilterChain chain = (req, res) -> {
                    RequestTimings t = RequestTimings.current();
                    if (t == null) { contaminated[0] = true; return; }
                    seen.add(t);
                    t.addSql(1_000_000);
                    // each thread must still see ITS OWN instance after work
                    if (RequestTimings.current() != t) contaminated[0] = true;
                };
                try {
                    ready.countDown();
                    go.await();
                    filter.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), chain);
                    if (RequestTimings.current() != null) contaminated[0] = true; // must be cleared
                } catch (Exception e) {
                    contaminated[0] = true;
                }
                return null;
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(contaminated[0]).isFalse();
        assertThat(seen).hasSize(threads); // every request had its own distinct RequestTimings
    }

    @Test
    void asyncRequest_completes_andLeavesNoThreadLocal() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsyncController())
                .addFilter(new OuterProfilingFilter(0, metrics), "/*")
                .build();

        MvcResult result = mvc.perform(get("/async-endpoint"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        assertThat(RequestTimings.current()).isNull();
    }

    @RestController
    static class AsyncController {
        @GetMapping("/async-endpoint")
        Callable<String> async() {
            return () -> "ok";
        }
    }
}
