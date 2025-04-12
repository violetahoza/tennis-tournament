package com.ex.tennistournament.security;

import com.ex.tennistournament.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Utility class for handling JSON Web Token (JWT) operations.
 * Provides functionality for token generation, validation, and parsing.
 *
 * Key responsibilities:
 * - Generates JWT tokens for authenticated users
 * - Validates incoming JWT tokens
 * - Extracts username from tokens
 * - Manages JWT signing keys
 *
 * Configuration:
 * - JWT secret key from application properties
 * - Token expiration time from application properties
 *
 * Error handling:
 * - Catches and logs various JWT-related exceptions
 * - Provides detailed error messages for different failure scenarios
 */
@Component
@Slf4j
public class JwtUtils {

    /**
     * Secret key used for signing JWT tokens.
     * Configured through application properties.
     */
    @Value("${tennis.app.jwtSecret}")
    private String jwtSecret;

    /**
     * Token expiration time in milliseconds.
     * Configured through application properties.
     */
    @Value("${tennis.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    /**
     * Generates a JWT token for an authenticated user.
     * Sets subject, issuance time, expiration, and signs the token.
     *
     * @param authentication the authentication object containing user details
     * @return signed JWT token as string
     */
    public String generateJwtToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * Creates a signing key from the JWT secret.
     * Uses HMAC-SHA algorithm for key generation.
     *
     * @return Key object used for signing tokens
     */
    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extracts username from a JWT token.
     * Parses and validates the token before extracting the subject claim.
     *
     * @param token the JWT token to parse
     * @return username stored in the token
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validates a JWT token.
     * Checks for token integrity, expiration, and format.
     *
     * @param authToken the token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }
}