package com.mbeltran.budgettracker.controller.rest;

import com.mbeltran.budgettracker.dto.ExpenseRequest;
import com.mbeltran.budgettracker.dto.ExpenseResponse;
import com.mbeltran.budgettracker.security.CurrentUser;
import com.mbeltran.budgettracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST internos consumidos por jQuery ($.ajax) desde el dashboard,
 * para el CRUD de gastos sin recargar la pagina. Devuelven siempre JSON.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseRestController {

    private final ExpenseService expenseService;

    @GetMapping
    public List<ExpenseResponse> list() {
        return expenseService.findAllForUser(CurrentUser.id());
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = expenseService.create(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(CurrentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
