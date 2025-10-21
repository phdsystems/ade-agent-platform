package dev.adeengineer.platform.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.adeengineer.agent.Agent;
import dev.adeengineer.agent.AgentInfo;
import dev.adeengineer.agent.TaskRequest;
import dev.adeengineer.agent.TaskResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates agent execution and multi-agent collaboration.
 *
 * <p>Core service for the Role Manager application.
 */
@Slf4j
@Service
public class RoleManager {

    /** Registry containing all available agents. */
    private final AgentRegistry agentRegistry;

    /**
     * Constructs a new RoleManager.
     *
     * @param registry Registry containing all available agents
     */
    public RoleManager(final AgentRegistry registry) {
        this.agentRegistry = registry;
    }

    /**
     * Execute a task with a single agent.
     *
     * @param request The task request
     * @return The task result
     */
    public TaskResult executeTask(final TaskRequest request) {
        log.info("Executing task for agent: {}", request.agentName());

        try {
            // Get the agent (throws IllegalArgumentException if not found)
            Agent agent = agentRegistry.getAgent(request.agentName());

            // Execute the task - BaseAgent handles everything
            TaskResult result = agent.executeTask(request);

            log.info(
                    "Task completed for agent {} in {}ms",
                    request.agentName(),
                    result.durationMs());

            return result;

        } catch (IllegalArgumentException e) {
            // Re-throw validation errors so controller returns 400 Bad Request
            log.warn("Invalid agent requested: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(
                    "Task execution failed for agent {}: {}",
                    request.agentName(),
                    e.getMessage(),
                    e);

            return TaskResult.failure(request.agentName(), request.task(), e.getMessage());
        }
    }

    /**
     * Execute a task with multiple agents in parallel.
     *
     * @param agentNames List of agent names to execute
     * @param task The task description
     * @param context Additional context
     * @return Map of agent name to task result
     */
    public Map<String, TaskResult> executeMultiAgentTask(
            final List<String> agentNames, final String task, final Map<String, Object> context) {
        log.info("Executing multi-agent task with {} agents: {}", agentNames.size(), agentNames);

        long startTime = System.currentTimeMillis();

        try {
            // Validate all agents exist
            for (String agentName : agentNames) {
                if (!agentRegistry.hasAgent(agentName)) {
                    throw new IllegalArgumentException("Unknown agent: " + agentName);
                }
            }

            // Execute all agents in parallel
            List<CompletableFuture<TaskResult>> futures =
                    agentNames.stream()
                            .map(
                                    agentName ->
                                            CompletableFuture.supplyAsync(
                                                    () -> {
                                                        TaskRequest request =
                                                                new TaskRequest(
                                                                        agentName, task, context);
                                                        return executeTask(request);
                                                    }))
                            .collect(Collectors.toList());

            // Wait for all to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results
            Map<String, TaskResult> results =
                    futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toMap(TaskResult::agentName, result -> result));

            long duration = System.currentTimeMillis() - startTime;
            log.info("Multi-agent task completed in {}ms", duration);

            return results;

        } catch (IllegalArgumentException e) {
            // Re-throw validation errors so controller returns 400 Bad Request
            log.warn("Invalid multi-agent request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Multi-agent task execution failed: {}", e.getMessage(), e);
            throw new RuntimeException("Multi-agent task failed: " + e.getMessage(), e);
        }
    }

    /**
     * List all available agents.
     *
     * @return List of available agent names
     */
    public List<String> listRoles() {
        return agentRegistry.getAvailableAgents();
    }

    /**
     * Get information about all agents.
     *
     * @return List of agent information objects
     */
    public List<AgentInfo> getRolesInfo() {
        return agentRegistry.getAllAgentsInfo();
    }

    /**
     * Get information about a specific agent.
     *
     * @param agentName The agent name
     * @return Agent information
     * @throws IllegalArgumentException if agent not found
     */
    public AgentInfo describeRole(final String agentName) {
        Agent agent = agentRegistry.getAgent(agentName);
        return agent.getAgentInfo();
    }
}
