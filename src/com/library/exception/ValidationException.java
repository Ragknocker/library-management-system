package com.library.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when entity or parameter input fails automated business validation checks.
 */
public class ValidationException extends LibraryException {
    private final List<String> errorMessages;

    public ValidationException(String message) {
        super(message);
        this.errorMessages = Collections.singletonList(message);
    }

    public ValidationException(List<String> errorMessages) {
        super(String.join("\n• ", errorMessages));
        this.errorMessages = errorMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}
