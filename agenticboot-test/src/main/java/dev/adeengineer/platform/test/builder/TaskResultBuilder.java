package dev.adeengineer.platform.test.builder;

import java.util.HashMap;
import java.util.Map;

import adeengineer.dev.agent.TaskResult;

/**
 * Fluent builder for creating TaskResult instances in tests.
 *
 * <p>Provides a convenient way to create TaskResult objects with sensible defaults and fluent API
 * for customization.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TaskResult result = TaskResultBuilder.success()
 *     .agentName("Developer")
 *     .task("Write unit tests")
 *     .output("Tests written successfully")
 *     .metadata("totalTokens", 150)
 *     .durationMs(1000L)
 *     .build();
 *
 * TaskResult failure = TaskResultBuilder.failure()
 *     .agentName("Developer")
 *     .task("Invalid task")
 *     .errorMessage("Task validation failed")
 *     .build();
 * }</pre>
 */
public class TaskResultBuilder {

    private String agentName = "TestAgent";
    private String task = "Test task";
    private String output;
    private Map<String, Object> metadata = new HashMap<>();
    private Long durationMs = 100L;
    private String errorMessage;
    private boolean isSuccess = true;

    private TaskResultBuilder(boolean isSuccess) {
        this.isSuccess = isSuccess;
        if (isSuccess) {
            this.output = "Task completed successfully";
        }
    }

    /**
     * Creates a builder for a successful task result.
     *
     * @return new builder for success result
     */
    public static TaskResultBuilder success() {
        return new TaskResultBuilder(true);
    }

    /**
     * Creates a builder for a failed task result.
     *
     * @return new builder for failure result
     */
    public static TaskResultBuilder failure() {
        return new TaskResultBuilder(false);
    }

    /**
     * Sets the agent name.
     *
     * @param agentName name of the agent that executed the task
     * @return this builder
     */
    public TaskResultBuilder agentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    /**
     * Sets the task description.
     *
     * @param task task description
     * @return this builder
     */
    public TaskResultBuilder task(String task) {
        this.task = task;
        return this;
    }

    /**
     * Sets the output (for successful results).
     *
     * @param output task output
     * @return this builder
     */
    public TaskResultBuilder output(String output) {
        this.output = output;
        return this;
    }

    /**
     * Sets the error message (for failed results).
     *
     * @param errorMessage error message
     * @return this builder
     */
    public TaskResultBuilder errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * Adds a metadata entry.
     *
     * @param key metadata key
     * @param value metadata value
     * @return this builder
     */
    public TaskResultBuilder metadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Sets the entire metadata map.
     *
     * @param metadata metadata map
     * @return this builder
     */
    public TaskResultBuilder metadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
        return this;
    }

    /**
     * Sets the execution duration in milliseconds.
     *
     * @param durationMs duration in milliseconds
     * @return this builder
     */
    public TaskResultBuilder durationMs(Long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    /**
     * Builds the TaskResult instance.
     *
     * @return configured TaskResult
     */
    public TaskResult build() {
        if (isSuccess) {
            return TaskResult.success(agentName, task, output, metadata, durationMs);
        } else {
            return TaskResult.failure(agentName, task, errorMessage);
        }
    }
}
