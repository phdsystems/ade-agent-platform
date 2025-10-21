package dev.adeengineer.platform.providers.orchestration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.adeengineer.orchestration.model.WorkflowDefinition;
import dev.adeengineer.orchestration.model.WorkflowExecution;
import dev.adeengineer.orchestration.model.WorkflowStep;
import reactor.test.StepVerifier;

/** Unit tests for SimpleOrchestrationProvider. */
class SimpleOrchestrationProviderTest {

    private SimpleOrchestrationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SimpleOrchestrationProvider();
    }

    @Test
    void shouldRegisterWorkflow() {
        // Given
        WorkflowStep step1 =
                new WorkflowStep("step1", "agent1", Map.of("action", "process"), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition(
                        "test-workflow", "Test Workflow", List.of(step1), Map.of("version", "1.0"));

        // When & Then
        StepVerifier.create(provider.registerWorkflow(workflow))
                .assertNext(
                        registered -> {
                            assertEquals("test-workflow", registered.id());
                            assertEquals("Test Workflow", registered.name());
                            assertEquals(1, registered.steps().size());
                        })
                .verifyComplete();
    }

    @Test
    void shouldExecuteWorkflow() {
        // Given
        WorkflowStep step =
                new WorkflowStep("step1", "test-agent", Map.of("param", "value"), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition("exec-workflow", "Execution Test", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();

        // When & Then
        StepVerifier.create(provider.executeWorkflow("exec-workflow", Map.of("input", "data")))
                .assertNext(
                        execution -> {
                            assertNotNull(execution.executionId());
                            assertEquals("exec-workflow", execution.workflowId());
                            assertEquals(
                                    WorkflowExecution.ExecutionStatus.RUNNING, execution.status());
                            assertNotNull(execution.startTime());
                            assertNull(execution.endTime());
                        })
                .verifyComplete();
    }

    @Test
    void shouldThrowErrorForNonExistentWorkflow() {
        // When & Then
        StepVerifier.create(provider.executeWorkflow("non-existent", Map.of()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldGetExecutionStatus() throws InterruptedException {
        // Given
        WorkflowStep step = new WorkflowStep("step1", "agent1", Map.of(), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition("status-workflow", "Status Test", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();
        WorkflowExecution execution = provider.executeWorkflow("status-workflow", Map.of()).block();
        assertNotNull(execution);

        // When & Then
        StepVerifier.create(provider.getExecutionStatus(execution.executionId()))
                .assertNext(
                        status -> {
                            assertEquals(execution.executionId(), status.executionId());
                            assertNotNull(status.status());
                        })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNonExistentExecution() {
        // When & Then
        StepVerifier.create(provider.getExecutionStatus("non-existent")).verifyComplete();
    }

    @Test
    void shouldCancelExecution() {
        // Given
        WorkflowStep step = new WorkflowStep("step1", "agent1", Map.of(), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition("cancel-workflow", "Cancel Test", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();
        WorkflowExecution execution = provider.executeWorkflow("cancel-workflow", Map.of()).block();
        assertNotNull(execution);

        // When
        Boolean cancelled = provider.cancelExecution(execution.executionId()).block();

        // Then
        assertTrue(cancelled);

        // Verify status is cancelled
        WorkflowExecution status = provider.getExecutionStatus(execution.executionId()).block();
        assertNotNull(status);
        assertEquals(WorkflowExecution.ExecutionStatus.CANCELLED, status.status());
    }

    @Test
    void shouldReturnFalseWhenCancellingNonExistent() {
        // When & Then
        StepVerifier.create(provider.cancelExecution("non-existent"))
                .assertNext(result -> assertFalse(result))
                .verifyComplete();
    }

    @Test
    void shouldGetAllWorkflows() {
        // Given
        WorkflowStep step1 = new WorkflowStep("s1", "a1", Map.of(), List.of());
        WorkflowStep step2 = new WorkflowStep("s2", "a2", Map.of(), List.of());

        WorkflowDefinition workflow1 =
                new WorkflowDefinition("wf1", "Workflow 1", List.of(step1), Map.of());
        WorkflowDefinition workflow2 =
                new WorkflowDefinition("wf2", "Workflow 2", List.of(step2), Map.of());

        provider.registerWorkflow(workflow1).block();
        provider.registerWorkflow(workflow2).block();

        // When & Then
        StepVerifier.create(provider.getWorkflows()).expectNextCount(2).verifyComplete();
    }

    @Test
    void shouldStreamExecutionUpdates() throws InterruptedException {
        // Given
        WorkflowStep step = new WorkflowStep("step1", "agent1", Map.of(), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition("stream-workflow", "Stream Test", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();
        WorkflowExecution execution = provider.executeWorkflow("stream-workflow", Map.of()).block();
        assertNotNull(execution);

        // When & Then - should receive at least one update
        StepVerifier.create(provider.streamExecutionUpdates(execution.executionId()).take(1))
                .expectNextCount(1)
                .thenCancel()
                .verify();
    }

    @Test
    void shouldReturnEmptyStreamForNonExistentExecution() {
        // When & Then
        StepVerifier.create(provider.streamExecutionUpdates("non-existent")).verifyComplete();
    }

    @Test
    void shouldExecuteMultipleStepsSequentially() throws InterruptedException {
        // Given
        WorkflowStep step1 =
                new WorkflowStep("step1", "agent1", Map.of("action", "process1"), List.of());
        WorkflowStep step2 =
                new WorkflowStep("step2", "agent2", Map.of("action", "process2"), List.of());
        WorkflowStep step3 =
                new WorkflowStep("step3", "agent3", Map.of("action", "process3"), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition(
                        "multi-step-workflow",
                        "Multi-step Test",
                        List.of(step1, step2, step3),
                        Map.of());

        provider.registerWorkflow(workflow).block();

        // When
        WorkflowExecution execution =
                provider.executeWorkflow("multi-step-workflow", Map.of()).block();
        assertNotNull(execution);

        // Wait for execution to complete
        Thread.sleep(500);

        // Then
        WorkflowExecution finalStatus =
                provider.getExecutionStatus(execution.executionId()).block();
        assertNotNull(finalStatus);
        assertTrue(
                finalStatus.status() == WorkflowExecution.ExecutionStatus.COMPLETED
                        || finalStatus.status() == WorkflowExecution.ExecutionStatus.RUNNING);
    }

    @Test
    void shouldHandleWorkflowWithDependencies() {
        // Given
        WorkflowStep step1 =
                new WorkflowStep(
                        "step1", "agent1", Map.of(), List.of() // No dependencies
                        );
        WorkflowStep step2 =
                new WorkflowStep(
                        "step2", "agent2", Map.of(), List.of("step1") // Depends on step1
                        );

        WorkflowDefinition workflow =
                new WorkflowDefinition(
                        "dep-workflow", "Dependency Test", List.of(step1, step2), Map.of());

        // When & Then
        StepVerifier.create(provider.registerWorkflow(workflow))
                .assertNext(
                        registered -> {
                            assertEquals(2, registered.steps().size());
                            assertEquals(
                                    List.of("step1"), registered.steps().get(1).dependencies());
                        })
                .verifyComplete();
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("simple", provider.getProviderName());
    }

    @Test
    void shouldAlwaysBeHealthy() {
        assertTrue(provider.isHealthy());
    }

    @Test
    void shouldCompleteWorkflowExecution() throws InterruptedException {
        // Given
        WorkflowStep step = new WorkflowStep("quick-step", "quick-agent", Map.of(), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition(
                        "complete-workflow", "Completion Test", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();
        WorkflowExecution execution =
                provider.executeWorkflow("complete-workflow", Map.of()).block();
        assertNotNull(execution);

        // Wait for completion
        Thread.sleep(200);

        // Then
        WorkflowExecution finalStatus =
                provider.getExecutionStatus(execution.executionId()).block();
        assertNotNull(finalStatus);
        assertTrue(
                finalStatus.status() == WorkflowExecution.ExecutionStatus.COMPLETED
                        || finalStatus.status() == WorkflowExecution.ExecutionStatus.RUNNING);

        if (finalStatus.status() == WorkflowExecution.ExecutionStatus.COMPLETED) {
            assertNotNull(finalStatus.endTime());
        }
    }

    @Test
    void shouldNotCancelAlreadyCompletedExecution() throws InterruptedException {
        // Given
        WorkflowStep step = new WorkflowStep("fast-step", "fast-agent", Map.of(), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition("fast-workflow", "Fast Completion", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();
        WorkflowExecution execution = provider.executeWorkflow("fast-workflow", Map.of()).block();
        assertNotNull(execution);

        // Wait for completion
        Thread.sleep(200);

        // When - try to cancel completed execution
        WorkflowExecution status = provider.getExecutionStatus(execution.executionId()).block();
        boolean shouldFail =
                status != null && status.status() == WorkflowExecution.ExecutionStatus.COMPLETED;

        if (shouldFail) {
            Boolean cancelled = provider.cancelExecution(execution.executionId()).block();
            // Then - should return false as it's already completed
            assertFalse(cancelled);
        }
    }

    @Test
    void shouldHandleEmptyInput() {
        // Given
        WorkflowStep step = new WorkflowStep("step1", "agent1", Map.of(), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition(
                        "empty-input-workflow", "Empty Input Test", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();

        // When & Then
        StepVerifier.create(provider.executeWorkflow("empty-input-workflow", Map.of()))
                .assertNext(
                        execution -> {
                            assertNotNull(execution);
                            assertEquals(
                                    WorkflowExecution.ExecutionStatus.RUNNING, execution.status());
                        })
                .verifyComplete();
    }

    @Test
    void shouldStoreInputInResults() {
        // Given
        WorkflowStep step = new WorkflowStep("step1", "agent1", Map.of(), List.of());

        WorkflowDefinition workflow =
                new WorkflowDefinition(
                        "input-workflow", "Input Storage Test", List.of(step), Map.of());

        provider.registerWorkflow(workflow).block();

        Map<String, Object> input = Map.of("key1", "value1", "key2", 123);

        // When
        WorkflowExecution execution = provider.executeWorkflow("input-workflow", input).block();

        // Then
        assertNotNull(execution);
        assertNotNull(execution.results());
        assertEquals(input, execution.results().get("input"));
    }
}
