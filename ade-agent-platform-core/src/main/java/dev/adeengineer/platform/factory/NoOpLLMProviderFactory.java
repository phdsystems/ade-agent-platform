package dev.adeengineer.platform.factory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op LLM provider factory that creates a placeholder provider.
 *
 * <p>This factory is used when no actual LLM provider implementation is available. It's
 * automatically disabled when an actual LLMProviderFactory bean is present.
 *
 * <p><b>Usage:</b> This allows ade-agent-platform to function as a library without requiring
 * inferencestr8a or any other LLM provider implementation as a dependency. Applications that need
 * LLM functionality should provide their own LLMProviderFactory.
 *
 * <p><b>Note:</b> This class is registered as a Spring bean in ProvidersAutoConfiguration.
 *
 * @since 0.2.0
 */
@Slf4j
public class NoOpLLMProviderFactory implements LLMProviderFactory {

    @Override
    public LLMProvider create(Map<String, Object> config) {
        log.warn(
                "Using NoOpLLMProvider - no actual LLM provider implementation available. "
                        + "Add inferencestr8a or another LLM provider implementation to enable LLM functionality.");
        return new NoOpLLMProvider();
    }

    @Override
    public LLMProvider createDefault() {
        return create(Collections.emptyMap());
    }

    @Override
    public List<String> getSupportedTypes() {
        return Collections.emptyList();
    }

    /** Placeholder LLM provider that returns empty responses. */
    private static class NoOpLLMProvider implements LLMProvider {

        @Override
        public LLMResponse generate(String prompt, double temperature, int maxTokens) {
            return new LLMResponse(
                    "No LLM provider configured", new UsageInfo(0, 0, 0, 0.0), "noop", "noop");
        }

        @Override
        public String getProviderName() {
            return "noop";
        }

        @Override
        public String getModel() {
            return "noop";
        }

        @Override
        public boolean isHealthy() {
            return false;
        }
    }
}
