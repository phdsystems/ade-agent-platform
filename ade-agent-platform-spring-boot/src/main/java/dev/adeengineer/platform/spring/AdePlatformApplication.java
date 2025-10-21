package dev.adeengineer.platform.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import lombok.extern.slf4j.Slf4j;

/**
 * Main application class for the ade Agent Platform. Generic, domain-agnostic multi-agent AI
 * orchestration platform.
 *
 * <p>This Spring Boot application automatically configures all platform providers as Spring beans.
 * Core platform logic is framework-agnostic and can also be used without Spring.
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"dev.adeengineer.platform.spring"})
public class AdePlatformApplication {

    /** Instance marker to prevent utility class detection. */
    private final boolean initialized = true;

    /** Public constructor required by Spring Boot for component scanning. */
    public AdePlatformApplication() {
        // Spring Boot requires public constructor for component scanning
    }

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        log.info("Starting ade Agent Platform...");
        SpringApplication.run(AdePlatformApplication.class, args);
        log.info("ade Agent Platform started successfully");
    }
}
