# Spring Profiles and E2E Testing Guide

**Date:** 2025-10-18
**Version:** 1.0

---

## TL;DR

**Spring component scanning + complex bean initialization = autowiring conflicts.** Solution: `@Profile("!e2etest")` on agent classes prevents autowiring during E2E tests while allowing normal production initialization. **Key concept**: Production uses `ApplicationRunner` for YAML-based initialization; E2E tests use `@TestConfiguration` for manual bean creation with mocked dependencies. **Quick rule**: If beans have complex initialization (file loading, external dependencies), use profiles to separate production vs test bean creation.

**Anti-pattern**: Don't let Spring auto-wire beans that require non-bean dependencies (like YAML-loaded configs). Use `@Profile` to conditionally disable autowiring.

---

## Table of Contents

- [Overview](#overview)
- [The Problem: Component Scanning Conflict](#the-problem-component-scanning-conflict)
- [Understanding Spring Concepts](#understanding-spring-concepts)
- [Production vs Test Environments](#production-vs-test-environments)
- [The Solution: @Profile Annotation](#the-solution-profile-annotation)
- [Complete E2E Test Implementation](#complete-e2e-test-implementation)
- [Why This Matters for E2E Testing](#why-this-matters-for-e2e-testing)
- [Visual Diagrams](#visual-diagrams)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)
- [References](#references)

---

## Overview

This guide explains a critical Spring Boot challenge encountered when implementing E2E tests for the Role Manager App: **how to prevent Spring component scanning from attempting to auto-wire beans that require complex initialization**.

**The Core Challenge**:
- Agent classes have `@Component` annotations (for production autowiring)
- Agents require `AgentConfig` objects (loaded from YAML files, not Spring beans)
- Spring component scanning tries to create agent beans immediately
- Tests fail because `AgentConfig` beans don't exist

**The Solution**:
- Use `@Profile("!e2etest")` to conditionally disable component scanning
- Manually create beans in test configuration with test-specific setup
- Separate production initialization from test initialization

---

## The Problem: Component Scanning Conflict

### What is Component Scanning?

Spring Boot automatically scans your codebase for classes with stereotype annotations:
- `@Component` - Generic Spring-managed bean
- `@Service` - Business logic layer
- `@Repository` - Data access layer
- `@Controller` / `@RestController` - Web layer

When Spring finds these annotations, it attempts to create instances (beans) and manage them in the application context.

**Example from Role Manager App**:
```java
@Component  // ← Spring sees this and says "I need to create this bean!"
public class DeveloperAgent extends BaseAgent {

    public DeveloperAgent(AgentConfig config,
                          LLMProvider llmProvider,
                          OutputFormatter outputFormatter) {
        super(config, llmProvider, outputFormatter);
    }
}
```

### The Autowiring Problem

When Spring tries to create the `DeveloperAgent` bean during context initialization:

**What Spring Needs**:
1. `AgentConfig` - **NOT a Spring bean!** Loaded from YAML files
2. `LLMProvider` - Spring bean (multiple implementations available)
3. `OutputFormatter` - Spring bean

**The Failure Cascade**:
```
[Spring Context Initialization]
  ↓
[Component Scanning finds @Component DeveloperAgent]
  ↓
[Attempts to create DeveloperAgent bean]
  ↓
[Analyzes constructor parameters]
  ↓
[Searches for AgentConfig bean]
  ↓
[ERROR: No qualifying bean of type 'AgentConfig' found]
  ↓
[ApplicationContext FAILED TO LOAD] ❌
  ↓
[All tests fail before they can run]
```

### Why This Happens

The problem is a **timing and design mismatch**:

1. **Component Scanning** happens during Spring context initialization (early phase)
2. **YAML Loading** happens in `ApplicationRunner` (late phase, after context is loaded)
3. Spring tries to create agents in step 1, but configs aren't available until step 2

**Production Code Design**:
```java
@Configuration
public class AppConfig {

    @Bean
    public ApplicationRunner agentInitializer(AgentRegistry registry, ...) {
        return args -> {
            // This runs AFTER context is fully initialized

            // 1. Load YAML files
            List<AgentConfig> configs = loader.loadConfigs("src/main/resources/agents/");

            // 2. Manually create agents (bypassing autowiring)
            for (AgentConfig config : configs) {
                AgentRole agent = createAgent(config, llmProvider, formatter);
                registry.registerAgent(agent);
            }
        };
    }
}
```

But `@Component` on agent classes tells Spring to try autowiring them **before** `ApplicationRunner` executes!

---

## Understanding Spring Concepts

### 1. Bean Lifecycle

```
Application Startup
    ↓
1. Component Scanning
   - Finds @Component, @Service, etc.
   - Creates bean definitions
    ↓
2. Dependency Resolution
   - Analyzes constructor parameters
   - Searches for matching beans
    ↓
3. Bean Instantiation
   - Creates bean instances
   - Injects dependencies
    ↓
4. Post-Processing
   - Applies @PostConstruct
   - Initializes beans
    ↓
5. Application Ready
   - Runs @EventListener(ApplicationReadyEvent)
   - Runs ApplicationRunner beans
    ↓
Application Running
```

**The Problem**: Agents need data from step 5 (YAML loading) but Spring tries to create them in step 3 (bean instantiation).

### 2. Autowiring Strategies

Spring can inject dependencies via:

**Constructor Injection** (recommended):
```java
@Component
public class DeveloperAgent {
    private final AgentConfig config;

    public DeveloperAgent(AgentConfig config) {  // Spring injects here
        this.config = config;
    }
}
```

**Field Injection**:
```java
@Component
public class DeveloperAgent {
    @Autowired
    private AgentConfig config;  // Spring injects here
}
```

**Setter Injection**:
```java
@Component
public class DeveloperAgent {
    private AgentConfig config;

    @Autowired
    public void setConfig(AgentConfig config) {  // Spring injects here
        this.config = config;
    }
}
```

All three fail if `AgentConfig` isn't a Spring bean!

### 3. Spring Profiles

Profiles allow conditional bean creation based on active environment:

**Common Use Cases**:
- `@Profile("dev")` - Only create in development
- `@Profile("prod")` - Only create in production
- `@Profile("test")` - Only create during testing
- `@Profile("!test")` - Create in all profiles EXCEPT test

**Profile Activation**:
```bash
# Via command line
java -jar app.jar --spring.profiles.active=prod

# Via environment variable
export SPRING_PROFILES_ACTIVE=prod

# Via application.properties
spring.profiles.active=prod

# In tests via annotation
@ActiveProfiles("e2etest")
```

---

## Production vs Test Environments

### Production Setup (Normal Application Startup)

**Problem in Production**:
Agents have `@Component` but need `AgentConfig` from YAML files.

**Why It Still Works**:
The production code uses a clever workaround - it **doesn't actually use** the `@Component` annotation for agent creation!

**Step 1: Agent Classes** (src/main/java/com/rolemanager/agents/)
```java
@Component  // Present but not actually used for bean creation!
@Profile("!e2etest")  // Only active when NOT in e2etest profile
public class DeveloperAgent extends BaseAgent {

    public DeveloperAgent(AgentConfig config,
                          LLMProvider llmProvider,
                          OutputFormatter outputFormatter) {
        super(config, llmProvider, outputFormatter);
    }
}
```

**Step 2: Configuration** (src/main/java/com/rolemanager/config/AppConfig.java)
```java
@Configuration
public class AppConfig {

    @Bean
    public ApplicationRunner agentInitializer(
            AgentRegistry registry,
            AgentConfigLoader loader,
            LLMProvider primaryProvider,
            OutputFormatter outputFormatter) {

        return args -> {
            // This runs AFTER Spring context is fully loaded

            // 1. Load agent configs from YAML files
            String configDir = env.getProperty("agents.config-dir", "src/main/resources/agents");
            List<AgentConfig> configs = loader.loadConfigs(configDir);

            // 2. Create agents manually (not via Spring autowiring)
            for (AgentConfig config : configs) {
                AgentRole agent = switch (config.role()) {
                    case "Software Developer" ->
                        new DeveloperAgent(config, primaryProvider, outputFormatter);
                    case "QA Engineer" ->
                        new QAAgent(config, primaryProvider, outputFormatter);
                    case "Security Engineer" ->
                        new SecurityAgent(config, primaryProvider, outputFormatter);
                    case "Engineering Manager" ->
                        new ManagerAgent(config, primaryProvider, outputFormatter);
                    default -> throw new IllegalArgumentException("Unknown role: " + config.role());
                };

                // 3. Register manually created agents
                registry.registerAgent(agent);
            }

            log.info("Initialized {} agents", configs.size());
        };
    }
}
```

**Why @Component is Present**:
Historical reasons or potential future refactoring. Currently, it doesn't affect production because `ApplicationRunner` creates agents manually.

**Why @Profile("!e2etest") is Critical**:
Without it, Spring would try to autowire agents during **E2E test startup**, causing failures.

### E2E Test Environment Requirements

E2E tests have unique needs:

**What E2E Tests Need**:
1. ✅ Full Spring Boot application context (real controllers, services)
2. ✅ Mocked LLM providers (no real API calls, fast execution)
3. ✅ Test-specific agent configurations (short role names like "Developer")
4. ✅ Isolated test data (doesn't affect production configs)
5. ✅ Fast startup time (< 30 seconds)

**What Breaks Without @Profile**:
```java
@SpringBootTest  // Start full application context
@ActiveProfiles("e2etest")  // Activate e2etest profile
class DeveloperAgentE2ETest {

    @MockBean
    private AnthropicProvider anthropicProvider;  // Mock LLM provider

    // Problem: Spring still tries to autowire DeveloperAgent!
    // @Component on DeveloperAgent triggers autowiring
    // Spring looks for AgentConfig bean → NOT FOUND
    // ApplicationContext FAILS TO LOAD ❌
}
```

**Error Message**:
```
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException:
No qualifying bean of type 'adeengineer.dev.platform.model.AgentConfig' available:
expected at least 1 bean which qualifies as autowire candidate.
```

---

## The Solution: @Profile Annotation

### Adding @Profile to Agent Classes

**Before** (broken for E2E tests):
```java
@Component
public class DeveloperAgent extends BaseAgent {
    // Spring tries to autowire this in ALL environments
    // Fails in E2E tests because AgentConfig bean doesn't exist
}
```

**After** (works in all environments):
```java
@Component
@Profile("!e2etest")  // ← Only autowire when NOT in e2etest profile
public class DeveloperAgent extends BaseAgent {
    // Production: @Profile("!e2etest") = true → bean created (then ignored)
    // E2E Test: @Profile("!e2etest") = false → bean NOT created ✅
}
```

### How @Profile Works

**Profile Evaluation**:
```java
@Profile("!e2etest")

// If active profile = "e2etest"
//   → !e2etest = false
//   → Bean is NOT created
//   → No autowiring attempted ✅

// If active profile = "dev" or "prod" or <default>
//   → !e2etest = true
//   → Bean is created (but not used due to manual registration)
```

**All Agent Classes Updated**:
```java
// DeveloperAgent.java
@Component
@Profile("!e2etest")
public class DeveloperAgent extends BaseAgent { }

// ManagerAgent.java
@Component
@Profile("!e2etest")
public class ManagerAgent extends BaseAgent { }

// QAAgent.java
@Component
@Profile("!e2etest")
public class QAAgent extends BaseAgent { }

// SecurityAgent.java
@Component
@Profile("!e2etest")
public class SecurityAgent extends BaseAgent { }
```

### Profile Negation Explained

**Syntax**:
- `@Profile("e2etest")` - Create bean ONLY when e2etest is active
- `@Profile("!e2etest")` - Create bean when e2etest is NOT active

**Examples**:
```java
@Profile("dev")          // Only in dev
@Profile("prod")         // Only in prod
@Profile("!prod")        // All except prod
@Profile("dev | test")   // In dev OR test
@Profile("!dev & !test") // Not in dev AND not in test
```

---

## Complete E2E Test Implementation

### Step 1: Activate E2E Profile in Tests

**BaseE2ETest.java**:
```java
package adeengineer.dev.platform.e2e;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2etest")  // ← Activates the e2etest profile
@TestPropertySource(locations = "classpath:application-e2etest.yml")
@Import(E2ETestConfiguration.class)  // Import test config
public abstract class BaseE2ETest {

    @MockBean
    protected AnthropicProvider anthropicProvider;  // Mock instead of real

    @MockBean
    protected OpenAIProvider openAIProvider;

    @MockBean
    protected OllamaProvider ollamaProvider;

    @BeforeEach
    void setUpBase() {
        // Configure mock LLM providers
        LLMResponse testResponse = new LLMResponse(
            "Test LLM response for E2E tests",
            new UsageInfo(25, 50, 75, 0.001),
            "test-provider",
            "test-model"
        );

        when(anthropicProvider.generate(anyString(), anyDouble(), anyInt()))
            .thenReturn(testResponse);
        when(anthropicProvider.isHealthy()).thenReturn(true);
    }
}
```

**What Happens**:
1. `@ActiveProfiles("e2etest")` activates the e2etest profile
2. `@Profile("!e2etest")` on agent classes evaluates to FALSE
3. Spring **skips** agent autowiring
4. No `AgentConfig` bean needed for autowiring ✅

### Step 2: Create Test Configuration

**E2ETestConfiguration.java**:
```java
package adeengineer.dev.platform.e2e;

import adeengineer.dev.platform.core.AgentRegistry;
import adeengineer.dev.platform.core.OutputFormatter;
import adeengineer.dev.platform.agents.*;
import adeengineer.dev.platform.llm.LLMProvider;
import adeengineer.dev.platform.model.AgentConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
@ComponentScan(
    basePackages = "adeengineer.dev.platform",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {DeveloperAgent.class, ManagerAgent.class, QAAgent.class, SecurityAgent.class}
    )
)
public class E2ETestConfiguration {

    /**
     * Provides a test AgentRegistry with manually created agents.
     * This avoids Spring autowiring issues with AgentConfig dependencies.
     */
    @Bean
    @Primary  // This bean takes precedence over any other AgentRegistry
    public AgentRegistry testAgentRegistry(
            @Qualifier("anthropicProvider") LLMProvider primaryProvider,
            OutputFormatter outputFormatter) {

        AgentRegistry registry = new AgentRegistry();

        // Create test agent configs with SHORT role names (easier for testing)
        AgentConfig developerConfig = new AgentConfig(
            "Developer",  // Short name instead of "Software Developer"
            "Test Software Developer agent for E2E testing",
            List.of("Code review", "Bug fixing", "Architecture design"),
            0.7,
            1000,
            "You are a {role}. Task: {task}",
            "technical"
        );

        AgentConfig managerConfig = new AgentConfig(
            "Manager",  // Short name instead of "Engineering Manager"
            "Test Engineering Manager agent for E2E testing",
            List.of("Team management", "Metrics reporting", "Sprint planning"),
            0.7,
            1000,
            "You are a {role}. Task: {task}",
            "executive"
        );

        AgentConfig qaConfig = new AgentConfig(
            "QA",  // Short name instead of "QA Engineer"
            "Test QA Engineer agent for E2E testing",
            List.of("Test planning", "Test case generation", "Quality assurance"),
            0.7,
            1000,
            "You are a {role}. Task: {task}",
            "technical"
        );

        AgentConfig securityConfig = new AgentConfig(
            "Security",  // Short name instead of "Security Engineer"
            "Test Security Engineer agent for E2E testing",
            List.of("Security analysis", "Vulnerability assessment", "Threat modeling"),
            0.7,
            1000,
            "You are a {role}. Task: {task}",
            "technical"
        );

        // Create and register agents MANUALLY (not via autowiring)
        registry.registerAgent(new DeveloperAgent(developerConfig, primaryProvider, outputFormatter));
        registry.registerAgent(new ManagerAgent(managerConfig, primaryProvider, outputFormatter));
        registry.registerAgent(new QAAgent(qaConfig, primaryProvider, outputFormatter));
        registry.registerAgent(new SecurityAgent(securityConfig, primaryProvider, outputFormatter));

        return registry;
    }
}
```

**Key Points**:
1. **@TestConfiguration** - Only loaded in test context
2. **@Primary** - This registry takes precedence
3. **@Qualifier("anthropicProvider")** - Resolves bean ambiguity (3 LLM provider mocks)
4. **Manual agent creation** - We create agents ourselves, bypassing autowiring
5. **Short role names** - Makes tests cleaner ("Developer" vs "Software Developer")

### Step 3: Configure Test Application Properties

**application-e2etest.yml**:
```yaml
spring:
  application:
    name: role-manager-app-e2e
  shell:
    interactive:
      enabled: false  # Disable Spring Shell for E2E tests

server:
  port: 0  # Random port to prevent conflicts

# LLM Provider Configuration (values don't matter, providers are mocked)
llm:
  primary-provider: test

  anthropic:
    api-key: test-anthropic-key-e2e
    model: claude-3-5-sonnet-20241022
    max-tokens: 2048
    temperature: 0.7

# Agent Configuration
agents:
  config-dir: src/test/resources/agents  # Not used, but required property

# Logging
logging:
  level:
    root: WARN
    adeengineer.dev.platform: DEBUG
    org.springframework.web: INFO

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### Step 4: Write E2E Tests

**DeveloperAgentE2ETest.java**:
```java
package adeengineer.dev.platform.e2e;

import adeengineer.dev.platform.model.TaskRequest;
import adeengineer.dev.platform.model.TaskResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeveloperAgentE2ETest extends BaseE2ETest {

    @Test
    void shouldExecuteCodeReviewTask() {
        // Arrange
        TaskRequest request = new TaskRequest(
            "Developer",  // Using short role name from test config
            "Review this code for potential bugs",
            Map.of("code", "public void process(String input) { System.out.println(input.length()); }")
        );

        // Act
        ResponseEntity<TaskResult> response = restTemplate.postForEntity(
            apiUrl("/tasks/execute"),
            request,
            TaskResult.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.roleName()).isEqualTo("Developer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }
}
```

---

## Why This Matters for E2E Testing

### 1. Test Isolation

**Without @Profile** (tests affect production):
```java
// Production and tests share the same bean creation logic
// Tests can't customize agent configurations
// Changing tests might break production
```

**With @Profile** (complete separation):
```java
// Production: Uses ApplicationRunner + YAML configs
// Tests: Uses E2ETestConfiguration + manual setup
// Zero cross-contamination ✅
```

### 2. Mocked Dependencies

**E2E tests need to mock LLM providers**:

**Why?**
- Real API calls cost money
- Real API calls are slow (2-5 seconds each)
- Real API calls are non-deterministic
- Real API calls require valid API keys

**How @Profile enables this**:
```java
@MockBean
protected AnthropicProvider anthropicProvider;  // Replaces real provider

// This mock is used by test agents created in E2ETestConfiguration
// Real agents (disabled by @Profile) would use real providers
```

### 3. Test-Specific Configuration

**Different role names for easier testing**:

**Production** (from YAML):
```yaml
role: "Software Developer"
description: "Senior software engineer specializing in..."
capabilities:
  - "Advanced code review"
  - "System architecture design"
  - "Technical mentoring"
```

**E2E Tests** (from E2ETestConfiguration):
```java
AgentConfig developerConfig = new AgentConfig(
    "Developer",  // Shorter, simpler
    "Test agent",
    List.of("Code review"),
    0.7, 1000,
    "You are a {role}. Task: {task}",
    "technical"
);
```

**Benefit**: Tests are cleaner and more readable:
```java
// Easy to read
new TaskRequest("Developer", "Review code", Map.of())

// vs production
new TaskRequest("Software Developer", "Review code", Map.of())
```

### 4. Fast Test Execution

**Performance Comparison**:

| Approach | Time | Reason |
|----------|------|--------|
| Without mocks | 30-90 seconds | Real LLM API calls (2-5s each × 22 tests) |
| With mocks | < 1 minute | No network calls, instant responses |

**Example timing**:
```bash
# E2E tests with mocked LLM providers
mvn failsafe:integration-test
# Tests run: 22, Failures: 0, Errors: 0, Time elapsed: 45.3 s
```

### 5. Deterministic Test Results

**Real LLM API** (non-deterministic):
```java
// Same input can produce different outputs
executeTask("Developer", "Review this code")
// Run 1: "Consider adding null checks"
// Run 2: "The code looks good"
// Run 3: "Add error handling"
```

**Mocked LLM** (deterministic):
```java
when(anthropicProvider.generate(anyString(), anyDouble(), anyInt()))
    .thenReturn(testResponse);  // Always returns same response

executeTask("Developer", "Review this code")
// Run 1: "Test LLM response for E2E tests"
// Run 2: "Test LLM response for E2E tests"
// Run 3: "Test LLM response for E2E tests"
```

### 6. No Real API Keys Required

**Without mocking**:
```bash
# Need valid API keys
export ANTHROPIC_API_KEY=sk-ant-real-key-xxx
export OPENAI_API_KEY=sk-real-key-xxx

# Risk of exposing keys in CI/CD
# Cost of running tests
```

**With mocking**:
```bash
# No API keys needed!
mvn failsafe:integration-test

# Safe for CI/CD
# Zero API costs
```

---

## Visual Diagrams

### Application Startup Flow

#### Without @Profile (Broken)

```
┌─────────────────────────────────────┐
│   Application Startup (E2E Test)   │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Phase 1: Component Scanning        │
│  - Found: @Component DeveloperAgent │
│  - Try to create bean...            │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Phase 2: Dependency Resolution     │
│  - Constructor needs: AgentConfig   │
│  - Search for AgentConfig bean...   │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│        ❌ ERROR ❌                   │
│  No qualifying bean of type         │
│  'AgentConfig' available            │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  ApplicationContext FAILED TO LOAD  │
│  All tests fail before running      │
└─────────────────────────────────────┘
```

#### With @Profile (Working)

```
┌─────────────────────────────────────┐
│   Application Startup (E2E Test)   │
│   @ActiveProfiles("e2etest")        │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Phase 1: Component Scanning        │
│  - Found: @Component                │
│            @Profile("!e2etest")     │
│            DeveloperAgent           │
│  - Profile check: "e2etest" active  │
│  - Result: SKIP (profile mismatch)  │
│  - ✅ Agent NOT autowired            │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Phase 2: Test Configuration        │
│  - @TestConfiguration loads         │
│  - E2ETestConfiguration detected    │
│  - Creates AgentConfig manually     │
│  - Creates agents manually          │
│  - Registers in AgentRegistry       │
│  - ✅ Complete control                │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Context Loaded Successfully ✅      │
│  Tests execute normally             │
└─────────────────────────────────────┘
```

### Bean Creation Comparison

#### Production Environment

```
Production Startup (profile: default)
│
├─ @Profile("!e2etest") evaluates to TRUE
│  └─ Component scanning creates bean definitions
│     (but these are never actually used)
│
├─ ApplicationRunner executes
│  ├─ Load YAML configs from src/main/resources/agents/
│  ├─ For each config:
│  │  ├─ Create AgentConfig object
│  │  ├─ Create Agent manually (new DeveloperAgent(...))
│  │  └─ Register in AgentRegistry
│  └─ Log: "Initialized 4 agents"
│
└─ Application Ready
   └─ Agents available via AgentRegistry
```

#### E2E Test Environment

```
E2E Test Startup (profile: e2etest)
│
├─ @Profile("!e2etest") evaluates to FALSE
│  └─ Component scanning SKIPS agent classes ✅
│
├─ @TestConfiguration loads
│  ├─ E2ETestConfiguration.testAgentRegistry() executes
│  ├─ Create test AgentConfig objects (short role names)
│  ├─ Create agents manually (new DeveloperAgent(...))
│  │  └─ Inject mocked LLM providers
│  └─ Register in AgentRegistry
│
├─ @MockBean providers replace real providers
│  ├─ AnthropicProvider → Mock
│  ├─ OpenAIProvider → Mock
│  └─ OllamaProvider → Mock
│
└─ Test Context Ready
   ├─ Agents use mocked LLM providers
   └─ Tests execute with deterministic results
```

---

## Troubleshooting

### Error: ApplicationContext Failed to Load

**Symptom**:
```
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException:
No qualifying bean of type 'adeengineer.dev.platform.model.AgentConfig' available
```

**Cause**: Agent classes missing `@Profile("!e2etest")` annotation

**Solution**:
```java
// Add @Profile to all agent classes
@Component
@Profile("!e2etest")  // ← Add this
public class DeveloperAgent extends BaseAgent { }
```

### Error: No Qualifying Bean of Type 'LLMProvider'

**Symptom**:
```
No qualifying bean of type 'adeengineer.dev.platform.llm.LLMProvider' available:
expected single matching bean but found 3:
anthropicProvider, openAIProvider, ollamaProvider
```

**Cause**: Multiple LLM provider beans exist (all mocked), Spring doesn't know which to inject

**Solution**:
```java
@Bean
@Primary
public AgentRegistry testAgentRegistry(
        @Qualifier("anthropicProvider") LLMProvider primaryProvider,  // ← Add @Qualifier
        OutputFormatter outputFormatter) {
    // ...
}
```

### Error: Tests Pass Locally But Fail in CI

**Symptom**: Tests work on development machine but fail in CI pipeline

**Possible Causes**:
1. Profile not activated correctly
2. Missing test configuration
3. Port conflicts

**Solution**:
```bash
# Verify profile is active
mvn failsafe:integration-test -X 2>&1 | grep "e2etest"

# Use random port (already configured)
server.port=0  # in application-e2etest.yml

# Check test configuration is loaded
@Import(E2ETestConfiguration.class)  # in BaseE2ETest
```

### Error: Agents Using Real LLM Providers Instead of Mocks

**Symptom**: Tests are slow or fail with 401 Unauthorized

**Cause**: Mocks not properly configured or agents created outside test configuration

**Solution**:
```java
// In BaseE2ETest
@MockBean  // ← Ensure this is present
protected AnthropicProvider anthropicProvider;

@BeforeEach  // ← Configure mock behavior
void setUpBase() {
    when(anthropicProvider.generate(anyString(), anyDouble(), anyInt()))
        .thenReturn(testResponse);
}

// In E2ETestConfiguration
@Qualifier("anthropicProvider")  // ← Use the mocked provider
LLMProvider primaryProvider
```

---

## Best Practices

### 1. Profile Naming

**Use descriptive, specific profile names**:
```java
// Good
@Profile("!e2etest")      // Clear intent: exclude from E2E tests
@Profile("integration")   // Specific purpose
@Profile("local-dev")     // Environment-specific

// Avoid
@Profile("!test")         // Too broad (excludes all tests)
@Profile("special")       // Unclear purpose
```

### 2. Test Configuration Organization

**Keep test configurations focused**:
```java
@TestConfiguration
public class E2ETestConfiguration {
    // Only E2E-specific beans
    @Bean @Primary
    public AgentRegistry testAgentRegistry(...) { }

    // Don't put unit test configs here
}
```

### 3. Profile Activation in Tests

**Always activate profiles explicitly**:
```java
@SpringBootTest
@ActiveProfiles("e2etest")  // ← Explicit, clear intent
public abstract class BaseE2ETest { }

// Don't rely on default profiles for tests
```

### 4. Mock Configuration

**Configure mocks comprehensively**:
```java
@BeforeEach
void setUpBase() {
    // Configure ALL methods that might be called
    when(anthropicProvider.generate(...)).thenReturn(testResponse);
    when(anthropicProvider.isHealthy()).thenReturn(true);
    when(anthropicProvider.getProviderName()).thenReturn("test-provider");
    when(anthropicProvider.getModel()).thenReturn("test-model");
}
```

### 5. Documentation

**Document why profiles are needed**:
```java
/**
 * Developer agent for code review and architecture guidance.
 *
 * Note: Uses @Profile("!e2etest") to prevent autowiring during E2E tests.
 * E2E tests create agents manually via E2ETestConfiguration.
 */
@Component
@Profile("!e2etest")
public class DeveloperAgent extends BaseAgent { }
```

### 6. Verify Profile Behavior

**Add tests to ensure profiles work correctly**:
```java
@Test
void shouldNotCreateAgentBeansInE2EProfile() {
    // This test runs with @ActiveProfiles("e2etest")
    // Verify agent beans are NOT in context

    assertThatThrownBy(() ->
        applicationContext.getBean(DeveloperAgent.class)
    ).isInstanceOf(NoSuchBeanDefinitionException.class);
}
```

---

## References

1. [Spring Framework Documentation - Bean Definition Profiles](https://docs.spring.io/spring-framework/reference/core/beans/definition.html#beans-definition-profiles)
2. [Spring Boot Testing - @ActiveProfiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications)
3. [Spring Boot Documentation - ApplicationRunner](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/ApplicationRunner.html)
4. [Testing Guide - Role Manager App](../dev-testing-guide.md)
5. [Baeldung - Spring Profiles](https://www.baeldung.com/spring-profiles)
6. [Baeldung - Spring @MockBean](https://www.baeldung.com/java-spring-mockito-mock-mockbean)

---

*Last Updated: 2025-10-18*
