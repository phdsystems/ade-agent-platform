package dev.adeengineer.platform.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * Prompt template engine with variable substitution and conditional logic.
 *
 * <p>Supports Mustache-like syntax for dynamic prompt generation.
 */
@Slf4j
public class PromptTemplateEngine {

    /** Pattern for matching template variables like {{varName}}. */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    /** Pattern for matching conditional blocks. Format: {{#if condition}}...{{/if}}. */
    private static final Pattern CONDITIONAL_PATTERN =
            Pattern.compile("\\{\\{#if ([^}]+)}}(.*?)\\{\\{/if}}", Pattern.DOTALL);

    /** Pattern for matching loop blocks like {{#each items}}...{{/each}}. */
    private static final Pattern LOOP_PATTERN =
            Pattern.compile("\\{\\{#each ([^}]+)}}(.*?)\\{\\{/each}}", Pattern.DOTALL);

    /** Registered prompt templates by name. */
    private final Map<String, PromptTemplate> templates = new HashMap<>();

    /**
     * Register a prompt template.
     *
     * @param name Template name
     * @param template Template definition
     */
    public void registerTemplate(final String name, final PromptTemplate template) {
        log.debug("Registering template: {}", name);
        templates.put(name, template);
    }

    /**
     * Render a template with variables.
     *
     * <p>This method supports two modes: 1. If templateNameOrContent matches a registered template
     * name, that template is rendered 2. Otherwise, templateNameOrContent is treated as template
     * content and rendered directly
     *
     * @param templateNameOrContent Template name or template content string
     * @param variables Variable values
     * @return Rendered prompt
     */
    public String render(final String templateNameOrContent, final Map<String, Object> variables) {
        PromptTemplate template = templates.get(templateNameOrContent);
        if (template != null) {
            return renderContent(template.content(), variables);
        }

        // Treat as direct template content
        return renderContent(templateNameOrContent, variables);
    }

    /**
     * Render a template string with variables.
     *
     * @param templateContent Template content
     * @param variables Variable values
     * @return Rendered prompt
     */
    private String renderContent(
            final String templateContent, final Map<String, Object> variables) {
        String result = templateContent;

        // Process conditionals
        result = processConditionals(result, variables);

        // Process loops
        result = processLoops(result, variables);

        // Process simple variables
        result = processVariables(result, variables);

        return result.trim();
    }

    /**
     * Process variable substitutions.
     *
     * @param content Template content
     * @param variables Variable values
     * @return Content with variables substituted
     */
    private String processVariables(final String content, final Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = variables.get(varName);

            if (value != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            } else {
                log.warn("Variable not found: {}", varName);
                matcher.appendReplacement(result, "");
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Process conditional blocks.
     *
     * @param content Template content
     * @param variables Variable values
     * @return Content with conditionals processed
     */
    private String processConditionals(final String content, final Map<String, Object> variables) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String body = matcher.group(2);

            boolean conditionMet = evaluateCondition(condition, variables);

            if (conditionMet) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(body));
            } else {
                matcher.appendReplacement(result, "");
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Process loop blocks.
     *
     * @param content Template content
     * @param variables Variable values
     * @return Content with loops expanded
     */
    private String processLoops(final String content, final Map<String, Object> variables) {
        Matcher matcher = LOOP_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String listName = matcher.group(1).trim();
            String body = matcher.group(2);

            Object listObj = variables.get(listName);
            if (listObj instanceof List<?> list) {
                StringBuilder loopResult = new StringBuilder();

                for (Object item : list) {
                    Map<String, Object> itemVars = new HashMap<>(variables);
                    if (item instanceof Map<?, ?> itemMap) {
                        itemMap.forEach((k, v) -> itemVars.put(k.toString(), v));
                    } else {
                        itemVars.put("item", item);
                    }

                    String rendered = processVariables(body, itemVars);
                    loopResult.append(rendered);
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(loopResult.toString()));
            } else {
                log.warn("Variable is not a list: {}", listName);
                matcher.appendReplacement(result, "");
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Evaluate a conditional expression.
     *
     * @param condition Condition variable name
     * @param variables Variable values
     * @return true if condition is met, false otherwise
     */
    private boolean evaluateCondition(final String condition, final Map<String, Object> variables) {
        Object value = variables.get(condition);

        if (value == null) {
            return false;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            return !((String) value).isEmpty();
        }

        if (value instanceof Collection<?>) {
            return !((Collection<?>) value).isEmpty();
        }

        return true;
    }

    /**
     * Load templates from a map of contents.
     *
     * @param templateContents Map of template names to content
     */
    public void loadTemplates(final Map<String, String> templateContents) {
        templateContents.forEach(
                (name, content) -> {
                    registerTemplate(name, new PromptTemplate(name, content, Map.of()));
                });
        log.info("Loaded {} templates", templateContents.size());
    }

    /**
     * Get all registered template names.
     *
     * @return Set of template names
     */
    public Set<String> getTemplateNames() {
        return new HashSet<>(templates.keySet());
    }

    /**
     * Prompt template definition.
     *
     * @param name Template name
     * @param content Template content with placeholders
     * @param defaults Default variable values
     */
    public record PromptTemplate(String name, String content, Map<String, Object> defaults) {}

    /** Builder for template rendering. */
    public static class TemplateBuilder {
        /** Template engine instance. */
        private final PromptTemplateEngine engine;

        /** Name of template to build. */
        private final String templateName;

        /** Variables for template substitution. */
        private final Map<String, Object> variables = new HashMap<>();

        /**
         * Constructs a template builder.
         *
         * @param templateEngine Template engine instance
         * @param template Name of template to build
         */
        public TemplateBuilder(final PromptTemplateEngine templateEngine, final String template) {
            this.engine = templateEngine;
            this.templateName = template;
        }

        /**
         * Add a variable to the template.
         *
         * @param key Variable name
         * @param value Variable value
         * @return This builder for chaining
         */
        public TemplateBuilder with(final String key, final Object value) {
            variables.put(key, value);
            return this;
        }

        /**
         * Add multiple variables to the template.
         *
         * @param vars Map of variables to add
         * @return This builder for chaining
         */
        public TemplateBuilder withAll(final Map<String, Object> vars) {
            variables.putAll(vars);
            return this;
        }

        /**
         * Build and render the template.
         *
         * @return Rendered template string
         */
        public String build() {
            return engine.render(templateName, variables);
        }
    }

    /**
     * Create a template builder.
     *
     * @param templateName Name of template to build
     * @return Template builder instance
     */
    public TemplateBuilder template(final String templateName) {
        return new TemplateBuilder(this, templateName);
    }
}
