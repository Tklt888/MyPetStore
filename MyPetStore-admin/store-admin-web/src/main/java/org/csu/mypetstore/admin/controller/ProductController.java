package org.csu.mypetstore.admin.controller;

import jakarta.validation.Valid;
import org.csu.mypetstore.admin.form.ProductCreateForm;
import org.csu.mypetstore.admin.form.ProductRestockForm;
import org.csu.mypetstore.admin.form.ProductUpdateForm;
import org.csu.mypetstore.model.command.ProductCreateCommand;
import org.csu.mypetstore.model.command.ProductRestockCommand;
import org.csu.mypetstore.model.command.ProductStatusCommand;
import org.csu.mypetstore.model.command.ProductUpdateCommand;
import org.csu.mypetstore.model.dto.ProductEditView;
import org.csu.mypetstore.model.query.ProductListQuery;
import org.csu.mypetstore.service.ProductConflictException;
import org.csu.mypetstore.service.ProductQueryService;
import org.csu.mypetstore.service.ProductService;
import org.csu.mypetstore.service.ProductValidationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {
    private final ProductQueryService queryService;
    private final ProductService productService;

    public ProductController(ProductQueryService queryService, ProductService productService) {
        this.queryService = queryService;
        this.productService = productService;
    }

    @GetMapping("/admin/products")
    public String list(ProductListQuery query, Model model) {
        model.addAttribute("page", queryService.findProducts(query));
        model.addAttribute("query", query);
        model.addAttribute("categoryOptions", productService.listCategoryOptions());
        return "product/list";
    }

    @GetMapping("/admin/products/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ProductCreateForm());
        prepareForm(model, false, null);
        return "product/form";
    }

    @PostMapping("/admin/products")
    public String create(@Valid @ModelAttribute("form") ProductCreateForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { productService.create(toCommand(form)); }
            catch (ProductValidationException exception) { reject(errors, exception); }
        }
        if (errors.hasErrors()) {
            model.addAttribute("errorMessage", firstError(errors));
            prepareForm(model, false, null);
            return "product/form";
        }
        redirect.addFlashAttribute("message", "商品已创建，当前为下架状态");
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/products/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        ProductEditView product = productService.getForEdit(id);
        model.addAttribute("form", updateForm(product));
        prepareForm(model, true, product);
        return "product/form";
    }

    @PostMapping("/admin/products/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") ProductUpdateForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { productService.update(id, toCommand(form)); }
            catch (ProductValidationException exception) { reject(errors, exception); }
            catch (ProductConflictException exception) { errors.reject("product.conflict", exception.getMessage()); }
        }
        if (errors.hasErrors()) {
            ProductEditView current = productService.getForEdit(id);
            model.addAttribute("errorMessage", firstError(errors));
            prepareForm(model, true, current);
            return "product/form";
        }
        redirect.addFlashAttribute("message", "商品资料已更新");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/status")
    public String changeStatus(@PathVariable long id, @RequestParam String status, @RequestParam Long version,
                               RedirectAttributes redirect) {
        try {
            productService.changeStatus(id, new ProductStatusCommand(status, version));
            redirect.addFlashAttribute("message", "ON_SALE".equals(status) ? "商品已上架" : "商品已下架");
        } catch (ProductValidationException | ProductConflictException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/products/{id}/restock")
    public String restockForm(@PathVariable long id, Model model) {
        ProductEditView product = productService.getForEdit(id);
        ProductRestockForm form = new ProductRestockForm();
        form.setVersion(product.version());
        model.addAttribute("form", form);
        model.addAttribute("product", product);
        return "product/restock";
    }

    @PostMapping("/admin/products/{id}/restock")
    public String restock(@PathVariable long id, @Valid @ModelAttribute("form") ProductRestockForm form,
                          BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { productService.restock(id, new ProductRestockCommand(form.getQuantity(), form.getVersion())); }
            catch (ProductValidationException exception) { reject(errors, exception); }
            catch (ProductConflictException exception) { errors.reject("product.conflict", exception.getMessage()); }
        }
        if (errors.hasErrors()) {
            model.addAttribute("product", productService.getForEdit(id));
            model.addAttribute("errorMessage", firstError(errors));
            return "product/restock";
        }
        redirect.addFlashAttribute("message", "补货成功");
        return "redirect:/admin/products";
    }

    private ProductCreateCommand toCommand(ProductCreateForm f) {
        return new ProductCreateCommand(f.getSku(), f.getCategoryId(), f.getName(), f.getSummary(), f.getDescription(),
                f.getCoverImage(), f.getPrice(), f.getStock(), f.getSortOrder());
    }
    private ProductUpdateCommand toCommand(ProductUpdateForm f) {
        return new ProductUpdateCommand(f.getCategoryId(), f.getName(), f.getSummary(), f.getDescription(),
                f.getCoverImage(), f.getPrice(), f.getSortOrder(), f.getVersion());
    }
    private ProductUpdateForm updateForm(ProductEditView p) {
        ProductUpdateForm f = new ProductUpdateForm();
        f.setCategoryId(p.categoryId()); f.setName(p.name()); f.setSummary(p.summary());
        f.setDescription(p.description()); f.setCoverImage(p.coverImage()); f.setPrice(p.price());
        f.setSortOrder(p.sortOrder()); f.setVersion(p.version()); return f;
    }
    private void prepareForm(Model model, boolean editing, ProductEditView product) {
        model.addAttribute("editing", editing);
        model.addAttribute("categoryOptions", productService.listCategoryOptions());
        if (product != null) model.addAttribute("product", product);
    }
    private void reject(BindingResult errors, ProductValidationException e) {
        if (e.getField() == null) errors.reject("product.invalid", e.getMessage());
        else errors.rejectValue(e.getField(), "product.invalid", e.getMessage());
    }
    private String firstError(BindingResult errors) {
        return errors.getAllErrors().isEmpty() ? "请检查表单内容" : errors.getAllErrors().getFirst().getDefaultMessage();
    }
}
