package dev.adeengineer.platform.providers.evaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.adeengineer.evaluation.EvaluationProvider;
import dev.adeengineer.evaluation.model.EvaluationCriteria;
import dev.adeengineer.evaluation.model.EvaluationResult;
import dev.adeengineer.evaluation.model.TestCase;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM-based implementation of EvaluationProvider. Uses another LLM to evaluate LLM outputs for
 * quality and correctness.
 *
 * <p>Features: - Criteria-based evaluation - Test case validation - A/B comparison - Multiple
 * evaluation metrics
 *
 * <p>Evaluation approaches: - Semantic similarity - Fact checking - Coherence analysis - Relevance
 * scoring - Compliance verification
 */
@Slf4j
public final class LLMEvaluationProvider implements EvaluationProvider {

    /** LLM provider for performing evaluations. */
    private final LLMProvider llmProvider;

    /** Supported evaluation metrics. */
    private static final List<String> SUPPORTED_METRICS =
            List.of(
                    "accuracy",
                    "relevance",
                    "coherence",
                    "completeness",
                    "factuality",
                    "toxicity",
                    "bias",
                    "fluency");

    /**
     * Creates an LLM-based evaluation provider.
     *
     * @param llm LLM provider for evaluation
     */
    public LLMEvaluationProvider(final LLMProvider llm) {
        this.llmProvider = llm;
        log.info("Initialized LLMEvaluationProvider with LLM: {}", llm.getProviderName());
    }

    /**
     * Evaluate an LLM output against criteria.
     *
     * @param output The LLM output to evaluate
     * @param criteria The evaluation criteria
     * @return Mono emitting the evaluation result
     */
    @Override
    public Mono<EvaluationResult> evaluate(final String output, final EvaluationCriteria criteria) {
        return Mono.fromCallable(
                () -> {
                    // Build evaluation prompt
                    final String evaluationPrompt = buildEvaluationPrompt(output, criteria);

                    // Get LLM evaluation (synchronous call)
                    final LLMResponse response =
                            llmProvider.generate(
                                    evaluationPrompt,
                                    0.0, // Low temperature for deterministic evaluation
                                    500);

                    if (response == null) {
                        throw new RuntimeException("LLM evaluation failed: null response");
                    }

                    // Parse evaluation result from LLM response
                    final Map<String, Double> scores = parseEvaluationScores(response.content());
                    final boolean passed = determinePass(scores, criteria);

                    // Calculate average score
                    final double avgScore =
                            scores.values().stream()
                                    .mapToDouble(Double::doubleValue)
                                    .average()
                                    .orElse(0.0);

                    // Use primary metric if available, otherwise "overall"
                    final String primaryMetric =
                            criteria.metrics().isEmpty() ? "overall" : criteria.metrics().get(0);

                    return new EvaluationResult(
                            primaryMetric,
                            avgScore,
                            passed,
                            Map.of("evaluator_model", llmProvider.getModel(), "all_scores", scores),
                            response.content() // Full LLM explanation as feedback
                            );
                });
    }

    /**
     * Run a test case.
     *
     * @param testCase The test case to run
     * @param actualOutput The actual LLM output
     * @return Mono emitting the evaluation result
     */
    @Override
    public Mono<EvaluationResult> runTestCase(final TestCase testCase, final String actualOutput) {
        return Mono.fromCallable(
                () -> {
                    // Build test evaluation prompt
                    final String evaluationPrompt = buildTestCasePrompt(testCase, actualOutput);

                    // Get LLM evaluation (synchronous call)
                    final LLMResponse response = llmProvider.generate(evaluationPrompt, 0.0, 500);

                    if (response == null) {
                        throw new RuntimeException("Test case evaluation failed: null response");
                    }

                    // Parse test result
                    final boolean passed = parseTestResult(response.content());
                    final double score = passed ? 1.0 : 0.0;

                    return new EvaluationResult(
                            "test_match",
                            score,
                            passed,
                            Map.of(
                                    "test_id", testCase.id(),
                                    "evaluator_model", llmProvider.getModel(),
                                    "expected", testCase.expectedOutput()),
                            response.content());
                });
    }

