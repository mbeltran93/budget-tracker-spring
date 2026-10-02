package com.mbeltran.budgettracker.controller.rest;

import com.mbeltran.budgettracker.dto.CategoryResponse;
import com.mbeltran.budgettracker.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint REST interno (consumido por jQuery y, ahora, por un cliente JWT) con las
 * categorias disponibles. Igual que en CategoryController (MVC), las categorias son
 * globales de la app, no por usuario -no hay filtro de ownership aqui tampoco-.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryRestController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getMonthlyLimit()))
                .toList();
    }
}
