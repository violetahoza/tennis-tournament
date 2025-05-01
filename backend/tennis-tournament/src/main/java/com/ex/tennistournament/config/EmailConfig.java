package com.ex.tennistournament.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Properties;

/**
 * Configuration class for email settings.
 * Sets up the JavaMailSender for sending emails with proper SMTP settings.
 */
@Configuration
@EnableAsync
public class EmailConfig {

    @Value("${spring.mail.host:smtp.example.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:noreply@tennistournament.com}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String starttls;

    @Value("${tennis.app.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Creates and configures the JavaMailSender bean.
     * Uses property values from application.properties.
     *
     * @return Configured JavaMailSenderImpl
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Basic settings
        mailSender.setHost(host);
        mailSender.setPort(port);

        // Only set username and password if email is enabled to avoid log warnings
        if (emailEnabled) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
        }

        // Mail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);

        // Debugging (only in dev)
        props.put("mail.debug", "true");

        return mailSender;
    }
}