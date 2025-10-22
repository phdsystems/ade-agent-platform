# AgentUnit - Test Framework for ade Agent Platform

**Version:** 0.2.0-SNAPSHOT
**Status:** Framework-agnostic test utilities

## Overview

AgentUnit is a test framework module providing reusable mock implementations, test data factories, and test utilities for all ade Agent Platform modules.

## Purpose

- **Single source of truth** for test utilities across all modules
- **Framework-agnostic** - works with JUnit, Spring Test, Quarkus Test, Micronaut Test
- **Eliminates code duplication** - test utilities defined once, used everywhere
- **Consistent testing patterns** across core, Spring Boot, Quarkus, and Micronaut modules

## Documentation

**For comprehensive documentation, see [doc/overview.md](doc/overview.md)**

Quick Links:
- **Getting Started** → [Usage](#usage) section below
- **Design Philosophy** → [Utilities vs Annotations Guide](doc/guide/utilities-vs-annotations-testing.md)
- **Complete Documentation Index** → [doc/overview.md](doc/overview.md)

## Module Structure

```
ade-agent-platform-agentunit/
└── src/main/java/dev/adeengineer/platform/test/
    ├── mock/                          # Mock implementations
    │   ├── MockAgent.java             # Mock Agent for testing
    │   └── MockLLMProvider.java       # Mock LLM provider
    ├── factory/                       # Test data factories
    │   └── TestData.java              # Factory methods for test data
    ├── builder/                       # Fluent test data builders
    │   ├── AgentConfigBuilder.java   # Build AgentConfig instances
    │   ├── TaskRequestBuilder.java   # Build TaskRequest instances
    │   └── TaskResultBuilder.java    # Build TaskResult instances
    ├── assertion/                     # Custom AssertJ assertions
    │   ├── TaskResultAssert.java     # TaskResult assertions
    │   └── AgentAssertions.java      # Entry point for assertions
    ├── base/                          # Base test classes
    │   ├── BaseAgentTest.java        # Base for agent unit tests
    │   └── BaseIntegrationTest.java  # Base for integration tests
    ├── quarkus/                       # Quarkus-specific utilities
    │   ├── BaseQuarkusTest.java      # Base for Quarkus tests
    │   └── QuarkusTestProfiles.java  # Common test profiles
    └── micronaut/                     # Micronaut-specific utilities
        ├── BaseMicronautTest.java    # Base for Micronaut tests
        └── MicronautTestProperties.java  # Common test properties
```

## Usage

### Maven Dependency

Add to your module's `pom.xml`:

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-agentunit</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

### Example Test

```java
package dev.adeengineer.platform.core;

import dev.adeengineer.platform.test.mock.MockAgent;
import dev.adeengineer.platform.test.mock.MockLLMProvider;
import dev.adeengineer.platform.test.factory.TestData;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AgentRegistryTest {
    @Test
    void shouldRegisterAgent() {
        // Arrange
        AgentRegistry registry = new AgentRegistry();
        MockAgent agent = new MockAgent("test-agent");

        // Act
        registry.registerAgent(agent);

        // Assert
        assertThat(registry.getAgent("test-agent")).isEqualTo(agent);
    }

    @Test
    void shouldExecuteTaskWithMockLLM() {
        // Arrange
        MockLLMProvider llmProvider = new MockLLMProvider();
        llmProvider.withResponseContent("Test response");

        // Act
        LLMResponse response = llmProvider.generate("test prompt", 0.7, 100);

        // Assert
        assertThat(response.content()).isEqualTo("Test response");
    }
}
```

## Available Test Utilities

### Phase 1: Core Utilities (v0.2.0)

#### MockAgent

Mock implementation of `Agent` interface for testing.

**Features:**
- Configurable task results
- Configurable exceptions
- Tracks execution count
- Supports custom capabilities

**Usage:**
```java
MockAgent agent = new MockAgent("test-agent");
agent.setResultToReturn(TaskResult.success(...));
agent.setExceptionToThrow(new RuntimeException("Test error"));

TaskResult result = agent.executeTask(request);
```

### MockLLMProvider

Mock implementation of `LLMProvider` for testing LLM-dependent code.

**Features:**
- Configurable response content
- Configurable health status
- Configurable exceptions
- Fluent API

**Usage:**
```java
MockLLMProvider provider = new MockLLMProvider()
    .withResponseContent("Mock LLM response")
    .withException(new RuntimeException("LLM error"));

LLMResponse response = provider.generate("prompt", 0.7, 100);
```

### TestData

Factory methods for creating test data with sensible defaults.

**Features:**
- Pre-configured `AgentConfig` instances
- Pre-configured `TaskRequest` instances
- Pre-configured `LLMResponse` instances
- Pre-configured `UsageInfo` instances

**Usage:**
```java
AgentConfig config = TestData.validAgentConfig();
TaskRequest request = TestData.validTaskRequest();
LLMResponse response = TestData.validLLMResponse();
UsageInfo usage = TestData.validUsageInfo();
```

### Phase 2: Advanced Utilities (v0.2.0)

#### Fluent Builders

Build test data with a fluent API for better readability.

**AgentConfigBuilder**:
```java
import dev.adeengineer.platform.test.builder.AgentConfigBuilder;

AgentConfig config = AgentConfigBuilder.builder()
    .name("Developer")
    .description("Software development agent")
    .capabilities("coding", "testing", "debugging")
    .temperature(0.7)
    .maxTokens(1000)
    .promptTemplate("You are a {role}. Task: {task}")
    .outputFormat("technical")
    .build();
```

**TaskRequestBuilder**:
```java
import dev.adeengineer.platform.test.builder.TaskRequestBuilder;

TaskRequest request = TaskRequestBuilder.builder()
    .agentName("Developer")
    .task("Write unit tests for UserService")
    .context("priority", "high")
    .context("deadline", "2024-01-15")
    .build();
```

**TaskResultBuilder**:
```java
import dev.adeengineer.platform.test.builder.TaskResultBuilder;

// Success result
TaskResult success = TaskResultBuilder.success()
    .agentName("Developer")
    .task("Write tests")
    .output("Tests written successfully")
    .metadata("totalTokens", 150)
    .durationMs(1000L)
    .build();

// Failure result
TaskResult failure = TaskResultBuilder.failure()
    .agentName("Developer")
    .task("Invalid task")
    .errorMessage("Task validation failed")
    .build();
```

#### Custom Assertions

Fluent AssertJ-style assertions for TaskResult.

**Usage**:
```java
import static dev.adeengineer.platform.test.assertion.AgentAssertions.assertThat;

TaskResult result = agent.executeTask(request);

assertThat(result)
    .isSuccessful()
    .hasAgentName("Developer")
    .hasOutputContaining("completed")
    .hasDurationLessThan(1000L)
    .hasMetadata("totalTokens", 150);
```

**Available Assertions**:
- `isSuccessful()` - Verify task succeeded
- `isFailure()` - Verify task failed
- `hasAgentName(String)` - Check agent name
- `hasTask(String)` - Check task description
- `hasOutput(String)` - Check exact output
- `hasOutputContaining(String)` - Check output substring
- `hasErrorMessageContaining(String)` - Check error message
- `hasDurationLessThan(Long)` - Check duration upper bound
- `hasDurationGreaterThan(Long)` - Check duration lower bound
- `hasMetadataKey(String)` - Check metadata key exists
- `hasMetadata(String, Object)` - Check metadata value

#### Base Test Classes

Extend these base classes for standardized test setup.

**BaseAgentTest** - For unit tests:
```java
import dev.adeengineer.platform.test.base.BaseAgentTest;

class MyAgentTest extends BaseAgentTest {

    private AgentRegistry agentRegistry;

    @BeforeEach
    void setUp() {
        super.setUpBaseAgent(); // Sets up mockLLMProvider
        agentRegistry = new AgentRegistry();
    }

    @Test
    void shouldExecuteTask() {
        configureMockLLM("Custom response");
        // mockLLMProvider is available from parent
        MockAgent agent = new MockAgent("test-agent");
        // Test logic...
    }
}
```

**BaseIntegrationTest** - For integration tests:
```java
import dev.adeengineer.platform.test.base.BaseIntegrationTest;

class OrchestrationIntegrationTest extends BaseIntegrationTest {

    private AgentRegistry agentRegistry;
    private ParallelAgentExecutor executor;

    @BeforeEach
    void setUp() {
        super.setUpIntegration(); // Sets up mockLLMProvider

        agentRegistry = new AgentRegistry();
        executor = new ParallelAgentExecutor(agentRegistry, mockLLMProvider);
    }

    @Test
    void shouldOrchestrate() {
        configureMockLLMResponse("Integration test response");
        // Test logic with multiple components...
    }
}
```

### Phase 3: Framework-Specific Utilities (v0.2.0)

#### Quarkus Test Utilities

**BaseQuarkusTest** - Base class for Quarkus-based tests:
```java
import dev.adeengineer.platform.test.quarkus.BaseQuarkusTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class MyQuarkusServiceTest extends BaseQuarkusTest {

    @Inject
    MyService myService;

    @Test
    void shouldExecuteWithMockLLM() {
        configureMockLLM("Quarkus test response");
        String result = myService.process("test input");
        assertThat(result).isEqualTo("Quarkus test response");
    }
}
```

**QuarkusTestProfiles** - Common test configuration profiles:
```java
import dev.adeengineer.platform.test.quarkus.QuarkusTestProfiles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

// Use MockLLMProfile for tests with mock LLM configuration
@QuarkusTest
@TestProfile(QuarkusTestProfiles.MockLLMProfile.class)
class MyServiceTest {
    // Test uses mock LLM provider settings
}

// Use IsolatedProfile to disable external services (Redis, DB, etc.)
@QuarkusTest
@TestProfile(QuarkusTestProfiles.IsolatedProfile.class)
class UnitTestWithoutExternalDeps {
    // Test runs without Redis, database, etc.
}

// Use FastTestProfile for faster tests (disables metrics, health, tracing)
@QuarkusTest
@TestProfile(QuarkusTestProfiles.FastTestProfile.class)
class FastUnitTest {
    // Test runs with minimal overhead
}
```

**Available Quarkus Test Profiles:**
- `MockLLMProfile` - Mock LLM provider configuration
- `IsolatedProfile` - Disables external services (Redis, database, caching)
- `FastTestProfile` - Disables heavy features (metrics, health, tracing)

#### Micronaut Test Utilities

**BaseMicronautTest** - Base class for Micronaut-based tests:
```java
import dev.adeengineer.platform.test.micronaut.BaseMicronautTest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
class MyMicronautServiceTest extends BaseMicronautTest {

    @Inject
    MyService myService;

    @Test
    void shouldExecuteWithMockLLM() {
        configureMockLLM("Micronaut test response");
        String result = myService.process("test input");
        assertThat(result).isEqualTo("Micronaut test response");
    }
}
```

**MicronautTestProperties** - Common test property providers:
```java
import dev.adeengineer.platform.test.micronaut.MicronautTestProperties;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

// Use MockLLMProperties for mock LLM configuration
@MicronautTest
class MyServiceTest implements MicronautTestProperties.MockLLMProperties {
    // Test uses mock LLM provider settings
}

// Use IsolatedProperties to disable external services
@MicronautTest
class UnitTestWithoutExternalDeps implements MicronautTestProperties.IsolatedProperties {
    // Test runs without Redis, database, etc.
}

// Use FastTestProperties for faster tests
@MicronautTest
class FastUnitTest implements MicronautTestProperties.FastTestProperties {
    // Test runs with minimal overhead
}

// Use FastIsolatedProperties for fastest tests with no external dependencies
@MicronautTest
class VeryFastUnitTest implements MicronautTestProperties.FastIsolatedProperties {
    // Test runs at maximum speed with no external services
}
```

**Available Micronaut Test Property Providers:**
- `MockLLMProperties` - Mock LLM provider configuration
- `IsolatedProperties` - Disables external services (Redis, database, caching)
- `FastTestProperties` - Disables heavy features (metrics, health, tracing)
- `FastIsolatedProperties` - Combined fast + isolated for maximum speed

## Migration from Duplicated Utilities

### Old Import Statements (Before AgentUnit)

```java
// Core module
import dev.adeengineer.platform.testutil.MockAgent;
import dev.adeengineer.platform.testutil.TestData;
import dev.adeengineer.platform.testutil.TestLLMProvider;

// Spring Boot module
import dev.adeengineer.platform.spring.testutil.MockAgent;
import dev.adeengineer.platform.spring.testutil.TestData;
import dev.adeengineer.platform.spring.testutil.TestLLMProvider;
```

### New Import Statements (With AgentUnit)

```java
// All modules
import dev.adeengineer.platform.test.mock.MockAgent;
import dev.adeengineer.platform.test.factory.TestData;
import dev.adeengineer.platform.test.mock.MockLLMProvider;  // Note: renamed from TestLLMProvider
```

## Benefits

### Before AgentUnit

- **347 lines of duplicated code** across 3 locations
- **3 copies** of same utilities (core, Spring Boot, legacy)
- Bug fixes required **3 identical changes**
- Risk of utilities diverging over time

### After AgentUnit

- **✅ Zero duplication** - single source of truth
- **✅ Consistent testing** - same utilities everywhere
- **✅ Easy maintenance** - fix once, works everywhere
- **✅ Framework-agnostic** - works with any DI framework

## Dependencies

AgentUnit depends on:
- `ade-agent` (SDK interfaces)
- `jackson-databind` (for test data serialization)
- `junit-jupiter` (JUnit 5)
- `assertj-core` (fluent assertions)
- `mockito-core` (mocking framework)
- `reactor-core` (reactive streams)
- `lombok` (code generation)

All dependencies are compile-scoped in AgentUnit but consumed as test-scoped by modules.

## Contributing

When adding new test utilities:
1. Place in appropriate package (`mock/`, `factory/`, `builder/`, `assertion/`, or `base/`)
2. Follow existing naming conventions
3. Add Javadoc for all public APIs
4. Update this README with usage examples

## License

Same license as parent ade Agent Platform project.

---

*Last Updated: 2025-10-21*
