package org.csu.mypetstore.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.csu.mypetstore.user.domain.StoreUser;
import org.csu.mypetstore.user.mapper.StoreUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class StoreAccountStatusFilter extends OncePerRequestFilter {

    private final StoreUserMapper users;

    public StoreAccountStatusFilter(StoreUserMapper users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals(request.getContextPath() + "/login");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof StorePrincipal principal
                && !isActiveCustomer(principal.id())) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?expired");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isActiveCustomer(long userId) {
        StoreUser user = users.selectById(userId);
        return user != null
                && "CUSTOMER".equals(user.getRole())
                && "ENABLED".equals(user.getStatus());
    }
}
