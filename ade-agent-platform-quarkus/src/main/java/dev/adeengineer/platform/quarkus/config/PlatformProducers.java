package dev.adeengineer.platform.quarkus.config;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import adeengineer.dev.agent.OutputFormatterRegistry;

import dev.adeengineer.embeddings.EmbeddingsProvider;
import dev.adeengineer.evaluation.EvaluationProvider;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.memory.MemoryProvider;
import dev.adeengineer.orchestration.OrchestrationProvider;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.DomainLoader;
import dev.adeengineer.platform.core.DomainManager;
import dev.adeengineer.platform.core.RoleManager;
import dev.adeengineer.platform.factory.LLMProviderFactory;
import dev.adeengineer.platform.factory.NoOpLLMProviderFactory;
import dev.adeengineer.platform.orchestration.ParallelAgentExecutor;
import dev.adeengineer.platform.orchestration.WorkflowEngine;
import dev.adeengineer.platform.providers.evaluation.LLMEvaluationProvider;
import dev.adeengineer.platform.providers.memory.InMemoryMemoryProvider;
import dev.adeengineer.platform.providers.orchestration.SimpleOrchestrationProvider;
import dev.adeengineer.platform.providers.storage.LocalStorageProvider;
import dev.adeengineer.platform.providers.tools.SimpleToolProvider;
import dev.adeengineer.platform.template.PromptTemplateEngine;
import dev.adeengineer.storage.StorageProvider;
import dev.adeengineer.tools.ToolProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Quarkus CDI producers for ade-agent-platform.
 *
 * <p>This class creates CDI beans for all infrastructure providers. The underlying provider
 * implementations are pure POJOs with no framework dependencies.
 *
 * <p><b>Note:</b> All producers create instances of core POJOs. There is no Quarkus-specific
 * business logic - everything is reused from the core module.
 *
 * <p><b>Non-Quarkus Usage:</b> Applications not using Quarkus can instantiate providers directly:
 *
 * <pre>{@code
 * AgentRegistry registry = new AgentRegistry();
 * MemoryProvider memory = new InMemoryMemoryProvider(embeddings);
 * }</pre>
 *
 * @since 0.2.0
 */
@Slf4j
@ApplicationScoped
public class PlatformProducers {

    /**
     * Produces an in-memory memory provider bean.
     *
     * @param embeddingsProvider Embeddings provider for vector search
     * @return InMemoryMemoryProvider instance
     */
    @Produces
    @Singleton
    public MemoryProvider inMemoryMemoryProvider(EmbeddingsProvider embeddingsProvider) {
        log.info("Producing InMemoryMemoryProvider");
        return new InMemoryMemoryProvider(embeddingsProvider);
    }

    /**
     * Produces a local storage provider bean.
     *
     * @param storagePath Root directory for document storage
     * @param objectMapper Jackson ObjectMapper for metadata serialization
     * @return LocalStorageProvider instance
     * @throws IOException if storage directory cannot be created
     */
    @Produces
    @Singleton
    public StorageProvider localStorageProvider(
            @ConfigProperty(name = "ade.storage.path", defaultValue = "./data/storage")
                    String storagePath,
            ObjectMapper objectMapper)
            throws IOException {
        log.info("Producing LocalStorageProvider with path: {}", storagePath);
        return new LocalStorageProvider(storagePath, objectMapper);
    }

    /**
     * Produces a simple tool provider bean.
     *
     * @return SimpleToolProvider instance with built-in tools
     */
    @Produces
    @Singleton
    public ToolProvider simpleToolProvider() {
        log.info("Producing SimpleToolProvider");
        return new SimpleToolProvider();
    }

    /**
     * Produces a simple orchestration provider bean.
     *
     * @return SimpleOrchestrationProvider instance
     */
    @Produces
    @Singleton
    public OrchestrationProvider simpleOrchestrationProvider() {
        log.info("Producing SimpleOrchestrationProvider");
        return new SimpleOrchestrationProvider();
    }

