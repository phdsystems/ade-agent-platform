# ade-agent-platform Architecture

## Purpose

This module provides **infrastructure provider implementations** for the AI Agent SDK. It is a **library module** that should have NO dependency on specific inference implementations like `inferencestr8a`.

## Architectural Principles

### 1. Dependency Inversion

```
ade-agent-sdk (interfaces)
    ↑         ↑
    |         |
Platform   inferencestr8a
  (infra)    (inference)
```

- **Platform** depends ONLY on SDK interfaces
- **inferencestr8a** depends ONLY on SDK interfaces
- Applications compose both at runtime

### 2. Provider Types

**This module (ade-agent-platform) implements:**
- ✅ `MemoryProvider` - InMemoryMemoryProvider
- ✅ `StorageProvider` - LocalStorageProvider
- ✅ `ToolProvider` - SimpleToolProvider
- ✅ `OrchestrationProvider` - SimpleOrchestrationProvider
- ✅ `EvaluationProvider` - LLMEvaluationProvider

**Other modules implement:**
- `LLMProvider` - inferencestr8a (VLLM, OpenAI, Anthropic, etc.)
- `EmbeddingsProvider` - inferencestr8a
- `ObservabilityProvider` - inferencestr8a

### 3. Factory Pattern

**Location:** `dev.adeengineer.platform.factory.LLMProviderFactory`

**Status:** Temporary abstraction (should move to SDK)

The factory interface allows applications to create providers dynamically without depending on concrete implementations.

**Current Implementation:**
- `NoOpLLMProviderFactory` - Placeholder when no actual implementation is available
- Uses `@ConditionalOnMissingBean` to auto-disable when real factory is present

**Future:** When SDK adds factory interfaces, this package can be deprecated and removed.

## Dependencies

### Required (Core Library)
- `ade-agent` (SDK core interfaces)
- Project Reactor (reactive streams)
- SLF4J (logging facade)
- Lombok (code generation)

### Optional (Spring Boot Integration)
- `ade-agent-async` (SDK async support)
- `ade-agent-composition` (SDK composition)
- `ade-agent-monitoring` (SDK monitoring)
- Spring Boot (framework) - **OPTIONAL** for auto-configuration
- Jackson (JSON serialization) - for LocalStorageProvider

### NOT Required (Test Scope Only)
- ❌ `inferencestr8a-core` - **Test scope only**, not a production dependency

## Testing Strategy

###Unit Tests
- ✅ Provider implementation tests (92 tests)
- ✅ Mock external dependencies (EmbeddingsProvider, LLMProvider)
- ✅ No dependency on inferencestr8a

### Integration/E2E Tests
- ⚠️ Currently disabled (require inferencestr8a)
- Located in `src/test/java/.../integration/` and `src/test/java/.../e2e/`
- **To enable:** Add inferencestr8a dependency and remove `@Disabled` annotations

**Test Exclusions (pom.xml):**
```xml
<excludes>
    <exclude>**/*E2ETest.java</exclude>
    <exclude>**/integration/*ProviderFailoverTest.java</exclude>
    <exclude>**/integration/*Integration*.java</exclude>
</excludes>
```

## Usage Patterns

### Framework-Agnostic (Pure Java)

Providers are pure POJOs and can be instantiated directly in any Java application:

```java
// No Spring required!
import dev.adeengineer.platform.providers.memory.InMemoryMemoryProvider;
import dev.adeengineer.platform.providers.tools.SimpleToolProvider;

public class MyApplication {
    public static void main(String[] args) {
        // Manual instantiation
        EmbeddingsProvider embeddings = ...; // your embeddings implementation
        MemoryProvider memory = new InMemoryMemoryProvider(embeddings);
        ToolProvider tools = new SimpleToolProvider();

        // Use the providers
        memory.store(entry).subscribe();
    }
}
```

### Spring Boot Auto-Configuration

When used in a Spring Boot application, providers are auto-configured via `ProvidersAutoConfiguration`:

```java
@SpringBootApplication
public class MySpringApp {

    @Autowired
    private MemoryProvider memoryProvider;  // Auto-configured!

    @Autowired
    private ToolProvider toolProvider;      // Auto-configured!
}
```

**Configuration Properties:**
```yaml
# application.yml
ade:
  storage:
    path: ./data/storage  # LocalStorageProvider root directory
```

### Custom Provider Override

Applications can override default providers:

```java
@Configuration
public class CustomProvidersConfig {

    @Bean
    @Primary  // Overrides auto-configured provider
    public MemoryProvider customMemoryProvider() {
        return new RedisMemoryProvider(...);
    }
}
```

## Application Composition

Applications that want to use both infrastructure AND inference providers should depend on BOTH modules:

```xml
<!-- Application pom.xml -->
<dependencies>
    <!-- Infrastructure providers -->
    <dependency>
        <groupId>adeengineer.dev</groupId>
        <artifactId>ade-agent-platform</artifactId>
    </dependency>

    <!-- Inference providers -->
    <dependency>
        <groupId>dev.adeengineer</groupId>
        <artifactId>inferencestr8a-core</artifactId>
    </dependency>
</dependencies>
```

Then compose via Spring Boot auto-configuration:
```java
@Configuration
public class AppConfiguration {

    @Bean
    public MyAgent myAgent(
        LLMProvider llmProvider,          // from inferencestr8a
        MemoryProvider memoryProvider,    // from ade-agent-platform
        ToolProvider toolProvider         // from ade-agent-platform
    ) {
        return new MyAgent(llmProvider, memoryProvider, toolProvider);
    }
}
```

## Benefits of This Architecture

1. ✅ **Replaceability** - Swap inferencestr8a with OpenAI, Anthropic, etc.
2. ✅ **Modularity** - Use only the providers you need
3. ✅ **Clean Separation** - Infrastructure vs. inference concerns
4. ✅ **Testability** - Each module tests independently
5. ✅ **No Circular Dependencies** - Clean dependency flow

## Migration Path

**For existing applications using ade-agent-platform with inferencestr8a:**

1. Add `inferencestr8a-core` as an explicit dependency
2. Provide an `LLMProviderFactory` bean (or use one from inferencestr8a)
3. The `NoOpLLMProviderFactory` will automatically disable itself

**For SDK maintainers:**

1. Add factory interfaces to SDK (`LLMProviderFactory`, `EmbeddingsProviderFactory`, etc.)
2. Update inferencestr8a to implement these factories
3. Update ade-agent-platform to use SDK factories
4. Deprecate and remove `dev.adeengineer.platform.factory` package

---

*Last Updated: 2025-10-21*
*Version: 0.2.0*
