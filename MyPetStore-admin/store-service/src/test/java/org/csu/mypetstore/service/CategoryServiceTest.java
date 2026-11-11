package org.csu.mypetstore.service;

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
import org.csu.mypetstore.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private CategoryMapper mapper;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CategoryMapper.class);
        service = new CategoryServiceImpl(mapper);
    }

    @Test
    void createNormalizesAndInsertsTopLevelCategory() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Category.class).setId(9L);
            return 1;
        }).when(mapper).insert(any(Category.class));

        long id = service.create(new CategoryCreateCommand("\u3000SMALL_PETS\u3000", "  小宠  ", 3, null));

        ArgumentCaptor<Category> category = ArgumentCaptor.forClass(Category.class);
        verify(mapper).insert(category.capture());
        assertEquals(9L, id);
        assertEquals("SMALL_PETS", category.getValue().getCode());
        assertEquals("小宠", category.getValue().getName());
        assertEquals(3, category.getValue().getSortOrder());
        assertEquals(null, category.getValue().getParentId());
    }

    @Test
    void createAcceptsOnlyExistingTopLevelParent() {
        Category parent = category(1L, null, "DOGS", "犬类", 10);
        when(mapper.selectById(1L)).thenReturn(parent);
        doAnswer(invocation -> {
            invocation.getArgument(0, Category.class).setId(101L);
            return 1;
        }).when(mapper).insert(any(Category.class));

        long id = service.create(new CategoryCreateCommand("DOG_TOYS", "玩具", 20, 1L));

        ArgumentCaptor<Category> category = ArgumentCaptor.forClass(Category.class);
        verify(mapper).insert(category.capture());
        assertEquals(101L, id);
        assertEquals(1L, category.getValue().getParentId());
    }

    @Test
    void invalidFieldsAreRejectedBeforeDatabaseAccess() {
        List<CategoryCreateCommand> invalid = List.of(
                new CategoryCreateCommand(null, "名称", 0, null),
                new CategoryCreateCommand("\u3000\u3000", "名称", 0, null),
                new CategoryCreateCommand("x".repeat(33), "名称", 0, null),
                new CategoryCreateCommand("CODE", "\u3000", 0, null),
                new CategoryCreateCommand("CODE", "名".repeat(51), 0, null),
                new CategoryCreateCommand("CODE", "名称", -1, null));

        for (CategoryCreateCommand command : invalid) {
            assertThrows(CategoryValidationException.class, () -> service.create(command));
        }

        verifyNoInteractions(mapper);
    }

    @Test
    void missingOrSecondLevelParentIsRejectedWithoutInsert() {
        when(mapper.selectById(99L)).thenReturn(null);
        when(mapper.selectById(101L)).thenReturn(category(101L, 1L, "DOG_TOYS", "玩具", 10));

        CategoryValidationException missing = assertThrows(CategoryValidationException.class,
                () -> service.create(new CategoryCreateCommand("NEW_ONE", "新分类", 0, 99L)));
        CategoryValidationException thirdLevel = assertThrows(CategoryValidationException.class,
                () -> service.create(new CategoryCreateCommand("THIRD", "第三级", 0, 101L)));

        assertEquals("parentId", missing.getField());
        assertEquals("parentId", thirdLevel.getField());
        verify(mapper, never()).insert(any(Category.class));
    }

    @Test
    void duplicateCodeAndSiblingNameUseFieldErrors() {
        when(mapper.selectByCode("DOGS")).thenReturn(category(1L, null, "DOGS", "犬类", 10));
        CategoryValidationException duplicateCode = assertThrows(CategoryValidationException.class,
                () -> service.create(new CategoryCreateCommand("DOGS", "犬科", 0, null)));

        when(mapper.selectByCode("CANINES")).thenReturn(null);
        when(mapper.selectByParentAndName(null, "犬类"))
                .thenReturn(category(1L, null, "DOGS", "犬类", 10));
        CategoryValidationException duplicateName = assertThrows(CategoryValidationException.class,
                () -> service.create(new CategoryCreateCommand("CANINES", "犬类", 0, null)));

        assertEquals("code", duplicateCode.getField());
        assertEquals("name", duplicateName.getField());
        verify(mapper, never()).insert(any(Category.class));
    }

    @Test
    void concurrentCreateConstraintFailureUsesSafeMessage() {
        doThrow(new DuplicateKeyException("SQL: duplicate uk_category_code"))
                .when(mapper).insert(any(Category.class));

        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
                () -> service.create(new CategoryCreateCommand("DOGS", "犬类", 0, null)));

        assertEquals("分类编码或同级名称已存在", exception.getMessage());
        assertFalse(exception.getMessage().contains("SQL"));
    }

    @Test
    void listTreeLoadsChildrenOnlyForTopLevelsOnCurrentPage() {
        CategoryRow dogs = row(1L, null, "DOGS", "犬类", 10);
        CategoryRow cats = row(2L, null, "CATS", "猫类", 20);
        Page<CategoryRow> topPage = new Page<>(2, 10, 21);
        topPage.setRecords(List.of(dogs, cats));
        when(mapper.selectTopLevelPage(any(Page.class))).thenReturn(topPage);
        CategoryRow pets = row(101L, 1L, "DOGS_PETS", "宠物", 10);
        when(mapper.selectChildrenByParentIds(List.of(1L, 2L))).thenReturn(List.of(pets));
        CategoryListQuery query = new CategoryListQuery();
        query.setPage(2L);

        IPage<CategoryTreeItem> result = service.listTree(query);

        assertEquals(2L, result.getCurrent());
        assertEquals(21L, result.getTotal());
        assertEquals(List.of(101L), result.getRecords().getFirst().children().stream()
                .map(CategoryRow::getId).toList());
        assertEquals(List.of(), result.getRecords().get(1).children());
        verify(mapper).selectChildrenByParentIds(List.of(1L, 2L));
    }

    @Test
    void emptyTreePageDoesNotQueryChildren() {
        Page<CategoryRow> empty = new Page<>(4, 10, 21);
        empty.setRecords(List.of());
        when(mapper.selectTopLevelPage(any(Page.class))).thenReturn(empty);

        IPage<CategoryTreeItem> result = service.listTree(new CategoryListQuery());

        assertEquals(List.of(), result.getRecords());
        verify(mapper, never()).selectChildrenByParentIds(any());
    }

    @Test
    void topLevelOptionsComeFromStableMapperQuery() {
        List<CategoryRow> options = List.of(row(1L, null, "DOGS", "犬类", 10));
        when(mapper.selectTopLevelOptions()).thenReturn(options);

        assertEquals(options, service.listTopLevelOptions());
    }

    @Test
    void editViewIncludesImmutableCodeAndParentName() {
        when(mapper.selectById(101L)).thenReturn(category(101L, 1L, "DOGS_PETS", "宠物", 10));
        when(mapper.selectById(1L)).thenReturn(category(1L, null, "DOGS", "犬类", 10));

        CategoryEditView view = service.getForEdit(101L);

        assertEquals("DOGS_PETS", view.code());
        assertEquals(1L, view.parentId());
        assertEquals("犬类", view.parentName());
    }

    @Test
    void updateChangesOnlyNormalizedNameAndSortOrder() {
        Category existing = category(101L, 1L, "DOGS_PETS", "宠物", 10);
        when(mapper.selectById(101L)).thenReturn(existing);
        when(mapper.selectByParentAndName(1L, "犬只"))
                .thenReturn(category(101L, 1L, "DOGS_PETS", "犬只", 10));

        service.update(101L, new CategoryUpdateCommand("\u3000犬只\u3000", 30));

        verify(mapper).updateNameAndSort(101L, "犬只", 30);
    }

    @Test
    void updateRejectsSiblingNameButAllowsCurrentCategory() {
        Category existing = category(101L, 1L, "DOGS_PETS", "宠物", 10);
        when(mapper.selectById(101L)).thenReturn(existing);
        when(mapper.selectByParentAndName(1L, "用品"))
                .thenReturn(category(102L, 1L, "DOGS_SUPPLIES", "用品", 20));

        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
                () -> service.update(101L, new CategoryUpdateCommand("用品", 10)));

        assertEquals("name", exception.getField());
        verify(mapper, never()).updateNameAndSort(any(), any(), any());
    }

    @Test
    void concurrentUpdateConstraintFailureUsesSafeMessage() {
        when(mapper.selectById(101L)).thenReturn(category(101L, 1L, "DOGS_PETS", "宠物", 10));
        doThrow(new DataIntegrityViolationException("SQL: sibling duplicate"))
                .when(mapper).updateNameAndSort(101L, "用品", 10);

        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
                () -> service.update(101L, new CategoryUpdateCommand("用品", 10)));

        assertEquals("同级分类名称已存在", exception.getMessage());
        assertFalse(exception.getMessage().contains("SQL"));
    }

    @Test
    void missingCategoryIsReportedForEditUpdateAndDelete() {
        when(mapper.selectById(404L)).thenReturn(null);

        assertThrows(CategoryNotFoundException.class, () -> service.getForEdit(404L));
        assertThrows(CategoryNotFoundException.class,
                () -> service.update(404L, new CategoryUpdateCommand("名称", 0)));
        assertThrows(CategoryNotFoundException.class, () -> service.delete(404L));
    }

    @Test
    void deleteRejectsChildrenAndProductReferences() {
        when(mapper.selectById(1L)).thenReturn(category(1L, null, "DOGS", "犬类", 10));
        when(mapper.countChildren(1L)).thenReturn(2L);
        CategoryValidationException children = assertThrows(CategoryValidationException.class,
                () -> service.delete(1L));

        when(mapper.selectById(101L)).thenReturn(category(101L, 1L, "DOGS_PETS", "宠物", 10));
        when(mapper.countChildren(101L)).thenReturn(0L);
        when(mapper.countProductReferences(101L)).thenReturn(4L);
        CategoryValidationException products = assertThrows(CategoryValidationException.class,
                () -> service.delete(101L));

        assertEquals("该分类存在子分类，不能删除", children.getMessage());
        assertEquals("该分类已被商品引用，不能删除", products.getMessage());
        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteRemovesUnreferencedCategoryOnce() {
        when(mapper.selectById(900L)).thenReturn(category(900L, null, "TEMP", "临时", 0));
        when(mapper.countChildren(900L)).thenReturn(0L);
        when(mapper.countProductReferences(900L)).thenReturn(0L);

        service.delete(900L);

        verify(mapper).deleteById(900L);
    }

    @Test
    void concurrentDeleteReferenceFailureUsesSafeMessage() {
        when(mapper.selectById(900L)).thenReturn(category(900L, null, "TEMP", "临时", 0));
        when(mapper.countChildren(900L)).thenReturn(0L);
        when(mapper.countProductReferences(900L)).thenReturn(0L);
        doThrow(new DataIntegrityViolationException("SQL: foreign key violation"))
                .when(mapper).deleteById(900L);

        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
                () -> service.delete(900L));

        assertEquals("分类已被引用，不能删除", exception.getMessage());
        assertFalse(exception.getMessage().contains("SQL"));
    }

    private Category category(Long id, Long parentId, String code, String name, int sortOrder) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setCode(code);
        category.setName(name);
        category.setSortOrder(sortOrder);
        return category;
    }

    private CategoryRow row(Long id, Long parentId, String code, String name, int sortOrder) {
        CategoryRow row = new CategoryRow();
        row.setId(id);
        row.setParentId(parentId);
        row.setCode(code);
        row.setName(name);
        row.setSortOrder(sortOrder);
        return row;
    }
}
