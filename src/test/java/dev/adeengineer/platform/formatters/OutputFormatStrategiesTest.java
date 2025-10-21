package dev.adeengineer.platform.formatters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.adeengineer.agent.OutputFormatStrategy;
import dev.adeengineer.agent.formatters.BusinessOutputFormatter;
import dev.adeengineer.agent.formatters.ExecutiveOutputFormatter;
import dev.adeengineer.agent.formatters.RawOutputFormatter;
import dev.adeengineer.agent.formatters.TechnicalOutputFormatter;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

/** Unit tests for output format strategy implementations. */
class OutputFormatStrategiesTest {

    private LLMResponse response;

    @BeforeEach
    void setUp() {
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.0015);
        response =
                new LLMResponse(
                        "This is the LLM response content", usage, "anthropic", "claude-3-sonnet");
    }

    @Test
    void testTechnicalOutputFormatter() {
        OutputFormatStrategy formatter = new TechnicalOutputFormatter();

        assertThat(formatter.getFormatName()).isEqualTo("technical");
        assertThat(formatter.getDescription()).contains("Technical format");

        String result = formatter.format(response);

        // Should contain original content
        assertThat(result).contains("This is the LLM response content");

        // Should contain technical details
        assertThat(result).contains("Technical Details");
        assertThat(result).contains("Provider: claude-3-sonnet");
        assertThat(result).contains("Model: anthropic");
        assertThat(result).contains("Tokens: 300");
    }

    @Test
    void testTechnicalOutputFormatterNullResponse() {
        OutputFormatStrategy formatter = new TechnicalOutputFormatter();

        String result = formatter.format(null);

        assertThat(result).isEmpty();
    }

    @Test
    void testTechnicalOutputFormatterNullContent() {
        UsageInfo usage = new UsageInfo(0, 0, 0, 0.0);
        LLMResponse nullContentResponse = new LLMResponse(null, usage, "provider", "model");

        OutputFormatStrategy formatter = new TechnicalOutputFormatter();
        String result = formatter.format(nullContentResponse);

        assertThat(result).isEmpty();
    }

    @Test
    void testBusinessOutputFormatter() {
        OutputFormatStrategy formatter = new BusinessOutputFormatter();

        assertThat(formatter.getFormatName()).isEqualTo("business");
        assertThat(formatter.getDescription()).contains("Business format");

        String result = formatter.format(response);

        // Should contain business summary
        assertThat(result).contains("Business Summary");
        assertThat(result).contains("This is the LLM response content");

        // Should contain cost information
        assertThat(result).contains("Resource Usage");
        assertThat(result).contains("Estimated Cost: $0.0015");

        // Should NOT contain technical provider details
        assertThat(result).doesNotContain("Provider:");
        assertThat(result).doesNotContain("Model:");
    }

    @Test
    void testBusinessOutputFormatterNullResponse() {
        OutputFormatStrategy formatter = new BusinessOutputFormatter();

        String result = formatter.format(null);

        assertThat(result).isEmpty();
    }

    @Test
    void testExecutiveOutputFormatter() {
        OutputFormatStrategy formatter = new ExecutiveOutputFormatter();

        assertThat(formatter.getFormatName()).isEqualTo("executive");
        assertThat(formatter.getDescription()).contains("Executive format");

        String result = formatter.format(response);

        // Should contain executive summary
        assertThat(result).contains("Executive Summary");
        assertThat(result).contains("This is the LLM response content");

        // Should NOT contain technical details or cost
        assertThat(result).doesNotContain("Provider:");
        assertThat(result).doesNotContain("Model:");
        assertThat(result).doesNotContain("Tokens:");
        assertThat(result).doesNotContain("Cost:");
    }

    @Test
    void testExecutiveOutputFormatterNullResponse() {
        OutputFormatStrategy formatter = new ExecutiveOutputFormatter();

        String result = formatter.format(null);

        assertThat(result).isEmpty();
    }

    @Test
    void testRawOutputFormatter() {
        OutputFormatStrategy formatter = new RawOutputFormatter();

        assertThat(formatter.getFormatName()).isEqualTo("raw");
        assertThat(formatter.getDescription()).contains("Raw format");

        String result = formatter.format(response);

        // Should contain ONLY the raw content, no headers or metadata
        assertThat(result).isEqualTo("This is the LLM response content");
        assertThat(result).doesNotContain("Summary");
        assertThat(result).doesNotContain("Provider:");
        assertThat(result).doesNotContain("Cost:");
    }

    @Test
    void testRawOutputFormatterNullResponse() {
        OutputFormatStrategy formatter = new RawOutputFormatter();

        String result = formatter.format(null);

        assertThat(result).isEmpty();
    }

    @Test
    void testRawOutputFormatterNullContent() {
        UsageInfo usage = new UsageInfo(0, 0, 0, 0.0);
        LLMResponse nullContentResponse = new LLMResponse(null, usage, "provider", "model");

        OutputFormatStrategy formatter = new RawOutputFormatter();
        String result = formatter.format(nullContentResponse);

        assertThat(result).isEmpty();
    }

    @Test
    void testFormattersReturnDifferentResults() {
        String technical = new TechnicalOutputFormatter().format(response);
        String business = new BusinessOutputFormatter().format(response);
        String executive = new ExecutiveOutputFormatter().format(response);
        String raw = new RawOutputFormatter().format(response);

        // All should contain the original content
        assertThat(technical).contains("This is the LLM response content");
        assertThat(business).contains("This is the LLM response content");
        assertThat(executive).contains("This is the LLM response content");
        assertThat(raw).contains("This is the LLM response content");

        // But they should all be different
        assertThat(technical).isNotEqualTo(business);
        assertThat(technical).isNotEqualTo(executive);
        assertThat(technical).isNotEqualTo(raw);
        assertThat(business).isNotEqualTo(executive);
        assertThat(business).isNotEqualTo(raw);
        assertThat(executive).isNotEqualTo(raw);
    }

    @Test
    void testFormattersPreserveSummaryHeaders() {
        // Technical has "Technical Details"
        String technical = new TechnicalOutputFormatter().format(response);
        assertThat(technical).contains("**Technical Details**");

        // Business has "Business Summary"
        String business = new BusinessOutputFormatter().format(response);
        assertThat(business).contains("## Business Summary");

        // Executive has "Executive Summary"
        String executive = new ExecutiveOutputFormatter().format(response);
        assertThat(executive).contains("## Executive Summary");

        // Raw has no headers
        String raw = new RawOutputFormatter().format(response);
        assertThat(raw).doesNotContain("##");
        assertThat(raw).doesNotContain("**");
    }
}
