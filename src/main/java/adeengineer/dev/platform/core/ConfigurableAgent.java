package adeengineer.dev.platform.core;

import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.llm.LLMProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * Generic configurable agent implementation.
 *
 * <p>This agent can represent any role in any domain based solely on its YAML configuration. No
 * code changes needed for new agents.
 *
 * <p>Replaces the need for separate agent classes (DeveloperAgent, QAAgent, SecurityAgent, etc.).
 * All role-specific behavior is defined in YAML configuration files.
 *
 * <h3>Configuration-Driven Behavior</h3>
 *
 * <p>Agent behavior is fully controlled via YAML:
 *
 * <ul>
 *   <li>name: Agent role name
 *   <li>description: What the agent does
 *   <li>capabilities: What the agent can help with
 *   <li>promptTemplate: LLM prompt template
 *   <li>temperature: LLM temperature (creativity)
 *   <li>maxTokens: Maximum response length
 *   <li>outputFormat: How to format output
 * </ul>
 *
 * <h3>Example YAML Configuration</h3>
 *
 * <pre>
 * name: "Diagnostic Specialist"
 * description: "AI assistant for medical diagnostics"
 * capabilities:
 *   - "Symptom analysis"
 *   - "Differential diagnosis"
 * temperature: 0.3
 * maxTokens: 2048
 * outputFormat: "clinical"
 * promptTemplate: |
 *   You are a Diagnostic Specialist AI.
 *   Patient Presentation: {task}
 *   Medical History: {context}
 *   Provide structured clinical assessment...
 * </pre>
 *
 * <h3>Usage</h3>
 *
 * <pre>
 * AgentConfig config = loadFromYaml("domains/healthcare/agents/diagnostics.yaml");
 * Agent agent = new ConfigurableAgent(config, llmProvider, formatterRegistry);
 * TaskResult result = agent.executeTask(request);
 * </pre>
 *
 * @see BaseAgent
 * @see AgentConfig
 * @since 0.2.0
 */
@Slf4j
public class ConfigurableAgent extends BaseAgent {

    /**
     * Constructs a new ConfigurableAgent.
     *
     * <p>All behavior is defined by the AgentConfig parameter.
     *
     * @param config Agent configuration from YAML
     * @param llmProvider LLM provider for task execution
     * @param formatterRegistry Output formatter registry
     */
    public ConfigurableAgent(
            final AgentConfig config,
            final LLMProvider llmProvider,
            final OutputFormatterRegistry formatterRegistry) {
        super(config, llmProvider, formatterRegistry);
        log.info("Initialized ConfigurableAgent: {} (domain-agnostic)", config.name());
    }

    // ConfigurableAgent uses the complete implementation from BaseAgent
    // All role-specific behavior is configured via YAML
    // No additional code needed
}
