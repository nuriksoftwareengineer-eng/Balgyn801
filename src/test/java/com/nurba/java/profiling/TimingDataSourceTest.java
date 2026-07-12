package com.nurba.java.profiling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Resilience of the DataSource wrapper: acquiring a connection must succeed and stay usable whether
 * or not a request is being profiled, and the timing bookkeeping must never break acquisition.
 */
class TimingDataSourceTest {

    @AfterEach
    void cleanup() {
        RequestTimings.clear();
    }

    @Test
    void getConnection_worksWithNoActiveRequest_andDelegatesToRawConnection() throws Exception {
        DataSource target = mock(DataSource.class);
        Connection raw = mock(Connection.class);
        when(target.getConnection()).thenReturn(raw);
        TimingDataSource ds = new TimingDataSource(target);

        assertThat(RequestTimings.current()).isNull(); // no request in flight
        Connection c = ds.getConnection();             // must not throw

        assertThat(c).isNotNull();
        c.close();
        verify(raw).close();                            // proxy delegates to the real connection
    }

    @Test
    void getConnection_recordsAcquisition_whenRequestActive() throws Exception {
        DataSource target = mock(DataSource.class);
        when(target.getConnection()).thenReturn(mock(Connection.class));
        TimingDataSource ds = new TimingDataSource(target);

        RequestTimings.begin();
        ds.getConnection();

        assertThat(RequestTimings.current().getConnectionCount).isEqualTo(1);
    }
}
