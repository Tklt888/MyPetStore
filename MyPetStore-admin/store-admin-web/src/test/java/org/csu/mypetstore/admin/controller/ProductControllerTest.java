package org.csu.mypetstore.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.admin.form.ProductCreateForm;
import org.csu.mypetstore.admin.form.ProductRestockForm;
import org.csu.mypetstore.admin.form.ProductUpdateForm;
import org.csu.mypetstore.model.command.ProductCreateCommand;
import org.csu.mypetstore.model.command.ProductRestockCommand;
import org.csu.mypetstore.model.command.ProductStatusCommand;
import org.csu.mypetstore.model.command.ProductUpdateCommand;
import org.csu.mypetstore.model.dto.ProductCategoryOption;
import org.csu.mypetstore.model.dto.ProductEditView;
import org.csu.mypetstore.model.dto.ProductListItem;
import org.csu.mypetstore.model.query.ProductListQuery;
import org.csu.mypetstore.service.ProductQueryService;
import org.csu.mypetstore.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private ProductQueryService queryService;
    private ProductService productService;
    private ProductController controller;

    @BeforeEach
    void setUp() {
        queryService = mock(ProductQueryService.class);
        productService = mock(ProductService.class);
        controller = new ProductController(queryService, productService);
        when(productService.listCategoryOptions()).thenReturn(List.of(
                new ProductCategoryOption(11L, 1L, "犬类", "宠物犬")));
    }

    @Test
    void listAddsResultsFiltersAndCategoryOptions() {
        ProductListQuery query = query(2L, "cat", "ON_SALE");
        Page<ProductListItem> result = new Page<>(2, 10, 21);
        when(queryService.findProducts(query)).thenReturn(result);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("product/list", controller.list(query, model));
        assertSame(result, model.get("page"));
        assertSame(query, model.get("query"));
        assertEquals(1, ((List<?>) model.get("categoryOptions")).size());
    }

    @Test
    void topLevelFilterIsRenderedAndPreservedInPagingLinks() {
        ProductListQuery query = query(2L, null, null);
        query.setTopCategoryId(1L);
        Page<ProductListItem> page = new Page<>(2, 10, 31);
        page.setRecords(List.of());
        WebContext context = webContext();
        context.setVariable("page", page); context.setVariable("query", query);
        context.setVariable("categoryOptions", List.of(new ProductCategoryOption(11L, 1L, "犬类", "宠物犬")));
        context.setVariable("topCategoryOptions", List.of(new ProductCategoryOption(11L, 1L, "犬类", "宠物犬")));
        context.setVariable("message", null); context.setVariable("error", null);

        String html = templateEngine().process("product/list", context);

        assertTrue(html.contains("name=\"topCategoryId\""));
        assertTrue(html.contains("topCategoryId=1"));
    }

    @Test
    void newFormUsesSafeDefaultsAndSecondLevelOptions() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("product/form", controller.newForm(model));
        ProductCreateForm form = (ProductCreateForm) model.get("form");
        assertEquals("images/logo.png", form.getCoverImage());
        assertEquals(0, form.getStock());
        assertEquals(0, form.getSortOrder());
        assertEquals(1, ((List<?>) model.get("categoryOptions")).size());
    }

    @Test
    void createPassesOnlyAllowedFieldsAndRedirects() {
        ProductCreateForm form = createForm();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");

        String view = controller.create(form, errors, new ExtendedModelMap(), new RedirectAttributesModelMap(),
                new MockHttpServletResponse());

        assertEquals("redirect:/admin/products", view);
        verify(productService).create(any(ProductCreateCommand.class));
    }

    @Test
    void editAndUpdateKeepSkuStockStatusReadOnly() {
        when(productService.getForEdit(8L)).thenReturn(view());
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("product/form", controller.editForm(8L, model));
        ProductUpdateForm form = (ProductUpdateForm) model.get("form");
        assertEquals(2L, form.getVersion());
        assertEquals("SKU-8", ((ProductEditView) model.get("product")).sku());

        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        assertEquals("redirect:/admin/products",
                controller.update(8L, form, errors, new ExtendedModelMap(), new RedirectAttributesModelMap(),
                        new MockHttpServletResponse()));
        verify(productService).update(any(Long.class), any(ProductUpdateCommand.class));
    }

    @Test
    void statusAndRestockSubmitVersionedCommands() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/admin/products",
                controller.changeStatus(8L, "ON_SALE", 2L, redirect));
        verify(productService).changeStatus(8L, new ProductStatusCommand("ON_SALE", 2L));

        ProductRestockForm form = new ProductRestockForm();
        form.setQuantity(3);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        when(productService.restock(8L, new ProductRestockCommand(3))).thenReturn(view());
        assertEquals("redirect:/admin/products",
                controller.restock(8L, form, errors, new ExtendedModelMap(), redirect,
                        new MockHttpServletResponse()));
    }

    @Test
    void validationAndConflictResponsesUse400And409() {
        ProductCreateForm invalid = createForm();
        BeanPropertyBindingResult createErrors = new BeanPropertyBindingResult(invalid, "form");
        createErrors.rejectValue("name", "invalid", "商品名称不能为空");
        MockHttpServletResponse createResponse = new MockHttpServletResponse();
        controller.create(invalid, createErrors, new ExtendedModelMap(), new RedirectAttributesModelMap(), createResponse);
        assertEquals(400, createResponse.getStatus());

        ProductUpdateForm update = new ProductUpdateForm();
        update.setCategoryId(11L); update.setName("商品"); update.setCoverImage("images/a.png");
        update.setPrice(new BigDecimal("10.00")); update.setSortOrder(0); update.setVersion(1L);
        when(productService.getForEdit(8L)).thenReturn(view());
        org.mockito.Mockito.doThrow(new org.csu.mypetstore.service.ProductConflictException())
                .when(productService).update(any(Long.class), any(ProductUpdateCommand.class));
        MockHttpServletResponse updateResponse = new MockHttpServletResponse();
        controller.update(8L, update, new BeanPropertyBindingResult(update, "form"), new ExtendedModelMap(),
                new RedirectAttributesModelMap(), updateResponse);
        assertEquals(409, updateResponse.getStatus());
    }

    @Test
    void actualRequestBindingValidatesInputAndIgnoresForgedReadOnlyFields() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();

        mvc.perform(post("/admin/products").param("sku", " "))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/admin/products/8")
                        .param("categoryId", "11").param("name", "商品")
                        .param("coverImage", "images/a.png").param("price", "10.00")
                        .param("sortOrder", "0").param("version", "2")
                        .param("sku", "FORGED").param("stock", "9999").param("status", "ON_SALE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));
        verify(productService).update(any(Long.class), any(ProductUpdateCommand.class));
    }

    @Test
    void listTemplateHasMaintenanceActionsNoDeleteAndValidPageRecovery() {
        ProductListQuery query = query(999L, null, null);
        Page<ProductListItem> page = new Page<>(999, 10, 11);
        page.setRecords(List.of());
        WebContext context = webContext();
        context.setVariable("page", page);
        context.setVariable("query", query);
        context.setVariable("categoryOptions", List.of());
        context.setVariable("topCategoryOptions", List.of());
        context.setVariable("message", null);
        context.setVariable("error", null);

        String html = templateEngine().process("product/list", context);

        assertTrue(html.contains("新增商品"));
        assertTrue(html.contains("编辑"));
        assertTrue(html.contains("补货"));
        assertTrue(html.contains("page=1"));
        assertTrue(html.contains("page=2"));
        assertFalse(html.contains("删除商品"));
    }

    @Test
    void nonEmptyListRendersRealEditStatusAndRestockControls() {
        ProductListQuery query = query(1L, null, null);
        Page<ProductListItem> page = new Page<>(1, 10, 1);
        ProductListItem item = new ProductListItem();
        item.setId(8L); item.setSku("SKU-8"); item.setName("商品"); item.setCategoryName("宠物犬");
        item.setTopCategoryName("犬类"); item.setPrice(new BigDecimal("10.00")); item.setStock(0);
        item.setStatus("DRAFT"); item.setSortOrder(0); item.setVersion(2L);
        page.setRecords(List.of(item));
        WebContext context = webContext();
        context.setVariable("page", page); context.setVariable("query", query);
        context.setVariable("categoryOptions", List.of()); context.setVariable("message", null); context.setVariable("error", null);
        context.setVariable("topCategoryOptions", List.of());

        String html = templateEngine().process("product/list", context);

        assertTrue(html.contains("/admin/products/8/edit"));
        assertTrue(html.contains("/admin/products/8/restock"));
        assertTrue(html.contains("/admin/products/8/status"));
        assertTrue(html.contains("name=\"version\" value=\"2\""));
    }

    private ProductCreateForm createForm() {
        ProductCreateForm form = new ProductCreateForm();
        form.setSku("SKU-8"); form.setCategoryId(11L); form.setName("商品");
        form.setCoverImage("images/a.png"); form.setPrice(new BigDecimal("10.00"));
        form.setStock(0); form.setSortOrder(0);
        return form;
    }

    private ProductEditView view() {
        return new ProductEditView(8L, "SKU-8", 11L, "商品", "简介", "描述", "images/a.png",
                new BigDecimal("10.00"), 7, "DRAFT", 0, 2L);
    }

    private ProductListQuery query(Long page, String keyword, String status) {
        ProductListQuery query = new ProductListQuery();
        query.setPage(page); query.setKeyword(keyword); query.setStatus(status);
        return query;
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
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        return engine;
    }
}
