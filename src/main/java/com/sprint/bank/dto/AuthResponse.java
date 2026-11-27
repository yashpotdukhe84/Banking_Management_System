package com.sprint.bank.dto;

/** Returned after a successful register or login. */
public class AuthResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final long expiresInSeconds;
    private final String email;
    private final String role;
    private final String fullName;

    public AuthResponse(String token, long expiresInSeconds, String email, String role, String fullName) {
        this.token = token;
        this.expiresInSeconds = expiresInSeconds;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
}
