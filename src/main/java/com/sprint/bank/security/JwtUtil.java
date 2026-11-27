package com.sprint.bank.security;

import com.sprint.bank.shared.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Creates, validates, and invalidates (on logout) JWT tokens. */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public JwtUtil(@Value("${app.jwt.secret}") String base64Secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (base64 encoded).");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /** Generates a signed JWT containing the user's email and role. */
    public String generateToken(String email, Role role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Returns the user's email if the token is valid and not logged out, otherwise null. */
    public String extractEmailIfValid(String token) {
        if (token == null || token.isBlank() || isTokenBlacklisted(token)) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /** Marks a token as logged out so it cannot be used again. */
    public void blacklistToken(String token) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.add(token);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
