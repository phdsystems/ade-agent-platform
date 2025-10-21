package dev.adeengineer.platform.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Central registry for all agents.
 *
 * <p>Manages all agents and provides fast lookup by agent name.
 */
@Slf4j
@Component
public class AgentRegistry {

    /** Concurrent map of agent name to agent instance. */
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * Register an agent.
     *
     * @param agent Agent to register
     */
    public void registerAgent(final Agent agent) {
        String agentName = agent.getName();
        agents.put(agentName, agent);
        log.info("Registered agent: {}", agentName);
    }

    /**
     * Get an agent by name.
     *
     * @param agentName Agent name to look up
     * @return Agent for the specified name
     * @throws IllegalArgumentException if agent not found
     */
    public Agent getAgent(final String agentName) {
        Agent agent = agents.get(agentName);
        if (agent == null) {
            throw new IllegalArgumentException(
                    "Unknown agent: " + agentName + ". Available agents: " + getAvailableAgents());
        }
        return agent;
    }

    /**
     * Check if an agent exists.
     *
     * @param agentName Agent name to check
     * @return true if agent exists, false otherwise
     */
    public boolean hasAgent(final String agentName) {
        return agents.containsKey(agentName);
    }

    /**
     * Get all registered agent names.
     *
     * @return Sorted list of registered agent names
     */
    public List<String> getAvailableAgents() {
        return agents.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * Get information about all agents.
     *
     * @return List of agent information objects, sorted by name
     */
    public List<AgentInfo> getAllAgentsInfo() {
        return agents.values().stream()
                .map(Agent::getAgentInfo)
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .collect(Collectors.toList());
    }

    /**
     * Get the total number of registered agents.
     *
     * @return Number of registered agents
     */
    public int getAgentCount() {
        return agents.size();
    }

    /** Validate that all expected agents are registered. */
    public void validateRegistry() {
        int count = getAgentCount();
        log.info("Registry validation: {} agents registered", count);

        if (count == 0) {
            log.warn("No agents registered! " + "Check your configuration.");
        }

        // Log all registered agents
        getAvailableAgents().forEach(agent -> log.debug("  - {}", agent));
    }
}
