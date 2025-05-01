package com.ex.tennistournament.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    @Captor
    private ArgumentCaptor<MimeMessage> mimeMessageCaptor;

    private String testEmail = "test@example.com";
    private String testSubject = "Test Subject";
    private Map<String, Object> testVariables;

    @BeforeEach
    void setUp() {
        testVariables = new HashMap<>();
        testVariables.put("playerName", "John Doe");
        testVariables.put("tournamentName", "Summer Slam");
        testVariables.put("status", "ACCEPTED");

        // Set the fromEmail field using reflection
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@tennistournament.com");
        // Enable email sending for tests
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
    }

    @Test
    void sendTemplateEmail_Success() throws MessagingException {
        // Setup
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("base-template"), any(Context.class))).thenReturn("<html>Email content</html>");

        // Execute
        emailService.sendTemplateEmail(testEmail, testSubject, "registration-confirmation", testVariables);

        // Verify
        verify(mailSender).send(mimeMessageCaptor.capture());
        verify(templateEngine).process(eq("base-template"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_BaseTemplateFailsFallbackToDirectTemplate() throws MessagingException {
        // Setup
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("base-template"), any(Context.class)))
                .thenThrow(new TemplateInputException("Base template not found"));
        when(templateEngine.process(eq("registration-confirmation"), any(Context.class)))
                .thenReturn("<html>Direct template content</html>");

        // Execute
        emailService.sendTemplateEmail(testEmail, testSubject, "registration-confirmation", testVariables);

        // Verify
        verify(mailSender).send(mimeMessageCaptor.capture());
        verify(templateEngine).process(eq("registration-confirmation"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_AllTemplatesFail_UseFallback() throws MessagingException {
        // Setup
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("base-template"), any(Context.class)))
                .thenThrow(new TemplateInputException("Base template not found"));
        when(templateEngine.process(eq("registration-confirmation"), any(Context.class)))
                .thenThrow(new TemplateInputException("Template not found"));

        // Execute
        emailService.sendTemplateEmail(testEmail, testSubject, "registration-confirmation", testVariables);

        // Verify
        verify(mailSender).send(mimeMessageCaptor.capture());
        // Both template processing attempts should fail
        verify(templateEngine).process(eq("base-template"), any(Context.class));
        verify(templateEngine).process(eq("registration-confirmation"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_DisabledEmails_ShouldNotSend() {
        // Disable email sending
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        // Execute
        emailService.sendTemplateEmail(testEmail, testSubject, "registration-confirmation", testVariables);

        // Verify no interactions with mailSender
        verifyNoInteractions(mailSender);
        verifyNoInteractions(templateEngine);
    }

    @Test
    void sendTemplateEmail_MessagingException_HandleGracefully() throws MessagingException {
        // Setup
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("base-template"), any(Context.class))).thenReturn("<html>Email content</html>");
        doThrow(new MessagingException("Failed to send")).when(mailSender).send(any(MimeMessage.class));

        // Execute - should not throw exception
        assertDoesNotThrow(() ->
                emailService.sendTemplateEmail(testEmail, testSubject, "registration-confirmation", testVariables)
        );

        // Verify attempt was made
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSimpleEmail_Success() throws MessagingException {
        // Setup
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        String simpleText = "This is a simple email";

        // Execute
        emailService.sendSimpleEmail(testEmail, testSubject, simpleText);

        // Verify
        verify(mailSender).send(mimeMessageCaptor.capture());
    }

    @Test
    void sendSimpleEmail_Disabled_ShouldNotSend() {
        // Disable email sending
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        // Execute
        emailService.sendSimpleEmail(testEmail, testSubject, "Simple message");

        // Verify no interactions
        verifyNoInteractions(mailSender);
    }

    @Test
    void testTemplate_Success() {
        // Setup
        String expectedContent = "<html>Test template content</html>";
        when(templateEngine.process(eq("test-template"), any(Context.class))).thenReturn(expectedContent);

        // Execute
        String result = emailService.testTemplate("test-template", testVariables);

        // Verify
        assertEquals(expectedContent, result);
        verify(templateEngine).process(eq("test-template"), any(Context.class));
    }

    @Test
    void testTemplate_Exception_ReturnsErrorMessage() {
        // Setup
        when(templateEngine.process(eq("invalid-template"), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // Execute
        String result = emailService.testTemplate("invalid-template", testVariables);

        // Verify
        assertTrue(result.contains("Template processing failed"));
    }

    @Test
    void testEmailConfiguration_Success() {
        // Setup
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Configure JavaMailSenderImpl for additional coverage
        JavaMailSenderImpl mailSenderImpl = mock(JavaMailSenderImpl.class);
        when(mailSenderImpl.getHost()).thenReturn("smtp.example.com");
        when(mailSenderImpl.getPort()).thenReturn(587);
        when(mailSenderImpl.getUsername()).thenReturn("user@example.com");
        when(mailSenderImpl.createMimeMessage()).thenReturn(mimeMessage);

        // Replace the mock with our custom implementation
        ReflectionTestUtils.setField(emailService, "mailSender", mailSenderImpl);

        // Execute
        boolean result = emailService.testEmailConfiguration();

        // Verify
        assertTrue(result);

        // Reset to original mock for other tests
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
    }

    @Test
    void testEmailConfiguration_MissingHost_ReturnsFalse() {
        // Setup
        JavaMailSenderImpl mailSenderImpl = mock(JavaMailSenderImpl.class);
        when(mailSenderImpl.getHost()).thenReturn("");  // Empty host
        when(mailSenderImpl.createMimeMessage()).thenReturn(mimeMessage);

        // Replace the mock
        ReflectionTestUtils.setField(emailService, "mailSender", mailSenderImpl);

        // Execute
        boolean result = emailService.testEmailConfiguration();

        // Verify
        assertFalse(result);

        // Reset
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
    }

    @Test
    void testEmailConfiguration_ExceptionThrown_ReturnsFalse() {
        // Setup
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Connection failed"));

        // Execute
        boolean result = emailService.testEmailConfiguration();

        // Verify
        assertFalse(result);
    }
}