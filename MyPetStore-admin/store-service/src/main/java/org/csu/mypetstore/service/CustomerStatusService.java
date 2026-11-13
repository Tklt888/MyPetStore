package org.csu.mypetstore.service;

import org.csu.mypetstore.model.command.CustomerStatusCommand;

public interface CustomerStatusService {
    CustomerStatusResult changeStatus(long customerId, CustomerStatusCommand command);
}