    /**
     * Produces an LLM evaluation provider bean.
     *
     * @param llmProvider LLM provider for evaluation (LLM-as-judge pattern)
     * @return LLMEvaluationProvider instance
     */
    @Produces
    @Singleton
    public EvaluationProvider llmEvaluationProvider(LLMProvider llmProvider) {
        log.info("Producing LLMEvaluationProvider");
        return new LLMEvaluationProvider(llmProvider);
    }

    /**
     * Produces a no-op LLM provider factory bean when no other factory is available.
     *
     * @return NoOpLLMProviderFactory instance
     */
    @Produces
    @Singleton
    public LLMProviderFactory noOpLLMProviderFactory() {
        log.info("Producing NoOpLLMProviderFactory");
        return new NoOpLLMProviderFactory();
    }

    /**
     * Produces an LLM provider bean using the factory.
     *
     * @param factory LLM provider factory
     * @return LLMProvider instance with failover support
     */
    @Produces
    @Singleton
    public LLMProvider llmProvider(LLMProviderFactory factory) {
        log.info("Producing LLMProvider from factory");
        return factory.getProviderWithFailover();
    }


    /**
     * Produces an agent registry bean.
     *
     * @return AgentRegistry instance
     */
    @Produces
    @Singleton
    public AgentRegistry agentRegistry() {
        log.info("Producing AgentRegistry");
        return new AgentRegistry();
    }

    /**
     * Produces output formatter registry bean.
     *
     * @return OutputFormatterRegistry instance
     */
    @Produces
    @Singleton
    public OutputFormatterRegistry outputFormatterRegistry() {
        log.info("Producing OutputFormatterRegistry");
        return new OutputFormatterRegistry();
    }

    /**
     * Produces a domain loader bean.
     *
     * @param agentRegistry Agent registry
     * @param formatterRegistry Output formatter registry
     * @return DomainLoader instance
     */
    @Produces
    @Singleton
    public DomainLoader domainLoader(
            AgentRegistry agentRegistry, OutputFormatterRegistry formatterRegistry) {
        log.info("Producing DomainLoader");
        return new DomainLoader(agentRegistry, formatterRegistry);
    }

    /**
     * Produces a domain manager bean.
     *
     * @param domainLoader Domain loader
     * @param agentRegistry Agent registry
     * @return DomainManager instance
     */
    @Produces
    @Singleton
    public DomainManager domainManager(DomainLoader domainLoader, AgentRegistry agentRegistry) {
        log.info("Producing DomainManager");
        return new DomainManager(domainLoader, agentRegistry);
    }

    /**
     * Produces a role manager bean.
     *
     * @param agentRegistry Agent registry
     * @return RoleManager instance
     */
    @Produces
    @Singleton
    public RoleManager roleManager(AgentRegistry agentRegistry) {
        log.info("Producing RoleManager");
        return new RoleManager(agentRegistry);
    }

    /**
     * Produces a parallel agent executor bean.
     *
     * @param agentRegistry Agent registry
     * @param llmProviderFactory LLM provider factory
     * @return ParallelAgentExecutor instance
     */
    @Produces
    @Singleton
    public ParallelAgentExecutor parallelAgentExecutor(
            AgentRegistry agentRegistry, LLMProviderFactory llmProviderFactory) {
        log.info("Producing ParallelAgentExecutor");
        return new ParallelAgentExecutor(
                agentRegistry, llmProviderFactory.getProviderWithFailover());
    }

    /**
     * Produces a workflow engine bean.
     *
     * @param parallelAgentExecutor Parallel agent executor
     * @return WorkflowEngine instance
     */
    @Produces
    @Singleton
    public WorkflowEngine workflowEngine(ParallelAgentExecutor parallelAgentExecutor) {
        log.info("Producing WorkflowEngine");
        return new WorkflowEngine(parallelAgentExecutor);
    }

    /**
     * Produces a prompt template engine bean.
     *
     * @return PromptTemplateEngine instance
     */
    @Produces
    @Singleton
    public PromptTemplateEngine promptTemplateEngine() {
        log.info("Producing PromptTemplateEngine");
        return new PromptTemplateEngine();
    }
}
