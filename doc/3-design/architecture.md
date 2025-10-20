# Architecture Design - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Pattern**: Multi-agent system with registry pattern + strategy pattern. **Components**: RoleManager (orchestrator) → AgentRegistry (13 role agents) → Task Router → LLM Provider → Output Formatter. **Tech stack**: Java 21, Spring Boot 3.5, Spring DI. **Key design**: Each role = AgentRole interface implementation, prompts externalized in YAML.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Role Manager App                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           RoleManager (Orchestrator)                  │  │
│  │  - executeTask(role, task)                           │  │
│  │  - executeMultiAgent(roles[], task)                  │  │
│  │  - listRoles()                                       │  │
│  └────────────┬─────────────────────────────────────────┘  │
│               │                                              │
│  ┌────────────▼──────────────────────────────────────────┐ │
│  │           AgentRegistry                               │ │
│  │  - Map<String, AgentRole> agents                     │ │
│  │  - getAgent(roleName): AgentRole                     │ │
│  │  - registerAgent(AgentRole)                          │ │
│  └────────────┬──────────────────────────────────────────┘ │
│               │                                              │
│  ┌────────────▼──────────────────────────────────────────┐ │
│  │      AgentRole Interface (13 implementations)         │ │
│  │  ┌──────────────────────────────────────────────────┐│ │
│  │  │ + DeveloperAgent                                 ││ │
│  │  │ + ManagerAgent                                   ││ │
│  │  │ + QAAgent                                        ││ │
│  │  │ + SecurityAgent                                  ││ │
│  │  │ + DevOpsAgent                                    ││ │
│  │  │ + ExecutiveAgent (CTO/Director/Sponsor)          ││ │
│  │  │ + ProductOwnerAgent                              ││ │
│  │  │ + TechnicalWriterAgent                           ││ │
│  │  │ + UIUXDesignerAgent                              ││ │
│  │  │ + DataEngineerAgent                              ││ │
│  │  │ + SREAgent (Site Reliability Engineer)           ││ │
│  │  │ + ComplianceAgent                                ││ │
│  │  │ + CustomerSupportAgent                           ││ │
│  │  └──────────────────────────────────────────────────┘│ │
│  └────────────┬──────────────────────────────────────────┘ │
│               │                                              │
│  ┌────────────▼──────────────────────────────────────────┐ │
│  │         LLMProviderFactory                            │ │
│  │  - getProvider(name): LLMProvider                    │ │
│  │  Cloud: Anthropic / OpenAI / HuggingFace             │ │
│  │  Local: Ollama / vLLM / TGI / Ray Serve              │ │
│  └────────────┬──────────────────────────────────────────┘ │
│               │                                              │
│  ┌────────────▼──────────────────────────────────────────┐ │
│  │         OutputFormatter                               │ │
│  │  - formatForRole(role, content): String              │ │
│  │  - Technical / Executive / Detailed formats          │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Component Design

### 1. AgentRole Interface

```java
public interface AgentRole {
    String getRoleName();
    String getDescription();
    List<String> getCapabilities();

    // Execute task and return LLM response
    LLMResponse executeTask(String task, Map<String, Object> context);

    // Format output for this role's preferences
    String formatOutput(LLMResponse response);

    // Get role-specific prompt template
    String getPromptTemplate();
}
```

### 2. Example Implementation - DeveloperAgent

```java
@Component
public class DeveloperAgent implements AgentRole {
    private final LLMProvider llm;
    private final String promptTemplate;

    @Autowired
    public DeveloperAgent(
        LLMProviderFactory llmFactory,
        @Value("${agents.developer.prompt}") String promptTemplate
    ) {
        this.llm = llmFactory.getDefaultProvider();
        this.promptTemplate = promptTemplate;
    }

    @Override
    public String getRoleName() {
        return "Software Developer";
    }

    @Override
    public LLMResponse executeTask(String task, Map<String, Object> context) {
        String prompt = String.format(promptTemplate, task, context);
        return llm.createCompletion(prompt, 4096, 0.7, Map.of());
    }

    @Override
    public String formatOutput(LLMResponse response) {
        // Technical format: code blocks, file paths, detailed analysis
        return formatAsTechnicalReport(response.getText());
    }
}
```

### 3. RoleManager Service

