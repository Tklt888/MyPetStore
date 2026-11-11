package org.csu.mypetstore.service;

import org.csu.mypetstore.model.dto.AdminAccount;

import java.util.Optional;

public interface AdminAccountService {

    Optional<AdminAccount> findLoginAccount(String username);

    boolean isActiveAdmin(Long userId);
}
