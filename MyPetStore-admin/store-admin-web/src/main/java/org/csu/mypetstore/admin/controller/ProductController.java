package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.model.query.ProductListQuery;
import org.csu.mypetstore.service.ProductQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping("/admin/products")
    public String list(ProductListQuery query, Model model) {
        model.addAttribute("page", productQueryService.findProducts(query));
        model.addAttribute("query", query);
        return "product/list";
    }
}
