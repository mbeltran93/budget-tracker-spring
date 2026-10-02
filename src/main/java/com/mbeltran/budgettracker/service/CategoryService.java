package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.dto.CategoryBudgetView;
import com.mbeltran.budgettracker.dto.CategoryForm;
import com.mbeltran.budgettracker.entity.Category;
import com.mbeltran.budgettracker.exception.DuplicateResourceException;
import com.mbeltran.budgettracker.exception.ResourceNotFoundException;
import com.mbeltran.budgettracker.repository.CategoryRepository;
import com.mbeltran.budgettracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetCalculationService budgetCalculationService;

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La categoria " + id + " no existe"));
    }

    /** Estado de presupuesto (gastado/limite/status) de todas las categorias para el mes actual del usuario dado. */
    @Transactional(readOnly = true)
    public List<CategoryBudgetView> budgetOverviewForCurrentMonth(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate from = currentMonth.atDay(1);
        LocalDate to = currentMonth.atEndOfMonth();

        return findAll().stream()
                .map(category -> {
                    var spent = expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                            userId, category.getId(), from, to);
                    return budgetCalculationService.evaluate(category, spent);
                })
                .toList();
    }

    @Transactional
    public Category create(CategoryForm form) {
        categoryRepository.findByNameIgnoreCase(form.getName()).ifPresent(existing -> {
            throw new DuplicateResourceException("Ya existe una categoria llamada " + form.getName());
        });
        Category category = Category.builder()
                .name(form.getName())
                .monthlyLimit(form.getMonthlyLimit())
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryForm form) {
        Category category = findByIdOrThrow(id);
        categoryRepository.findByNameIgnoreCase(form.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Ya existe una categoria llamada " + form.getName());
                });
        category.setName(form.getName());
        category.setMonthlyLimit(form.getMonthlyLimit());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findByIdOrThrow(id);
        categoryRepository.delete(category);
    }
}
