package dev.adeengineer.platform.spring.testutil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

/** Test data factory for creating test objects with sensible defaults. */
public class TestData {

    // TaskRequest factory methods
    public static TaskRequest validTaskRequest() {
        return new TaskRequest("Developer", "Write unit tests", Map.of("priority", "high"));
    }

    public static TaskRequest taskRequestWithRole(String agentName) {
        return new TaskRequest(agentName, "Test task", Map.of());
    }

    public static TaskRequest taskRequestWithTask(String task) {
        return new TaskRequest("Developer", task, Map.of());
    }

    // AgentConfig factory methods
    public static AgentConfig validAgentConfig() {
        return new AgentConfig(
                "Developer",
                "Software development agent",
                List.of("coding", "testing", "debugging"),
                0.7,
                1000,
                "You are a {role}. Task: {task}",
                "technical");
    }

    public static AgentConfig agentConfigWithRole(String name) {
        return new AgentConfig(
                name,
                "Test agent",
                List.of("capability1", "capability2"),
                0.7,
                500,
                "Template",
                "technical");
    }

    public static AgentConfig agentConfigWithTemperature(double temperature) {
        return new AgentConfig(
                "Test", "Test agent", List.of("test"), temperature, 500, "Template", "technical");
    }

    public static AgentConfig agentConfigWithMaxTokens(int maxTokens) {
        return new AgentConfig(
                "Test", "Test agent", List.of("test"), 0.7, maxTokens, "Template", "technical");
    }

    // UsageInfo factory methods (for LLM responses)
    public static UsageInfo validUsageInfo() {
        return new UsageInfo(100, 200, 300, 0.01);
    }

    public static UsageInfo usageInfoWithTokens(int inputTokens, int outputTokens) {
        return new UsageInfo(inputTokens, outputTokens, inputTokens + outputTokens, 0.01);
    }

    // LLMResponse factory methods
    public static LLMResponse validLLMResponse() {
        return new LLMResponse(
                "This is a test response", validUsageInfo(), "test-provider", "test-model");
    }

    public static LLMResponse llmResponseWithContent(String content) {
        return new LLMResponse(content, validUsageInfo(), "test-provider", "test-model");
    }

    public static LLMResponse llmResponseWithProvider(String provider, String model) {
        return new LLMResponse("Test content", validUsageInfo(), provider, model);
    }

    // TaskResult factory methods
    public static TaskResult successfulTaskResult() {
        Map<String, Object> metadata = createMetadata(validUsageInfo());
        return TaskResult.success(
                "Developer", "Test task", "Task completed successfully", metadata, 1000L);
    }

    public static TaskResult failedTaskResult() {
        return TaskResult.failure("Developer", "Test task", "Task failed: Something went wrong");
    }

    public static TaskResult taskResultWithRole(String agentName) {
        Map<String, Object> metadata = createMetadata(validUsageInfo());
        return TaskResult.success(agentName, "Test task", "Output", metadata, 500L);
    }

    // AgentInfo factory methods
    public static AgentInfo validAgentInfo() {
        return new AgentInfo(
                "Developer",
                "Software development agent",
                List.of("coding", "testing", "debugging"),
                "technical");
    }

    public static AgentInfo roleInfoWithRole(String name) {
        return new AgentInfo(name, "Description for " + name, List.of("capability1"), "technical");
    }

    // Helper methods
    private static Map<String, Object> createMetadata(UsageInfo usage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("inputTokens", usage.inputTokens());
        metadata.put("outputTokens", usage.outputTokens());
        metadata.put("totalTokens", usage.totalTokens());
        metadata.put("estimatedCost", usage.estimatedCost());
        return metadata;
    }

    public static String blankString() {
        return "   ";
    }

    public static String longString(int length) {
        return "a".repeat(length);
    }
}
