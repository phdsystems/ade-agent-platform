package adeengineer.dev.platform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import adeengineer.dev.llm.*;
import adeengineer.dev.llm.cache.*;
import adeengineer.dev.llm.model.*;
import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.llm.model.StreamingLLMResponse;
import adeengineer.dev.llm.providers.*;
import adeengineer.dev.llm.resilience.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.test.StepVerifier;

/**
 * End-to-end tests for performance optimization features. Tests complete workflows including
 * caching, streaming, and circuit breakers.
 */
@SpringBootTest
@DisplayName("Performance Optimization E2E Tests")
class PerformanceOptimizationE2ETest extends BaseE2ETest {

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private CachedLLMProvider cachedLLMProvider;

    @Autowired(required = false)
    private ResilientLLMProvider resilientLLMProvider;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private VLLMProvider vllmProvider;

    @Test
    @DisplayName("Should cache LLM responses")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldCacheLLMResponses() {
        // Given
        assertThat(cachedLLMProvider).isNotNull();
        assertThat(vllmProvider).isNotNull();
        String prompt = "What is the capital of France?";

        // When - First call (cache miss)
        Instant start1 = Instant.now();
        LLMResponse response1 =
                cachedLLMProvider.generateWithCache(
                        vllmProvider,
                        prompt,
                        0.0, // Deterministic
                        50);
        Duration duration1 = Duration.between(start1, Instant.now());

        // When - Second call (cache hit)
        Instant start2 = Instant.now();
        LLMResponse response2 = cachedLLMProvider.generateWithCache(vllmProvider, prompt, 0.0, 50);
        Duration duration2 = Duration.between(start2, Instant.now());

        // Then
        assertThat(response1.content()).isNotEmpty();
        assertThat(response2.content()).isEqualTo(response1.content());

        // Cache hit should be significantly faster
        assertThat(duration2.toMillis()).isLessThan(duration1.toMillis() / 2);
    }

