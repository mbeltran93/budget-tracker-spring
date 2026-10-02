package com.mbeltran.budgettracker.controller.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbeltran.budgettracker.security.CurrentUser;
import com.mbeltran.budgettracker.service.CategoryService;
import com.mbeltran.budgettracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) throws Exception {
        Long userId = CurrentUser.id();

        var categories = categoryService.findAll();
        var budgetOverview = categoryService.budgetOverviewForCurrentMonth(userId);
        var recentExpenses = expenseService.recentForUser(userId, 10);
        var totalThisMonth = expenseService.totalForCurrentMonth(userId);

        model.addAttribute("categories", categories);
        model.addAttribute("budgetOverview", budgetOverview);
        model.addAttribute("recentExpenses", recentExpenses);
        model.addAttribute("totalThisMonth", totalThisMonth);
        model.addAttribute("budgetOverviewJson", objectMapper.writeValueAsString(budgetOverview));

        return "dashboard";
    }
}
