package org.csu.mypetstore.admin.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

public class AdminAuthenticationFailureHandler implements AuthenticationFailureHandler {

    public static final String LOGIN_USERNAME = "LOGIN_USERNAME";
    private static final int MAX_USERNAME_LENGTH = 50;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String submittedUsername = request.getParameter("username");
        String safeUsername = submittedUsername == null ? "" : submittedUsername.trim();
        if (safeUsername.length() > MAX_USERNAME_LENGTH) {
            safeUsername = safeUsername.substring(0, MAX_USERNAME_LENGTH);
        }
        request.getSession().setAttribute(LOGIN_USERNAME, safeUsername);
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
