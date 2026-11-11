package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.service.ProductService;
import org.csu.mypetstore.admin.security.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("applicationName", "MyPetStore 管理端");
        model.addAttribute("productCount", productService.countProducts());
        model.addAttribute("adminDisplayName", principal.displayName());
        return "index";
    }
}
