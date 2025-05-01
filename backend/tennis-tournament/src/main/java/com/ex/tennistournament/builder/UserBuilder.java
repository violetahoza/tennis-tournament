package com.ex.tennistournament.builder;

import com.ex.tennistournament.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Pattern;

/**
 * UserBuilder is a concrete builder for creating User objects.
 * It implements the Builder interface and provides methods for setting User attributes.
 */
public class UserBuilder implements Builder<User> {
    private final User user;
    private final PasswordEncoder passwordEncoder;

    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    // Password validation regex patterns
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASSWORD_LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile(".*\\d.*");

    // Stores the raw password before encoding
    private String rawPassword;

    /**
     * Constructor initializes a new User object and sets the PasswordEncoder.
     *
     * @param passwordEncoder the PasswordEncoder used to encode passwords
     */
    public UserBuilder(PasswordEncoder passwordEncoder) {
        this.user = new User();
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Sets the username for the User.
     *
     * @param username the username to set
     * @return the current instance of UserBuilder
     */
    public UserBuilder username(String username) {
        this.user.setUsername(username);
        return this;
    }

    /**
     * Sets the email for the User.
     *
     * @param email the email to set
     * @return the current instance of UserBuilder
     */
    public UserBuilder email(String email) {
        this.user.setEmail(email);
        return this;
    }

    /**
     * Sets the raw password for the User.
     * The password will be validated and encoded during the build process.
     *
     * @param password the raw password to set
     * @return the current instance of UserBuilder
     */
    public UserBuilder password(String password) {
        this.rawPassword = password; // Store raw password for validation
        return this;
    }

    /**
     * Sets the first name for the User.
     *
     * @param firstName the first name to set
     * @return the current instance of UserBuilder
     */
    public UserBuilder firstName(String firstName) {
        this.user.setFirstName(firstName);
        return this;
    }

    /**
     * Sets the last name for the User.
     *
     * @param lastName the last name to set
     * @return the current instance of UserBuilder
     */
    public UserBuilder lastName(String lastName) {
        this.user.setLastName(lastName);
        return this;
    }

    /**
     * Sets the user type for the User.
     *
     * @param userType the user type to set
     * @return the current instance of UserBuilder
     */
    public UserBuilder userType(User.UserType userType) {
        this.user.setUserType(userType);
        return this;
    }

    /**
     * Builds and returns the User object.
     * Validates the User attributes and encodes the password before returning the object.
     *
     * @return the built User object
     * @throws IllegalStateException if any required attribute is missing or invalid
     */
    @Override
    public User build() {
        // Validate user before returning
        validateUser();

        // Encode password if it was provided
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

        return user;
    }

    /**
     * Validates the User object to ensure all required attributes are set and valid.
     *
     * @throws IllegalStateException if any required attribute is missing or invalid
     */
    private void validateUser() {
        // Username validation
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalStateException("Username cannot be empty");
        }

        if (user.getUsername().length() < 3) {
            throw new IllegalStateException("Username must be at least 3 characters long");
        }

        if (user.getUsername().length() > 50) {
            throw new IllegalStateException("Username cannot exceed 50 characters");
        }

        // Email validation
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalStateException("Email cannot be empty");
        }

        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            throw new IllegalStateException("Invalid email format");
        }

        // Password validation (only for new users or password changes)
        if (rawPassword != null) {
            if (rawPassword.length() < 8) {
                throw new IllegalStateException("Password must be at least 8 characters long");
            }

            if (!PASSWORD_UPPERCASE.matcher(rawPassword).matches()) {
                throw new IllegalStateException("Password must contain at least one uppercase letter");
            }

            if (!PASSWORD_LOWERCASE.matcher(rawPassword).matches()) {
                throw new IllegalStateException("Password must contain at least one lowercase letter");
            }

            if (!PASSWORD_DIGIT.matcher(rawPassword).matches()) {
                throw new IllegalStateException("Password must contain at least one digit");
            }
        }

        // Name validation
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            throw new IllegalStateException("First name cannot be empty");
        }

        if (user.getFirstName().length() < 2) {
            throw new IllegalStateException("First name must be at least 2 characters long");
        }

        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            throw new IllegalStateException("Last name cannot be empty");
        }

        if (user.getLastName().length() < 2) {
            throw new IllegalStateException("Last name must be at least 2 characters long");
        }

        // User type validation
        if (user.getUserType() == null) {
            throw new IllegalStateException("User type must be specified");
        }
    }
}