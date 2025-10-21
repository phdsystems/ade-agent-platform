package dev.adeengineer.platform.e2e;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

/**
 * Base class for E2E tests. Provides common setup and utilities for testing the full application
 * stack.
 *
 * <p><b>Note:</b> E2E tests are disabled by default as they require inferencestr8a dependency.
 * To enable E2E tests, add inferencestr8a-core to dependencies and remove @Disabled annotation.
 */
@Disabled("E2E tests require inferencestr8a-core dependency")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2etest")
@TestPropertySource(locations = "classpath:application-e2etest.yml")
@Import(E2ETestConfiguration.class)
public abstract class BaseE2ETest {

    @LocalServerPort protected int port;

    @Autowired protected TestRestTemplate restTemplate;

    // E2E tests would use real provider implementations from inferencestr8a
    // @MockBean protected AnthropicProvider anthropicProvider;
    // @MockBean protected OpenAIProvider openAIProvider;
    // @MockBean protected OllamaProvider ollamaProvider;

    protected String baseUrl;

    @BeforeEach
    void setUpBase() {
        baseUrl = "http://localhost:" + port;

        // E2E tests disabled - would configure mock LLM providers here
        /*
        UsageInfo testUsage = new UsageInfo(25, 50, 75, 0.001);
        LLMResponse testResponse =
                new LLMResponse(
                        "Test LLM response for E2E tests",
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
        */
    }

    /** Get the full URL for an API endpoint */
    protected String apiUrl(String endpoint) {
        return baseUrl + "/api" + endpoint;
    }

    /** Get the full URL for an actuator endpoint */
    protected String actuatorUrl(String endpoint) {
        return baseUrl + "/actuator" + endpoint;
    }
}
