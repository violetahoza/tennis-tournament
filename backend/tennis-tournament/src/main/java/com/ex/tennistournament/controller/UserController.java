package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.PasswordUpdateDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing user accounts.
 * Handles user operations including queries, updates, and deletions.
 *
 * Security:
 * - Admin access required for user listing and deletion
 * - Self-service operations for password updates
 * - Uses @PreAuthorize for role-based authorization
 *
 * Features:
 * - User CRUD operations
 * - User type filtering
 * - Password management
 * - Input validation
 *
 * Validations:
 * - Request body validation using @Valid
 * - User existence checks
 * - Password complexity rules
 * - User type constraints
 *
 * Integration:
 * - Works with UserService for business logic
 * - Uses UserDto for data transfer
 * - Handles UserType enum for user categories
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Retrieves all users in the system. Admin only.
     *
     * @return ResponseEntity with list of all users
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Retrieves users filtered by type. Admin only.
     *
     * @param userType Type of users to retrieve
     * @return ResponseEntity with filtered list of users
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserDto>> getUsersByType(@PathVariable("type") User.UserType userType) {
        return ResponseEntity.ok(userService.getUsersByType(userType));
    }

    /**
     * Retrieves a specific user by ID.
     *
     * @param id User ID to retrieve
     * @return ResponseEntity with user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Updates user information.
     *
     * @param id User ID to update
     * @param userDto Updated user details
     * @return ResponseEntity with updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("id") Long id, @Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    /**
     * Updates user password.
     *
     * @param id User ID
     * @param passwordDto New password details
     * @return ResponseEntity with updated user
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<UserDto> updatePassword(@PathVariable("id") Long id, @Valid @RequestBody PasswordUpdateDto passwordDto) {
        return ResponseEntity.ok(userService.updatePassword(id, passwordDto));
    }

    /**
     * Deletes a user from the system. Admin only.
     *
     * @param id User ID to delete
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}