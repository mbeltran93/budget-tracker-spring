package com.mbeltran.budgettracker.exception;

/** Lanzada al intentar crear un recurso que viola una restriccion de unicidad (email, nombre de categoria). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
