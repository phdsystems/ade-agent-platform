package dev.adeengineer.platform.test.base;

import org.junit.jupiter.api.BeforeEach;

import dev.adeengineer.platform.test.mock.MockLLMProvider;

/**
 * Base class for agent-related unit tests.
 *
 * <p>Provides common setup and utilities for testing agents. Subclasses can extend this to get
 * standard test infrastructure.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * class MyAgentTest extends BaseAgentTest {
 *
 *     private AgentRegistry agentRegistry;
 *
 *     @BeforeEach
 *     void setUp() {
 *         super.setUpBaseAgent(); // Call parent setup
 *         agentRegistry = new AgentRegistry(); // Initialize your components
 *     }
 *
 *     @Test
 *     void shouldExecuteTask() {
 *         // mockLLMProvider is available from parent
 *         MockAgent agent = new MockAgent("test-agent");
 *         TaskResult result = agent.executeTask(validTaskRequest());
 *         assertThat(result).isSuccessful();
 *     }
 * }
 * }</pre>
 */
public abstract class BaseAgentTest {

    /** Mock LLM provider for testing without real API calls */
    protected MockLLMProvider mockLLMProvider;

    /**
     * Sets up common test infrastructure before each test.
     *
     * <p>Initializes MockLLMProvider with sensible defaults.
     *
     * <p>Note: Subclasses should call this method if they override @BeforeEach.
     */
    @BeforeEach
    protected void setUpBaseAgent() {
        mockLLMProvider = new MockLLMProvider();
    }

    /**
     * Configures the mock LLM provider with a custom response.
     *
     * @param responseContent content to return from LLM calls
     */
    protected void configureMockLLM(String responseContent) {
        mockLLMProvider.withResponseContent(responseContent);
    }

    /**
     * Configures the mock LLM provider to throw an exception.
     *
     * @param exception exception to throw
     */
    protected void configureMockLLMToFail(RuntimeException exception) {
        mockLLMProvider.withException(exception);
    }
}
