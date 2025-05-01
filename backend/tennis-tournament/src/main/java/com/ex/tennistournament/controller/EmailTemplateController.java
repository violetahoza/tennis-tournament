package com.ex.tennistournament.controller;

import com.ex.tennistournament.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for testing and managing email templates.
 * Provides endpoints to test template rendering and email sending functionality.
 * Restricted to ADMIN users for security.
 */
@RestController
@RequestMapping("/api/admin/email-templates")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateController {

    private final EmailService emailService;

    /**
     * Tests template rendering without sending an email.
     * Returns the processed HTML content for verification.
     *
     * @param templateName Name of the template to test
     * @return The rendered HTML content
     */
    @GetMapping("/test-template/{templateName}")
    public ResponseEntity<String> testTemplate(@PathVariable String templateName) {
        log.info("Testing template rendering for: {}", templateName);

        // Create dummy data for testing
        Map<String, Object> variables = createSampleVariables();

        // Process the template
        String content = emailService.testTemplate(templateName, variables);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }

    /**
     * Tests email sending functionality by sending a test email.
     *
     * @param email Email address to send test email to
     * @param templateName Name of template to use (optional)
     * @return Status message
     */
    @PostMapping("/send-test")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "registration-confirmation") String templateName) {

        log.info("Sending test email to {} using template {}", email, templateName);

        try {
            // Create dummy data for testing
            Map<String, Object> variables = createSampleVariables();

            // Send test email
            emailService.sendTemplateEmail(
                    email,
                    "Tennis Tournament - Test Email",
                    templateName,
                    variables
            );

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test email sent to " + email + " using template " + templateName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending test email", e);

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to send test email: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Creates sample variables for template testing
     */
    private Map<String, Object> createSampleVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("playerName", "John Doe");
        variables.put("tournamentName", "Summer Open 2025");
        variables.put("status", "PENDING");
        variables.put("newStatus", "APPROVED");
        variables.put("oldStatus", "PENDING");
        variables.put("registrationDate", "2025-05-01");
        variables.put("cancellationDate", "2025-05-01");
        variables.put("updateDate", "2025-05-01");
        return variables;
    }

    /**
     * Tests email configuration and server connection.
     *
     * @return Status of email configuration
     */
    @GetMapping("/test-config")
    public ResponseEntity<Map<String, Object>> testEmailConfig() {
        log.info("Testing email server configuration");

        Map<String, Object> response = new HashMap<>();
        boolean configValid = emailService.testEmailConfiguration();

        response.put("configValid", configValid);
        response.put("emailEnabled", true); // This should be dynamically obtained from configuration

        if (!configValid) {
            response.put("message", "Email configuration appears to be invalid");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("message", "Email configuration is valid");
        return ResponseEntity.ok(response);
    }
}