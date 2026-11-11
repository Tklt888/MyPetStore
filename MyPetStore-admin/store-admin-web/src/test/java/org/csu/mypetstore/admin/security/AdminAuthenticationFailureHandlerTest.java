package org.csu.mypetstore.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdminAuthenticationFailureHandlerTest {

    @Test
    void failureStoresOnlyTrimmedBoundedUsername() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "  " + "x".repeat(60) + "  ");
        request.setParameter("password", "must-not-enter-session");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AdminAuthenticationFailureHandler handler = new AdminAuthenticationFailureHandler();

        handler.onAuthenticationFailure(request, response,
                new BadCredentialsException("bad credentials"));

        assertEquals("x".repeat(50), request.getSession().getAttribute("LOGIN_USERNAME"));
        assertNull(request.getSession().getAttribute("password"));
        assertEquals("/login?error", response.getRedirectedUrl());
    }
}
