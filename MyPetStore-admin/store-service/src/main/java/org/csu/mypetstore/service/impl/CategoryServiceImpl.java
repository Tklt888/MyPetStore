package org.csu.mypetstore.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.dao.CategoryMapper;
import org.csu.mypetstore.model.Category;
import org.csu.mypetstore.model.command.CategoryCreateCommand;
import org.csu.mypetstore.model.command.CategoryUpdateCommand;
import org.csu.mypetstore.model.dto.CategoryEditView;
import org.csu.mypetstore.model.dto.CategoryRow;
import org.csu.mypetstore.model.dto.CategoryTreeItem;
import org.csu.mypetstore.model.query.CategoryListQuery;
import org.csu.mypetstore.service.CategoryService;
import org.csu.mypetstore.service.CategoryNotFoundException;
import org.csu.mypetstore.service.CategoryValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_CODE_LENGTH = 32;
    private static final int MAX_NAME_LENGTH = 50;

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public IPage<CategoryTreeItem> listTree(CategoryListQuery query) {
        IPage<CategoryRow> topLevelPage = categoryMapper.selectTopLevelPage(
                new Page<>(query.getPage(), CategoryListQuery.PAGE_SIZE));
        List<CategoryRow> topLevels = topLevelPage.getRecords();
        List<CategoryTreeItem> treeItems;
        if (topLevels.isEmpty()) {
            treeItems = List.of();
        } else {
            List<Long> parentIds = topLevels.stream().map(CategoryRow::getId).toList();
            Map<Long, List<CategoryRow>> childrenByParent = categoryMapper
                    .selectChildrenByParentIds(parentIds).stream()
                    .collect(Collectors.groupingBy(CategoryRow::getParentId));
            treeItems = topLevels.stream()
                    .map(row -> new CategoryTreeItem(
                            row.getId(), row.getCode(), row.getName(), row.getSortOrder(),
                            childrenByParent.getOrDefault(row.getId(), List.of())))
                    .toList();
        }
        Page<CategoryTreeItem> result = new Page<>(
                topLevelPage.getCurrent(), topLevelPage.getSize(), topLevelPage.getTotal());
        result.setRecords(treeItems);
        return result;
    }

    @Override
    public List<CategoryRow> listTopLevelOptions() {
        return List.copyOf(categoryMapper.selectTopLevelOptions());
    }

    @Override
    public CategoryEditView getForEdit(long id) {
        Category category = findRequired(id);
        String parentName = null;
        if (category.getParentId() != null) {
            Category parent = categoryMapper.selectById(category.getParentId());
            parentName = parent == null ? null : parent.getName();
        }
        return new CategoryEditView(
                category.getId(), category.getCode(), category.getParentId(), parentName,
                category.getName(), category.getSortOrder());
    }

    @Override
    @Transactional
    public long create(CategoryCreateCommand command) {
        String code = requiredText(command.code(), MAX_CODE_LENGTH, "code", "分类编码不能为空", "分类编码不能超过32个字符");
        String name = requiredText(command.name(), MAX_NAME_LENGTH, "name", "分类名称不能为空", "分类名称不能超过50个字符");
        int sortOrder = validSortOrder(command.sortOrder());

        Long parentId = command.parentId();
        if (parentId != null) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null || parent.getParentId() != null) {
                throw new CategoryValidationException("parentId", "父分类必须是存在的一级分类");
            }
        }
        if (categoryMapper.selectByCode(code) != null) {
            throw new CategoryValidationException("code", "分类编码已存在");
        }
        if (categoryMapper.selectByParentAndName(parentId, name) != null) {
            throw new CategoryValidationException("name", "同级分类名称已存在");
        }

        Category category = new Category();
        category.setCode(code);
        category.setParentId(parentId);
        category.setName(name);
        category.setSortOrder(sortOrder);
        try {
            categoryMapper.insert(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryValidationException(null, "分类编码或同级名称已存在");
        }
        return category.getId();
    }

    @Override
    @Transactional
    public void update(long id, CategoryUpdateCommand command) {
        String name = requiredText(command.name(), MAX_NAME_LENGTH, "name",
                "分类名称不能为空", "分类名称不能超过50个字符");
        int sortOrder = validSortOrder(command.sortOrder());
        Category existing = findRequired(id);
        Category duplicate = categoryMapper.selectByParentAndName(existing.getParentId(), name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new CategoryValidationException("name", "同级分类名称已存在");
        }
        try {
            categoryMapper.updateNameAndSort(id, name, sortOrder);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryValidationException("name", "同级分类名称已存在");
        }
    }

    @Override
    @Transactional
    public void delete(long id) {
        findRequired(id);
        if (categoryMapper.countChildren(id) > 0) {
            throw new CategoryValidationException(null, "该分类存在子分类，不能删除");
        }
        if (categoryMapper.countProductReferences(id) > 0) {
            throw new CategoryValidationException(null, "该分类已被商品引用，不能删除");
        }
        try {
            categoryMapper.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryValidationException(null, "分类已被引用，不能删除");
        }
    }

    private String requiredText(String value, int maxLength, String field, String requiredMessage, String lengthMessage) {
        if (value == null) {
            throw new CategoryValidationException(field, requiredMessage);
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new CategoryValidationException(field, requiredMessage);
        }
        if (normalized.length() > maxLength) {
            throw new CategoryValidationException(field, lengthMessage);
        }
        return normalized;
    }

    private int validSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new CategoryValidationException("sortOrder", "排序必须是非负整数");
        }
        return sortOrder;
    }

    private Category findRequired(long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new CategoryNotFoundException(id);
        }
        return category;
    }
}
