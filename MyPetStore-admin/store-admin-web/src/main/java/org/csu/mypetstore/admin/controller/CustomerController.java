package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.model.command.CustomerStatusCommand;
import org.csu.mypetstore.model.query.CustomerListQuery;
import org.csu.mypetstore.service.CustomerQueryService;
import org.csu.mypetstore.service.CustomerStatusResult;
import org.csu.mypetstore.service.CustomerStatusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class CustomerController {
    private final CustomerQueryService queryService;
    private final CustomerStatusService statusService;

    public CustomerController(CustomerQueryService queryService, CustomerStatusService statusService) {
        this.queryService = queryService;
        this.statusService = statusService;
    }

    @GetMapping("/admin/customers")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String page,
                       @RequestParam(required = false) String pageSize, Model model) {
        CustomerListQuery query = new CustomerListQuery();
        query.setKeyword(keyword); query.setStatus(status);
        query.setPage(parseLong(page, 1)); query.setPageSize(parseLong(pageSize, 10));
        query.normalize();
        query.setPage(query.normalizedPage()); query.setPageSize(query.normalizedPageSize());
        model.addAttribute("page", queryService.findCustomers(query));
        model.addAttribute("query", query);
        List<Long> options = new ArrayList<>(List.of(10L, 20L, 50L));
        if (!options.contains(query.normalizedPageSize())) options.add(query.normalizedPageSize());
        options.sort(Long::compareTo);
        model.addAttribute("pageSizeOptions", options);
        return "customer/list";
    }

    @PostMapping("/admin/customers/{id}/status")
    public String changeStatus(@PathVariable long id, @RequestParam String targetStatus,
                               RedirectAttributes redirect) {
        CustomerStatusResult result = statusService.changeStatus(id, new CustomerStatusCommand(targetStatus));
        if (result == CustomerStatusResult.UNCHANGED) {
            redirect.addFlashAttribute("message", "客户账号状态未变化");
        } else {
            redirect.addFlashAttribute("message", "DISABLED".equalsIgnoreCase(targetStatus.strip())
                    ? "客户账号已禁用" : "客户账号已启用");
        }
        return "redirect:/admin/customers";
    }

    private long parseLong(String value, long defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Long.parseLong(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("分页参数格式不正确"); }
    }
}
