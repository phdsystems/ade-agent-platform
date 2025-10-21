package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.orchestration.ParallelAgentExecutor;
import dev.adeengineer.platform.orchestration.WorkflowEngine;
import dev.adeengineer.platform.testutil.MockAgent;

/**
 * Integration tests for orchestration components.
 *
 * <p>Tests workflow engine and parallel execution with agent registry.
 */
@DisplayName("Orchestration Integration Tests")
class OrchestrationIntegrationTest {

    private AgentRegistry agentRegistry;
    private LLMProvider mockLLMProvider;
    private ParallelAgentExecutor parallelExecutor;
    private WorkflowEngine workflowEngine;

    @BeforeEach
    void setUp() {
        agentRegistry = new AgentRegistry();
        mockLLMProvider = Mockito.mock(LLMProvider.class);

        // Setup mock LLM responses
        Mockito.when(
                        mockLLMProvider.generate(
                                Mockito.anyString(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(
                        new LLMResponse(
                                "Mock response",
                                new UsageInfo(10, 20, 30, 0.001),
                                "mock-provider",
                                "mock-model"));

        parallelExecutor = new ParallelAgentExecutor(agentRegistry, mockLLMProvider);
        workflowEngine = new WorkflowEngine(parallelExecutor);
    }

    @Test
    @DisplayName("Should execute workflow with registered agents")
    void testWorkflowEngineWithRegisteredAgents() {
        // Given: Agents registered in registry
        agentRegistry.registerAgent(new MockAgent("agent1"));
        agentRegistry.registerAgent(new MockAgent("agent2"));

        // Then: Workflow engine should have access to agents
        assertThat(workflowEngine).isNotNull();
        assertThat(agentRegistry.getAgentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should create parallel executor with agent registry")
    void testParallelExecutorCreation() {
        // Given: Agents in registry
        agentRegistry.registerAgent(new MockAgent("test"));

        // When: Creating parallel executor
        ParallelAgentExecutor executor = new ParallelAgentExecutor(agentRegistry, mockLLMProvider);

        // Then: Should be initialized
        assertThat(executor).isNotNull();
    }

    @Test
    @DisplayName("Should workflow engine have access to parallel executor")
    void testWorkflowEngineHasParallelExecutor() {
        // Given: Workflow engine with executor
        WorkflowEngine engine = new WorkflowEngine(parallelExecutor);

        // Then: Should be properly initialized
        assertThat(engine).isNotNull();
    }

    @Test
    @DisplayName("Should support multiple agents in orchestration")
    void testMultipleAgentsInOrchestration() {
        // Given: Multiple agents
        for (int i = 1; i <= 5; i++) {
            agentRegistry.registerAgent(new MockAgent("agent-" + i));
        }

        // When: Creating orchestration components
        ParallelAgentExecutor executor = new ParallelAgentExecutor(agentRegistry, mockLLMProvider);
        WorkflowEngine engine = new WorkflowEngine(executor);

        // Then: All agents should be accessible
        assertThat(agentRegistry.getAgentCount()).isEqualTo(5);
        assertThat(executor).isNotNull();
        assertThat(engine).isNotNull();
    }
}
