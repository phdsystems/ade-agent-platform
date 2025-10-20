package adeengineer.dev.platform.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;
import adeengineer.dev.llm.LLMProvider;
import adeengineer.dev.llm.model.LLMResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Base implementation of Agent using agent-sdk standard interface.
 *
 * <p>Specific role agents extend this class to customize behavior.
 *
 * <p>This class implements the agent-sdk Agent interface and integrates with LLM providers for task
 * execution.
 */
@Slf4j
public abstract class BaseAgent implements Agent {

    /** Agent configuration containing role, prompts, and parameters. */
    private final AgentConfig config;

    /** LLM provider for generating responses. */
    private final LLMProvider llmProvider;

    /** Formatter for formatting agent outputs. */
    private final OutputFormatter outputFormatter;

    /** Formatter registry for formatting agent outputs (new system). */
    private final OutputFormatterRegistry formatterRegistry;

    /**
     * Constructs a new BaseAgent (legacy constructor).
     *
     * @param agentConfig Agent configuration (agent-sdk)
     * @param provider LLM provider for task execution
     * @param formatter Formatter for agent outputs
     * @deprecated Use constructor with OutputFormatterRegistry instead
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    protected BaseAgent(
            final AgentConfig agentConfig,
            final LLMProvider provider,
            final OutputFormatter formatter) {
        this.config = agentConfig;
        this.llmProvider = provider;
        this.outputFormatter = formatter;
        this.formatterRegistry = null;
    }

    /**
     * Constructs a new BaseAgent with formatter registry (new system).
     *
     * @param agentConfig Agent configuration (agent-sdk)
     * @param provider LLM provider for task execution
     * @param registry Output formatter registry
     */
    protected BaseAgent(
            final AgentConfig agentConfig,
            final LLMProvider provider,
            final OutputFormatterRegistry registry) {
        this.config = agentConfig;
        this.llmProvider = provider;
        this.outputFormatter = null;
        this.formatterRegistry = registry;
    }

    // ========== Agent Interface Methods ==========

    /**
     * Get the unique identifier/name of this agent.
     *
     * @return Agent name
     */
    @Override
    public String getName() {
        return config.name();
    }

    /**
     * Get a description of this agent's purpose.
     *
     * @return Agent description
     */
    @Override
    public String getDescription() {
        return config.description();
    }

    /**
     * Get the list of capabilities this agent has.
     *
     * @return List of capability strings
     */
    @Override
    public List<String> getCapabilities() {
        return config.capabilities();
    }

    /**
     * Execute a task using the LLM provider.
     *
     * @param request Task request containing task and context
     * @return Task result with output and metadata
     */
    @Override
    public TaskResult executeTask(final TaskRequest request) {
        log.info("Executing task for agent: {}", config.name());
        long startTime = System.currentTimeMillis();

        try {
            // Build the prompt
            String prompt = buildPrompt(request.task(), request.context());

            // Call LLM provider
            LLMResponse llmResponse =
                    llmProvider.generate(prompt, config.temperature(), config.maxTokens());

            // Calculate duration
            long durationMs = System.currentTimeMillis() - startTime;

            // Format output
            String formattedOutput = formatOutput(llmResponse);

            // Create metadata
            Map<String, Object> metadata = createMetadata(llmResponse);

            log.info("Task completed successfully for agent: {}", config.name());

            return TaskResult.success(
                    getName(), request.task(), formattedOutput, metadata, durationMs);

        } catch (Exception e) {
            log.error("Error executing task for agent {}: {}", config.name(), e.getMessage(), e);

            return TaskResult.failure(getName(), request.task(), e.getMessage());
        }
    }

    /**
     * Get comprehensive information about this agent.
     *
     * @return Agent information object
     */
    @Override
    public AgentInfo getAgentInfo() {
        return new AgentInfo(getName(), getDescription(), getCapabilities(), config.outputFormat());
    }

    // ========== Helper Methods ==========

    /**
     * Build a prompt from the template and task description.
     *
     * @param task Task description
     * @param context Optional context map
     * @return Complete prompt for LLM
     */
    protected String buildPrompt(final String task, final Map<String, Object> context) {
        String template = config.promptTemplate();

        // Replace placeholders in template
        String prompt = template.replace("{role}", config.name()).replace("{task}", task);

        // Add context if provided
        if (context != null && !context.isEmpty()) {
            StringBuilder contextStr = new StringBuilder();
            context.forEach(
                    (key, value) -> contextStr.append(key).append(": ").append(value).append("\n"));
            prompt = prompt.replace("{context}", contextStr.toString());
        } else {
            prompt = prompt.replace("{context}", "No additional context provided.");
        }

        log.debug("Built prompt for agent {}: {} characters", config.name(), prompt.length());
        return prompt;
    }

    /**
     * Format the LLM response according to the agent's output format.
     *
     * @param response LLM response to format
     * @return Formatted output string
     */
    protected String formatOutput(final LLMResponse response) {
        // Use new registry if available, otherwise fall back to old formatter
        if (formatterRegistry != null) {
            return formatterRegistry.format(response, config.outputFormat());
        } else if (outputFormatter != null) {
            return outputFormatter.format(response, config.outputFormat());
        } else {
            log.warn("No formatter available, returning raw content");
            return response.content() != null ? response.content() : "";
        }
    }

    /**
     * Create metadata map from LLM response.
     *
     * @param response LLM response
     * @return Metadata map
     */
    protected Map<String, Object> createMetadata(final LLMResponse response) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", response.provider());
        metadata.put("model", response.model());
        metadata.put("inputTokens", response.usage().inputTokens());
        metadata.put("outputTokens", response.usage().outputTokens());
        metadata.put("totalTokens", response.usage().totalTokens());
        metadata.put("estimatedCost", response.usage().estimatedCost());
        return metadata;
    }

    /**
     * Get the agent configuration.
     *
     * @return Agent configuration
     */
    public AgentConfig getConfig() {
        return config;
    }

    /**
     * Get the temperature parameter for LLM generation.
     *
     * @return Temperature value from configuration
     */
    public double getTemperature() {
        return config.temperature();
    }

    /**
     * Get the maximum tokens for LLM generation.
     *
     * @return Maximum tokens from configuration
     */
    public int getMaxTokens() {
        return config.maxTokens();
    }
}
