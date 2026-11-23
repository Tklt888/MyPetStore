package org.csu.mypetstore.user.service;

import org.csu.mypetstore.user.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreRulesTest {
    @Test void captchaCanOnlyBeUsedOnce() {
        CaptchaService service = new CaptchaService();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CaptchaService.KEY, "1234");
        assertTrue(service.verify(session, "1234"));
        assertFalse(service.verify(session, "1234"));
    }

    @Test void registrationRejectsShortPasswordsBeforeWriting() {
        StoreUserMapper users = mock(StoreUserMapper.class);
        AccountService service = new AccountService(users, mock(PasswordEncoder.class));
        StoreException error = assertThrows(StoreException.class,
                () -> service.register("alice2", "short", "Alice", null, null));
        assertTrue(error.getMessage().contains("8—72"));
        verifyNoInteractions(users);
    }

    @Test void catalogRejectsUnknownSortValues() {
        CatalogService service = new CatalogService(mock(CatalogMapper.class));
        assertThrows(StoreException.class, () -> service.search(null, null, "ALL", "DROP_TABLE", 1, 6));
    }
}
