package org.csu.mypetstore.user.security;

import jakarta.servlet.FilterChain;
import org.csu.mypetstore.user.domain.StoreUser;
import org.csu.mypetstore.user.mapper.StoreUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreAccountStatusFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enabledCustomerContinuesTheFilterChain() throws Exception {
        StoreUserMapper users = mock(StoreUserMapper.class);
        when(users.selectById(7L)).thenReturn(user("CUSTOMER", "ENABLED"));
        StoreAccountStatusFilter filter = new StoreAccountStatusFilter(users);
        authenticateCustomer();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request("/orders"), new MockHttpServletResponse(), countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
    }

    @Test
    void disabledCustomerLosesAuthenticationAndSession() throws Exception {
        StoreUserMapper users = mock(StoreUserMapper.class);
        when(users.selectById(7L)).thenReturn(user("CUSTOMER", "DISABLED"));
        StoreAccountStatusFilter filter = new StoreAccountStatusFilter(users);
        authenticateCustomer();
        MockHttpServletRequest request = request("/cart");
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
        StoreUserMapper users = mock(StoreUserMapper.class);
        StoreAccountStatusFilter filter = new StoreAccountStatusFilter(users);

        filter.doFilter(request("/main"), new MockHttpServletResponse(), countingChain(new AtomicInteger()));
        authenticateCustomer();
        filter.doFilter(request("/login"), new MockHttpServletResponse(), countingChain(new AtomicInteger()));

        verify(users, never()).selectById(7L);
    }

    private void authenticateCustomer() {
        StorePrincipal principal = new StorePrincipal(7L, "alice", "hash", "Alice", "ENABLED");
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

    private StoreUser user(String role, String status) {
        StoreUser user = new StoreUser();
        user.setId(7L);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
