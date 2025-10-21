package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.adeengineer.embeddings.EmbeddingsProvider;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.providers.evaluation.LLMEvaluationProvider;
import dev.adeengineer.platform.providers.memory.InMemoryMemoryProvider;
import dev.adeengineer.platform.providers.orchestration.SimpleOrchestrationProvider;
import dev.adeengineer.platform.providers.tools.SimpleToolProvider;

/**
 * Integration tests for provider implementations.
 *
 * <p>Tests that provider implementations work correctly with their dependencies.
 */
@DisplayName("Provider Integration Tests")
class ProviderIntegrationTest {

    private LLMProvider mockLLMProvider;
    private EmbeddingsProvider mockEmbeddingsProvider;

    @BeforeEach
    void setUp() {
        mockLLMProvider = Mockito.mock(LLMProvider.class);
        mockEmbeddingsProvider = Mockito.mock(EmbeddingsProvider.class);

        // Setup mock responses
        Mockito.when(
                        mockLLMProvider.generate(
                                Mockito.anyString(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(
                        new LLMResponse(
                                "Mock response",
                                new UsageInfo(10, 20, 30, 0.001),
                                "mock-provider",
                                "mock-model"));
    }

    @Test
    @DisplayName("Should create InMemoryMemoryProvider with embeddings provider")
    void testInMemoryMemoryProviderCreation() {
        // When: Creating memory provider
        InMemoryMemoryProvider memoryProvider = new InMemoryMemoryProvider(mockEmbeddingsProvider);

        // Then: Should be initialized
        assertThat(memoryProvider).isNotNull();
    }

    @Test
    @DisplayName("Should create LLMEvaluationProvider with LLM provider")
    void testLLMEvaluationProviderCreation() {
        // When: Creating evaluation provider
        LLMEvaluationProvider evaluationProvider = new LLMEvaluationProvider(mockLLMProvider);

        // Then: Should be initialized
        assertThat(evaluationProvider).isNotNull();
    }

    @Test
    @DisplayName("Should create SimpleOrchestrationProvider without dependencies")
    void testSimpleOrchestrationProviderCreation() {
        // When: Creating orchestration provider
        SimpleOrchestrationProvider orchestrationProvider = new SimpleOrchestrationProvider();

        // Then: Should be initialized
        assertThat(orchestrationProvider).isNotNull();
    }

    @Test
    @DisplayName("Should create SimpleToolProvider with built-in tools")
    void testSimpleToolProviderHasTools() {
        // When: Creating tool provider
        SimpleToolProvider toolProvider = new SimpleToolProvider();

        // Then: Should have tools available
        assertThat(toolProvider).isNotNull();
        assertThat(toolProvider.getAvailableTools()).isNotNull();
    }

    @Test
    @DisplayName("Should support multiple providers working together")
    void testMultipleProvidersIntegration() {
        // Given: Multiple providers
        InMemoryMemoryProvider memoryProvider = new InMemoryMemoryProvider(mockEmbeddingsProvider);
        LLMEvaluationProvider evaluationProvider = new LLMEvaluationProvider(mockLLMProvider);
        SimpleOrchestrationProvider orchestrationProvider = new SimpleOrchestrationProvider();
        SimpleToolProvider toolProvider = new SimpleToolProvider();

        // Then: All should be functional
        assertThat(memoryProvider).isNotNull();
        assertThat(evaluationProvider).isNotNull();
        assertThat(orchestrationProvider).isNotNull();
        assertThat(toolProvider).isNotNull();
        assertThat(toolProvider.getAvailableTools()).isNotNull();
    }
}
