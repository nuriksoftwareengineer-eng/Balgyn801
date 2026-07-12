package com.nurba.java.profiling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

/**
 * TEMPORARY diagnostic instrumentation — a permanent, opt-in production profiler.
 * <p>
 * Entirely gated behind {@code app.profiling.enabled=true} (env {@code APP_PROFILING_ENABLED=true}):
 * when the flag is off NONE of these beans are created, the DataSource is not wrapped, and there is
 * zero runtime overhead. When on, every request is timed and any request slower than
 * {@code app.profiling.slow-request-threshold-ms} (default 100) logs a per-stage breakdown under the
 * "PROFILE" logger at WARN. Enable it in the environment where latency reproduces, then grep "SLOW".
 */
@Configuration
@ConditionalOnProperty(name = "app.profiling.enabled", havingValue = "true")
public class ProfilingConfig implements WebMvcConfigurer {

    @Value("${app.profiling.slow-request-threshold-ms:100}")
    private long slowRequestThresholdMs;

    /** APM context collector (Hikari/JVM/PG). Only created when profiling is enabled. */
    @Bean
    public ProfilingMetrics profilingMetrics(DataSource dataSource) {
        return new ProfilingMetrics(dataSource);
    }

    /** Outermost — runs BEFORE the Spring Security filter chain (which sits at order -100). */
    @Bean
    public FilterRegistrationBean<OuterProfilingFilter> outerProfilingFilter(ProfilingMetrics metrics) {
        FilterRegistrationBean<OuterProfilingFilter> reg = new FilterRegistrationBean<>(
                new OuterProfilingFilter(slowRequestThresholdMs, metrics));
        reg.setOrder(-200); // Spring Security's FilterChainProxy is at -100 → this runs first
        reg.addUrlPatterns("/*");
        return reg;
    }

    /** Runs AFTER the Spring Security chain → marks the security-filters boundary. */
    @Bean
    public FilterRegistrationBean<PostSecurityProfilingFilter> postSecurityProfilingFilter() {
        FilterRegistrationBean<PostSecurityProfilingFilter> reg = new FilterRegistrationBean<>(
                new PostSecurityProfilingFilter());
        reg.setOrder(0); // after Spring Security (-100), still before the DispatcherServlet
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ProfilingInterceptor()).addPathPatterns("/**");
    }

    /**
     * Wraps the autoconfigured Hikari DataSource so {@code getConnection()} and SQL execution are
     * timed. Static so it is registered early enough to post-process the DataSource bean.
     */
    @Bean
    static BeanPostProcessor dataSourceTimingPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds && !(bean instanceof TimingDataSource)) {
                    return new TimingDataSource(ds);
                }
                return bean;
            }
        };
    }
}
