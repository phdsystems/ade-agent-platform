package dev.adeengineer.platform.cli;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.factory.LLMProviderFactory;
import dev.adeengineer.platform.core.DomainManager;
import dev.adeengineer.platform.core.DomainManager.DomainMetadata;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Shell CLI commands for domain management operations.
 *
 * <p>Provides commands for:
 *
 * <ul>
 *   <li>Listing loaded domains
 *   <li>Loading and reloading domains
 *   <li>Unloading domains
 *   <li>Discovering available domains
 *   <li>Viewing domain details and health
 * </ul>
 *
 * @since 0.2.0
 */
@Slf4j
@ShellComponent
public class DomainCommands {

    /** Separator line width for formatted output. */
    private static final int SEPARATOR_WIDTH = 60;

    /** Short separator line width for list output. */
    private static final int SHORT_SEPARATOR_WIDTH = 40;

    /** Date formatter for displaying timestamps. */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final DomainManager domainManager;
    private final LLMProviderFactory llmProviderFactory;

    /**
     * Constructs a new DomainCommands.
     *
     * @param domainManager Domain manager service
     * @param llmProviderFactory LLM provider factory
     */
    public DomainCommands(
            final DomainManager domainManager, final LLMProviderFactory llmProviderFactory) {
        this.domainManager = domainManager;
        this.llmProviderFactory = llmProviderFactory;
    }

    /**
     * List all loaded domains.
     *
     * <p>Usage: list-domains
     *
     * @return Formatted list of loaded domains
     */
    @ShellMethod(key = "list-domains", value = "List all loaded domain plugins")
    public String listDomains() {
        log.info("CLI: Listing all domains");

        Set<String> domains = domainManager.getLoadedDomains();

        if (domains.isEmpty()) {
            return "No domains loaded.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Loaded Domains (").append(domains.size()).append("):\n");
        sb.append("=".repeat(SHORT_SEPARATOR_WIDTH)).append("\n");

        for (String domain : domains) {
            DomainMetadata metadata = domainManager.getDomainMetadata(domain);
            if (metadata != null) {
                sb.append("  - ")
                        .append(domain)
                        .append(" (v")
                        .append(metadata.config().getVersion())
                        .append(", ")
                        .append(metadata.agentCount())
                        .append(" agents)\n");
            } else {
                sb.append("  - ").append(domain).append("\n");
            }
        }

        sb.append("\nTotal agents: ").append(domainManager.getTotalAgentCount()).append("\n");

        return sb.toString();
    }

    /**
     * Show detailed information about a specific domain.
     *
     * <p>Usage: show-domain "healthcare"
     *
     * @param domainName Domain name to show
     * @return Formatted domain information
     */
    @ShellMethod(key = "show-domain", value = "Show detailed information about a domain")
    public String showDomain(@ShellOption(help = "Domain name") final String domainName) {
        log.info("CLI: Showing domain: {}", domainName);

        DomainMetadata metadata = domainManager.getDomainMetadata(domainName);
        if (metadata == null) {
            return "Error: Domain not found: " + domainName;
        }

        return formatDomainInfo(domainName, metadata);
    }