```java
@Service
public class RoleManager {
    private final AgentRegistry registry;
    private final OutputFormatter formatter;

    public TaskResult executeTask(String roleName, String task, Map<String, Object> context) {
        AgentRole agent = registry.getAgent(roleName);
        LLMResponse response = agent.executeTask(task, context);
        String formatted = agent.formatOutput(response);

        return new TaskResult(roleName, task, formatted, response.getUsage());
    }

    public TaskResult executeMultiAgent(List<String> roles, String task, Map<String, Object> context) {
        List<TaskResult> results = roles.stream()
            .map(role -> executeTask(role, task, context))
            .toList();

        return aggregateResults(results);
    }

    public List<RoleInfo> listRoles() {
        return registry.getAllRoles().stream()
            .map(agent -> new RoleInfo(
                agent.getRoleName(),
                agent.getDescription(),
                agent.getCapabilities()
            ))
            .toList();
    }
}
```

---

## Data Model

### TaskResult
```java
public record TaskResult(
    String roleName,
    String task,
    String output,
    LLMUsage usage,
    Instant timestamp
) {}
```

### RoleInfo
```java
public record RoleInfo(
    String name,
    String description,
    List<String> capabilities
) {}
```

### AgentConfig (YAML)
```yaml
role: Software Developer
description: "Provides technical code reviews and implementation guidance"
capabilities:
  - Code review
  - Bug diagnosis
  - Refactoring suggestions
  - Test generation
temperature: 0.7
max_tokens: 4096
prompt_template: |
  You are an expert Software Developer assisting with: {task}

  Context: {context}

  Provide detailed technical analysis with:
  - Specific file paths and line numbers
  - Code examples
  - Best practices

  Format output as structured markdown.
```

---

## Design Patterns

The Role Manager App leverages several proven design patterns to achieve extensibility, maintainability, and resilience:

### 1. Strategy Pattern
**Purpose:** Define a family of algorithms (agent behaviors) and make them interchangeable

**Implementation:**
- `AgentRole` interface with 13 concrete implementations
- Each agent (DeveloperAgent, QAAgent, SecurityAgent, etc.) implements the same interface
- Clients (RoleManager) work with the interface, not concrete classes

**Benefits:**
- Add new role agents without modifying existing code (Open/Closed Principle)
- Swap agent implementations at runtime
- Test each agent in isolation

**Example:**
```java
// Strategy interface
public interface AgentRole {
    LLMResponse executeTask(String task, Map<String, Object> context);
    String formatOutput(LLMResponse response);
}

// Concrete strategies
public class DeveloperAgent implements AgentRole { ... }
public class QAAgent implements AgentRole { ... }
public class SecurityAgent implements AgentRole { ... }
```

### 2. Registry Pattern
**Purpose:** Provide a centralized, well-known location for storing and retrieving agent instances

**Implementation:**
- `AgentRegistry` maintains a `Map<String, AgentRole>` of all agents
- Single source of truth for available agents
- Thread-safe concurrent access

**Benefits:**
- Fast O(1) agent lookup by role name
- Central validation point (ensure all 13 agents are registered)
- Simplifies agent lifecycle management
- Supports dynamic agent discovery

**Example:**
```java
@Component
public class AgentRegistry {
    private final Map<String, AgentRole> agents = new ConcurrentHashMap<>();

    public void registerAgent(AgentRole agent) {
        agents.put(agent.getRoleName(), agent);
    }

    public AgentRole getAgent(String roleName) {
        return agents.get(roleName);
    }
}
```

### 3. Factory Pattern
**Purpose:** Encapsulate object creation logic, particularly for LLM provider selection

**Implementation:**
- `LLMProviderFactory` creates appropriate LLM provider instances
- Abstracts differences between Anthropic, OpenAI, and Ollama
- Handles provider configuration and initialization

**Benefits:**
- Client code doesn't need to know concrete provider classes
- Easy to add new LLM providers
- Centralized provider configuration logic
- Supports provider failover strategies

**Example:**
```java
@Component
public class LLMProviderFactory {
    public LLMProvider getDefaultProvider() {
        String providerName = config.getDefaultProvider();
        return getProvider(providerName);
    }

    public LLMProvider getProvider(String name) {
        return switch (name) {
            case "anthropic" -> new AnthropicProvider(config.getAnthropicApiKey());
            case "openai" -> new OpenAIProvider(config.getOpenAIApiKey());
            case "ollama" -> new OllamaProvider(config.getOllamaUrl());
            default -> throw new IllegalArgumentException("Unknown provider: " + name);
        };
    }
}
```

