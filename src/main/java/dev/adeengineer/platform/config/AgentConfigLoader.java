package dev.adeengineer.platform.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import dev.adeengineer.agent.AgentConfig;

import lombok.extern.slf4j.Slf4j;

/** Loads agent configurations from YAML files using agent-sdk AgentConfig. */
@Slf4j
@Component
public class AgentConfigLoader {

    /** Directory containing agent configuration YAML files. */
    private final String configDir;

    /** Jackson ObjectMapper configured for YAML parsing. */
    private final ObjectMapper yamlMapper;

    /**
     * Constructs a new AgentConfigLoader.
     *
     * @param directory Directory containing agent configuration files
     */
    public AgentConfigLoader(@Value("${agents.config-dir:config/agents}") final String directory) {
        this.configDir = directory;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        log.info("Initialized AgentConfigLoader " + "with config directory: {}", directory);
    }

    /**
     * Load all agent configurations from YAML files in the config directory.
     *
     * @return List of all loaded agent configurations
     */
    public List<AgentConfig> loadAllConfigs() {
        List<AgentConfig> configs = new ArrayList<>();

        File configDirFile = new File(configDir);
        if (!configDirFile.exists() || !configDirFile.isDirectory()) {
            log.warn("Agent config directory does not exist: {}", configDir);
            return configs;
        }

        File[] yamlFiles =
                configDirFile.listFiles(
                        (dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            log.warn("No YAML configuration files found in: {}", configDir);
            return configs;
        }

        for (File yamlFile : yamlFiles) {
            try {
                AgentConfig config = loadConfig(yamlFile);
                configs.add(config);
                log.info("Loaded agent config: {} from {}", config.name(), yamlFile.getName());
            } catch (Exception e) {
                log.error(
                        "Failed to load config from {}: {}", yamlFile.getName(), e.getMessage(), e);
            }
        }

        log.info("Loaded {} agent configurations", configs.size());
        return configs;
    }

    /**
     * Load a single agent configuration from a YAML file.
     *
     * @param yamlFile YAML file to load
     * @return Loaded agent configuration
     * @throws IOException if file read fails
     */
    public AgentConfig loadConfig(final File yamlFile) throws IOException {
        return yamlMapper.readValue(yamlFile, AgentConfig.class);
    }

    /**
     * Load a single agent configuration by agent name.
     *
     * @param agentName Agent name to search for
     * @return Agent configuration for the specified agent
     * @throws IOException if file read fails
     * @throws IllegalArgumentException if agent not found
     */
    public AgentConfig loadConfigByName(final String agentName) throws IOException {
        File configDirFile = new File(configDir);
        File[] yamlFiles =
                configDirFile.listFiles(
                        (dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));

        if (yamlFiles != null) {
            for (File yamlFile : yamlFiles) {
                AgentConfig config = loadConfig(yamlFile);
                if (config.name().equals(agentName)) {
                    return config;
                }
            }
        }

        throw new IllegalArgumentException("No configuration found for agent: " + agentName);
    }
}
