package dev.adeengineer.platform.test.annotation;

import static dev.adeengineer.platform.test.assertion.AgentAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.adeengineer.platform.test.factory.TestData;
import dev.adeengineer.platform.test.mock.MockLLMProvider;

import adeengineer.dev.agent.TaskResult;

/**
 * Example tests demonstrating {@link AgenticBootTest} annotation usage patterns.
 *
 * <p>These tests serve as both examples and verification that the annotation-based approach works
 * for real-world scenarios.
 */
@AgenticBootTest
class AgenticBootTestExamplesTest {

    @MockLLM(responseContent = "Code review completed successfully")
    MockLLMProvider llmProvider;

    @MockAgent(name = "developer", capabilities = {"coding", "testing", "debugging"})
    dev.adeengineer.platform.test.mock.MockAgent developerAgent;

    @MockAgent(name = "reviewer", capabilities = {"code-review", "security-audit"})
    dev.adeengineer.platform.test.mock.MockAgent reviewerAgent;

    @Test
    void exampleSimpleAgentExecution() {
        // Given a task request
        var request = TestData.validTaskRequest();

        // When agent executes the task
        TaskResult result = developerAgent.executeTask(request);

        // Then task succeeds
        assertThat(result)
                .isSuccessful()
                .hasAgentName("developer")
                .hasOutputContaining("Mock response");
    }

    @Test
    void exampleMultipleAgentsOrchestration() {
        // Given two agents with different capabilities
        assertThat(developerAgent.getCapabilities()).contains("coding");
        assertThat(reviewerAgent.getCapabilities()).contains("code-review");

        // When both execute tasks
        TaskResult devResult = developerAgent.executeTask(TestData.validTaskRequest());
        TaskResult reviewResult = reviewerAgent.executeTask(TestData.validTaskRequest());

        // Then both succeed
        assertThat(devResult).isSuccessful();
        assertThat(reviewResult).isSuccessful();
    }

    @Test
    void exampleLLMProviderGeneration() {
        // Given an LLM provider configured with custom response
        assertThat(llmProvider).isNotNull();
        assertThat(llmProvider.getProviderName()).isEqualTo("test-provider");

        // When generating a response
        var response = llmProvider.generate("Review this code", 0.7, 500);

        // Then response matches configuration
        assertThat(response.content()).isEqualTo("Code review completed successfully");
        assertThat(response.usage()).isNotNull();
    }

    @Test
    void exampleCustomizingMockBehavior() {
        // Given a mock configured to return custom result
        TaskResult customResult =
                TaskResult.success(
                        "developer", "custom task", "Successfully implemented feature X", null, 2000L);
        developerAgent.setResultToReturn(customResult);

        // When executing task
        TaskResult result = developerAgent.executeTask(TestData.validTaskRequest());

        // Then custom result is returned
        assertThat(result.output()).isEqualTo("Successfully implemented feature X");
        assertThat(result.durationMs()).isEqualTo(2000L);
    }

    @Test
    void exampleSimulatingFailure() {
        // Given a mock configured to throw exception
        developerAgent.setExceptionToThrow(new RuntimeException("Task execution failed"));

        // When executing task
        // Then exception is thrown
        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> developerAgent.executeTask(TestData.validTaskRequest()));
    }

    @Test
    void exampleDynamicLLMConfiguration() {
        // Given an LLM provider that can be reconfigured
        llmProvider.withResponseContent("First response");
        var firstResponse = llmProvider.generate("prompt", 0.7, 100);

        // When reconfiguring the provider
        llmProvider.withResponseContent("Second response");
        var secondResponse = llmProvider.generate("prompt", 0.7, 100);

        // Then responses reflect configuration
        assertThat(firstResponse.content()).isEqualTo("First response");
        assertThat(secondResponse.content()).isEqualTo("Second response");
    }

    @Test
    void exampleAgentCapabilitiesValidation() {
        // Given agents with specific capabilities
        // When checking capabilities
        boolean canCode = developerAgent.getCapabilities().contains("coding");
        boolean canReview = reviewerAgent.getCapabilities().contains("code-review");

        // Then capabilities match configuration
        assertThat(canCode).isTrue();
        assertThat(canReview).isTrue();
    }

    @Test
    void exampleNoBoilerplateSetup() {
        // Note: No @BeforeEach setup needed
        // Note: No manual mock instantiation needed
        // Mocks are ready to use immediately

        // Just write test logic
        TaskResult result = developerAgent.executeTask(TestData.validTaskRequest());
        assertThat(result).isSuccessful();
    }
}
