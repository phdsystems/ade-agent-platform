package dev.adeengineer.platform.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.adeengineer.agent.Agent;
import dev.adeengineer.agent.AgentInfo;
import dev.adeengineer.platform.testutil.MockAgent;

@DisplayName("AgentRegistry Tests")
class AgentRegistryTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistry();
    }

    @Test
    @DisplayName("Should register a single agent")
    void shouldRegisterSingleAgent() {
        // Given
        Agent agent = new MockAgent("Developer");

        // When
        registry.registerAgent(agent);

        // Then
        assertThat(registry.hasAgent("Developer")).isTrue();
        assertThat(registry.getAgentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should register multiple agents")
    void shouldRegisterMultipleAgents() {
        // Given
        Agent dev = new MockAgent("Developer");
        Agent qa = new MockAgent("QA Engineer");
        Agent security = new MockAgent("Security");

        // When
        registry.registerAgent(dev);
        registry.registerAgent(qa);
        registry.registerAgent(security);

        // Then
        assertThat(registry.getAgentCount()).isEqualTo(3);
        assertThat(registry.hasAgent("Developer")).isTrue();
        assertThat(registry.hasAgent("QA Engineer")).isTrue();
        assertThat(registry.hasAgent("Security")).isTrue();
    }

    @Test
    @DisplayName("Should retrieve registered agent by role name")
    void shouldRetrieveAgentByRoleName() {
        // Given
        Agent agent = new MockAgent("Developer");
        registry.registerAgent(agent);

        // When
        Agent retrieved = registry.getAgent("Developer");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Developer");
    }

    @Test
    @DisplayName("Should throw exception when retrieving non-existent agent")
    void shouldThrowExceptionWhenAgentNotFound() {
        // When/Then
        assertThatThrownBy(() -> registry.getAgent("NonExistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown agent: NonExistent")
                .hasMessageContaining("Available agents:");
    }

    @Test
    @DisplayName("Should return false when checking for non-existent agent")
    void shouldReturnFalseForNonExistentAgent() {
        // When
        boolean exists = registry.hasAgent("NonExistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return empty list when no agents registered")
    void shouldReturnEmptyListWhenNoAgents() {
        // When
        List<String> roles = registry.getAvailableAgents();

        // Then
        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("Should return sorted list of available roles")
    void shouldReturnSortedRolesList() {
        // Given
        registry.registerAgent(new MockAgent("Zebra"));
        registry.registerAgent(new MockAgent("Apple"));
        registry.registerAgent(new MockAgent("Mango"));

        // When
        List<String> roles = registry.getAvailableAgents();

        // Then
        assertThat(roles).containsExactly("Apple", "Mango", "Zebra");
    }

    @Test
    @DisplayName("Should replace agent when registering with same role name")
    void shouldReplaceAgentWithSameRoleName() {
        // Given
        Agent agent1 = new MockAgent("Developer", "First description", List.of("cap1"));
        Agent agent2 = new MockAgent("Developer", "Second description", List.of("cap2"));

        // When
        registry.registerAgent(agent1);
        registry.registerAgent(agent2);

        // Then
        assertThat(registry.getAgentCount()).isEqualTo(1);
        Agent retrieved = registry.getAgent("Developer");
        assertThat(retrieved.getDescription()).isEqualTo("Second description");
    }

    @Test
    @DisplayName("Should return all roles info sorted by role name")
    void shouldReturnAllRolesInfoSorted() {
        // Given
        registry.registerAgent(new MockAgent("Zebra"));
        registry.registerAgent(new MockAgent("Apple"));
        registry.registerAgent(new MockAgent("Mango"));

        // When
        List<AgentInfo> rolesInfo = registry.getAllAgentsInfo();

        // Then
        assertThat(rolesInfo).hasSize(3);
        assertThat(rolesInfo.get(0).name()).isEqualTo("Apple");
        assertThat(rolesInfo.get(1).name()).isEqualTo("Mango");
        assertThat(rolesInfo.get(2).name()).isEqualTo("Zebra");
    }

    @Test
    @DisplayName("Should return empty list for roles info when no agents")
    void shouldReturnEmptyRolesInfoWhenNoAgents() {
        // When
        List<AgentInfo> rolesInfo = registry.getAllAgentsInfo();

        // Then
        assertThat(rolesInfo).isEmpty();
    }

    @Test
    @DisplayName("Should return correct agent count")
    void shouldReturnCorrectAgentCount() {
        // Given
        assertThat(registry.getAgentCount()).isZero();

        // When/Then
        registry.registerAgent(new MockAgent("Agent1"));
        assertThat(registry.getAgentCount()).isEqualTo(1);

        registry.registerAgent(new MockAgent("Agent2"));
        assertThat(registry.getAgentCount()).isEqualTo(2);

        registry.registerAgent(new MockAgent("Agent3"));
        assertThat(registry.getAgentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should validate registry without throwing exception")
    void shouldValidateRegistryWithoutException() {
        // Given
        registry.registerAgent(new MockAgent("Developer"));

        // When/Then - should not throw
        assertThatCode(() -> registry.validateRegistry()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate empty registry without throwing exception")
    void shouldValidateEmptyRegistryWithoutException() {
        // When/Then - should not throw, just log warning
        assertThatCode(() -> registry.validateRegistry()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle concurrent registration safely")
    void shouldHandleConcurrentRegistrationSafely() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // When - concurrent registration
        for (int i = 0; i < threadCount; i++) {
            final String roleName = "Agent" + i;
            threads[i] =
                    new Thread(
                            () -> {
                                registry.registerAgent(new MockAgent(roleName));
                            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        assertThat(registry.getAgentCount()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("Should handle roles with special characters")
    void shouldHandleRolesWithSpecialCharacters() {
        // Given
        Agent agent = new MockAgent("Developer (Senior)");

        // When
        registry.registerAgent(agent);

        // Then
        assertThat(registry.hasAgent("Developer (Senior)")).isTrue();
        Agent retrieved = registry.getAgent("Developer (Senior)");
        assertThat(retrieved.getName()).isEqualTo("Developer (Senior)");
    }

    @Test
    @DisplayName("Should handle roles with unicode characters")
    void shouldHandleRolesWithUnicodeCharacters() {
        // Given
        Agent agent = new MockAgent("开发者"); // Chinese for "Developer"

        // When
        registry.registerAgent(agent);

        // Then
        assertThat(registry.hasAgent("开发者")).isTrue();
        Agent retrieved = registry.getAgent("开发者");
        assertThat(retrieved.getName()).isEqualTo("开发者");
    }
}
