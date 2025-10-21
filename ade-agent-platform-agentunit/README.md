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

## Module Structure

```
ade-agent-platform-agentunit/
└── src/main/java/dev/adeengineer/platform/test/
    ├── mock/                  # Mock implementations
    │   ├── MockAgent.java     # Mock Agent for testing
    │   └── MockLLMProvider.java  # Mock LLM provider
    ├── factory/               # Test data factories
    │   └── TestData.java      # Factory methods for test data
    ├── builder/               # Test data builders (future)
    ├── assertion/             # Custom assertions (future)
    └── base/                  # Base test classes (future)
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

### MockAgent

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
