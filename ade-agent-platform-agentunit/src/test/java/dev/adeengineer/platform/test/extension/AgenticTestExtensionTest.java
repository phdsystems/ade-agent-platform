package dev.adeengineer.platform.test.extension;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockAgent;
import dev.adeengineer.platform.test.annotation.MockLLM;
import dev.adeengineer.platform.test.factory.TestData;
import dev.adeengineer.platform.test.mock.MockLLMProvider;

import adeengineer.dev.agent.TaskResult;

/**
 * Tests for {@link AgenticTestExtension}.
 *
 * <p>Verifies that the extension correctly injects and configures mock instances.
 */
@AgenticTest
class AgenticTestExtensionTest {

    @MockLLM(responseContent = "Custom test response", providerName = "test-llm", model = "gpt-test")
    MockLLMProvider llmProvider;

    @MockAgent(name = "developer", capabilities = {"coding", "testing", "debugging"})
    dev.adeengineer.platform.test.mock.MockAgent developerAgent;

    @MockAgent(name = "reviewer", description = "Code review agent")
    dev.adeengineer.platform.test.mock.MockAgent reviewerAgent;

    @Test
    void shouldInjectMockLLMProvider() {
        // Verify MockLLMProvider was injected
        assertThat(llmProvider).isNotNull();
    }

    @Test
    void shouldConfigureMockLLMProviderFromAnnotation() {
        // Verify configuration from @MockLLM annotation
        assertThat(llmProvider.getProviderName()).isEqualTo("test-llm");
        assertThat(llmProvider.getModel()).isEqualTo("gpt-test");
        assertThat(llmProvider.isHealthy()).isTrue();
    }

    @Test
    void shouldConfigureMockLLMResponseContent() {
        // Verify custom response content
        var response = llmProvider.generate("test prompt", 0.7, 100);
        assertThat(response.content()).isEqualTo("Custom test response");
    }

    @Test
    void shouldInjectMultipleMockAgents() {
        // Verify both agents were injected
        assertThat(developerAgent).isNotNull();
        assertThat(reviewerAgent).isNotNull();
    }

    @Test
    void shouldConfigureMockAgentName() {
        // Verify agent names from annotation
        assertThat(developerAgent.getName()).isEqualTo("developer");
        assertThat(reviewerAgent.getName()).isEqualTo("reviewer");
    }

    @Test
    void shouldConfigureMockAgentCapabilities() {
        // Verify capabilities configuration
        assertThat(developerAgent.getCapabilities())
                .containsExactly("coding", "testing", "debugging");
    }

    @Test
    void shouldConfigureMockAgentDescription() {
        // Verify custom description
        assertThat(reviewerAgent.getDescription()).isEqualTo("Code review agent");
    }

    @Test
    void shouldUseMockAgentDefaultDescription() {
        // Verify default description when not specified
        assertThat(developerAgent.getDescription()).isEqualTo("Mock agent for developer");
    }

    @Test
    void shouldExecuteTaskWithMockAgent() {
        // Verify mock agent is functional
        TaskResult result = developerAgent.executeTask(TestData.validTaskRequest());

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("developer");
    }

    @Test
    void shouldSupportMultipleTestMethods() {
        // Verify mocks work across multiple test methods
        assertThat(llmProvider).isNotNull();
        assertThat(developerAgent).isNotNull();

        // Each test should have fresh instances
        var response = llmProvider.generate("another prompt", 0.5, 50);
        assertThat(response.content()).isEqualTo("Custom test response");
    }

    @Test
    void shouldAllowMockReconfiguration() {
        // Verify mocks can be reconfigured in tests
        llmProvider.withResponseContent("New response");

        var response = llmProvider.generate("test", 0.7, 100);
        assertThat(response.content()).isEqualTo("New response");
    }

    @Test
    void shouldAllowMockAgentReconfiguration() {
        // Verify mock agents can be reconfigured
        TaskResult customResult =
                TaskResult.success("developer", "custom task", "Custom output", null, 500L);
        developerAgent.setResultToReturn(customResult);

        TaskResult result = developerAgent.executeTask(TestData.validTaskRequest());
        assertThat(result.output()).isEqualTo("Custom output");
        assertThat(result.durationMs()).isEqualTo(500L);
    }
}
