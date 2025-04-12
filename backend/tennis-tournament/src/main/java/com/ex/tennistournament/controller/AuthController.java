package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.JwtResponseDto;
import com.ex.tennistournament.dto.LoginDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.dto.UserRegistrationDto;
import com.ex.tennistournament.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller handling authentication and user registration endpoints.
 * Provides APIs for user sign-in and sign-up operations.
 *
 * Endpoints:
 * - POST /api/auth/signin: Authenticates existing users
 * - POST /api/auth/signup: Registers new users
 *
 * Security:
 * - Uses JWT for authentication
 * - Validates request DTOs
 * - Returns JWT token upon successful authentication
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;


    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginDto Contains username and password
     * @return JWT response containing token and user details
     */
    @PostMapping("/signin")
    public ResponseEntity<JwtResponseDto> authenticateUser(@Valid @RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(userService.authenticateUser(loginDto));
    }

    /**
     * Registers a new user in the system.
     *
     * @param registrationDto Contains user registration details
     * @return Created user information
     */
    @PostMapping("/signup")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        return ResponseEntity.ok(userService.registerUser(registrationDto));
    }
}