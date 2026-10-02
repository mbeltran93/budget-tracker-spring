package com.mbeltran.budgettracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Modelo del formulario Thymeleaf (th:object) para crear/editar una categoria. */
@Getter
@Setter
@NoArgsConstructor
public class CategoryForm {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre es demasiado largo")
    private String name;

    @NotNull(message = "El limite mensual es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El limite no puede ser negativo")
    private BigDecimal monthlyLimit;
}
