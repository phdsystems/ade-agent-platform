package dev.adeengineer.platform.test.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import adeengineer.dev.agent.AgentConfig;

/**
 * Fluent builder for creating AgentConfig instances in tests.
 *
 * <p>Provides a convenient way to create AgentConfig objects with sensible defaults and fluent
 * API for customization.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * AgentConfig config = AgentConfigBuilder.builder()
 *     .name("Developer")
 *     .description("Software development agent")
 *     .capabilities("coding", "testing", "debugging")
 *     .temperature(0.7)
 *     .maxTokens(1000)
 *     .build();
 * }</pre>
 */
public class AgentConfigBuilder {

    private String name = "TestAgent";
    private String description = "Test agent description";
    private List<String> capabilities = new ArrayList<>(Arrays.asList("test-capability"));
    private double temperature = 0.7;
    private int maxTokens = 500;
    private String promptTemplate = "You are a {role}. Task: {task}";
    private String outputFormat = "technical";

    private AgentConfigBuilder() {}

    /**
     * Creates a new builder instance.
     *
     * @return new builder
     */
    public static AgentConfigBuilder builder() {
        return new AgentConfigBuilder();
    }

    /**
     * Sets the agent name.
     *
     * @param name agent name
     * @return this builder
     */
    public AgentConfigBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the agent description.
     *
     * @param description agent description
     * @return this builder
     */
    public AgentConfigBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the agent capabilities.
     *
     * @param capabilities agent capabilities (varargs)
     * @return this builder
     */
    public AgentConfigBuilder capabilities(String... capabilities) {
        this.capabilities = new ArrayList<>(Arrays.asList(capabilities));
        return this;
    }

    /**
     * Sets the agent capabilities.
     *
     * @param capabilities agent capabilities list
     * @return this builder
     */
    public AgentConfigBuilder capabilities(List<String> capabilities) {
        this.capabilities = new ArrayList<>(capabilities);
        return this;
    }

    /**
     * Adds a capability to the agent.
     *
     * @param capability capability to add
     * @return this builder
     */
    public AgentConfigBuilder addCapability(String capability) {
        this.capabilities.add(capability);
        return this;
    }

    /**
     * Sets the LLM temperature.
     *
     * @param temperature temperature (0.0 to 1.0)
     * @return this builder
     */
    public AgentConfigBuilder temperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    /**
     * Sets the maximum tokens for LLM responses.
     *
     * @param maxTokens max tokens
     * @return this builder
     */
    public AgentConfigBuilder maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    /**
     * Sets the prompt template.
     *
     * @param promptTemplate prompt template string
     * @return this builder
     */
    public AgentConfigBuilder promptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
        return this;
    }

    /**
     * Sets the output format.
     *
     * @param outputFormat output format (e.g., "technical", "conversational")
     * @return this builder
     */
    public AgentConfigBuilder outputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    /**
     * Builds the AgentConfig instance.
     *
     * @return configured AgentConfig
     */
    public AgentConfig build() {
        return new AgentConfig(
                name, description, capabilities, temperature, maxTokens, promptTemplate, outputFormat);
    }
}
