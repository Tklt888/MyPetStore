package org.csu.mypetstore.service;

import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.StoreUser;
import org.csu.mypetstore.model.dto.AdminAccount;
import org.csu.mypetstore.service.impl.AdminAccountServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAccountServiceTest {

    @Test
    void validUsernameIsTrimmedBeforeLookup() {
        AtomicReference<String> requestedUsername = new AtomicReference<>();
        StoreUser admin = user(4L, "admin", "hash", "商城管理员", "ADMIN", "ENABLED");
        AdminAccountService service = new AdminAccountServiceImpl(mapper(
                requestedUsername, new AtomicInteger(), admin));

        Optional<AdminAccount> result = service.findLoginAccount("  admin  ");

        assertTrue(result.isPresent());
        assertEquals("admin", requestedUsername.get());
        assertEquals(4L, result.orElseThrow().id());
    }

    @Test
    void invalidUsernamesDoNotQueryDatabase() {
        AtomicInteger queryCount = new AtomicInteger();
        AdminAccountService service = new AdminAccountServiceImpl(mapper(
                new AtomicReference<>(), queryCount, null));

        assertTrue(service.findLoginAccount(null).isEmpty());
        assertTrue(service.findLoginAccount("   ").isEmpty());
        assertTrue(service.findLoginAccount("\u3000\u3000").isEmpty());
        assertTrue(service.findLoginAccount("x".repeat(51)).isEmpty());
        assertEquals(0, queryCount.get());
    }

    @Test
    void onlyEnabledAdminIsActive() {
        AdminAccountService service = new AdminAccountServiceImpl(statusMapper());

        assertTrue(service.isActiveAdmin(1L));
        assertFalse(service.isActiveAdmin(2L));
        assertFalse(service.isActiveAdmin(3L));
        assertFalse(service.isActiveAdmin(4L));
        assertFalse(service.isActiveAdmin(null));
    }

    private StoreUserMapper mapper(
            AtomicReference<String> requestedUsername,
            AtomicInteger queryCount,
            StoreUser result) {
        return (StoreUserMapper) Proxy.newProxyInstance(
                StoreUserMapper.class.getClassLoader(),
                new Class<?>[]{StoreUserMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectByUsername")) {
                        queryCount.incrementAndGet();
                        requestedUsername.set((String) args[0]);
                        return result;
                    }
                    return null;
                });
    }

    private StoreUserMapper statusMapper() {
        return (StoreUserMapper) Proxy.newProxyInstance(
                StoreUserMapper.class.getClassLoader(),
                new Class<?>[]{StoreUserMapper.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("selectRoleStatusById")) {
                        return null;
                    }
                    Long id = (Long) args[0];
                    return switch (id.intValue()) {
                        case 1 -> user(id, null, null, null, "ADMIN", "ENABLED");
                        case 2 -> user(id, null, null, null, "CUSTOMER", "ENABLED");
                        case 3 -> user(id, null, null, null, "ADMIN", "DISABLED");
                        default -> null;
                    };
                });
    }

    private StoreUser user(
            Long id,
            String username,
            String passwordHash,
            String displayName,
            String role,
            String status) {
        StoreUser user = new StoreUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
