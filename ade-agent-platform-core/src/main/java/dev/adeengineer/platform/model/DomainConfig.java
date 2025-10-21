package dev.adeengineer.platform.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Configuration for a domain plugin.
 *
 * <p>Defines metadata and settings for a complete domain (e.g., software engineering, healthcare,
 * legal, finance). Each domain contains multiple agents and defines its own output formats.
 *
 * <h3>Example domain.yaml</h3>
 *
 * <pre>
 * name: "healthcare"
 * version: "1.0.0"
 * description: "Healthcare domain with diagnostic and treatment agents"
 * outputFormats:
 *   - clinical
 *   - patient-friendly
 *   - administrative
 * agentDirectory: "agents/"
 * dependencies: []
 * </pre>
 *
 * @since 0.2.0
 */
@Data
@Builder
@Jacksonized
public class DomainConfig {

    /**
     * Unique domain name (e.g., "healthcare", "legal", "finance").
     *
     * <p>Used for identification and namespacing.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Domain version (semantic versioning recommended).
     *
     * <p>Example: "1.0.0", "2.1.3-beta"
     */
    @JsonProperty("version")
    private String version;

    /** Human-readable description of the domain. */
    @JsonProperty("description")
    private String description;

    /**
     * List of output format names provided by this domain.
     *
     * <p>These formats will be registered when the domain loads.
     *
     * <p>Examples for healthcare: ["clinical", "patient-friendly", "administrative"]
     *
     * <p>Examples for legal: ["legal-memo", "client-summary", "regulatory-report"]
     */
    @JsonProperty("outputFormats")
    @Builder.Default
    private List<String> outputFormats = new ArrayList<>();

    /**
     * Relative directory path containing agent YAML files.
     *
     * <p>Default: "agents/"
     */
    @JsonProperty("agentDirectory")
    @Builder.Default
    private String agentDirectory = "agents/";

    /**
     * List of domain dependencies (for future use).
     *
     * <p>Allows domains to build on other domains. Currently unused.
     */
    @JsonProperty("dependencies")
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();

    /**
     * Domain metadata (extensible key-value map).
     *
     * <p>For domain-specific configuration that doesn't fit standard fields.
     */
    @JsonProperty("metadata")
    private java.util.Map<String, Object> metadata;

    /**
     * Whether this domain is enabled.
     *
     * <p>Default: true
     */
    @JsonProperty("enabled")
    @Builder.Default
    private boolean enabled = true;

    /**
     * Validate the domain configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Domain name cannot be null or blank");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Domain version cannot be null or blank");
        }
        if (agentDirectory == null || agentDirectory.isBlank()) {
            throw new IllegalArgumentException("Agent directory cannot be null or blank");
        }
    }

    /**
     * Get a display name for this domain.
     *
     * @return Domain name and version (e.g., "healthcare v1.0.0")
     */
    public String getDisplayName() {
        return name + " v" + version;
    }
}
