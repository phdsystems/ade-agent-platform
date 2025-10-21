package dev.adeengineer.platform.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.LLMProviderFactory;
import dev.adeengineer.platform.core.DomainManager;
import dev.adeengineer.platform.core.DomainManager.DomainMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * REST API controller for domain management operations.
 *
 * <p>Provides endpoints for:
 *
 * <ul>
 *   <li>Listing all loaded domains
 *   <li>Getting domain details
 *   <li>Loading new domains
 *   <li>Reloading existing domains
 *   <li>Unloading domains
 *   <li>Discovering available domains
 * </ul>
 *
 * @since 0.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainManager domainManager;
    private final LLMProviderFactory llmProviderFactory;

    /**
     * Constructs a new DomainController.
     *
     * @param domainManager Domain manager service
     * @param llmProviderFactory LLM provider factory
     */
    public DomainController(
            final DomainManager domainManager, final LLMProviderFactory llmProviderFactory) {
        this.domainManager = domainManager;
        this.llmProviderFactory = llmProviderFactory;
    }

    /**
     * List all loaded domains.
     *
     * <p>GET /api/domains
     *
     * @return Set of domain names
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listDomains() {
        log.debug("GET /api/domains - List all domains");

        Set<String> domains = domainManager.getLoadedDomains();

        Map<String, Object> response = new HashMap<>();
        response.put("domains", domains);
        response.put("count", domains.size());
        response.put("totalAgents", domainManager.getTotalAgentCount());

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed information about all domains.
     *
     * <p>GET /api/domains/info
     *
     * @return Map of domain name to metadata
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, DomainInfo>> getAllDomainsInfo() {
        log.debug("GET /api/domains/info - Get all domain information");

        Map<String, DomainMetadata> metadata = domainManager.getAllDomainMetadata();

        Map<String, DomainInfo> info = new HashMap<>();
        metadata.forEach((name, meta) -> info.put(name, toDomainInfo(name, meta)));

        return ResponseEntity.ok(info);
    }

    /**
     * Get information about a specific domain.
     *
     * <p>GET /api/domains/{domainName}
     *
     * @param domainName Domain name
     * @return Domain information
     */
    @GetMapping("/{domainName}")
    public ResponseEntity<DomainInfo> getDomainInfo(@PathVariable final String domainName) {
        log.debug("GET /api/domains/{} - Get domain info", domainName);

        DomainMetadata metadata = domainManager.getDomainMetadata(domainName);
        if (metadata == null) {
            log.warn("Domain not found: {}", domainName);
            return ResponseEntity.notFound().build();
        }

        DomainInfo info = toDomainInfo(domainName, metadata);
        return ResponseEntity.ok(info);
    }

    /**
     * Load a domain from a specified path.
     *
     * <p>POST /api/domains/load
     *
     * <p>Request body:
     *
     * <pre>
     * {
     *   "domainPath": "domains/my-domain"
     * }
     * </pre>
     *
     * @param request Load request
     * @return Load result
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> loadDomain(
            @RequestBody final LoadDomainRequest request) {
        log.info("POST /api/domains/load - Load domain from: {}", request.domainPath());

        try {
            LLMProvider llmProvider = llmProviderFactory.getProviderWithFailover();

            int agentCount = domainManager.loadDomain(request.domainPath(), llmProvider);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domainPath", request.domainPath());
            response.put("agentsLoaded", agentCount);
            response.put("message", "Domain loaded successfully: " + agentCount + " agents");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to load domain from {}: {}", request.domainPath(), e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("domainPath", request.domainPath());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Reload an existing domain.
     *
     * <p>POST /api/domains/{domainName}/reload
     *
     * @param domainName Domain name to reload
     * @return Reload result
     */
    @PostMapping("/{domainName}/reload")
    public ResponseEntity<Map<String, Object>> reloadDomain(@PathVariable final String domainName) {
        log.info("POST /api/domains/{}/reload - Reload domain", domainName);

        try {
            LLMProvider llmProvider = llmProviderFactory.getProviderWithFailover();

            int agentCount = domainManager.reloadDomain(domainName, llmProvider);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domainName", domainName);
            response.put("agentsLoaded", agentCount);
            response.put("message", "Domain reloaded successfully: " + agentCount + " agents");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Domain not found: {}", domainName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Domain not found: " + domainName);

            return ResponseEntity.notFound().build();

        } catch (IOException e) {
            log.error("Failed to reload domain {}: {}", domainName, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("domainName", domainName);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Unload a domain.
     *
     * <p>DELETE /api/domains/{domainName}
     *
     * @param domainName Domain name to unload
     * @return Unload result
     */
    @DeleteMapping("/{domainName}")
    public ResponseEntity<Map<String, Object>> unloadDomain(@PathVariable final String domainName) {
        log.info("DELETE /api/domains/{} - Unload domain", domainName);

        try {
            int agentsRemoved = domainManager.unloadDomain(domainName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domainName", domainName);
            response.put("agentsRemoved", agentsRemoved);
            response.put("message", "Domain unloaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Domain not found: {}", domainName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Domain not found: " + domainName);

            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Discover available domains in the domains directory.
     *
     * <p>GET /api/domains/discover
     *
     * @return List of discovered domain paths
     */
    @GetMapping("/discover")
    public ResponseEntity<Map<String, Object>> discoverDomains() {
        log.debug("GET /api/domains/discover - Discover available domains");

        try {
            // Use default domains path
            List<String> discovered = domainManager.discoverDomains("domains");

            Map<String, Object> response = new HashMap<>();
            response.put("domains", discovered);
            response.put("count", discovered.size());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to discover domains: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get health status for a domain.
     *
     * <p>GET /api/domains/{domainName}/health
     *
     * @param domainName Domain name
     * @return Health status
     */
    @GetMapping("/{domainName}/health")
    public ResponseEntity<Map<String, String>> getDomainHealth(
            @PathVariable final String domainName) {
        log.debug("GET /api/domains/{}/health - Get health", domainName);

        String health = domainManager.getDomainHealth(domainName);

        Map<String, String> response = new HashMap<>();
        response.put("domainName", domainName);
        response.put("health", health);

        return ResponseEntity.ok(response);
    }

    // ========== Helper Methods and DTOs ==========

    /** Convert DomainMetadata to DomainInfo DTO. */
    private DomainInfo toDomainInfo(final String domainName, final DomainMetadata metadata) {
        return new DomainInfo(
                metadata.config().getName(),
                metadata.config().getVersion(),
                metadata.config().getDescription(),
                metadata.agentCount(),
                metadata.config().getOutputFormats(),
                metadata.enabled(),
                domainManager.getDomainHealth(domainName),
                metadata.path(),
                metadata.loadedTimestamp());
    }

    /**
     * Domain information DTO for API responses.
     *
     * @param name Domain name
     * @param version Domain version
     * @param description Domain description
     * @param agentCount Number of agents in the domain
     * @param outputFormats Available output formats
     * @param enabled Whether the domain is enabled
     * @param health Health status of the domain
     * @param path File path to the domain
     * @param loadedTimestamp Timestamp when domain was loaded
     */
    public record DomainInfo(
            String name,
            String version,
            String description,
            int agentCount,
            List<String> outputFormats,
            boolean enabled,
            String health,
            String path,
            long loadedTimestamp) {}

    /**
     * Request body for loading a domain.
     *
     * @param domainPath Path to the domain directory
     */
    public record LoadDomainRequest(String domainPath) {}
}
