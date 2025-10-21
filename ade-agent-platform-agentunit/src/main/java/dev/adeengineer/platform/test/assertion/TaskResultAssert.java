package dev.adeengineer.platform.test.assertion;

import org.assertj.core.api.AbstractAssert;

import adeengineer.dev.agent.TaskResult;

/**
 * Custom AssertJ assertions for TaskResult.
 *
 * <p>Provides fluent assertions for verifying TaskResult properties in a readable way.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * import static dev.adeengineer.platform.test.assertion.AgentAssertions.assertThat;
 *
 * TaskResult result = agent.executeTask(request);
 *
 * assertThat(result)
 *     .isSuccessful()
 *     .hasAgentName("Developer")
 *     .hasOutputContaining("completed")
 *     .hasDurationLessThan(1000L);
 * }</pre>
 */
public class TaskResultAssert extends AbstractAssert<TaskResultAssert, TaskResult> {

    public TaskResultAssert(TaskResult actual) {
        super(actual, TaskResultAssert.class);
    }

    /**
     * Verifies that the task result is successful.
     *
     * @return this assertion
     */
    public TaskResultAssert isSuccessful() {
        isNotNull();
        if (!actual.success()) {
            failWithMessage(
                    "Expected task result to be successful but it failed with error: %s",
                    actual.errorMessage());
        }
        return this;
    }

    /**
     * Verifies that the task result is a failure.
     *
     * @return this assertion
     */
    public TaskResultAssert isFailure() {
        isNotNull();
        if (actual.success()) {
            failWithMessage("Expected task result to be a failure but it was successful");
        }
        return this;
    }

    /**
     * Verifies that the task result has the expected agent name.
     *
     * @param expectedAgentName expected agent name
     * @return this assertion
     */
    public TaskResultAssert hasAgentName(String expectedAgentName) {
        isNotNull();
        if (!actual.agentName().equals(expectedAgentName)) {
            failWithMessage(
                    "Expected agent name to be <%s> but was <%s>",
                    expectedAgentName, actual.agentName());
        }
        return this;
    }

    /**
     * Verifies that the task result has the expected task.
     *
     * @param expectedTask expected task
     * @return this assertion
     */
    public TaskResultAssert hasTask(String expectedTask) {
        isNotNull();
        if (!actual.task().equals(expectedTask)) {
            failWithMessage(
                    "Expected task to be <%s> but was <%s>", expectedTask, actual.task());
        }
        return this;
    }

    /**
     * Verifies that the output contains the expected substring.
     *
     * @param substring expected substring
     * @return this assertion
     */
    public TaskResultAssert hasOutputContaining(String substring) {
        isNotNull();
        isSuccessful(); // Can only check output if successful
        if (!actual.output().contains(substring)) {
            failWithMessage(
                    "Expected output to contain <%s> but was <%s>", substring, actual.output());
        }
        return this;
    }

    /**
     * Verifies that the output equals the expected value.
     *
     * @param expectedOutput expected output
     * @return this assertion
     */
    public TaskResultAssert hasOutput(String expectedOutput) {
        isNotNull();
        isSuccessful();
        if (!actual.output().equals(expectedOutput)) {
            failWithMessage(
                    "Expected output to be <%s> but was <%s>", expectedOutput, actual.output());
        }
        return this;
    }

    /**
     * Verifies that the error message contains the expected substring.
     *
     * @param substring expected substring
     * @return this assertion
     */
    public TaskResultAssert hasErrorMessageContaining(String substring) {
        isNotNull();
        isFailure(); // Can only check error message if failed
        if (!actual.errorMessage().contains(substring)) {
            failWithMessage(
                    "Expected error message to contain <%s> but was <%s>",
                    substring, actual.errorMessage());
        }
        return this;
    }

    /**
     * Verifies that the duration is less than the expected maximum.
     *
     * @param maxDurationMs maximum duration in milliseconds
     * @return this assertion
     */
    public TaskResultAssert hasDurationLessThan(Long maxDurationMs) {
        isNotNull();
        if (actual.durationMs() >= maxDurationMs) {
            failWithMessage(
                    "Expected duration to be less than <%s>ms but was <%s>ms",
                    maxDurationMs, actual.durationMs());
        }
        return this;
    }

    /**
     * Verifies that the duration is greater than the expected minimum.
     *
     * @param minDurationMs minimum duration in milliseconds
     * @return this assertion
     */
    public TaskResultAssert hasDurationGreaterThan(Long minDurationMs) {
        isNotNull();
        if (actual.durationMs() <= minDurationMs) {
            failWithMessage(
                    "Expected duration to be greater than <%s>ms but was <%s>ms",
                    minDurationMs, actual.durationMs());
        }
        return this;
    }

    /**
     * Verifies that the metadata contains the expected key.
     *
     * @param key expected metadata key
     * @return this assertion
     */
    public TaskResultAssert hasMetadataKey(String key) {
        isNotNull();
        if (actual.metadata() == null || !actual.metadata().containsKey(key)) {
            failWithMessage("Expected metadata to contain key <%s> but it was not present", key);
        }
        return this;
    }

    /**
     * Verifies that the metadata contains the expected key-value pair.
     *
     * @param key expected metadata key
     * @param value expected metadata value
     * @return this assertion
     */
    public TaskResultAssert hasMetadata(String key, Object value) {
        isNotNull();
        hasMetadataKey(key);
        Object actualValue = actual.metadata().get(key);
        if (!actualValue.equals(value)) {
            failWithMessage(
                    "Expected metadata[%s] to be <%s> but was <%s>", key, value, actualValue);
        }
        return this;
    }
}
