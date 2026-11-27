package com.sprint.bank.shared.exception;

/** Login blocked because of too many failed attempts. Mapped to HTTP 423 (Locked). */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
