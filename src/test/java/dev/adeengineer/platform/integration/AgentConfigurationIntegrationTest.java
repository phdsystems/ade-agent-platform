package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentConfig;

import dev.adeengineer.platform.config.AgentConfigLoader;
import dev.adeengineer.platform.core.AgentRegistry;

/**
 * Integration tests for agent configuration loading and initialization. Tests the complete startup
 * path: YAML loading → Agent creation → Registry population.
 */
@DisplayName("Agent Configuration Integration Tests")
class AgentConfigurationIntegrationTest extends BaseIntegrationTest {

    @Autowired private AgentConfigLoader configLoader;

    @Autowired private AgentRegistry registry;

    @Autowired private ApplicationContext context;

    @Test
    @DisplayName("Should load agent configurations from YAML files")
    void shouldLoadAgentConfigurationsFromYaml() {
        // When
        List<AgentConfig> configs = configLoader.loadAllConfigs();

        // Then
        assertThat(configs).isNotEmpty();
        assertThat(configs).hasSizeGreaterThanOrEqualTo(4); // Developer, QA, Security, Manager

        // Verify config structure
        for (AgentConfig config : configs) {
            assertThat(config.name()).isNotBlank();
            assertThat(config.promptTemplate()).isNotBlank();
            assertThat(config.temperature()).isBetween(0.0, 1.0);
            assertThat(config.maxTokens()).isPositive();
        }
    }

    @Test
    @DisplayName("Should register all agents from configurations on startup")
    void shouldRegisterAllAgentsFromConfigurationsOnStartup() {
        // Given - ApplicationRunner has already executed during context startup

        // Then
        assertThat(registry.getAgentCount()).isGreaterThanOrEqualTo(4);
        assertThat(registry.hasAgent("Software Developer")).isTrue();
        assertThat(registry.hasAgent("QA Engineer")).isTrue();
        assertThat(registry.hasAgent("Security Engineer")).isTrue();
        assertThat(registry.hasAgent("Engineering Manager")).isTrue();
    }

    @Test
    @DisplayName("Should create agents with correct configurations")
    void shouldCreateAgentsWithCorrectConfigurations() {
        // When
        Agent developer = registry.getAgent("Software Developer");
        Agent qa = registry.getAgent("QA Engineer");

        // Then
        assertThat(developer).isNotNull();
        assertThat(developer.getName()).isEqualTo("Software Developer");

        assertThat(qa).isNotNull();
        assertThat(qa.getName()).isEqualTo("QA Engineer");
    }

    @Test
    @DisplayName("Should load configuration by specific role name")
    void shouldLoadConfigurationBySpecificRoleName() throws Exception {
        // When
        AgentConfig developerConfig = configLoader.loadConfigByName("Software Developer");

        // Then
        assertThat(developerConfig).isNotNull();
        assertThat(developerConfig.name()).isEqualTo("Software Developer");
        assertThat(developerConfig.description()).isNotBlank();
        assertThat(developerConfig.temperature()).isBetween(0.0, 1.0);
        assertThat(developerConfig.maxTokens()).isPositive();
    }

    @Test
    @DisplayName("Should have all agents available via Spring context")
    void shouldHaveAllAgentsAvailableViaSpringContext() {
        // Then
        assertThat(context.getBean(AgentRegistry.class)).isNotNull();
        assertThat(context.getBean(AgentConfigLoader.class)).isNotNull();

        // Verify registry is populated
        AgentRegistry registryBean = context.getBean(AgentRegistry.class);
        assertThat(registryBean.getAvailableAgents()).isNotEmpty();
        assertThat(registryBean.getAvailableAgents())
                .contains(
                        "Software Developer",
                        "QA Engineer",
                        "Security Engineer",
                        "Engineering Manager");
    }

    @Test
    @DisplayName("Should validate agent registry on startup")
    void shouldValidateAgentRegistryOnStartup() {
        // Given - ApplicationRunner has executed

        // When
        List<String> availableRoles = registry.getAvailableAgents();

        // Then
        assertThat(availableRoles).isNotEmpty();
        assertThat(registry.getAgentCount()).isEqualTo(availableRoles.size());

        // Verify each role can be retrieved
        for (String role : availableRoles) {
            assertThat(registry.hasAgent(role)).isTrue();
            assertThat(registry.getAgent(role)).isNotNull();
        }
    }

    @Test
    @DisplayName("Should load configurations with prompt templates")
    void shouldLoadConfigurationsWithPromptTemplates() {
        // When
        List<AgentConfig> configs = configLoader.loadAllConfigs();

        // Then
        for (AgentConfig config : configs) {
            assertThat(config.promptTemplate())
                    .as("Config for role %s should have non-empty prompt template", config.name())
                    .isNotBlank();

            // Prompt templates should be descriptive (not just a few words)
            assertThat(config.promptTemplate().length())
                    .as("Prompt template for %s should be descriptive", config.name())
                    .isGreaterThan(20);
        }
    }

    @Test
    @DisplayName("Should load configurations with valid parameter ranges")
    void shouldLoadConfigurationsWithValidParameterRanges() {
        // When
        List<AgentConfig> configs = configLoader.loadAllConfigs();

        // Then
        for (AgentConfig config : configs) {
            assertThat(config.temperature())
                    .as("Temperature for %s should be between 0.0 and 1.0", config.name())
                    .isBetween(0.0, 1.0);

            assertThat(config.maxTokens())
                    .as("Max tokens for %s should be positive", config.name())
                    .isPositive();

            assertThat(config.maxTokens())
                    .as("Max tokens for %s should be reasonable", config.name())
                    .isLessThanOrEqualTo(100000);
        }
    }
}
