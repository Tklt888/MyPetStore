package org.csu.mypetstore.admin.security;

import jakarta.servlet.FilterChain;
import org.csu.mypetstore.model.dto.AdminAccount;
import org.csu.mypetstore.service.AdminAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAccountStatusFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeAdminContinuesTheFilterChain() throws Exception {
        AtomicInteger chainCalls = new AtomicInteger();
        AdminAccountStatusFilter filter = new AdminAccountStatusFilter(accountService(true, new AtomicInteger()));
        authenticateAdmin();

        filter.doFilter(request("/admin/products"), new MockHttpServletResponse(),
                countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
    }

    @Test
    void inactiveAdminLosesAuthenticationAndSession() throws Exception {
        AdminAccountStatusFilter filter = new AdminAccountStatusFilter(accountService(false, new AtomicInteger()));
        authenticateAdmin();
        MockHttpServletRequest request = request("/admin/customers/7/status");
        MockHttpSession session = (MockHttpSession) request.getSession();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, response, countingChain(chainCalls));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(session.isInvalid());
        assertEquals("/login?expired", response.getRedirectedUrl());
        assertEquals(0, chainCalls.get());
    }

    @Test
    void anonymousAndLoginRequestsDoNotQueryAccountStatus() throws Exception {
        AtomicInteger statusQueries = new AtomicInteger();
        AdminAccountStatusFilter filter = new AdminAccountStatusFilter(accountService(true, statusQueries));

        filter.doFilter(request("/admin/products"), new MockHttpServletResponse(),
                countingChain(new AtomicInteger()));
        authenticateAdmin();
        filter.doFilter(request("/login"), new MockHttpServletResponse(),
                countingChain(new AtomicInteger()));

        assertEquals(0, statusQueries.get());
    }

    private void authenticateAdmin() {
        AdminPrincipal principal = new AdminPrincipal(4L, "admin", "hash", "商城管理员");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    private FilterChain countingChain(AtomicInteger calls) {
        return (request, response) -> calls.incrementAndGet();
    }

    private AdminAccountService accountService(boolean active, AtomicInteger statusQueries) {
        return new AdminAccountService() {
            @Override
            public Optional<AdminAccount> findLoginAccount(String username) {
                return Optional.empty();
            }

            @Override
            public boolean isActiveAdmin(Long userId) {
                statusQueries.incrementAndGet();
                return active;
            }
        };
    }
}
