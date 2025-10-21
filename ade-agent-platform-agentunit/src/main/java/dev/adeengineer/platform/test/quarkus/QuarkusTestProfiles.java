package dev.adeengineer.platform.test.quarkus;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Collection of common Quarkus test profiles for AgentUnit tests.
 *
 * <p>Test profiles allow customizing application configuration for specific test scenarios.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @QuarkusTest
 * @TestProfile(QuarkusTestProfiles.MockLLMProfile.class)
 * class MyServiceTest {
 *     // Test will use mock LLM provider configuration
 * }
 * }</pre>
 */
public class QuarkusTestProfiles {

    /**
     * Test profile that configures mock LLM provider settings.
     *
     * <p>Use this profile when testing components that depend on LLM providers but don't need real
     * API calls.
     */
    public static class MockLLMProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "llm.provider", "mock",
                    "llm.model", "mock-model",
                    "llm.temperature", "0.7",
                    "llm.max-tokens", "500");
        }
    }

    /**
     * Test profile that disables external services (Redis, database, etc.).
     *
     * <p>Use this profile for pure unit tests that should run without external dependencies.
     */
    public static class IsolatedProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.redis.devservices.enabled", "false",
                    "quarkus.datasource.devservices.enabled", "false",
                    "quarkus.cache.enabled", "false");
        }
    }

    /**
     * Test profile for fast unit tests.
     *
     * <p>Disables heavy features like metrics, health checks, and tracing for faster test
     * execution.
     */
    public static class FastTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.smallrye-health.enabled", "false",
                    "quarkus.micrometer.enabled", "false",
                    "quarkus.otel.enabled", "false",
                    "quarkus.log.level", "WARN");
        }
    }

    private QuarkusTestProfiles() {
        // Utility class
    }
}
