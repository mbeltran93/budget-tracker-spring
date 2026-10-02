package com.mbeltran.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Vista de solo lectura del estado de presupuesto de una categoria,
 * usada tanto en el dashboard (grafico) como en la lista de categorias (barra de progreso).
 */
@Getter
@AllArgsConstructor
public class CategoryBudgetView {

    private final Long categoryId;
    private final String categoryName;
    private final BigDecimal monthlyLimit;
    private final BigDecimal spent;
    private final BigDecimal remaining;
    private final int percentUsed;
    private final BudgetStatus status;
}
