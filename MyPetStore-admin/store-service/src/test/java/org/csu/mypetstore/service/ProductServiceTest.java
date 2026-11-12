package org.csu.mypetstore.service;

import org.csu.mypetstore.dao.CategoryMapper;
import org.csu.mypetstore.dao.ProductMapper;
import org.csu.mypetstore.model.Category;
import org.csu.mypetstore.model.Product;
import org.csu.mypetstore.model.command.ProductCreateCommand;
import org.csu.mypetstore.model.command.ProductRestockCommand;
import org.csu.mypetstore.model.command.ProductStatusCommand;
import org.csu.mypetstore.model.command.ProductUpdateCommand;
import org.csu.mypetstore.model.dto.ProductEditView;
import org.csu.mypetstore.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductMapper productMapper;
    private CategoryMapper categoryMapper;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        service = new ProductServiceImpl(productMapper, categoryMapper);
    }

    @Test
    void createNormalizesFieldsAndForcesDraftVersionZero() {
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L));

        service.create(new ProductCreateCommand(" DOG-NEW ", 11L, " 新商品 ", " 简介 ", " 描述 ",
                " images/dog.png ", new BigDecimal("12.30"), 0, 5));

        var captor = org.mockito.ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        Product saved = captor.getValue();
        assertEquals("DOG-NEW", saved.getSku());
        assertEquals("新商品", saved.getName());
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(0L, saved.getVersion());
    }

    @Test
    void createRejectsInvalidTextPriceNumbersAndCoverPaths() {
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L));
        ProductValidationException blank = assertThrows(ProductValidationException.class,
                () -> service.create(create("\u3000", new BigDecimal("1.00"), 0, 0, "images/a.png")));
        ProductValidationException scale = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("1.001"), 0, 0, "images/a.png")));
        ProductValidationException range = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("10000000000.00"), 0, 0, "images/a.png")));
        ProductValidationException negative = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("1.00"), -1, -1, "images/a.png")));
        ProductValidationException url = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("1.00"), 0, 0, "https://x/a.png")));
        ProductValidationException traversal = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("1.00"), 0, 0, "images/../a.png")));
        ProductValidationException scheme = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("1.00"), 0, 0, "javascript:alert(1)")));
        ProductValidationException data = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("1.00"), 0, 0, "data:image/svg+xml,x")));
        ProductValidationException windows = assertThrows(ProductValidationException.class,
                () -> service.create(create("SKU", new BigDecimal("1.00"), 0, 0, "C:\\Users\\x.png")));

        assertEquals("sku", blank.getField());
        assertEquals("price", scale.getField());
        assertEquals("price", range.getField());
        assertEquals("stock", negative.getField());
        assertEquals("coverImage", url.getField());
        assertEquals("coverImage", traversal.getField());
        assertEquals("coverImage", scheme.getField());
        assertEquals("coverImage", data.getField());
        assertEquals("coverImage", windows.getField());
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void createRequiresExistingSecondLevelCategoryAndUniqueSku() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, null));
        assertThrows(ProductValidationException.class, () -> service.create(createForCategory(1L, "SKU")));

        when(categoryMapper.selectById(404L)).thenReturn(null);
        assertThrows(ProductValidationException.class, () -> service.create(createForCategory(404L, "SKU")));

        when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L));
        when(productMapper.selectBySku("SKU")).thenReturn(product(8L, 2L));
        assertThrows(ProductValidationException.class, () -> service.create(createForCategory(11L, "SKU")));
    }

    @Test
    void concurrentDuplicateSkuUsesSafeMessage() {
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L));
        doThrow(new DuplicateKeyException("SQL uk_product_sku")).when(productMapper).insert(any(Product.class));

        ProductValidationException exception = assertThrows(ProductValidationException.class,
                () -> service.create(createForCategory(11L, "SKU")));

        assertEquals("SKU已存在", exception.getMessage());
        assertFalse(exception.getMessage().contains("SQL"));
    }

    @Test
    void descriptionCapacityAndNonDuplicateIntegrityFailureUseAccurateSafeMessages() {
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L));
        String tooManyUtf8Bytes = "犬".repeat(21846);
        ProductCreateCommand oversized = new ProductCreateCommand("SKU", 11L, "名称", null,
                tooManyUtf8Bytes, "images/a.png", new BigDecimal("1.00"), 0, 0);
        ProductValidationException length = assertThrows(ProductValidationException.class,
                () -> service.create(oversized));
        assertEquals("description", length.getField());

        doThrow(new DataIntegrityViolationException("SQL foreign key fk_product_category"))
                .when(productMapper).insert(any(Product.class));
        ProductValidationException integrity = assertThrows(ProductValidationException.class,
                () -> service.create(createForCategory(11L, "SKU")));
        assertEquals("商品资料不符合数据库约束，请刷新后重试", integrity.getMessage());
        assertFalse(integrity.getMessage().contains("SQL"));
        assertFalse(integrity.getMessage().contains("SKU已存在"));
    }

    @Test
    void updateUsesOnlyEditableFieldsAndOldVersion() {
        when(productMapper.selectDetailById(8L)).thenReturn(view(8L, 3L, "DRAFT", 7, 2L));
        when(categoryMapper.selectById(12L)).thenReturn(category(12L, 1L));
        when(productMapper.updateDetails(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        service.update(8L, update(12L, 2L));

        verify(productMapper).updateDetails(8L, 12L, "新名称", "简介", "描述", "images/new.png",
                new BigDecimal("20.00"), 3, 2L);
        verify(productMapper, never()).updateStatus(any(), any(), any());
        verify(productMapper, never()).incrementStock(any(), any());
    }

    @Test
    void staleUpdateStatusAndRestockAllReportConflict() {
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L));
        when(categoryMapper.selectById(12L)).thenReturn(category(12L, 1L));
        when(productMapper.selectDetailById(8L)).thenReturn(view(8L, 3L, "DRAFT", 7, 3L));
        when(productMapper.updateDetails(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(productMapper.updateStatus(8L, "ON_SALE", 2L)).thenReturn(0);

        assertThrows(ProductConflictException.class, () -> service.update(8L, update(12L, 2L)));
        assertThrows(ProductConflictException.class,
                () -> service.changeStatus(8L, new ProductStatusCommand("ON_SALE", 2L)));
    }

    @Test
    void zeroStockProductCanGoOnSale() {
        when(productMapper.selectDetailById(8L)).thenReturn(view(8L, 3L, "DRAFT", 0, 2L));
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L));
        when(productMapper.updateStatus(8L, "ON_SALE", 2L)).thenReturn(1);

        service.changeStatus(8L, new ProductStatusCommand("ON_SALE", 2L));

        verify(productMapper).updateStatus(8L, "ON_SALE", 2L);
    }

    @Test
    void restockRequiresPositiveQuantityAndReturnsDatabaseResult() {
        assertThrows(ProductValidationException.class,
                () -> service.restock(8L, new ProductRestockCommand(0)));

        when(productMapper.selectDetailById(8L))
                .thenReturn(view(8L, 3L, "DRAFT", Integer.MAX_VALUE, 2L));
        when(productMapper.incrementStock(8L, 1)).thenReturn(0);
        assertThrows(ProductValidationException.class,
                () -> service.restock(8L, new ProductRestockCommand(1)));

        ProductEditView updated = view(8L, 3L, "DRAFT", 13, 3L);
        when(productMapper.selectDetailById(8L))
                .thenReturn(view(8L, 3L, "DRAFT", 10, 2L), updated);
        when(productMapper.incrementStock(8L, 3)).thenReturn(1);
        assertEquals(13, service.restock(8L, new ProductRestockCommand(3)).stock());
    }

    @Test
    void missingProductUsesNotFound() {
        when(productMapper.selectDetailById(404L)).thenReturn(null);

        assertThrows(ProductNotFoundException.class, () -> service.getForEdit(404L));
        assertThrows(ProductNotFoundException.class,
                () -> service.changeStatus(404L, new ProductStatusCommand("DRAFT", 0L)));
    }

    private ProductCreateCommand create(String sku, BigDecimal price, int stock, int sort, String cover) {
        return new ProductCreateCommand(sku, 11L, "名称", null, null, cover, price, stock, sort);
    }

    private ProductCreateCommand createForCategory(Long categoryId, String sku) {
        return new ProductCreateCommand(sku, categoryId, "名称", null, null, "images/a.png",
                new BigDecimal("1.00"), 0, 0);
    }

    private ProductUpdateCommand update(Long categoryId, Long version) {
        return new ProductUpdateCommand(categoryId, "新名称", "简介", "描述", "images/new.png",
                new BigDecimal("20.00"), 3, version);
    }

    private Category category(Long id, Long parentId) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        return category;
    }

    private Product product(Long id, Long version) {
        Product product = new Product();
        product.setId(id);
        product.setVersion(version);
        return product;
    }

    private ProductEditView view(Long id, Long categoryId, String status, int stock, Long version) {
        return new ProductEditView(id, "SKU", 11L, "名称", null, null, "images/a.png",
                new BigDecimal("10.00"), stock, status, 0, version);
    }
}
