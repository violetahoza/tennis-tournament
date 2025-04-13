package com.ex.tennistournament.security;

import com.ex.tennistournament.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for handling JSON Web Token (JWT) operations.
 * Provides functionality for token generation, validation, and parsing.
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
     * Also includes user roles in the token claims.
     *
     * @param authentication the authentication object containing user details
     * @return signed JWT token as string
     */
    public String generateJwtToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

        // Extract authorities/roles to include in the token
        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        log.debug("Generating token for user: {}, with authorities: {}", userPrincipal.getUsername(), authorities);

        // Create claims for the token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userPrincipal.getId());
        claims.put("email", userPrincipal.getEmail());
        claims.put("userType", userPrincipal.getUserType().name());
        claims.put("authorities", authorities);
        claims.put("firstName", userPrincipal.getFirstName());
        claims.put("lastName", userPrincipal.getLastName());

        return Jwts.builder()
                .setClaims(claims)
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
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.getSubject();
        log.debug("Extracted username from token: {}", username);

        // Log all claims for debugging
        log.debug("Token claims: {}", claims);

        return username;
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
            log.debug("JWT token validated successfully");
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