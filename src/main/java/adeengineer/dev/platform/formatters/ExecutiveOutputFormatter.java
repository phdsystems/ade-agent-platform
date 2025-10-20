package adeengineer.dev.platform.formatters;

import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.platform.core.OutputFormatStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * Executive output formatter for leadership and executive audiences.
 *
 * <p>One-page summaries, business value, strategic recommendations. Minimal technical details,
 * focus on bottom line.
 *
 * <p>Suitable for: Executive, Engineering Director, CTO, Project Sponsor roles.
 */
@Slf4j
public final class ExecutiveOutputFormatter implements OutputFormatStrategy {

    @Override
    public String format(final LLMResponse response) {
        if (response == null || response.content() == null) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();

        // Executives want the bottom line first
        formatted.append("## Executive Summary\n\n");
        formatted.append(response.content());

        // Keep it concise - no technical details
        return formatted.toString();
    }

    @Override
    public String getFormatName() {
        return "executive";
    }

    @Override
    public String getDescription() {
        return "Executive format with one-page summaries and strategic "
                + "recommendations for leadership";
    }
}
