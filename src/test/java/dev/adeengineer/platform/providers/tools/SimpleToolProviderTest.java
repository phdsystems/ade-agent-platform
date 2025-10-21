package dev.adeengineer.platform.providers.tools;

import dev.adeengineer.tools.model.Tool;
import dev.adeengineer.tools.model.ToolInvocation;
import dev.adeengineer.tools.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimpleToolProvider.
 */
class SimpleToolProviderTest {

    private SimpleToolProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SimpleToolProvider();
    }

    @Test
    void shouldRegisterTool() {
        // Given
        Tool tool = new Tool(
                "custom_tool",
                "A custom tool for testing",
                Map.of("param1", Map.of("type", "string")),
                "test"
        );

        // When & Then
        StepVerifier.create(provider.registerTool(tool))
                .assertNext(registered -> {
                    assertEquals("custom_tool", registered.name());
                    assertEquals("A custom tool for testing", registered.description());
                })
                .verifyComplete();

        // Verify tool exists
        assertTrue(provider.hasTool("custom_tool"));
    }

    @Test
    void shouldRegisterToolWithImplementation() {
        // Given
        Tool tool = new Tool(
                "uppercase",
                "Converts text to uppercase",
                Map.of("text", Map.of("type", "string")),
                "transformation"
        );

        // When
        provider.registerTool(tool, params -> {
            String text = (String) params.get("text");
            return text != null ? text.toUpperCase() : "";
        }).block();

        // Then
        assertTrue(provider.hasTool("uppercase"));
    }

    @Test
    void shouldInvokeEchoTool() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "echo",
                Map.of("text", "Hello, World!"),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals("Hello, World!", result.result());
                    assertNull(result.error());
                })
                .verifyComplete();
    }

    @Test
    void shouldInvokeCalculatorToolAdd() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "calculator",
                Map.of(
                        "operation", "add",
                        "a", 10.0,
                        "b", 5.0
                ),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals(15.0, result.result());
                })
                .verifyComplete();
    }

    @Test
    void shouldInvokeCalculatorToolSubtract() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "calculator",
                Map.of(
                        "operation", "subtract",
                        "a", 10.0,
                        "b", 3.0
                ),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals(7.0, result.result());
                })
                .verifyComplete();
    }

    @Test
    void shouldInvokeCalculatorToolMultiply() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "calculator",
                Map.of(
                        "operation", "multiply",
                        "a", 4.0,
                        "b", 5.0
                ),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals(20.0, result.result());
                })
                .verifyComplete();
    }

    @Test
    void shouldInvokeCalculatorToolDivide() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "calculator",
                Map.of(
                        "operation", "divide",
                        "a", 20.0,
                        "b", 4.0
                ),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals(5.0, result.result());
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleDivideByZero() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "calculator",
                Map.of(
                        "operation", "divide",
                        "a", 10.0,
                        "b", 0.0
                ),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals(Double.NaN, result.result());
                })
                .verifyComplete();
    }

    @Test
    void shouldInvokeTimestampTool() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "get_timestamp",
                Map.of(),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertNotNull(result.result());
                    assertTrue(result.result() instanceof Long);
                    assertTrue((Long) result.result() > 0);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorForUnknownTool() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "unknown_tool",
                Map.of(),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertFalse(result.success());
                    assertNull(result.result());
                    assertTrue(result.error().contains("Tool not found"));
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorForToolWithoutImplementation() {
        // Given
        Tool tool = new Tool(
                "no_impl",
                "Tool without implementation",
                Map.of(),
                "test"
        );
        provider.registerTool(tool).block();

        ToolInvocation invocation = new ToolInvocation(
                "no_impl",
                Map.of(),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertFalse(result.success());
                    assertTrue(result.error().contains("No implementation registered"));
                })
                .verifyComplete();
    }

    @Test
    void shouldGetAllAvailableTools() {
        // When & Then - built-in tools should be available
        StepVerifier.create(provider.getAvailableTools())
                .expectNextMatches(tool -> tool.name().equals("echo") ||
                        tool.name().equals("calculator") ||
                        tool.name().equals("get_timestamp"))
                .expectNextMatches(tool -> tool.name().equals("echo") ||
                        tool.name().equals("calculator") ||
                        tool.name().equals("get_timestamp"))
                .expectNextMatches(tool -> tool.name().equals("echo") ||
                        tool.name().equals("calculator") ||
                        tool.name().equals("get_timestamp"))
                .verifyComplete();
    }

    @Test
    void shouldGetSpecificTool() {
        // When & Then
        StepVerifier.create(provider.getTool("echo"))
                .assertNext(tool -> {
                    assertEquals("echo", tool.name());
                    assertTrue(tool.description().contains("Returns"));
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNonExistentTool() {
        // When & Then
        StepVerifier.create(provider.getTool("non_existent"))
                .verifyComplete();
    }

    @Test
    void shouldCheckToolExists() {
        // Then
        assertTrue(provider.hasTool("echo"));
        assertTrue(provider.hasTool("calculator"));
        assertTrue(provider.hasTool("get_timestamp"));
        assertFalse(provider.hasTool("non_existent"));
    }

    @Test
    void shouldUnregisterTool() {
        // Given
        Tool tool = new Tool(
                "temp_tool",
                "Temporary tool",
                Map.of(),
                "test"
        );
        provider.registerTool(tool).block();

        assertTrue(provider.hasTool("temp_tool"));

        // When
        Boolean unregistered = provider.unregisterTool("temp_tool").block();

        // Then
        assertTrue(unregistered);
        assertFalse(provider.hasTool("temp_tool"));
    }

    @Test
    void shouldReturnFalseWhenUnregisteringNonExistent() {
        // When & Then
        StepVerifier.create(provider.unregisterTool("non_existent"))
                .assertNext(result -> assertFalse(result))
                .verifyComplete();
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("simple", provider.getProviderName());
    }

    @Test
    void shouldAlwaysBeHealthy() {
        assertTrue(provider.isHealthy());
    }

    @Test
    void shouldInvokeCustomToolWithImplementation() {
        // Given
        Tool tool = new Tool(
                "reverse",
                "Reverses a string",
                Map.of("text", Map.of("type", "string")),
                "transformation"
        );

        provider.registerTool(tool, params -> {
            String text = (String) params.get("text");
            return text != null ?
                    new StringBuilder(text).reverse().toString() : "";
        }).block();

        ToolInvocation invocation = new ToolInvocation(
                "reverse",
                Map.of("text", "hello"),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals("olleh", result.result());
                })
                .verifyComplete();
    }

    @Test
    void shouldCaptureExecutionTime() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "echo",
                Map.of("text", "test"),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.executionTimeMs() >= 0);
                    assertNotNull(result.timestamp());
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleExceptionInToolExecution() {
        // Given
        Tool tool = new Tool(
                "error_tool",
                "A tool that throws an exception",
                Map.of(),
                "test"
        );

        provider.registerTool(tool, params -> {
            throw new RuntimeException("Intentional error");
        }).block();

        ToolInvocation invocation = new ToolInvocation(
                "error_tool",
                Map.of(),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertFalse(result.success());
                    assertTrue(result.error().contains("Intentional error"));
                    assertNull(result.result());
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleIntegerParametersInCalculator() {
        // Given
        ToolInvocation invocation = new ToolInvocation(
                "calculator",
                Map.of(
                        "operation", "add",
                        "a", 5,  // Integer instead of Double
                        "b", 3   // Integer instead of Double
                ),
                Map.of()
        );

        // When & Then
        StepVerifier.create(provider.invoke(invocation))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals(8.0, result.result());
                })
                .verifyComplete();
    }
}
