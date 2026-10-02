package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.dto.ExpenseRequest;
import com.mbeltran.budgettracker.dto.ExpenseResponse;
import com.mbeltran.budgettracker.entity.Category;
import com.mbeltran.budgettracker.entity.Expense;
import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.exception.ResourceNotFoundException;
import com.mbeltran.budgettracker.repository.CategoryRepository;
import com.mbeltran.budgettracker.repository.ExpenseRepository;
import com.mbeltran.budgettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAllForUser(Long userId) {
        return expenseRepository.findByUserIdOrderByDateDescIdDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Mapea a ExpenseResponse (no entidades) adentro de la transaccion: la categoria
     * es LAZY y open-in-view esta deshabilitado, asi que si devolvieramos la entidad
     * Expense tal cual, Thymeleaf fallaria con LazyInitializationException al pintar
     * expense.category.name fuera de la sesion de Hibernate.
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> recentForUser(Long userId, int limit) {
        return expenseRepository.findByUserIdOrderByDateDescIdDesc(userId).stream()
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalForCurrentMonth(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        return expenseRepository.sumAmountByUserAndDateRange(
                userId, currentMonth.atDay(1), currentMonth.atEndOfMonth());
    }

    @Transactional
    public ExpenseResponse create(Long userId, ExpenseRequest request) {
        User user = userRepository.getReferenceById(userId);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La categoria " + request.getCategoryId() + " no existe"));

        Expense expense = Expense.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .description(request.getDescription())
                .date(request.getDate())
                .build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request) {
        Expense expense = findOwnedByUserOrThrow(userId, expenseId);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La categoria " + request.getCategoryId() + " no existe"));

        expense.setCategory(category);
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setDate(request.getDate());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long userId, Long expenseId) {
        Expense expense = findOwnedByUserOrThrow(userId, expenseId);
        expenseRepository.delete(expense);
    }

    private Expense findOwnedByUserOrThrow(Long userId, Long expenseId) {
        return expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El gasto " + expenseId + " no existe o no te pertenece"));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getDate()
        );
    }
}
