package dev.adeengineer.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import dev.adeengineer.embeddings.EmbeddingsProvider;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.cache.CachedLLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;

/**
 * Unit tests for CachedLLMProvider. Tests caching behavior without requiring actual LLM providers.
 */
@SpringBootTest
@TestPropertySource(
        properties = {
            "llm.cache.enabled=true",
            "llm.cache.ttl-minutes=60",
            "llm.cache.max-size=100"
        })
@DisplayName("CachedLLMProvider Tests")
class CachedLLMProviderTest {

    @MockBean private LLMProvider mockProvider;

    @MockBean private EmbeddingsProvider embeddingsProvider;

    private CachedLLMProvider cachedProvider;

    @MockBean private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cachedProvider = new CachedLLMProvider();
        when(mockProvider.getProviderName()).thenReturn("test-provider");
    }

    @Test
    @DisplayName("Should generate hash for prompt")
    void shouldGenerateHashForPrompt() {
        // Given
        String prompt = "Hello, world!";

        // When
        String hash1 = cachedProvider.hashPrompt(prompt);
        String hash2 = cachedProvider.hashPrompt(prompt);

        // Then
        assertThat(hash1).isNotNull();
        assertThat(hash1).hasSize(16); // First 16 characters of SHA-256
        assertThat(hash1).isEqualTo(hash2); // Same prompt = same hash
    }

    @Test
    @DisplayName("Should generate different hashes for different prompts")
    void shouldGenerateDifferentHashesForDifferentPrompts() {
        // Given
        String prompt1 = "Hello, world!";
        String prompt2 = "Goodbye, world!";

        // When
        String hash1 = cachedProvider.hashPrompt(prompt1);
        String hash2 = cachedProvider.hashPrompt(prompt2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Should delegate to provider when cache disabled")
    void shouldDelegateToProviderWhenCacheDisabled() {
        // Given
        String prompt = "Test prompt";
        UsageInfo usage = new UsageInfo(10, 20, 30, 0.001);
        LLMResponse expectedResponse =
                new LLMResponse("Test response", usage, "test-provider", "test-model");

        when(mockProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(expectedResponse);

        // When
        LLMResponse response = cachedProvider.generateWithCache(mockProvider, prompt, 0.7, 100);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(mockProvider, times(1)).generate(prompt, 0.7, 100);
    }
}
