package dev.adeengineer.platform.providers.tools;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import dev.adeengineer.tools.ToolProvider;
import dev.adeengineer.tools.model.Tool;
import dev.adeengineer.tools.model.ToolInvocation;
import dev.adeengineer.tools.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Simple in-memory implementation of ToolProvider. Manages tool registration and invocation with
 * synchronous execution.
 *
 * <p>Features: - Dynamic tool registration - Thread-safe concurrent access - Function-based tool
 * implementation - Automatic result wrapping
 *
 * <p>Use cases: - Custom function calling - API integrations - External service connectors - Plugin
 * systems
 */
@Slf4j
public final class SimpleToolProvider implements ToolProvider {

    /** Registry of available tools. */
    private final Map<String, Tool> toolRegistry = new ConcurrentHashMap<>();

    /** Registry of tool implementations. */
    private final Map<String, Function<Map<String, Object>, Object>> toolImplementations =
            new ConcurrentHashMap<>();

    /** Creates a simple tool provider. */
    public SimpleToolProvider() {
        log.info("Initialized SimpleToolProvider");
        registerBuiltInTools();
    }

    /**
     * Register a tool with the provider.
     *
     * @param tool The tool definition to register
     * @return Mono emitting the registered tool
     */
    @Override
    public Mono<Tool> registerTool(final Tool tool) {
        toolRegistry.put(tool.name(), tool);
        log.info("Registered tool: {}", tool.name());
        return Mono.just(tool);
    }

    /**
     * Register a tool with its implementation function.
     *
     * @param tool The tool definition
     * @param implementation The function that implements the tool
     * @return Mono emitting the registered tool
     */
    public Mono<Tool> registerTool(
            final Tool tool, final Function<Map<String, Object>, Object> implementation) {
        toolRegistry.put(tool.name(), tool);
        toolImplementations.put(tool.name(), implementation);
        log.info("Registered tool with implementation: {}", tool.name());
        return Mono.just(tool);
    }

    /**
     * Invoke a tool by name.
     *
     * @param invocation The tool invocation request
     * @return Mono emitting the tool execution result
     */
    @Override
    public Mono<ToolResult> invoke(final ToolInvocation invocation) {
        return Mono.fromCallable(
                () -> {
                    final Tool tool = toolRegistry.get(invocation.toolName());
                    if (tool == null) {
                        return new ToolResult(
                                invocation.toolName(),
                                false,
                                null,
                                "Tool not found: " + invocation.toolName(),
                                0L,
                                Instant.now());
                    }

                    final Function<Map<String, Object>, Object> implementation =
                            toolImplementations.get(invocation.toolName());
                    if (implementation == null) {
                        return new ToolResult(
                                invocation.toolName(),
                                false,
                                null,
                                "No implementation registered for tool: " + invocation.toolName(),
                                0L,
                                Instant.now());
                    }

                    // Execute tool
                    final long startTime = System.currentTimeMillis();
                    try {
                        final Object result = implementation.apply(invocation.arguments());
                        final long executionTime = System.currentTimeMillis() - startTime;

                        log.debug(
                                "Tool {} executed successfully in {}ms",
                                invocation.toolName(),
                                executionTime);

                        return new ToolResult(
                                invocation.toolName(),
                                true,
                                result,
                                null,
                                executionTime,
                                Instant.now());

                    } catch (Exception e) {
                        final long executionTime = System.currentTimeMillis() - startTime;
                        log.error(
                                "Tool {} execution failed: {}",
                                invocation.toolName(),
                                e.getMessage(),
                                e);

                        return new ToolResult(
                                invocation.toolName(),
                                false,
                                null,
                                "Execution failed: " + e.getMessage(),
                                executionTime,
                                Instant.now());
                    }
                });
    }

    /**
     * Get all available tools.
     *
     * @return Flux of all registered tools
     */
    @Override
    public Flux<Tool> getAvailableTools() {
        return Flux.fromIterable(toolRegistry.values());
    }

    /**
     * Get a specific tool by name.
     *
     * @param toolName The tool name
     * @return Mono emitting the tool, or empty if not found
     */
    @Override
    public Mono<Tool> getTool(final String toolName) {
        final Tool tool = toolRegistry.get(toolName);
        return tool != null ? Mono.just(tool) : Mono.empty();
    }

    /**
     * Check if a tool exists.
     *
     * @param toolName The tool name
     * @return true if the tool is registered
     */
    @Override
    public boolean hasTool(final String toolName) {
        return toolRegistry.containsKey(toolName);
    }

    /**
     * Unregister a tool.
     *
     * @param toolName The tool name to unregister
     * @return Mono emitting true if unregistered, false if not found
     */
    @Override
    public Mono<Boolean> unregisterTool(final String toolName) {
        final boolean removed = toolRegistry.remove(toolName) != null;
        if (removed) {
            toolImplementations.remove(toolName);
            log.info("Unregistered tool: {}", toolName);
        }
        return Mono.just(removed);
    }

    /**
     * Get the provider name.
     *
     * @return Provider name
     */
    @Override
    public String getProviderName() {
        return "simple";
    }

    /**
     * Check if the provider is healthy and accessible.
     *
     * @return true if the provider is ready to use
     */
    @Override
    public boolean isHealthy() {
        return true; // Always healthy for in-memory implementation
    }

    /** Register built-in tools that are always available. */
    private void registerBuiltInTools() {
        // Echo tool - returns input unchanged
        final Tool echoTool =
                new Tool(
                        "echo",
                        "Returns the input text unchanged (useful for testing)",
                        Map.of(
                                "text",
                                Map.of(
                                        "type", "string",
                                        "description", "The text to echo",
                                        "required", true)),
                        "utility");

        registerTool(
                        echoTool,
                        params -> {
                            String text = (String) params.get("text");
                            return text != null ? text : "";
                        })
                .subscribe();

        // Calculator tool - performs basic arithmetic
        final Tool calculatorTool =
                new Tool(
                        "calculator",
                        "Performs basic arithmetic operations (+, -, *, /)",
                        Map.of(
                                "operation",
                                        Map.of(
                                                "type", "string",
                                                "description",
                                                        "The operation to perform: add, subtract, multiply, divide",
                                                "required", true),
                                "a",
                                        Map.of(
                                                "type", "number",
                                                "description", "First operand",
                                                "required", true),
                                "b",
                                        Map.of(
                                                "type", "number",
                                                "description", "Second operand",
                                                "required", true)),
                        "computation");

        registerTool(
                        calculatorTool,
                        params -> {
                            String operation = (String) params.get("operation");
                            double a = ((Number) params.get("a")).doubleValue();
                            double b = ((Number) params.get("b")).doubleValue();

                            return switch (operation) {
                                case "add" -> a + b;
                                case "subtract" -> a - b;
                                case "multiply" -> a * b;
                                case "divide" -> b != 0 ? a / b : Double.NaN;
                                default -> "Unknown operation: " + operation;
                            };
                        })
                .subscribe();

        // Timestamp tool - returns current timestamp
        final Tool timestampTool =
                new Tool(
                        "get_timestamp",
                        "Returns the current Unix timestamp in milliseconds",
                        Map.of(),
                        "utility");

        registerTool(timestampTool, params -> System.currentTimeMillis()).subscribe();

        log.info("Registered {} built-in tools", 3);
    }
}
