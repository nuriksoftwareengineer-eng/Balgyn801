package com.nurba.java.profiling;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TEMPORARY filter registered AFTER the Spring Security chain. The gap between the outer filter's
 * entry and this checkpoint is the time spent inside all Spring Security filters.
 */
public class PostSecurityProfilingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RequestTimings t = RequestTimings.current();
        if (t != null) {
            t.securityDoneNanos = System.nanoTime();
        }
        filterChain.doFilter(request, response);
    }
}
