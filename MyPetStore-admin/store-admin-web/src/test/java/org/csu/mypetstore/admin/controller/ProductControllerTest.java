package org.csu.mypetstore.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.model.dto.ProductListItem;
import org.csu.mypetstore.model.query.ProductListQuery;
import org.csu.mypetstore.service.ProductQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductControllerTest {

    @Test
    void listAddsPagedResultsAndOriginalFiltersToModel() {
        ProductListQuery query = query(2L, "cat", "ON_SALE");
        Page<ProductListItem> result = new Page<>(2, 10, 21);
        result.setRecords(List.of(product(1L, "CAT-001", "短毛猫")));
        AtomicReference<ProductListQuery> received = new AtomicReference<>();
        ProductQueryService service = request -> {
            received.set(request);
            return result;
        };
        ProductController controller = new ProductController(service);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.list(query, model);

        assertEquals("product/list", view);
        assertSame(query, received.get());
        assertSame(result, model.get("page"));
        assertSame(query, model.get("query"));
    }

    @Test
    void emptyListRendersEmptyStateAndPreservesFiltersInPagingLinks() {
        ProductListQuery query = query(2L, "cat", "ON_SALE");
        Page<ProductListItem> emptyPage = new Page<>(2, 10, 21);
        emptyPage.setRecords(List.of());
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(
                JakartaServletWebApplication.buildApplication(servletContext)
                        .buildExchange(request, response));
        context.setVariable("page", emptyPage);
        context.setVariable("query", query);

        String html = templateEngine().process("product/list", context);

        assertTrue(html.contains("没有找到符合条件的商品"));
        assertTrue(html.contains("keyword=cat"));
        assertTrue(html.contains("status=ON_SALE"));
    }

    private ProductListQuery query(Long page, String keyword, String status) {
        ProductListQuery query = new ProductListQuery();
        query.setPage(page);
        query.setKeyword(keyword);
        query.setStatus(status);
        return query;
    }

    private ProductListItem product(Long id, String sku, String name) {
        ProductListItem item = new ProductListItem();
        item.setId(id);
        item.setSku(sku);
        item.setName(name);
        return item;
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
