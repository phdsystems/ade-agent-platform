package dev.adeengineer.platform.spring.e2e;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.agent.OutputFormatterRegistry;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.ConfigurableAgent;

/**
 * Test configuration for E2E tests that provides mock agent setup. Uses ConfigurableAgent for
 * domain-agnostic testing.
 */
@TestConfiguration
@ComponentScan(basePackages = "com.rolemanager")
public class E2ETestConfiguration {

    /**
     * Provides a test AgentRegistry with manually created ConfigurableAgent instances. Uses
     * domain-agnostic ConfigurableAgent instead of legacy hardcoded classes.
     */
    @Bean
    @Primary
    public AgentRegistry testAgentRegistry(
            @Qualifier("anthropicProvider") LLMProvider primaryProvider,
            OutputFormatterRegistry formatterRegistry) {
        AgentRegistry registry = new AgentRegistry();

        // Create test agent configs with short role names matching test expectations
        AgentConfig developerConfig =
                new AgentConfig(
                        "Developer",
                        "Test Software Developer agent for E2E testing",
                        List.of("Code review", "Bug fixing", "Architecture design"),
                        0.7,
                        1000,
                        "You are a {role}. Task: {task}",
                        "technical");

        AgentConfig managerConfig =
                new AgentConfig(
                        "Manager",
                        "Test Engineering Manager agent for E2E testing",
                        List.of("Team management", "Metrics reporting", "Sprint planning"),
                        0.7,
                        1000,
                        "You are a {role}. Task: {task}",
                        "executive");

        AgentConfig qaConfig =
                new AgentConfig(
                        "QA",
                        "Test QA Engineer agent for E2E testing",
                        List.of("Test planning", "Test case generation", "Quality assurance"),
                        0.7,
                        1000,
                        "You are a {role}. Task: {task}",
                        "technical");

        AgentConfig securityConfig =
                new AgentConfig(
                        "Security",
                        "Test Security Engineer agent for E2E testing",
                        List.of("Security analysis", "Vulnerability assessment", "Threat modeling"),
                        0.7,
                        1000,
                        "You are a {role}. Task: {task}",
                        "technical");

        // Create and register ConfigurableAgent instances
        registry.registerAgent(
                new ConfigurableAgent(developerConfig, primaryProvider, formatterRegistry));
        registry.registerAgent(
                new ConfigurableAgent(managerConfig, primaryProvider, formatterRegistry));
        registry.registerAgent(new ConfigurableAgent(qaConfig, primaryProvider, formatterRegistry));
        registry.registerAgent(
                new ConfigurableAgent(securityConfig, primaryProvider, formatterRegistry));

        return registry;
    }
}
