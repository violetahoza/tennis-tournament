package com.ex.tennistournament.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for handling not found resources in the application.
 * Extends RuntimeException for unchecked exception handling.
 *
 * Features:
 * - Automatically mapped to HTTP 404 (Not Found) response
 * - Used for indicating missing database entities
 * - Handled by GlobalExceptionHandler for consistent error responses
 *
 * Usage:
 * - Thrown when requested resources (tournaments, matches, users) are not found
 * - Converted to ErrorDetails response with appropriate status and message
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}