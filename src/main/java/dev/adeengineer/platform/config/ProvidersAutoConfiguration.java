package dev.adeengineer.platform.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.adeengineer.embeddings.EmbeddingsProvider;
import dev.adeengineer.evaluation.EvaluationProvider;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.memory.MemoryProvider;
import dev.adeengineer.orchestration.OrchestrationProvider;
import dev.adeengineer.platform.factory.LLMProviderFactory;
import dev.adeengineer.platform.factory.NoOpLLMProviderFactory;
import dev.adeengineer.platform.providers.evaluation.LLMEvaluationProvider;
import dev.adeengineer.platform.providers.memory.InMemoryMemoryProvider;
import dev.adeengineer.platform.providers.orchestration.SimpleOrchestrationProvider;
import dev.adeengineer.platform.providers.storage.LocalStorageProvider;
import dev.adeengineer.platform.providers.tools.SimpleToolProvider;
import dev.adeengineer.storage.StorageProvider;
import dev.adeengineer.tools.ToolProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot auto-configuration for ade-agent-platform providers.
 *
 * <p>This configuration class automatically creates beans for all infrastructure providers when
 * used in a Spring Boot application. Providers are created with {@code @ConditionalOnMissingBean}
 * so applications can override them with custom implementations.
 *
 * <p><b>Note:</b> The underlying provider implementations are pure POJOs with no Spring
 * dependencies. This configuration is only needed for Spring Boot applications that want automatic
 * bean registration.
 *
 * <p><b>Non-Spring Usage:</b> Applications not using Spring can instantiate providers directly:
 *
 * <pre>{@code
 * EmbeddingsProvider embeddings = ...;
 * MemoryProvider memory = new InMemoryMemoryProvider(embeddings);
 * }</pre>
 *
 * @since 0.2.0
 */
@Slf4j
@Configuration
public class ProvidersAutoConfiguration {

    /**
     * Creates an in-memory memory provider bean.
     *
     * @param embeddingsProvider Embeddings provider for vector search
     * @return InMemoryMemoryProvider instance
     */
    @Bean
    @ConditionalOnMissingBean(MemoryProvider.class)
    public MemoryProvider inMemoryMemoryProvider(EmbeddingsProvider embeddingsProvider) {
        log.info("Auto-configuring InMemoryMemoryProvider");
        return new InMemoryMemoryProvider(embeddingsProvider);
    }

    /**
     * Creates a local storage provider bean.
     *
     * @param storagePath Root directory for document storage
     * @param objectMapper Jackson ObjectMapper for metadata serialization
     * @return LocalStorageProvider instance
     * @throws IOException if storage directory cannot be created
     */
    @Bean
    @ConditionalOnMissingBean(StorageProvider.class)
    public StorageProvider localStorageProvider(
            @Value("${ade.storage.path:./data/storage}") String storagePath,
            ObjectMapper objectMapper)
            throws IOException {
        log.info("Auto-configuring LocalStorageProvider with path: {}", storagePath);
        return new LocalStorageProvider(storagePath, objectMapper);
    }

    /**
     * Creates a simple tool provider bean.
     *
     * @return SimpleToolProvider instance with built-in tools
     */
    @Bean
    @ConditionalOnMissingBean(ToolProvider.class)
    public ToolProvider simpleToolProvider() {
        log.info("Auto-configuring SimpleToolProvider");
        return new SimpleToolProvider();
    }

    /**
     * Creates a simple orchestration provider bean.
     *
     * @return SimpleOrchestrationProvider instance
     */
    @Bean
    @ConditionalOnMissingBean(OrchestrationProvider.class)
    public OrchestrationProvider simpleOrchestrationProvider() {
        log.info("Auto-configuring SimpleOrchestrationProvider");
        return new SimpleOrchestrationProvider();
    }

    /**
     * Creates an LLM evaluation provider bean.
     *
     * @param llmProvider LLM provider for evaluation (LLM-as-judge pattern)
     * @return LLMEvaluationProvider instance
     */
    @Bean
    @ConditionalOnMissingBean(EvaluationProvider.class)
    public EvaluationProvider llmEvaluationProvider(LLMProvider llmProvider) {
        log.info("Auto-configuring LLMEvaluationProvider");
        return new LLMEvaluationProvider(llmProvider);
    }

    /**
     * Creates a no-op LLM provider factory bean when no other factory is available.
     *
     * @return NoOpLLMProviderFactory instance
     */
    @Bean
    @ConditionalOnMissingBean(LLMProviderFactory.class)
    public LLMProviderFactory noOpLLMProviderFactory() {
        log.info("Auto-configuring NoOpLLMProviderFactory");
        return new NoOpLLMProviderFactory();
    }
}
