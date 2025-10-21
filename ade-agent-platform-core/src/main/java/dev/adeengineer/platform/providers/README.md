# Infrastructure Providers

Framework-agnostic implementations of AI Agent SDK infrastructure providers.

## Overview

This package provides **pure POJO implementations** of infrastructure providers from the `ade-agent-sdk`. These providers have **no Spring dependencies** and can be used in any Java application.

## Available Providers

### 1. Memory Provider
**Implementation:** `InMemoryMemoryProvider`
- Vector similarity search using cosine similarity
- Conversation history tracking via metadata
- Thread-safe concurrent operations
- Not persistent (in-memory only)

**Usage:**
```java
EmbeddingsProvider embeddings = ...; // your embeddings implementation
MemoryProvider memory = new InMemoryMemoryProvider(embeddings);

// Store a memory
MemoryEntry entry = new MemoryEntry(null, "content", Map.of(), Instant.now(), 0.8);
memory.store(entry).block();

// Search by similarity
memory.search("query", 5, Map.of()).subscribe(result -> {
    System.out.println("Found: " + result.entry().content());
});
```

### 2. Storage Provider
**Implementation:** `LocalStorageProvider`
- Filesystem-based document storage
- Metadata persistence to JSON files
- Binary content support
- Query with filters, limits, offsets

**Usage:**
```java
ObjectMapper mapper = new ObjectMapper();
StorageProvider storage = new LocalStorageProvider("./data/storage", mapper);

// Store a document
byte[] content = "Document content".getBytes();
Document doc = new Document(null, content, "text/plain", Map.of(), Instant.now(), content.length);
storage.store(doc).block();

// Query documents
StorageQuery query = new StorageQuery(Map.of("category", "A"), 0, 10, null, true);
storage.query(query).subscribe(doc -> {
    System.out.println("Found: " + doc.id());
});
```

### 3. Tool Provider
**Implementation:** `SimpleToolProvider`
- Dynamic tool registration
- Built-in tools: echo, calculator, get_timestamp
- Custom tool support with implementations

**Usage:**
```java
ToolProvider tools = new SimpleToolProvider();

// Use built-in tool
ToolInvocation invocation = new ToolInvocation(
    "calculator",
    Map.of("operation", "add", "a", 10.0, "b", 5.0),
    Map.of()
);
tools.invoke(invocation).subscribe(result -> {
    System.out.println("Result: " + result.result()); // 15.0
});

// Register custom tool
Tool customTool = new Tool(
    "uppercase",
    "Converts text to uppercase",
    Map.of("text", Map.of("type", "string")),
    "transformation"
);

tools.registerTool(customTool, params -> {
    String text = (String) params.get("text");
    return text != null ? text.toUpperCase() : "";
}).block();
```

### 4. Orchestration Provider
**Implementation:** `SimpleOrchestrationProvider`
- Sequential workflow execution
- Event streaming via Reactor Sinks
- Workflow cancellation support
- Step dependencies

**Usage:**
```java
OrchestrationProvider orchestration = new SimpleOrchestrationProvider();

// Define workflow
WorkflowStep step1 = new WorkflowStep("step1", "agent1", Map.of(), List.of());
WorkflowStep step2 = new WorkflowStep("step2", "agent2", Map.of(), List.of("step1"));

WorkflowDefinition workflow = new WorkflowDefinition(
    "my-workflow",
    "My Workflow",
    List.of(step1, step2),
    Map.of()
);

// Register and execute
orchestration.registerWorkflow(workflow).block();
WorkflowExecution execution = orchestration.executeWorkflow("my-workflow", Map.of()).block();

// Stream updates
orchestration.streamExecutionUpdates(execution.executionId())
    .subscribe(update -> {
        System.out.println("Status: " + update.status());
    });
```

### 5. Evaluation Provider
**Implementation:** `LLMEvaluationProvider`
- LLM-as-judge pattern
- Criteria-based evaluation
- Test case validation
- A/B comparison

**Usage:**
```java
LLMProvider llm = ...; // your LLM implementation
EvaluationProvider evaluator = new LLMEvaluationProvider(llm);

// Evaluate output
EvaluationCriteria criteria = new EvaluationCriteria(
    List.of("accuracy", "relevance"),
    0.7,
    "Reference text"
);

evaluator.evaluate("Output to evaluate", criteria).subscribe(result -> {
    System.out.println("Score: " + result.score());
    System.out.println("Passed: " + result.passed());
});

// Compare outputs
evaluator.compare("Output A", "Output B", criteria).subscribe(comparison -> {
    if (comparison < 0) {
        System.out.println("Output A is better");
    } else if (comparison > 0) {
        System.out.println("Output B is better");
    } else {
        System.out.println("Outputs are equal");
    }
});
```

## Spring Boot Integration

When used in a Spring Boot application, providers are automatically configured via `ProvidersAutoConfiguration`.

Simply add the dependency and autowire:

```java
@SpringBootApplication
public class MyApp {

    @Autowired
    private MemoryProvider memory;

    @Autowired
    private ToolProvider tools;

    // Providers are auto-configured and ready to use!
}
```

**Configuration:**
```yaml
# application.yml
ade:
  storage:
    path: ./data/storage  # LocalStorageProvider root directory (default: ./data/storage)
```

## Design Principles

1. ✅ **Framework-Agnostic** - No Spring dependencies in provider implementations
2. ✅ **Pure POJOs** - Can be instantiated directly via `new`
3. ✅ **Immutable** - Thread-safe concurrent operations
4. ✅ **Reactive** - Project Reactor (Mono/Flux) for async operations
5. ✅ **SDK-Compliant** - Implement interfaces from `ade-agent-sdk`

## Testing

All providers have comprehensive unit tests that work without Spring:

```bash
# Run all provider tests
mvn test -Dtest="*ProviderTest"

# Run specific provider test
mvn test -Dtest="SimpleToolProviderTest"
```

See `src/test/java/dev/adeengineer/platform/providers/TEST_README.md` for test documentation.

## Dependencies

**Required:**
- `ade-agent-sdk` (provider interfaces)
- Project Reactor (reactive streams)

**Optional:**
- Jackson ObjectMapper (for LocalStorageProvider metadata serialization)
- EmbeddingsProvider implementation (for InMemoryMemoryProvider)
- LLMProvider implementation (for LLMEvaluationProvider)

---

*Last Updated: 2025-10-21*
*Version: 0.2.0*
