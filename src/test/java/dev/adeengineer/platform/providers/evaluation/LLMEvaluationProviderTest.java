package dev.adeengineer.platform.providers.evaluation;

import dev.adeengineer.evaluation.model.EvaluationCriteria;
import dev.adeengineer.evaluation.model.EvaluationResult;
import dev.adeengineer.evaluation.model.TestCase;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LLMEvaluationProvider.
 */
@ExtendWith(MockitoExtension.class)
class LLMEvaluationProviderTest {

    @Mock
    private LLMProvider llmProvider;

    private LLMEvaluationProvider provider;

    @BeforeEach
    void setUp() {
        // Stub methods that may be called during provider operations (lenient to avoid unnecessary stubbing errors)
        lenient().when(llmProvider.getProviderName()).thenReturn("test-provider");
        lenient().when(llmProvider.getModel()).thenReturn("test-model");
        provider = new LLMEvaluationProvider(llmProvider);
    }

    @Test
    void shouldEvaluateOutput() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("accuracy", "relevance"),
                0.7,
                "Reference text"
        );

        String output = "This is a test output";

        // Mock LLM response with scores
        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "accuracy: 0.8\nrelevance: 0.9\nOVERALL ASSESSMENT: Good\nPASS/FAIL: PASS",
                        new UsageInfo(100, 50, 150, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.evaluate(output, criteria))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertTrue(result.score() >= 0.0 && result.score() <= 1.0);
                    assertNotNull(result.feedback());
                })
                .verifyComplete();
    }

    @Test
    void shouldPassWhenScoreAboveThreshold() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.6,
                null
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "quality: 0.8\nPASS",
                        new UsageInfo(50, 25, 75, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.evaluate("output", criteria))
                .assertNext(result -> assertTrue(result.passed()))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenScoreBelowThreshold() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.8,
                null
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "quality: 0.5\nFAIL",
                        new UsageInfo(50, 25, 75, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.evaluate("output", criteria))
                .assertNext(result -> assertFalse(result.passed()))
                .verifyComplete();
    }

    @Test
    void shouldRunTestCase() {
        // Given
        TestCase testCase = new TestCase(
                "test-1",
                "What is 2+2?",
                "4",
                new EvaluationCriteria(List.of("accuracy"), 0.9, null),
                Map.of()
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "MATCH: YES\nEXPLANATION: The answer is correct",
                        new UsageInfo(80, 40, 120, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.runTestCase(testCase, "4"))
                .assertNext(result -> {
                    assertEquals("test_match", result.metric());
                    assertTrue(result.passed());
                    assertEquals(1.0, result.score());
                })
                .verifyComplete();
    }

    @Test
    void shouldFailTestCaseWhenOutputDoesNotMatch() {
        // Given
        TestCase testCase = new TestCase(
                "test-2",
                "What is 2+2?",
                "4",
                new EvaluationCriteria(List.of("accuracy"), 0.9, null),
                Map.of()
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "MATCH: NO\nEXPLANATION: The answer is incorrect",
                        new UsageInfo(80, 40, 120, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.runTestCase(testCase, "5"))
                .assertNext(result -> {
                    assertFalse(result.passed());
                    assertEquals(0.0, result.score());
                })
                .verifyComplete();
    }

    @Test
    void shouldCompareOutputsAndReturnNegativeForA() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.5,
                null
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "WINNER: A\nEXPLANATION: Output A is better",
                        new UsageInfo(60, 30, 90, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.compare("Output A", "Output B", criteria))
                .assertNext(result -> assertEquals(-1, result))
                .verifyComplete();
    }

    @Test
    void shouldCompareOutputsAndReturnPositiveForB() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.5,
                null
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "WINNER: B\nEXPLANATION: Output B is better",
                        new UsageInfo(60, 30, 90, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.compare("Output A", "Output B", criteria))
                .assertNext(result -> assertEquals(1, result))
                .verifyComplete();
    }

    @Test
    void shouldCompareOutputsAndReturnZeroForTie() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.5,
                null
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "WINNER: TIE\nEXPLANATION: Both outputs are equal",
                        new UsageInfo(60, 30, 90, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.compare("Output A", "Output B", criteria))
                .assertNext(result -> assertEquals(0, result))
                .verifyComplete();
    }

    @Test
    void shouldGetSupportedMetrics() {
        // When & Then
        StepVerifier.create(provider.getSupportedMetrics())
                .expectNext("accuracy")
                .expectNext("relevance")
                .expectNext("coherence")
                .expectNext("completeness")
                .expectNext("factuality")
                .expectNext("toxicity")
                .expectNext("bias")
                .expectNext("fluency")
                .verifyComplete();
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("llm-evaluator", provider.getProviderName());
    }

    @Test
    void shouldBeHealthyWhenLLMProviderIsHealthy() {
        when(llmProvider.isHealthy()).thenReturn(true);
        assertTrue(provider.isHealthy());
    }

    @Test
    void shouldBeUnhealthyWhenLLMProviderIsUnhealthy() {
        when(llmProvider.isHealthy()).thenReturn(false);
        assertFalse(provider.isHealthy());
    }

    @Test
    void shouldHandleMultipleMetrics() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("accuracy", "relevance", "coherence"),
                0.7,
                null
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "accuracy: 0.8\nrelevance: 0.9\ncoherence: 0.7\nPASS",
                        new UsageInfo(100, 50, 150, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.evaluate("output", criteria))
                .assertNext(result -> {
                    assertTrue(result.passed());
                    assertTrue(result.score() >= 0.7);
                    assertNotNull(result.details().get("all_scores"));
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleReferenceData() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("accuracy"),
                0.8,
                "This is reference data for comparison"
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "accuracy: 0.85\nPASS",
                        new UsageInfo(100, 50, 150, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then
        StepVerifier.create(provider.evaluate("output", criteria))
                .assertNext(result -> {
                    assertTrue(result.passed());
                    assertNotNull(result.feedback());
                })
                .verifyComplete();
    }

    @Test
    void shouldUseZeroTemperatureForDeterministicEvaluation() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.5,
                null
        );

        when(llmProvider.generate(anyString(), eq(0.0), anyInt())).thenReturn(
                new LLMResponse(
                        "quality: 0.7\nPASS",
                        new UsageInfo(50, 25, 75, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When
        provider.evaluate("output", criteria).block();

        // Then - verify temperature was 0.0 (done via mock verification)
        // If wrong temperature used, mock wouldn't match and would return null
    }

    @Test
    void shouldIncludeEvaluatorModelInDetails() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.5,
                null
        );

        when(llmProvider.getModel()).thenReturn("gpt-4");
        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "quality: 0.8\nPASS",
                        new UsageInfo(50, 25, 75, 0.0),
                        "test-llm",
                        "gpt-4"
                )
        );

        // When & Then
        StepVerifier.create(provider.evaluate("output", criteria))
                .assertNext(result -> {
                    assertEquals("gpt-4", result.details().get("evaluator_model"));
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyLLMResponse() {
        // Given
        EvaluationCriteria criteria = new EvaluationCriteria(
                List.of("quality"),
                0.5,
                null
        );

        when(llmProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(
                new LLMResponse(
                        "No scores provided",
                        new UsageInfo(10, 5, 15, 0.0),
                        "test-llm",
                        "test-model"
                )
        );

        // When & Then - should not throw, should handle gracefully
        StepVerifier.create(provider.evaluate("output", criteria))
                .assertNext(result -> {
                    assertNotNull(result);
                    // Default score should be provided
                    assertTrue(result.score() >= 0.0 && result.score() <= 1.0);
                })
                .verifyComplete();
    }
}
