package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;

import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.test.mock.MockAgent;

/**
 * Integration tests for AgentRegistry with actual Agent implementations.
 *
 * <p>Tests the complete agent registration and retrieval workflow.
 */
@DisplayName("AgentRegistry Integration Tests")
class AgentRegistryIntegrationTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistry();
    }

    @Test
    @DisplayName("Should register and retrieve multiple agents successfully")
    void testRegisterAndRetrieveMultipleAgents() {
        // Given: Multiple agents
        Agent developer = new MockAgent("developer");
        Agent tester = new MockAgent("tester");
        Agent reviewer = new MockAgent("reviewer");

        // When: Registering agents
        registry.registerAgent(developer);
        registry.registerAgent(tester);
        registry.registerAgent(reviewer);

        // Then: All should be registered and retrievable
        assertThat(registry.getAgentCount()).isEqualTo(3);
        assertThat(registry.hasAgent("developer")).isTrue();
        assertThat(registry.hasAgent("tester")).isTrue();
        assertThat(registry.hasAgent("reviewer")).isTrue();

        Agent retrievedDev = registry.getAgent("developer");
        assertThat(retrievedDev.getName()).isEqualTo("developer");
    }

    @Test
    @DisplayName("Should throw exception when retrieving non-existent agent")
    void testGetNonExistentAgentThrowsException() {
        // When/Then: Retrieving non-existent agent should throw
        assertThatThrownBy(() -> registry.getAgent("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown agent");
    }

    @Test
    @DisplayName("Should return all available agent names sorted")
    void testGetAvailableAgentsReturnsSortedNames() {
        // Given: Multiple agents
        registry.registerAgent(new MockAgent("charlie"));
        registry.registerAgent(new MockAgent("alice"));
        registry.registerAgent(new MockAgent("bob"));

        // When: Getting available agents
        var agents = registry.getAvailableAgents();

        // Then: Should return sorted names
        assertThat(agents).hasSize(3).containsExactly("alice", "bob", "charlie");
    }

    @Test
    @DisplayName("Should return agent info for all registered agents")
    void testGetAllAgentsInfoReturnsMetadata() {
        // Given: Multiple agents with metadata
        registry.registerAgent(new MockAgent("agent1"));
        registry.registerAgent(new MockAgent("agent2"));

        // When: Getting all agent info
        var agentsInfo = registry.getAllAgentsInfo();

        // Then: Should return info for all agents
        assertThat(agentsInfo).hasSize(2);
        assertThat(agentsInfo)
                .extracting(AgentInfo::name)
                .containsExactlyInAnyOrder("agent1", "agent2");
    }

    @Test
    @DisplayName("Should validate registry with agents without throwing")
    void testValidateRegistryWithAgents() {
        // Given: Registry with agents
        registry.registerAgent(new MockAgent("test"));

        // When/Then: Validation should succeed
        registry.validateRegistry();
        assertThat(registry.getAgentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should validate empty registry without throwing")
    void testValidateEmptyRegistry() {
        // When/Then: Empty registry validation should succeed
        registry.validateRegistry();
        assertThat(registry.getAgentCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should overwrite when registering agent with same name")
    void testRegisterSameAgentTwiceOverwrites() {
        // Given: Two agents with same name
        Agent agent1 = new MockAgent("duplicate");
        Agent agent2 = new MockAgent("duplicate");

        // When: Registering both
        registry.registerAgent(agent1);
        registry.registerAgent(agent2);

        // Then: Should have only one agent (last registered)
        assertThat(registry.getAgentCount()).isEqualTo(1);
        assertThat(registry.getAgent("duplicate")).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent agent registration")
    void testConcurrentAgentRegistration() {
        // Given: Multiple threads registering agents
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                registry.registerAgent(new MockAgent("agent-" + index));
                            });
        }

        // When: Starting all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for completion
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Then: All agents should be registered
        assertThat(registry.getAgentCount()).isEqualTo(threadCount);
    }
}
