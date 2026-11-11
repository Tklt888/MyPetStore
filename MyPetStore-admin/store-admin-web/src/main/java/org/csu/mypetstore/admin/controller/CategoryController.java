package org.csu.mypetstore.admin.controller;

import jakarta.validation.Valid;
import org.csu.mypetstore.admin.form.CategoryCreateForm;
import org.csu.mypetstore.admin.form.CategoryUpdateForm;
import org.csu.mypetstore.model.command.CategoryCreateCommand;
import org.csu.mypetstore.model.command.CategoryUpdateCommand;
import org.csu.mypetstore.model.dto.CategoryEditView;
import org.csu.mypetstore.model.dto.CategoryRow;
import org.csu.mypetstore.model.query.CategoryListQuery;
import org.csu.mypetstore.service.CategoryNotFoundException;
import org.csu.mypetstore.service.CategoryService;
import org.csu.mypetstore.service.CategoryValidationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/admin/categories")
    public String list(CategoryListQuery query, Model model) {
        model.addAttribute("page", categoryService.listTree(query));
        model.addAttribute("query", query);
        return "category/list";
    }

    @GetMapping("/admin/categories/new")
    public String newForm(@RequestParam(required = false) Long parentId, Model model) {
        CategoryCreateForm form = new CategoryCreateForm();
        form.setParentId(parentId);
        model.addAttribute("form", form);
        prepareCreateModel(parentId, model);
        return "category/form";
    }

    @PostMapping("/admin/categories")
    public String create(
            @Valid @ModelAttribute("form") CategoryCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                categoryService.create(new CategoryCreateCommand(
                        form.getCode(), form.getName(), form.getSortOrder(), form.getParentId()));
            } catch (CategoryValidationException exception) {
                reject(bindingResult, exception);
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", firstError(bindingResult));
            prepareCreateModel(form.getParentId(), model);
            return "category/form";
        }
        redirectAttributes.addFlashAttribute("message", "分类已创建");
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        CategoryEditView view = categoryService.getForEdit(id);
        CategoryUpdateForm form = new CategoryUpdateForm();
        form.setName(view.name());
        form.setSortOrder(view.sortOrder());
        model.addAttribute("form", form);
        prepareEditModel(view, model);
        return "category/form";
    }

    @PostMapping("/admin/categories/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") CategoryUpdateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                categoryService.update(id, new CategoryUpdateCommand(form.getName(), form.getSortOrder()));
            } catch (CategoryValidationException exception) {
                reject(bindingResult, exception);
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", firstError(bindingResult));
            prepareEditModel(categoryService.getForEdit(id), model);
            return "category/form";
        }
        redirectAttributes.addFlashAttribute("message", "分类已更新");
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("message", "分类已删除");
        } catch (CategoryValidationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/categories";
    }

    private void prepareCreateModel(Long parentId, Model model) {
        List<CategoryRow> options = categoryService.listTopLevelOptions();
        model.addAttribute("topLevelOptions", options);
        model.addAttribute("editing", false);
        if (parentId == null) {
            model.addAttribute("parentName", null);
            return;
        }
        CategoryRow parent = options.stream()
                .filter(option -> option.getId().equals(parentId))
                .findFirst()
                .orElseThrow(() -> new CategoryNotFoundException(parentId));
        model.addAttribute("parentName", parent.getName());
    }

    private void prepareEditModel(CategoryEditView view, Model model) {
        model.addAttribute("editing", true);
        model.addAttribute("category", view);
        model.addAttribute("parentName", view.parentName());
    }

    private void reject(BindingResult bindingResult, CategoryValidationException exception) {
        if (exception.getField() == null) {
            bindingResult.reject("category.invalid", exception.getMessage());
        } else {
            bindingResult.rejectValue(exception.getField(), "category.invalid", exception.getMessage());
        }
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? "请检查表单内容"
                : bindingResult.getAllErrors().getFirst().getDefaultMessage();
    }
}
