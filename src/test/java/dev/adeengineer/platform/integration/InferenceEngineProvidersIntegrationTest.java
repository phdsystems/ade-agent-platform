package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.providers.BentoMLProvider;
import dev.adeengineer.llm.providers.RayServeProvider;
import dev.adeengineer.llm.providers.TextGenerationInferenceProvider;

/**
 * Integration tests for local inference engine providers (TGI, Ray Serve, BentoML). Tests actual
 * API integration with these self-hosted inference engines.
 *
 * <p>Requirements: Respective servers running on configured URLs
 *
 * <p>Tests are conditional - only run when specific environment variables are set
 */
@DisplayName("Inference Engine Providers Integration Tests")
class InferenceEngineProvidersIntegrationTest extends BaseIntegrationTest {

    @Autowired private TextGenerationInferenceProvider tgiProvider;

    @Autowired private RayServeProvider rayServeProvider;

    @Autowired private BentoMLProvider bentoMLProvider;

    // ========== Text Generation Inference (TGI) Tests ==========

    @Test
    @DisplayName("Should have TGI provider bean available")
    void shouldHaveTGIProviderBean() {
        assertThat(tgiProvider).isNotNull();
        assertThat(tgiProvider.getProviderName()).isEqualTo("tgi");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TGI_ENABLED", matches = "true")
    @DisplayName("TGI: Should generate response")
    void tgiShouldGenerateResponse() {
        // Given
        String prompt = "Say 'Hello from TGI!' and nothing else.";

        // When
        LLMResponse response = tgiProvider.generate(prompt, 0.1, 50);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        assertThat(response.provider()).isEqualTo("tgi");
        assertThat(response.usage().estimatedCost()).isEqualTo(0.0); // Free
    }

    @Test
    @DisplayName("TGI: Should perform health check")
    void tgiShouldPerformHealthCheck() {
        boolean isHealthy = tgiProvider.isHealthy();
        assertThat(isHealthy).isIn(true, false); // Just verify no exception
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TGI_ENABLED", matches = "true")
    @DisplayName("TGI: Should be healthy when server running")
    void tgiShouldBeHealthyWhenServerRunning() {
        assertThat(tgiProvider.isHealthy()).isTrue();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TGI_ENABLED", matches = "true")
    @DisplayName("TGI: Should handle code generation")
    void tgiShouldHandleCodeGeneration() {
        // Given
        String prompt = "Write a simple Python hello world function.";

        // When
        LLMResponse response = tgiProvider.generate(prompt, 0.7, 200);

        // Then
        assertThat(response.content()).isNotBlank();
        assertThat(response.content().toLowerCase()).contains("def");
    }

    // ========== Ray Serve Tests ==========

    @Test
    @DisplayName("Should have Ray Serve provider bean available")
    void shouldHaveRayServeProviderBean() {
        assertThat(rayServeProvider).isNotNull();
        assertThat(rayServeProvider.getProviderName()).isEqualTo("rayserve");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RAYSERVE_ENABLED", matches = "true")
    @DisplayName("Ray Serve: Should generate response")
    void rayServeShouldGenerateResponse() {
        // Given
        String prompt = "Say 'Hello from Ray Serve!' and nothing else.";

        // When
        LLMResponse response = rayServeProvider.generate(prompt, 0.1, 50);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        assertThat(response.provider()).isEqualTo("rayserve");
        assertThat(response.usage().estimatedCost()).isEqualTo(0.0); // Free
    }

    @Test
    @DisplayName("Ray Serve: Should perform health check")
    void rayServeShouldPerformHealthCheck() {
        boolean isHealthy = rayServeProvider.isHealthy();
        assertThat(isHealthy).isIn(true, false); // Just verify no exception
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RAYSERVE_ENABLED", matches = "true")
    @DisplayName("Ray Serve: Should be healthy when server running")
    void rayServeShouldBeHealthyWhenServerRunning() {
        assertThat(rayServeProvider.isHealthy()).isTrue();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RAYSERVE_ENABLED", matches = "true")
    @DisplayName("Ray Serve: Should provide token estimates")
    void rayServeShouldProvideTokenEstimates() {
        // Given
        String prompt = "Test";

        // When
        LLMResponse response = rayServeProvider.generate(prompt, 0.7, 20);

        // Then
        // Usage info available in metadata;
        assertThat(response.usage().totalTokens()).isGreaterThan(0);
    }

    // ========== BentoML Tests ==========

    @Test
    @DisplayName("Should have BentoML provider bean available")
    void shouldHaveBentoMLProviderBean() {
        assertThat(bentoMLProvider).isNotNull();
        assertThat(bentoMLProvider.getProviderName()).isEqualTo("bentoml");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "BENTOML_ENABLED", matches = "true")
    @DisplayName("BentoML: Should generate response")
    void bentoMLShouldGenerateResponse() {
        // Given
        String prompt = "Say 'Hello from BentoML!' and nothing else.";

        // When
        LLMResponse response = bentoMLProvider.generate(prompt, 0.1, 50);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        assertThat(response.provider()).isEqualTo("bentoml");
        assertThat(response.usage().estimatedCost()).isEqualTo(0.0); // Free
    }

    @Test
    @DisplayName("BentoML: Should perform health check")
    void bentoMLShouldPerformHealthCheck() {
        boolean isHealthy = bentoMLProvider.isHealthy();
        assertThat(isHealthy).isIn(true, false); // Just verify no exception
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "BENTOML_ENABLED", matches = "true")
    @DisplayName("BentoML: Should be healthy when server running")
    void bentoMLShouldBeHealthyWhenServerRunning() {
        assertThat(bentoMLProvider.isHealthy()).isTrue();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "BENTOML_ENABLED", matches = "true")
    @DisplayName("BentoML: Should handle different response formats")
    void bentoMLShouldHandleDifferentResponseFormats() {
        // Given
        String prompt = "Test response format";

        // When
        LLMResponse response = bentoMLProvider.generate(prompt, 0.7, 50);

        // Then - Should handle various BentoML response formats gracefully
        assertThat(response.content()).isNotBlank();
        assertThat(response.usage().totalTokens()).isGreaterThan(0);
    }

    // ========== General Tests for All Three Providers ==========

    @Test
    @DisplayName("All providers should have zero cost")
    void allProvidersShouldHaveZeroCost() {
        // When/Then - All are local/self-hosted, so all should be free
        String testPrompt = "Hi";

        if (tgiProvider.isHealthy()) {
            LLMResponse response = tgiProvider.generate(testPrompt, 0.7, 10);
            assertThat(response.usage().estimatedCost()).isEqualTo(0.0);
        }

        if (rayServeProvider.isHealthy()) {
            LLMResponse response = rayServeProvider.generate(testPrompt, 0.7, 10);
            assertThat(response.usage().estimatedCost()).isEqualTo(0.0);
        }

        if (bentoMLProvider.isHealthy()) {
            LLMResponse response = bentoMLProvider.generate(testPrompt, 0.7, 10);
            assertThat(response.usage().estimatedCost()).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("All providers should return correct provider names")
    void allProvidersShouldReturnCorrectNames() {
        assertThat(tgiProvider.getProviderName()).isEqualTo("tgi");
        assertThat(rayServeProvider.getProviderName()).isEqualTo("rayserve");
        assertThat(bentoMLProvider.getProviderName()).isEqualTo("bentoml");
    }

    @Test
    @DisplayName("All providers should have configured models")
    void allProvidersShouldHaveConfiguredModels() {
        assertThat(tgiProvider.getModel()).isNotBlank();
        assertThat(rayServeProvider.getModel()).isNotBlank();
        assertThat(bentoMLProvider.getModel()).isNotBlank();
    }
}
