package com.ex.tennistournament.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for JWT-based security.
 * Handles unauthorized access attempts in the application.
 *
 * Key responsibilities:
 * - Intercepts unauthorized access attempts
 * - Generates standardized JSON error responses
 * - Logs authentication failures
 * - Sets appropriate HTTP status codes
 *
 * Features:
 * - Custom error message formatting
 * - JSON response generation
 * - Detailed error logging
 * - Path information inclusion
 */
@Component
@Slf4j
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    /**
     * Called when an unauthenticated user attempts to access protected resources.
     * Generates a JSON response with error details and sets HTTP status to 401.
     *
     * @param request HTTP request that resulted in an AuthenticationException
     * @param response HTTP response to be modified
     * @param authException the exception that triggered this handler
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        log.error("Unauthorized error: {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}