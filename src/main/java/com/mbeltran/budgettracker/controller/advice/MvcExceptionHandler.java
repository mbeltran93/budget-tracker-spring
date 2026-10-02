package com.mbeltran.budgettracker.controller.advice;

import com.mbeltran.budgettracker.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Manejo de errores para los controllers Thymeleaf (/dashboard, /categories, ...):
 * siempre devuelve una vista de error renderizada en el servidor.
 */
@ControllerAdvice(basePackages = "com.mbeltran.budgettracker.controller.mvc")
public class MvcExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }
}