    /**
     * Run multiple test cases in batch.
     *
     * @param testCases The test cases to run
     * @param outputs The corresponding outputs (same order as test cases)
     * @return Flux of evaluation results
     */
    @Override
    public Flux<EvaluationResult> runTestSuite(
            final Flux<TestCase> testCases, final Flux<String> outputs) {
        return Flux.zip(testCases, outputs)
                .flatMap(tuple -> runTestCase(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Compare two LLM outputs for A/B testing.
     *
     * @param outputA First output
     * @param outputB Second output
     * @param criteria Evaluation criteria
     * @return Mono emitting comparison result (-1 if A better, 0 if equal, 1 if B better)
     */
    @Override
    public Mono<Integer> compare(
            final String outputA, final String outputB, final EvaluationCriteria criteria) {
        return Mono.fromCallable(
                () -> {
                    // Build comparison prompt
                    final String comparisonPrompt =
                            buildComparisonPrompt(outputA, outputB, criteria);

                    // Get LLM comparison (synchronous call)
                    final LLMResponse response = llmProvider.generate(comparisonPrompt, 0.0, 300);

                    if (response == null) {
                        throw new RuntimeException("Comparison failed: null response");
                    }

                    // Parse comparison result
                    return parseComparisonResult(response.content());
                });
    }

    /**
     * Get supported evaluation metrics.
     *
     * @return Flux of available metric names
     */
    @Override
    public Flux<String> getSupportedMetrics() {
        return Flux.fromIterable(SUPPORTED_METRICS);
    }

    /**
     * Get the provider name.
     *
     * @return Provider name
     */
    @Override
    public String getProviderName() {
        return "llm-evaluator";
    }

    /**
     * Check if the provider is healthy and accessible.
     *
     * @return true if the provider is ready to use
     */
    @Override
    public boolean isHealthy() {
        return llmProvider != null && llmProvider.isHealthy();
    }

    /**
     * Build evaluation prompt for LLM.
     *
     * @param output The output to evaluate
     * @param criteria Evaluation criteria
     * @return Evaluation prompt
     */
    private String buildEvaluationPrompt(final String output, final EvaluationCriteria criteria) {
        return String.format(
                """
                You are an expert evaluator. Evaluate the following output based on the given criteria.

                EVALUATION CRITERIA:
                Metrics: %s
                Pass Threshold: %.2f
                Reference Data: %s

                OUTPUT TO EVALUATE:
                %s

                Provide a detailed evaluation with scores for each metric (0.0 to 1.0).
                Format your response as:

                SCORES:
                metric_name: score (explanation)

                OVERALL ASSESSMENT:
                [Your detailed assessment]

                PASS/FAIL: [PASS or FAIL]
                """,
                String.join(", ", criteria.metrics()),
                criteria.threshold(),
                criteria.referenceData() != null ? criteria.referenceData() : "None",
                output);
    }

    /**
     * Build test case evaluation prompt.
     *
     * @param testCase The test case
     * @param actualOutput The actual output
     * @return Test evaluation prompt
     */
    private String buildTestCasePrompt(final TestCase testCase, final String actualOutput) {
        return String.format(
                """
                You are an expert test evaluator. Compare the actual output against the expected output.

                TEST CASE:
                ID: %s
                Input: %s

                EXPECTED OUTPUT:
                %s

                ACTUAL OUTPUT:
                %s

                Determine if the actual output matches the expected output (considering semantic meaning, not just exact text).

                Respond with:
                MATCH: [YES or NO]
                EXPLANATION: [Brief explanation of why it matches or doesn't match]
                """,
                testCase.id(), testCase.input(), testCase.expectedOutput(), actualOutput);
    }

    /**
     * Build comparison prompt for A/B testing.
     *
     * @param outputA First output
     * @param outputB Second output
     * @param criteria Evaluation criteria
     * @return Comparison prompt
     */
    private String buildComparisonPrompt(
            final String outputA, final String outputB, final EvaluationCriteria criteria) {
        return String.format(
                """
                You are an expert evaluator. Compare two outputs based on the given criteria.

                EVALUATION CRITERIA:
                Metrics: %s
                Reference: %s

                OUTPUT A:
                %s

                OUTPUT B:
                %s

                Which output is better? Consider all aspects of the criteria.

                Respond with:
                WINNER: [A, B, or TIE]
                EXPLANATION: [Brief explanation of your decision]
                """,
                String.join(", ", criteria.metrics()),
                criteria.referenceData() != null ? criteria.referenceData() : "None",
                outputA,
                outputB);
    }

    /**
     * Parse evaluation scores from LLM response.
     *
     * @param response LLM response text
     * @return Map of metric scores
     */
    private Map<String, Double> parseEvaluationScores(final String response) {
        final Map<String, Double> scores = new HashMap<>();

        // Simple parsing - look for "metric: score" patterns
        final String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                final String[] parts = line.split(":");
                if (parts.length >= 2) {
                    final String metric = parts[0].trim().toLowerCase();
                    final String scoreText = parts[1].trim().split(" ")[0]; // Take first word
                    try {
                        final double score = Double.parseDouble(scoreText);
                        if (SUPPORTED_METRICS.contains(metric)) {
                            scores.put(metric, score);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid scores
                    }
                }
            }
        }

        // If no scores parsed, provide default
        if (scores.isEmpty()) {
            scores.put("overall", response.toLowerCase().contains("pass") ? 0.8 : 0.3);
        }

        return scores;
    }

    /**
     * Determine if evaluation passed based on scores and criteria.
     *
     * @param scores Evaluation scores
     * @param criteria Evaluation criteria
     * @return true if passed
     */
    private boolean determinePass(
            final Map<String, Double> scores, final EvaluationCriteria criteria) {
        if (scores.isEmpty()) {
            return false;
        }

        // Calculate average score
        final double avgScore =
                scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return avgScore >= criteria.threshold();
    }

    /**
     * Parse test result from LLM response.
     *
     * @param response LLM response text
     * @return true if test passed
     */
    private boolean parseTestResult(final String response) {
        final String normalized = response.toLowerCase();
        return normalized.contains("match: yes") || normalized.contains("pass");
    }

    /**
     * Parse comparison result from LLM response.
     *
     * @param response LLM response text
     * @return -1 if A better, 0 if tie, 1 if B better
     */
    private int parseComparisonResult(final String response) {
        final String normalized = response.toLowerCase();
        if (normalized.contains("winner: a")) {
            return -1;
        } else if (normalized.contains("winner: b")) {
            return 1;
        } else {
            return 0; // Tie or unclear
        }
    }
}
