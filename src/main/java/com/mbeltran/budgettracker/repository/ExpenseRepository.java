package com.mbeltran.budgettracker.repository;

import com.mbeltran.budgettracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByDateDescIdDesc(Long userId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select e from Expense e
            where e.user.id = :userId
            and e.date between :from and :to
            order by e.date desc, e.id desc
            """)
    List<Expense> findByUserAndDateRange(@Param("userId") Long userId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.user.id = :userId
            and e.date between :from and :to
            """)
    BigDecimal sumAmountByUserAndDateRange(@Param("userId") Long userId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.user.id = :userId
            and e.category.id = :categoryId
            and e.date between :from and :to
            """)
    BigDecimal sumAmountByUserAndCategoryAndDateRange(@Param("userId") Long userId,
                                                        @Param("categoryId") Long categoryId,
                                                        @Param("from") LocalDate from,
                                                        @Param("to") LocalDate to);
}
