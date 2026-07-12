package com.nurba.java.profiling;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the master switch by code: with {@code app.profiling.enabled=false} (or absent) NONE of the
 * profiler beans exist, the DataSource is left untouched (no wrapping), and therefore no filter,
 * interceptor, BeanPostProcessor, GC listener or JDBC proxy is ever created. Enabled = all present.
 */
class ProfilingConfigConditionalTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("dataSource", DataSource.class, () -> Mockito.mock(DataSource.class))
            .withConfiguration(UserConfigurations.of(ProfilingConfig.class));

    @Test
    void disabled_createsNoProfilerBeans_andDoesNotWrapDataSource() {
        runner.withPropertyValues("app.profiling.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ProfilingMetrics.class);
            assertThat(ctx).doesNotHaveBean("outerProfilingFilter");
            assertThat(ctx).doesNotHaveBean("postSecurityProfilingFilter");
            assertThat(ctx).doesNotHaveBean("dataSourceTimingPostProcessor");
            // The DataSource is the raw bean — never wrapped in TimingDataSource.
            assertThat(ctx.getBean(DataSource.class)).isNotInstanceOf(TimingDataSource.class);
        });
    }

    @Test
    void absentProperty_defaultsToDisabled() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ProfilingMetrics.class);
            assertThat(ctx).doesNotHaveBean("outerProfilingFilter");
            assertThat(ctx.getBean(DataSource.class)).isNotInstanceOf(TimingDataSource.class);
        });
    }

    @Test
    void enabled_createsBeans_andWrapsDataSource() {
        runner.withPropertyValues("app.profiling.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(ProfilingMetrics.class);
            assertThat(ctx).hasBean("outerProfilingFilter");
            assertThat(ctx).hasBean("postSecurityProfilingFilter");
            assertThat(ctx.getBean(DataSource.class)).isInstanceOf(TimingDataSource.class);
        });
    }
}
