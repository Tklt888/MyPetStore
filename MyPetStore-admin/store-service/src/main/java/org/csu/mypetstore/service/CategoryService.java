package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.csu.mypetstore.model.command.CategoryCreateCommand;
import org.csu.mypetstore.model.command.CategoryUpdateCommand;
import org.csu.mypetstore.model.dto.CategoryEditView;
import org.csu.mypetstore.model.dto.CategoryRow;
import org.csu.mypetstore.model.dto.CategoryTreeItem;
import org.csu.mypetstore.model.query.CategoryListQuery;

import java.util.List;

public interface CategoryService {

    IPage<CategoryTreeItem> listTree(CategoryListQuery query);

    List<CategoryRow> listTopLevelOptions();

    CategoryEditView getForEdit(long id);

    long create(CategoryCreateCommand command);

    void update(long id, CategoryUpdateCommand command);

    void delete(long id);
}
