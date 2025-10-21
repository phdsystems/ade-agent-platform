package dev.adeengineer.platform.spring.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.llm.providers.AnthropicProvider;
import dev.adeengineer.llm.providers.OllamaProvider;
import dev.adeengineer.llm.providers.OpenAIProvider;

/**
 * Base class for provider failover integration tests. Uses mocked providers to test failover logic
 * and provider selection.
 *
 * <p>Note: This is a special case - failover testing requires mocking provider health checks to
 * simulate different failure scenarios. Most integration tests should extend {@link
 * BaseIntegrationTest} instead to use real providers.
 */
@SpringBootTest
@ActiveProfiles("integrationtest")
@Import(IntegrationTestConfiguration.class)
public abstract class BaseProviderFailoverTest {

    @MockBean protected AnthropicProvider anthropicProvider;

    @MockBean protected OpenAIProvider openAIProvider;

    @MockBean protected OllamaProvider ollamaProvider;

    @BeforeEach
    void setUpBase() {
        // Configure mock LLM providers for failover testing
        UsageInfo testUsage = new UsageInfo(25, 50, 75, 0.001);
        LLMResponse testResponse =
                new LLMResponse(
                        "Test LLM response for failover tests",
                        testUsage,
                        "test-provider",
                        "test-model");

        // Configure Anthropic provider (primary in tests)
        when(anthropicProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(testResponse);
        when(anthropicProvider.isHealthy()).thenReturn(true);
        when(anthropicProvider.getProviderName()).thenReturn("anthropic-test");
        when(anthropicProvider.getModel()).thenReturn("claude-test");

        // Configure OpenAI provider (fallback)
        when(openAIProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(testResponse);
        when(openAIProvider.isHealthy()).thenReturn(false);
        when(openAIProvider.getProviderName()).thenReturn("openai-test");
        when(openAIProvider.getModel()).thenReturn("gpt-test");

        // Configure Ollama provider (fallback)
        when(ollamaProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(testResponse);
        when(ollamaProvider.isHealthy()).thenReturn(false);
        when(ollamaProvider.getProviderName()).thenReturn("ollama-test");
        when(ollamaProvider.getModel()).thenReturn("qwen-test");
    }
}
