package dev.adeengineer.platform.test.base;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Base class for integration tests.
 *
 * <p>Provides common setup for integration testing with mocked LLM provider.
 * Subclasses should initialize their own platform components (AgentRegistry, DomainLoader, etc.)
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * class OrchestrationIntegrationTest extends BaseIntegrationTest {
 *
 *     private AgentRegistry agentRegistry;
 *     private ParallelAgentExecutor parallelExecutor;
 *
 *     @BeforeEach
 *     void setUp() {
 *         super.setUpIntegration(); // Call parent to set up mockLLMProvider
 *
 *         // Initialize your components
 *         agentRegistry = new AgentRegistry();
 *         parallelExecutor = new ParallelAgentExecutor(agentRegistry, mockLLMProvider);
 *     }
 *
 *     @Test
 *     void shouldExecuteWorkflow() {
 *         MockAgent agent = new MockAgent("test");
 *         agentRegistry.registerAgent(agent);
 *         // Test logic...
 *     }
 * }
 * }</pre>
 */
public abstract class BaseIntegrationTest {

    /** Mock LLM provider (Mockito mock) */
    protected LLMProvider mockLLMProvider;

    /**
     * Sets up integration test infrastructure before each test.
     *
     * <p>Initializes and configures mock LLM provider with default responses.
     *
     * <p>Note: Subclasses should call this method if they override @BeforeEach.
     */
    @BeforeEach
    protected void setUpIntegration() {
        // Create and configure mock LLM provider
        mockLLMProvider = Mockito.mock(LLMProvider.class);
        configureMockLLMProvider();
    }

    /**
     * Configures the mock LLM provider with default test responses.
     *
     * <p>Override this method to customize LLM mock behavior.
     */
    protected void configureMockLLMProvider() {
        UsageInfo defaultUsage = new UsageInfo(10, 20, 30, 0.001);
        LLMResponse defaultResponse =
                new LLMResponse(
                        "Integration test response", defaultUsage, "mock-provider", "mock-model");

        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(defaultResponse);
        when(mockLLMProvider.isHealthy()).thenReturn(true);
        when(mockLLMProvider.getProviderName()).thenReturn("mock-provider");
        when(mockLLMProvider.getModel()).thenReturn("mock-model");
    }

    /**
     * Configures mock LLM provider with a specific response.
     *
     * @param responseContent content to return
     */
    protected void configureMockLLMResponse(String responseContent) {
        UsageInfo usage = new UsageInfo(10, 20, 30, 0.001);
        LLMResponse response =
                new LLMResponse(responseContent, usage, "mock-provider", "mock-model");

        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(response);
    }
}
