package dev.adeengineer.platform.providers.orchestration;

import dev.adeengineer.orchestration.OrchestrationProvider;
import dev.adeengineer.orchestration.model.WorkflowDefinition;
import dev.adeengineer.orchestration.model.WorkflowExecution;
import dev.adeengineer.orchestration.model.WorkflowStep;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of OrchestrationProvider.
 * Provides basic workflow orchestration without external dependencies.
 *
 * <p>Features:
 * - Sequential workflow execution
 * - Execution status tracking
 * - Real-time execution streaming
 * - Workflow cancellation
 *
 * <p>Limitations:
 * - No persistence (executions lost on restart)
 * - Single-node only (no distributed execution)
 * - Basic error handling
 * - Sequential execution only (no parallelism)
 */
@Slf4j

public final class SimpleOrchestrationProvider implements OrchestrationProvider {

    /** Registry of workflow definitions. */
    private final Map<String, WorkflowDefinition> workflowRegistry = new ConcurrentHashMap<>();

    /** Registry of workflow executions. */
    private final Map<String, WorkflowExecution> executionRegistry = new ConcurrentHashMap<>();

    /** Event sinks for streaming execution updates. */
    private final Map<String, Sinks.Many<WorkflowExecution>> executionSinks =
            new ConcurrentHashMap<>();

    /**
     * Creates a simple orchestration provider.
     */
    public SimpleOrchestrationProvider() {
        log.info("Initialized SimpleOrchestrationProvider");
    }

    /**
     * Register a workflow definition.
     *
     * @param workflow The workflow to register
     * @return Mono emitting the registered workflow
     */
    @Override
    public Mono<WorkflowDefinition> registerWorkflow(final WorkflowDefinition workflow) {
        workflowRegistry.put(workflow.id(), workflow);
        log.info("Registered workflow: {}", workflow.id());
        return Mono.just(workflow);
    }

    /**
     * Execute a workflow.
     *
     * @param workflowId The workflow to execute
     * @param input Input parameters for the workflow
     * @return Mono emitting the workflow execution
     */
    @Override
    public Mono<WorkflowExecution> executeWorkflow(
            final String workflowId,
            final Map<String, Object> input) {
        return Mono.fromCallable(() -> {
            final WorkflowDefinition workflow = workflowRegistry.get(workflowId);
            if (workflow == null) {
                throw new IllegalArgumentException("Workflow not found: " + workflowId);
            }

            // Create execution
            final String executionId = UUID.randomUUID().toString();
            final WorkflowExecution execution = new WorkflowExecution(
                    workflowId,
                    executionId,
                    WorkflowExecution.ExecutionStatus.RUNNING,
                    Instant.now(),
                    null,  // endTime
                    Map.of("input", input),  // results
                    null   // error
            );

            executionRegistry.put(executionId, execution);

            // Create event sink for streaming
            final Sinks.Many<WorkflowExecution> sink = Sinks.many().multicast().onBackpressureBuffer();
            executionSinks.put(executionId, sink);
            sink.tryEmitNext(execution);

            // Execute workflow asynchronously
            Mono.fromRunnable(() -> executeWorkflowAsync(executionId, workflow, input))
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .subscribe();

            log.info("Started workflow execution: {}", executionId);
            return execution;
        });
    }

    /**
     * Get workflow execution status.
     *
     * @param executionId The execution identifier
     * @return Mono emitting the current execution state
     */
    @Override
    public Mono<WorkflowExecution> getExecutionStatus(final String executionId) {
        final WorkflowExecution execution = executionRegistry.get(executionId);
        return execution != null ? Mono.just(execution) : Mono.empty();
    }

    /**
     * Cancel a running workflow execution.
     *
     * @param executionId The execution to cancel
     * @return Mono emitting true if cancelled, false if already completed
     */
    @Override
    public Mono<Boolean> cancelExecution(final String executionId) {
        return Mono.fromCallable(() -> {
            final WorkflowExecution execution = executionRegistry.get(executionId);
            if (execution == null || execution.status() != WorkflowExecution.ExecutionStatus.RUNNING) {
                return false;
            }

            // Update execution status
            final WorkflowExecution cancelledExecution = new WorkflowExecution(
                    execution.workflowId(),
                    execution.executionId(),
                    WorkflowExecution.ExecutionStatus.CANCELLED,
                    execution.startTime(),
                    Instant.now(),
                    execution.results(),
                    "Cancelled by user"
            );

            executionRegistry.put(executionId, cancelledExecution);

            // Emit cancellation event
            final Sinks.Many<WorkflowExecution> sink = executionSinks.get(executionId);
            if (sink != null) {
                sink.tryEmitNext(cancelledExecution);
                sink.tryEmitComplete();
            }

            log.info("Cancelled workflow execution: {}", executionId);
            return true;
        });
    }

