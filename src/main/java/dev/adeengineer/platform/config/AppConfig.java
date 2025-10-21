package dev.adeengineer.platform.config;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.DomainLoader;
import dev.adeengineer.platform.factory.LLMProviderFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring configuration for the ade Agent Platform application. Loads agent configurations from
 * domain plugins and registers all agents on startup.
 *
 * <p>Uses domain plugin system for configurable, YAML-driven agents. Default path is 'examples/'
 * for demonstration. Applications should configure their own domain path via 'agents.domains-path'
 * property.
 *
 * @since 0.1.0
 */
@Slf4j
@Configuration
public class AppConfig {

    /**
     * Enable new domain plugin system.
     *
     * <p>Default: true. Set to false to use legacy agent loading only.
     */
    @Value("${agents.domain-plugin-enabled:true}")
    private boolean domainPluginEnabled;

    /**
     * Base path for domain plugins.
     *
     * <p>Default: ../examples (example domains for reference)
     */
    @Value("${agents.domains-path:../examples}")
    private String domainsPath;

    /**
     * Configure Jackson ObjectMapper with Java 8 time support.
     *
     * @return Configured ObjectMapper with JavaTimeModule registered
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Initialize agents using domain plugin system.
     *
     * <p>Loads agents from configured path (default: examples/). Applications should override
     * agents.domains-path to point to their own domains.
     *
     * @param domainLoader Domain loader
     * @param registry Agent registry
     * @param llmProviderFactory LLM provider factory
     * @return ApplicationRunner for domain loading
     * @since 0.2.0
     */
    @Bean
    @Order(1)
    public ApplicationRunner initializeDomainsFromPlugins(
            final DomainLoader domainLoader,
            final AgentRegistry registry,
            final LLMProviderFactory llmProviderFactory) {
        return args -> {
            if (!domainPluginEnabled) {
                log.info("Domain plugin system disabled. " + "Skipping domain loading.");
                return;
            }

            log.info("=== NEW SYSTEM: Loading domains from plugins ===");

            // Check if domains directory exists
            if (!Files.exists(Paths.get(domainsPath))) {
                log.warn(
                        "Domains directory not found: {}. "
                                + "No domains will be loaded from plugins.",
                        domainsPath);
                return;
            }

            // Get LLM provider
            LLMProvider llmProvider = llmProviderFactory.getProviderWithFailover();

            // Load all domains
            try {
                int agentCount = domainLoader.loadAllDomains(domainsPath, llmProvider);

                if (agentCount > 0) {
                    log.info(
                            "=== Domain plugin loading complete: " + "{} agents registered ===",
                            agentCount);
                } else {
                    log.warn("No agents loaded from domain plugins");
                }
            } catch (Exception e) {
                log.error("Error loading domains: {}", e.getMessage(), e);
            }
        };
    }
}
