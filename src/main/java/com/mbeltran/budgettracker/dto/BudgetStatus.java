package com.mbeltran.budgettracker.dto;

/**
 * Semaforo de uso de presupuesto para una categoria.
 * OK (verde) &lt; 50% usado, WARNING (amarillo) entre 50% y 99.99%, OVER (rojo) &gt;= 100%.
 */
public enum BudgetStatus {
    OK("success"),
    WARNING("warning"),
    OVER("danger");

    private final String bootstrapClass;

    BudgetStatus(String bootstrapClass) {
        this.bootstrapClass = bootstrapClass;
    }

    /** Sufijo de clase Bootstrap (bg-success / bg-warning / bg-danger). */
    public String getBootstrapClass() {
        return bootstrapClass;
    }
}
