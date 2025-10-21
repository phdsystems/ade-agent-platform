package dev.adeengineer.platform.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import adeengineer.dev.agent.OutputFormatterRegistry;

/**
 * Spring configuration for output formatters.
 *
 * <p>Provides OutputFormatterRegistry from ade-agent SDK as a Spring bean.
 */
@Configuration
public class FormatterConfiguration {

    /**
     * Creates OutputFormatterRegistry bean.
     *
     * <p>The registry is auto-initialized with built-in formatters (technical, business, executive,
     * raw) via its constructor.
     *
     * @return OutputFormatterRegistry instance
     */
    @Bean
    public OutputFormatterRegistry outputFormatterRegistry() {
        return new OutputFormatterRegistry();
    }
}
