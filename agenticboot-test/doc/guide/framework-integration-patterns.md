# Framework Integration Patterns - AgentUnit

**Date:** 2025-10-22
**Version:** 1.0

## TL;DR

**Key concept**: `@AgenticBootTest` is AgenticBoot's testing annotation, similar to `@SpringBootTest` for Spring, `@QuarkusTest` for Quarkus, and `@MicronautTest` for Micronaut. **Critical points**: Use `@AgenticBootTest` alone for pure AgenticBoot tests → Combine with framework annotations for integration tests → Real framework beans + AgenticBoot mocks = best of both worlds. **Quick rule**: Pure AgenticBoot test → `@AgenticBootTest` only. Integration test → Framework annotation + `@AgenticBootTest`.

**Quick decision**: Testing AgenticBoot components in isolation → use `@AgenticBootTest`. Testing framework integration → combine `@SpringBootTest` + `@AgenticBootTest` (or Quarkus/Micronaut equivalent).

---

## Table of Contents

1. [Overview](#overview)
2. [Framework Testing Annotations](#framework-testing-annotations)
3. [Pure AgenticBoot Testing](#pure-agenticboot-testing)
4. [Spring Boot Integration](#spring-boot-integration)
5. [Quarkus Integration](#quarkus-integration)
6. [Micronaut Integration](#micronaut-integration)
7. [Best Practices](#best-practices)
8. [Common Patterns](#common-patterns)
9. [References](#references)

---

## Overview

AgentUnit's **`@AgenticBootTest`** annotation follows the established pattern of framework-specific testing annotations.

### The Pattern: Framework → @FrameworkTest

Each framework provides a testing annotation that bootstraps their test infrastructure:

```
Spring Boot  → @SpringBootTest
Quarkus      → @QuarkusTest
Micronaut    → @MicronautTest
AgenticBoot  → @AgenticBootTest  ✅
```

**Key Insight:** Just as `@SpringBootTest` is to Spring Boot, **`@AgenticBootTest` is to AgenticBoot**.

This consistent naming makes the relationship clear:
- `@SpringBootTest` starts Spring Boot test infrastructure
- `@QuarkusTest` starts Quarkus test infrastructure
- `@MicronautTest` starts Micronaut test infrastructure
- `@AgenticBootTest` starts AgenticBoot test infrastructure

This guide explains how to use `@AgenticBootTest` effectively, both standalone and in combination with other frameworks.

---

## Framework Testing Annotations

### Comparison Table

| Framework | Testing Annotation | What It Provides |
|-----------|-------------------|------------------|
| **AgenticBoot** | `@AgenticBootTest` | Mock injection (`@MockLLM`, `@MockAgent`), AgenticBoot test utilities |
| **Spring Boot** | `@SpringBootTest` | Spring application context, `@Autowired` injection, Spring beans |
| **Quarkus** | `@QuarkusTest` | Quarkus CDI container, `@Inject` injection, Quarkus beans |
| **Micronaut** | `@MicronautTest` | Micronaut DI container, `@Inject` injection, Micronaut beans |

### Why @AgenticBootTest?

**Consistency with ecosystem:**
- Spring developers expect `@SpringBootTest`
- Quarkus developers expect `@QuarkusTest`
- Micronaut developers expect `@MicronautTest`
- AgenticBoot developers expect `@AgenticBootTest` ✅

**Benefits:**
- ✅ **Familiar pattern** - Follows framework conventions
- ✅ **Clear intent** - "This is an AgenticBoot test"
- ✅ **Auto-configuration** - Mocks injected automatically
- ✅ **Framework-agnostic** - Works with or without DI frameworks

---

## Pure AgenticBoot Testing

### When to Use

Use `@AgenticBootTest` alone when:
- Testing AgenticBoot components in isolation
- No framework dependencies required
- Pure unit tests for agents, tasks, LLM providers

### Example: Agent Registry Test

```java
package dev.adeengineer.platform.core;

import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockAgent;
import dev.adeengineer.platform.test.factory.TestData;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@AgenticBootTest  // Pure AgenticBoot test
class AgentRegistryTest {

    @MockAgent(name = "developer", capabilities = {"coding", "testing"})
    MockAgent developerAgent;

    @MockAgent(name = "reviewer", capabilities = {"code-review"})
    MockAgent reviewerAgent;

    @Test
    void shouldRegisterMultipleAgents() {
        // Pure AgenticBoot - no framework dependencies
        AgentRegistry registry = new AgentRegistry();

        registry.register(developerAgent);
        registry.register(reviewerAgent);

        assertThat(registry.getAllAgents()).hasSize(2);
        assertThat(registry.getAgent("developer")).isEqualTo(developerAgent);
        assertThat(registry.getAgent("reviewer")).isEqualTo(reviewerAgent);
    }

    @Test
    void shouldExecuteAgentTasks() {
        TaskRequest request = TestData.validTaskRequest();
        TaskResult result = developerAgent.executeTask(request);

        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("developer");
    }
}
```

### Example: LLM Provider Test

```java
import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockLLM;
import dev.adeengineer.platform.test.mock.MockLLMProvider;

@AgenticBootTest
class LLMProviderTest {

    @MockLLM(
        responseContent = "Generated code implementation",
        providerName = "openai",
        model = "gpt-4"
    )
    MockLLMProvider llmProvider;

    @Test
    void shouldGenerateResponse() {
        var response = llmProvider.generate("Write a function", 0.7, 500);

        assertThat(response.content()).isEqualTo("Generated code implementation");
        assertThat(response.providerName()).isEqualTo("openai");
        assertThat(response.model()).isEqualTo("gpt-4");
    }

    @Test
    void shouldTrackUsage() {
        var response = llmProvider.generate("test prompt", 0.7, 100);

        assertThat(response.usage()).isNotNull();
        assertThat(response.usage().totalTokens()).isGreaterThan(0);
    }
}
```

---

## Spring Boot Integration

### When to Use

Combine `@SpringBootTest` + `@AgenticBootTest` when:
- Testing Spring services that use AgenticBoot components
- Need Spring dependency injection + AgenticBoot mocks
- Integration testing with Spring application context

### Pattern: Spring Service with AgenticBoot Mocks

```java
package dev.adeengineer.platform.spring;

import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockLLM;
import dev.adeengineer.platform.test.annotation.MockAgent;
import dev.adeengineer.platform.test.mock.MockLLMProvider;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

@SpringBootTest  // Starts Spring application context
@AgenticBootTest     // Enables AgenticBoot mock injection
class AgentServiceIntegrationTest {

    @Autowired
    AgentService agentService;  // Real Spring bean

    @Autowired
    TaskQueue taskQueue;  // Real Spring bean

    @MockLLM(responseContent = "Spring integration test response")
    MockLLMProvider mockLLM;  // AgenticBoot mock

    @MockAgent(name = "spring-agent")
    MockAgent mockAgent;  // AgenticBoot mock

    @Test
    void shouldProcessTaskWithSpringService() {
        // Combine real Spring services with AgenticBoot mocks
        agentService.registerAgent(mockAgent);

        TaskRequest request = TaskRequest.builder()
            .agentName("spring-agent")
            .task("Process with Spring")
            .build();

        TaskResult result = agentService.executeTask(request, mockLLM);

        assertThat(result.success()).isTrue();
        assertThat(taskQueue.size()).isEqualTo(0);  // Spring service cleared queue
    }

    @Test
    void shouldInjectSpringBeansAndAgenticBootMocks() {
        // Verify both injection mechanisms work
        assertThat(agentService).isNotNull();  // Spring injection
        assertThat(taskQueue).isNotNull();     // Spring injection
        assertThat(mockLLM).isNotNull();       // AgenticBoot injection
        assertThat(mockAgent).isNotNull();     // AgenticBoot injection
    }
}
```

### Pattern: Spring Repository Test

```java
import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockAgent;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;

@DataJpaTest  // Spring Data JPA test slice
@AgenticBootTest  // AgenticBoot mocks
class AgentRepositoryTest {

    @Autowired
    AgentRepository repository;  // Real Spring Data repository

    @MockAgent(name = "persistent-agent")
    MockAgent mockAgent;  // AgenticBoot mock

    @Test
    void shouldPersistAgentEntity() {
        AgentEntity entity = new AgentEntity();
        entity.setName(mockAgent.getName());
        entity.setCapabilities(mockAgent.getCapabilities());

        AgentEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("persistent-agent");
    }
}
```

---

## Quarkus Integration

### When to Use

Combine `@QuarkusTest` + `@AgenticBootTest` when:
- Testing Quarkus CDI beans with AgenticBoot components
- Need Quarkus injection + AgenticBoot mocks
- Integration testing with Quarkus application

### Pattern: Quarkus Service with AgenticBoot Mocks

```java
package dev.adeengineer.platform.quarkus;

import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockLLM;
import dev.adeengineer.platform.test.annotation.MockAgent;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest  // Starts Quarkus CDI container
@AgenticBootTest  // Enables AgenticBoot mock injection
class QuarkusAgentServiceTest {

    @Inject
    AgentService agentService;  // Real Quarkus CDI bean

    @Inject
    AgentRegistry registry;  // Real Quarkus CDI bean

    @MockLLM(responseContent = "Quarkus test response")
    MockLLMProvider mockLLM;  // AgenticBoot mock

    @MockAgent(name = "quarkus-agent", capabilities = {"fast-startup"})
    MockAgent mockAgent;  // AgenticBoot mock

    @Test
    void shouldIntegrateWithQuarkusCDI() {
        // Use real Quarkus beans with AgenticBoot mocks
        registry.register(mockAgent);

        TaskResult result = agentService.execute(mockAgent, mockLLM);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Quarkus test response");
    }

    @Test
    void shouldSupportQuarkusAndAgenticBootInjection() {
        assertThat(agentService).isNotNull();  // Quarkus CDI
        assertThat(registry).isNotNull();      // Quarkus CDI
        assertThat(mockLLM).isNotNull();       // AgenticBoot
        assertThat(mockAgent).isNotNull();     // AgenticBoot
    }
}
```

### Pattern: Quarkus REST Resource Test

```java
import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockAgent;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@AgenticBootTest
class AgentResourceTest {

    @MockAgent(name = "rest-agent")
    MockAgent mockAgent;

    @Test
    void shouldExposeAgentViaREST() {
        // Mock prepared by AgenticBoot
        // Test Quarkus REST endpoint
        RestAssured.given()
            .when().get("/agents/" + mockAgent.getName())
            .then()
            .statusCode(200)
            .body("name", equalTo("rest-agent"));
    }
}
```

---

## Micronaut Integration

### When to Use

Combine `@MicronautTest` + `@AgenticBootTest` when:
- Testing Micronaut DI beans with AgenticBoot components
- Need Micronaut injection + AgenticBoot mocks
- Integration testing with Micronaut application

### Pattern: Micronaut Service with AgenticBoot Mocks

```java
package dev.adeengineer.platform.micronaut;

import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockLLM;
import dev.adeengineer.platform.test.annotation.MockAgent;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest  // Starts Micronaut DI container
@AgenticBootTest    // Enables AgenticBoot mock injection
class MicronautAgentServiceTest {

    @Inject
    AgentService agentService;  // Real Micronaut bean

    @Inject
    EventPublisher eventPublisher;  // Real Micronaut bean

    @MockLLM(responseContent = "Micronaut reactive response")
    MockLLMProvider mockLLM;  // AgenticBoot mock

    @MockAgent(name = "micronaut-agent")
    MockAgent mockAgent;  // AgenticBoot mock

    @Test
    void shouldIntegrateWithMicronautDI() {
        // Use real Micronaut beans with AgenticBoot mocks
        agentService.registerAgent(mockAgent);

        TaskResult result = agentService.process(mockAgent, mockLLM);

        assertThat(result.success()).isTrue();
        assertThat(eventPublisher.getEventCount()).isGreaterThan(0);
    }

    @Test
    void shouldSupportReactiveProcessing() {
        // Micronaut reactive + AgenticBoot mocks
        Flux<TaskResult> results = agentService.processReactive(mockAgent, mockLLM);

        StepVerifier.create(results)
            .expectNextMatches(r -> r.success())
            .verifyComplete();
    }
}
```

### Pattern: Micronaut HTTP Client Test

```java
import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockAgent;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;

@MicronautTest
@AgenticBootTest
class AgentControllerTest {

    @Inject
    @Client("/")
    HttpClient client;  // Micronaut HTTP client

    @MockAgent(name = "http-agent")
    MockAgent mockAgent;  // AgenticBoot mock

    @Test
    void shouldReturnAgentViaHTTP() {
        var response = client.toBlocking()
            .retrieve("/agents/" + mockAgent.getName(), AgentDTO.class);

        assertThat(response.getName()).isEqualTo("http-agent");
    }
}
```

---

## Best Practices

### 1. Use Pure @AgenticBootTest for Unit Tests

✅ **DO:**
```java
@AgenticBootTest  // Pure AgenticBoot test
class AgentLogicTest {
    @MockAgent MockAgent agent;
    // Test agent logic in isolation
}
```

❌ **DON'T:**
```java
@SpringBootTest  // Unnecessary framework overhead
@AgenticBootTest
class AgentLogicTest {
    @MockAgent MockAgent agent;
    // No Spring dependencies used
}
```

### 2. Combine Annotations for Integration Tests

✅ **DO:**
```java
@SpringBootTest  // Need Spring beans
@AgenticBootTest     // Need AgenticBoot mocks
class ServiceIntegrationTest {
    @Autowired MyService service;  // Spring bean
    @MockLLM MockLLMProvider llm;  // AgenticBoot mock
}
```

### 3. Keep Test Scopes Appropriate

✅ **DO:**
```java
@DataJpaTest  // Minimal Spring slice
@AgenticBootTest
class RepositoryTest {
    // Only JPA infrastructure loaded
}
```

❌ **DON'T:**
```java
@SpringBootTest  // Loads entire application
@AgenticBootTest
class RepositoryTest {
    // Too heavy for repository test
}
```

### 4. Use Framework Annotations Correctly

✅ **DO:**
```java
@QuarkusTest  // Correct Quarkus annotation
@AgenticBootTest
class QuarkusTest {
    @Inject Service service;
}
```

❌ **DON'T:**
```java
@SpringBootTest  // Wrong framework!
@AgenticBootTest
class QuarkusTest {
    @Inject Service service;  // Won't work
}
```

---

## Common Patterns

### Pattern: Testing Service with External Dependencies

```java
@SpringBootTest
@AgenticBootTest
class PaymentServiceTest {

    @Autowired
    PaymentService paymentService;  // Real service

    @MockLLM(responseContent = "Payment approved")
    MockLLMProvider llmProvider;  // Mock LLM

    @MockAgent(name = "fraud-detector")
    MockAgent fraudAgent;  // Mock agent

    @Test
    void shouldProcessPaymentWithFraudCheck() {
        Payment payment = new Payment(100.00, "USD");

        // Use real service with mocked external dependencies
        PaymentResult result = paymentService.process(payment, fraudAgent, llmProvider);

        assertThat(result.isApproved()).isTrue();
    }
}
```

### Pattern: Testing Async/Reactive Flows

```java
@MicronautTest
@AgenticBootTest
class AsyncAgentServiceTest {

    @Inject
    AsyncAgentService service;

    @MockAgent(name = "async-agent")
    MockAgent mockAgent;

    @Test
    void shouldProcessTasksAsynchronously() {
        // AgenticBoot mock in async context
        CompletableFuture<TaskResult> future = service.executeAsync(mockAgent);

        TaskResult result = future.get(5, TimeUnit.SECONDS);
        assertThat(result.success()).isTrue();
    }
}
```

### Pattern: Testing with Test Profiles

```java
@QuarkusTest
@TestProfile(DevProfile.class)  // Quarkus test profile
@AgenticBootTest
class ProfiledTest {

    @Inject
    @ConfigProperty(name = "app.environment")
    String environment;

    @MockAgent MockAgent agent;

    @Test
    void shouldUseDevProfile() {
        assertThat(environment).isEqualTo("development");
        assertThat(agent).isNotNull();  // AgenticBoot still works
    }
}
```

---

## References

### Official Framework Documentation

1. **Spring Boot Testing**
   - https://docs.spring.io/spring-boot/reference/testing/index.html
   - `@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`

2. **Quarkus Testing Guide**
   - https://quarkus.io/guides/getting-started-testing
   - `@QuarkusTest`, `@TestProfile`, CDI injection

3. **Micronaut Testing**
   - https://docs.micronaut.io/latest/guide/#testing
   - `@MicronautTest`, reactive testing, HTTP clients

### JUnit 5 Extension Model

4. **JUnit 5 User Guide - Extensions**
   - https://junit.org/junit5/docs/current/user-guide/#extensions
   - Extension API, `BeforeEachCallback`, annotation composition

### Related AgentUnit Documentation

5. **AgentUnit README**
   - [README.md](../../README.md)
   - Complete API reference for `@AgenticBootTest` and utilities

6. **Utilities vs Annotations Guide**
   - [utilities-vs-annotations-testing.md](utilities-vs-annotations-testing.md)
   - Comparison of annotation-based vs utilities-based testing

---

**Last Updated:** 2025-10-22
**Version:** 1.0
