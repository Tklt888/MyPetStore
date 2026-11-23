package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.service.DashboardService;
import org.csu.mypetstore.admin.security.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("applicationName", "MyPetStore 管理端");
        model.addAttribute("dashboard", dashboardService.load());
        model.addAttribute("adminDisplayName", principal.displayName());
        model.addAttribute("adminUsername", principal.getUsername());
        model.addAttribute("dashboardDate", LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", java.util.Locale.SIMPLIFIED_CHINESE)));
        return "index";
    }
}