### 4. Template Method Pattern
**Purpose:** Define the skeleton of an algorithm, allowing subclasses to override specific steps

**Implementation:**
- Base agent behavior defined in abstract class or interface
- Prompt templates serve as the "template method"
- Each agent customizes the template with role-specific instructions

**Benefits:**
- Consistent structure across all agents
- Reuse common logic (LLM invocation, error handling)
- Allow role-specific customization (prompts, formatting)

**Example:**
```yaml
# Base template structure (same for all agents)
prompt_template: |
  You are an expert {role} assisting with: {task}

  Context: {context}

  {role_specific_instructions}

  Format output as structured markdown.

# Developer-specific customization
role_specific_instructions: |
  Provide detailed technical analysis with:
  - Specific file paths and line numbers
  - Code examples
  - Best practices
```

### 5. Dependency Injection Pattern
**Purpose:** Invert control of dependency creation and management

**Implementation:**
- Spring Framework's DI container
- Constructor injection for all dependencies
- `@Autowired`, `@Component`, `@Service` annotations

**Benefits:**
- Loose coupling between components
- Easy to test with mock dependencies
- Framework manages object lifecycle
- Configuration externalized

**Example:**
```java
@Component
public class DeveloperAgent implements AgentRole {
    private final LLMProvider llm;
    private final OutputFormatter formatter;

    @Autowired
    public DeveloperAgent(
        LLMProviderFactory llmFactory,
        OutputFormatter formatter
    ) {
        this.llm = llmFactory.getDefaultProvider();
        this.formatter = formatter;
    }
}
```

### 6. Circuit Breaker Pattern
**Purpose:** Prevent cascading failures when external services (LLM APIs) are down

**Implementation:**
- Track consecutive failures per LLM provider
- Open circuit after threshold (e.g., 5 failures)
- Wait for recovery period (e.g., 30 seconds)
- Close circuit after successful calls

**Benefits:**
- Fail fast when provider is known to be down
- Prevent wasted retry attempts
- Automatic recovery when service returns
- Improved system resilience

**Example:**
```java
public class CircuitBreaker {
    private int failureCount = 0;
    private final int threshold = 5;
    private CircuitState state = CircuitState.CLOSED;
    private Instant openedAt;

    public <T> T call(Supplier<T> operation) {
        if (state == OPEN && Duration.between(openedAt, Instant.now()).getSeconds() > 30) {
            state = HALF_OPEN;  // Try again after timeout
        }

        if (state == OPEN) {
            throw new CircuitOpenException("Circuit breaker is open");
        }

        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
}
```

### 7. Retry Pattern with Exponential Backoff
**Purpose:** Automatically retry failed operations with increasing delays

**Implementation:**
- Retry LLM API calls on transient failures (429, 500, 503)
- Exponential backoff: 1s, 2s, 4s between retries
- Maximum 3 retry attempts

**Benefits:**
- Recover from transient network issues
- Avoid overwhelming failing services
- Exponential backoff gives services time to recover
- Improved reliability without infinite retries

**Example:**
```java
public LLMResponse callWithRetry(Supplier<LLMResponse> operation) {
    int maxRetries = 3;
    long waitMillis = 1000;  // Start with 1 second

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
        try {
            return operation.get();
        } catch (RetryableException e) {
            if (attempt == maxRetries) throw e;

            Thread.sleep(waitMillis);
            waitMillis *= 2;  // Exponential backoff: 1s, 2s, 4s
        }
    }
}
```

### 8. Facade Pattern
**Purpose:** Provide a simplified interface to a complex subsystem

**Implementation:**
- `RoleManager` acts as facade for agent system
- Hides complexity of registry lookup, multi-agent coordination, result aggregation
- Clients interact with simple `executeTask()` API

**Benefits:**
- Simple API for complex operations
- Decouples clients from internal implementation
- Easier to refactor internal components
- Consistent entry point for all agent operations

