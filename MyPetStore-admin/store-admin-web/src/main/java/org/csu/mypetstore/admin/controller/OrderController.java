package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.model.query.OrderListQuery;
import org.csu.mypetstore.service.OrderFulfillmentService;
import org.csu.mypetstore.service.OrderQueryService;
import org.csu.mypetstore.service.OrderStateException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class OrderController {
    private static final Map<String, String> STATUS_LABELS = statusLabels();
    private final OrderQueryService queryService;
    private final OrderFulfillmentService fulfillmentService;

    public OrderController(OrderQueryService queryService, OrderFulfillmentService fulfillmentService) {
        this.queryService = queryService;
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/admin/orders")
    public String list(@RequestParam(required = false) String orderNo,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String page,
                       @RequestParam(required = false) String pageSize, Model model) {
        OrderListQuery query = new OrderListQuery();
        query.setOrderNo(orderNo); query.setStatus(status);
        query.setPage(parseLong(page, 1L)); query.setPageSize(parseLong(pageSize, 10L));
        model.addAttribute("page", queryService.findOrders(query));
        model.addAttribute("query", query);
        model.addAttribute("statusLabels", STATUS_LABELS);
        return "order/list";
    }

    @GetMapping("/admin/orders/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("order", queryService.getOrder(id));
        model.addAttribute("statusLabels", STATUS_LABELS);
        return "order/detail";
    }

    @PostMapping("/admin/orders/{id}/ship")
    public String ship(@PathVariable long id, RedirectAttributes redirect) {
        try {
            fulfillmentService.ship(id);
            redirect.addFlashAttribute("message", "订单已发货");
        } catch (OrderStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    private long parseLong(String value, long defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Long.parseLong(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("分页参数格式不正确"); }
    }

    private static Map<String, String> statusLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("PENDING_PAYMENT", "待支付"); labels.put("PAID", "已支付");
        labels.put("SHIPPED", "已发货"); labels.put("COMPLETED", "已完成");
        labels.put("CANCELLED", "已取消");
        return Map.copyOf(labels);
    }
}
