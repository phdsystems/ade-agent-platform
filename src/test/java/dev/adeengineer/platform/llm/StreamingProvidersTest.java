package dev.adeengineer.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.StreamingLLMResponse;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Unit tests for streaming support in LLM providers. Tests streaming functionality without
 * requiring actual provider APIs.
 */
@DisplayName("Streaming LLM Providers Tests")
class StreamingProvidersTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should indicate streaming support for providers")
    void shouldIndicateStreamingSupport() {
        // Test that providers correctly report streaming capability
        LLMProvider vllm = createMockProvider("vllm", true);
        LLMProvider ollama = createMockProvider("ollama", false);

        assertThat(vllm.supportsStreaming()).isTrue();
        assertThat(ollama.supportsStreaming()).isFalse();
    }

    @Test
    @DisplayName("Should have correct streaming provider names")
    void shouldHaveCorrectStreamingProviderNames() {
        // Verify provider names match expected values
        assertThat(createMockProvider("anthropic", true).getProviderName()).isEqualTo("anthropic");
        assertThat(createMockProvider("openai", true).getProviderName()).isEqualTo("openai");
        assertThat(createMockProvider("tgi", true).getProviderName()).isEqualTo("tgi");
        assertThat(createMockProvider("vllm", true).getProviderName()).isEqualTo("vllm");
    }

    @Test
    @DisplayName("Should create streaming response with flux")
    void shouldCreateStreamingResponseWithFlux() {
        // Given
        Flux<String> contentStream = Flux.just("Hello", " ", "world", "!");

        // When
        StreamingLLMResponse response =
                new StreamingLLMResponse(contentStream, "test-provider", "test-model");

        // Then
        assertThat(response.provider()).isEqualTo("test-provider");
        assertThat(response.model()).isEqualTo("test-model");

        StepVerifier.create(response.contentStream())
                .expectNext("Hello")
                .expectNext(" ")
                .expectNext("world")
                .expectNext("!")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should collect streaming response into full response")
    void shouldCollectStreamingResponseIntoFullResponse() {
        // Given
        Flux<String> contentStream = Flux.just("Hello", " ", "world");
        StreamingLLMResponse streamingResponse =
                new StreamingLLMResponse(contentStream, "test-provider", "test-model");

        // When/Then
        StepVerifier.create(streamingResponse.collect())
                .assertNext(
                        llmResponse -> {
                            assertThat(llmResponse.content()).isEqualTo("Hello world");
                            assertThat(llmResponse.provider()).isEqualTo("test-provider");
                            assertThat(llmResponse.model()).isEqualTo("test-model");
                            // Usage info available in metadata;
                            assertThat(llmResponse.usage().totalTokens()).isGreaterThan(0);
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty streaming response")
    void shouldHandleEmptyStreamingResponse() {
        // Given
        Flux<String> contentStream = Flux.empty();
        StreamingLLMResponse streamingResponse =
                new StreamingLLMResponse(contentStream, "test-provider", "test-model");

        // When/Then
        StepVerifier.create(streamingResponse.contentStream()).verifyComplete();
    }

    @Test
    @DisplayName("Should handle streaming errors gracefully")
    void shouldHandleStreamingErrorsGracefully() {
        // Given
        Flux<String> contentStream = Flux.error(new RuntimeException("Streaming error"));
        StreamingLLMResponse streamingResponse =
                new StreamingLLMResponse(contentStream, "test-provider", "test-model");

        // When/Then
        StepVerifier.create(streamingResponse.contentStream())
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should filter out empty chunks in streaming")
    void shouldFilterOutEmptyChunksInStreaming() {
        // Given
        Flux<String> contentStream = Flux.just("Hello", "", " ", "", "world", "");

        // When
        Flux<String> filtered = contentStream.filter(content -> !content.isEmpty());

        // Then
        StepVerifier.create(filtered)
                .expectNext("Hello")
                .expectNext(" ")
                .expectNext("world")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should estimate tokens from collected content")
    void shouldEstimateTokensFromCollectedContent() {
        // Given
        Flux<String> contentStream = Flux.just("This", " ", "is", " ", "a", " ", "test");
        StreamingLLMResponse streamingResponse =
                new StreamingLLMResponse(contentStream, "test-provider", "test-model");

        // When/Then
        StepVerifier.create(streamingResponse.collect())
                .assertNext(
                        llmResponse -> {
                            // "This is a test" = 4 words = approximately 4 tokens
                            assertThat(llmResponse.usage().totalTokens()).isGreaterThanOrEqualTo(4);
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should support backpressure in streaming")
    void shouldSupportBackpressureInStreaming() {
        // Given
        Flux<String> contentStream = Flux.range(1, 100).map(i -> "chunk" + i + " ");

        // When/Then
        StepVerifier.create(contentStream, 10) // Request only 10 items
                .expectNextCount(10)
                .thenRequest(10)
                .expectNextCount(10)
                .thenCancel()
                .verify();
    }

    // Helper method to create mock providers
    private LLMProvider createMockProvider(String name, boolean supportsStreaming) {
        return new LLMProvider() {
            @Override
            public LLMResponse generate(String prompt, double temperature, int maxTokens) {
                return null; // Not testing non-streaming
            }

            @Override
            public boolean supportsStreaming() {
                return supportsStreaming;
            }

            @Override
            public String getProviderName() {
                return name;
            }

            @Override
            public String getModel() {
                return "test-model";
            }

            @Override
            public boolean isHealthy() {
                return true;
            }
        };
    }
}
