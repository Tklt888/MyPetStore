package org.csu.mypetstore.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.model.dto.OrderDetail;
import org.csu.mypetstore.model.dto.OrderListItem;
import org.csu.mypetstore.service.OrderFulfillmentService;
import org.csu.mypetstore.service.OrderNotFoundException;
import org.csu.mypetstore.service.OrderQueryService;
import org.csu.mypetstore.service.OrderStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class OrderControllerTest {
    private OrderQueryService queryService;
    private OrderFulfillmentService fulfillmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryService = mock(OrderQueryService.class);
        fulfillmentService = mock(OrderFulfillmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(queryService, fulfillmentService))
                .setControllerAdvice(new OrderExceptionHandler()).build();
    }

    @Test
    void listAddsPageQueryAndStatusLabels() throws Exception {
        Page<OrderListItem> page = new Page<>(1, 10, 0);
        when(queryService.findOrders(any())).thenReturn(page);

        mockMvc.perform(get("/admin/orders").param("orderNo", " ORD-1 ").param("status", "PAID"))
                .andExpect(status().isOk()).andExpect(view().name("order/list"))
                .andExpect(model().attribute("page", page))
                .andExpect(model().attributeExists("query", "statusLabels"));
    }

    @Test
    void pageParsingFollowsUnifiedRules() throws Exception {
        when(queryService.findOrders(any())).thenAnswer(invocation -> {
            org.csu.mypetstore.model.query.OrderListQuery query = invocation.getArgument(0);
            query.normalize();
            return new Page<OrderListItem>(query.normalizedPage(), query.normalizedPageSize(), 0);
        });
        mockMvc.perform(get("/admin/orders").param("page", "-7").param("pageSize", "50"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/orders").param("page", "abc"))
                .andExpect(status().isBadRequest()).andExpect(view().name("error/400"));
        mockMvc.perform(get("/admin/orders").param("page", "999999999999999999999"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/orders").param("pageSize", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/orders").param("pageSize", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailAndMissingOrderUseExpectedViews() throws Exception {
        when(queryService.getOrder(8L)).thenReturn(detail(8L, "PAID"));
        when(queryService.getOrder(404L)).thenThrow(new OrderNotFoundException(404L));

        mockMvc.perform(get("/admin/orders/8"))
                .andExpect(status().isOk()).andExpect(view().name("order/detail"))
                .andExpect(model().attributeExists("order", "statusLabels"));
        mockMvc.perform(get("/admin/orders/404"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"));
    }

    @Test
    void shipmentUsesPostRedirectGetAndSafeFailureMessage() throws Exception {
        mockMvc.perform(post("/admin/orders/8/ship"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/8"))
                .andExpect(flash().attribute("message", "订单已发货"));
        verify(fulfillmentService).ship(8L);

        doThrow(new OrderStateException("当前订单状态不能发货"))
                .when(fulfillmentService).ship(9L);
        mockMvc.perform(post("/admin/orders/9/ship"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "当前订单状态不能发货"));
    }

    @Test
    void paidDetailTemplateShowsSnapshotAndProtectedShippingForm() {
        WebContext context = webContext();
        OrderDetail order = detail(8L, "PAID").withItems(List.of(
                new org.csu.mypetstore.model.dto.OrderItemSnapshot(1L, 2L, "OLD-SKU", "成交时名称",
                        "images/old.png", new BigDecimal("12.50"), 2, new BigDecimal("25.00"))));
        context.setVariable("order", order);
        context.setVariable("statusLabels", Map.of("PAID", "已支付"));
        context.setVariable("message", null); context.setVariable("error", null);
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("order/detail", context);

        org.junit.jupiter.api.Assertions.assertTrue(html.contains("成交时名称"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("MOCK"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("action=\"/admin/orders/8/ship\""));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("name=\"_csrf\""));
    }

    private WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        return new WebContext(JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML"); resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private OrderDetail detail(Long id, String state) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 13, 10, 0);
        return new OrderDetail(id, "ORD-1", 2L, "customer", "顾客", state,
                new BigDecimal("25.00"), "张三", "13800000000", "长沙市", now, now,
                "PAID".equals(state) ? "MOCK" : null, "PAID".equals(state) ? "PAY-1" : null,
                "PAID".equals(state) ? now : null, null, null, null, List.of());
    }
}
