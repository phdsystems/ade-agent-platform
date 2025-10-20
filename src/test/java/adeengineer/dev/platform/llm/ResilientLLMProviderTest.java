package adeengineer.dev.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import adeengineer.dev.llm.LLMProvider;
import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.llm.model.StreamingLLMResponse;
import adeengineer.dev.llm.model.UsageInfo;
import adeengineer.dev.llm.resilience.ResilientLLMProvider;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Flux;

/**
 * Unit tests for circuit breaker protection on LLM providers. Tests fault tolerance and fail-fast
 * behavior.
 */
@DisplayName("Resilient LLM Provider Tests")
class ResilientLLMProviderTest {

    private ResilientLLMProvider resilientProvider;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private MockLLMProvider mockProvider;

    @BeforeEach
    void setUp() {
        // Create circuit breaker registry with test configuration
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50) // Open after 50% failures
                        .minimumNumberOfCalls(3) // Need at least 3 calls
                        .waitDurationInOpenState(Duration.ofMillis(100))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .slidingWindowSize(5)
                        .build();

        circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
        resilientProvider = new ResilientLLMProvider(circuitBreakerRegistry);
        mockProvider = new MockLLMProvider("test-provider");
    }

    @Test
    @DisplayName("Should allow calls when circuit breaker is closed")
    void shouldAllowCallsWhenCircuitBreakerClosed() {
        // Given
        mockProvider.setShouldSucceed(true);

        // When
        LLMResponse response =
                resilientProvider.generateWithCircuitBreaker(mockProvider, "test prompt", 0.7, 100);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Mock response");
        assertThat(mockProvider.getCallCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should open circuit breaker after failure threshold")
    void shouldOpenCircuitBreakerAfterFailureThreshold() {
        // Given
        mockProvider.setShouldSucceed(false);

        // When - Make enough calls to open circuit breaker
        for (int i = 0; i < 3; i++) {
            try {
                resilientProvider.generateWithCircuitBreaker(mockProvider, "test prompt", 0.7, 100);
            } catch (Exception e) {
                // Expected failures
            }
        }

        // Then - Circuit breaker should be open
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-provider");
        assertThat(circuitBreaker.getState())
                .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);
    }

    @Test
    @DisplayName("Should fail fast when circuit breaker is open")
    void shouldFailFastWhenCircuitBreakerOpen() {
        // Given - Open the circuit breaker
        mockProvider.setShouldSucceed(false);
        for (int i = 0; i < 3; i++) {
            try {
                resilientProvider.generateWithCircuitBreaker(mockProvider, "test", 0.7, 100);
            } catch (Exception e) {
                // Expected
            }
        }

        // When/Then - Should fail immediately without calling provider
        int initialCallCount = mockProvider.getCallCount();
        assertThatThrownBy(
                        () ->
                                resilientProvider.generateWithCircuitBreaker(
                                        mockProvider, "test", 0.7, 100))
                .isInstanceOf(Exception.class);

        // Verify provider was not called (circuit breaker failed fast)
        assertThat(mockProvider.getCallCount()).isEqualTo(initialCallCount);
    }

    @Test
    @DisplayName("Should transition to half-open after wait duration")
    void shouldTransitionToHalfOpenAfterWaitDuration() throws InterruptedException {
        // Given - Open the circuit breaker
        mockProvider.setShouldSucceed(false);
        for (int i = 0; i < 3; i++) {
            try {
                resilientProvider.generateWithCircuitBreaker(mockProvider, "test", 0.7, 100);
            } catch (Exception e) {
                // Expected
            }
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-provider");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When - Wait for circuit breaker to transition to half-open
        Thread.sleep(150); // Wait duration is 100ms in config

        // Then - Circuit breaker should allow test calls
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Should close circuit breaker after successful recovery")
    void shouldCloseCircuitBreakerAfterSuccessfulRecovery() throws InterruptedException {
        // Given - Open the circuit breaker
        mockProvider.setShouldSucceed(false);
        for (int i = 0; i < 3; i++) {
            try {
                resilientProvider.generateWithCircuitBreaker(mockProvider, "test", 0.7, 100);
            } catch (Exception e) {
                // Expected
            }
        }

        // Wait for half-open state
        Thread.sleep(150);

        // When - Succeed in half-open state
        mockProvider.setShouldSucceed(true);
        for (int i = 0; i < 2; i++) { // Need 2 successful calls based on config
            resilientProvider.generateWithCircuitBreaker(mockProvider, "test", 0.7, 100);
        }

        // Then - Circuit breaker should be closed
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-provider");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Should handle streaming calls with circuit breaker")
    void shouldHandleStreamingCallsWithCircuitBreaker() {
        // Given
        mockProvider.setShouldSucceed(true);

        // When
        StreamingLLMResponse response =
                resilientProvider.generateStreamWithCircuitBreaker(
                        mockProvider, "test prompt", 0.7, 100);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.provider()).isEqualTo("test-provider");
    }

    @Test
    @DisplayName("Should track circuit breaker metrics")
    void shouldTrackCircuitBreakerMetrics() {
        // Given
        mockProvider.setShouldSucceed(true);

        // When
        for (int i = 0; i < 5; i++) {
            resilientProvider.generateWithCircuitBreaker(mockProvider, "test", 0.7, 100);
        }

        // Then
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-provider");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(5);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getFailureRate()).isEqualTo(0.0f);
    }

    // Mock LLM Provider for testing
    private static class MockLLMProvider implements LLMProvider {
        private final String name;
        private boolean shouldSucceed = true;
        private int callCount = 0;

        public MockLLMProvider(String name) {
            this.name = name;
        }

        public void setShouldSucceed(boolean shouldSucceed) {
            this.shouldSucceed = shouldSucceed;
        }

        public int getCallCount() {
            return callCount;
        }

        @Override
        public LLMResponse generate(String prompt, double temperature, int maxTokens) {
            callCount++;
            if (!shouldSucceed) {
                throw new RuntimeException("Mock provider failure");
            }
            UsageInfo usage = new UsageInfo(10, 20, 30, 0.001);
            return new LLMResponse("Mock response", usage, name, "test-model");
        }

        @Override
        public StreamingLLMResponse generateStream(
                String prompt, double temperature, int maxTokens) {
            if (!shouldSucceed) {
                throw new RuntimeException("Mock provider streaming failure");
            }
            Flux<String> stream = Flux.just("Mock", " ", "streaming", " ", "response");
            return new StreamingLLMResponse(stream, name, "test-model");
        }

        @Override
        public boolean supportsStreaming() {
            return true;
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
            return shouldSucceed;
        }
    }
}
