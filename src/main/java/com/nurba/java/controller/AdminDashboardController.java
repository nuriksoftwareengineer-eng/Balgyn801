package com.nurba.java.controller;

import com.nurba.java.dto.responce.DashboardStatsResponse;
import com.nurba.java.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public DashboardStatsResponse getStats(@RequestParam(defaultValue = "30") int days) {
        return dashboardService.getStats(Math.min(days, 365));
    }
}
