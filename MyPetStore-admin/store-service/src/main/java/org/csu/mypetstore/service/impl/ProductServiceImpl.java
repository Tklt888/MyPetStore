package org.csu.mypetstore.service.impl;

import org.csu.mypetstore.dao.CategoryMapper;
import org.csu.mypetstore.dao.ProductMapper;
import org.csu.mypetstore.model.Category;
import org.csu.mypetstore.model.Product;
import org.csu.mypetstore.model.command.ProductCreateCommand;
import org.csu.mypetstore.model.command.ProductRestockCommand;
import org.csu.mypetstore.model.command.ProductStatusCommand;
import org.csu.mypetstore.model.command.ProductUpdateCommand;
import org.csu.mypetstore.model.dto.ProductCategoryOption;
import org.csu.mypetstore.model.dto.ProductEditView;
import org.csu.mypetstore.service.ProductConflictException;
import org.csu.mypetstore.service.ProductNotFoundException;
import org.csu.mypetstore.service.ProductService;
import org.csu.mypetstore.service.ProductValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Service
public class ProductServiceImpl implements ProductService {

    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");
    private static final Set<String> STATUSES = Set.of("DRAFT", "ON_SALE");

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public ProductServiceImpl(ProductMapper productMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public long countProducts() { return productMapper.selectCount(null); }

    @Override
    public List<ProductCategoryOption> listCategoryOptions() {
        return List.copyOf(productMapper.selectCategoryOptions());
    }

    @Override
    public ProductEditView getForEdit(long id) { return findRequired(id); }

    @Override
    @Transactional
    public long create(ProductCreateCommand command) {
        String sku = required(command.sku(), 40, "sku", "SKU不能为空", "SKU不能超过40个字符");
        Long categoryId = validCategory(command.categoryId());
        String name = required(command.name(), 120, "name", "商品名称不能为空", "商品名称不能超过120个字符");
        String summary = optional(command.summary(), 500, "summary", "简介不能超过500个字符");
        String description = validDescription(command.description());
        String cover = validCover(command.coverImage());
        BigDecimal price = validPrice(command.price());
        int stock = nonNegative(command.stock(), "stock", "初始库存必须是非负整数");
        int sort = nonNegative(command.sortOrder(), "sortOrder", "排序必须是非负整数");
        if (productMapper.selectBySku(sku) != null) {
            throw new ProductValidationException("sku", "SKU已存在");
        }
        Product product = new Product();
        product.setSku(sku);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setSummary(summary);
        product.setDescription(description);
        product.setCoverImage(cover);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus("DRAFT");
        product.setSortOrder(sort);
        product.setVersion(0L);
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException exception) {
            throw new ProductValidationException("sku", "SKU已存在");
        } catch (DataIntegrityViolationException exception) {
            throw new ProductValidationException(null, "商品资料不符合数据库约束，请刷新后重试");
        }
        return product.getId() == null ? 0L : product.getId();
    }

    @Override
    @Transactional
    public void update(long id, ProductUpdateCommand command) {
        findRequired(id);
        Long categoryId = validCategory(command.categoryId());
        String name = required(command.name(), 120, "name", "商品名称不能为空", "商品名称不能超过120个字符");
        String summary = optional(command.summary(), 500, "summary", "简介不能超过500个字符");
        String description = validDescription(command.description());
        String cover = validCover(command.coverImage());
        BigDecimal price = validPrice(command.price());
        int sort = nonNegative(command.sortOrder(), "sortOrder", "排序必须是非负整数");
        long version = validVersion(command.version());
        try {
            int changed = productMapper.updateDetails(id, categoryId, name, summary, description, cover, price, sort, version);
            requireChanged(id, changed);
        } catch (DataIntegrityViolationException exception) {
            throw new ProductValidationException(null, "商品资料不符合数据库约束");
        }
    }

    @Override
    @Transactional
    public void changeStatus(long id, ProductStatusCommand command) {
        ProductEditView current = findRequired(id);
        String status = command.status();
        if (!STATUSES.contains(status)) {
            throw new ProductValidationException("status", "商品状态不合法");
        }
        long version = validVersion(command.version());
        if ("ON_SALE".equals(status)) {
            validCategory(current.categoryId());
            validPrice(current.price());
            validCover(current.coverImage());
        }
        requireChanged(id, productMapper.updateStatus(id, status, version));
    }

    @Override
    @Transactional
    public ProductEditView restock(long id, ProductRestockCommand command) {
        Integer quantity = command.quantity();
        if (quantity == null || quantity <= 0) {
            throw new ProductValidationException("quantity", "补货数量必须是正整数");
        }
        ProductEditView before = findRequired(id);
        int changed = productMapper.incrementStock(id, quantity);
        if (changed == 0) {
            ProductEditView current = productMapper.selectDetailById(id);
            if (current == null) throw new ProductNotFoundException(id);
            if (before.stock() > Integer.MAX_VALUE - quantity) {
                throw new ProductValidationException("quantity", "补货后库存超出允许范围");
            }
            throw new ProductValidationException(null, "补货失败，请刷新后重试");
        }
        return findRequired(id);
    }

    private void requireChanged(long id, int changed) {
        if (changed > 0) return;
        if (productMapper.selectDetailById(id) == null) throw new ProductNotFoundException(id);
        throw new ProductConflictException();
    }

    private ProductEditView findRequired(long id) {
        ProductEditView view = productMapper.selectDetailById(id);
        if (view == null) throw new ProductNotFoundException(id);
        return view;
    }

    private Long validCategory(Long categoryId) {
        if (categoryId == null) throw new ProductValidationException("categoryId", "请选择二级分类");
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getParentId() == null) {
            throw new ProductValidationException("categoryId", "商品必须归属存在的二级分类");
        }
        return categoryId;
    }

    private String required(String value, int max, String field, String emptyMessage, String longMessage) {
        if (value == null || value.strip().isEmpty()) throw new ProductValidationException(field, emptyMessage);
        String result = value.strip();
        if (result.length() > max) throw new ProductValidationException(field, longMessage);
        return result;
    }

    private String optional(String value, int max, String field, String longMessage) {
        if (value == null || value.strip().isEmpty()) return null;
        String result = value.strip();
        if (result.length() > max) throw new ProductValidationException(field, longMessage);
        return result;
    }

    private String validCover(String value) {
        String cover = required(value, 255, "coverImage", "封面路径不能为空", "封面路径不能超过255个字符");
        if (!cover.matches("[A-Za-z0-9][A-Za-z0-9/_\\-.]*") || cover.contains("..") || cover.contains("\\")) {
            throw new ProductValidationException("coverImage", "封面必须是安全的站内相对路径");
        }
        return cover;
    }

    private String validDescription(String value) {
        String description = optional(value, Integer.MAX_VALUE, "description", "描述过长");
        if (description != null && description.getBytes(StandardCharsets.UTF_8).length > 65_535) {
            throw new ProductValidationException("description", "描述不能超过65535字节");
        }
        return description;
    }

    private BigDecimal validPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0 || price.scale() > 2 || price.compareTo(MAX_PRICE) > 0) {
            throw new ProductValidationException("price", "价格必须大于0、最多两位小数且不超过9999999999.99");
        }
        return price;
    }

    private int nonNegative(Integer value, String field, String message) {
        if (value == null || value < 0) throw new ProductValidationException(field, message);
        return value;
    }

    private long validVersion(Long version) {
        if (version == null || version < 0) throw new ProductValidationException("version", "商品版本不合法");
        return version;
    }
}
