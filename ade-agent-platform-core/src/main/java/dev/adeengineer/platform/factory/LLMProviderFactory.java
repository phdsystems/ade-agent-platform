package dev.adeengineer.platform.factory;

import java.util.List;
import java.util.Map;

import dev.adeengineer.llm.LLMProvider;

/**
 * Factory interface for creating LLM provider instances.
 *
 * <p>This interface defines the contract for creating LLM providers dynamically. Implementations
 * are responsible for provider-specific instantiation and configuration.
 *
 * <p><b>Note:</b> This interface should eventually move to the ade-agent-sdk as part of the core
 * provider abstraction layer.
 *
 * @since 0.2.0
 */
public interface LLMProviderFactory {

    /**
     * Create an LLM provider with the given configuration.
     *
     * @param config Provider configuration (model name, API key, endpoint, etc.)
     * @return Configured LLMProvider instance
     * @throws IllegalArgumentException if configuration is invalid
     */
    LLMProvider create(Map<String, Object> config);

    /**
     * Create an LLM provider with default configuration.
     *
     * @return LLMProvider instance with default settings
     */
    LLMProvider createDefault();

    /**
     * Get supported provider types by this factory.
     *
     * <p>Examples: "openai", "anthropic", "vllm", "ollama"
     *
     * @return List of provider type identifiers
     */
    List<String> getSupportedTypes();

    /**
     * Check if this factory supports the given provider type.
     *
     * @param type Provider type identifier
     * @return true if supported, false otherwise
     */
    default boolean supports(String type) {
        return getSupportedTypes().contains(type);
    }

    /**
     * Get an LLM provider with failover support.
     *
     * <p>This method returns a provider that automatically fails over to backup providers if the
     * primary provider fails. The exact failover strategy is implementation-specific.
     *
     * @return LLMProvider with failover capabilities
     */
    default LLMProvider getProviderWithFailover() {
        return createDefault();
    }
}
