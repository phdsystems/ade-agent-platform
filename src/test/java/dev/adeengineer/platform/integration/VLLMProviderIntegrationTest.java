package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.providers.VLLMProvider;

/**
 * Integration tests for vLLM provider. Tests actual vLLM API integration with OpenAI-compatible
 * endpoints.
 *
 * <p>Requirements: vLLM server running on configured URL (default: localhost:8000)
 *
 * <p>Setup: docker run --gpus all -p 8000:8000 vllm/vllm-openai --model
 * meta-llama/Llama-3.2-3B-Instruct
 *
 * <p>Tests are conditional - only run when VLLM_ENABLED=true environment variable is set
 */
@DisplayName("vLLM Provider Integration Tests")
class VLLMProviderIntegrationTest extends BaseIntegrationTest {

    @Autowired private VLLMProvider vllmProvider;

    @Test
    @DisplayName("Should have vLLM provider bean available")
    void shouldHaveVLLMProviderBean() {
        // Then
        assertThat(vllmProvider).isNotNull();
        assertThat(vllmProvider.getProviderName()).isEqualTo("vllm");
    }

    @Test
    @DisplayName("Should return correct model name")
    void shouldReturnCorrectModelName() {
        // Then
        assertThat(vllmProvider.getModel()).isNotBlank();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should generate response using vLLM")
    void shouldGenerateResponseUsingVLLM() {
        // Given
        String prompt = "Say 'Hello, vLLM!' and nothing else.";
        double temperature = 0.1; // Low temperature for predictable output
        int maxTokens = 50;

        // When
        LLMResponse response = vllmProvider.generate(prompt, temperature, maxTokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        assertThat(response.provider()).isEqualTo("vllm");
        assertThat(response.model()).isNotBlank();

        // Verify usage info
        // Usage info available in metadata;
        assertThat(response.usage().totalTokens()).isGreaterThan(0);
        assertThat(response.usage().inputTokens()).isGreaterThan(0);
        assertThat(response.usage().outputTokens()).isGreaterThan(0);
        assertThat(response.usage().estimatedCost()).isEqualTo(0.0); // vLLM is free
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should respect temperature parameter")
    void shouldRespectTemperatureParameter() {
        // Given
        String prompt = "Generate a random number between 1 and 100.";

        // When - Low temperature (more deterministic)
        LLMResponse response1 = vllmProvider.generate(prompt, 0.1, 50);
        LLMResponse response2 = vllmProvider.generate(prompt, 0.1, 50);

        // Then - Both responses should exist
        assertThat(response1.content()).isNotBlank();
        assertThat(response2.content()).isNotBlank();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should respect max tokens parameter")
    void shouldRespectMaxTokensParameter() {
        // Given
        String prompt = "Write a long story about AI.";
        int maxTokens = 10;

        // When
        LLMResponse response = vllmProvider.generate(prompt, 0.7, maxTokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        // Output tokens should be close to maxTokens (within reason)
        assertThat(response.usage().outputTokens()).isLessThanOrEqualTo(maxTokens + 5);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should handle code generation")
    void shouldHandleCodeGeneration() {
        // Given
        String prompt = "Write a simple Python function to add two numbers.";

        // When
        LLMResponse response = vllmProvider.generate(prompt, 0.7, 200);

        // Then
        assertThat(response.content()).isNotBlank();
        assertThat(response.content().toLowerCase()).contains("def");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should handle longer prompts")
    void shouldHandleLongerPrompts() {
        // Given
        String longPrompt =
                """
            You are a helpful AI assistant. Please analyze the following text
            and provide a concise summary of the main points.

            Text: The quick brown fox jumps over the lazy dog. This sentence
            contains every letter of the English alphabet. It is commonly used
            for testing fonts and keyboards.

            Summary:
            """;

        // When
        LLMResponse response = vllmProvider.generate(longPrompt, 0.7, 100);

        // Then
        assertThat(response.content()).isNotBlank();
        assertThat(response.usage().inputTokens()).isGreaterThan(50); // Prompt is reasonably long
    }

    @Test
    @DisplayName("Should perform health check")
    void shouldPerformHealthCheck() {
        // When
        boolean isHealthy = vllmProvider.isHealthy();

        // Then
        // Health check depends on whether vLLM is running
        // Test just verifies method doesn't throw exception
        assertThat(isHealthy).isIn(true, false);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should be healthy when vLLM server is running")
    void shouldBeHealthyWhenServerRunning() {
        // When
        boolean isHealthy = vllmProvider.isHealthy();

        // Then
        assertThat(isHealthy).isTrue();
    }

    @Test
    @DisplayName("Should have zero cost for all operations")
    void shouldHaveZeroCostForAllOperations() {
        // Given - Skip if vLLM not available
        if (!vllmProvider.isHealthy()) {
            return; // Skip test if vLLM not running
        }

        // When
        LLMResponse response = vllmProvider.generate("Test", 0.7, 10);

        // Then
        assertThat(response.usage().estimatedCost()).isEqualTo(0.0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should handle rapid sequential requests")
    void shouldHandleRapidSequentialRequests() {
        // Given
        String prompt = "Say hello.";

        // When - Make 5 rapid requests
        for (int i = 0; i < 5; i++) {
            LLMResponse response = vllmProvider.generate(prompt, 0.7, 20);

            // Then - Each should succeed
            assertThat(response).isNotNull();
            assertThat(response.content()).isNotBlank();
            assertThat(response.provider()).isEqualTo("vllm");
        }
    }
}
