package org.csu.mypetstore.service.impl;

import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.StoreUser;
import org.csu.mypetstore.model.dto.AdminAccount;
import org.csu.mypetstore.service.AdminAccountService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminAccountServiceImpl implements AdminAccountService {

    private static final int MAX_USERNAME_LENGTH = 50;

    private final StoreUserMapper storeUserMapper;

    public AdminAccountServiceImpl(StoreUserMapper storeUserMapper) {
        this.storeUserMapper = storeUserMapper;
    }

    @Override
    public Optional<AdminAccount> findLoginAccount(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String normalized = username.strip();
        if (normalized.isEmpty() || normalized.length() > MAX_USERNAME_LENGTH) {
            return Optional.empty();
        }
        return Optional.ofNullable(storeUserMapper.selectByUsername(normalized))
                .map(this::toAdminAccount);
    }

    @Override
    public boolean isActiveAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        StoreUser user = storeUserMapper.selectRoleStatusById(userId);
        return user != null
                && "ADMIN".equals(user.getRole())
                && "ENABLED".equals(user.getStatus());
    }

    private AdminAccount toAdminAccount(StoreUser user) {
        return new AdminAccount(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus());
    }
}
