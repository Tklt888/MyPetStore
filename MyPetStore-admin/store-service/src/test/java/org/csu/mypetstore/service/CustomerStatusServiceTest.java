package org.csu.mypetstore.service;

import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.StoreUser;
import org.csu.mypetstore.model.command.CustomerStatusCommand;
import org.csu.mypetstore.service.impl.CustomerStatusServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerStatusServiceTest {
    @Test
    void enablesAndDisablesWithExpectedOldState() {
        StoreUserMapper mapper = mock(StoreUserMapper.class);
        when(mapper.updateCustomerStatus(7L, "ENABLED", "DISABLED")).thenReturn(1);
        when(mapper.updateCustomerStatus(8L, "DISABLED", "ENABLED")).thenReturn(1);
        CustomerStatusService service = new CustomerStatusServiceImpl(mapper);

        assertEquals(CustomerStatusResult.CHANGED,
                service.changeStatus(7L, new CustomerStatusCommand("DISABLED")));
        assertEquals(CustomerStatusResult.CHANGED,
                service.changeStatus(8L, new CustomerStatusCommand("ENABLED")));
    }

    @Test
    void rejectsUnknownTargetWithoutWriting() {
        StoreUserMapper mapper = mock(StoreUserMapper.class);
        CustomerStatusService service = new CustomerStatusServiceImpl(mapper);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(7L, new CustomerStatusCommand("ADMIN")));
        verify(mapper, never()).updateCustomerStatus(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void failedUpdateDistinguishesMissingForbiddenRepeatAndConcurrentState() {
        StoreUserMapper missingMapper = mock(StoreUserMapper.class);
        when(missingMapper.selectRoleStatusForUpdate(1L)).thenReturn(null);
        assertThrows(CustomerNotFoundException.class,
                () -> new CustomerStatusServiceImpl(missingMapper)
                        .changeStatus(1L, new CustomerStatusCommand("DISABLED")));

        StoreUserMapper adminMapper = mock(StoreUserMapper.class);
        when(adminMapper.selectRoleStatusForUpdate(2L)).thenReturn(user(2L, "ADMIN", "ENABLED"));
        assertThrows(CustomerForbiddenException.class,
                () -> new CustomerStatusServiceImpl(adminMapper)
                        .changeStatus(2L, new CustomerStatusCommand("DISABLED")));

        StoreUserMapper repeatedMapper = mock(StoreUserMapper.class);
        when(repeatedMapper.selectRoleStatusForUpdate(3L)).thenReturn(user(3L, "CUSTOMER", "DISABLED"));
        assertEquals(CustomerStatusResult.UNCHANGED,
                new CustomerStatusServiceImpl(repeatedMapper)
                        .changeStatus(3L, new CustomerStatusCommand("DISABLED")));

        StoreUserMapper concurrentMapper = mock(StoreUserMapper.class);
        when(concurrentMapper.selectRoleStatusForUpdate(4L)).thenReturn(user(4L, "CUSTOMER", "ENABLED"));
        assertThrows(CustomerStateException.class,
                () -> new CustomerStatusServiceImpl(concurrentMapper)
                        .changeStatus(4L, new CustomerStatusCommand("DISABLED")));
    }

    private StoreUser user(Long id, String role, String status) {
        StoreUser user = new StoreUser(); user.setId(id); user.setRole(role); user.setStatus(status);
        return user;
    }
}
