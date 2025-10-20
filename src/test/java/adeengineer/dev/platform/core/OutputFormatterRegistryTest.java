package adeengineer.dev.platform.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.llm.model.UsageInfo;

/** Unit tests for OutputFormatterRegistry. */
class OutputFormatterRegistryTest {

    private OutputFormatterRegistry registry;
    private LLMResponse mockResponse;

    @BeforeEach
    void setUp() {
        registry = new OutputFormatterRegistry();
        registry.registerBuiltInFormats();

        // Create mock LLM response
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.001);
        mockResponse = new LLMResponse("Test content", usage, "test-provider", "test-model");
    }

    @Test
    void testBuiltInFormatsRegistered() {
        // Verify all built-in formats are registered
        Set<String> formats = registry.getAvailableFormats();

        assertThat(formats).contains("technical", "business", "executive", "raw");
        assertThat(registry.getFormatterCount()).isEqualTo(4);
    }

    @Test
    void testRegisterCustomFormat() {
        // Create custom formatter
        OutputFormatStrategy customFormatter =
                new OutputFormatStrategy() {
                    @Override
                    public String format(LLMResponse response) {
                        return "CUSTOM: " + response.content();
                    }

                    @Override
                    public String getFormatName() {
                        return "custom";
                    }
                };

        registry.registerFormat("custom", customFormatter);

        assertThat(registry.hasFormat("custom")).isTrue();
        assertThat(registry.getFormatterCount()).isEqualTo(5);
    }

    @Test
    void testFormatWithTechnical() {
        String result = registry.format(mockResponse, "technical");

        assertThat(result).contains("Test content");
        assertThat(result).contains("Technical Details");
        assertThat(result).contains("Provider: test-provider");
        assertThat(result).contains("Model: test-model");
        assertThat(result).contains("Tokens: 300");
    }

    @Test
    void testFormatWithBusiness() {
        String result = registry.format(mockResponse, "business");

        assertThat(result).contains("Business Summary");
        assertThat(result).contains("Test content");
        assertThat(result).contains("Resource Usage");
        assertThat(result).contains("Estimated Cost: $0.0010");
    }

    @Test
    void testFormatWithExecutive() {
        String result = registry.format(mockResponse, "executive");

        assertThat(result).contains("Executive Summary");
        assertThat(result).contains("Test content");
        assertThat(result).doesNotContain("Provider:"); // No technical details
    }

    @Test
    void testFormatWithRaw() {
        String result = registry.format(mockResponse, "raw");

        assertThat(result).isEqualTo("Test content");
        assertThat(result).doesNotContain("Summary"); // No headers
        assertThat(result).doesNotContain("Provider:"); // No metadata
    }

    @Test
    void testFormatCaseInsensitive() {
        String result1 = registry.format(mockResponse, "TECHNICAL");
        String result2 = registry.format(mockResponse, "Technical");
        String result3 = registry.format(mockResponse, "technical");

        assertThat(result1).isEqualTo(result2);
        assertThat(result2).isEqualTo(result3);
    }

    @Test
    void testFormatUnknownType() {
        String result = registry.format(mockResponse, "unknown-format");

        // Should fall back to raw content
        assertThat(result).isEqualTo("Test content");
    }

    @Test
    void testFormatNullResponse() {
        String result = registry.format(null, "technical");

        assertThat(result).isEmpty();
    }

    @Test
    void testFormatNullType() {
        String result = registry.format(mockResponse, null);

        // Should fall back to raw content
        assertThat(result).isEqualTo("Test content");
    }

    @Test
    void testFormatBlankType() {
        String result = registry.format(mockResponse, "   ");

        // Should fall back to raw content
        assertThat(result).isEqualTo("Test content");
    }

    @Test
    void testHasFormat() {
        assertThat(registry.hasFormat("technical")).isTrue();
        assertThat(registry.hasFormat("TECHNICAL")).isTrue();
        assertThat(registry.hasFormat("nonexistent")).isFalse();
        assertThat(registry.hasFormat(null)).isFalse();
        assertThat(registry.hasFormat("")).isFalse();
    }

    @Test
    void testGetFormatter() {
        OutputFormatStrategy formatter = registry.getFormatter("technical");

        assertThat(formatter).isNotNull();
        assertThat(formatter.getFormatName()).isEqualTo("technical");
    }

    @Test
    void testGetFormatterCaseInsensitive() {
        OutputFormatStrategy formatter1 = registry.getFormatter("TECHNICAL");
        OutputFormatStrategy formatter2 = registry.getFormatter("technical");

        assertThat(formatter1).isEqualTo(formatter2);
    }

    @Test
    void testGetFormatterNotFound() {
        OutputFormatStrategy formatter = registry.getFormatter("nonexistent");

        assertThat(formatter).isNull();
    }

    @Test
    void testUnregisterFormat() {
        assertThat(registry.hasFormat("technical")).isTrue();

        boolean removed = registry.unregisterFormat("technical");

        assertThat(removed).isTrue();
        assertThat(registry.hasFormat("technical")).isFalse();
        assertThat(registry.getFormatterCount()).isEqualTo(3);
    }

    @Test
    void testUnregisterNonexistentFormat() {
        boolean removed = registry.unregisterFormat("nonexistent");

        assertThat(removed).isFalse();
    }

    @Test
    void testRegisterNullName() {
        OutputFormatStrategy formatter =
                new OutputFormatStrategy() {
                    @Override
                    public String format(LLMResponse response) {
                        return response.content();
                    }

                    @Override
                    public String getFormatName() {
                        return "test";
                    }
                };

        assertThatThrownBy(() -> registry.registerFormat(null, formatter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Format name cannot be null or blank");
    }

    @Test
    void testRegisterBlankName() {
        OutputFormatStrategy formatter =
                new OutputFormatStrategy() {
                    @Override
                    public String format(LLMResponse response) {
                        return response.content();
                    }

                    @Override
                    public String getFormatName() {
                        return "test";
                    }
                };

        assertThatThrownBy(() -> registry.registerFormat("   ", formatter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Format name cannot be null or blank");
    }

    @Test
    void testRegisterNullFormatter() {
        assertThatThrownBy(() -> registry.registerFormat("test", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formatter cannot be null");
    }

    @Test
    void testGetAvailableFormats() {
        Set<String> formats = registry.getAvailableFormats();

        assertThat(formats).isNotEmpty();
        assertThat(formats).containsExactlyInAnyOrder("technical", "business", "executive", "raw");

        // Returned set should be immutable (copy)
        assertThatThrownBy(() -> formats.add("new-format"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testFormatterOverride() {
        // Create custom technical formatter
        OutputFormatStrategy customTechnical =
                new OutputFormatStrategy() {
                    @Override
                    public String format(LLMResponse response) {
                        return "OVERRIDE: " + response.content();
                    }

                    @Override
                    public String getFormatName() {
                        return "technical";
                    }
                };

        registry.registerFormat("technical", customTechnical);

        String result = registry.format(mockResponse, "technical");

        assertThat(result).isEqualTo("OVERRIDE: Test content");
    }
}
