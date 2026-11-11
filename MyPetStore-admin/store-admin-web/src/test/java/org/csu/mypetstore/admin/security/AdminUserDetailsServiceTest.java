package org.csu.mypetstore.admin.security;

import org.csu.mypetstore.model.dto.AdminAccount;
import org.csu.mypetstore.service.AdminAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserDetailsServiceTest {

    private static final String ADMIN_HASH =
            "$2a$10$TWF0IW1UuBapXm2v.rwh2u9CemHHGhrjybt9CLxmNcXG1t6boGrtK";

    @Test
    void enabledAdminBecomesAdminPrincipal() {
        AdminUserDetailsService service = new AdminUserDetailsService(
                accountService(account("ADMIN", "ENABLED")));

        AdminPrincipal principal = (AdminPrincipal) service.loadUserByUsername("admin");

        assertEquals(4L, principal.userId());
        assertEquals("admin", principal.getUsername());
        assertEquals("商城管理员", principal.displayName());
        assertEquals("ROLE_ADMIN", principal.getAuthorities().iterator().next().getAuthority());
        assertFalse(principal.toString().contains(ADMIN_HASH));
    }

    @Test
    void unavailableAccountsHaveTheSameFailureType() {
        assertUnavailable(account("CUSTOMER", "ENABLED"));
        assertUnavailable(account("ADMIN", "DISABLED"));
        assertUnavailable(null);
    }

    @Test
    void demoAdminHashMatchesOnlyTheTeachingPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        assertTrue(encoder.matches("PetStore123!", ADMIN_HASH));
        assertFalse(encoder.matches("wrong-password", ADMIN_HASH));
    }

    private void assertUnavailable(AdminAccount account) {
        AdminUserDetailsService service = new AdminUserDetailsService(accountService(account));
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("admin"));
    }

    private AdminAccount account(String role, String status) {
        return new AdminAccount(4L, "admin", ADMIN_HASH, "商城管理员", role, status);
    }

    private AdminAccountService accountService(AdminAccount account) {
        return new AdminAccountService() {
            @Override
            public Optional<AdminAccount> findLoginAccount(String username) {
                return Optional.ofNullable(account);
            }

            @Override
            public boolean isActiveAdmin(Long userId) {
                return false;
            }
        };
    }
}
