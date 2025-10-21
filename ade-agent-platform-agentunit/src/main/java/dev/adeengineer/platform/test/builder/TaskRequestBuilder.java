package dev.adeengineer.platform.test.builder;

import java.util.HashMap;
import java.util.Map;

import adeengineer.dev.agent.TaskRequest;

/**
 * Fluent builder for creating TaskRequest instances in tests.
 *
 * <p>Provides a convenient way to create TaskRequest objects with sensible defaults and fluent API
 * for customization.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TaskRequest request = TaskRequestBuilder.builder()
 *     .agentName("Developer")
 *     .task("Write unit tests for UserService")
 *     .context("priority", "high")
 *     .context("deadline", "2024-01-15")
 *     .build();
 * }</pre>
 */
public class TaskRequestBuilder {

    private String agentName = "TestAgent";
    private String task = "Test task";
    private Map<String, Object> context = new HashMap<>();

    private TaskRequestBuilder() {}

    /**
     * Creates a new builder instance.
     *
     * @return new builder
     */
    public static TaskRequestBuilder builder() {
        return new TaskRequestBuilder();
    }

    /**
     * Sets the agent name.
     *
     * @param agentName name of the agent to execute the task
     * @return this builder
     */
    public TaskRequestBuilder agentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    /**
     * Sets the task description.
     *
     * @param task task description
     * @return this builder
     */
    public TaskRequestBuilder task(String task) {
        this.task = task;
        return this;
    }

    /**
     * Adds a context entry.
     *
     * @param key context key
     * @param value context value
     * @return this builder
     */
    public TaskRequestBuilder context(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * Sets the entire context map.
     *
     * @param context context map
     * @return this builder
     */
    public TaskRequestBuilder context(Map<String, Object> context) {
        this.context = new HashMap<>(context);
        return this;
    }

    /**
     * Adds multiple context entries.
     *
     * @param key1 first key
     * @param value1 first value
     * @param key2 second key
     * @param value2 second value
     * @return this builder
     */
    public TaskRequestBuilder context(String key1, Object value1, String key2, Object value2) {
        this.context.put(key1, value1);
        this.context.put(key2, value2);
        return this;
    }

    /**
     * Builds the TaskRequest instance.
     *
     * @return configured TaskRequest
     */
    public TaskRequest build() {
        return new TaskRequest(agentName, task, context);
    }
}
