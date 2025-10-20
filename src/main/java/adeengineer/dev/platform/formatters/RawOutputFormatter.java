package adeengineer.dev.platform.formatters;

import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.platform.core.OutputFormatStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * Raw output formatter that returns unmodified LLM response.
 *
 * <p>No formatting, no headers, no footers - just the raw content from the LLM provider.
 *
 * <p>Useful for: Custom processing, API integrations, testing, or when you want maximum control
 * over formatting.
 */
@Slf4j
public final class RawOutputFormatter implements OutputFormatStrategy {

    @Override
    public String format(final LLMResponse response) {
        if (response == null || response.content() == null) {
            return "";
        }

        // Return content as-is, no modifications
        return response.content();
    }

    @Override
    public String getFormatName() {
        return "raw";
    }

    @Override
    public String getDescription() {
        return "Raw format with unmodified LLM response content";
    }
}
