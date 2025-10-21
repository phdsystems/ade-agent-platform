package dev.adeengineer.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.adeengineer.agent.TaskResult;
import dev.adeengineer.platform.orchestration.WorkflowEngine.Workflow;
import dev.adeengineer.platform.orchestration.WorkflowEngine.WorkflowResult;
import dev.adeengineer.platform.orchestration.WorkflowEngine.WorkflowStep;

/** Unit tests for WorkflowEngine. */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock private ParallelAgentExecutor parallelExecutor;

    private WorkflowEngine engine;

    @BeforeEach
    void setUp() {
        engine = new WorkflowEngine(parallelExecutor);
    }

    @Test
    void shouldExecuteSimpleWorkflow() {
        // Given
        WorkflowStep step1 = new WorkflowStep("design", "Architect", "Design system", List.of());
        WorkflowStep step2 =
                new WorkflowStep("implement", "Developer", "Implement design", List.of("design"));

        Workflow workflow = new Workflow("simple-workflow", List.of(step1, step2));

        Map<String, Object> designMetadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        50,
                        "totalTokens",
                        60,
                        "estimatedCost",
                        0.001);
        Map<String, Object> implementMetadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        100,
                        "totalTokens",
                        110,
                        "estimatedCost",
                        0.002);

        TaskResult designResult =
                TaskResult.success(
                        "Architect", "Design system", "Design completed", designMetadata, 100);
        TaskResult implementResult =
                TaskResult.success(
                        "Developer",
                        "Implement design",
                        "Implementation completed",
                        implementMetadata,
                        200);

        when(parallelExecutor.executeParallel(any()))
                .thenReturn(List.of(designResult), List.of(implementResult));

        // When
        WorkflowResult result = engine.executeWorkflow(workflow);

        // Then
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.stepResults()).hasSize(2);
        assertThat(result.stepResults()).containsKeys("design", "implement");
    }

    @Test
    void shouldExecuteParallelSteps() {
        // Given
        WorkflowStep step1 = new WorkflowStep("frontend", "Frontend Dev", "Build UI", List.of());
        WorkflowStep step2 = new WorkflowStep("backend", "Backend Dev", "Build API", List.of());
        WorkflowStep step3 =
                new WorkflowStep(
                        "integrate", "DevOps", "Integrate", List.of("frontend", "backend"));

        Workflow workflow = new Workflow("parallel-workflow", List.of(step1, step2, step3));

        Map<String, Object> metadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        50,
                        "totalTokens",
                        60,
                        "estimatedCost",
                        0.001);

        TaskResult frontendResult =
                TaskResult.success("Frontend Dev", "Build UI", "UI completed", metadata, 100);
        TaskResult backendResult =
                TaskResult.success("Backend Dev", "Build API", "API completed", metadata, 100);
        TaskResult integrateResult =
                TaskResult.success("DevOps", "Integrate", "Integration completed", metadata, 100);

        when(parallelExecutor.executeParallel(any()))
                .thenReturn(List.of(frontendResult))
                .thenReturn(List.of(backendResult))
                .thenReturn(List.of(integrateResult));

        // When
        WorkflowResult result = engine.executeWorkflow(workflow);

        // Then
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.stepResults()).hasSize(3);
    }

    @Test
    void shouldDetectCyclicDependency() {
        // Given
        WorkflowStep step1 = new WorkflowStep("step1", "Role1", "Task1", List.of("step2"));
        WorkflowStep step2 = new WorkflowStep("step2", "Role2", "Task2", List.of("step1"));

        Workflow workflow = new Workflow("cyclic-workflow", List.of(step1, step2));

        // When/Then
        assertThatThrownBy(() -> engine.executeWorkflow(workflow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldExecuteChain() {
        // Given
        List<WorkflowStep> steps =
                List.of(
                        new WorkflowStep("step1", "Role1", "Task1"),
                        new WorkflowStep("step2", "Role2", "Task2"),
                        new WorkflowStep("step3", "Role3", "Task3"));

        Map<String, Object> metadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        50,
                        "totalTokens",
                        60,
                        "estimatedCost",
                        0.001);

        TaskResult result1 = TaskResult.success("Role1", "Task1", "Output1", metadata, 100);
        TaskResult result2 = TaskResult.success("Role2", "Task2", "Output2", metadata, 100);
        TaskResult result3 = TaskResult.success("Role3", "Task3", "Output3", metadata, 100);

        when(parallelExecutor.executeParallel(any()))
                .thenReturn(List.of(result1))
                .thenReturn(List.of(result2))
                .thenReturn(List.of(result3));

        // When
        TaskResult finalResult = engine.executeChain(steps);

        // Then
        assertThat(finalResult.success()).isTrue();
        assertThat(finalResult.output()).isEqualTo("Output3");
    }

    @Test
    void shouldStopChainOnFailure() {
        // Given
        List<WorkflowStep> steps =
                List.of(
                        new WorkflowStep("step1", "Role1", "Task1"),
                        new WorkflowStep("step2", "Role2", "Task2"),
                        new WorkflowStep("step3", "Role3", "Task3"));

        Map<String, Object> metadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        50,
                        "totalTokens",
                        60,
                        "estimatedCost",
                        0.001);

        TaskResult result1 = TaskResult.success("Role1", "Task1", "Output1", metadata, 100);
        TaskResult result2 = TaskResult.failure("Role2", "Task2", "Error occurred");

        when(parallelExecutor.executeParallel(any()))
                .thenReturn(List.of(result1))
                .thenReturn(List.of(result2));

        // When
        TaskResult finalResult = engine.executeChain(steps);

        // Then
        assertThat(finalResult.success()).isFalse();
        assertThat(finalResult.errorMessage()).contains("Error occurred");
    }

    @Test
    void shouldExecuteFanOutFanIn() {
        // Given
        String initialTask = "Review code from different perspectives";
        List<String> fanOutRoles = List.of("Security Expert", "Performance Expert", "UX Expert");
        String aggregatorRole = "Tech Lead";

        Map<String, Object> metadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        50,
                        "totalTokens",
                        60,
                        "estimatedCost",
                        0.001);
        Map<String, Object> aggMetadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        100,
                        "totalTokens",
                        110,
                        "estimatedCost",
                        0.002);

        TaskResult securityResult =
                TaskResult.success(
                        "Security Expert",
                        initialTask,
                        "Security review: No vulnerabilities found",
                        metadata,
                        100);
        TaskResult performanceResult =
                TaskResult.success(
                        "Performance Expert",
                        initialTask,
                        "Performance review: Optimizations needed",
                        metadata,
                        100);
        TaskResult uxResult =
                TaskResult.success(
                        "UX Expert", initialTask, "UX review: User flow is clear", metadata, 100);
        TaskResult aggregatedResult =
                TaskResult.success(
                        "Tech Lead",
                        "Synthesize and summarize the following responses:",
                        "Overall: Code is secure and user-friendly but needs performance optimization",
                        aggMetadata,
                        200);

        when(parallelExecutor.executeParallel(any()))
                .thenReturn(List.of(securityResult, performanceResult, uxResult))
                .thenReturn(List.of(aggregatedResult));

        // When
        TaskResult result = engine.executeFanOutFanIn(initialTask, fanOutRoles, aggregatorRole);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Overall");
    }

    @Test
    void shouldHandleComplexDAG() {
        // Given
        WorkflowStep gather =
                new WorkflowStep("gather", "Analyst", "Gather requirements", List.of());
        WorkflowStep designUI =
                new WorkflowStep("designUI", "UX Designer", "Design UI", List.of("gather"));
        WorkflowStep designAPI =
                new WorkflowStep("designAPI", "Architect", "Design API", List.of("gather"));
        WorkflowStep implement =
                new WorkflowStep(
                        "implement", "Developer", "Implement", List.of("designUI", "designAPI"));
        WorkflowStep test = new WorkflowStep("test", "QA", "Test", List.of("implement"));

        Workflow workflow =
                new Workflow("complex-dag", List.of(gather, designUI, designAPI, implement, test));

        Map<String, Object> metadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        50,
                        "totalTokens",
                        60,
                        "estimatedCost",
                        0.001);
        Map<String, Object> implMetadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        100,
                        "totalTokens",
                        110,
                        "estimatedCost",
                        0.002);

        TaskResult gatherResult =
                TaskResult.success(
                        "Analyst", "Gather requirements", "Requirements gathered", metadata, 100);
        TaskResult designUIResult =
                TaskResult.success("UX Designer", "Design UI", "UI designed", metadata, 100);
        TaskResult designAPIResult =
                TaskResult.success("Architect", "Design API", "API designed", metadata, 100);
        TaskResult implementResult =
                TaskResult.success(
                        "Developer", "Implement", "Implementation done", implMetadata, 200);
        TaskResult testResult = TaskResult.success("QA", "Test", "Tests passed", metadata, 100);

        when(parallelExecutor.executeParallel(any()))
                .thenReturn(List.of(gatherResult))
                .thenReturn(List.of(designUIResult))
                .thenReturn(List.of(designAPIResult))
                .thenReturn(List.of(implementResult))
                .thenReturn(List.of(testResult));

        // When
        WorkflowResult result = engine.executeWorkflow(workflow);

        // Then
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.stepResults()).hasSize(5);
        assertThat(result.stepResults())
                .containsKeys("gather", "designUI", "designAPI", "implement", "test");
    }

    @Test
    void shouldPassContextBetweenChainSteps() {
        // Given
        List<WorkflowStep> steps =
                List.of(
                        new WorkflowStep("step1", "Role1", "Analyze requirements"),
                        new WorkflowStep("step2", "Role2", "Design based on analysis"));

        Map<String, Object> metadata =
                Map.of(
                        "inputTokens",
                        10,
                        "outputTokens",
                        50,
                        "totalTokens",
                        60,
                        "estimatedCost",
                        0.001);

        TaskResult result1 =
                TaskResult.success(
                        "Role1", "Analyze requirements", "Analysis: Need 3 APIs", metadata, 100);
        TaskResult result2 =
                TaskResult.success(
                        "Role2",
                        "Design based on analysis",
                        "Designed 3 APIs as specified",
                        metadata,
                        100);

        when(parallelExecutor.executeParallel(any()))
                .thenReturn(List.of(result1))
                .thenReturn(List.of(result2));

        // When
        TaskResult finalResult = engine.executeChain(steps);

        // Then
        assertThat(finalResult.success()).isTrue();
        assertThat(finalResult.output()).contains("Designed");
    }
}
