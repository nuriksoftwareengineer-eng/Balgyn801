package com.nurba.java.profiling;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * TEMPORARY interceptor marking the controller boundary.
 * <p>
 * {@code preHandle} fires right before the controller method; {@code postHandle} right after it
 * returns but BEFORE Jackson serialization; {@code afterCompletion} after the response is written.
 * That lets the outer filter split "handler (SQL + mapping)" from "JSON serialization + commit".
 */
public class ProfilingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestTimings t = RequestTimings.current();
        if (t != null) {
            t.handlerStartNanos = System.nanoTime();
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        RequestTimings t = RequestTimings.current();
        if (t != null) {
            t.handlerEndNanos = System.nanoTime();
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        RequestTimings t = RequestTimings.current();
        if (t != null) {
            t.completedNanos = System.nanoTime();
        }
    }
}
