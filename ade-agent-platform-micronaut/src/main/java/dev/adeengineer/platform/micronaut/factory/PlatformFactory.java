package dev.adeengineer.platform.micronaut.factory;

import java.io.IOException;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Singleton;

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
 * Micronaut factory for ade-agent-platform beans.
 *
 * <p>This factory creates Micronaut-managed beans for all infrastructure providers. The underlying
 * provider implementations are pure POJOs with no framework dependencies.
 *
 * <p><b>Note:</b> All factory methods create instances of core POJOs. There is no
 * Micronaut-specific business logic - everything is reused from the core module.
 *
 * <p><b>Non-Micronaut Usage:</b> Applications not using Micronaut can instantiate providers
 * directly:
 *
 * <pre>{@code
 * AgentRegistry registry = new AgentRegistry();
 * MemoryProvider memory = new InMemoryMemoryProvider(embeddings);
 * }</pre>
 *
 * @since 0.2.0
 */
@Slf4j
@Factory
public class PlatformFactory {

    /**
     * Creates an in-memory memory provider bean.
     *
     * @param embeddingsProvider Embeddings provider for vector search
     * @return InMemoryMemoryProvider instance
     */
    @Singleton
    @Requires(missingBeans = MemoryProvider.class)
    public MemoryProvider inMemoryMemoryProvider(EmbeddingsProvider embeddingsProvider) {
        log.info("Creating InMemoryMemoryProvider");
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
    @Singleton
    @Requires(missingBeans = StorageProvider.class)
    public StorageProvider localStorageProvider(
            @Property(name = "ade.storage.path", defaultValue = "./data/storage")
                    String storagePath,
            ObjectMapper objectMapper)
            throws IOException {
        log.info("Creating LocalStorageProvider with path: {}", storagePath);
        return new LocalStorageProvider(storagePath, objectMapper);
    }

    /**
     * Creates a simple tool provider bean.
     *
     * @return SimpleToolProvider instance with built-in tools
     */
    @Singleton
    @Requires(missingBeans = ToolProvider.class)
    public ToolProvider simpleToolProvider() {
        log.info("Creating SimpleToolProvider");
        return new SimpleToolProvider();
    }

    /**
     * Creates a simple orchestration provider bean.
     *
     * @return SimpleOrchestrationProvider instance
     */
    @Singleton
    @Requires(missingBeans = OrchestrationProvider.class)
    public OrchestrationProvider simpleOrchestrationProvider() {
        log.info("Creating SimpleOrchestrationProvider");
        return new SimpleOrchestrationProvider();
    }

    /**
     * Creates an LLM evaluation provider bean.
     *
     * @param llmProvider LLM provider for evaluation (LLM-as-judge pattern)
     * @return LLMEvaluationProvider instance
     */
    @Singleton
    @Requires(missingBeans = EvaluationProvider.class)
    public EvaluationProvider llmEvaluationProvider(LLMProvider llmProvider) {
        log.info("Creating LLMEvaluationProvider");
        return new LLMEvaluationProvider(llmProvider);
    }

    /**
     * Creates a no-op LLM provider factory bean when no other factory is available.
     *
     * @return NoOpLLMProviderFactory instance
     */
    @Singleton
    @Requires(missingBeans = LLMProviderFactory.class)
    public LLMProviderFactory noOpLLMProviderFactory() {
        log.info("Creating NoOpLLMProviderFactory");
        return new NoOpLLMProviderFactory();
    }

    /**
     * Creates an agent registry bean.
     *
     * @return AgentRegistry instance
     */
    @Singleton
    public AgentRegistry agentRegistry() {
        log.info("Creating AgentRegistry");
        return new AgentRegistry();
    }

    /**
     * Creates output formatter registry bean.
     *
     * @return OutputFormatterRegistry instance
     */
    @Singleton
    public OutputFormatterRegistry outputFormatterRegistry() {
        log.info("Creating OutputFormatterRegistry");
        return new OutputFormatterRegistry();
    }

    /**
     * Creates a domain loader bean.
     *
     * @param agentRegistry Agent registry
     * @param formatterRegistry Output formatter registry
     * @return DomainLoader instance
     */
    @Singleton
    public DomainLoader domainLoader(
            AgentRegistry agentRegistry, OutputFormatterRegistry formatterRegistry) {
        log.info("Creating DomainLoader");
        return new DomainLoader(agentRegistry, formatterRegistry);
    }

    /**
     * Creates a domain manager bean.
     *
     * @param domainLoader Domain loader
     * @param agentRegistry Agent registry
     * @return DomainManager instance
     */
    @Singleton
    public DomainManager domainManager(DomainLoader domainLoader, AgentRegistry agentRegistry) {
        log.info("Creating DomainManager");
        return new DomainManager(domainLoader, agentRegistry);
    }

    /**
     * Creates a role manager bean.
     *
     * @param agentRegistry Agent registry
     * @return RoleManager instance
     */
    @Singleton
    public RoleManager roleManager(AgentRegistry agentRegistry) {
        log.info("Creating RoleManager");
        return new RoleManager(agentRegistry);
    }

    /**
     * Creates a parallel agent executor bean.
     *
     * @param agentRegistry Agent registry
     * @param llmProviderFactory LLM provider factory
     * @return ParallelAgentExecutor instance
     */
    @Singleton
    public ParallelAgentExecutor parallelAgentExecutor(
            AgentRegistry agentRegistry, LLMProviderFactory llmProviderFactory) {
        log.info("Creating ParallelAgentExecutor");
        return new ParallelAgentExecutor(
                agentRegistry, llmProviderFactory.getProviderWithFailover());
    }

    /**
     * Creates a workflow engine bean.
     *
     * @param parallelAgentExecutor Parallel agent executor
     * @return WorkflowEngine instance
     */
    @Singleton
    public WorkflowEngine workflowEngine(ParallelAgentExecutor parallelAgentExecutor) {
        log.info("Creating WorkflowEngine");
        return new WorkflowEngine(parallelAgentExecutor);
    }

    /**
     * Creates a prompt template engine bean.
     *
     * @return PromptTemplateEngine instance
     */
    @Singleton
    public PromptTemplateEngine promptTemplateEngine() {
        log.info("Creating PromptTemplateEngine");
        return new PromptTemplateEngine();
    }
}
