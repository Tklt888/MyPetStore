package org.csu.mypetstore.admin;

import org.csu.mypetstore.admin.controller.HomeController;
import org.csu.mypetstore.admin.security.AdminPrincipal;
import org.csu.mypetstore.model.dto.DashboardSnapshot;
import org.csu.mypetstore.model.dto.DashboardStockItem;
import org.csu.mypetstore.model.dto.OrderListItem;
import org.csu.mypetstore.service.DashboardService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminApplicationTest {

    @Test
    void homePageRendersRealDashboardSnapshot() {
        DashboardService dashboardService = mock(DashboardService.class);
        DashboardSnapshot snapshot = new DashboardSnapshot(3, 5, 40, 18, java.util.List.of(), java.util.List.of());
        when(dashboardService.load()).thenReturn(snapshot);
        HomeController controller = new HomeController(dashboardService);
        ExtendedModelMap model = new ExtendedModelMap();
        AdminPrincipal principal = new AdminPrincipal(4L, "admin", "hash", "商城管理员");

        String viewName = controller.index(principal, model);

        assertEquals("index", viewName);
        assertEquals("MyPetStore 管理端", model.get("applicationName"));
        assertEquals(snapshot, model.get("dashboard"));
        assertEquals("商城管理员", model.get("adminDisplayName"));
        assertEquals("admin", model.get("adminUsername"));
    }

    @Test
    void homePageUsesPostLogoutFormWithCsrf() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(
                JakartaServletWebApplication.buildApplication(servletContext)
                        .buildExchange(request, response));
        context.setVariable("applicationName", "MyPetStore 管理端");
        context.setVariable("productCount", 40L);
        context.setVariable("adminDisplayName", "商城管理员");
        context.setVariable("adminUsername", "admin");
        context.setVariable("pendingShipmentCount", 0L);
        context.setVariable("dashboardDate", "2026年11月17日 星期二");
        context.setVariable("dashboard", new DashboardSnapshot(0, 0, 40, 0,
                java.util.List.of(), java.util.List.of()));
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("index", context);

        assertTrue(html.contains("method=\"post\""));
        assertTrue(html.contains("action=\"/logout\""));
        assertTrue(html.contains("name=\"_csrf\""));
        assertFalse(html.contains("href=\"/logout\""));
    }

    @Test
    void dashboardRendersRealWorkQueueData() {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        OrderListItem order = new OrderListItem(7L, "ORD-7", 2L, "lin", "林同学",
                new BigDecimal("128.00"), "PAID", LocalDateTime.of(2026, 11, 17, 9, 30), null, null);
        DashboardStockItem product = new DashboardStockItem(9L, "低库存商品", "/images/p9.jpg", 1, "ON_SALE");
        context.setVariable("applicationName", "MyPetStore 管理端");
        context.setVariable("adminDisplayName", "商城管理员");
        context.setVariable("adminUsername", "admin");
        context.setVariable("pendingShipmentCount", 1L);
        context.setVariable("dashboardDate", "2026年11月17日 星期二");
        context.setVariable("dashboard", new DashboardSnapshot(1, 1, 40, 18,
                java.util.List.of(order), java.util.List.of(product)));

        String html = templateEngine().process("index", context);

        assertTrue(html.contains("ORD-7"));
        assertTrue(html.contains("低库存商品"));
        assertTrue(html.contains("maxStock=3"));
        assertTrue(html.contains("商城管理员"));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
