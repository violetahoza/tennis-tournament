package com.ex.tennistournament.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;

/**
 * Configuration class for Thymeleaf email templates.
 * Sets up the template resolvers and template engine for email rendering.
 */
@Configuration
public class ThymeleafConfig {

    /**
     * Creates a template resolver for email templates.
     * Configures the resolver to look for HTML templates in the /templates/email/ directory.
     *
     * @return Configured ClassLoaderTemplateResolver
     */
    @Bean
    public ClassLoaderTemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);  // Set to true in production
        resolver.setCheckExistence(true);
        resolver.setOrder(1);
        return resolver;
    }

    /**
     * Creates a default Spring template resolver for other templates.
     * This is a backup resolver that will be used if the email resolver fails.
     *
     * @return Configured SpringResourceTemplateResolver
     */
    @Bean
    public SpringResourceTemplateResolver springTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);  // Set to true in production
        resolver.setCheckExistence(true);
        resolver.setOrder(2);
        return resolver;
    }

    /**
     * Creates and configures the Thymeleaf template engine.
     * Adds both template resolvers to the engine.
     *
     * @return Configured SpringTemplateEngine
     */
    @Bean
    @Primary
    public SpringTemplateEngine thymeleafTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(emailTemplateResolver());
        templateEngine.addTemplateResolver(springTemplateResolver());
        return templateEngine;
    }
}