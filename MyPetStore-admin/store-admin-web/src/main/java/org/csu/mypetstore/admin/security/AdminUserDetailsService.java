package org.csu.mypetstore.admin.security;

import org.csu.mypetstore.model.dto.AdminAccount;
import org.csu.mypetstore.service.AdminAccountService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private static final String UNAVAILABLE = "Admin account unavailable";

    private final AdminAccountService adminAccountService;

    public AdminUserDetailsService(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminAccount account = adminAccountService.findLoginAccount(username)
                .orElseThrow(() -> new UsernameNotFoundException(UNAVAILABLE));
        if (!"ADMIN".equals(account.role()) || !"ENABLED".equals(account.status())) {
            throw new UsernameNotFoundException(UNAVAILABLE);
        }
        return new AdminPrincipal(
                account.id(),
                account.username(),
                account.passwordHash(),
                account.displayName());
    }
}