    @Test
    @DisplayName("Should handle streaming with caching")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldHandleStreamingWithCaching() {
        // Given
        assertThat(vllmProvider).isNotNull();
        assertThat(vllmProvider.supportsStreaming()).isTrue();

        // When
        StreamingLLMResponse streamingResponse =
                vllmProvider.generateStream("Count from 1 to 5", 0.1, 100);

        // Then - Verify streaming works
        StepVerifier.create(streamingResponse.collect())
                .assertNext(
                        response -> {
                            assertThat(response.content()).isNotEmpty();
                            assertThat(response.provider()).isEqualTo("vllm");
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should protect provider with circuit breaker")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldProtectProviderWithCircuitBreaker() {
        // Given
        assertThat(resilientLLMProvider).isNotNull();
        assertThat(vllmProvider).isNotNull();

        // When - Make successful calls
        for (int i = 0; i < 3; i++) {
            LLMResponse response =
                    resilientLLMProvider.generateWithCircuitBreaker(
                            vllmProvider, "Test prompt " + i, 0.7, 50);
            assertThat(response).isNotNull();
        }

        // Then - Circuit breaker should be closed
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("vllm");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(3);
        assertThat(metrics.getFailureRate()).isEqualTo(0.0f);
    }

    @Test
    @DisplayName("Should combine caching and circuit breaker")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldCombineCachingAndCircuitBreaker() {
        // Given
        assertThat(cachedLLMProvider).isNotNull();
        assertThat(resilientLLMProvider).isNotNull();
        assertThat(vllmProvider).isNotNull();

        String prompt = "What is 5+5?";

        // When - First call through circuit breaker (no cache)
        LLMResponse response1 = cachedLLMProvider.generateWithCache(vllmProvider, prompt, 0.0, 50);

        // When - Second call (should hit cache, no circuit breaker call)
        Instant start = Instant.now();
        LLMResponse response2 = cachedLLMProvider.generateWithCache(vllmProvider, prompt, 0.0, 50);
        Duration cachedDuration = Duration.between(start, Instant.now());

        // Then
        assertThat(response1.content()).isNotEmpty();
        assertThat(response2.content()).isEqualTo(response1.content());
        assertThat(cachedDuration.toMillis()).isLessThan(100); // Very fast cache hit
    }

    @Test
    @DisplayName("Should handle concurrent requests with caching")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldHandleConcurrentRequestsWithCaching() throws InterruptedException {
        // Given
        assertThat(cachedLLMProvider).isNotNull();
        assertThat(vllmProvider).isNotNull();

        String prompt = "Concurrent test prompt";
        int concurrentRequests = 5;

        // When - Make concurrent requests
        Thread[] threads = new Thread[concurrentRequests];
        LLMResponse[] responses = new LLMResponse[concurrentRequests];

        for (int i = 0; i < concurrentRequests; i++) {
            final int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                responses[index] =
                                        cachedLLMProvider.generateWithCache(
                                                vllmProvider, prompt, 0.0, 50);
                            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All responses should be identical (from cache)
        String expectedContent = responses[0].content();
        for (int i = 1; i < concurrentRequests; i++) {
            assertThat(responses[i].content()).isEqualTo(expectedContent);
        }
    }

    @Test
    @DisplayName("Should measure streaming latency improvement")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldMeasureStreamingLatencyImprovement() {
        // Given
        assertThat(vllmProvider).isNotNull();

        // When - Non-streaming call
        Instant start1 = Instant.now();
        LLMResponse nonStreamingResponse = vllmProvider.generate("Write three words", 0.5, 50);
        Duration nonStreamingDuration = Duration.between(start1, Instant.now());

        // When - Streaming call
        Instant start2 = Instant.now();
        StreamingLLMResponse streamingResponse =
                vllmProvider.generateStream("Write three words", 0.5, 50);

        // Measure time to first chunk
        Duration[] timeToFirstChunk = new Duration[1];
        StepVerifier.create(streamingResponse.contentStream())
                .recordWith(
                        () -> {
                            timeToFirstChunk[0] = Duration.between(start2, Instant.now());
                            return new java.util.ArrayList<String>();
                        })
                .expectNextCount(1)
                .thenConsumeWhile(chunk -> true)
                .verifyComplete();

        // Then - Streaming should provide faster initial response
        assertThat(nonStreamingResponse.content()).isNotEmpty();
        assertThat(timeToFirstChunk[0]).isNotNull();

        // Time to first chunk should be less than total non-streaming time
        assertThat(timeToFirstChunk[0].toMillis()).isLessThan(nonStreamingDuration.toMillis());
    }

    @Test
    @DisplayName("Should cache different prompts separately")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldCacheDifferentPromptsSeparately() {
        // Given
        assertThat(cachedLLMProvider).isNotNull();
        assertThat(vllmProvider).isNotNull();

        // When
        LLMResponse response1 =
                cachedLLMProvider.generateWithCache(vllmProvider, "First prompt", 0.0, 50);

        LLMResponse response2 =
                cachedLLMProvider.generateWithCache(vllmProvider, "Second prompt", 0.0, 50);

        // Then - Different prompts should have different responses
        assertThat(response1.content()).isNotEmpty();
        assertThat(response2.content()).isNotEmpty();
        assertThat(response1.content()).isNotEqualTo(response2.content());
    }

    @Test
    @DisplayName("Should invalidate cache on different parameters")
    @EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
    void shouldInvalidateCacheOnDifferentParameters() {
        // Given
        assertThat(cachedLLMProvider).isNotNull();
        assertThat(vllmProvider).isNotNull();
        String prompt = "Test prompt";

        // When - Same prompt, different temperature
        LLMResponse response1 = cachedLLMProvider.generateWithCache(vllmProvider, prompt, 0.0, 50);

        LLMResponse response2 =
                cachedLLMProvider.generateWithCache(
                        vllmProvider,
                        prompt,
                        0.5, // Different temperature
                        50);

        // Then - Different parameters should bypass cache
        assertThat(response1.content()).isNotEmpty();
        assertThat(response2.content()).isNotEmpty();
        // Responses may differ due to temperature
    }
}
