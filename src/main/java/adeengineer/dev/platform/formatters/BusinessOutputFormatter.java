package adeengineer.dev.platform.formatters;

import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.platform.core.OutputFormatStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * Business output formatter for management and business audiences.
 *
 * <p>Focus on metrics, trends, actionable insights, and cost information.
 *
 * <p>Suitable for: Engineering Manager, Product Owner, Compliance Officer, Customer Support, UI/UX
 * Designer roles.
 */
@Slf4j
public final class BusinessOutputFormatter implements OutputFormatStrategy {

    @Override
    public String format(final LLMResponse response) {
        if (response == null || response.content() == null) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();

        // Business users want executive summary first
        formatted.append("## Business Summary\n\n");
        formatted.append(response.content());

        // Add cost information for business users
        formatted.append("\n\n---\n");
        formatted.append("**Resource Usage**\n");
        formatted
                .append("- Estimated Cost: $")
                .append(String.format("%.4f", response.usage().estimatedCost()))
                .append("\n");

        return formatted.toString();
    }

    @Override
    public String getFormatName() {
        return "business";
    }

    @Override
    public String getDescription() {
        return "Business format with metrics, trends, and cost information "
                + "for management audiences";
    }
}
