package org.csu.mypetstore.admin;

import org.csu.mypetstore.admin.controller.HomeController;
import org.csu.mypetstore.admin.security.AdminPrincipal;
import org.csu.mypetstore.service.ProductService;
import org.junit.jupiter.api.Test;
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

class AdminApplicationTest {

    @Test
    void homePageRendersAdminApplicationNameAndProductCount() {
        ProductService productService = () -> 40L;
        HomeController controller = new HomeController(productService);
        ExtendedModelMap model = new ExtendedModelMap();
        AdminPrincipal principal = new AdminPrincipal(4L, "admin", "hash", "商城管理员");

        String viewName = controller.index(principal, model);

        assertEquals("index", viewName);
        assertEquals("MyPetStore 管理端", model.get("applicationName"));
        assertEquals(40L, model.get("productCount"));
        assertEquals("商城管理员", model.get("adminDisplayName"));
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
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("index", context);

        assertTrue(html.contains("method=\"post\""));
        assertTrue(html.contains("action=\"/logout\""));
        assertTrue(html.contains("name=\"_csrf\""));
        assertFalse(html.contains("href=\"/logout\""));
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
