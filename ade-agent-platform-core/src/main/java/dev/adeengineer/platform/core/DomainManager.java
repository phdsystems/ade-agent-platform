package dev.adeengineer.platform.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;


import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.model.DomainConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing domain plugins.
 *
 * <p>Provides high-level domain management operations including:
 *
 * <ul>
 *   <li>Domain discovery and loading
 *   <li>Domain lifecycle management (load, unload, reload)
 *   <li>Domain metadata tracking
 *   <li>Domain status and health monitoring
 * </ul>
 *
 * @since 0.2.0
 */
@Slf4j
public class DomainManager {

    private final DomainLoader domainLoader;
    private final AgentRegistry agentRegistry;
    private final Map<String, DomainMetadata> loadedDomains;

    /**
     * Metadata for a loaded domain.
     *
     * @param config Domain configuration
     * @param path File path to the domain
     * @param agentCount Number of agents loaded
     * @param loadedTimestamp Timestamp when domain was loaded
     * @param enabled Whether the domain is enabled
     */
    public record DomainMetadata(
            DomainConfig config,
            String path,
            int agentCount,
            long loadedTimestamp,
            boolean enabled) {
        public String displayInfo() {
            return String.format(
                    "%s v%s (%d agents)", config.getName(), config.getVersion(), agentCount);
        }
    }

    /**
     * Constructs a new DomainManager.
     *
     * @param domainLoader Domain loader for loading domains
     * @param agentRegistry Agent registry for tracking agents
     */
    public DomainManager(final DomainLoader domainLoader, final AgentRegistry agentRegistry) {
        this.domainLoader = domainLoader;
        this.agentRegistry = agentRegistry;
        this.loadedDomains = new ConcurrentHashMap<>();
    }

    /**
     * Load a domain from the specified path.
     *
     * @param domainPath Path to domain directory
     * @param llmProvider LLM provider for agents
     * @return Number of agents loaded
     * @throws IOException if domain cannot be loaded
     */
    public int loadDomain(final String domainPath, final LLMProvider llmProvider)
            throws IOException {
        log.info("Loading domain from: {}", domainPath);

        // Load domain
        int agentCount = domainLoader.loadDomain(domainPath, llmProvider);

        // Track loaded domain
        Path path = Paths.get(domainPath);
        DomainConfig config = loadDomainConfig(domainPath);

        DomainMetadata metadata =
                new DomainMetadata(
                        config,
                        domainPath,
                        agentCount,
                        System.currentTimeMillis(),
                        config.isEnabled());

        loadedDomains.put(config.getName(), metadata);

        log.info("Domain '{}' loaded: {}", config.getName(), metadata.displayInfo());

        return agentCount;
    }

    /**
     * Reload a domain (unload and load again).
     *
     * @param domainName Name of domain to reload
     * @param llmProvider LLM provider for agents
     * @return Number of agents loaded
     * @throws IOException if domain cannot be reloaded
     * @throws IllegalArgumentException if domain not found
     */
    public int reloadDomain(final String domainName, final LLMProvider llmProvider)
            throws IOException {
        log.info("Reloading domain: {}", domainName);

        DomainMetadata metadata = loadedDomains.get(domainName);
        if (metadata == null) {
            throw new IllegalArgumentException("Domain not found: " + domainName);
        }

        // Unload domain (remove its agents)
        unloadDomain(domainName);

        // Reload domain
        return loadDomain(metadata.path(), llmProvider);
    }

    /**
     * Unload a domain (remove its agents from registry).
     *
     * @param domainName Name of domain to unload
     * @return Number of agents removed
     * @throws IllegalArgumentException if domain not found
     */
    public int unloadDomain(final String domainName) {
        log.info("Unloading domain: {}", domainName);

        DomainMetadata metadata = loadedDomains.get(domainName);
        if (metadata == null) {
            throw new IllegalArgumentException("Domain not found: " + domainName);
        }

        // Remove all agents from this domain
        // Note: AgentRegistry doesn't track domain ownership yet,
        // so this is a simplified implementation
        int removedCount = 0;

        // TODO: Track agent-domain mapping in AgentRegistry
        // For now, just log the action
        log.warn("Domain unload not fully implemented. " + "Agent removal requires restart.");

        loadedDomains.remove(domainName);

        log.info("Domain '{}' unloaded (marked for removal)", domainName);
        return removedCount;
    }

    /**
     * Discover available domains in a directory.
     *
     * @param domainsPath Base path to domains directory
     * @return List of discovered domain paths
     * @throws IOException if directory cannot be read
     */
    public List<String> discoverDomains(final String domainsPath) throws IOException {
        log.info("Discovering domains in: {}", domainsPath);

        Path basePath = Paths.get(domainsPath);
        if (!Files.exists(basePath)) {
            log.warn("Domains directory not found: {}", domainsPath);
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(basePath, 1)) {
            List<String> domains =
                    paths.filter(Files::isDirectory)
                            .filter(path -> !path.equals(basePath))
                            .filter(path -> Files.exists(path.resolve("domain.yaml")))
                            .map(Path::toString)
                            .toList();

            log.info("Discovered {} domain(s)", domains.size());
            return domains;
        }
    }

    /**
     * Get all loaded domains.
     *
     * @return Set of domain names
     */
    public Set<String> getLoadedDomains() {
        return Set.copyOf(loadedDomains.keySet());
    }

    /**
     * Get metadata for a specific domain.
     *
     * @param domainName Domain name
     * @return Domain metadata, or null if not found
     */
    public DomainMetadata getDomainMetadata(final String domainName) {
        return loadedDomains.get(domainName);
    }

    /**
     * Get all domain metadata.
     *
     * @return Map of domain name to metadata
     */
    public Map<String, DomainMetadata> getAllDomainMetadata() {
        return Map.copyOf(loadedDomains);
    }

    /**
     * Check if a domain is loaded.
     *
     * @param domainName Domain name
     * @return true if loaded, false otherwise
     */
    public boolean isDomainLoaded(final String domainName) {
        return loadedDomains.containsKey(domainName);
    }

    /**
     * Get total number of loaded domains.
     *
     * @return Domain count
     */
    public int getDomainCount() {
        return loadedDomains.size();
    }

    /**
     * Get total number of agents across all domains.
     *
     * @return Total agent count
     */
    public int getTotalAgentCount() {
        return loadedDomains.values().stream().mapToInt(DomainMetadata::agentCount).sum();
    }

    /**
     * Get domain health status.
     *
     * @param domainName Domain name
     * @return Health status string
     */
    public String getDomainHealth(final String domainName) {
        DomainMetadata metadata = loadedDomains.get(domainName);
        if (metadata == null) {
            return "NOT_LOADED";
        }

        if (!metadata.enabled()) {
            return "DISABLED";
        }

        if (metadata.agentCount() == 0) {
            return "NO_AGENTS";
        }

        return "HEALTHY";
    }

    /**
     * Load domain configuration from domain.yaml.
     *
     * @param domainPath Path to domain directory
     * @return Domain configuration
     * @throws IOException if config cannot be loaded
     */
    private DomainConfig loadDomainConfig(final String domainPath) throws IOException {
        // Delegate to DomainLoader's private method via reflection
        // or implement directly here
        Path configPath = Paths.get(domainPath, "domain.yaml");

        com.fasterxml.jackson.databind.ObjectMapper yamlMapper =
                new com.fasterxml.jackson.databind.ObjectMapper(
                        new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());

        return yamlMapper.readValue(configPath.toFile(), DomainConfig.class);
    }
}
