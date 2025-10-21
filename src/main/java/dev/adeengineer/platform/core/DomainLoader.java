package dev.adeengineer.platform.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import dev.adeengineer.agent.Agent;
import dev.adeengineer.agent.AgentConfig;
import dev.adeengineer.agent.OutputFormatterRegistry;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.config.AgentConfigLoader;
import dev.adeengineer.platform.model.DomainConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Loads domain plugins from the file system.
 *
 * <p>Responsible for:
 *
 * <ul>
 *   <li>Loading domain.yaml configuration
 *   <li>Discovering agent YAML files in the domain
 *   <li>Creating ConfigurableAgent instances
 *   <li>Registering agents in the AgentRegistry
 *   <li>Registering domain-specific output formats
 * </ul>
 *
 * <h3>Domain Directory Structure</h3>
 *
 * <pre>
 * domains/
 * └── healthcare/
 *     ├── domain.yaml
 *     └── agents/
 *         ├── diagnostics.yaml
 *         ├── treatment.yaml
 *         └── triage.yaml
 * </pre>
 *
 * @since 0.2.0
 */
@Slf4j
@Component
public class DomainLoader {

    private final AgentRegistry agentRegistry;
    private final AgentConfigLoader agentConfigLoader;
    private final OutputFormatterRegistry formatterRegistry;
    private final ObjectMapper yamlMapper;

    /**
     * Constructs a new DomainLoader.
     *
     * @param agentRegistry Registry to store loaded agents
     * @param agentConfigLoader Loader for agent YAML files
     * @param formatterRegistry Registry for output formatters
     */
    public DomainLoader(
            final AgentRegistry agentRegistry,
            final AgentConfigLoader agentConfigLoader,
            final OutputFormatterRegistry formatterRegistry) {
        this.agentRegistry = agentRegistry;
        this.agentConfigLoader = agentConfigLoader;
        this.formatterRegistry = formatterRegistry;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Load a domain from the specified directory path.
     *
     * @param domainPath Path to domain directory (e.g., "domains/healthcare")
     * @param llmProvider LLM provider for agent execution
     * @return Number of agents loaded
     * @throws IOException if domain configuration cannot be read
     */
    public int loadDomain(final String domainPath, final LLMProvider llmProvider)
            throws IOException {
        log.info("Loading domain from: {}", domainPath);

        // Load domain configuration
        DomainConfig domainConfig = loadDomainConfig(domainPath);
        domainConfig.validate();

        if (!domainConfig.isEnabled()) {
            log.info("Domain '{}' is disabled, skipping load", domainConfig.getName());
            return 0;
        }

        log.info(
                "Loading domain: {} v{} - {}",
                domainConfig.getName(),
                domainConfig.getVersion(),
                domainConfig.getDescription());

        // Register domain-specific output formats (if any)
        registerOutputFormats(domainConfig);

        // Load all agent configurations from domain
        String agentDirectoryPath = domainPath + "/" + domainConfig.getAgentDirectory();
        List<AgentConfig> agentConfigs = loadAgentConfigs(agentDirectoryPath);

        if (agentConfigs.isEmpty()) {
            log.warn("No agents found in domain: {}", domainConfig.getName());
            return 0;
        }

        // Create and register agents
        int loadedCount = 0;
        for (AgentConfig agentConfig : agentConfigs) {
            try {
                Agent agent = new ConfigurableAgent(agentConfig, llmProvider, formatterRegistry);
                agentRegistry.registerAgent(agent);
                loadedCount++;
                log.debug(
                        "Loaded agent: {} from domain: {}",
                        agentConfig.name(),
                        domainConfig.getName());
            } catch (Exception e) {
                log.error(
                        "Failed to create agent '{}' in domain '{}': {}",
                        agentConfig.name(),
                        domainConfig.getName(),
                        e.getMessage(),
                        e);
            }
        }

        log.info(
                "Domain '{}' loaded successfully: {} agents registered",
                domainConfig.getName(),
                loadedCount);

        return loadedCount;
    }

    /**
     * Load domain configuration from domain.yaml file.
     *
     * @param domainPath Path to domain directory
     * @return Domain configuration
     * @throws IOException if file cannot be read or parsed
     */
    private DomainConfig loadDomainConfig(final String domainPath) throws IOException {
        Path configPath = Paths.get(domainPath, "domain.yaml");
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            throw new IOException("Domain configuration not found: " + configPath);
        }

        log.debug("Loading domain config from: {}", configPath);
        return yamlMapper.readValue(configFile, DomainConfig.class);
    }

