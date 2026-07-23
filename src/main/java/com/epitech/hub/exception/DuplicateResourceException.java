package com.epitech.hub.exception;

/** Levee lorsqu'une contrainte d'unicite serait violee (email ou nom d'utilisateur deja pris). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
