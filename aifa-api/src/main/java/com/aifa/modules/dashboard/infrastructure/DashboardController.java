package com.aifa.modules.dashboard.infrastructure;

import com.aifa.modules.dashboard.application.DashboardService;
import com.aifa.modules.dashboard.application.dto.DashboardSummaryResponse;
import com.aifa.shared.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUserProvider;

    public DashboardController(DashboardService dashboardService, CurrentUserProvider currentUserProvider) {
        this.dashboardService = dashboardService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary(currentUserProvider.getCurrentUserId());
    }
}
