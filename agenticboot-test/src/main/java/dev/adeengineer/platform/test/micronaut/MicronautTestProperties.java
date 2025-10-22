package dev.adeengineer.platform.test.micronaut;

import java.util.Map;

import io.micronaut.test.support.TestPropertyProvider;

/**
 * Collection of common Micronaut test property providers for AgentUnit tests.
 *
 * <p>Test property providers allow customizing application configuration for specific test
 * scenarios.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @MicronautTest
 * class MyServiceTest implements MicronautTestProperties.MockLLMProperties {
 *
 *     @Inject
 *     MyService myService;
 *
 *     @Test
 *     void shouldUseTestProperties() {
 *         // Test will use mock LLM provider configuration
 *     }
 * }
 * }</pre>
 */
public class MicronautTestProperties {

    /**
     * Test properties that configure mock LLM provider settings.
     *
     * <p>Use this when testing components that depend on LLM providers but don't need real API
     * calls.
     */
    public interface MockLLMProperties extends TestPropertyProvider {

        @Override
        default Map<String, String> getProperties() {
            return Map.of(
                    "llm.provider", "mock",
                    "llm.model", "mock-model",
                    "llm.temperature", "0.7",
                    "llm.max-tokens", "500");
        }
    }

    /**
     * Test properties that disable external services (Redis, database, etc.).
     *
     * <p>Use this for pure unit tests that should run without external dependencies.
     */
    public interface IsolatedProperties extends TestPropertyProvider {

        @Override
        default Map<String, String> getProperties() {
            return Map.of(
                    "redis.enabled", "false",
                    "datasources.default.enabled", "false",
                    "micronaut.caches.enabled", "false");
        }
    }

    /**
     * Test properties for fast unit tests.
     *
     * <p>Disables heavy features like metrics, health checks, and tracing for faster test
     * execution.
     */
    public interface FastTestProperties extends TestPropertyProvider {

        @Override
        default Map<String, String> getProperties() {
            return Map.of(
                    "endpoints.health.enabled", "false",
                    "micronaut.metrics.enabled", "false",
                    "tracing.enabled", "false",
                    "logger.levels.root", "WARN");
        }
    }

    /**
     * Combined properties for isolated and fast tests.
     *
     * <p>Useful for the fastest possible unit tests with no external dependencies.
     */
    public interface FastIsolatedProperties extends TestPropertyProvider {

        @Override
        default Map<String, String> getProperties() {
            return Map.of(
                    // Isolated
                    "redis.enabled", "false",
                    "datasources.default.enabled", "false",
                    "micronaut.caches.enabled", "false",
                    // Fast
                    "endpoints.health.enabled", "false",
                    "micronaut.metrics.enabled", "false",
                    "tracing.enabled", "false",
                    "logger.levels.root", "WARN");
        }
    }

    private MicronautTestProperties() {
        // Utility class
    }
}
