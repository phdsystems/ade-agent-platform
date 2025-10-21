package dev.adeengineer.platform.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests. Provides Spring context without web server, using REAL LLM
 * provider (Ollama). Tests multi-component interactions at the service layer with actual API calls.
 *
 * <p>Unlike E2E tests which mock LLM providers, integration tests use a real provider to validate
 * error handling, logging, and provider failover behavior with actual errors.
 *
 * <p>Configuration: Uses Ollama as primary provider (local, no API key required). See
 * application-integrationtest.yml for provider configuration.
 */
@SpringBootTest
@ActiveProfiles("integrationtest")
@Import(IntegrationTestConfiguration.class)
public abstract class BaseIntegrationTest {
    // No mocks - uses real Ollama provider from application-integrationtest.yml
}
