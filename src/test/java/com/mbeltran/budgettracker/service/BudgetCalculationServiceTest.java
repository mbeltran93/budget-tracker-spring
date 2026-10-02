package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.dto.BudgetStatus;
import com.mbeltran.budgettracker.dto.CategoryBudgetView;
import com.mbeltran.budgettracker.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La logica de calculo de presupuesto es el corazon del dominio (de ahi que tenga
 * su propio service, separado de persistencia): dado un limite y lo gastado, decide
 * el semaforo verde/amarillo/rojo que se ve en el dashboard y en la lista de categorias.
 */
class BudgetCalculationServiceTest {

    private final BudgetCalculationService service = new BudgetCalculationService();

    private Category category(String name, String limit) {
        return Category.builder().id(1L).name(name).monthlyLimit(new BigDecimal(limit)).build();
    }

    @Test
    @DisplayName("0% gastado -> estado OK, resta el limite completo")
    void zeroPercentIsOk() {
        CategoryBudgetView result = service.evaluate(category("Comida", "500.00"), BigDecimal.ZERO);

        assertThat(result.getPercentUsed()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(BudgetStatus.OK);
        assertThat(result.getRemaining()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("50% gastado exactamente -> cruza a WARNING (umbral inclusivo)")
    void fiftyPercentIsWarning() {
        CategoryBudgetView result = service.evaluate(category("Comida", "500.00"), new BigDecimal("250.00"));

        assertThat(result.getPercentUsed()).isEqualTo(50);
        assertThat(result.getStatus()).isEqualTo(BudgetStatus.WARNING);
        assertThat(result.getRemaining()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("100% gastado exactamente -> estado OVER, sin presupuesto restante")
    void hundredPercentIsOver() {
        CategoryBudgetView result = service.evaluate(category("Comida", "500.00"), new BigDecimal("500.00"));

        assertThat(result.getPercentUsed()).isEqualTo(100);
        assertThat(result.getStatus()).isEqualTo(BudgetStatus.OVER);
        assertThat(result.getRemaining()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Sobregirado (150% del limite) -> sigue OVER, remaining negativo, barra tope 100%")
    void overBudgetIsOverWithNegativeRemaining() {
        CategoryBudgetView result = service.evaluate(category("Comida", "500.00"), new BigDecimal("750.00"));

        assertThat(result.getStatus()).isEqualTo(BudgetStatus.OVER);
        assertThat(result.getPercentUsed()).isEqualTo(100);
        assertThat(result.getRemaining()).isEqualByComparingTo("-250.00");
    }

    @ParameterizedTest(name = "{0}% usado -> {1}")
    @DisplayName("Umbrales de porcentaje mapean al status correcto")
    @CsvSource({
            "0,OK",
            "25,OK",
            "49,OK",
            "50,WARNING",
            "75,WARNING",
            "99,WARNING",
            "100,OVER",
            "200,OVER"
    })
    void statusForBoundaries(int percent, BudgetStatus expected) {
        assertThat(service.statusFor(percent)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Categoria sin limite (0) con algo gastado -> OVER, no divide por cero")
    void zeroLimitWithExpenseIsOver() {
        CategoryBudgetView result = service.evaluate(category("Sin limite", "0.00"), new BigDecimal("10.00"));

        assertThat(result.getStatus()).isEqualTo(BudgetStatus.OVER);
        assertThat(result.getPercentUsed()).isEqualTo(100);
    }

    @Test
    @DisplayName("Categoria sin limite (0) y sin gastos -> OK")
    void zeroLimitWithNoExpenseIsOk() {
        CategoryBudgetView result = service.evaluate(category("Sin limite", "0.00"), BigDecimal.ZERO);

        assertThat(result.getStatus()).isEqualTo(BudgetStatus.OK);
        assertThat(result.getPercentUsed()).isEqualTo(0);
    }

    @Test
    @DisplayName("spent nulo se trata como cero")
    void nullSpentTreatedAsZero() {
        CategoryBudgetView result = service.evaluate(category("Comida", "500.00"), null);

        assertThat(result.getPercentUsed()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(BudgetStatus.OK);
    }
}
