package com.ex.tennistournament.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Service for sending email notifications.
 * Uses Spring Mail with Thymeleaf templates for email content.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@tennistournament.com}")
    private String fromEmail;

    @Value("${tennis.app.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Send an email with a Thymeleaf template
     */
    @Async
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Would have sent email to: {}, subject: {}, template: {}",
                    to, subject, templateName);
            return;
        }

        try {
            log.info("Preparing to send email to {}, template: {}", to, templateName);

            // First try to use the base template with content fragment
            String htmlContent;
            try {
                // Add base template path for content inclusion
                variables.put("content", templateName);

                // Process the template with Thymeleaf using the base template
                htmlContent = processTemplate("base-template", variables);
                log.debug("Successfully processed base template with content: {}", templateName);
            } catch (Exception e) {
                log.warn("Failed to use base template with content fragment. Trying direct template: {}", e.getMessage());

                // Remove the content variable to avoid conflicts
                variables.remove("content");

                // Process the specific template directly
                htmlContent = processTemplate(templateName, variables);
            }

            // Create and send the email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.info("Sending email to: {}", to);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}", to, e);
            // Try sending a fallback email
            try {
                sendFallbackEmail(to, subject, variables);
            } catch (Exception ex) {
                log.error("Even fallback email failed to: {}", to, ex);
            }
        }
    }

    /**
     * Process a template with Thymeleaf
     */
    private String processTemplate(String templateName, Map<String, Object> variables) {
        log.debug("Processing template '{}' with variables: {}", templateName, variables.keySet());

        try {
            // First standard attempt with Thymeleaf
            Context context = new Context();
            context.setVariables(variables);

            String result = templateEngine.process(templateName, context);
            if (result != null && !result.trim().isEmpty()) {
                log.debug("Template '{}' processed successfully", templateName);
                return result;
            }

            log.warn("Template '{}' processing returned empty result", templateName);
        } catch (TemplateInputException tie) {
            log.warn("Template '{}' not found by Thymeleaf: {}", templateName, tie.getMessage());
        } catch (Exception e) {
            log.warn("Error processing template '{}': {}", templateName, e.getMessage());
        }

        // Try to load template as raw resource
        try {
            // Try with both potential paths
            String[] potentialPaths = {
                    "templates/" + templateName + ".html",
                    "templates/email/" + templateName + ".html"
            };

            for (String path : potentialPaths) {
                log.debug("Trying to load template from path: {}", path);
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                    log.debug("Found template at: {}", path);

                    // Basic variable substitution
                    for (Map.Entry<String, Object> entry : variables.entrySet()) {
                        String placeholder = "${" + entry.getKey() + "}";
                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                        content = content.replace(placeholder, value);
                    }

                    return content;
                }
            }

            log.warn("Could not find template file at any expected location");
        } catch (IOException e) {
            log.warn("Error loading raw template file: {}", e.getMessage());
        }

        // If all else fails, generate basic content
        log.info("Falling back to generated email content for template: {}", templateName);
        return generateBasicEmailContent(variables);
    }

    /**
     * Send a fallback email when template processing fails
     */
    private void sendFallbackEmail(String to, String subject, Map<String, Object> variables) throws MessagingException {
        log.info("Sending fallback email to: {}", to);

        String htmlContent = generateBasicEmailContent(variables);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Fallback email sent successfully to: {}", to);
    }

    /**
     * Generate basic HTML content when template processing fails
     */
    private String generateBasicEmailContent(Map<String, Object> variables) {
        String playerName = variables.containsKey("playerName") ? variables.get("playerName").toString() : "Player";
        String tournamentName = variables.containsKey("tournamentName") ? variables.get("tournamentName").toString() : "tournament";
        String status = variables.getOrDefault("status",
                variables.getOrDefault("newStatus", "updated")).toString();
        String oldStatus = variables.containsKey("oldStatus") ? variables.get("oldStatus").toString() : "";

        return "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<div style='background-color: #4CAF50; color: white; padding: 20px; text-align: center;'>" +
                "<h1>Tennis Tournament</h1>" +
                "</div>" +
                "<div style='padding: 20px; background-color: #f9f9f9;'>" +
                "<h2>Tennis Tournament Notification</h2>" +
                "<p>Dear " + playerName + ",</p>" +
                "<p>Your registration for <strong>" + tournamentName + "</strong> has been " + status + ".</p>" +
                (oldStatus.isEmpty() ? "" : "<p>Your status has been updated from " + oldStatus + " to " + status + ".</p>") +
                "<p>Please log into the application for more details.</p>" +
                "<p>Best regards,<br>Tennis Tournament Team</p>" +
                "</div>" +
                "<div style='background-color: #f1f1f1; padding: 10px; text-align: center; font-size: 12px; color: #666;'>" +
                "<p>&copy; 2025 Tennis Tournament Application. All rights reserved.</p>" +
                "<p>This is an automated email. Please do not reply to this message.</p>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    /**
     * Send a plain text email
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Email body text
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Would have sent email to: {}, subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send simple email to: {}", to, e);
        }
    }

    /**
     * Test method to validate the email templates
     */
    public String testTemplate(String templateName, Map<String, Object> variables) {
        try {
            return processTemplate(templateName, variables);
        } catch (Exception e) {
            log.error("Template test failed: {}", e.getMessage());
            return "Template processing failed: " + e.getMessage();
        }
    }

    /**
     * Simple method to verify email configuration is working
     */
    public boolean testEmailConfiguration() {
        try {
            // Test connection to mail server by attempting to get a session
            log.info("Testing mail server connection...");

            // For JavaMailSender, we can check if it's properly configured by creating a MimeMessage
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // If we're using JavaMailSenderImpl, we can do a more thorough check
            if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl) {
                org.springframework.mail.javamail.JavaMailSenderImpl mailSenderImpl =
                        (org.springframework.mail.javamail.JavaMailSenderImpl) mailSender;

                // Log mail server properties for debugging
                log.info("Mail server host: {}", mailSenderImpl.getHost());
                log.info("Mail server port: {}", mailSenderImpl.getPort());
                log.info("Mail server username: {}", mailSenderImpl.getUsername());

                // Check if we have essential properties
                if (mailSenderImpl.getHost() == null || mailSenderImpl.getHost().isEmpty()) {
                    log.error("Mail server host is not configured");
                    return false;
                }

                // Additional validation could be done here
            }

            log.info("Mail server configuration check successful!");
            return true;
        } catch (Exception e) {
            log.error("Mail server configuration check failed: {}", e.getMessage(), e);
            return false;
        }
    }
}