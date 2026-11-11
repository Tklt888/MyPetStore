package org.csu.mypetstore.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.model.dto.CategoryEditView;
import org.csu.mypetstore.model.dto.CategoryRow;
import org.csu.mypetstore.model.dto.CategoryTreeItem;
import org.csu.mypetstore.model.query.CategoryListQuery;
import org.csu.mypetstore.service.CategoryNotFoundException;
import org.csu.mypetstore.service.CategoryService;
import org.csu.mypetstore.service.CategoryValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class CategoryControllerTest {

    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(categoryService))
                .setControllerAdvice(new CategoryExceptionHandler())
                .build();
    }

    @Test
    void listAddsPagedTreeAndQueryToModel() throws Exception {
        Page<CategoryTreeItem> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(new CategoryTreeItem(1L, "DOGS", "犬类", 10, List.of())));
        when(categoryService.listTree(any(CategoryListQuery.class))).thenReturn(page);

        mockMvc.perform(get("/admin/categories").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/list"))
                .andExpect(model().attribute("page", page))
                .andExpect(model().attributeExists("query"));
    }

    @Test
    void newChildFormUsesOnlyExistingTopLevelParent() throws Exception {
        CategoryRow dogs = row(1L, "DOGS", "犬类");
        when(categoryService.listTopLevelOptions()).thenReturn(List.of(dogs));

        mockMvc.perform(get("/admin/categories/new").param("parentId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/form"))
                .andExpect(model().attribute("parentName", "犬类"))
                .andExpect(model().attributeExists("form", "topLevelOptions"));

        mockMvc.perform(get("/admin/categories/new").param("parentId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void createUsesPostRedirectGetAndFlashMessage() throws Exception {
        when(categoryService.create(any())).thenReturn(9L);

        mockMvc.perform(post("/admin/categories")
                        .param("code", "SMALL_PETS")
                        .param("name", "小宠")
                        .param("sortOrder", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("message", "分类已创建"));

        verify(categoryService).create(any());
    }

    @Test
    void createValidationFailureReturnsFormAndPreservesInput() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .param("code", "")
                        .param("name", "小宠")
                        .param("sortOrder", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/form"))
                .andExpect(model().attributeHasFieldErrors("form", "code", "sortOrder"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void serviceFieldErrorReturnsCreateForm() throws Exception {
        when(categoryService.create(any()))
                .thenThrow(new CategoryValidationException("code", "分类编码已存在"));

        mockMvc.perform(post("/admin/categories")
                        .param("code", "DOGS")
                        .param("name", "犬科")
                        .param("sortOrder", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/form"))
                .andExpect(model().attributeHasFieldErrorCode("form", "code", "category.invalid"));
    }

    @Test
    void editNotFoundReturns404() throws Exception {
        when(categoryService.getForEdit(404L)).thenThrow(new CategoryNotFoundException(404L));

        mockMvc.perform(get("/admin/categories/404/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void updateUsesOnlyNameAndSortThenRedirects() throws Exception {
        mockMvc.perform(post("/admin/categories/101")
                        .param("name", "犬只")
                        .param("sortOrder", "30")
                        .param("code", "TAMPERED")
                        .param("parentId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("message", "分类已更新"));

        verify(categoryService).update(any(Long.class), any());
    }

    @Test
    void deleteBusinessRejectionRedirectsWithSafeMessage() throws Exception {
        doThrow(new CategoryValidationException(null, "该分类存在子分类，不能删除"))
                .when(categoryService).delete(1L);

        mockMvc.perform(post("/admin/categories/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("error", "该分类存在子分类，不能删除"));
    }

    @Test
    void listTemplateShowsTreePaginationAndProtectedWriteForms() {
        CategoryRow child = row(101L, "DOGS_PETS", "宠物");
        child.setParentId(1L);
        Page<CategoryTreeItem> page = new Page<>(1, 10, 11);
        page.setRecords(List.of(new CategoryTreeItem(1L, "DOGS", "犬类", 10, List.of(child))));
        WebContext context = webContext();
        context.setVariable("page", page);
        context.setVariable("query", new CategoryListQuery());
        context.setVariable("message", "分类已创建");
        context.setVariable("error", null);
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("category/list", context);

        assertTrue(html.contains("犬类"));
        assertTrue(html.contains("DOGS_PETS"));
        assertTrue(html.contains("宠物"));
        assertTrue(html.contains("/admin/categories/new?parentId=1"));
        assertTrue(html.contains("action=\"/admin/categories/1/delete\""));
        assertTrue(html.contains("method=\"post\""));
        assertTrue(html.contains("name=\"_csrf\""));
        assertTrue(html.contains("token-123"));
        assertTrue(html.contains("confirm("));
        assertTrue(html.contains("page=2"));
    }

    @Test
    void outOfRangeEmptyPageLinksDirectlyToValidPages() {
        Page<CategoryTreeItem> page = new Page<>(999, 10, 11);
        page.setRecords(List.of());
        WebContext context = webContext();
        context.setVariable("page", page);
        context.setVariable("query", new CategoryListQuery());
        context.setVariable("message", null);
        context.setVariable("error", null);
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("category/list", context);

        assertTrue(html.contains("href=\"/admin/categories?page=1\""));
        assertTrue(html.contains("href=\"/admin/categories?page=2\""));
    }

    @Test
    void editTemplateKeepsCodeAndParentReadOnly() {
        WebContext context = webContext();
        context.setVariable("editing", true);
        context.setVariable("category",
                new CategoryEditView(101L, "DOGS_PETS", 1L, "犬类", "宠物", 10));
        org.csu.mypetstore.admin.form.CategoryUpdateForm form =
                new org.csu.mypetstore.admin.form.CategoryUpdateForm();
        form.setName("宠物");
        form.setSortOrder(10);
        context.setVariable("form", form);
        context.setVariable("parentName", "犬类");
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("category/form", context);

        assertTrue(html.contains("DOGS_PETS"));
        assertTrue(html.contains("犬类"));
        assertTrue(html.contains("action=\"/admin/categories/101\""));
        assertFalse(html.contains("name=\"code\""));
        assertFalse(html.contains("name=\"parentId\""));
        assertTrue(html.contains("name=\"_csrf\""));
    }

    @Test
    void createTemplatePostsEditableFieldsWithCsrf() {
        WebContext context = webContext();
        org.csu.mypetstore.admin.form.CategoryCreateForm form =
                new org.csu.mypetstore.admin.form.CategoryCreateForm();
        form.setSortOrder(0);
        context.setVariable("editing", false);
        context.setVariable("form", form);
        context.setVariable("parentName", null);
        context.setVariable("topLevelOptions", List.of());
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));

        String html = templateEngine().process("category/form", context);

        assertTrue(html.contains("action=\"/admin/categories\""));
        assertTrue(html.contains("method=\"post\""));
        assertTrue(html.contains("name=\"code\""));
        assertTrue(html.contains("name=\"name\""));
        assertTrue(html.contains("name=\"sortOrder\""));
        assertTrue(html.contains("name=\"_csrf\""));
    }

    private CategoryRow row(Long id, String code, String name) {
        CategoryRow row = new CategoryRow();
        row.setId(id);
        row.setCode(code);
        row.setName(name);
        row.setSortOrder(10);
        return row;
    }

    private WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        return new WebContext(JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(request, response));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
