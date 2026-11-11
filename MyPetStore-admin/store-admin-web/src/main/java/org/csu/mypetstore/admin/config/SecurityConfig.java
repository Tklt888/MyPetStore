package org.csu.mypetstore.admin.config;

import org.csu.mypetstore.admin.security.AdminAccountStatusFilter;
import org.csu.mypetstore.admin.security.AdminAuthenticationFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AdminAuthenticationFailureHandler adminAuthenticationFailureHandler() {
        return new AdminAuthenticationFailureHandler();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AdminAccountStatusFilter accountStatusFilter,
            AdminAuthenticationFailureHandler authenticationFailureHandler) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureHandler(authenticationFailureHandler)
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("PETSTORE_ADMIN_SESSION"))
                .addFilterBefore(accountStatusFilter, AuthorizationFilter.class);
        return http.build();
    }
}
