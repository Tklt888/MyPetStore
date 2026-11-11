package org.csu.mypetstore.service;

import org.csu.mypetstore.model.command.ProductCreateCommand;
import org.csu.mypetstore.model.command.ProductRestockCommand;
import org.csu.mypetstore.model.command.ProductStatusCommand;
import org.csu.mypetstore.model.command.ProductUpdateCommand;
import org.csu.mypetstore.model.dto.ProductCategoryOption;
import org.csu.mypetstore.model.dto.ProductEditView;

import java.util.List;

public interface ProductService {
    long countProducts();
    List<ProductCategoryOption> listCategoryOptions();
    ProductEditView getForEdit(long id);
    long create(ProductCreateCommand command);
    void update(long id, ProductUpdateCommand command);
    void changeStatus(long id, ProductStatusCommand command);
    ProductEditView restock(long id, ProductRestockCommand command);
}
