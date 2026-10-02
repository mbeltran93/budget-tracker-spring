package com.mbeltran.budgettracker.exception;

/** Lanzada cuando una entidad (gasto, categoria, usuario) no existe o no pertenece al usuario actual. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
