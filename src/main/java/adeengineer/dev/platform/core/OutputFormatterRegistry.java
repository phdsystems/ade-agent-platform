package adeengineer.dev.platform.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.platform.formatters.BusinessOutputFormatter;
import adeengineer.dev.platform.formatters.ExecutiveOutputFormatter;
import adeengineer.dev.platform.formatters.RawOutputFormatter;
import adeengineer.dev.platform.formatters.TechnicalOutputFormatter;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry for output format strategies.
 *
 * <p>Manages registration and lookup of output formatters for different audiences and use cases.
 * Supports built-in formats (technical, business, executive, raw) and custom domain-specific
 * formats.
 *
 * <p>Thread-safe implementation using ConcurrentHashMap.
 *
 * @see OutputFormatStrategy
 */
@Slf4j
@Component
public class OutputFormatterRegistry {

    /** Map of format names to formatter implementations. */
    private final Map<String, OutputFormatStrategy> formatters = new ConcurrentHashMap<>();

    /**
     * Register a new output format strategy.
     *
     * @param name The format name (converted to lowercase)
     * @param formatter The formatter implementation
     * @throws IllegalArgumentException if name or formatter is null
     */
    public void registerFormat(final String name, final OutputFormatStrategy formatter) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Format name cannot be null or blank");
        }
        if (formatter == null) {
            throw new IllegalArgumentException("Formatter cannot be null");
        }

        String normalizedName = name.toLowerCase().trim();
        formatters.put(normalizedName, formatter);
        log.info(
                "Registered output format: {} ({})",
                normalizedName,
                formatter.getClass().getSimpleName());
    }

    /**
     * Format an LLM response using the specified format type.
     *
     * @param response The LLM response to format
     * @param formatType The format type (case-insensitive)
     * @return Formatted output string
     */
    public String format(final LLMResponse response, final String formatType) {
        if (response == null) {
            log.warn("Received null response for formatting");
            return "";
        }

        if (formatType == null || formatType.isBlank()) {
            log.warn("No format type specified, using raw format");
            return response.content() != null ? response.content() : "";
        }

        String normalizedType = formatType.toLowerCase().trim();
        OutputFormatStrategy formatter = formatters.get(normalizedType);

        if (formatter == null) {
            log.warn(
                    "Unknown format type: {}. Using raw format. " + "Available formats: {}",
                    formatType,
                    getAvailableFormats());
            return response.content() != null ? response.content() : "";
        }

        try {
            return formatter.format(response);
        } catch (Exception e) {
            log.error("Error formatting response with {}: {}", formatType, e.getMessage(), e);
            // Fallback to raw content
            return response.content() != null ? response.content() : "";
        }
    }

    /**
     * Get a specific formatter by name.
     *
     * @param formatName The format name (case-insensitive)
     * @return The formatter, or null if not found
     */
    public OutputFormatStrategy getFormatter(final String formatName) {
        if (formatName == null || formatName.isBlank()) {
            return null;
        }
        return formatters.get(formatName.toLowerCase().trim());
    }

    /**
     * Check if a format is registered.
     *
     * @param formatName The format name (case-insensitive)
     * @return true if the format exists, false otherwise
     */
    public boolean hasFormat(final String formatName) {
        if (formatName == null || formatName.isBlank()) {
            return false;
        }
        return formatters.containsKey(formatName.toLowerCase().trim());
    }

    /**
     * Get the set of all registered format names.
     *
     * @return Set of format names
     */
    public Set<String> getAvailableFormats() {
        return Set.copyOf(formatters.keySet());
    }

    /**
     * Get the count of registered formatters.
     *
     * @return Number of formatters
     */
    public int getFormatterCount() {
        return formatters.size();
    }

    /**
     * Unregister a format (useful for testing or hot-reloading).
     *
     * @param formatName The format name to remove
     * @return true if format was removed, false if it didn't exist
     */
    public boolean unregisterFormat(final String formatName) {
        if (formatName == null || formatName.isBlank()) {
            return false;
        }
        String normalizedName = formatName.toLowerCase().trim();
        boolean removed = formatters.remove(normalizedName) != null;
        if (removed) {
            log.info("Unregistered output format: {}", normalizedName);
        }
        return removed;
    }

    /**
     * Initialize built-in formatters on application startup.
     *
     * <p>Registers the default formatters: technical, business, executive, and raw.
     */
    @PostConstruct
    public void registerBuiltInFormats() {
        log.info("Registering built-in output formatters...");

        registerFormat("technical", new TechnicalOutputFormatter());
        registerFormat("business", new BusinessOutputFormatter());
        registerFormat("executive", new ExecutiveOutputFormatter());
        registerFormat("raw", new RawOutputFormatter());

        log.info("Built-in formatters registered: {}", String.join(", ", getAvailableFormats()));
    }
}
