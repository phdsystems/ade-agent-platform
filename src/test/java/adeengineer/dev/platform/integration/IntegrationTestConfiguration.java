package adeengineer.dev.platform.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Test configuration for integration tests. Uses domain plugin system for agent loading. */
@TestConfiguration
@ComponentScan(basePackages = "com.rolemanager")
public class IntegrationTestConfiguration {
    // No custom beans needed - uses AppConfig domain plugin initialization
}
