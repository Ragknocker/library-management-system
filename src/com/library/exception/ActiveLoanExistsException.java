package com.library.exception;

public class ActiveLoanExistsException extends LibraryException {
    public ActiveLoanExistsException(String message) {
        super(message);
    }
}
