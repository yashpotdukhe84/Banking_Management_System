package com.sprint.bank.shared.exception;

/**
 * Bad email or password. The message is deliberately generic so an attacker cannot tell
 * "no such user" from "wrong password" (prevents user enumeration). Mapped to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
