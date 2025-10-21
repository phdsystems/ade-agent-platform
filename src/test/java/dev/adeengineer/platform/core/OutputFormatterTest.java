package dev.adeengineer.platform.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.adeengineer.agent.OutputFormatter;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.testutil.TestData;

@DisplayName("OutputFormatter Tests")
class OutputFormatterTest {

    private OutputFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new OutputFormatter();
    }

    @Test
    @DisplayName("Should format output with technical format")
    void shouldFormatTechnicalOutput() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, "technical");

        // Then
        assertThat(formatted).contains(response.content());
        assertThat(formatted).contains("**Technical Details**");
        assertThat(formatted).contains("Provider:");
        assertThat(formatted).contains("Model:");
        assertThat(formatted).contains("Tokens:");
        assertThat(formatted).contains(response.provider());
        assertThat(formatted).contains(response.model());
    }

    @Test
    @DisplayName("Should format output with business format")
    void shouldFormatBusinessOutput() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, "business");

        // Then
        assertThat(formatted).contains("## Business Summary");
        assertThat(formatted).contains(response.content());
        assertThat(formatted).contains("**Resource Usage**");
        assertThat(formatted).contains("Estimated Cost:");
        assertThat(formatted).contains("$");
    }

    @Test
    @DisplayName("Should format output with executive format")
    void shouldFormatExecutiveOutput() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, "executive");

        // Then
        assertThat(formatted).contains("## Executive Summary");
        assertThat(formatted).contains(response.content());
        assertThat(formatted).doesNotContain("Provider:");
        assertThat(formatted).doesNotContain("Tokens:");
        assertThat(formatted).doesNotContain("Cost:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"TECHNICAL", "Technical", "TeChnical"})
    @DisplayName("Should handle technical format case-insensitively")
    void shouldHandleTechnicalFormatCaseInsensitively(String formatType) {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, formatType);

        // Then
        assertThat(formatted).contains("**Technical Details**");
    }

    @ParameterizedTest
    @ValueSource(strings = {"BUSINESS", "Business", "BuSiNeSs"})
    @DisplayName("Should handle business format case-insensitively")
    void shouldHandleBusinessFormatCaseInsensitively(String formatType) {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, formatType);

        // Then
        assertThat(formatted).contains("## Business Summary");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXECUTIVE", "Executive", "ExEcUtIvE"})
    @DisplayName("Should handle executive format case-insensitively")
    void shouldHandleExecutiveFormatCaseInsensitively(String formatType) {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, formatType);

        // Then
        assertThat(formatted).contains("## Executive Summary");
    }

    @Test
    @DisplayName("Should return raw content for unknown format")
    void shouldReturnRawContentForUnknownFormat() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, "unknown");

        // Then
        assertThat(formatted).isEqualTo(response.content());
        assertThat(formatted).doesNotContain("**Technical Details**");
        assertThat(formatted).doesNotContain("## Business Summary");
        assertThat(formatted).doesNotContain("## Executive Summary");
    }

    @Test
    @DisplayName("Should handle null response")
    void shouldHandleNullResponse() {
        // When
        String formatted = formatter.format(null, "technical");

        // Then
        assertThat(formatted).isEmpty();
    }

    @Test
    @DisplayName("Should handle response with null content")
    void shouldHandleResponseWithNullContent() {
        // Given
        LLMResponse response =
                new LLMResponse("Valid content", TestData.validUsageInfo(), "provider", "model");
        // Create a response with null content through constructor
        LLMResponse nullContentResponse = null;

        // When
        String formatted = formatter.format(nullContentResponse, "technical");

        // Then
        assertThat(formatted).isEmpty();
    }

    @Test
    @DisplayName("Should format cost with correct precision in business format")
    void shouldFormatCostWithCorrectPrecision() {
        // Given
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.123456);
        LLMResponse response = new LLMResponse("Content", usage, "provider", "model");

        // When
        String formatted = formatter.format(response, "business");

        // Then
        assertThat(formatted).contains("$0.1235");
    }

    @Test
    @DisplayName("Should include total tokens in technical format")
    void shouldIncludeTotalTokensInTechnicalFormat() {
        // Given
        UsageInfo usage = new UsageInfo(150, 250, 400, 0.01);
        LLMResponse response = new LLMResponse("Content", usage, "anthropic", "claude");

        // When
        String formatted = formatter.format(response, "technical");

        // Then
        assertThat(formatted).contains("Tokens: 400");
    }

    @Test
    @DisplayName("Should handle long content in technical format")
    void shouldHandleLongContentInTechnicalFormat() {
        // Given
        String longContent = TestData.longString(10000);
        LLMResponse response = TestData.llmResponseWithContent(longContent);

        // When
        String formatted = formatter.format(response, "technical");

        // Then
        assertThat(formatted).contains(longContent);
        assertThat(formatted).contains("**Technical Details**");
    }

    @Test
    @DisplayName("Should handle empty format type by returning raw content")
    void shouldHandleEmptyFormatType() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, "");

        // Then
        assertThat(formatted).isEqualTo(response.content());
    }

    @Test
    @DisplayName("Should preserve markdown formatting in content")
    void shouldPreserveMarkdownFormattingInContent() {
        // Given
        String markdownContent =
                "# Header\n## Subheader\n- Item 1\n- Item 2\n```java\ncode();\n```";
        LLMResponse response = TestData.llmResponseWithContent(markdownContent);

        // When
        String formatted = formatter.format(response, "technical");

        // Then
        assertThat(formatted).contains("# Header");
        assertThat(formatted).contains("```java");
    }

    @Test
    @DisplayName("Should handle special characters in provider name")
    void shouldHandleSpecialCharactersInProviderName() {
        // Given
        LLMResponse response = TestData.llmResponseWithProvider("provider-v2.0", "model_name");

        // When
        String formatted = formatter.format(response, "technical");

        // Then
        assertThat(formatted).contains("Provider: provider-v2.0");
        assertThat(formatted).contains("Model: model_name");
    }

    @Test
    @DisplayName("Technical format should include all metadata")
    void technicalFormatShouldIncludeAllMetadata() {
        // Given
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.05);
        LLMResponse response = new LLMResponse("Technical content", usage, "ollama", "qwen3:0.6b");

        // When
        String formatted = formatter.format(response, "technical");

        // Then
        assertThat(formatted).contains("Provider: ollama");
        assertThat(formatted).contains("Model: qwen3:0.6b");
        assertThat(formatted).contains("Tokens: 300");
        assertThat(formatted).endsWith("Tokens: 300\n");
    }

    @Test
    @DisplayName("Business format should only include cost")
    void businessFormatShouldOnlyIncludeCost() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, "business");

        // Then
        assertThat(formatted).contains("Estimated Cost:");
        assertThat(formatted).doesNotContain("Provider:");
        assertThat(formatted).doesNotContain("Model:");
        assertThat(formatted).doesNotContain("Tokens:");
    }

    @Test
    @DisplayName("Executive format should be most concise")
    void executiveFormatShouldBeMostConcise() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // When
        String formatted = formatter.format(response, "executive");

        // Then
        assertThat(formatted).doesNotContain("Provider:");
        assertThat(formatted).doesNotContain("Model:");
        assertThat(formatted).doesNotContain("Tokens:");
        assertThat(formatted).doesNotContain("Cost:");
        assertThat(formatted).doesNotContain("**Technical Details**");
        assertThat(formatted).doesNotContain("**Resource Usage**");
    }

    @Test
    @DisplayName("Should handle zero cost in business format")
    void shouldHandleZeroCostInBusinessFormat() {
        // Given
        UsageInfo usage = new UsageInfo(10, 20, 30, 0.0);
        LLMResponse response = new LLMResponse("Content", usage, "provider", "model");

        // When
        String formatted = formatter.format(response, "business");

        // Then
        assertThat(formatted).contains("$0.0000");
    }

    @Test
    @DisplayName("Should handle high cost in business format")
    void shouldHandleHighCostInBusinessFormat() {
        // Given
        UsageInfo usage = new UsageInfo(100000, 200000, 300000, 15.5678);
        LLMResponse response = new LLMResponse("Content", usage, "provider", "model");

        // When
        String formatted = formatter.format(response, "business");

        // Then
        assertThat(formatted).contains("$15.5678");
    }
}
