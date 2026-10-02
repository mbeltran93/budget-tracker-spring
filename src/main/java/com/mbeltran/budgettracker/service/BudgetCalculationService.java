package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.dto.BudgetStatus;
import com.mbeltran.budgettracker.dto.CategoryBudgetView;
import com.mbeltran.budgettracker.entity.Category;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Logica pura de calculo de presupuesto: dado un limite mensual y lo gastado,
 * determina el porcentaje usado, lo que queda disponible y el semaforo
 * (OK &lt; 50%, WARNING 50-99%, OVER &gt;= 100%) que se pinta en la barra de progreso
 * y en el grafico del dashboard.
 */
@Service
public class BudgetCalculationService {

    private static final BigDecimal WARNING_THRESHOLD = BigDecimal.valueOf(50);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public CategoryBudgetView evaluate(Category category, BigDecimal spentRaw) {
        BigDecimal limit = category.getMonthlyLimit() == null ? BigDecimal.ZERO : category.getMonthlyLimit();
        BigDecimal spent = spentRaw == null ? BigDecimal.ZERO : spentRaw;

        int percentUsed = percentUsed(limit, spent);
        BudgetStatus status = statusFor(percentUsed);
        BigDecimal remaining = limit.subtract(spent);

        return new CategoryBudgetView(
                category.getId(),
                category.getName(),
                limit,
                spent,
                remaining,
                percentUsed,
                status
        );
    }

    /**
     * Porcentaje usado del limite, redondeado hacia abajo, con tope visual de 100
     * (una categoria sobregirada en un 250% sigue mostrando una barra llena, pero
     * el status/remaining ya reflejan que esta sobregirada).
     */
    public int percentUsed(BigDecimal limit, BigDecimal spent) {
        if (limit == null || limit.compareTo(BigDecimal.ZERO) <= 0) {
            return spent != null && spent.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        BigDecimal percent = spent.multiply(HUNDRED).divide(limit, 0, RoundingMode.FLOOR);
        if (percent.compareTo(HUNDRED) > 0) {
            return 100;
        }
        return percent.intValue();
    }

    public BudgetStatus statusFor(int percentUsed) {
        if (percentUsed >= 100) {
            return BudgetStatus.OVER;
        }
        if (BigDecimal.valueOf(percentUsed).compareTo(WARNING_THRESHOLD) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.OK;
    }
}
