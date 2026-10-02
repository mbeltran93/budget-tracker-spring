package com.mbeltran.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen del mes actual: lo que jQuery vuelve a pedir despues de cada alta/edicion/baja
 * de un gasto para refrescar el total, el grafico de Chart.js y las barras de progreso,
 * sin recargar la pagina.
 */
@Getter
@AllArgsConstructor
public class DashboardSummary {

    private final BigDecimal totalThisMonth;
    private final List<CategoryBudgetView> categories;
}
