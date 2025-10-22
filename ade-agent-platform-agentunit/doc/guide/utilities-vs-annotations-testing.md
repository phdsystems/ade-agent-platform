# Utilities-Based vs Annotation-Based Testing - AgentUnit

**Date:** 2025-10-22
**Version:** 1.0

## TL;DR

**Key concept**: AgentUnit uses **utilities-based testing** (direct instantiation) instead of annotation-based dependency injection for core test utilities, maximizing portability and minimizing framework lock-in. **Critical points**: MockLLMProvider and MockAgent use `new MockXxx()` → no `@Mock` annotations required → works with any framework. **Quick rule**: Use utilities (no annotations) for core testing, use annotations only for framework integration (`@QuarkusTest`, `@MicronautTest`).

**Quick decision**: Writing unit tests → use utilities (new MockLLMProvider()). Integrating with DI framework → use framework annotations only at integration boundaries.

---

## Table of Contents

1. [Overview](#overview)
2. [Core Philosophy](#core-philosophy)
3. [Detailed Comparison](#detailed-comparison)
4. [Code Examples](#code-examples)
5. [Migration Guide](#migration-guide)
6. [When to Use Each Approach](#when-to-use-each-approach)
7. [Best Practices](#best-practices)
8. [Common Pitfalls](#common-pitfalls)
9. [References](#references)

---

## Overview

AgentUnit adopts a **utilities-based testing approach** that prioritizes:

1. **Framework agnosticism** - Works with JUnit, Spring, Quarkus, Micronaut
2. **Explicit behavior** - No hidden magic or injection
3. **Simplicity** - Just Java objects and constructors
4. **Portability** - Copy tests between frameworks without changes

This contrasts with traditional **annotation-based testing** frameworks (Mockito's `@Mock`, Spring's `@MockBean`) that lock tests to specific frameworks.

---

## Core Philosophy

### AgentUnit's Principle

> **"Annotations are for framework integration, not for test utilities"**

```
┌─────────────────────────────────────────┐
│     Application Code (Framework)        │
│  @Inject, @Autowired, @ConfigProperty   │  ← Framework annotations OK
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│      Test Integration Layer              │
│   @QuarkusTest, @MicronautTest          │  ← Framework annotations OK
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│      Test Utilities (AgentUnit)         │
│  new MockLLMProvider(), TestData.*      │  ← NO ANNOTATIONS
└─────────────────────────────────────────┘
```

**Why?**
- **Test utilities** should be reusable across all frameworks
- **Framework integration** happens at the boundary, not in utilities
- **Portability** is more valuable than convenience

---

## Detailed Comparison

### 1. Instantiation

| Aspect | Utilities-Based (AgentUnit) | Annotation-Based |
|--------|---------------------------|------------------|
| **Syntax** | `new MockLLMProvider()` | `@Mock LLMProvider provider` |
| **Visibility** | Explicit constructor call | Hidden by framework |
| **Debugging** | Standard Java debugging | Framework magic to debug |
| **Dependencies** | None (just Java) | Framework required |
| **Learning Curve** | Minimal (Java basics) | Framework-specific docs |

**Example:**

```java
// Utilities-based (AgentUnit)
MockLLMProvider provider = new MockLLMProvider()
    .withResponseContent("Test response");

// Annotation-based (Mockito)
@Mock
private LLMProvider provider;

@BeforeEach
void setUp() {
    when(provider.generate(any(), anyDouble(), anyInt()))
        .thenReturn(new LLMResponse("Test response", ...));
}
```

### 2. Configuration

| Aspect | Utilities-Based | Annotation-Based |
|--------|----------------|------------------|
| **Fluent API** | ✅ `provider.withResponseContent()` | ❌ Verbose `when().thenReturn()` |
| **Multiple States** | ✅ Easy to configure | ⚠️ Complex with ArgumentMatchers |
| **Readability** | ✅ Self-documenting | ⚠️ DSL to learn |
| **Type Safety** | ✅ Compile-time checks | ⚠️ Runtime matching |

**Example:**

```java
// Utilities-based - Clear intent
MockLLMProvider provider = new MockLLMProvider()
    .withResponseContent("Success response");

provider.withException(new RuntimeException("Failure"));

// Annotation-based - Verbose setup
when(provider.generate(eq("prompt1"), anyDouble(), anyInt()))
    .thenReturn(successResponse);
when(provider.generate(eq("prompt2"), anyDouble(), anyInt()))
    .thenThrow(new RuntimeException("Failure"));
```

### 3. Framework Portability

| Framework | Utilities-Based | Annotation-Based |
|-----------|----------------|------------------|
| **Pure JUnit** | ✅ Works out of box | ⚠️ Requires Mockito extension |
| **Spring Boot** | ✅ No changes needed | ❌ `@MockBean` (Spring-specific) |
| **Quarkus** | ✅ No changes needed | ❌ `@Mock` (different semantics) |
| **Micronaut** | ✅ No changes needed | ❌ `@MockBean` (Micronaut-specific) |

**Example - Same Test, Multiple Frameworks:**

```java
// Utilities-based - Works in ALL frameworks
class AgentTest {
    @Test
    void shouldExecuteTask() {
        MockLLMProvider provider = new MockLLMProvider();
        MockAgent agent = new MockAgent("test-agent");
        // Test logic...
    }
}

// Annotation-based - Requires framework changes
@ExtendWith(MockitoExtension.class)  // JUnit + Mockito
// OR
@SpringBootTest                       // Spring
@MockBean LLMProvider provider;
// OR
@QuarkusTest                          // Quarkus (different mock semantics)
```

### 4. Dependency Weight

| Aspect | Utilities-Based | Annotation-Based |
|--------|----------------|------------------|
| **Required JARs** | JUnit + AgentUnit | JUnit + Mockito/Spring/Quarkus |
| **Transitive Deps** | Minimal | Framework ecosystem |
| **Startup Time** | Fast | Framework bootstrap overhead |
| **Binary Size** | Small | Larger (framework libs) |

---

## Code Examples

### Example 1: Simple Unit Test

**Utilities-Based (AgentUnit):**

```java
package dev.adeengineer.platform.core;

import dev.adeengineer.platform.test.mock.MockAgent;
import dev.adeengineer.platform.test.mock.MockLLMProvider;
import dev.adeengineer.platform.test.factory.TestData;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AgentRegistryTest {

    @Test
    void shouldRegisterAndRetrieveAgent() {
        // Arrange - Explicit instantiation
        AgentRegistry registry = new AgentRegistry();
        MockAgent agent = new MockAgent("developer");

        // Act
        registry.register(agent);
        Agent retrieved = registry.getAgent("developer");

        // Assert
        assertThat(retrieved).isEqualTo(agent);
        assertThat(retrieved.getName()).isEqualTo("developer");
    }

    @Test
    void shouldExecuteTaskWithLLM() {
        // Arrange - Fluent configuration
        MockLLMProvider llmProvider = new MockLLMProvider()
            .withResponseContent("Task completed successfully");

        MockAgent agent = new MockAgent("developer");
        TaskRequest request = TestData.validTaskRequest();

        // Act
        TaskResult result = agent.executeTask(request);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Mock response");
    }
}
```

**Annotation-Based (Mockito):**

```java
package dev.adeengineer.platform.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)  // Framework lock-in
class AgentRegistryTest {

    @Mock
    private LLMProvider llmProvider;

    @Mock
    private Agent agent;

    @InjectMocks
    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        // Verbose configuration
        when(agent.getName()).thenReturn("developer");
        when(agent.executeTask(any()))
            .thenReturn(TaskResult.success(
                "developer",
                "test task",
                "Task completed",
                Map.of(),
                100L
            ));
    }

    @Test
    void shouldRegisterAndRetrieveAgent() {
        // Act
        registry.register(agent);
        Agent retrieved = registry.getAgent("developer");

        // Assert
        assertThat(retrieved).isEqualTo(agent);
        verify(agent).getName();  // Additional verification needed
    }
}
```

### Example 2: Base Test Class

**Utilities-Based (AgentUnit):**

```java
import dev.adeengineer.platform.test.base.BaseAgentTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MyAgentTest extends BaseAgentTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        super.setUpBaseAgent();  // Explicit call - mockLLMProvider available
        registry = new AgentRegistry();
    }

    @Test
    void shouldExecuteWithCustomResponse() {
        // Configure inherited mockLLMProvider
        configureMockLLM("Custom LLM response");

        MockAgent agent = new MockAgent("developer");
        TaskResult result = agent.executeTask(TestData.validTaskRequest());

        assertThat(result.success()).isTrue();
    }
}
```

**Annotation-Based (Spring Boot):**

```java
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.Test;

@SpringBootTest  // Heavy framework bootstrap
class MyAgentTest {

    @MockBean  // Spring-specific
    private LLMProvider llmProvider;

    @Autowired
    private AgentRegistry registry;

    @Test
    void shouldExecuteWithCustomResponse() {
        // Configure mock
        when(llmProvider.generate(any(), anyDouble(), anyInt()))
            .thenReturn(new LLMResponse("Custom LLM response", ...));

        // Test logic
    }
}
```

### Example 3: Framework Integration

**Utilities-Based with Quarkus:**

```java
import dev.adeengineer.platform.test.quarkus.BaseQuarkusTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest  // ← ONLY framework annotation
class QuarkusAgentServiceTest extends BaseQuarkusTest {

    @Inject  // ← Real service injection
    AgentService agentService;

    @Test
    void shouldProcessWithMockLLM() {
        // Utilities approach for test data
        configureMockLLM("Quarkus test response");

        MockAgent agent = new MockAgent("quarkus-agent");
        TaskRequest request = TestData.validTaskRequest();

        TaskResult result = agentService.execute(agent, request);

        assertThat(result.success()).isTrue();
    }
}
```

**Key Insight:** Framework annotations (`@QuarkusTest`, `@Inject`) are used only for **framework integration**, while test utilities (`MockAgent`, `MockLLMProvider`) remain **annotation-free**.

---

## Migration Guide

### Step 1: Replace @Mock with Direct Instantiation

**Before (Mockito):**
```java
@ExtendWith(MockitoExtension.class)
class MyTest {
    @Mock
    private LLMProvider llmProvider;

    @BeforeEach
    void setUp() {
        when(llmProvider.generate(any(), anyDouble(), anyInt()))
            .thenReturn(new LLMResponse("Test", ...));
    }
}
```

**After (AgentUnit):**
```java
class MyTest {
    @Test
    void test() {
        MockLLMProvider llmProvider = new MockLLMProvider()
            .withResponseContent("Test");

        // Use directly in test
    }
}
```

### Step 2: Replace @InjectMocks with Constructor

**Before (Mockito):**
```java
@Mock
private LLMProvider llmProvider;

@InjectMocks
private AgentOrchestrator orchestrator;
```

**After (AgentUnit):**
```java
@Test
void test() {
    MockLLMProvider llmProvider = new MockLLMProvider();
    AgentOrchestrator orchestrator = new AgentOrchestrator(llmProvider);

    // Test logic
}
```

### Step 3: Replace when().thenReturn() with Fluent API

**Before (Mockito):**
```java
when(llmProvider.generate(eq("prompt1"), anyDouble(), anyInt()))
    .thenReturn(response1);
when(llmProvider.generate(eq("prompt2"), anyDouble(), anyInt()))
    .thenReturn(response2);
```

**After (AgentUnit):**
```java
MockLLMProvider llmProvider = new MockLLMProvider()
    .withResponseContent("prompt1 response");

// For different responses, create multiple instances or reconfigure
llmProvider.withResponseContent("prompt2 response");
```

### Step 4: Use Base Classes for Common Setup

**Before (Custom Base Class):**
```java
abstract class BaseTest {
    @Mock
    protected LLMProvider llmProvider;

    @BeforeEach
    void baseSetup() {
        MockitoAnnotations.openMocks(this);
    }
}
```

**After (AgentUnit):**
```java
import dev.adeengineer.platform.test.base.BaseAgentTest;

class MyTest extends BaseAgentTest {
    @BeforeEach
    void setUp() {
        super.setUpBaseAgent();  // mockLLMProvider available
    }
}
```

---

## When to Use Each Approach

### Use Utilities-Based (AgentUnit) When:

✅ **Writing unit tests** - No framework needed
✅ **Testing core logic** - Business logic without DI
✅ **Maximizing portability** - Tests work across frameworks
✅ **Avoiding framework lock-in** - Future-proofing
✅ **Learning/teaching** - Simpler concepts
✅ **Fast test execution** - No framework bootstrap

**Example Scenarios:**
- Testing `AgentRegistry` class
- Testing `TaskRequest` validation
- Testing `ParallelAgentExecutor` logic
- Testing `AgentConfig` builders

### Use Annotation-Based When:

✅ **Framework integration required** - Testing DI container
✅ **Testing framework features** - Transaction management, security
✅ **Complex dependency graphs** - Many injected beans
✅ **Already committed to framework** - Existing Spring/Quarkus codebase

**Example Scenarios:**
- Testing Spring Boot `@Service` with `@Transactional`
- Testing Quarkus REST endpoints
- Testing Micronaut reactive streams
- Integration tests with real database

### Hybrid Approach (Recommended)

**Combine both for optimal results:**

```java
@QuarkusTest  // ← Framework integration
class HybridTest extends BaseQuarkusTest {  // ← Utilities from base

    @Inject  // ← Framework injection
    RealService realService;

    @Test
    void shouldIntegrate() {
        // Utilities for test data
        MockLLMProvider mockLLM = new MockLLMProvider()
            .withResponseContent("Test response");

        MockAgent agent = new MockAgent("test");
        TaskRequest request = TestData.validTaskRequest();

        // Framework-injected service
        TaskResult result = realService.execute(agent, request);

        assertThat(result.success()).isTrue();
    }
}
```

**Best of both worlds:**
- Framework integration at boundaries (`@QuarkusTest`, `@Inject`)
- Test utilities remain portable (`new MockLLMProvider()`)

---

## Best Practices

### 1. Prefer Utilities for Core Logic

```java
// ✅ GOOD - Utilities-based unit test
@Test
void shouldCalculateTokenUsage() {
    MockLLMProvider provider = new MockLLMProvider()
        .withResponseContent("test");

    UsageInfo usage = provider.generate("prompt", 0.7, 100).usage();

    assertThat(usage.totalTokens()).isGreaterThan(0);
}

// ❌ AVOID - Unnecessary framework overhead
@ExtendWith(MockitoExtension.class)
@Test
void shouldCalculateTokenUsage(@Mock LLMProvider provider) {
    when(provider.generate(any(), anyDouble(), anyInt()))
        .thenReturn(new LLMResponse(...));
    // ... same test logic
}
```

### 2. Use Base Classes for Repeated Setup

```java
// ✅ GOOD - Extend base class for common infrastructure
class MyAgentTest extends BaseAgentTest {
    @BeforeEach
    void setUp() {
        super.setUpBaseAgent();  // Inherits mockLLMProvider
    }
}

// ❌ AVOID - Duplicating setup in every test
class MyAgentTest {
    private MockLLMProvider mockLLMProvider;

    @BeforeEach
    void setUp() {
        mockLLMProvider = new MockLLMProvider();
        // ... repeated in every test class
    }
}
```

### 3. Framework Annotations at Integration Boundaries Only

```java
// ✅ GOOD - Framework annotation for integration
@QuarkusTest
class IntegrationTest {
    @Inject
    AgentService service;  // Real service

    @Test
    void test() {
        MockAgent agent = new MockAgent("test");  // Utility mock
    }
}

// ❌ AVOID - Framework annotations for simple mocks
@QuarkusTest
class UnitTest {
    @Inject
    @Mock  // Unnecessary - could be new MockAgent()
    Agent agent;
}
```

### 4. Use Factory Methods for Common Test Data

```java
// ✅ GOOD - Reusable factory methods
@Test
void shouldProcessValidRequest() {
    TaskRequest request = TestData.validTaskRequest();
    TaskResult result = agent.executeTask(request);
}

// ❌ AVOID - Duplicating test data construction
@Test
void shouldProcessValidRequest() {
    TaskRequest request = new TaskRequest(
        "agent-name",
        "task description",
        Map.of("context", "value")
    );
    // ... repeated across tests
}
```

### 5. Fluent API for Readable Configuration

```java
// ✅ GOOD - Fluent configuration
MockLLMProvider provider = new MockLLMProvider()
    .withResponseContent("Success")
    .withException(new RuntimeException("Expected error"));

// ❌ AVOID - Setter soup
MockLLMProvider provider = new MockLLMProvider();
provider.setResponseContent("Success");
provider.setExceptionToThrow(new RuntimeException("Expected error"));
```

---

## Common Pitfalls

### 1. ❌ Mixing Mockito with AgentUnit

**Problem:**
```java
@ExtendWith(MockitoExtension.class)
class MixedTest {
    @Mock  // Mockito
    private LLMProvider llmProvider;

    @Test
    void test() {
        MockAgent agent = new MockAgent("test");  // AgentUnit
        // Mixing approaches - confusing
    }
}
```

**Solution:**
```java
class ConsistentTest {
    @Test
    void test() {
        MockLLMProvider llmProvider = new MockLLMProvider();  // AgentUnit
        MockAgent agent = new MockAgent("test");              // AgentUnit
        // Consistent utilities approach
    }
}
```

### 2. ❌ Over-Using Base Classes

**Problem:**
```java
class SimpleTest extends BaseAgentTest {  // Unnecessary inheritance
    @Test
    void test() {
        // Test doesn't use mockLLMProvider at all
        String result = StringUtils.capitalize("test");
        assertThat(result).isEqualTo("Test");
    }
}
```

**Solution:**
```java
class SimpleTest {  // No inheritance needed
    @Test
    void test() {
        String result = StringUtils.capitalize("test");
        assertThat(result).isEqualTo("Test");
    }
}
```

### 3. ❌ Framework Annotations in Utilities Module

**Problem:**
```java
// In agentunit module
import org.springframework.stereotype.Component;

@Component  // ❌ Framework dependency in utilities
public class MockLLMProvider implements LLMProvider {
    // ...
}
```

**Solution:**
```java
// In agentunit module
public class MockLLMProvider implements LLMProvider {  // ✅ Plain Java
    // No framework annotations
}
```

### 4. ❌ Not Calling super.setUp() in Base Classes

**Problem:**
```java
class MyTest extends BaseAgentTest {
    @BeforeEach
    void setUp() {
        // Forgot to call super.setUpBaseAgent()
        // mockLLMProvider is null!
    }

    @Test
    void test() {
        configureMockLLM("response");  // NullPointerException
    }
}
```

**Solution:**
```java
class MyTest extends BaseAgentTest {
    @BeforeEach
    void setUp() {
        super.setUpBaseAgent();  // ✅ Initialize parent
        // Now mockLLMProvider is available
    }
}
```

---

## References

### Official Documentation

1. **JUnit 5 User Guide**
   - https://junit.org/junit5/docs/current/user-guide/
   - Standard Java testing framework

2. **AssertJ Documentation**
   - https://assertj.github.io/doc/
   - Fluent assertion library used by AgentUnit

3. **Mockito Documentation**
   - https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
   - For understanding annotation-based approach

### Framework Integration Guides

4. **Quarkus Testing Guide**
   - https://quarkus.io/guides/getting-started-testing
   - Testing Quarkus applications

5. **Micronaut Testing Documentation**
   - https://docs.micronaut.io/latest/guide/#testing
   - Testing Micronaut applications

6. **Spring Boot Testing**
   - https://docs.spring.io/spring-boot/reference/testing/index.html
   - Testing Spring Boot applications

### Design Principles

7. **Effective Java (3rd Edition)** - Joshua Bloch
   - Item 15: "Minimize mutability"
   - Item 20: "Prefer interfaces to abstract classes"
   - Principles behind utilities design

8. **Growing Object-Oriented Software, Guided by Tests** - Steve Freeman, Nat Pryce
   - Chapter 21: "Test Readability"
   - Chapter 22: "Constructing Complex Test Data"
   - Testing best practices

### Related AgentUnit Documentation

9. **AgentUnit README**
   - `/home/developer/ade-agent-platform/ade-agent-platform-agentunit/README.md`
   - Complete AgentUnit features and usage

---

**Last Updated:** 2025-10-22
**Version:** 1.0
