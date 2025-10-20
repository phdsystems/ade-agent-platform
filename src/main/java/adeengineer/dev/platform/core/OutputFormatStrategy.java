package adeengineer.dev.platform.core;

import adeengineer.dev.llm.model.LLMResponse;

/**
 * Strategy interface for formatting LLM responses.
 *
 * <p>Implementations define how to format LLM output for different audiences and use cases
 * (technical, business, executive, clinical, legal, etc.).
 *
 * <p>This interface enables domain-specific output formatting without modifying core agent code.
 *
 * @see OutputFormatterRegistry
 */
public interface OutputFormatStrategy {

    /**
     * Format an LLM response according to the strategy.
     *
     * @param response The LLM response to format
     * @return Formatted output string
     */
    String format(LLMResponse response);

    /**
     * Get the name of this format strategy.
     *
     * <p>Used for registration and lookup in the registry.
     *
     * @return Format name (e.g., "technical", "clinical", "legal-memo")
     */
    String getFormatName();

    /**
     * Get a description of this format strategy.
     *
     * @return Human-readable description of the format
     */
    default String getDescription() {
        return "Output format: " + getFormatName();
    }
}
