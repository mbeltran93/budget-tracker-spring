package com.mbeltran.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Representacion JSON de un gasto, devuelta por los endpoints REST internos. */
@Getter
@AllArgsConstructor
public class ExpenseResponse {

    private final Long id;
    private final Long categoryId;
    private final String categoryName;
    private final BigDecimal amount;
    private final String description;
    private final LocalDate date;
}
