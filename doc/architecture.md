# Architecture - ade-agent-platform

**Date:** 2025-10-21
**Version:** 0.2.0

## TL;DR

**Framework-agnostic multi-agent platform** with pluggable integrations. **Core principle**: Business logic in framework-agnostic POJOs → Integration modules wrap with DI/features. **Zero duplication**: Spring Boot, Quarkus, Micronaut all reuse the same core. **Decision rule**: Need vanilla Java → use core only. Need framework features (DI, REST, CLI) → add integration module.

---

## Table of Contents

- [Overview](#overview)
- [Architectural Principles](#architectural-principles)
- [Module Structure](#module-structure)
- [Core Module (Framework-Agnostic)](#core-module-framework-agnostic)
- [Integration Modules](#integration-modules)
  - [Spring Boot Integration](#spring-boot-integration)
  - [Quarkus Integration](#quarkus-integration)
  - [Micronaut Integration](#micronaut-integration)
- [Dependency Flow](#dependency-flow)
- [Code Reuse Pattern](#code-reuse-pattern)
- [Usage Examples](#usage-examples)
- [Migration Path](#migration-path)

---

## Overview

The ade-agent-platform is designed as a **framework-agnostic core** with optional **framework integration modules**. This architecture allows users to:

1. **Use vanilla Java** - Pure POJOs, no framework dependencies
2. **Use Spring Boot** - Auto-configuration, DI, REST APIs, CLI
3. **Use Quarkus** - Native compilation, fast startup, CDI
4. **Use Micronaut** - Compile-time DI, GraalVM support, low memory

All frameworks **reuse the exact same business logic** from the core module. There is **zero code duplication**.

---

## Architectural Principles

### 1. Framework-Agnostic Core

**Principle**: All business logic resides in POJOs with no framework dependencies.

**Benefits**:
- ✅ Works with any framework (or no framework)
- ✅ Fast compilation (no annotation processing overhead)
- ✅ Easy testing (no framework context required)
- ✅ Future-proof (survives framework changes)

**Implementation**:
```java
// Core POJO - no framework annotations
public class AgentRegistry {
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
    }
}
```

### 2. Integration Modules as Thin Wrappers

**Principle**: Framework modules only provide DI wiring and framework-specific features.

**Pattern**:
```
User Code → Framework DI Container → Core POJOs
```

**What integration modules add**:
- Dependency injection configuration
- REST API endpoints
- CLI commands
- Framework-specific features (metrics, health checks, etc.)

**What integration modules do NOT contain**:
- ❌ Business logic (that's in core)
- ❌ Duplicate implementations
- ❌ Core algorithm reimplementations

### 3. Dependency Inversion

**Principle**: Integration modules depend on core; core knows nothing about frameworks.

```
┌─────────────────────────┐
│  Integration Modules    │
│  (Spring, Quarkus, etc) │
│         │               │
│         ▼               │
│    [depends on]         │
│         │               │
│         ▼               │
│   Core Module (POJOs)   │
│   [knows nothing about  │
│    frameworks]          │
└─────────────────────────┘
```

**Maven enforcement**: Core POM has zero framework dependencies.

---

## Module Structure

```
ade-agent-platform/
├── pom.xml                              # Parent POM (aggregator)
├── ade-agent-platform-core/             # Framework-agnostic core
│   ├── pom.xml                          # NO framework dependencies
│   └── src/main/java/
│       └── dev.adeengineer.platform/
│           ├── core/                    # Core business logic (24 classes)
│           ├── providers/               # Provider implementations
│           ├── orchestration/           # Workflow engine
│           ├── template/                # Prompt templates
│           └── factory/                 # Factory classes
├── ade-agent-platform-spring-boot/      # Spring Boot integration
│   ├── pom.xml                          # Depends on core
│   └── src/main/java/
│       └── dev.adeengineer.platform.spring/
│           ├── config/                  # Auto-configuration
│           ├── controller/              # REST endpoints
│           ├── shell/                   # CLI commands
│           └── AdePlatformApplication.java
├── ade-agent-platform-quarkus/          # Quarkus integration
│   ├── pom.xml                          # Depends on core
│   └── src/main/java/
│       └── dev.adeengineer.platform.quarkus/
│           ├── config/                  # CDI producers
│           └── resource/                # JAX-RS endpoints
├── ade-agent-platform-micronaut/        # Micronaut integration
│   ├── pom.xml                          # Depends on core
│   └── src/main/java/
│       └── dev.adeengineer.platform.micronaut/
│           ├── factory/                 # Bean factories
│           └── controller/              # HTTP controllers
└── examples/
    ├── vanilla-java/                    # Pure Java example
    ├── spring-boot-example/             # Spring Boot usage
    ├── quarkus-example/                 # Quarkus usage
    └── micronaut-example/               # Micronaut usage
```

---

## Core Module (Framework-Agnostic)

### Purpose

Provides all business logic as pure POJOs that can be instantiated directly with `new`.

### Key Components

| Package | Classes | Purpose |
|---------|---------|---------|
| `core` | 5 classes | Agent registry, domain loading, role management |
| `providers` | 5 classes | Memory, storage, tools, orchestration, evaluation |
| `orchestration` | 4 classes | Parallel execution, workflow engine |
| `template` | 1 class | Prompt template processing |
| `factory` | 2 classes | LLM provider factories |
| `model` | 7 classes | Domain models, metrics, statistics |

### Dependencies (All Framework-Agnostic)

- **Jackson**: JSON/YAML processing
- **Reactor**: Reactive streams
- **Caffeine**: In-memory caching
- **SLF4J**: Logging facade
- **ade-agent-sdk**: Core agent abstractions

**Critical**: NO Spring, NO Quarkus, NO Micronaut in core.

### Example: Direct Instantiation

```java
// Pure Java - no framework
AgentRegistry registry = new AgentRegistry();
OutputFormatterRegistry formatter = new OutputFormatterRegistry();
DomainLoader loader = new DomainLoader(registry, formatter);

LLMProvider llm = new OpenAIProvider(apiKey);
int agentCount = loader.loadAllDomains("./domains", llm);
```

---

## Integration Modules

### Spring Boot Integration

**Module**: `ade-agent-platform-spring-boot`

**Provides**:
- Auto-configuration via `@EnableAutoConfiguration`
- Bean definitions for all core components
- REST API endpoints (`/api/agents/**`)
- Spring Shell CLI commands
- Actuator health checks and metrics
- Redis caching support
- Resilience4j circuit breakers

**Pattern**: Auto-configuration wraps core POJOs

```java
@Configuration
public class ProvidersAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AgentRegistry.class)
    public AgentRegistry agentRegistry() {
        return new AgentRegistry();  // Core POJO
    }

    @Bean
    @ConditionalOnMissingBean(DomainLoader.class)
    public DomainLoader domainLoader(
            AgentRegistry registry,           // Spring injects
            OutputFormatterRegistry formatter) {
        return new DomainLoader(registry, formatter);  // Core POJO
    }
}
```

**User Code**:
```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

@Service
public class MyService {
    @Autowired
    private AgentRegistry registry;  // Auto-wired

    public void execute() {
        Agent agent = registry.getAgent("developer");
        String result = agent.execute("Write Java code");
    }
}
```

---

### Quarkus Integration

**Module**: `ade-agent-platform-quarkus`

**Provides**:
- CDI producers for core components
- JAX-RS REST endpoints
- Native image support (GraalVM)
- Quarkus health checks
- Fast startup time (<1 second)
- Low memory footprint

**Pattern**: CDI producers wrap core POJOs

```java
@ApplicationScoped
public class PlatformProducers {

    @Produces
    @Singleton
    public AgentRegistry agentRegistry() {
        return new AgentRegistry();  // Core POJO
    }

    @Produces
    @Singleton
    public DomainLoader domainLoader(
            AgentRegistry registry,
            OutputFormatterRegistry formatter) {
        return new DomainLoader(registry, formatter);  // Core POJO
    }
}
```

**User Code**:
```java
@Path("/api/agents")
@ApplicationScoped
public class AgentResource {

    @Inject
    AgentRegistry registry;  // CDI injection

    @POST
    @Path("/{agentName}/execute")
    public Response execute(
            @PathParam("agentName") String agentName,
            AgentTask task) {
        Agent agent = registry.getAgent(agentName);
        String result = agent.execute(task.getDescription());
        return Response.ok(result).build();
    }
}
```

**Benefits**:
- ✅ Native executable: 50MB instead of 150MB JAR
- ✅ Startup: 0.8s instead of 3-5s (Spring Boot)
- ✅ Memory: 30MB instead of 100MB+
- ✅ Same business logic as Spring Boot (reuses core)

---

### Micronaut Integration

**Module**: `ade-agent-platform-micronaut`

**Provides**:
- Compile-time dependency injection
- HTTP server and client
- Native image support (GraalVM)
- Micronaut management endpoints
- Fast startup and low memory usage

**Pattern**: Factory beans wrap core POJOs

```java
@Factory
public class PlatformFactory {

    @Singleton
    public AgentRegistry agentRegistry() {
        return new AgentRegistry();  // Core POJO
    }

    @Singleton
    public DomainLoader domainLoader(
            AgentRegistry registry,
            OutputFormatterRegistry formatter) {
        return new DomainLoader(registry, formatter);  // Core POJO
    }
}
```

**User Code**:
```java
@Controller("/api/agents")
public class AgentController {

    private final AgentRegistry registry;

    @Inject
    public AgentController(AgentRegistry registry) {
        this.registry = registry;
    }

    @Post("/{agentName}/execute")
    public HttpResponse<String> execute(
            @PathVariable String agentName,
            @Body AgentTask task) {
        Agent agent = registry.getAgent(agentName);
        String result = agent.execute(task.getDescription());
        return HttpResponse.ok(result);
    }
}
```

**Benefits**:
- ✅ Compile-time DI: No reflection overhead
- ✅ Startup: ~1s
- ✅ Memory: 40MB
- ✅ Same business logic as Spring Boot and Quarkus (reuses core)

---

## Dependency Flow

### Maven Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│            ade-agent-platform-parent                │
│                  (parent POM)                       │
└───────────────────┬─────────────────────────────────┘
                    │
        ┌───────────┼───────────┬──────────────┐
        │           │           │              │
┌───────▼───────┐   │   ┌───────▼──────┐  ┌───▼────────────┐
│  Core Module  │   │   │   Quarkus    │  │   Micronaut    │
│   (POJOs)     │   │   │  Integration │  │   Integration  │
│               │   │   │     │        │  │       │        │
│ NO framework  │   │   │   depends on │  │   depends on   │
│ dependencies  │   │   │     core     │  │     core       │
└───────────────┘   │   └──────────────┘  └────────────────┘
                    │
            ┌───────▼────────┐
            │  Spring Boot   │
            │  Integration   │
            │       │        │
            │   depends on   │
            │     core       │
            └────────────────┘
```

### Critical Rule

**Integration modules can depend on core. Core CANNOT depend on integration modules.**

This ensures core remains framework-agnostic.

---

## Code Reuse Pattern

### Same Class, Different Lifecycle Management

```
┌──────────────────────────────────────────────────────┐
│            AgentRegistry.java (CORE)                 │
│         (SINGLE implementation)                      │
└────────┬─────────────┬──────────────┬────────────────┘
         │             │              │
   ┌─────▼──────┐ ┌────▼─────┐  ┌────▼──────┐
   │  Vanilla   │ │  Spring  │  │  Quarkus  │
   │   Java     │ │   Boot   │  │           │
   └────────────┘ └──────────┘  └───────────┘

Vanilla Java:           Spring Boot:              Quarkus:
─────────────          ─────────────             ─────────
main() {               @Bean                     @Produces
  registry = new       AgentRegistry             @Singleton
   AgentRegistry();    agentRegistry() {         AgentRegistry
                         return new              agentRegistry() {
  registry.               AgentRegistry();         return new
   registerAgent(...);  }                           AgentRegistry();
}                                                }

                       @Service                  @ApplicationScoped
                       class MyService {         class MyService {
                         @Autowired                @Inject
                         AgentRegistry              AgentRegistry
                          registry;                  registry;
                       }                         }
```

**Same code, different wiring.**

---

## Usage Examples

### 1. Vanilla Java (No Framework)

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-core</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

```java
// Direct instantiation
AgentRegistry registry = new AgentRegistry();
DomainLoader loader = new DomainLoader(registry, formatter);
loader.loadAllDomains("./domains", llmProvider);

Agent agent = registry.getAgent("developer");
String result = agent.execute("Write code");
```

---

### 2. Spring Boot

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-spring-boot</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

@Service
public class AgentService {
    @Autowired
    private AgentRegistry registry;  // Auto-configured

    public String executeAgent(String agentName, String task) {
        return registry.getAgent(agentName).execute(task);
    }
}
```

**application.yml**:
```yaml
ade:
  storage:
    path: ./data/storage
  domains:
    base-path: ./domains
```

---

### 3. Quarkus

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-quarkus</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

```java
@Path("/agents")
@ApplicationScoped
public class AgentResource {

    @Inject
    AgentRegistry registry;  // CDI injection

    @POST
    @Path("/{name}/execute")
    public String execute(@PathParam("name") String name, String task) {
        return registry.getAgent(name).execute(task);
    }
}
```

**Native image**:
```bash
./mvnw package -Pnative
./target/my-app-runner  # 50MB, starts in 0.8s
```

---

### 4. Micronaut

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-micronaut</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

```java
@Controller("/agents")
public class AgentController {

    private final AgentRegistry registry;

    @Inject
    public AgentController(AgentRegistry registry) {
        this.registry = registry;
    }

    @Post("/{name}/execute")
    public HttpResponse<String> execute(
            @PathVariable String name,
            @Body String task) {
        String result = registry.getAgent(name).execute(task);
        return HttpResponse.ok(result);
    }
}
```

---

## Migration Path

### From 0.1.x (Spring-Only) to 0.2.x (Multi-Framework)

**Old (0.1.x)**:
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform</artifactId>
    <version>0.1.0</version>
</dependency>
```

**New (0.2.x)** - Choose one:

**Option 1: Spring Boot** (most similar to 0.1.x):
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-spring-boot</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

**Option 2: Vanilla Java** (if you don't need Spring):
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-core</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

**Option 3: Quarkus** (native images, fast startup):
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-quarkus</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

**Option 4: Micronaut** (compile-time DI, low memory):
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-micronaut</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

**Package name changes**:
- `dev.adeengineer.platform.config.*` → `dev.adeengineer.platform.spring.config.*`
- `dev.adeengineer.platform.controller.*` → `dev.adeengineer.platform.spring.controller.*`
- Core classes unchanged: `dev.adeengineer.platform.core.*`

See `MIGRATION.md` for detailed migration steps.

---

## Summary: Framework Comparison

| Aspect | Vanilla Java | Spring Boot | Quarkus | Micronaut |
|--------|--------------|-------------|---------|-----------|
| **Dependency** | core | spring-boot | quarkus | micronaut |
| **DI Container** | Manual | Spring | CDI | Micronaut |
| **Startup Time** | Instant | 3-5s | 0.8s | 1s |
| **Memory Usage** | Minimal | 100MB+ | 30MB | 40MB |
| **JAR Size** | 10MB | 50MB | 10MB | 15MB |
| **Native Image** | N/A | Limited | Yes (GraalVM) | Yes (GraalVM) |
| **REST API** | Manual | Auto | JAX-RS | HTTP |
| **CLI** | Manual | Spring Shell | Picocli | Manual |
| **Business Logic** | ✅ Core | ✅ Core (reused) | ✅ Core (reused) | ✅ Core (reused) |
| **Code Duplication** | N/A | ❌ None | ❌ None | ❌ None |

**Key Takeaway**: All frameworks reuse the same business logic. Choose based on your deployment requirements.

---

**Last Updated:** 2025-10-21
