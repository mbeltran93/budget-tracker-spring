package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.dto.CategoryForm;
import com.mbeltran.budgettracker.entity.Category;
import com.mbeltran.budgettracker.exception.DuplicateResourceException;
import com.mbeltran.budgettracker.exception.ResourceNotFoundException;
import com.mbeltran.budgettracker.repository.CategoryRepository;
import com.mbeltran.budgettracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, expenseRepository, new BudgetCalculationService());
    }

    @Test
    void createsCategoryWhenNameIsUnique() {
        CategoryForm form = form("Comida", "500.00");
        when(categoryRepository.findByNameIgnoreCase("Comida")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.create(form);

        assertThat(created.getName()).isEqualTo("Comida");
        assertThat(created.getMonthlyLimit()).isEqualByComparingTo("500.00");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void rejectsDuplicateCategoryNameOnCreate() {
        CategoryForm form = form("Comida", "500.00");
        when(categoryRepository.findByNameIgnoreCase("Comida"))
                .thenReturn(Optional.of(Category.builder().id(1L).name("Comida").monthlyLimit(BigDecimal.TEN).build()));

        assertThatThrownBy(() -> categoryService.create(form))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void allowsRenamingToItsOwnCurrentNameOnUpdate() {
        Category existing = Category.builder().id(5L).name("Comida").monthlyLimit(BigDecimal.TEN).build();
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByNameIgnoreCase("Comida")).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = categoryService.update(5L, form("Comida", "600.00"));

        assertThat(updated.getMonthlyLimit()).isEqualByComparingTo("600.00");
    }

    @Test
    void throwsWhenUpdatingMissingCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, form("X", "1.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void budgetOverviewMapsEachCategoryUsingSpentFromExpenseRepository() {
        Category comida = Category.builder().id(1L).name("Comida").monthlyLimit(new BigDecimal("500.00")).build();
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(comida));
        when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(eq(42L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("250.00"));

        var overview = categoryService.budgetOverviewForCurrentMonth(42L);

        assertThat(overview).hasSize(1);
        assertThat(overview.get(0).getPercentUsed()).isEqualTo(50);
    }

    private CategoryForm form(String name, String limit) {
        CategoryForm form = new CategoryForm();
        form.setName(name);
        form.setMonthlyLimit(new BigDecimal(limit));
        return form;
    }
}
