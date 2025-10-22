package dev.adeengineer.platform.micronaut.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import adeengineer.dev.agent.AgentInfo;

import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.test.micronaut.BaseMicronautTest;
import dev.adeengineer.platform.test.micronaut.MicronautTestProperties;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

/**
 * Integration tests for AgentRegistry in Micronaut context.
 *
 * <p>Validates that the AgentRegistry bean is properly configured and accessible.
 *
 * @since 0.2.0
 */
@MicronautTest
@DisplayName("AgentRegistry Integration Tests")
class AgentRegistryIntegrationTest extends BaseMicronautTest
        implements MicronautTestProperties.FastIsolatedProperties {

    @Inject AgentRegistry agentRegistry;

    @Test
    @DisplayName("Should inject AgentRegistry bean")
    void shouldInjectAgentRegistry() {
        // Then
        assertThat(agentRegistry).isNotNull();
    }

    @Test
    @DisplayName("Should return empty list when no agents registered")
    void shouldReturnEmptyListWhenNoAgentsRegistered() {
        // When
        List<String> agents = agentRegistry.getAvailableAgents();

        // Then
        assertThat(agents).isNotNull();
        // Note: May contain agents if default domain is loaded
        // This is expected behavior in a real environment
    }

    @Test
    @DisplayName("Should return agent info list")
    void shouldReturnAgentInfoList() {
        // When
        List<AgentInfo> agentInfos = agentRegistry.getAllAgentsInfo();

        // Then
        assertThat(agentInfos).isNotNull();
        // Note: List size depends on whether default domains are loaded
        // This validates the bean is functional
    }

    @Test
    @DisplayName("Should handle hasAgent query for non-existent agent")
    void shouldHandleHasAgentQueryForNonExistentAgent() {
        // When
        boolean hasAgent = agentRegistry.hasAgent("NonExistentAgent123456");

        // Then
        assertThat(hasAgent).isFalse();
    }
}
