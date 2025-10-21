package dev.adeengineer.platform.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.adeengineer.agent.TaskRequest;
import dev.adeengineer.agent.TaskResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Workflow engine for agent chaining and orchestration.
 *
 * <p>Supports DAG-based workflow execution with dependencies.
 */
@Slf4j
@Service
public class WorkflowEngine {

    /** Executor for parallel agent tasks. */
    private final ParallelAgentExecutor parallelExecutor;

    /**
     * Constructs a new WorkflowEngine.
     *
     * @param executor Executor for parallel agent tasks
     */
    public WorkflowEngine(final ParallelAgentExecutor executor) {
        this.parallelExecutor = executor;
    }

    /**
     * Execute a workflow defined by a directed acyclic graph (DAG).
     *
     * @param workflow Workflow definition
     * @return Workflow execution result
     */
    public WorkflowResult executeWorkflow(final Workflow workflow) {
        log.info("Executing workflow: {}", workflow.name());

        Map<String, TaskResult> results = new HashMap<>();
        Map<String, CompletableFuture<TaskResult>> futures = new HashMap<>();

        // Topological sort to determine execution order
        List<WorkflowStep> sortedSteps = topologicalSort(workflow);

        // Execute steps in dependency order
        for (WorkflowStep step : sortedSteps) {
            log.debug("Executing step: {}", step.name());

            // Wait for dependencies
            List<TaskResult> dependencyResults = new ArrayList<>();
            for (String dependency : step.dependencies()) {
                try {
                    TaskResult result = futures.get(dependency).get();
                    dependencyResults.add(result);
                } catch (Exception e) {
                    log.error("Failed to get dependency result: {}", e.getMessage());
                    return WorkflowResult.failed(
                            workflow.name(), "Dependency failed: " + dependency);
                }
            }

            // Build context from dependencies
            Map<String, Object> context = buildContext(dependencyResults);

            // Execute step
            TaskRequest task = new TaskRequest(step.role(), step.task(), context);

            CompletableFuture<TaskResult> future =
                    CompletableFuture.supplyAsync(
                            () -> parallelExecutor.executeParallel(List.of(task)).get(0));

            futures.put(step.name(), future);
        }

        // Wait for all steps to complete
        futures.values()
                .forEach(
                        f -> {
                            try {
                                f.get();
                            } catch (Exception e) {
                                log.error("Step execution failed: {}", e.getMessage());
                            }
                        });

        // Collect results
        futures.forEach(
                (name, future) -> {
                    try {
                        results.put(name, future.get());
                    } catch (Exception e) {
                        log.error("Failed to collect result for step: {}", name);
                    }
                });

        return WorkflowResult.success(workflow.name(), results);
    }

    /**
     * Execute a sequential chain of agents.
     *
     * @param steps List of workflow steps
     * @return Final task result
     */
    public TaskResult executeChain(final List<WorkflowStep> steps) {
        log.info("Executing agent chain with {} steps", steps.size());

        TaskResult previousResult = null;
        for (WorkflowStep step : steps) {
            Map<String, Object> context = null;
            if (previousResult != null) {
                context = Map.of("previousOutput", previousResult.output());
            }

            TaskRequest task = new TaskRequest(step.role(), step.task(), context);

            List<TaskResult> results = parallelExecutor.executeParallel(List.of(task));
            previousResult = results.get(0);

            if (!previousResult.success()) {
                log.error("Chain execution failed at step: {}", step.name());
                return previousResult;
            }
        }

        return previousResult;
    }

    /**
     * Execute a fan-out/fan-in pattern.
     *
     * <p>One task fans out to multiple agents, then results are aggregated.
     *
     * @param initialTask Initial task
     * @param fanOutRoles Roles to fan out to
     * @param aggregatorRole Role for aggregation
     * @return Aggregated result
     */
    public TaskResult executeFanOutFanIn(
            final String initialTask, final List<String> fanOutRoles, final String aggregatorRole) {
        log.info("Executing fan-out/fan-in with {} roles", fanOutRoles.size());

        // Fan-out: Execute initial task in parallel across multiple roles
        List<TaskRequest> fanOutTasks =
                fanOutRoles.stream().map(role -> new TaskRequest(role, initialTask, null)).toList();

        List<TaskResult> fanOutResults = parallelExecutor.executeParallel(fanOutTasks);

        // Fan-in: Aggregate results
        Map<String, Object> aggregatedContext = new HashMap<>();
        for (int i = 0; i < fanOutResults.size(); i++) {
            TaskResult result = fanOutResults.get(i);
            aggregatedContext.put("result_" + i, result.output());
            aggregatedContext.put("result_" + i + "_role", result.agentName());
        }

        TaskRequest aggregationTask =
                new TaskRequest(
                        aggregatorRole,
                        "Synthesize and summarize the following responses:",
                        aggregatedContext);

        return parallelExecutor.executeParallel(List.of(aggregationTask)).get(0);
    }

