package org.csu.mypetstore.user.config;

import org.csu.mypetstore.user.security.*;import org.csu.mypetstore.user.service.CaptchaService;
import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.web.SecurityFilterChain;import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean SecurityFilterChain filterChain(HttpSecurity http,StoreUserDetailsService users,CaptchaService captcha,StoreAccountStatusFilter accountStatus)throws Exception{
        http.userDetailsService(users).authorizeHttpRequests(a->a.requestMatchers("/","/index","/main","/category","/product","/login","/register","/captcha.svg","/css/**","/js/**","/images/**","/error").permitAll().anyRequest().hasRole("CUSTOMER"))
            .formLogin(f->f.loginPage("/login").loginProcessingUrl("/login").defaultSuccessUrl("/main").failureUrl("/login?error").permitAll())
            .logout(l->l.logoutUrl("/logout").logoutSuccessUrl("/main?logout").invalidateHttpSession(true).deleteCookies("PETSTORE_USER_SESSION"))
            .addFilterBefore(new CaptchaFilter(captcha),UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(accountStatus,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
