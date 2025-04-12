package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.JwtResponseDto;
import com.ex.tennistournament.dto.LoginDto;
import com.ex.tennistournament.dto.PasswordUpdateDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.dto.UserRegistrationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public UserDto registerUser(UserRegistrationDto registrationDto) {
        // Check if username is already taken
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Check if email is already in use
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // Create new user
        User user = User.builder()
                .username(registrationDto.getUsername())
                .password(passwordEncoder.encode(registrationDto.getPassword()))
                .email(registrationDto.getEmail())
                .firstName(registrationDto.getFirstName())
                .lastName(registrationDto.getLastName())
                .userType(registrationDto.getUserType())
                .build();

        User savedUser = userRepository.save(user);
        return mapUserToDto(savedUser);
    }

    public JwtResponseDto authenticateUser(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User userDetails = (User) authentication.getPrincipal();

        return JwtResponseDto.builder()
                .token(jwt)
                .type("Bearer")
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .userType(userDetails.getUserType())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .build();
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    public List<UserDto> getUsersByType(User.UserType userType) {
        return userRepository.findByUserType(userType).stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Security check: ensure the current user can only update their own profile
        // unless they are an ADMIN
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getId().equals(id) && currentUser.getUserType() != User.UserType.ADMIN) {
            throw new AccessDeniedException("You can only update your own profile.");
        }

        // Check if username is already taken by another user
        if (!user.getUsername().equals(userDto.getUsername()) &&
                userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Check if email is already in use by another user
        if (!user.getEmail().equals(userDto.getEmail()) &&
                userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());

        // Only administrators can change user roles
        if (currentUser.getUserType() == User.UserType.ADMIN && userDto.getUserType() != null) {
            user.setUserType(userDto.getUserType());
        }

        // Update password if provided
        String password = userDto.getPassword();
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    @Transactional
    public UserDto updatePassword(Long id, PasswordUpdateDto passwordDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Security check: ensure users can only change their own password
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getId().equals(id)) {
            throw new AccessDeniedException("You can only change your own password.");
        }

        // Verify current password
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), passwordDto.getCurrentPassword())
            );
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
        User updatedUser = userRepository.save(user);

        return mapUserToDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                // Do not include password in the response
                .build();
    }
}