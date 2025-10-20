# Implementation Test Strategy - Role Manager App

**Date:** 2025-10-18
**Version:** 2.0
**SDLC Phase:** 3 - Design (Testing Strategy Decision)

---

## TL;DR

**Three-layer testing strategy**: Unit (fast, isolated) → Integration (medium, multi-component, **REAL Ollama**) → E2E (slow, full stack, **mocked providers**). **Inverted philosophy**: Integration tests use REAL LLM provider to validate error handling; E2E tests mock providers for fast API contract testing. **Current coverage**: 10 unit tests + 52 integration tests + 6 E2E tests = 68 total tests. **Test pyramid**: 15% unit, 76% integration, 9% E2E.

**Key insight**: Integration tests validate **error behavior** with real API calls; E2E tests validate **API contracts** with mocks. **Prerequisites**: Ollama running with `qwen2.5:0.5b` for integration tests.

---

## Table of Contents

- [Overview](#overview)
- [Test Types](#test-types)
- [Test Pyramid](#test-pyramid)
- [Current Test Coverage](#current-test-coverage)
- [When to Use Each Test Type](#when-to-use-each-test-type)
- [Test Execution](#test-execution)
- [Test Organization](#test-organization)
- [Testing Workflow](#testing-workflow)
- [Quality Metrics](#quality-metrics)
- [Best Practices](#best-practices)
- [References](#references)

---

## Overview

The Role Manager App uses a **three-layer testing strategy** that balances speed, confidence, and maintainability:

1. **Unit Tests** - Fast, isolated component testing with mocked dependencies
2. **Integration Tests** - Multi-component testing with **REAL LLM provider** (Ollama), validates error handling
3. **E2E Tests** - Full application testing via REST API with **mocked LLM providers**

### Inverted Philosophy: Real Providers in Integration, Mocks in E2E

**Traditional Approach**:
- Integration tests mock external APIs ❌ (hides real errors)
- E2E tests use real APIs ✅ (slow, expensive)

**Our Approach** (validates error handling strategy):
- **Integration tests** → Use REAL Ollama provider ✅ (validates error handling, logging, failover)
- **E2E tests** → Mock LLM providers ✅ (fast, reliable API contract testing)

**Why This Works**:
- Integration tests validate **error behavior** with real API errors
- E2E tests validate **API contracts** and don't need real LLM responses
- Faster E2E tests (no external API dependency)
- Better error handling coverage (real timeouts, auth failures, rate limits)

This strategy provides:
- **Fast feedback** during development (unit tests < 1 second, integration < 20 seconds)
- **High confidence** in error handling (real provider errors in integration tests)
- **Full workflow validation** (E2E tests with mocked providers)
- **Easy debugging** when tests fail (isolation at appropriate layers)

---

## Test Types

### 1. Unit Tests

**Purpose**: Test individual components in isolation

**Characteristics**:
- No Spring context loaded
- All dependencies mocked (Mockito)
- Extremely fast (< 10ms per test)
- Focus on business logic

**Example Use Cases**:
- Model validation (`AgentConfig` constructor validation)
- Service logic (`RoleManager` with mocked registry)
- Controller logic (`TaskController` with mocked service)

**Technology Stack**:
- JUnit 5 (Jupiter)
- Mockito for mocking
- AssertJ for assertions
- `@ExtendWith(MockitoExtension.class)` for unit tests
- `@WebMvcTest` for controller tests

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class RoleManagerTest {
    @Mock
    private AgentRegistry mockRegistry;

    @InjectMocks
    private RoleManager roleManager;

    @Test
    void shouldExecuteTaskWithAgent() {
        // Test with mocked dependencies
    }
}
```

### 2. Integration Tests ⭐ **Most Used**

**Purpose**: Test multi-component collaboration with Spring context AND validate error handling with real LLM provider

**Characteristics**:
- Full Spring context loaded (no web server)
- Real Spring beans INCLUDING **real Ollama provider**
- Medium speed (~100ms per test)
- Focus on component interaction AND error handling validation

**What Makes This Special**:
- Uses **REAL Ollama provider** (not mocked!)
- Validates error handling strategy with actual API errors
- Tests provider failover with real health checks
- Observes real timeout, authentication, and rate limit errors
- Validates logging behavior with actual error responses

**Example Use Cases**:
- Configuration loading and agent initialization
- Service layer collaboration (RoleManager + AgentRegistry + Agents)
- **Error handling validation with real API failures**
- **Provider failover with real health checks**
- Multi-agent coordination with real LLM calls
- Output formatting integration

**Technology Stack**:
- `@SpringBootTest` (no web environment)
- **REAL Ollama provider** from `application-integrationtest.yml`
- `@ActiveProfiles("integrationtest")`
- Real Spring bean wiring
- Exception: `LLMProviderFailoverIntegrationTest` uses `@MockBean` to control health states

**Prerequisites**:
- Requires Ollama running locally with `qwen2.5:0.5b` model
- See [Integration Test Setup Guide](guide/integration-test-setup.md)

**Example**:
```java
@SpringBootTest
@ActiveProfiles("integrationtest")
class RoleManagerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private RoleManager roleManager;

    @Test
    void shouldExecuteTaskWithRealAgent() {
        // Test with real Spring beans AND real Ollama provider
        // Will see actual API calls, errors, and failover behavior
    }
}
```

### 3. E2E Tests

**Purpose**: Test complete user workflows via REST API with mocked LLM providers

**Characteristics**:
- Full Spring Boot application with web server
- HTTP requests via `TestRestTemplate`
- **Mocked LLM providers** (`@MockBean` for all providers)
- Slow (~2-5 seconds per test due to web server startup)
- Focus on API contracts, HTTP layer, and user workflows

**What Makes This Special**:
- Tests HTTP request/response serialization
- Validates REST API contracts (status codes, response formats)
- No external API dependencies (fast, reliable)
- Tests full stack including web layer

**Example Use Cases**:
- Role discovery via GET /api/roles
- Task execution via POST /api/tasks/execute
- Multi-agent workflows via POST /api/tasks/multi-agent
- HTTP status code validation (200, 400, 404, 500)
- Error response validation (ErrorResponse format)
- Request validation (missing fields, invalid JSON)

**Technology Stack**:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `TestRestTemplate` for HTTP calls
- **`@MockBean`** for AnthropicProvider, OpenAIProvider, OllamaProvider
- `@ActiveProfiles("e2etest")`
- Custom test configuration (E2ETestConfiguration)

**Why Mock Providers in E2E?**
- E2E tests focus on HTTP layer, not LLM behavior
- Faster tests (no real API calls)
- More reliable (no external dependency)
- Integration tests already validate real provider behavior

**Example**:
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("e2etest")
class DeveloperAgentE2ETest extends BaseE2ETest {
    @Test
    void shouldExecuteTaskViaAPI() {
        ResponseEntity<TaskResult> response = restTemplate.postForEntity(
            apiUrl("/tasks/execute"),
            request,
            TaskResult.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Provider was mocked, response is deterministic
    }
}
```

---

## Test Pyramid

### Visual Representation

```
                 /\
                /  \
               / E2E\ (6 tests, 9%)
              /______\
             /        \
            /   Integ  \ (52 tests, 76%)
           /____________\
          /              \
         /      Unit      \ (10 tests, 15%)
        /__________________\
```

### Current Distribution

| Test Type | Count | Percentage | Avg Speed | Purpose |
|-----------|-------|------------|-----------|---------|
| **Unit** | 10 | 15% | ~10ms | Logic validation |
| **Integration** | 52 | 76% | ~100ms | Component collaboration |
| **E2E** | 6 | 9% | ~3s | Workflow validation |
| **Total** | **68** | **100%** | - | - |

### Why This Distribution?

**Traditional Pyramid**: 70% unit, 20% integration, 10% E2E

**Our Pyramid**: 15% unit, 76% integration, 9% E2E

**Rationale**:
1. **Service-Layer Complexity**: Most business logic is in multi-component interactions
2. **Spring Wiring**: Need to test bean configuration and dependency injection
3. **Fast Feedback**: Integration tests are fast enough (~100ms) to run frequently
4. **High Value**: Integration tests catch more bugs than pure unit tests
5. **Less Mocking**: Integration tests use real components → more realistic

**Trade-off**: Slightly slower test suite, but catches more integration bugs early.

---

## Current Test Coverage

### Unit Tests (10 tests)

**API Layer** (2 tests):
- `RoleControllerTest` - Controller logic with mocked RoleManager
- `TaskControllerTest` - Controller logic with mocked RoleManager

**Core Layer** (3 tests):
- `RoleManagerTest` - Service logic with mocked AgentRegistry
- `AgentRegistryTest` - Registry logic (no dependencies)
- `OutputFormatterTest` - Formatting logic (no dependencies)

**Model Layer** (5 tests):
- `AgentConfigTest` - Constructor validation
- `TaskRequestTest` - Constructor validation
- `RoleInfoTest` - Model structure
- `TaskResultTest` - Model structure
- `UsageInfoTest` - Constructor validation

### Integration Tests (52 tests) ⭐

**AgentConfigurationIntegrationTest** (8 tests):
- Load agent configurations from YAML files
- Register agents from configurations on startup
- Create agents with correct configurations
- Load configuration by specific role name
- Validate agent registry on startup
- Load configurations with prompt templates
- Load configurations with valid parameter ranges

**RoleManagerIntegrationTest** (11 tests):
- Execute task with real agent from registry
- List all registered roles
- Describe specific role from registry
- Throw exception for unknown role
- Execute multiple tasks with same agent
- Execute tasks with different agents
- Execute multi-agent task with real agents
- Throw exception for multi-agent task with unknown role
- Handle task execution with context parameters
- Return usage information for executed tasks
- Measure task execution duration

**LLMProviderFailoverIntegrationTest** (10 tests):
- Select primary provider when healthy
- Failover to OpenAI when Anthropic unhealthy
- Failover to Ollama when others unhealthy
- Return primary provider when all unhealthy
- Get specific provider by name
- Throw exception for unknown provider name
- Use healthy provider for generation
- Failover during generation if primary fails
- Get primary provider based on configuration
- Maintain failover order consistency

**MultiAgentCoordinationIntegrationTest** (10 tests):
- Coordinate Developer and QA agents
- Coordinate Developer, QA, and Security agents
- Coordinate all four agents together
- Execute agents efficiently
- Handle context sharing across agents
- Handle partial failure gracefully
- Handle empty role list
- Handle single agent in multi-agent task
- Collect usage statistics from all agents
- Handle duplicate roles

**AgentOutputFormattingIntegrationTest** (13 tests):
- Format technical output for Developer agent
- Format technical output for QA agent
- Format technical output for Security agent
- Format executive output for Manager agent
- Apply formatter with technical format type
- Apply formatter with business format type
- Apply formatter with executive format type
- Return raw content for unknown format type
- Handle null response gracefully
- Have different output formats for different agent types
- Preserve content when formatting
- Format cost correctly in business format
- Format tokens correctly in technical format

### E2E Tests (6 tests)

**RoleDiscoveryE2ETest** (2 tests):
- GET /api/roles - List all roles
- GET /api/roles/{name} - Get specific role (404 for unknown)

**DeveloperAgentE2ETest** (1 test):
- POST /api/tasks/execute - Execute developer task

**QAAgentE2ETest** (1 test):
- POST /api/tasks/execute - Execute QA task

**ManagerAgentE2ETest** (1 test):
- POST /api/tasks/execute - Execute manager task

**MultiAgentE2ETest** (1 test):
- POST /api/tasks/multi-agent - Execute multi-agent task

---

## When to Use Each Test Type

### Use Unit Tests When:

✅ Testing **pure business logic** (no Spring dependencies)
✅ Testing **input validation** (model constructors)
✅ Testing **single method behavior** in isolation
✅ Need **extremely fast feedback** (< 1 second for all tests)
✅ Testing **edge cases** with many permutations

**Examples**:
- `AgentConfig` constructor throws exception for invalid temperature
- `TaskRequest` validation rejects null/blank inputs
- `RoleManager.executeTask()` propagates IllegalArgumentException

### Use Integration Tests When: ⭐

✅ Testing **multi-component collaboration**
✅ Testing **Spring bean wiring and configuration**
✅ Testing **ApplicationRunner initialization**
✅ Testing **service layer without HTTP overhead**
✅ Testing **error handling with REAL API failures** ⭐ **NEW**
✅ Testing **provider failover with real health checks** ⭐ **NEW**
✅ Testing **logging behavior with actual errors** ⭐ **NEW**
✅ Testing **multi-agent coordination**
✅ Testing **configuration loading from YAML**
✅ Testing **timeout and rate limit handling** ⭐ **NEW**

**Examples**:
- `AppConfig.initializeAgents()` loads YAMLs and registers agents
- `RoleManager` + `AgentRegistry` + real agents execute task with real Ollama
- **Real Ollama errors propagate through all layers and get logged correctly**
- **Provider health check fails → automatic failover to next provider**
- `LLMProviderFactory` fails over from unhealthy Ollama to Anthropic
- Multiple agents execute in parallel with real LLM calls and aggregate results

**Special Case - Failover Testing**:
- `LLMProviderFailoverIntegrationTest` uses mocked providers to control health states
- All other integration tests use real Ollama provider

### Use E2E Tests When:

✅ Testing **complete user workflows via REST API**
✅ Testing **HTTP status codes and error responses**
✅ Testing **API contracts** (request/response structure)
✅ Testing **end-to-end scenarios** from user perspective
✅ Testing **error handling at HTTP layer**

**Examples**:
- GET /api/roles returns 200 with list of roles
- POST /api/tasks/execute with unknown role returns 400
- Multi-agent workflow via API returns aggregated results
- Invalid request returns appropriate HTTP error code

---

## Test Execution

### Run All Tests

```bash
mvn test
```

**Expected output**:
```
Tests run: 68, Failures: 0, Errors: 0, Skipped: 0
Time: ~25 seconds
BUILD SUCCESS
```

### Run by Test Type

```bash
# Unit tests only (fast, no prerequisites)
mvn test -Dtest="*Test" -DexcludeGroups="integration,e2e"

# Integration tests only (requires Ollama running!)
mvn test -Dtest="*IntegrationTest"

# E2E tests only (slow, no external dependencies)
mvn test -Dtest="*E2ETest"
```

**⚠️ Prerequisites for Integration Tests**:
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull model
ollama pull qwen2.5:0.5b

# Start server
ollama serve
```

See [Integration Test Setup Guide](guide/integration-test-setup.md) for details.

### Run Specific Test Class

```bash
mvn test -Dtest="RoleManagerIntegrationTest"
```

### Run Specific Test Method

```bash
mvn test -Dtest="RoleManagerIntegrationTest#shouldExecuteTaskWithRealAgent"
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
```

View report: `target/site/jacoco/index.html`

### Continuous Integration

Tests run automatically on:
- Every commit (pre-commit hook)
- Every push to GitHub
- Pull request creation

---

## Test Organization

### Directory Structure

```
src/test/
├── java/com/rolemanager/
│   ├── api/                    # Unit tests for controllers
│   │   ├── RoleControllerTest.java
│   │   └── TaskControllerTest.java
│   ├── core/                   # Unit tests for services
│   │   ├── RoleManagerTest.java
│   │   ├── AgentRegistryTest.java
│   │   └── OutputFormatterTest.java
│   ├── model/                  # Unit tests for models
│   │   ├── AgentConfigTest.java
│   │   ├── TaskRequestTest.java
│   │   └── ... (5 model tests)
│   ├── integration/            # Integration tests
│   │   ├── BaseIntegrationTest.java
│   │   ├── IntegrationTestConfiguration.java
│   │   ├── AgentConfigurationIntegrationTest.java
│   │   ├── RoleManagerIntegrationTest.java
│   │   ├── LLMProviderFailoverIntegrationTest.java
│   │   ├── MultiAgentCoordinationIntegrationTest.java
│   │   └── AgentOutputFormattingIntegrationTest.java
│   ├── e2e/                    # E2E tests
│   │   ├── BaseE2ETest.java
│   │   ├── E2ETestConfiguration.java
│   │   ├── RoleDiscoveryE2ETest.java
│   │   ├── DeveloperAgentE2ETest.java
│   │   ├── QAAgentE2ETest.java
│   │   ├── ManagerAgentE2ETest.java
│   │   └── MultiAgentE2ETest.java
│   └── testutil/               # Test utilities
│       ├── TestData.java
│       ├── TestLLMProvider.java
│       └── MockAgent.java
└── resources/
    ├── application-test.yml
    ├── application-integrationtest.yml
    ├── application-e2etest.yml
    └── agents/                 # Test agent configs
        ├── developer.yaml
        ├── qa.yaml
        ├── security.yaml
        └── manager.yaml
```

### Naming Conventions

**Test Classes**:
- Unit: `{ClassName}Test.java`
- Integration: `{Feature}IntegrationTest.java`
- E2E: `{Feature}E2ETest.java`

**Test Methods**:
- Format: `should{ExpectedBehavior}When{Condition}`
- Example: `shouldReturnRoleInfoWhenRoleExists()`
- Alternative: `shouldExecuteTaskWithRealAgent()`

**Display Names**:
```java
@DisplayName("Should execute task with real agent from registry")
```

---

## Testing Workflow

### Development Workflow

```
1. Write failing test
   ├── Unit test for new logic
   ├── Integration test for new feature
   └── E2E test for new API endpoint

2. Run tests → RED
   mvn test -Dtest="NewFeatureTest"

3. Implement feature

4. Run tests → GREEN
   mvn test -Dtest="NewFeatureTest"

5. Refactor

6. Run all tests → GREEN
   mvn test

7. Commit
   git commit -m "feat: add new feature"
```

### Pre-Commit Testing

**Automatic** (via pre-commit hook):
```
1. trim trailing whitespace
2. check yaml syntax
3. maven validate
4. checkstyle
5. maven test-compile
```

**Manual** (recommended):
```bash
# Run fast tests before commit
mvn test -Dtest="*Test,*IntegrationTest"

# Run all tests before push
mvn test
```

### CI/CD Pipeline

```yaml
# GitHub Actions example
jobs:
  test:
    steps:
      - name: Run unit tests
        run: mvn test -Dtest="*Test"

      - name: Run integration tests
        run: mvn test -Dtest="*IntegrationTest"

      - name: Run E2E tests
        run: mvn test -Dtest="*E2ETest"

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

---

## Quality Metrics

### Test Coverage Goals

| Layer | Goal | Current | Status |
|-------|------|---------|--------|
| **API** | 90% | ~95% | ✅ Excellent |
| **Core** | 85% | ~90% | ✅ Excellent |
| **Model** | 95% | ~98% | ✅ Excellent |
| **Overall** | 85% | ~90% | ✅ Excellent |

### Test Performance Goals

| Test Type | Goal | Current | Status |
|-----------|------|---------|--------|
| **Unit** | < 1s total | ~0.5s | ✅ Excellent |
| **Integration** | < 20s total | ~20s | ✅ Good |
| **E2E** | < 30s total | ~20s | ✅ Excellent |
| **All Tests** | < 1 minute | ~25s | ✅ Excellent |

### Quality Gates

**Before Merge**:
- ✅ All tests pass (100%)
- ✅ Code coverage ≥ 85%
- ✅ No checkstyle violations
- ✅ Maven validate succeeds

**Before Release**:
- ✅ All tests pass
- ✅ E2E tests pass with real LLM providers (manual)
- ✅ Performance tests pass
- ✅ Security scan clean

---

## Best Practices

### General Testing Principles

1. **AAA Pattern** (Arrange, Act, Assert)
   ```java
   @Test
   void shouldReturnTrueWhenValid() {
       // Arrange
       String input = "valid";

       // Act
       boolean result = validator.validate(input);

       // Assert
       assertThat(result).isTrue();
   }
   ```

2. **One Assertion Per Test** (when possible)
   - Focus on single behavior
   - Easier to debug failures
   - More maintainable

3. **Descriptive Names**
   ```java
   // Good
   shouldReturnRoleInfoWhenRoleExists()

   // Bad
   testGetRole()
   ```

4. **Test Independence**
   - Tests should not depend on each other
   - Order should not matter
   - Use `@BeforeEach` for setup

5. **Fast Feedback**
   - Unit tests: < 10ms
   - Integration tests: < 200ms
   - E2E tests: < 5s

### Unit Testing Best Practices

- ✅ Mock all dependencies
- ✅ Test edge cases (null, empty, invalid)
- ✅ Use `@ExtendWith(MockitoExtension.class)`
- ✅ Verify mock interactions with `verify()`
- ❌ Don't load Spring context
- ❌ Don't make external calls

### Integration Testing Best Practices

- ✅ Extend `BaseIntegrationTest`
- ✅ Use `@Autowired` for real beans
- ✅ Mock only external dependencies (LLM providers)
- ✅ Test multi-component collaboration
- ✅ Verify Spring configuration loading
- ❌ Don't test HTTP layer
- ❌ Don't duplicate unit test logic

### E2E Testing Best Practices

- ✅ Extend `BaseE2ETest`
- ✅ Use `TestRestTemplate` for HTTP calls
- ✅ Test complete user workflows
- ✅ Verify HTTP status codes
- ✅ Test error responses
- ❌ Don't test internal implementation details
- ❌ Don't make real LLM API calls

---

## References

### Internal Documentation

- **Integration Testing Guide**: `doc/4-development/guide/integration-testing-guide.md`
- **E2E Testing Guide**: `doc/4-development/guide/spring-profiles-e2e-testing.md`
- **Error Handling Strategy**: `doc/3-design/error-handling-strategy.md`
- **Developer Guide**: `doc/4-development/developer-guide.md`

### External Resources

1. **Testing Best Practices**
   - https://martinfowler.com/articles/practical-test-pyramid.html
   - https://www.baeldung.com/spring-boot-testing
   - https://rieckpil.de/testing-spring-boot-applications-masterclass/

2. **Spring Boot Testing**
   - https://docs.spring.io/spring-boot/reference/testing/index.html
   - https://docs.spring.io/spring-framework/reference/testing.html

3. **JUnit 5**
   - https://junit.org/junit5/docs/current/user-guide/
   - https://www.baeldung.com/junit-5

4. **AssertJ**
   - https://assertj.github.io/doc/
   - https://www.baeldung.com/introduction-to-assertj

5. **Mockito**
   - https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
   - https://www.baeldung.com/mockito-series

---

**Last Updated**: 2025-10-18
**Version**: 2.0.0
**Total Test Count**: 68 tests (10 unit + 52 integration + 6 E2E)
**Test Execution Time**: ~25 seconds
