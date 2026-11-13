package org.csu.mypetstore.service.impl;

import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.StoreUser;
import org.csu.mypetstore.model.command.CustomerStatusCommand;
import org.csu.mypetstore.service.CustomerForbiddenException;
import org.csu.mypetstore.service.CustomerNotFoundException;
import org.csu.mypetstore.service.CustomerStateException;
import org.csu.mypetstore.service.CustomerStatusResult;
import org.csu.mypetstore.service.CustomerStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class CustomerStatusServiceImpl implements CustomerStatusService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("ENABLED", "DISABLED");
    private final StoreUserMapper mapper;

    public CustomerStatusServiceImpl(StoreUserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CustomerStatusResult changeStatus(long customerId, CustomerStatusCommand command) {
        if (customerId <= 0) throw new IllegalArgumentException("客户编号必须为正数");
        if (command == null || command.targetStatus() == null) {
            throw new IllegalArgumentException("目标状态不能为空");
        }

        String targetStatus = command.targetStatus().strip().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException("不支持的客户状态");
        }
        String oldStatus = "ENABLED".equals(targetStatus) ? "DISABLED" : "ENABLED";
        if (mapper.updateCustomerStatus(customerId, oldStatus, targetStatus) == 1) {
            return CustomerStatusResult.CHANGED;
        }

        StoreUser current = mapper.selectRoleStatusForUpdate(customerId);
        if (current == null) throw new CustomerNotFoundException(customerId);
        if (!"CUSTOMER".equals(current.getRole())) throw new CustomerForbiddenException();
        if (targetStatus.equals(current.getStatus())) return CustomerStatusResult.UNCHANGED;
        throw new CustomerStateException("账号状态已发生变化，请刷新后重试");
    }
}
