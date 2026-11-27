package com.sprint.bank.shared.exception;

/** Thrown when a unique value already exists (e.g. email). Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
