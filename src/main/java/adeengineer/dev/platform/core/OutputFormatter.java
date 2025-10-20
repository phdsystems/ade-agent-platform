package adeengineer.dev.platform.core;

import org.springframework.stereotype.Component;

import adeengineer.dev.llm.model.LLMResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Formats LLM output according to role-specific preferences.
 *
 * <p>Different roles prefer different output styles:
 *
 * <ul>
 *   <li>Technical: Detailed analysis with code blocks
 *   <li>Business: Metrics, trends, actionable insights
 *   <li>Executive: One-page summaries, business value
 * </ul>
 */
@Slf4j
@Component
public class OutputFormatter {

    /**
     * Format output based on the specified format type.
     *
     * @param response The LLM response
     * @param formatType The output format ("technical", "business", "executive")
     * @return Formatted output
     */
    public String format(final LLMResponse response, final String formatType) {
        if (response == null || response.content() == null) {
            return "";
        }

        String content = response.content();

        return switch (formatType.toLowerCase()) {
            case "technical" -> formatTechnical(content, response);
            case "business" -> formatBusiness(content, response);
            case "executive" -> formatExecutive(content, response);
            default -> content; // Return raw content if format unknown
        };
    }

    /**
     * Format for technical audiences (developers, DevOps, QA, etc.).
     *
     * <p>Includes detailed analysis, code blocks, technical terminology.
     *
     * @param content Response content to format
     * @param response Full LLM response for metadata
     * @return Formatted technical output
     */
    private String formatTechnical(final String content, final LLMResponse response) {
        StringBuilder formatted = new StringBuilder();
        formatted.append(content);

        // Add metadata footer for technical users
        formatted.append("\n\n---\n");
        formatted.append("**Technical Details**\n");
        formatted.append("- Provider: ").append(response.provider()).append("\n");
        formatted.append("- Model: ").append(response.model()).append("\n");
        formatted.append("- Tokens: ").append(response.usage().totalTokens()).append("\n");

        return formatted.toString();
    }

    /**
     * Format for business audiences (managers, product owners, finance).
     *
     * <p>Focus on metrics, trends, and actionable insights.
     *
     * @param content Response content to format
     * @param response Full LLM response for cost data
     * @return Formatted business output
     */
    private String formatBusiness(final String content, final LLMResponse response) {
        StringBuilder formatted = new StringBuilder();

        // Business users want executive summary first
        formatted.append("## Business Summary\n\n");
        formatted.append(content);

        // Add cost information for business users
        formatted.append("\n\n---\n");
        formatted.append("**Resource Usage**\n");
        formatted
                .append("- Estimated Cost: $")
                .append(String.format("%.4f", response.usage().estimatedCost()))
                .append("\n");

        return formatted.toString();
    }

    /**
     * Format for executive audiences (directors, CTO, sponsors).
     *
     * <p>One-page summaries, business value, strategic recommendations.
     *
     * @param content Response content to format
     * @param response Full LLM response (unused in executive format)
     * @return Formatted executive output
     */
    private String formatExecutive(final String content, final LLMResponse response) {
        StringBuilder formatted = new StringBuilder();

        // Executives want the bottom line first
        formatted.append("## Executive Summary\n\n");
        formatted.append(content);

        // Keep it concise - no technical details
        return formatted.toString();
    }
}