**Example:**
```java
@Service
public class RoleManager {  // Facade
    private final AgentRegistry registry;
    private final OutputFormatter formatter;
    private final LLMProviderFactory llmFactory;

    // Simple API hides complex orchestration
    public TaskResult executeTask(String roleName, String task) {
        AgentRole agent = registry.getAgent(roleName);  // Registry lookup
        LLMResponse response = agent.executeTask(task);  // Strategy execution
        String formatted = formatter.format(response, roleName);  // Formatting
        return new TaskResult(roleName, task, formatted);  // Result packaging
    }
}
```

### 9. Builder Pattern (via Lombok)
**Purpose:** Construct complex objects step by step

**Implementation:**
- `@Builder` annotation on data classes
- Fluent API for object construction
- Immutable objects with all-args constructor

**Benefits:**
- Readable object construction
- Optional parameters without telescoping constructors
- Immutable objects (thread-safe)

**Example:**
```java
@Builder
public record TaskRequest(
    String roleName,
    String task,
    Map<String, Object> context,
    Instant timestamp
) {}

// Usage
TaskRequest request = TaskRequest.builder()
    .roleName("Software Developer")
    .task("Review PR #123")
    .context(Map.of("repo", "my-app"))
    .timestamp(Instant.now())
    .build();
```

### 10. Observer Pattern (Future Enhancement)
**Purpose:** Notify interested parties when agent task state changes

**Implementation (Planned):**
- `TaskEventPublisher` notifies listeners on task start/complete/fail
- Listeners: Metrics collector, audit logger, notification service

**Benefits:**
- Loose coupling between task execution and monitoring
- Easy to add new listeners (e.g., Slack notifications)
- Centralized event handling

**Example (Future):**
```java
public interface TaskEventListener {
    void onTaskStarted(TaskEvent event);
    void onTaskCompleted(TaskEvent event);
    void onTaskFailed(TaskEvent event);
}

@Component
public class MetricsCollector implements TaskEventListener {
    @Override
    public void onTaskCompleted(TaskEvent event) {
        metrics.recordTaskDuration(event.getDuration());
        metrics.recordTokenUsage(event.getUsage());
    }
}
```

### Pattern Summary Table

| Pattern | Component | Purpose | Benefit |
|---------|-----------|---------|---------|
| **Strategy** | AgentRole interface | Interchangeable agent behaviors | Extensibility (add roles without modifying core) |
| **Registry** | AgentRegistry | Centralized agent storage | Fast lookup, single source of truth |
| **Factory** | LLMProviderFactory | Abstract provider creation | Provider flexibility, easy failover |
| **Template Method** | Prompt templates | Consistent agent structure | Reuse common logic, allow customization |
| **Dependency Injection** | Spring Framework | Invert dependency control | Testability, loose coupling |
| **Circuit Breaker** | LLM provider calls | Prevent cascading failures | Resilience, fail fast |
| **Retry with Backoff** | API error handling | Auto-retry transient failures | Reliability, graceful degradation |
| **Facade** | RoleManager | Simplify complex subsystem | Easy-to-use API, decouple clients |
| **Builder** | Data records | Construct complex objects | Readability, immutability |
| **Observer** | Task events (future) | Notify on state changes | Extensibility, loose coupling |

---

## LLM Integration Architecture

The application supports two types of LLM integration:

### Cloud API Providers
Direct API integration with commercial LLM services:
- **Anthropic Claude** - Claude 3.5 Sonnet via REST API
- **OpenAI GPT** - GPT-4 Turbo via REST API
- **HuggingFace Inference API** - Hosted model endpoints (free tier available)

**Characteristics:**
- Pay-per-token pricing
- No local infrastructure required
- Managed scaling and availability
- Access to latest/proprietary models
- Network latency for each request

### Local Inference Engines
Self-hosted inference engines running open-source models:
- **Ollama** - Optimized local inference (Llama, Mistral, Qwen, etc.) ✅ **Implemented**
- **vLLM** - High-performance inference with PagedAttention ✅ **Implemented**
- **Text Generation Inference (TGI)** - HuggingFace's production inference server ✅ **Implemented**
- **Ray Serve** - Scalable ML model serving framework ✅ **Implemented**
- **BentoML** - ML serving platform with auto-scaling ✅ **Implemented**

**Characteristics:**
- Free usage (compute costs only)
- Full data privacy (no external API calls)
- Lower latency (local network)
- Control over model selection and updates
- Requires infrastructure setup and maintenance

