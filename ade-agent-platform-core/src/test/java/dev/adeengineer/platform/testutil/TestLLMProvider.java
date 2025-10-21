package dev.adeengineer.platform.testutil;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

/**
 * Test implementation of LLMProvider for unit testing. Provides configurable responses without
 * making actual API calls.
 */
public class TestLLMProvider implements LLMProvider {

    private final String providerName;
    private final String model;
    private final boolean healthy;
    private String responseContent = "Test LLM response";
    private RuntimeException exceptionToThrow;

    public TestLLMProvider() {
        this("test-provider", "test-model", true);
    }

    public TestLLMProvider(String providerName, String model, boolean healthy) {
        this.providerName = providerName;
        this.model = model;
        this.healthy = healthy;
    }

    @Override
    public LLMResponse generate(String prompt, double temperature, int maxTokens) {
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }

        UsageInfo usage =
                new UsageInfo(
                        prompt.length() / 4, // Rough token estimate
                        responseContent.length() / 4,
                        (prompt.length() + responseContent.length()) / 4,
                        0.001);

        return new LLMResponse(responseContent, usage, providerName, model);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    // Configuration methods for testing
    public TestLLMProvider withResponseContent(String content) {
        this.responseContent = content;
        return this;
    }

    public TestLLMProvider withException(RuntimeException exception) {
        this.exceptionToThrow = exception;
        return this;
    }
}
