package org.csu.mypetstore.admin;

import org.csu.mypetstore.admin.controller.HomeController;
import org.csu.mypetstore.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminApplicationTest {

    @Test
    void homePageRendersAdminApplicationNameAndProductCount() {
        ProductService productService = () -> 40L;
        HomeController controller = new HomeController(productService);
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = controller.index(model);

        assertEquals("index", viewName);
        assertEquals("MyPetStore 管理端", model.get("applicationName"));
        assertEquals(40L, model.get("productCount"));
    }
}
