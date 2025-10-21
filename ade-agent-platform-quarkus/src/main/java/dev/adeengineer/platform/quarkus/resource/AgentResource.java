package dev.adeengineer.platform.quarkus.resource;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;

import dev.adeengineer.platform.core.AgentRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * JAX-RS resource for agent execution endpoints.
 *
 * <p>Provides REST API for discovering and executing agents.
 *
 * @since 0.2.0
 */
@Slf4j
@Path("/api/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentResource {

    private final AgentRegistry agentRegistry;

    @Inject
    public AgentResource(final AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    /**
     * Get all registered agents.
     *
     * @return List of agent information
     */
    @GET
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
    @GET
    @Path("/{agentName}")
    public Response getAgent(@PathParam("agentName") final String agentName) {
        log.info("Getting agent: {}", agentName);

        if (!agentRegistry.hasAgent(agentName)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(
                            new ErrorResponse(
                                    "Agent not found: " + agentName,
                                    agentRegistry.getAvailableAgents()))
                    .build();
        }

        Agent agent = agentRegistry.getAgent(agentName);
        return Response.ok(agent.getAgentInfo()).build();
    }

    /**
     * Execute an agent task.
     *
     * @param agentName Agent name to execute
     * @param task Task description
     * @return Execution result
     */
    @POST
    @Path("/{agentName}/execute")
    public Response executeAgent(
            @PathParam("agentName") final String agentName, final AgentTask task) {
        log.info("Executing agent '{}' with task: {}", agentName, task.getDescription());

        if (!agentRegistry.hasAgent(agentName)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(
                            new ErrorResponse(
                                    "Agent not found: " + agentName,
                                    agentRegistry.getAvailableAgents()))
                    .build();
        }

        try {
            Agent agent = agentRegistry.getAgent(agentName);
            // TODO: Implement agent execution based on ade-agent SDK interface
            // The exact method depends on the Agent interface from ade-agent-sdk
            String result = "Agent execution - requires ade-agent SDK implementation";
            return Response.ok(new AgentResponse(agentName, task.getDescription(), result))
                    .build();
        } catch (Exception e) {
            log.error("Failed to execute agent: {}", agentName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Execution failed: " + e.getMessage(), null))
                    .build();
        }
    }

    /** Agent task request. */
    public record AgentTask(String description) {
        public String getDescription() {
            return description;
        }
    }

    /** Agent execution response. */
    public record AgentResponse(String agentName, String task, String result) {}

    /** Error response. */
    public record ErrorResponse(String error, List<String> availableAgents) {}
}
