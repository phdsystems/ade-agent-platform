package dev.adeengineer.platform.test.assertion;

import adeengineer.dev.agent.TaskResult;

/**
 * Entry point for custom AgentUnit assertions.
 *
 * <p>Import this class statically to use custom assertions:
 *
 * <pre>{@code
 * import static dev.adeengineer.platform.test.assertion.AgentAssertions.assertThat;
 *
 * TaskResult result = agent.executeTask(request);
 * assertThat(result).isSuccessful().hasAgentName("Developer");
 * }</pre>
 */
public class AgentAssertions {

    /**
     * Creates assertions for TaskResult.
     *
     * @param actual the task result to assert
     * @return assertion object
     */
    public static TaskResultAssert assertThat(TaskResult actual) {
        return new TaskResultAssert(actual);
    }

    private AgentAssertions() {
        // Utility class - no instantiation
    }
}
