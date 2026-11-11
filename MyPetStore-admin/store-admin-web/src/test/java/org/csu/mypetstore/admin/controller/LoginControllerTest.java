package org.csu.mypetstore.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginControllerTest {

    @Test
    void loginConsumesRememberedUsernameAndShowsOneMessage() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("LOGIN_USERNAME", "admin");
        ExtendedModelMap model = new ExtendedModelMap();
        LoginController controller = new LoginController();

        String view = controller.login("", null, null, session, model);

        assertEquals("login", view);
        assertEquals("admin", model.get("username"));
        assertEquals("账号或密码错误，或账号不可用", model.get("message"));
        assertNull(session.getAttribute("LOGIN_USERNAME"));
    }

    @Test
    void logoutAndExpiredMessagesAreDistinct() {
        LoginController controller = new LoginController();
        ExtendedModelMap logoutModel = new ExtendedModelMap();
        ExtendedModelMap expiredModel = new ExtendedModelMap();

        controller.login(null, "", null, new MockHttpSession(), logoutModel);
        controller.login(null, null, "", new MockHttpSession(), expiredModel);

        assertEquals("已安全退出", logoutModel.get("message"));
        assertEquals("登录状态已失效，请重新登录", expiredModel.get("message"));
    }

    @Test
    void loginTemplatePostsCredentialsWithCsrf() {
        WebContext context = webContext();
        context.setVariable("username", "admin");
        context.setVariable("message", null);
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("login", context);

        assertTrue(html.contains("method=\"post\""));
        assertTrue(html.contains("action=\"/login\""));
        assertTrue(html.contains("name=\"username\""));
        assertTrue(html.contains("name=\"password\""));
        assertTrue(html.contains("name=\"_csrf\""));
        assertTrue(html.contains("value=\"token-123\""));
        assertFalse(html.contains("PetStore123!"));
    }

    private WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        return new WebContext(JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(request, response));
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