    /**
     * Topological sort for DAG execution order.
     *
     * @param workflow Workflow to sort
     * @return Sorted list of workflow steps
     * @throws IllegalArgumentException if workflow contains a cycle
     */
    private List<WorkflowStep> topologicalSort(final Workflow workflow) {
        Map<String, WorkflowStep> stepMap =
                workflow.steps().stream().collect(Collectors.toMap(WorkflowStep::name, s -> s));

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();

        // Initialize
        for (WorkflowStep step : workflow.steps()) {
            inDegree.put(step.name(), step.dependencies().size());
            adjacencyList.put(step.name(), new ArrayList<>());
        }

        // Build adjacency list
        for (WorkflowStep step : workflow.steps()) {
            for (String dependency : step.dependencies()) {
                adjacencyList.get(dependency).add(step.name());
            }
        }

        // Topological sort using Kahn's algorithm
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<WorkflowStep> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String stepName = queue.poll();
            sorted.add(stepMap.get(stepName));

            for (String neighbor : adjacencyList.get(stepName)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (sorted.size() != workflow.steps().size()) {
            throw new IllegalArgumentException("Workflow contains a cycle");
        }

        return sorted;
    }

    /**
     * Build context map from dependency results.
     *
     * @param dependencyResults List of dependency task results
     * @return Context map with dependency outputs, or null if empty
     */
    private Map<String, Object> buildContext(final List<TaskResult> dependencyResults) {
        if (dependencyResults.isEmpty()) {
            return null;
        }

        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < dependencyResults.size(); i++) {
            TaskResult result = dependencyResults.get(i);
            context.put("dependency_" + i, result.output());
            context.put("dependency_" + i + "_role", result.agentName());
        }
        return context;
    }

    /**
     * Workflow definition.
     *
     * @param name Workflow name
     * @param steps List of workflow steps
     */
    public record Workflow(String name, List<WorkflowStep> steps) {}

    /**
     * Workflow step definition.
     *
     * @param name Step name
     * @param role Agent role to execute this step
     * @param task Task description for the agent
     * @param dependencies List of step names this step depends on
     */
    public record WorkflowStep(String name, String role, String task, List<String> dependencies) {
        /**
         * Constructs a workflow step with no dependencies.
         *
         * @param stepName Step name
         * @param agentRole Agent role
         * @param taskDesc Task description
         */
        public WorkflowStep(final String stepName, final String agentRole, final String taskDesc) {
            this(stepName, agentRole, taskDesc, List.of());
        }
    }

    /**
     * Workflow execution result.
     *
     * @param workflowName Name of the workflow
     * @param status Execution status (success or failed)
     * @param stepResults Map of step names to task results
     * @param errorMessage Error message if execution failed
     */
    public record WorkflowResult(
            String workflowName,
            String status,
            Map<String, TaskResult> stepResults,
            String errorMessage) {
        /**
         * Create a successful workflow result.
         *
         * @param workflowName Name of the workflow
         * @param stepResults Map of step results
         * @return Successful workflow result
         */
        public static WorkflowResult success(
                final String workflowName, final Map<String, TaskResult> stepResults) {
            return new WorkflowResult(workflowName, "success", stepResults, null);
        }

        /**
         * Create a failed workflow result.
         *
         * @param workflowName Name of the workflow
         * @param errorMessage Error message
         * @return Failed workflow result
         */
        public static WorkflowResult failed(final String workflowName, final String errorMessage) {
            return new WorkflowResult(workflowName, "failed", Map.of(), errorMessage);
        }
    }
}
