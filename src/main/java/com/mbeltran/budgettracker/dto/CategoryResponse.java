package com.mbeltran.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/** Representacion JSON minima de una categoria, para GET /api/categories. */
@Getter
@AllArgsConstructor
public class CategoryResponse {

    private final Long id;
    private final String name;
    private final BigDecimal monthlyLimit;
}
