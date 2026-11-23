package org.csu.mypetstore.user.controller;

import org.csu.mypetstore.user.service.CatalogService;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;

@Controller
public class CatalogController {
    private final CatalogService catalog;public CatalogController(CatalogService c){catalog=c;}
    @GetMapping({"/","/index"})String index(){return "index";}
    @GetMapping("/main")String main(Model m){m.addAttribute("categories",catalog.categories());m.addAttribute("newest",catalog.newest());m.addAttribute("featured",catalog.featured());return "Main";}
    @GetMapping("/category")String category(@RequestParam(required=false)String keyword,@RequestParam(required=false)Long categoryId,@RequestParam(defaultValue="ALL")String productType,@RequestParam(defaultValue="DEFAULT")String sort,@RequestParam(defaultValue="1")long page,@RequestParam(defaultValue="6")long pageSize,Model m){m.addAttribute("categories",catalog.categories());m.addAttribute("page",catalog.search(keyword,categoryId,productType,sort,page,pageSize));m.addAttribute("keyword",keyword);m.addAttribute("categoryId",categoryId);m.addAttribute("productType",productType);m.addAttribute("sort",sort);m.addAttribute("pageSize",pageSize);return "Category";}
    @GetMapping("/product")String product(@RequestParam long id,Model m){m.addAttribute("product",catalog.detail(id));return "Product";}
}
