package dev.adeengineer.platform.orchestration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.core.AgentRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Parallel agent execution service using virtual threads.
 *
 * <p>Enables multiple agents to work concurrently on independent tasks.
 */
@Slf4j
public class ParallelAgentExecutor {

    /** Default parallel execution timeout in minutes. */
    private static final int DEFAULT_TIMEOUT_MINUTES = 5;

    /** Default LLM temperature for task execution. */
    private static final double DEFAULT_TEMPERATURE = 0.7;

    /** Default maximum tokens for LLM generation. */
    private static final int DEFAULT_MAX_TOKENS = 2000;

    /** Shutdown timeout in seconds. */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    /** Registry for looking up agents. */
    private final AgentRegistry agentRegistry;

    /** LLM provider for task execution. */
    private final LLMProvider llmProvider;

    /** Executor service using virtual threads. */
    private final ExecutorService executorService;

    /**
     * Constructs a new ParallelAgentExecutor.
     *
     * @param registry Registry for looking up agents
     * @param provider LLM provider for task execution
     */
    public ParallelAgentExecutor(final AgentRegistry registry, final LLMProvider provider) {
        this.agentRegistry = registry;
        this.llmProvider = provider;
        // Use virtual threads for lightweight concurrency (Java 21+)
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Execute multiple tasks in parallel using different agents.
     *
     * @param tasks List of tasks to execute
     * @return List of results in the same order as input tasks
     */
    public List<TaskResult> executeParallel(final List<TaskRequest> tasks) {
        log.info("Executing {} tasks in parallel", tasks.size());
        Instant startTime = Instant.now();

        // Submit all tasks
        List<CompletableFuture<TaskResult>> futures =
                tasks.stream()
                        .map(
                                task ->
                                        CompletableFuture.supplyAsync(
                                                () -> executeTask(task), executorService))
                        .toList();

        // Wait for all to complete
        CompletableFuture<Void> allOf =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            // Wait with timeout
            allOf.get(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("Parallel execution timed out after 5 minutes");
            futures.forEach(f -> f.cancel(true));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Parallel execution failed: {}", e.getMessage(), e);
        }

        // Collect results
        List<TaskResult> results = new ArrayList<>();
        for (CompletableFuture<TaskResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.error("Failed to get task result: {}", e.getMessage());
                results.add(
                        TaskResult.failure(
                                "unknown", "unknown", "Task execution failed: " + e.getMessage()));
            }
        }

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.info("Completed {} tasks in {}ms", tasks.size(), durationMs);

        return results;
    }

    /**
     * Execute multiple tasks in parallel and aggregate results.
     *
     * @param tasks List of tasks to execute
     * @param aggregator Function to aggregate results
     * @param <R> Result type
     * @return Aggregated result
     */
    public <R> R executeAndAggregate(
            final List<TaskRequest> tasks,
            final java.util.function.Function<List<TaskResult>, R> aggregator) {
        List<TaskResult> results = executeParallel(tasks);
        return aggregator.apply(results);
    }

    /**
     * Execute tasks in parallel with a custom thread pool size.
     *
     * @param tasks List of tasks to execute
     * @param maxConcurrency Maximum concurrent executions
     * @return List of results
     */
    public List<TaskResult> executeParallelWithLimit(
            final List<TaskRequest> tasks, final int maxConcurrency) {
        log.info("Executing {} tasks with max concurrency of {}", tasks.size(), maxConcurrency);

        ExecutorService limitedExecutor = Executors.newFixedThreadPool(maxConcurrency);
        List<Future<TaskResult>> futures = new ArrayList<>();

        try {
            // Submit all tasks
            for (TaskRequest task : tasks) {
                Future<TaskResult> future = limitedExecutor.submit(() -> executeTask(task));
                futures.add(future);
            }

            // Collect results
            List<TaskResult> results = new ArrayList<>();
            for (Future<TaskResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    log.error("Failed to get task result: {}", e.getMessage());
                    results.add(
                            TaskResult.failure(
                                    "unknown",
                                    "unknown",
                                    "Task execution failed: " + e.getMessage()));
                }
            }

            return results;
        } finally {
            limitedExecutor.shutdown();
        }
    }

    /**
     * Execute tasks in batches with specified batch size.
     *
     * @param tasks List of tasks to execute
     * @param batchSize Number of tasks per batch
     * @return List of results
     */
    public List<TaskResult> executeBatched(final List<TaskRequest> tasks, final int batchSize) {
        log.info("Executing {} tasks in batches of {}", tasks.size(), batchSize);

        List<TaskResult> allResults = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tasks.size());
            List<TaskRequest> batch = tasks.subList(i, end);
            int batchNum = (i / batchSize) + 1;
            int totalBatches = (tasks.size() + batchSize - 1) / batchSize;
            log.debug("Processing batch {}/{}", batchNum, totalBatches);

            List<TaskResult> batchResults = executeParallel(batch);
            allResults.addAll(batchResults);
        }

        return allResults;
    }

    /**
     * Execute a task with a specific agent.
     *
     * @param task Task to execute
     * @return Task result
     */
    private TaskResult executeTask(final TaskRequest task) {
        try {
            log.debug("Executing task for agent: {}", task.agentName());
            long startTime = System.currentTimeMillis();

            // Get agent
            var agent = agentRegistry.getAgent(task.agentName());
            if (agent == null) {
                return TaskResult.failure(
                        task.agentName(), task.task(), "Agent not found: " + task.agentName());
            }

            // Execute task using the agent
            TaskResult result = agent.executeTask(task);

            return result;
        } catch (Exception e) {
            log.error(
                    "Task execution failed for agent {}: {}", task.agentName(), e.getMessage(), e);
            return TaskResult.failure(
                    task.agentName(), task.task(), "Execution failed: " + e.getMessage());
        }
    }

    /** Shutdown the executor service. */
    public void shutdown() {
        log.info("Shutting down parallel executor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
