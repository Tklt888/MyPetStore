package org.csu.mypetstore.service;

import org.csu.mypetstore.model.dto.DashboardSnapshot;

public interface DashboardService {
    DashboardSnapshot load();
    long pendingShipmentCount();
}
