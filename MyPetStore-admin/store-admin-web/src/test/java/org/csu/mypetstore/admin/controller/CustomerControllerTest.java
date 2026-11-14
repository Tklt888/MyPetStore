package org.csu.mypetstore.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.model.command.CustomerStatusCommand;
import org.csu.mypetstore.model.dto.CustomerListItem;
import org.csu.mypetstore.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerControllerTest {
    private CustomerQueryService queryService;
    private CustomerStatusService statusService;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        queryService = mock(CustomerQueryService.class);
        statusService = mock(CustomerStatusService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CustomerController(queryService, statusService))
                .setControllerAdvice(new CustomerExceptionHandler()).build();
    }

    @Test void listReturnsPageAndRejectsBadPaging() throws Exception {
        when(queryService.findCustomers(any())).thenReturn(new Page<CustomerListItem>(1, 10, 0));
        mvc.perform(get("/admin/customers").param("keyword", " tom ").param("status", "ENABLED"))
                .andExpect(status().isOk()).andExpect(view().name("customer/list"))
                .andExpect(model().attributeExists("page", "query", "pageSizeOptions"));
        mvc.perform(get("/admin/customers").param("page", "overflow999999999999999"))
                .andExpect(status().isBadRequest()).andExpect(view().name("error/400"));
        mvc.perform(get("/admin/customers").param("pageSize", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test void listCanonicalizesStatusForStableFormRoundTrip() throws Exception {
        when(queryService.findCustomers(any())).thenReturn(new Page<CustomerListItem>(1, 10, 0));
        mvc.perform(get("/admin/customers").param("status", " enabled "))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("query"));
        ArgumentCaptor<org.csu.mypetstore.model.query.CustomerListQuery> captor =
                ArgumentCaptor.forClass(org.csu.mypetstore.model.query.CustomerListQuery.class);
        verify(queryService).findCustomers(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("ENABLED", captor.getValue().getStatus());
    }

    @Test void changeStatusUsesPrgForChangedAndRepeated() throws Exception {
        when(statusService.changeStatus(eq(7L), any(CustomerStatusCommand.class)))
                .thenReturn(CustomerStatusResult.CHANGED, CustomerStatusResult.UNCHANGED);
        mvc.perform(post("/admin/customers/7/status").param("targetStatus", "DISABLED"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/customers"))
                .andExpect(flash().attribute("message", "客户账号已禁用"));
        mvc.perform(post("/admin/customers/7/status").param("targetStatus", "DISABLED"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message", "客户账号状态未变化"));
    }

    @Test void mapsForbiddenMissingAndInvalidToRealHttpStatuses() throws Exception {
        when(statusService.changeStatus(eq(1L), any())).thenThrow(new CustomerForbiddenException());
        when(statusService.changeStatus(eq(2L), any())).thenThrow(new CustomerNotFoundException(2L));
        when(statusService.changeStatus(eq(3L), any())).thenThrow(new IllegalArgumentException("不支持的客户状态"));
        mvc.perform(post("/admin/customers/1/status").param("targetStatus", "DISABLED"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/customers/2/status").param("targetStatus", "DISABLED"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/admin/customers/3/status").param("targetStatus", "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test void templateHasOnlyReverseStatusActionCsrfAndNoSensitiveEditing() {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        Page<CustomerListItem> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(new CustomerListItem(7L, "tom", "汤姆", "t@example.com",
                "13800000000", "ENABLED", LocalDateTime.of(2026, 11, 13, 10, 0))));
        org.csu.mypetstore.model.query.CustomerListQuery query = new org.csu.mypetstore.model.query.CustomerListQuery();
        query.setPage(1L); query.setPageSize(10L); query.normalize();
        context.setVariable("page", page); context.setVariable("query", query);
        context.setVariable("pageSizeOptions", List.of(10L, 20L, 50L)); context.setVariable("message", null);
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"));
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        String html = engine.process("customer/list", context);
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("action=\"/admin/customers/7/status\""));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("value=\"DISABLED\""));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("name=\"_csrf\""));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("password_hash"));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("删除客户"));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("编辑角色"));
    }

    @Test void outOfRangeTemplateOffersFirstAndLastPageWithoutDroppingFilters() {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        Page<CustomerListItem> page = new Page<>(9, 10, 21); page.setRecords(List.of());
        org.csu.mypetstore.model.query.CustomerListQuery query = new org.csu.mypetstore.model.query.CustomerListQuery();
        query.setKeyword("tom"); query.setStatus("ENABLED"); query.setPage(9L); query.setPageSize(10L); query.normalize();
        context.setVariable("page", page); context.setVariable("query", query);
        context.setVariable("pageSizeOptions", List.of(10L, 20L, 50L)); context.setVariable("message", null);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/"); resolver.setSuffix(".html");
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        String html = engine.process("customer/list", context);
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("返回第一页"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("前往最后一页"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("keyword=tom"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("status=ENABLED"));
    }
}
