package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.response.DashboardResponse;
import com.thefarhany.crmsales.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard data retrieved successfully", dashboard)
        );
    }
}
