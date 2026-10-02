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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    private ExpenseService expenseService;

    private final Long userId = 1L;
    private final User user = User.builder().id(userId).name("Demo").email("demo@test.com").password("x").build();
    private final Category category = Category.builder().id(2L).name("Comida").monthlyLimit(BigDecimal.TEN).build();

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void createsExpenseForOwnerUserAndGivenCategory() {
        ExpenseRequest request = new ExpenseRequest(2L, new BigDecimal("55.50"), "Super", LocalDate.of(2026, 10, 1));
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        ExpenseResponse response = expenseService.create(userId, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getCategoryName()).isEqualTo("Comida");
        assertThat(response.getAmount()).isEqualByComparingTo("55.50");

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void createThrowsWhenCategoryDoesNotExist() {
        ExpenseRequest request = new ExpenseRequest(999L, BigDecimal.ONE, "x", LocalDate.now());
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.create(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateFailsWhenExpenseDoesNotBelongToUser() {
        when(expenseRepository.findByIdAndUserId(50L, userId)).thenReturn(Optional.empty());

        ExpenseRequest request = new ExpenseRequest(2L, BigDecimal.ONE, "x", LocalDate.now());

        assertThatThrownBy(() -> expenseService.update(userId, 50L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void deleteRemovesOnlyWhenOwnedByUser() {
        Expense expense = Expense.builder().id(7L).user(user).category(category)
                .amount(BigDecimal.ONE).date(LocalDate.now()).build();
        when(expenseRepository.findByIdAndUserId(7L, userId)).thenReturn(Optional.of(expense));

        expenseService.delete(userId, 7L);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void deleteThrowsWhenNotOwnedByUser() {
        when(expenseRepository.findByIdAndUserId(7L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(userId, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(expenseRepository, never()).delete(any(Expense.class));
    }
}
