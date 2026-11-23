package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.admin.security.AdminPrincipal;
import org.csu.mypetstore.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdminNavigationAdvice {
    private final DashboardService dashboardService;

    public AdminNavigationAdvice(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @ModelAttribute("pendingShipmentCount")
    public long pendingShipmentCount(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AdminPrincipal
                ? dashboardService.pendingShipmentCount() : 0L;
    }

    @ModelAttribute("adminUsername")
    public String adminUsername(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AdminPrincipal principal
                ? principal.getUsername() : "admin";
    }

    @ModelAttribute("adminDisplayName")
    public String adminDisplayName(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AdminPrincipal principal
                ? principal.displayName() : "商城管理员";
    }
}
