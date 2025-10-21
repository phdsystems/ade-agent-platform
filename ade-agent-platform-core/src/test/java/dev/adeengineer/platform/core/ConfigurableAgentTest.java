package dev.adeengineer.platform.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.agent.OutputFormatterRegistry;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

/** Unit tests for ConfigurableAgent. */
class ConfigurableAgentTest {

    private LLMProvider mockLLMProvider;
    private OutputFormatterRegistry mockRegistry;
    private ConfigurableAgent agent;
    private AgentConfig testConfig;

    @BeforeEach
    void setUp() {
        mockLLMProvider = mock(LLMProvider.class);
        mockRegistry = mock(OutputFormatterRegistry.class);

        // Create test agent configuration
        testConfig =
                new AgentConfig(
                        "Test Agent",
                        "A test agent for unit testing",
                        List.of("testing", "validation"),
                        0.7,
                        2048,
                        "You are a {role}. Task: {task}. Context: {context}",
                        "technical");

        agent = new ConfigurableAgent(testConfig, mockLLMProvider, mockRegistry);
    }

    @Test
    void testGetName() {
        assertThat(agent.getName()).isEqualTo("Test Agent");
    }

    @Test
    void testGetDescription() {
        assertThat(agent.getDescription()).isEqualTo("A test agent for unit testing");
    }

    @Test
    void testGetCapabilities() {
        assertThat(agent.getCapabilities()).containsExactly("testing", "validation");
    }

    @Test
    void testGetAgentInfo() {
        AgentInfo info = agent.getAgentInfo();

        assertThat(info.name()).isEqualTo("Test Agent");
        assertThat(info.description()).isEqualTo("A test agent for unit testing");
        assertThat(info.capabilities()).containsExactly("testing", "validation");
        assertThat(info.outputFormat()).isEqualTo("technical");
    }

    @Test
    void testExecuteTaskSuccess() {
        // Arrange
        TaskRequest request =
                new TaskRequest("Test Agent", "Analyze this code", Map.of("file", "test.java"));

        UsageInfo usage = new UsageInfo(50, 100, 150, 0.0005);
        LLMResponse llmResponse =
                new LLMResponse("Analysis complete", usage, "test-provider", "test-model");

        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(llmResponse);
        when(mockRegistry.format(any(), anyString())).thenReturn("Formatted: Analysis complete");

        // Act
        TaskResult result = agent.executeTask(request);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Test Agent");
        assertThat(result.task()).isEqualTo("Analyze this code");
        assertThat(result.output()).isEqualTo("Formatted: Analysis complete");
        assertThat(result.errorMessage()).isNull();

        // Verify metadata
        assertThat(result.metadata()).containsEntry("provider", "test-provider");
        assertThat(result.metadata()).containsEntry("model", "test-model");
        assertThat(result.metadata()).containsEntry("inputTokens", 50);
        assertThat(result.metadata()).containsEntry("outputTokens", 100);
        assertThat(result.metadata()).containsEntry("totalTokens", 150);
        assertThat(result.metadata()).containsEntry("estimatedCost", 0.0005);
    }

    @Test
    void testExecuteTaskBuildPrompt() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Test Agent",
                        "Review PR #123",
                        Map.of("repo", "myrepo", "branch", "feature"));

        UsageInfo usage = new UsageInfo(10, 20, 30, 0.0001);
        LLMResponse llmResponse = new LLMResponse("Review done", usage, "provider", "model");

        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(llmResponse);
        when(mockRegistry.format(any(), anyString())).thenReturn("Review done");

        // Act
        agent.executeTask(request);

        // Assert - verify prompt was built correctly
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> tempCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Integer> tokensCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(mockLLMProvider)
                .generate(promptCaptor.capture(), tempCaptor.capture(), tokensCaptor.capture());

        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("You are a Test Agent");
        assertThat(capturedPrompt).contains("Task: Review PR #123");
        assertThat(capturedPrompt).contains("repo: myrepo");
        assertThat(capturedPrompt).contains("branch: feature");

        assertThat(tempCaptor.getValue()).isEqualTo(0.7);
        assertThat(tokensCaptor.getValue()).isEqualTo(2048);
    }

    @Test
    void testExecuteTaskWithoutContext() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Test Agent", "Simple task", Map.of() // Empty context
                        );

        UsageInfo usage = new UsageInfo(5, 10, 15, 0.0001);
        LLMResponse llmResponse = new LLMResponse("Done", usage, "provider", "model");

        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(llmResponse);
        when(mockRegistry.format(any(), anyString())).thenReturn("Done");

        // Act
        agent.executeTask(request);

        // Assert - verify prompt handles empty context
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLLMProvider).generate(promptCaptor.capture(), anyDouble(), anyInt());

        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Simple task");
        assertThat(capturedPrompt).contains("No additional context provided");
    }

    @Test
    void testExecuteTaskFailure() {
        // Arrange
        TaskRequest request = new TaskRequest("Test Agent", "Failing task", Map.of());

        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenThrow(new RuntimeException("LLM provider error"));

        // Act
        TaskResult result = agent.executeTask(request);

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.agentName()).isEqualTo("Test Agent");
        assertThat(result.task()).isEqualTo("Failing task");
        assertThat(result.output()).isNull();
        assertThat(result.errorMessage()).contains("LLM provider error");
    }

    @Test
    void testGetTemperature() {
        assertThat(agent.getTemperature()).isEqualTo(0.7);
    }

    @Test
    void testGetMaxTokens() {
        assertThat(agent.getMaxTokens()).isEqualTo(2048);
    }

    @Test
    void testGetConfig() {
        AgentConfig config = agent.getConfig();

        assertThat(config.name()).isEqualTo("Test Agent");
        assertThat(config.temperature()).isEqualTo(0.7);
        assertThat(config.maxTokens()).isEqualTo(2048);
    }

    @Test
    void testDifferentOutputFormats() {
        // Test with clinical format
        AgentConfig clinicalConfig =
                new AgentConfig(
                        "Healthcare Agent",
                        "Medical agent",
                        List.of("diagnosis"),
                        0.3,
                        2048,
                        "clinical",
                        "Medical prompt");

        ConfigurableAgent clinicalAgent =
                new ConfigurableAgent(clinicalConfig, mockLLMProvider, mockRegistry);

        TaskRequest request = new TaskRequest("Healthcare Agent", "Diagnose", Map.of());

        UsageInfo usage = new UsageInfo(10, 20, 30, 0.0001);
        LLMResponse response = new LLMResponse("Diagnosis", usage, "p", "m");

        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(response);
        when(mockRegistry.format(any(), anyString())).thenReturn("Clinical format");

        clinicalAgent.executeTask(request);

        // Verify clinical format was requested
        verify(mockRegistry).format(any(), anyString());
    }
}
