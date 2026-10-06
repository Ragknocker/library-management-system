package com.library.exception;

/**
 * Thrown when low-level JDBC or database transaction failures occur.
 */
public class DatabaseException extends LibraryException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
