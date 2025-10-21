# Framework-Agnostic Migration Plan - ade-agent-platform

**Date:** 2025-10-21
**Version:** 1.0
**Status:** Planning

## TL;DR

**Goal**: Split ade-agent-platform into framework-agnostic core + optional framework integrations. **Key benefit**: Users choose Spring Boot, Quarkus, Micronaut, or vanilla Java → True infrastructure-only platform. **Timeline**: 6-10 hours implementation.

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Proposed Multi-Module Structure](#proposed-multi-module-structure)
3. [Module Dependencies](#module-dependencies)
4. [Implementation Steps](#implementation-steps)
5. [Usage Examples After Migration](#usage-examples-after-migration)
6. [Migration Impact](#migration-impact)
7. [Next Steps](#next-steps)

---

## Current State Analysis

**Total Files:** 39 Java files in `src/main`
- **Framework-agnostic (19 files):** `providers/`, `core/`, `orchestration/`, `template/`, `util/`, `factory/`, `model/`, `finetuning/`
- **Spring-dependent (20 files):** `api/` (4), `cli/` (3), `config/` (6), main application (1), + scattered dependencies

**Tests:** 58 test files, 7 use `@SpringBootTest`

**Spring Dependencies:**
- `spring-boot-starter` (core)
- `spring-boot-starter-web` (REST API)
- `spring-boot-starter-webflux` (reactive)
- `spring-shell-starter` (CLI)
- `spring-boot-starter-cache` (caching)
- `spring-boot-starter-data-redis` (Redis)

**Key Insight:** Core provider implementations are already POJOs with no Spring dependencies (see `InMemoryMemoryProvider.java:35` - "pure POJO with no Spring dependencies").

---

## Proposed Multi-Module Structure

```
ade-agent-platform/                    # Parent POM
├── pom.xml                            # Parent aggregator
├── ade-agent-platform-core/           # Module 1: Framework-agnostic core
│   ├── pom.xml
│   └── src/
│       ├── main/java/dev/adeengineer/platform/
│       │   ├── providers/             # All provider implementations
│       │   │   ├── evaluation/        # LLMEvaluationProvider
│       │   │   ├── memory/            # InMemoryMemoryProvider
│       │   │   ├── orchestration/     # SimpleOrchestrationProvider
│       │   │   ├── storage/           # LocalStorageProvider
│       │   │   └── tools/             # SimpleToolProvider
│       │   ├── core/                  # AgentRegistry, DomainLoader, etc.
│       │   ├── orchestration/         # WorkflowEngine, ParallelExecutor
│       │   ├── template/              # PromptTemplateEngine
│       │   ├── factory/               # Provider factories
│       │   ├── model/                 # Data models
│       │   ├── finetuning/            # Fine-tuning services
│       │   └── util/                  # Utilities
│       └── test/                      # Unit tests (51 files - no Spring)
│
├── ade-agent-platform-spring-boot/    # Module 2: Spring Boot integration
│   ├── pom.xml
│   └── src/
│       ├── main/java/dev/adeengineer/platform/spring/
│       │   ├── config/                # Spring auto-configuration
│       │   │   ├── ProvidersAutoConfiguration.java
│       │   │   ├── AppConfig.java
│       │   │   ├── CacheConfig.java
│       │   │   ├── FormatterConfiguration.java
│       │   │   └── AgentConfigLoader.java
│       │   ├── api/                   # REST controllers
│       │   │   ├── DomainController.java
│       │   │   ├── RoleController.java
│       │   │   └── TaskController.java
│       │   ├── cli/                   # Spring Shell commands
│       │   │   ├── DomainCommands.java
│       │   │   ├── RoleManagerCommands.java
│       │   │   └── (other CLI commands)
│       │   └── AdePlatformApplication.java
│       └── test/                      # Integration tests (7 @SpringBootTest)
│
├── ade-agent-platform-quarkus/        # Module 3: Future Quarkus integration
│   └── (future implementation)
│
└── ade-agent-platform-micronaut/      # Module 4: Future Micronaut integration
    └── (future implementation)
```

---

## Module Dependencies

### ade-agent-platform-core

**Dependencies:**
- ade-agent SDK (core, async, composition, monitoring)
- Jackson (JSON/YAML processing)
- Reactor (reactive streams)
- Caffeine (in-memory caching)
- Lombok (code generation)
- **NO SPRING DEPENDENCIES**

**Provides:**
- All provider implementations (memory, storage, orchestration, evaluation, tools)
- Core platform logic (AgentRegistry, DomainLoader, DomainManager)
- Orchestration (WorkflowEngine, ParallelAgentExecutor)
- Template engine (PromptTemplateEngine)
- Factories (LLMProviderFactory, NoOpLLMProviderFactory)
- Models and utilities

### ade-agent-platform-spring-boot

**Dependencies:**
- `ade-agent-platform-core` (required)
- Spring Boot starters (web, webflux, cache, data-redis)
- Spring Shell

**Provides:**
- Auto-configuration for all providers as Spring beans
- REST API controllers
- CLI commands via Spring Shell
- Spring-specific configuration (caching, Redis, etc.)

---

## Implementation Steps

### Phase 1: Create Multi-Module Structure

#### Step 1: Create Parent POM

**File:** `pom.xml` (root - rename current to `pom.xml.old`)

**Contents:**
- Define modules: `core`, `spring-boot`
- Shared dependency management (versions, common dependencies)
- Common build configuration (compiler settings, plugins)
- Packaging: `pom`

#### Step 2: Create `ade-agent-platform-core/` Module

**Actions:**
1. Create directory structure: `ade-agent-platform-core/src/main/java/`
2. Create `pom.xml` with framework-agnostic dependencies
3. Move packages:
   - `providers/` (all 5 subdirectories)
   - `core/` (6 files)
   - `orchestration/` (3 files)
   - `template/` (2 files)
   - `util/` (utilities)
   - `factory/` (provider factories)
   - `model/` (data models)
   - `finetuning/` (fine-tuning services)
4. Move unit tests (51 files without `@SpringBootTest`)
5. **CRITICAL:** Remove all Spring imports from moved files

**Dependencies to include:**
- ade-agent SDK modules
- Jackson (databind, datatype-jsr310, dataformat-yaml)
- Reactor (reactor-netty)
- Caffeine (caching)
- Lombok
- OpenAI client
- Project Reactor

#### Step 3: Create `ade-agent-platform-spring-boot/` Module

**Actions:**
1. Create directory structure: `ade-agent-platform-spring-boot/src/main/java/`
2. Create `pom.xml` with Spring Boot dependencies
3. Move packages:
   - `config/` → `spring/config/` (6 files)
   - `api/` → `spring/api/` (4 files)
   - `cli/` → `spring/cli/` (3 files)
   - `AdePlatformApplication.java` → `spring/`
4. Move integration tests (7 files with `@SpringBootTest`)
5. Add dependency on `ade-agent-platform-core`

**Dependencies to include:**
- `ade-agent-platform-core`
- All current Spring Boot starters
- Spring Shell

### Phase 2: Update Code

#### Step 4: Update Package Names

**Package Renaming (Spring Boot module only):**
```
dev.adeengineer.platform.config → dev.adeengineer.platform.spring.config
dev.adeengineer.platform.api → dev.adeengineer.platform.spring.api
dev.adeengineer.platform.cli → dev.adeengineer.platform.spring.cli
```

**Core module:** Keep existing package names (`dev.adeengineer.platform.*`)

#### Step 5: Fix Imports and References

**In Spring Boot module:**
- Update all imports to reference core classes
- Update `@ComponentScan` base packages
- Update REST controller paths if needed
- Ensure no circular dependencies

**Validation:**
- Core module MUST NOT import anything from `dev.adeengineer.platform.spring.*`
- Spring Boot module CAN import from `dev.adeengineer.platform.*`

#### Step 6: Update Spring Auto-Configuration

**File:** `spring-boot/src/main/java/dev/adeengineer/platform/spring/config/ProvidersAutoConfiguration.java`

**Changes:**
1. Add `@ConditionalOnClass` for optional dependencies:
   ```java
   @Bean
   @ConditionalOnMissingBean(MemoryProvider.class)
   @ConditionalOnBean(EmbeddingsProvider.class)
   public MemoryProvider inMemoryMemoryProvider(EmbeddingsProvider embeddingsProvider) {
       // ...
   }
   ```

2. Add `@ConditionalOnClass` for Spring-specific features:
   ```java
   @ConditionalOnClass(name = "org.springframework.cache.CacheManager")
   ```

3. Make all beans truly optional with graceful degradation

**Create:** `spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
```
dev.adeengineer.platform.spring.config.ProvidersAutoConfiguration
dev.adeengineer.platform.spring.config.CacheConfig
dev.adeengineer.platform.spring.config.FormatterConfiguration
```

### Phase 3: Test & Validate

#### Step 7: Test Core Module Independently

**Actions:**
1. Run unit tests: `mvn test -pl ade-agent-platform-core`
2. Verify no Spring classes on classpath
3. Create vanilla Java usage example (see below)
4. Confirm zero Spring dependencies: `mvn dependency:tree -pl ade-agent-platform-core | grep spring`

**Expected result:** All tests pass, no Spring dependencies found

#### Step 8: Test Spring Boot Integration

**Actions:**
1. Run integration tests: `mvn test -pl ade-agent-platform-spring-boot`
2. Verify auto-configuration works
3. Test backward compatibility with existing applications
4. Run full build: `mvn clean install`

**Expected result:** All tests pass, Spring Boot application starts successfully

#### Step 9: Create Usage Examples

**Create:** `examples/vanilla-java/VanillaJavaExample.java`
```java
// Demonstrates using platform without any framework
public class VanillaJavaExample {
    public static void main(String[] args) {
        // Instantiate providers directly
        EmbeddingsProvider embeddings = new MyEmbeddingsImpl();
        MemoryProvider memory = new InMemoryMemoryProvider(embeddings);
        LLMProvider llm = new MyLLMImpl();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        StorageProvider storage = new LocalStorageProvider("./data", mapper);

        AgentRegistry registry = new AgentRegistry();
        DomainLoader loader = new DomainLoader(registry, storage);

        // Use platform - no Spring required!
    }
}
```

**Create:** `examples/spring-boot/SpringBootExample.java`
```java
// Demonstrates Spring Boot auto-configuration
@SpringBootApplication
public class SpringBootExample {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootExample.class, args);
    }

    @Service
    public static class MyService {
        @Autowired private AgentRegistry registry;
        @Autowired private MemoryProvider memory;
        // Everything auto-configured by Spring!
    }
}
```

### Phase 4: Documentation & Migration

#### Step 10: Update Documentation

**Update:** `README.md`
- Explain multi-module structure
- Add "Quick Start" for vanilla Java users
- Add "Quick Start" for Spring Boot users
- Document migration path for existing users

**Create:** `doc/migration-guide.md`
- Breaking changes (Maven coordinates)
- Step-by-step migration instructions
- Troubleshooting common issues

**Update:** `doc/3-design/architecture.md`
- Add module architecture diagram
- Document framework abstraction layer
- Explain provider instantiation patterns

#### Step 11: Update Build Configuration

**Actions:**
1. Configure Maven deployment for both artifacts
2. Ensure version synchronization (both modules released together)
3. Update CI/CD to build both modules
4. Configure BOM (Bill of Materials) for easy version management

---

## Usage Examples After Migration

### Vanilla Java (No Framework)

**Maven dependency:**
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-core</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Java code:**
```java
// Direct instantiation - no DI framework needed
EmbeddingsProvider embeddings = new MyEmbeddingsImpl();
MemoryProvider memory = new InMemoryMemoryProvider(embeddings);
LLMProvider llm = new MyLLMImpl();

ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());
StorageProvider storage = new LocalStorageProvider("./data", mapper);

AgentRegistry registry = new AgentRegistry();
DomainLoader loader = new DomainLoader(registry, storage);

// Use platform directly
int agentCount = loader.loadAllDomains("./domains", llm);
```

### Spring Boot

**Maven dependency:**
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-spring-boot</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Configuration:**
```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

@Service
public class MyService {
    @Autowired private AgentRegistry registry;
    @Autowired private MemoryProvider memory;
    @Autowired private DomainLoader loader;

    // All providers auto-configured!
    public void doSomething() {
        registry.getAllAgents();
    }
}
```

**Override providers:**
```java
@Configuration
public class CustomConfig {
    @Bean
    public MemoryProvider customMemoryProvider() {
        // Spring will use this instead of default
        return new MyCustomMemoryProvider();
    }
}
```

### Quarkus (Future)

**Maven dependency:**
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-quarkus</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Configuration:**
```java
@ApplicationScoped
public class QuarkusConfig {
    @Produces
    public MemoryProvider memoryProvider(EmbeddingsProvider embeddings) {
        return new InMemoryMemoryProvider(embeddings);
    }
}
```

### Micronaut (Future)

**Maven dependency:**
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-micronaut</artifactId>
    <version>0.2.0</version>
</dependency>
```

---

## Migration Impact

### Breaking Changes

**Maven Coordinates:**
- **Old:** `adeengineer.dev:ade-agent-platform:0.2.0`
- **New (Spring Boot users):** `adeengineer.dev:ade-agent-platform-spring-boot:0.2.0`
- **New (Vanilla Java users):** `adeengineer.dev:ade-agent-platform-core:0.2.0`

**Package Names (Spring Boot only):**
- Old: `dev.adeengineer.platform.config.*`
- New: `dev.adeengineer.platform.spring.config.*`

**Note:** Core package names remain unchanged (`dev.adeengineer.platform.providers.*`, etc.)

### Migration Steps for Existing Users

**Spring Boot applications:**
1. Update `pom.xml`:
   ```xml
   <!-- OLD -->
   <dependency>
       <groupId>adeengineer.dev</groupId>
       <artifactId>ade-agent-platform</artifactId>
   </dependency>

   <!-- NEW -->
   <dependency>
       <groupId>adeengineer.dev</groupId>
       <artifactId>ade-agent-platform-spring-boot</artifactId>
   </dependency>
   ```

2. Update imports (if using config classes directly):
   ```java
   // OLD
   import dev.adeengineer.platform.config.AppConfig;

   // NEW
   import dev.adeengineer.platform.spring.config.AppConfig;
   ```

3. No changes needed for provider usage (those packages unchanged)

**Vanilla Java applications (new capability):**
- Can now use `ade-agent-platform-core` without any Spring dependencies!

### Benefits

✅ **Framework flexibility**: Choose Spring Boot, Quarkus, Micronaut, or no framework
✅ **Smaller dependencies**: Core module has no Spring jars
✅ **Easier testing**: Core is pure Java, easier to unit test
✅ **True infrastructure-only**: No forced framework choice
✅ **Future-proof**: Easy to add Quarkus/Micronaut support
✅ **Better separation**: Clear distinction between platform logic and framework integration
✅ **Lower barrier to entry**: Can use platform without learning Spring

### Effort Estimate

| Task | Time |
|------|------|
| File movement & directory structure | 2-3 hours |
| POM configuration (parent + 2 modules) | 1-2 hours |
| Import updates & package renaming | 1 hour |
| Testing & validation | 2-3 hours |
| Documentation updates | 1-2 hours |
| **Total** | **7-11 hours** |

---

## Next Steps

### Immediate Actions

1. ✅ Save this plan to `doc/framework-agnostic-migration-plan.md`
2. ✅ Create feature branch: `feature/framework-agnostic`
3. ⏳ Create parent POM structure
4. ⏳ Create `ade-agent-platform-core` module
5. ⏳ Create `ade-agent-platform-spring-boot` module

### Follow-up Actions

6. Update code (imports, packages, configurations)
7. Test both modules independently
8. Create usage examples
9. Update documentation
10. Merge to main after validation

### Future Enhancements

- Add Quarkus integration module
- Add Micronaut integration module
- Create Spring Boot starter BOM
- Performance benchmarks (vanilla vs Spring)

---

## References

- **Project Documentation:** `/home/developer/ade-agent-platform/doc/`
- **Current POM:** `/home/developer/ade-agent-platform/pom.xml`
- **Provider Implementations:** `/home/developer/ade-agent-platform/src/main/java/dev/adeengineer/platform/providers/`
- **Spring Configuration:** `/home/developer/ade-agent-platform/src/main/java/dev/adeengineer/platform/config/`

---

*Last Updated: 2025-10-21*
