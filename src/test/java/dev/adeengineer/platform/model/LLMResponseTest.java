package dev.adeengineer.platform.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.testutil.TestData;

@DisplayName("LLMResponse Model Tests")
class LLMResponseTest {

    @Test
    @DisplayName("Should create valid LLMResponse with all fields")
    void shouldCreateValidLLMResponse() {
        // Given
        String content = "This is a response";
        UsageInfo usage = TestData.validUsageInfo();
        String provider = "anthropic";
        String model = "claude-3-5-sonnet";

        // When
        LLMResponse response = new LLMResponse(content, usage, provider, model);

        // Then
        assertThat(response.content()).isEqualTo(content);
        assertThat(response.usage()).isEqualTo(usage);
        assertThat(response.provider()).isEqualTo(provider);
        assertThat(response.model()).isEqualTo(model);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should throw exception when content is null, empty, or blank")
    void shouldThrowExceptionWhenContentIsInvalid(String invalidContent) {
        // When/Then
        assertThatThrownBy(
                        () ->
                                new LLMResponse(
                                        invalidContent,
                                        TestData.validUsageInfo(),
                                        "provider",
                                        "model"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content cannot be null or blank");
    }

    @Test
    @DisplayName("Should create LLMResponse with null usage")
    void shouldCreateLLMResponseWithNullUsage() {
        // When
        LLMResponse response = new LLMResponse("Content", null, "provider", "model");

        // Then
        assertThat(response.usage()).isNull();
        assertThat(response.content()).isEqualTo("Content");
    }

    @Test
    @DisplayName("Should create LLMResponse with null provider")
    void shouldCreateLLMResponseWithNullProvider() {
        // When
        LLMResponse response = new LLMResponse("Content", TestData.validUsageInfo(), null, "model");

        // Then
        assertThat(response.provider()).isNull();
        assertThat(response.content()).isEqualTo("Content");
    }

    @Test
    @DisplayName("Should create LLMResponse with null model")
    void shouldCreateLLMResponseWithNullModel() {
        // When
        LLMResponse response =
                new LLMResponse("Content", TestData.validUsageInfo(), "provider", null);

        // Then
        assertThat(response.model()).isNull();
        assertThat(response.content()).isEqualTo("Content");
    }

    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        String longContent = TestData.longString(10000);

        // When
        LLMResponse response = TestData.llmResponseWithContent(longContent);

        // Then
        assertThat(response.content()).hasSize(10000);
    }

    @Test
    @DisplayName("Should handle content with special characters")
    void shouldHandleContentWithSpecialCharacters() {
        // Given
        String specialContent = "Content with\nnewlines\tand\ttabs and émojis 🚀";

        // When
        LLMResponse response = TestData.llmResponseWithContent(specialContent);

        // Then
        assertThat(response.content()).isEqualTo(specialContent);
    }

    @Test
    @DisplayName("Should support record equality")
    void shouldSupportRecordEquality() {
        // Given
        LLMResponse response1 = TestData.validLLMResponse();
        LLMResponse response2 =
                new LLMResponse(
                        response1.content(),
                        response1.usage(),
                        response1.provider(),
                        response1.model());

        // Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        // Given
        LLMResponse response = TestData.validLLMResponse();

        // Then
        String toString = response.toString();
        assertThat(toString).contains("test-provider");
        assertThat(toString).contains("test-model");
    }

    @Test
    @DisplayName("Should handle different provider names")
    void shouldHandleDifferentProviderNames() {
        // When
        LLMResponse anthropic = TestData.llmResponseWithProvider("anthropic", "claude");
        LLMResponse openai = TestData.llmResponseWithProvider("openai", "gpt-4");
        LLMResponse ollama = TestData.llmResponseWithProvider("ollama", "llama");

        // Then
        assertThat(anthropic.provider()).isEqualTo("anthropic");
        assertThat(openai.provider()).isEqualTo("openai");
        assertThat(ollama.provider()).isEqualTo("ollama");
    }
}
