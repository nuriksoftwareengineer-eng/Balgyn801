package com.nurba.java.service;

import com.nurba.java.dto.responce.DashboardStatsResponse;

public interface DashboardService {
    DashboardStatsResponse getStats(int days);
}