    /**
     * Get all registered workflows.
     *
     * @return Flux of workflow definitions
     */
    @Override
    public Flux<WorkflowDefinition> getWorkflows() {
        return Flux.fromIterable(workflowRegistry.values());
    }

    /**
     * Stream execution events as the workflow progresses.
     *
     * @param executionId The execution to monitor
     * @return Flux of execution updates
     */
    @Override
    public Flux<WorkflowExecution> streamExecutionUpdates(final String executionId) {
        final Sinks.Many<WorkflowExecution> sink = executionSinks.get(executionId);
        if (sink == null) {
            return Flux.empty();
        }
        return sink.asFlux();
    }

    /**
     * Get the provider name.
     *
     * @return Provider name
     */
    @Override
    public String getProviderName() {
        return "simple";
    }

    /**
     * Check if the provider is healthy and accessible.
     *
     * @return true if the provider is ready to use
     */
    @Override
    public boolean isHealthy() {
        return true;  // Always healthy for in-memory implementation
    }

    /**
     * Execute workflow asynchronously.
     *
     * @param executionId The execution ID
     * @param workflow The workflow definition
     * @param input Input parameters
     */
    private void executeWorkflowAsync(
            final String executionId,
            final WorkflowDefinition workflow,
            final Map<String, Object> input) {
        try {
            Map<String, Object> currentOutput = Map.copyOf(input);

            // Execute each step sequentially
            for (WorkflowStep step : workflow.steps()) {
                // Check if cancelled
                final WorkflowExecution currentExecution = executionRegistry.get(executionId);
                if (currentExecution.status() == WorkflowExecution.ExecutionStatus.CANCELLED) {
                    log.info("Workflow execution cancelled: {}", executionId);
                    return;
                }

                // Update current step in results
                Map<String, Object> updatedResults = new ConcurrentHashMap<>(currentExecution.results());
                updatedResults.put("current_step", step.id());

                // Update current step
                final WorkflowExecution stepExecution = new WorkflowExecution(
                        workflow.id(),
                        executionId,
                        WorkflowExecution.ExecutionStatus.RUNNING,
                        currentExecution.startTime(),
                        null,
                        updatedResults,
                        null
                );

                executionRegistry.put(executionId, stepExecution);

                // Emit step progress
                final Sinks.Many<WorkflowExecution> sink = executionSinks.get(executionId);
                if (sink != null) {
                    sink.tryEmitNext(stepExecution);
                }

                log.debug("Executing workflow step: {} - agent: {}", step.id(), step.agentName());

                // Simulate step execution (in real implementation, would invoke actual step logic)
                Thread.sleep(100);  // Simulate work

                // For now, pass output through (in real implementation, would process step)
                currentOutput = Map.copyOf(step.input());
            }

            // Mark as completed
            Map<String, Object> finalResults = new ConcurrentHashMap<>();
            finalResults.put("input", input);
            finalResults.put("output", currentOutput);

            final WorkflowExecution completedExecution = new WorkflowExecution(
                    workflow.id(),
                    executionId,
                    WorkflowExecution.ExecutionStatus.COMPLETED,
                    executionRegistry.get(executionId).startTime(),
                    Instant.now(),
                    finalResults,
                    null
            );

            executionRegistry.put(executionId, completedExecution);

            // Emit completion event
            final Sinks.Many<WorkflowExecution> sink = executionSinks.get(executionId);
            if (sink != null) {
                sink.tryEmitNext(completedExecution);
                sink.tryEmitComplete();
            }

            log.info("Completed workflow execution: {}", executionId);

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", executionId, e);

            // Mark as failed
            final WorkflowExecution currentState = executionRegistry.get(executionId);
            final WorkflowExecution failedExecution = new WorkflowExecution(
                    workflow.id(),
                    executionId,
                    WorkflowExecution.ExecutionStatus.FAILED,
                    currentState != null ? currentState.startTime() : Instant.now(),
                    Instant.now(),
                    currentState != null ? currentState.results() : Map.of("input", input),
                    "Execution failed: " + e.getMessage()
            );

            executionRegistry.put(executionId, failedExecution);

            // Emit failure event
            final Sinks.Many<WorkflowExecution> sink = executionSinks.get(executionId);
            if (sink != null) {
                sink.tryEmitNext(failedExecution);
                sink.tryEmitError(e);
            }
        }
    }
}
