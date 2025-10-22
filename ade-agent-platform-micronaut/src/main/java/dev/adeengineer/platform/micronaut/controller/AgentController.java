package dev.adeengineer.platform.micronaut.controller;

import java.util.List;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;

import dev.adeengineer.platform.core.AgentRegistry;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP controller for agent execution endpoints.
 *
 * <p>Provides REST API for discovering and executing agents.
 *
 * @since 0.2.0
 */
@Slf4j
@Controller("/api/agents")
public class AgentController {

    private final AgentRegistry agentRegistry;

    public AgentController(final AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    /**
     * Get all registered agents.
     *
     * @return List of agent information
     */
    @Get
    @Produces(MediaType.APPLICATION_JSON)
    public List<AgentInfo> listAgents() {
        log.info("Listing all agents");
        return agentRegistry.getAllAgentsInfo();
    }

    /**
     * Get specific agent information.
     *
     * @param agentName Agent name
     * @return Agent information
     */
    @Get("/{agentName}")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> getAgent(@PathVariable final String agentName) {
        log.info("Getting agent: {}", agentName);

        if (!agentRegistry.hasAgent(agentName)) {
            return HttpResponse.notFound(
                    new ErrorResponse(
                            "Agent not found: " + agentName, agentRegistry.getAvailableAgents()));
        }

        Agent agent = agentRegistry.getAgent(agentName);
        return HttpResponse.ok(agent.getAgentInfo());
    }

    /**
     * Execute an agent task.
     *
     * @param agentName Agent name to execute
     * @param task Task description
     * @return Execution result
     */
    @Post("/{agentName}/execute")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> executeAgent(
            @PathVariable final String agentName, @Body final AgentTask task) {
        log.info("Executing agent '{}' with task: {}", agentName, task.description());

        if (!agentRegistry.hasAgent(agentName)) {
            return HttpResponse.notFound(
                    new ErrorResponse(
                            "Agent not found: " + agentName, agentRegistry.getAvailableAgents()));
        }

        try {
            Agent agent = agentRegistry.getAgent(agentName);
            // TODO: Implement agent execution based on ade-agent SDK interface
            // The exact method depends on the Agent interface from ade-agent-sdk
            String result = "Agent execution - requires ade-agent SDK implementation";
            return HttpResponse.ok(new AgentResponse(agentName, task.description(), result));
        } catch (Exception e) {
            log.error("Failed to execute agent: {}", agentName, e);
            return HttpResponse.serverError(
                    new ErrorResponse("Execution failed: " + e.getMessage(), null));
        }
    }

    /** Agent task request. */
    public record AgentTask(String description) {}

    /** Agent execution response. */
    public record AgentResponse(String agentName, String task, String result) {}

    /** Error response. */
    public record ErrorResponse(String error, List<String> availableAgents) {}
}
