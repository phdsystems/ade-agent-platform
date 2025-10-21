package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.LLMProviderFactory;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

/**
 * Integration tests for LLM provider selection and failover logic. Tests resilience and provider
 * health checks.
 *
 * <p>Note: Extends BaseProviderFailoverTest (with mocked providers) rather than BaseIntegrationTest
 * to enable testing different provider health scenarios.
 */
@DisplayName("LLM Provider Failover Integration Tests")
class LLMProviderFailoverIntegrationTest extends BaseProviderFailoverTest {

    @Autowired private LLMProviderFactory factory;

    @Test
    @DisplayName("Should select primary provider when healthy")
    void shouldSelectPrimaryProviderWhenHealthy() {
        // Given
        when(anthropicProvider.isHealthy()).thenReturn(true);
        when(openAIProvider.isHealthy()).thenReturn(true);
        when(ollamaProvider.isHealthy()).thenReturn(true);

        // When
        LLMProvider provider = factory.getProviderWithFailover();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getProviderName()).isEqualTo("anthropic-test");
    }

    @Test
    @DisplayName("Should failover to OpenAI when Anthropic is unhealthy")
    void shouldFailoverToOpenAIWhenAnthropicUnhealthy() {
        // Given
        when(anthropicProvider.isHealthy()).thenReturn(false);
        when(openAIProvider.isHealthy()).thenReturn(true);
        when(ollamaProvider.isHealthy()).thenReturn(false);

        // When
        LLMProvider provider = factory.getProviderWithFailover();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getProviderName()).isEqualTo("openai-test");
    }

    @Test
    @DisplayName("Should failover to Ollama when Anthropic and OpenAI are unhealthy")
    void shouldFailoverToOllamaWhenOthersUnhealthy() {
        // Given
        when(anthropicProvider.isHealthy()).thenReturn(false);
        when(openAIProvider.isHealthy()).thenReturn(false);
        when(ollamaProvider.isHealthy()).thenReturn(true);

        // When
        LLMProvider provider = factory.getProviderWithFailover();

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider.getProviderName()).isEqualTo("ollama-test");
    }

    @Test
    @DisplayName("Should return primary provider when all providers are unhealthy")
    void shouldReturnPrimaryProviderWhenAllUnhealthy() {
        // Given - All providers unhealthy
        when(anthropicProvider.isHealthy()).thenReturn(false);
        when(openAIProvider.isHealthy()).thenReturn(false);
        when(ollamaProvider.isHealthy()).thenReturn(false);

        // When
        LLMProvider provider = factory.getProviderWithFailover();

        // Then - Should return primary (Anthropic) as fallback
        assertThat(provider).isNotNull();
        assertThat(provider.getProviderName()).isEqualTo("anthropic-test");
    }

    @Test
    @DisplayName("Should get specific provider by name")
    void shouldGetSpecificProviderByName() {
        // When
        LLMProvider anthropic = factory.getProvider("anthropic");
        LLMProvider openai = factory.getProvider("openai");
        LLMProvider ollama = factory.getProvider("ollama");

        // Then
        assertThat(anthropic).isNotNull();
        assertThat(anthropic.getProviderName()).isEqualTo("anthropic-test");

        assertThat(openai).isNotNull();
        assertThat(openai.getProviderName()).isEqualTo("openai-test");

        assertThat(ollama).isNotNull();
        assertThat(ollama.getProviderName()).isEqualTo("ollama-test");
    }

    @Test
    @DisplayName("Should throw exception for unknown provider name")
    void shouldThrowExceptionForUnknownProviderName() {
        // When/Then
        assertThatThrownBy(() -> factory.getProvider("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown provider");
    }

    @Test
    @DisplayName("Should use healthy provider for generation")
    void shouldUseHealthyProviderForGeneration() {
        // Given
        when(anthropicProvider.isHealthy()).thenReturn(true);

        UsageInfo testUsage = new UsageInfo(10, 20, 30, 0.001);
        LLMResponse expectedResponse =
                new LLMResponse("Generated content", testUsage, "anthropic-test", "claude-test");
        when(anthropicProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(expectedResponse);

        // When
        LLMProvider provider = factory.getProviderWithFailover();
        LLMResponse response = provider.generate("Test prompt", 0.7, 1000);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Generated content");
        assertThat(response.provider()).isEqualTo("anthropic-test");
        assertThat(response.model()).isEqualTo("claude-test");
    }

    @Test
    @DisplayName("Should failover during generation if primary fails")
    void shouldFailoverDuringGenerationIfPrimaryFails() {
        // Given - Anthropic unhealthy, OpenAI healthy
        when(anthropicProvider.isHealthy()).thenReturn(false);
        when(openAIProvider.isHealthy()).thenReturn(true);

        UsageInfo testUsage = new UsageInfo(15, 25, 40, 0.002);
        LLMResponse expectedResponse =
                new LLMResponse("Failover content", testUsage, "openai-test", "gpt-test");
        when(openAIProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(expectedResponse);

        // When
        LLMProvider provider = factory.getProviderWithFailover();
        LLMResponse response = provider.generate("Test prompt", 0.7, 1000);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Failover content");
        assertThat(response.provider()).isEqualTo("openai-test");
        assertThat(response.model()).isEqualTo("gpt-test");
    }

    @Test
    @DisplayName("Should get primary provider based on configuration")
    void shouldGetPrimaryProviderBasedOnConfiguration() {
        // When
        LLMProvider primary = factory.getPrimaryProvider();

        // Then
        assertThat(primary).isNotNull();
        // Primary is Anthropic for failover tests (mocked)
        assertThat(primary.getProviderName()).isEqualTo("anthropic-test");
    }

    @Test
    @DisplayName("Should maintain failover order consistency")
    void shouldMaintainFailoverOrderConsistency() {
        // Given - Test failover sequence
        when(anthropicProvider.isHealthy()).thenReturn(false);
        when(openAIProvider.isHealthy()).thenReturn(false);
        when(ollamaProvider.isHealthy()).thenReturn(true);

        // When - Call failover multiple times
        LLMProvider provider1 = factory.getProviderWithFailover();
        LLMProvider provider2 = factory.getProviderWithFailover();
        LLMProvider provider3 = factory.getProviderWithFailover();

        // Then - Should consistently select Ollama
        assertThat(provider1.getProviderName()).isEqualTo("ollama-test");
        assertThat(provider2.getProviderName()).isEqualTo("ollama-test");
        assertThat(provider3.getProviderName()).isEqualTo("ollama-test");
    }
}
