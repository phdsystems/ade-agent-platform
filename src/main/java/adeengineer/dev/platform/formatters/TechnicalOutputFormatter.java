package adeengineer.dev.platform.formatters;

import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.platform.core.OutputFormatStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * Technical output formatter for developer and technical audiences.
 *
 * <p>Includes detailed analysis, code blocks, technical terminology, and metadata (provider, model,
 * token usage).
 *
 * <p>Suitable for: Software Developer, QA Engineer, Security Engineer, DevOps Engineer, SRE, Data
 * Engineer, Technical Writer roles.
 */
@Slf4j
public final class TechnicalOutputFormatter implements OutputFormatStrategy {

    @Override
    public String format(final LLMResponse response) {
        if (response == null || response.content() == null) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        formatted.append(response.content());

        // Add metadata footer for technical users
        formatted.append("\n\n---\n");
        formatted.append("**Technical Details**\n");
        formatted.append("- Provider: ").append(response.provider()).append("\n");
        formatted.append("- Model: ").append(response.model()).append("\n");
        formatted.append("- Tokens: ").append(response.usage().totalTokens()).append("\n");

        return formatted.toString();
    }

    @Override
    public String getFormatName() {
        return "technical";
    }

    @Override
    public String getDescription() {
        return "Technical format with detailed analysis, code blocks, "
                + "and metadata for developer audiences";
    }
}
