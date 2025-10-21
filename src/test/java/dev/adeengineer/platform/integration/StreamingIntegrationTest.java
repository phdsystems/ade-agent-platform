package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.adeengineer.llm.*;
import dev.adeengineer.llm.cache.*;
import dev.adeengineer.llm.model.*;
import dev.adeengineer.llm.model.StreamingLLMResponse;
import dev.adeengineer.llm.providers.*;
import dev.adeengineer.llm.resilience.*;
import reactor.test.StepVerifier;

/**
 * Integration tests for streaming functionality across all providers. Tests require actual provider
 * infrastructure to be running.
 */
@SpringBootTest
@DisplayName("Streaming Integration Tests")
class StreamingIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private VLLMProvider vllmProvider;

    @Autowired(required = false)
    private TextGenerationInferenceProvider tgiProvider;

    @Autowired(required = false)
    private AnthropicProvider anthropicProvider;

    @Autowired(required = false)
    private OpenAIProvider openaiProvider;

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should stream responses from vLLM")
    void shouldStreamResponsesFromVLLM() {
        // Given
        assertThat(vllmProvider).isNotNull();
        assertThat(vllmProvider.supportsStreaming()).isTrue();

        // When
        StreamingLLMResponse response = vllmProvider.generateStream("Count from 1 to 3", 0.1, 50);

        // Then
        assertThat(response.provider()).isEqualTo("vllm");

        StepVerifier.create(response.contentStream())
                .expectNextCount(1) // At least one chunk
                .thenConsumeWhile(chunk -> !chunk.isEmpty()) // Consume all chunks
                .verifyComplete();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TGI_ENABLED", matches = "true")
    @DisplayName("Should stream responses from TGI")
    void shouldStreamResponsesFromTGI() {
        // Given
        assertThat(tgiProvider).isNotNull();
        assertThat(tgiProvider.supportsStreaming()).isTrue();

        // When
        StreamingLLMResponse response = tgiProvider.generateStream("Say hello", 0.1, 50);

        // Then
        assertThat(response.provider()).isEqualTo("tgi");

        StepVerifier.create(response.contentStream())
                .expectNextCount(1)
                .thenConsumeWhile(chunk -> !chunk.isEmpty())
                .verifyComplete();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    @DisplayName("Should stream responses from Anthropic")
    void shouldStreamResponsesFromAnthropic() {
        // Given
        assertThat(anthropicProvider).isNotNull();
        assertThat(anthropicProvider.supportsStreaming()).isTrue();

        // When
        StreamingLLMResponse response =
                anthropicProvider.generateStream("Say hello in one word", 0.1, 20);

        // Then
        assertThat(response.provider()).isEqualTo("anthropic");
        assertThat(response.model()).contains("claude");

        StepVerifier.create(response.contentStream())
                .expectNextCount(1)
                .thenConsumeWhile(chunk -> !chunk.isEmpty())
                .verifyComplete();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    @DisplayName("Should stream responses from OpenAI")
    void shouldStreamResponsesFromOpenAI() {
        // Given
        assertThat(openaiProvider).isNotNull();
        assertThat(openaiProvider.supportsStreaming()).isTrue();

        // When
        StreamingLLMResponse response =
                openaiProvider.generateStream("Say hello in one word", 0.1, 20);

        // Then
        assertThat(response.provider()).isEqualTo("openai");
        assertThat(response.model()).contains("gpt");

        StepVerifier.create(response.contentStream())
                .expectNextCount(1)
                .thenConsumeWhile(chunk -> !chunk.isEmpty())
                .verifyComplete();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should collect streaming response into full response")
    void shouldCollectStreamingResponseIntoFullResponse() {
        // Given
        assertThat(vllmProvider).isNotNull();

        // When
        StreamingLLMResponse streamingResponse =
                vllmProvider.generateStream("What is 2+2?", 0.1, 100);

        // Then
        StepVerifier.create(streamingResponse.collect())
                .assertNext(
                        llmResponse -> {
                            assertThat(llmResponse.content()).isNotEmpty();
                            assertThat(llmResponse.provider()).isEqualTo("vllm");
                            // Usage info available in metadata;
                            assertThat(llmResponse.usage().totalTokens()).isGreaterThan(0);
                        })
                .verifyComplete();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should handle backpressure in streaming")
    void shouldHandleBackpressureInStreaming() {
        // Given
        assertThat(vllmProvider).isNotNull();

        // When
        StreamingLLMResponse response =
                vllmProvider.generateStream("Write a short story", 0.7, 200);

        // Then - Request chunks slowly to test backpressure
        StepVerifier.create(response.contentStream(), 1)
                .expectNextCount(1)
                .thenRequest(5)
                .expectNextCount(5)
                .thenCancel() // Cancel after consuming some chunks
                .verify(Duration.ofSeconds(30));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should stream with different temperatures")
    void shouldStreamWithDifferentTemperatures() {
        // Given
        assertThat(vllmProvider).isNotNull();

        // When - Low temperature (deterministic)
        StreamingLLMResponse lowTempResponse = vllmProvider.generateStream("Say 'test'", 0.0, 10);

        // When - High temperature (creative)
        StreamingLLMResponse highTempResponse = vllmProvider.generateStream("Say 'test'", 1.0, 10);

        // Then - Both should complete successfully
        StepVerifier.create(lowTempResponse.collect())
                .assertNext(response -> assertThat(response.content()).isNotEmpty())
                .verifyComplete();

        StepVerifier.create(highTempResponse.collect())
                .assertNext(response -> assertThat(response.content()).isNotEmpty())
                .verifyComplete();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    @DisplayName("Should handle empty streaming response gracefully")
    void shouldHandleEmptyStreamingResponseGracefully() {
        // Given
        assertThat(vllmProvider).isNotNull();

        // When - Very short max tokens
        StreamingLLMResponse response = vllmProvider.generateStream("Hi", 0.1, 1);

        // Then - Should complete without errors
        StepVerifier.create(response.collect())
                .assertNext(
                        llmResponse -> {
                            // May be empty or very short
                            assertThat(llmResponse).isNotNull();
                        })
                .verifyComplete();
    }
}
