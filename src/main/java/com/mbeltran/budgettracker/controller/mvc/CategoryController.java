package com.mbeltran.budgettracker.controller.mvc;

import com.mbeltran.budgettracker.dto.CategoryForm;
import com.mbeltran.budgettracker.exception.DuplicateResourceException;
import com.mbeltran.budgettracker.security.CurrentUser;
import com.mbeltran.budgettracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Flujo clasico de formulario + recarga de pagina (Thymeleaf puro, th:action),
 * a diferencia del CRUD de gastos que se maneja todo por AJAX.
 */
@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("budgetOverview", categoryService.budgetOverviewForCurrentMonth(CurrentUser.id()));
        return "categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return "categories/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var category = categoryService.findByIdOrThrow(id);
        CategoryForm form = new CategoryForm();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setMonthlyLimit(category.getMonthlyLimit());
        model.addAttribute("categoryForm", form);
        return "categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (bindingResult.hasErrors()) {
            return "categories/form";
        }
        try {
            categoryService.create(form);
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return "categories/form";
        }
        redirectAttributes.addFlashAttribute("message", "Categoria creada correctamente");
        return "redirect:/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("categoryForm") CategoryForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "categories/form";
        }
        try {
            categoryService.update(id, form);
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return "categories/form";
        }
        redirectAttributes.addFlashAttribute("message", "Categoria actualizada correctamente");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Categoria eliminada");
        return "redirect:/categories";
    }
}