### Integration Pattern

```
┌────────────────────────────────────────────────────────┐
│                  LLMProviderFactory                    │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────────────┐      ┌─────────────────────┐   │
│  │  Cloud Providers │      │ Inference Engines   │   │
│  │                  │      │                     │   │
│  │  • Anthropic     │      │  • Ollama           │   │
│  │  • OpenAI        │      │  • vLLM             │   │
│  │  • HuggingFace   │      │  • TGI              │   │
│  │    Inference API │      │  • Ray Serve        │   │
│  └────────┬─────────┘      └──────────┬──────────┘   │
│           │                           │              │
│           └───────────┬───────────────┘              │
│                       │                              │
│              ┌────────▼────────┐                     │
│              │  LLMProvider    │                     │
│              │   Interface     │                     │
│              └─────────────────┘                     │
└────────────────────────────────────────────────────────┘
```

**Provider Selection Logic:**
1. Primary provider configured via `llm.primary-provider`
2. Automatic failover to secondary providers if primary unavailable
3. Health checks determine provider availability
4. Default order: Anthropic → OpenAI → Ollama → vLLM → TGI → Ray Serve → BentoML → HuggingFace
5. Supported providers: `anthropic`, `openai`, `ollama`, `vllm`, `tgi`, `rayserve`, `bentoml`, `huggingface`

See [Local Setup Guide](../4-development/local-setup-guide.md) for detailed inference engine comparison and setup instructions.

---

## Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.5
- **DI**: Spring Framework
- **LLM Integration**: Direct REST API (WebClient)
- **CLI**: Spring Shell
- **Config**: YAML (application.yml + agent configs)
- **Logging**: SLF4J + Logback
- **Testing**: JUnit 5, Mockito

---

## Configuration

### application.yml
```yaml
role-manager:
  agents:
    config-dir: config/agents
    reload-on-change: true

llm:
  default-provider: anthropic
  timeout-seconds: 30
  max-retries: 3

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: claude-3-5-sonnet-20241022

openai:
  api-key: ${OPENAI_API_KEY}
  model: gpt-4-turbo-preview
```

---

## Deployment Architecture

```
┌──────────────┐
│ CLI Client   │
└──────┬───────┘
       │ HTTP/REST
┌──────▼────────────────────────────────────────┐
│  Role Manager App (Spring Boot)               │
│  ┌─────────────────────────────────────────┐  │
│  │  AgentRegistry - 13 Agents              │  │
│  └──────────────────┬──────────────────────┘  │
│                     │                          │
│  ┌──────────────────▼──────────────────────┐  │
│  │     LLMProviderFactory                  │  │
│  └──────────────────┬──────────────────────┘  │
└───────────────────┬─┬────────────────────────┘
                    │ │
         ┌──────────┘ └──────────┐
         │                       │
┌────────▼─────────┐  ┌──────────▼─────────────┐
│ Cloud Providers  │  │ Inference Engines      │
│ - Anthropic API  │  │ - Ollama (ade-srv)     │
│ - OpenAI API     │  │ - vLLM                 │
│ - HuggingFace    │  │ - TGI                  │
│   Inference API  │  │ - Ray Serve            │
└──────────────────┘  └────────────────────────┘
```

---

## Security Considerations

1. **API Key Management**: Store in environment variables, never in code
2. **Input Validation**: Sanitize all user inputs before LLM prompts
3. **Output Sanitization**: Remove any credentials from LLM outputs
4. **Rate Limiting**: Prevent abuse of LLM APIs
5. **Audit Logging**: Log all agent interactions for security review

---

## Scalability

- **Stateless Design**: Each agent execution is independent
- **Caching**: Cache common LLM responses
- **Async Execution**: Multi-agent tasks run in parallel
- **Resource Limits**: Configurable limits on concurrent agents

---

## References

1. [Requirements](../1-planning/requirements.md)
2. [API Design](api-design.md)
3. [Data Model](data-model.md)
4. [Workflow Diagrams](workflow.md) - Process sequences and timing
5. [Data Flow Diagrams](dataflow.md) - Data transformations
6. [Workflow vs Data Flow Guide](../4-development/guide/workflow-vs-dataflow.md) - Understanding the difference

---

*Last Updated: 2025-10-17*
