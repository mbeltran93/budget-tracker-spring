package com.mbeltran.budgettracker.controller.rest;

import com.mbeltran.budgettracker.dto.DashboardSummary;
import com.mbeltran.budgettracker.security.CurrentUser;
import com.mbeltran.budgettracker.service.CategoryService;
import com.mbeltran.budgettracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno que jQuery reconsulta despues de cada alta/edicion/baja de un gasto
 * para refrescar el total del mes, el grafico de Chart.js y las barras de progreso
 * de las categorias, sin recargar el dashboard.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;

    @GetMapping("/summary")
    public DashboardSummary summary() {
        Long userId = CurrentUser.id();
        var total = expenseService.totalForCurrentMonth(userId);
        var categories = categoryService.budgetOverviewForCurrentMonth(userId);
        return new DashboardSummary(total, categories);
    }
}
