# Integration Testing Guide

**Date:** 2025-10-18
**Version:** 1.0

---

## TL;DR

**Integration tests use REAL LLM providers** (Ollama locally) to validate error handling, logging, and failover behavior. **Unlike E2E tests** which mock providers for speed, integration tests make actual API calls to catch real errors. **Prerequisites**: Ollama running with `qwen2.5:0.5b` model. **Value**: Validates error handling strategy, provider failover, real timeouts, and authentication failures. **See**: [Integration Test Setup Guide](integration-test-setup.md) for Ollama installation.

**Quick rule**: Integration tests validate **real error behavior** while E2E tests validate **API contracts** with mocked responses.

---

## Table of Contents

- [Overview](#overview)
- [Integration Tests vs Other Test Types](#integration-tests-vs-other-test-types)
- [What Integration Tests Cover](#what-integration-tests-cover)
- [Test Structure](#test-structure)
- [Running Integration Tests](#running-integration-tests)
- [Writing Integration Tests](#writing-integration-tests)
- [Test Profiles and Configuration](#test-profiles-and-configuration)
- [Examples](#examples)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [References](#references)

---

## Overview

Integration tests fill the gap between isolated unit tests and full E2E HTTP tests. They test **multi-component interactions with Spring context** while avoiding the overhead of web server startup and HTTP serialization.

**The Integration Testing Problem**:
- Unit tests mock everything → Don't catch real errors or failover behavior
- E2E tests test everything → Slow, hard to debug, mocked providers hide errors
- Error handling strategy needs validation → Need real API errors
- Need something in between → Integration tests with real providers

**The Solution**:
- Use `@SpringBootTest` WITHOUT `webEnvironment`
- Load full Spring application context
- Use **REAL LLM provider** (Ollama locally, not mocked)
- Test service layer with real Spring beans
- Validate error handling with actual API failures
- Test provider failover with real health checks

**Key Philosophy**:
- **Integration tests** → Use REAL providers to validate error handling
- **E2E tests** → Mock providers for speed and reliability
- This inverts the traditional approach but validates our error handling strategy

---

## Integration Tests vs Other Test Types

| Aspect | Unit Tests | **Integration Tests** | E2E Tests |
|--------|-----------|----------------------|-----------|
| **Speed** | Fast (~ms) | **Medium (~100ms)** | Slow (~seconds) |
| **Scope** | Single class | **Multi-component** | Full HTTP stack |
| **Spring Context** | ❌ No | **✅ Yes** | ✅ Yes + Web Server |
| **LLM Calls** | Mocked | **✅ REAL (Ollama)** | ❌ Mocked |
| **HTTP Layer** | ❌ No | **❌ No** | ✅ Yes (REST API) |
| **Config Loading** | ❌ No | **✅ Yes (YAML)** | ✅ Yes |
| **Agent Registry** | Mocked | **✅ Real** | ✅ Real |
| **Agent Instances** | Mocked | **✅ Real** | ✅ Real (via HTTP) |
| **Service Layer** | Isolated | **✅ Integrated** | Via HTTP |
| **Error Handling** | ❌ Mocked | **✅ Real errors** | ❌ Mocked |
| **Provider Failover** | ❌ No | **✅ Real health checks** | ❌ Mocked |
| **Best For** | Logic testing | **Error handling validation** | User workflows |

---

## What Integration Tests Cover

### 1. Configuration Loading (`AgentConfigurationIntegrationTest`)

**Tests**:
- YAML file loading from filesystem
- `AgentConfigLoader` parsing agent configurations
- `AppConfig.initializeAgents()` ApplicationRunner
- Agent registration in Spring context
- Configuration validation (temperature, tokens, prompts)

**Value**: Catches config loading bugs before deployment

**Example**:
```java
@Test
void shouldLoadAgentConfigurationsFromYaml() {
    List<AgentConfig> configs = configLoader.loadAllConfigs();

    assertThat(configs).hasSizeGreaterThanOrEqualTo(4);
    assertThat(configs).extracting(AgentConfig::role)
        .contains("Software Developer", "QA Engineer");
}
```

### 2. Service Layer Integration (`RoleManagerIntegrationTest`)

**Tests**:
- RoleManager + AgentRegistry + Real Agents
- Task execution with real agent instances
- Multi-agent coordination
- Role discovery and listing
- Error propagation through layers

**Value**: Tests how services collaborate without HTTP noise

**Example**:
```java
@Test
void shouldExecuteTaskWithRealAgentFromRegistry() {
    TaskRequest request = new TaskRequest(
        "Software Developer",
        "Write unit tests",
        Map.of()
    );

    TaskResult result = roleManager.executeTask(request);

    assertThat(result.success()).isTrue();
    assertThat(result.output()).isNotBlank();
    assertThat(result.usage()).isNotNull();
}
```

### 3. LLM Provider Failover (`LLMProviderFailoverIntegrationTest`)

**Special Case**: This test uses mocked providers (extends `BaseProviderFailoverTest` instead of `BaseIntegrationTest`) because testing specific failover scenarios requires controlling provider health states.

**Tests**:
- Primary provider selection
- Failover logic (Anthropic → OpenAI → Ollama)
- Health check integration
- Provider availability handling

**Value**: Tests failover logic in isolation with controlled health states

**Note**: Real failover behavior is also tested by other integration tests when Ollama is down - you'll see logs showing automatic failover to Anthropic/OpenAI.

**Example**:
```java
@Test
void shouldFailoverToOpenAIWhenAnthropicUnhealthy() {
    when(anthropicProvider.isHealthy()).thenReturn(false);
    when(openAIProvider.isHealthy()).thenReturn(true);

    LLMProvider provider = factory.getProviderWithFailover();

    assertThat(provider.getProviderName()).isEqualTo("openai-test");
}
```

### 4. Multi-Agent Coordination (`MultiAgentCoordinationIntegrationTest`)

**Tests**:
- Parallel agent execution
- Context sharing across agents
- Aggregated results from multiple agents
- Partial failure handling
- Usage statistics aggregation

**Value**: Tests complex workflows without HTTP serialization overhead

**Example**:
```java
@Test
void shouldCoordinateDeveloperQAAndSecurityAgents() {
    List<String> roles = List.of(
        "Software Developer",
        "QA Engineer",
        "Security Engineer"
    );

    Map<String, TaskResult> results = roleManager.executeMultiAgentTask(
        roles, "Build secure payment system", Map.of()
    );

    assertThat(results).hasSize(3);
    results.values().forEach(result ->
        assertThat(result.success()).isTrue()
    );
}
```

### 5. Output Formatting (`AgentOutputFormattingIntegrationTest`)

**Tests**:
- Role-specific output formatting (technical vs business vs executive)
- OutputFormatter integration with agents
- Format directives from YAML config
- Content preservation during formatting

**Value**: Tests config-driven behavior with real components

**Example**:
```java
@Test
void shouldFormatTechnicalOutputForDeveloperAgent() {
    TaskRequest request = new TaskRequest(
        "Software Developer",
        "Review code",
        Map.of()
    );

    TaskResult result = roleManager.executeTask(request);

    assertThat(result.output()).contains("Technical Details");
    assertThat(result.output()).contains("Provider:");
    assertThat(result.output()).contains("Model:");
}
```

---

## Test Structure

### Package Organization

```
src/test/java/com/rolemanager/
├── integration/                         # Integration tests
│   ├── BaseIntegrationTest.java        # Base class with REAL Ollama provider
│   ├── BaseProviderFailoverTest.java   # Base class with mocked providers (failover testing only)
│   ├── IntegrationTestConfiguration.java # Test-specific Spring config
│   ├── AgentConfigurationIntegrationTest.java
│   ├── RoleManagerIntegrationTest.java
│   ├── LLMProviderFailoverIntegrationTest.java  # Only test extending BaseProviderFailoverTest
│   ├── MultiAgentCoordinationIntegrationTest.java
│   └── AgentOutputFormattingIntegrationTest.java
```

### Base Test Classes

#### BaseIntegrationTest (Default - Uses Real Provider)

Most integration tests extend `BaseIntegrationTest` which uses **REAL Ollama provider**:

```java
@SpringBootTest
@ActiveProfiles("integrationtest")
@Import(IntegrationTestConfiguration.class)
public abstract class BaseIntegrationTest {
    // No mocks - uses real Ollama provider from application-integrationtest.yml
}
```

**Philosophy**: Make real API calls to validate error handling, logging, and failover behavior.

**Prerequisites**: Requires Ollama running locally with `qwen2.5:0.5b` model. See [Integration Test Setup Guide](integration-test-setup.md).

#### BaseProviderFailoverTest (Special Case - Uses Mocks)

Only `LLMProviderFailoverIntegrationTest` extends this to test specific failover scenarios:

```java
@SpringBootTest
@ActiveProfiles("integrationtest")
@Import(IntegrationTestConfiguration.class)
public abstract class BaseProviderFailoverTest {

    @MockBean
    protected AnthropicProvider anthropicProvider;

    @MockBean
    protected OpenAIProvider openAIProvider;

    @MockBean
    protected OllamaProvider ollamaProvider;

    @BeforeEach
    void setUpBase() {
        // Configure mock providers to control health states
        when(anthropicProvider.isHealthy()).thenReturn(true);
        when(openAIProvider.isHealthy()).thenReturn(false);
        when(ollamaProvider.isHealthy()).thenReturn(false);
    }
}
```

**Use case**: Testing failover logic requires controlling provider health states, which can't be done reliably with real providers.

### Test Configuration

`IntegrationTestConfiguration` excludes `@Component` agent classes to allow programmatic initialization:

```java
@TestConfiguration
@ComponentScan(
    basePackages = "adeengineer.dev.platform",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            DeveloperAgent.class,
            ManagerAgent.class,
            QAAgent.class,
            SecurityAgent.class
        }
    )
)
public class IntegrationTestConfiguration {
    // No custom beans - uses real AppConfig initialization
}
```

---

## Running Integration Tests

### Prerequisites

**IMPORTANT**: Integration tests require Ollama running locally.

1. Install Ollama: https://ollama.com/download
2. Pull model: `ollama pull qwen2.5:0.5b`
3. Start server: `ollama serve`

See [Integration Test Setup Guide](integration-test-setup.md) for detailed instructions.

### Run All Integration Tests

```bash
mvn test -Dtest="*IntegrationTest"
```

**Expected output**:
```
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Run Specific Integration Test Class

```bash
mvn test -Dtest="RoleManagerIntegrationTest"
```

### Run Specific Test Method

```bash
mvn test -Dtest="RoleManagerIntegrationTest#shouldExecuteTaskWithRealAgentFromRegistry"
```

### Run with Verbose Output

```bash
mvn test -Dtest="*IntegrationTest" -X
```

### Run Integration Tests in IDE

**IntelliJ IDEA**:
1. Right-click on `src/test/java/com/rolemanager/integration` package
2. Select "Run Tests in 'integration'"

**VS Code**:
1. Open test file
2. Click "Run Test" or "Debug Test" above test method

---

## Writing Integration Tests

### Step 1: Create Test Class

Extend `BaseIntegrationTest` and use descriptive class name:

```java
package adeengineer.dev.platform.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Feature Name Integration Tests")
class FeatureIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ServiceUnderTest service;

    @Test
    @DisplayName("Should test specific behavior")
    void shouldTestSpecificBehavior() {
        // Test implementation
    }
}
```

### Step 2: Inject Real Spring Beans

Use `@Autowired` to inject services you want to test:

```java
@Autowired
private RoleManager roleManager;

@Autowired
private AgentRegistry registry;

@Autowired
private OutputFormatter formatter;
```

### Step 3: Write Tests Using AAA Pattern

**Arrange → Act → Assert**:

```java
@Test
@DisplayName("Should coordinate multiple agents")
void shouldCoordinateMultipleAgents() {
    // Arrange
    List<String> roles = List.of("Developer", "QA");
    String task = "Build feature";
    Map<String, Object> context = Map.of("priority", "high");

    // Act
    Map<String, TaskResult> results = roleManager.executeMultiAgentTask(
        roles, task, context
    );

    // Assert
    assertThat(results).hasSize(2);
    assertThat(results.get("Developer").success()).isTrue();
    assertThat(results.get("QA").success()).isTrue();
}
```

### Step 4: Test Both Success and Failure Cases

```java
@Test
@DisplayName("Should handle unknown role gracefully")
void shouldHandleUnknownRoleGracefully() {
    // Arrange
    TaskRequest request = new TaskRequest(
        "NonExistentRole",
        "Some task",
        Map.of()
    );

    // Act & Assert
    assertThatThrownBy(() -> roleManager.executeTask(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown role");
}
```

---

## Test Profiles and Configuration

### Integration Test Profile

Integration tests use the `integrationtest` profile defined in `application-integrationtest.yml`:

```yaml
spring:
  application:
    name: role-manager-app-integration

agents:
  config-dir: config/agents

llm:
  primary-provider: anthropic

logging:
  level:
    root: INFO
    adeengineer.dev.platform: DEBUG
```

### Agent Profile Exclusion

Agent classes use `@Profile("!e2etest & !integrationtest")` to prevent autowiring conflicts:

```java
@Component
@Profile("!e2etest & !integrationtest")
public class DeveloperAgent extends BaseAgent {
    // Agent implementation
}
```

**Why This Matters**:
- Production: Agents created via `AppConfig.initializeAgents()` ApplicationRunner
- Integration Tests: Agents created via same ApplicationRunner with test profile
- E2E Tests: Agents created manually in `E2ETestConfiguration`

---

## Examples

### Example 1: Testing Configuration Loading

```java
@Test
@DisplayName("Should load all agent configurations from YAML")
void shouldLoadAllAgentConfigurationsFromYaml() {
    // When
    List<AgentConfig> configs = configLoader.loadAllConfigs();

    // Then
    assertThat(configs).isNotEmpty();
    assertThat(configs).hasSizeGreaterThanOrEqualTo(4);

    for (AgentConfig config : configs) {
        assertThat(config.role()).isNotBlank();
        assertThat(config.promptTemplate()).isNotBlank();
        assertThat(config.temperature()).isBetween(0.0, 1.0);
        assertThat(config.maxTokens()).isPositive();
    }
}
```

### Example 2: Testing Service Layer Integration

```java
@Test
@DisplayName("Should execute multi-agent task with real agents")
void shouldExecuteMultiAgentTaskWithRealAgents() {
    // Given
    List<String> roles = List.of("Software Developer", "QA Engineer");
    String task = "Design and test new feature";
    Map<String, Object> context = Map.of("feature", "user authentication");

    // When
    Map<String, TaskResult> results = roleManager.executeMultiAgentTask(
        roles, task, context
    );

    // Then
    assertThat(results).hasSize(2);
    assertThat(results).containsKeys("Software Developer", "QA Engineer");

    TaskResult devResult = results.get("Software Developer");
    TaskResult qaResult = results.get("QA Engineer");

    assertThat(devResult.success()).isTrue();
    assertThat(qaResult.success()).isTrue();
    assertThat(devResult.output()).isNotBlank();
    assertThat(qaResult.output()).isNotBlank();
}
```

### Example 3: Testing Provider Failover

```java
@Test
@DisplayName("Should failover to Ollama when others unhealthy")
void shouldFailoverToOllamaWhenOthersUnhealthy() {
    // Given
    when(anthropicProvider.isHealthy()).thenReturn(false);
    when(openAIProvider.isHealthy()).thenReturn(false);
    when(ollamaProvider.isHealthy()).thenReturn(true);

    // When
    LLMProvider provider = factory.getProviderWithFailover();

    // Then
    assertThat(provider).isNotNull();
    assertThat(provider.getProviderName()).isEqualTo("ollama-test");
}
```

---

## Best Practices

### DO ✅

1. **Test Multi-Component Interactions**
   - Use integration tests when multiple Spring beans interact
   - Example: RoleManager + AgentRegistry + Agents

2. **Mock Only External Dependencies**
   - Mock LLM providers (external API calls)
   - Use real Spring beans for everything else

3. **Test Configuration Loading**
   - Verify YAML parsing
   - Validate ApplicationRunner initialization
   - Check bean wiring

4. **Use Descriptive Test Names**
   ```java
   @DisplayName("Should execute task with real agent from registry")
   void shouldExecuteTaskWithRealAgentFromRegistry()
   ```

5. **Test Both Success and Error Paths**
   - Happy path: Valid input → Success
   - Error path: Invalid input → Exception

6. **Verify Complete Behavior**
   - Check result fields (output, usage, duration)
   - Validate error messages
   - Assert state changes

### DON'T ❌

1. **Don't Test HTTP Layer**
   - Use E2E tests for REST API testing
   - Integration tests bypass HTTP

2. **Don't Make Real LLM API Calls**
   - Always mock LLM providers
   - Keep tests fast and deterministic

3. **Don't Test Single Components in Isolation**
   - Use unit tests for isolated component testing
   - Integration tests are for collaboration

4. **Don't Duplicate Unit Test Logic**
   - Integration tests test different concerns
   - Focus on multi-component interactions

5. **Don't Ignore Test Performance**
   - Integration tests should be < 1 second each
   - Use `@BeforeEach` wisely (runs before every test)

---

## Troubleshooting

### Problem: Spring Context Fails to Load

**Symptom**:
```
No qualifying bean of type 'AgentConfig' available
```

**Solution**:
- Verify agent classes have `@Profile("!e2etest & !integrationtest")`
- Check `IntegrationTestConfiguration` excludes agent classes
- Confirm `application-integrationtest.yml` exists

### Problem: Tests Are Slow

**Symptom**:
```
Tests take > 1 second each
```

**Solution**:
- Check if Spring context is being reloaded per test (should reuse)
- Verify `@MockBean` setup in `BaseIntegrationTest`
- Look for unnecessary `@DirtiesContext` annotations

### Problem: Agent Not Found

**Symptom**:
```
Unknown role: Software Developer
```

**Solution**:
- Verify agent YAML config exists in `config/agents/`
- Check `AgentConfigLoader` is loading configs
- Confirm `ApplicationRunner` executed successfully

### Problem: Mock Provider Not Working

**Symptom**:
```
NullPointerException when calling provider.generate()
```

**Solution**:
- Check `BaseIntegrationTest.setUpBase()` is being called
- Verify `@MockBean` annotations on providers
- Ensure test extends `BaseIntegrationTest`

---

## References

### Internal Documentation

- **E2E Testing Guide**: `doc/4-development/guide/spring-profiles-e2e-testing.md`
- **Error Handling Strategy**: `doc/3-design/error-handling-strategy.md`
- **Developer Guide**: `doc/4-development/developer-guide.md`

### Official Documentation

1. **Spring Boot Testing**
   - https://docs.spring.io/spring-boot/reference/testing/index.html
   - https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html

2. **Spring Test Context Framework**
   - https://docs.spring.io/spring-framework/reference/testing/testcontext-framework.html
   - https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-springboottest.html

3. **Spring Profiles**
   - https://docs.spring.io/spring-boot/reference/features/profiles.html
   - https://docs.spring.io/spring-framework/reference/core/beans/environment.html

4. **AssertJ Documentation**
   - https://assertj.github.io/doc/
   - https://www.baeldung.com/introduction-to-assertj

### Best Practices

1. **Testing Best Practices**
   - https://martinfowler.com/articles/practical-test-pyramid.html
   - https://www.baeldung.com/spring-boot-testing

2. **Integration Testing Patterns**
   - https://www.baeldung.com/integration-testing-in-spring
   - https://rieckpil.de/spring-boot-integration-tests-setup-guide/

---

**Last Updated**: 2025-10-18
**Version**: 1.0.0
**Test Coverage**: 52 integration tests across 5 test classes
