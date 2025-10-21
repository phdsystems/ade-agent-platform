package dev.adeengineer.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

/**
 * Main application class for the ade Agent Platform. Generic, domain-agnostic multi-agent AI
 * orchestration platform.
 */
@Slf4j
@SpringBootApplication
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
