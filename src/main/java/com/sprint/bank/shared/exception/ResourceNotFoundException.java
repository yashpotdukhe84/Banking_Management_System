package com.sprint.bank.shared.exception;

/** Base "404" exception. Module exceptions (AccountNotFoundException, ...) extend this. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
