package org.csu.mypetstore.admin.config;

import org.csu.mypetstore.model.dto.AdminAccount;
import org.csu.mypetstore.service.AdminAccountService;
import org.csu.mypetstore.admin.security.AdminAccountStatusFilter;
import org.csu.mypetstore.admin.security.AdminPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityConfigTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityConfigTest {
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(true);
    private static final AtomicInteger CUSTOMER_STATUS_CALLS = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach void resetState() { ACTIVE.set(true); CUSTOMER_STATUS_CALLS.set(0); }

    @Test
    void loginIsPublicButAdminPagesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/customers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/unknown"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logoutRequiresPostAndCsrf() throws Exception {
        mockMvc.perform(get("/logout").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/logout").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/categories/1/delete")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/products")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/products/1/status")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/orders/1/ship")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/customers/1/status")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/customers/1/status").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedAdminWithCsrfCanReachCustomerStatusEndpoint() throws Exception {
        AdminPrincipal principal = new AdminPrincipal(4L, "admin", "hash", "商城管理员");
        var token = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, principal.getAuthorities());
        mockMvc.perform(post("/admin/customers/1/status").with(authentication(token)).with(csrf()))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1, CUSTOMER_STATUS_CALLS.get());
    }

    @Test
    void disabledAdminIsRedirectedBeforeCustomerStatusController() throws Exception {
        ACTIVE.set(false);
        AdminPrincipal principal = new AdminPrincipal(4L, "admin", "hash", "商城管理员");
        var token = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, principal.getAuthorities());
        mockMvc.perform(post("/admin/customers/1/status").with(authentication(token)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/login?expired"));
        org.junit.jupiter.api.Assertions.assertEquals(0, CUSTOMER_STATUS_CALLS.get());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({SecurityConfig.class, AdminAccountStatusFilter.class, TestController.class})
    static class TestApplication {

        @Bean
        AdminAccountService adminAccountService() {
            return new AdminAccountService() {
                @Override
                public Optional<AdminAccount> findLoginAccount(String username) {
                    return Optional.empty();
                }

                @Override
                public boolean isActiveAdmin(Long userId) {
                    return ACTIVE.get();
                }
            };
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password("{noop}password").roles("ADMIN").build());
        }
    }

    @RestController
    static class TestController {

        @GetMapping({"/login", "/", "/admin/products", "/admin/categories", "/admin/orders", "/admin/customers"})
        String page() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.PostMapping("/admin/customers/{id}/status")
        String customerStatus() { CUSTOMER_STATUS_CALLS.incrementAndGet(); return "ok"; }
    }
}