    /**
     * Show detailed information about all domains.
     *
     * <p>Usage: show-domains
     *
     * @return Formatted list of all domains with details
     */
    @ShellMethod(key = "show-domains", value = "Show detailed information about all domains")
    public String showDomains() {
        log.info("CLI: Showing all domains with details");

        Map<String, DomainMetadata> allMetadata = domainManager.getAllDomainMetadata();

        if (allMetadata.isEmpty()) {
            return "No domains loaded.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Loaded Domains (").append(allMetadata.size()).append("):\n");
        sb.append("=".repeat(SEPARATOR_WIDTH)).append("\n\n");

        for (Map.Entry<String, DomainMetadata> entry : allMetadata.entrySet()) {
            sb.append(formatDomainInfo(entry.getKey(), entry.getValue()));
            sb.append("\n").append("-".repeat(SEPARATOR_WIDTH)).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Load a domain from a specified path.
     *
     * <p>Usage: load-domain --path "domains/my-domain"
     *
     * @param domainPath Path to domain directory
     * @return Load result message
     */
    @ShellMethod(key = "load-domain", value = "Load a domain plugin from a directory")
    public String loadDomain(
            @ShellOption(help = "Path to domain directory") final String domainPath) {
        log.info("CLI: Loading domain from: {}", domainPath);

        try {
            LLMProvider llmProvider = llmProviderFactory.getProviderWithFailover();

            int agentCount = domainManager.loadDomain(domainPath, llmProvider);

            return String.format(
                    "✓ Domain loaded successfully: %d agents\n" + "Path: %s",
                    agentCount, domainPath);

        } catch (IOException e) {
            log.error("Failed to load domain from {}: {}", domainPath, e.getMessage(), e);
            return "Error: Failed to load domain: " + e.getMessage();
        }
    }

    /**
     * Reload an existing domain.
     *
     * <p>Usage: reload-domain "healthcare"
     *
     * @param domainName Domain name to reload
     * @return Reload result message
     */
    @ShellMethod(key = "reload-domain", value = "Reload an existing domain plugin")
    public String reloadDomain(
            @ShellOption(help = "Domain name to reload") final String domainName) {
        log.info("CLI: Reloading domain: {}", domainName);

        try {
            LLMProvider llmProvider = llmProviderFactory.getProviderWithFailover();

            int agentCount = domainManager.reloadDomain(domainName, llmProvider);

            return String.format(
                    "✓ Domain reloaded successfully: %d agents\n" + "Domain: %s",
                    agentCount, domainName);

        } catch (IllegalArgumentException e) {
            log.error("Domain not found: {}", domainName);
            return "Error: Domain not found: " + domainName;

        } catch (IOException e) {
            log.error("Failed to reload domain {}: {}", domainName, e.getMessage(), e);
            return "Error: Failed to reload domain: " + e.getMessage();
        }
    }

    /**
     * Unload a domain.
     *
     * <p>Usage: unload-domain "healthcare"
     *
     * @param domainName Domain name to unload
     * @return Unload result message
     */
    @ShellMethod(key = "unload-domain", value = "Unload a domain plugin")
    public String unloadDomain(
            @ShellOption(help = "Domain name to unload") final String domainName) {
        log.info("CLI: Unloading domain: {}", domainName);

        try {
            int agentsRemoved = domainManager.unloadDomain(domainName);

            return String.format(
                    "✓ Domain unloaded successfully\n" + "Domain: %s\n" + "Agents removed: %d",
                    domainName, agentsRemoved);

        } catch (IllegalArgumentException e) {
            log.error("Domain not found: {}", domainName);
            return "Error: Domain not found: " + domainName;
        }
    }

    /**
     * Discover available domains in the domains directory.
     *
     * <p>Usage: discover-domains
     *
     * <p>Usage: discover-domains --path "custom/path"
     *
     * @param domainsPath Optional custom path to domains directory
     * @return List of discovered domain paths
     */
    @ShellMethod(key = "discover-domains", value = "Discover available domain plugins")
    public String discoverDomains(
            @ShellOption(defaultValue = "domains", help = "Path to domains directory")
                    final String domainsPath) {
        log.info("CLI: Discovering domains in: {}", domainsPath);

        try {
            List<String> discovered = domainManager.discoverDomains(domainsPath);

            if (discovered.isEmpty()) {
                return String.format("No domains found in: %s", domainsPath);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Discovered Domains (").append(discovered.size()).append("):\n");
            sb.append("=".repeat(SHORT_SEPARATOR_WIDTH)).append("\n");

            for (String domainPath : discovered) {
                // Check if already loaded
                String domainName = extractDomainName(domainPath);
                boolean loaded = domainManager.isDomainLoaded(domainName);

                sb.append("  - ").append(domainPath);
                if (loaded) {
                    sb.append(" [LOADED]");
                }
                sb.append("\n");
            }

            sb.append("\nTo load a domain: load-domain --path <path>\n");

            return sb.toString();

        } catch (IOException e) {
            log.error("Failed to discover domains: {}", e.getMessage(), e);
            return "Error: Failed to discover domains: " + e.getMessage();
        }
    }

    /**
     * Get health status for a domain.
     *
     * <p>Usage: domain-health "healthcare"
     *
     * @param domainName Domain name
     * @return Health status
     */
    @ShellMethod(key = "domain-health", value = "Check health status of a domain")
    public String domainHealth(@ShellOption(help = "Domain name") final String domainName) {
        log.info("CLI: Checking health for domain: {}", domainName);

        String health = domainManager.getDomainHealth(domainName);

        StringBuilder sb = new StringBuilder();
        sb.append("Domain: ").append(domainName).append("\n");
        sb.append("Health: ").append(health);

        // Add interpretation
        switch (health) {
            case "HEALTHY":
                sb.append(" ✓");
                break;
            case "NOT_LOADED":
                sb.append(" (not loaded)");
                break;
            case "DISABLED":
                sb.append(" (disabled in config)");
                break;
            case "NO_AGENTS":
                sb.append(" ⚠ (no agents loaded)");
                break;
            default:
                sb.append(" ?");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Format domain information for display.
     *
     * @param domainName Domain name
     * @param metadata Domain metadata
     * @return Formatted domain details
     */
    private String formatDomainInfo(final String domainName, final DomainMetadata metadata) {
        StringBuilder sb = new StringBuilder();

        sb.append("Domain: ").append(domainName).append("\n");
        sb.append("Version: ").append(metadata.config().getVersion()).append("\n");
        sb.append("Description: ").append(metadata.config().getDescription()).append("\n");
        sb.append("Agents: ").append(metadata.agentCount()).append("\n");
        sb.append("Output Formats: ")
                .append(String.join(", ", metadata.config().getOutputFormats()))
                .append("\n");
        sb.append("Enabled: ").append(metadata.enabled() ? "Yes" : "No").append("\n");
        sb.append("Health: ").append(domainManager.getDomainHealth(domainName)).append("\n");
        sb.append("Path: ").append(metadata.path()).append("\n");

        String loadedTime = DATE_FORMATTER.format(Instant.ofEpochMilli(metadata.loadedTimestamp()));
        sb.append("Loaded: ").append(loadedTime).append("\n");

        return sb.toString();
    }

    /**
     * Extract domain name from domain path.
     *
     * @param domainPath Full path to domain
     * @return Domain name (last path component)
     */
    private String extractDomainName(final String domainPath) {
        String[] parts = domainPath.split("/");
        return parts[parts.length - 1];
    }
}
