package dev.adeengineer.platform.test.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

/** Mock implementation of Agent for testing. */
public class MockAgent implements Agent {

    private final String name;
    private final String description;
    private final List<String> capabilities;
    private final String outputFormat;
    private TaskResult resultToReturn;
    private RuntimeException exceptionToThrow;

    public MockAgent(String name) {
        this(name, "Mock agent for " + name, List.of("capability1", "capability2"));
    }

    public MockAgent(String name, String description, List<String> capabilities) {
        this.name = name;
        this.description = description;
        this.capabilities = capabilities;
        this.outputFormat = "technical";

        // Default successful result
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalTokens", 100);
        metadata.put("inputTokens", 50);
        metadata.put("outputTokens", 50);

        this.resultToReturn =
                TaskResult.success(name, "test task", "Mock response from " + name, metadata, 100L);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<String> getCapabilities() {
        return capabilities;
    }

    @Override
    public TaskResult executeTask(TaskRequest request) {
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }

        // Update result with actual request details
        if (resultToReturn.success()) {
            Map<String, Object> metadata =
                    resultToReturn.metadata() != null
                            ? new HashMap<>(resultToReturn.metadata())
                            : new HashMap<>();

            return TaskResult.success(
                    name,
                    request.task(),
                    resultToReturn.output(),
                    metadata,
                    resultToReturn.durationMs());
        } else {
            return TaskResult.failure(name, request.task(), resultToReturn.errorMessage());
        }
    }

    @Override
    public AgentInfo getAgentInfo() {
        return new AgentInfo(name, description, capabilities, outputFormat);
    }

    // Test helper methods
    public void setResultToReturn(TaskResult result) {
        this.resultToReturn = result;
    }

    public void setExceptionToThrow(RuntimeException exception) {
        this.exceptionToThrow = exception;
    }
}