    /**
     * Load all agent configurations from the agents directory.
     *
     * @param agentDirectoryPath Path to agents directory
     * @return List of agent configurations
     * @throws IOException if directory cannot be read
     */
    private List<AgentConfig> loadAgentConfigs(final String agentDirectoryPath) throws IOException {
        Path agentDir = Paths.get(agentDirectoryPath);

        if (!Files.exists(agentDir)) {
            log.warn("Agent directory not found: {}", agentDirectoryPath);
            return List.of();
        }

        if (!Files.isDirectory(agentDir)) {
            throw new IOException("Not a directory: " + agentDirectoryPath);
        }

        List<AgentConfig> configs = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(agentDir, 1)) {
            paths.filter(Files::isRegularFile)
                    .filter(
                            path ->
                                    path.toString().endsWith(".yaml")
                                            || path.toString().endsWith(".yml"))
                    .forEach(
                            path -> {
                                try {
                                    AgentConfig config =
                                            agentConfigLoader.loadConfig(path.toFile());
                                    configs.add(config);
                                    log.debug(
                                            "Loaded agent config: {} from {}",
                                            config.name(),
                                            path.getFileName());
                                } catch (IOException e) {
                                    log.error(
                                            "Failed to load agent config from {}: {}",
                                            path,
                                            e.getMessage(),
                                            e);
                                }
                            });
        }

        return configs;
    }

    /**
     * Register domain-specific output formats.
     *
     * <p>Currently logs format names. Future versions will support loading custom formatter
     * implementations.
     *
     * @param domainConfig Domain configuration
     */
    private void registerOutputFormats(final DomainConfig domainConfig) {
        List<String> formats = domainConfig.getOutputFormats();
        if (formats == null || formats.isEmpty()) {
            log.debug("No output formats defined for domain: {}", domainConfig.getName());
            return;
        }

        log.info(
                "Domain '{}' defines output formats: {}",
                domainConfig.getName(),
                String.join(", ", formats));

        // TODO: In Phase 3, load and register custom formatter implementations
        // For now, formats must be registered manually or use built-ins
        for (String formatName : formats) {
            if (!formatterRegistry.hasFormat(formatName)) {
                log.warn(
                        "Output format '{}' not registered. " + "Using raw format as fallback.",
                        formatName);
            }
        }
    }

    /**
     * Discover and load all domains from the domains/ directory.
     *
     * @param domainsBasePath Base path to domains directory (e.g., "domains")
     * @param llmProvider LLM provider for agent execution
     * @return Total number of agents loaded across all domains
     */
    public int loadAllDomains(final String domainsBasePath, final LLMProvider llmProvider) {
        log.info("Discovering domains in: {}", domainsBasePath);

        Path domainsPath = Paths.get(domainsBasePath);
        if (!Files.exists(domainsPath)) {
            log.warn("Domains directory not found: {}", domainsBasePath);
            return 0;
        }

        int totalLoaded = 0;

        try (Stream<Path> paths = Files.walk(domainsPath, 1)) {
            List<Path> domainDirs =
                    paths.filter(Files::isDirectory)
                            .filter(path -> !path.equals(domainsPath)) // Exclude base dir
                            .toList();

            log.info("Found {} potential domain(s) in: {}", domainDirs.size(), domainsBasePath);

            for (Path domainDir : domainDirs) {
                try {
                    int loaded = loadDomain(domainDir.toString(), llmProvider);
                    totalLoaded += loaded;
                } catch (Exception e) {
                    log.error("Failed to load domain from {}: {}", domainDir, e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            log.error("Error discovering domains: {}", e.getMessage(), e);
        }

        log.info("Domain loading complete: {} total agents across all domains", totalLoaded);

        return totalLoaded;
    }
}
