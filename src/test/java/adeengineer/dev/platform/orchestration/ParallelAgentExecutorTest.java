package adeengineer.dev.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;
import adeengineer.dev.llm.LLMProvider;
import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.llm.model.UsageInfo;
import adeengineer.dev.platform.core.AgentRegistry;
import adeengineer.dev.platform.testutil.MockAgent;

/** Unit tests for ParallelAgentExecutor. */
@ExtendWith(MockitoExtension.class)
class ParallelAgentExecutorTest {

    @Mock private AgentRegistry agentRegistry;

    @Mock private LLMProvider llmProvider;

    private ParallelAgentExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ParallelAgentExecutor(agentRegistry, llmProvider);
    }

    @Test
    void shouldExecuteMultipleTasksInParallel() {
        // Given
        MockAgent developerAgent = new MockAgent("Developer");
        MockAgent qaAgent = new MockAgent("QA Engineer");

        when(agentRegistry.getAgent("Developer")).thenReturn(developerAgent);
        when(agentRegistry.getAgent("QA Engineer")).thenReturn(qaAgent);

        LLMResponse devResponse =
                new LLMResponse(
                        "Code implementation", new UsageInfo(10, 50, 60, 0.001), "openai", "gpt-4");
        LLMResponse qaResponse =
                new LLMResponse("Test plan", new UsageInfo(10, 40, 50, 0.001), "openai", "gpt-4");

        when(llmProvider.generate(any(), anyDouble(), anyInt()))
                .thenReturn(devResponse, qaResponse);

        List<TaskRequest> tasks =
                List.of(
                        new TaskRequest("Developer", "Implement feature X", null),
                        new TaskRequest("QA Engineer", "Create test plan", null));

        // When
        List<TaskResult> results = executor.executeParallel(tasks);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).success()).isTrue();
        assertThat(results.get(0).output()).isEqualTo("Code implementation");
        assertThat(results.get(1).success()).isTrue();
        assertThat(results.get(1).output()).isEqualTo("Test plan");
    }

    @Test
    void shouldHandleContextInTaskRequests() {
        // Given
        MockAgent developerAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(developerAgent);

        LLMResponse response =
                new LLMResponse(
                        "Implementation with context",
                        new UsageInfo(10, 50, 60, 0.001),
                        "openai",
                        "gpt-4");
        when(llmProvider.generate(any(), anyDouble(), anyInt())).thenReturn(response);

        Map<String, Object> context =
                Map.of(
                        "previousOutput", "Design completed",
                        "requirements", "Must use Java 21");
        TaskRequest task = new TaskRequest("Developer", "Implement feature", context);

        // When
        List<TaskResult> results = executor.executeParallel(List.of(task));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isTrue();
        assertThat(results.get(0).output()).contains("Implementation");
    }

    @Test
    void shouldHandleAgentNotFound() {
        // Given
        when(agentRegistry.getAgent("NonExistentRole")).thenReturn(null);

        TaskRequest task = new TaskRequest("NonExistentRole", "Some task", null);

        // When
        List<TaskResult> results = executor.executeParallel(List.of(task));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isFalse();
        assertThat(results.get(0).errorMessage()).contains("Agent not found");
    }

    @Test
    void shouldExecuteWithConcurrencyLimit() {
        // Given
        MockAgent developerAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(developerAgent);

        LLMResponse response =
                new LLMResponse("Response", new UsageInfo(10, 50, 60, 0.001), "openai", "gpt-4");
        when(llmProvider.generate(any(), anyDouble(), anyInt())).thenReturn(response);

        List<TaskRequest> tasks =
                List.of(
                        new TaskRequest("Developer", "Task 1", null),
                        new TaskRequest("Developer", "Task 2", null),
                        new TaskRequest("Developer", "Task 3", null),
                        new TaskRequest("Developer", "Task 4", null),
                        new TaskRequest("Developer", "Task 5", null));

        // When
        List<TaskResult> results = executor.executeParallelWithLimit(tasks, 2);

        // Then
        assertThat(results).hasSize(5);
        assertThat(results).allMatch(TaskResult::success);
    }

    @Test
    void shouldExecuteInBatches() {
        // Given
        MockAgent developerAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(developerAgent);

        LLMResponse response =
                new LLMResponse("Response", new UsageInfo(10, 50, 60, 0.001), "openai", "gpt-4");
        when(llmProvider.generate(any(), anyDouble(), anyInt())).thenReturn(response);

        List<TaskRequest> tasks =
                List.of(
                        new TaskRequest("Developer", "Task 1", null),
                        new TaskRequest("Developer", "Task 2", null),
                        new TaskRequest("Developer", "Task 3", null),
                        new TaskRequest("Developer", "Task 4", null),
                        new TaskRequest("Developer", "Task 5", null));

        // When
        List<TaskResult> results = executor.executeBatched(tasks, 2);

        // Then
        assertThat(results).hasSize(5);
        assertThat(results).allMatch(TaskResult::success);
    }

    @Test
    void shouldAggregateResults() {
        // Given
        MockAgent developerAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(developerAgent);

        LLMResponse response =
                new LLMResponse("Response", new UsageInfo(10, 50, 60, 0.001), "openai", "gpt-4");
        when(llmProvider.generate(any(), anyDouble(), anyInt())).thenReturn(response);

        List<TaskRequest> tasks =
                List.of(
                        new TaskRequest("Developer", "Task 1", null),
                        new TaskRequest("Developer", "Task 2", null));

        // When
        String aggregated =
                executor.executeAndAggregate(
                        tasks,
                        results ->
                                results.stream()
                                        .map(TaskResult::output)
                                        .reduce("", (a, b) -> a + " | " + b));

        // Then
        assertThat(aggregated).contains("Response | Response");
    }

    @Test
    void shouldTrackExecutionDuration() {
        // Given
        MockAgent developerAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(developerAgent);

        LLMResponse response =
                new LLMResponse("Response", new UsageInfo(10, 50, 60, 0.001), "openai", "gpt-4");
        when(llmProvider.generate(any(), anyDouble(), anyInt())).thenReturn(response);

        TaskRequest task = new TaskRequest("Developer", "Task", null);

        // When
        List<TaskResult> results = executor.executeParallel(List.of(task));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldHandleEmptyTaskList() {
        // When
        List<TaskResult> results = executor.executeParallel(List.of());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void shouldHandleLLMProviderException() {
        // Given
        MockAgent developerAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(developerAgent);
        when(llmProvider.generate(any(), anyDouble(), anyInt()))
                .thenThrow(new RuntimeException("LLM error"));

        TaskRequest task = new TaskRequest("Developer", "Task", null);

        // When
        List<TaskResult> results = executor.executeParallel(List.of(task));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isFalse();
        assertThat(results.get(0).errorMessage()).contains("Execution failed");
    }
}
