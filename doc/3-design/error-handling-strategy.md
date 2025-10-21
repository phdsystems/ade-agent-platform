# Error Handling Strategy

## TL;DR

**Layered error handling with fail-fast validation**: Model layer validates in constructors → Service layer propagates validation errors and catches execution failures → Controller layer maps exceptions to HTTP status codes (400/404/500). **Key rule**: Validation errors throw `IllegalArgumentException` → Controller returns 400/404. Execution errors → Log full context with stack trace → Return 500 or `TaskResult.failure()`. **Anti-pattern**: Silent failures, swallowed exceptions, generic error messages without context.

**Quick decision**: Input validation? → Fail fast in constructor/service with `IllegalArgumentException`. External system failure? → Return `TaskResult.failure()` with error message. Unexpected error? → Log with stack trace, return 500.

---

## Table of Contents

- [Overview](#overview)
- [Quick Reference](#quick-reference)
- [Error Categories](#error-categories)
- [Layer-by-Layer Strategy](#layer-by-layer-strategy)
  - [1. Model Layer: Constructor Validation](#1-model-layer-constructor-validation)
  - [2. Service Layer: Business Logic](#2-service-layer-business-logic)
  - [3. Controller Layer: HTTP Response Mapping](#3-controller-layer-http-response-mapping)
- [Exception Type Guidelines](#exception-type-guidelines)
- [Logging Standards](#logging-standards)
- [Test Patterns](#test-patterns)
- [Design Decisions](#design-decisions)
- [Examples](#examples)
- [References](#references)

---

## Overview

This document describes the error handling strategy for the Role Manager application. The strategy follows a **layered approach** where each layer has specific responsibilities for validation, error propagation, and error response.

**Core Principles**:
1. **Fail fast** - Validate inputs as early as possible
2. **Meaningful errors** - Provide context-rich error messages
3. **Appropriate logging** - Log errors at the correct level with full context
4. **Graceful degradation** - Handle execution failures without crashing
5. **Simple and predictable** - Direct exception handling without complex frameworks

---

## Quick Reference

### HTTP Status Code Mapping

| Scenario | Exception Type | HTTP Status | Example |
|----------|---------------|-------------|---------|
| Unknown role | `IllegalArgumentException` | `404 Not Found` | `GET /api/roles/NonExistent` |
| Invalid input (validation) | `IllegalArgumentException` | `400 Bad Request` | Blank task, negative tokens |
| Execution failure (system) | `Exception` (catch-all) | `500 Internal Server Error` | LLM connection timeout |
| Success | - | `200 OK` | Normal operation |

### Exception Hierarchy

```
IllegalArgumentException  → Validation errors (400/404)
  ├─ Null or blank inputs
  ├─ Unknown role names
  └─ Out-of-range values

IllegalStateException     → Invalid state (500)
  └─ Null response from agent

RuntimeException          → Execution failures (500)
  ├─ LLM provider errors
  ├─ Network failures
  └─ Unexpected errors
```

### Logging Levels

| Level | When to Use | Include Stack Trace? |
|-------|-------------|---------------------|
| `DEBUG` | Routine operations, method entry | No |
| `INFO` | Successful operations, metrics | No |
| `WARN` | Validation failures (400 errors) | No |
| `ERROR` | System failures (500 errors) | **Yes** |

---

## Error Categories

### 1. Validation Errors (Client Errors - 4xx)

**Characteristics**:
- Invalid input from client
- Predictable, recoverable
- No stack trace needed (not a bug)

**Examples**:
- Null or blank parameters
- Unknown role name
- Out-of-range values (temperature, tokens)

**Handling**:
- Throw `IllegalArgumentException`
- Log at `WARN` level
- Return HTTP 400 Bad Request or 404 Not Found

### 2. Execution Errors (Server Errors - 5xx)

**Characteristics**:
- External system failures
- Unpredictable, may be transient
- Requires investigation

**Examples**:
- LLM provider timeout
- Network connection failure
- Null response from external service

**Handling**:
- Catch in service layer
- Log at `ERROR` level with full stack trace
- Return HTTP 500 Internal Server Error OR `TaskResult.failure()`

### 3. State Errors (Server Errors - 5xx)

**Characteristics**:
- Invalid application state
- Indicates programming error
- Should not happen in production

**Examples**:
- Agent returns null response
- Missing required configuration

**Handling**:
- Throw `IllegalStateException`
- Log at `ERROR` level with stack trace
- Return HTTP 500 Internal Server Error

---

## Layer-by-Layer Strategy

### 1. Model Layer: Constructor Validation

**Responsibility**: Validate invariants at object creation time

**Pattern**: Fail-fast validation in record constructors

**Example** (`TaskRequest.java:19-24`):
```java
public TaskRequest {
    if (roleName == null || roleName.isBlank()) {
        throw new IllegalArgumentException("Role name cannot be null or blank");
    }
    if (task == null || task.isBlank()) {
        throw new IllegalArgumentException("Task cannot be null or blank");
    }
}
```

**Example** (`AgentConfig.java:22-30`):
```java
public AgentConfig {
    if (role == null || role.isBlank()) {
        throw new IllegalArgumentException("Role cannot be null or blank");
    }
    if (temperature < 0.0 || temperature > 1.0) {
        throw new IllegalArgumentException("Temperature must be between 0.0 and 1.0");
    }
    if (maxTokens <= 0) {
        throw new IllegalArgumentException("Max tokens must be positive");
    }
}
```

**Guidelines**:
- ✅ Validate all constructor parameters
- ✅ Use `IllegalArgumentException` for invalid inputs
- ✅ Provide specific error messages
- ✅ Check nulls, blanks, ranges, and business rules
- ❌ Don't catch exceptions in constructors
- ❌ Don't log in model classes

---

### 2. Service Layer: Business Logic

**Responsibility**: Validate business rules, handle execution errors

**Pattern**: Propagate validation errors, catch execution errors

#### Strategy A: Propagate Validation Errors

**When**: Client provides invalid input (unknown role, invalid parameters)

**Example** (`RoleManager.java:135-140`):
```java
public TaskResult executeTask(TaskRequest request) {
    try {
        AgentRole agent = agentRegistry.getAgent(request.roleName());
        // ...
    } catch (IllegalArgumentException e) {
        log.warn("Invalid role requested: {}", e.getMessage());
        throw e;  // Re-throw so controller returns 400
    }
}
```

**Example** (`AgentRegistry.java:40-47`):
```java
public AgentRole getAgent(String roleName) {
    AgentRole agent = agents.get(roleName);
    if (agent == null) {
        throw new IllegalArgumentException(
            "Unknown role: " + roleName + ". Available roles: " + getAvailableRoles()
        );
    }
    return agent;
}
```

**Guidelines**:
- ✅ Re-throw `IllegalArgumentException` to controller
- ✅ Log at `WARN` level before re-throwing
- ✅ Provide helpful context in error messages
- ✅ Include available alternatives when possible

#### Strategy B: Convert Execution Errors to Results

**When**: External system fails (LLM provider, network)

**Example** (`RoleManager.java:155-165`):
```java
public TaskResult executeTask(TaskRequest request) {
    try {
        // Execute task
    } catch (IllegalArgumentException e) {
        throw e;  // Propagate validation errors
    } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("Task execution failed for role {}: {}",
                  request.roleName(), e.getMessage(), e);

        return TaskResult.failure(
            request.roleName(),
            request.task(),
            e.getMessage()
        );
    }
}
```

**Example** (`BaseAgent.java:30-40`):
```java
public LLMResponse executeTask(TaskRequest request) {
    try {
        String prompt = buildPrompt(request.task(), request.context());
        return llmProvider.generate(prompt, config.temperature(), config.maxTokens());
    } catch (Exception e) {
        log.error("Error executing task for role {}: {}",
                  config.role(), e.getMessage(), e);
        throw new RuntimeException("Failed to execute task: " + e.getMessage(), e);
    }
}
```

**Guidelines**:
- ✅ Catch `Exception` for unexpected errors
- ✅ Log at `ERROR` level with full stack trace
- ✅ Return `TaskResult.failure()` for graceful degradation
- ✅ Include duration/context in logs
- ✅ Preserve original exception as cause

---

### 3. Controller Layer: HTTP Response Mapping

**Responsibility**: Map exceptions to HTTP status codes

**Pattern**: Try-catch with specific status codes for each exception type

**Example** (`RoleController.java:30-40`):
```java
@GetMapping("/{roleName}")
public ResponseEntity<RoleInfo> describeRole(@PathVariable String roleName) {
    log.debug("API request to describe role: {}", roleName);

    try {
        RoleInfo roleInfo = roleManager.describeRole(roleName);
        return ResponseEntity.ok(roleInfo);
    } catch (IllegalArgumentException e) {
        log.warn("Role not found: {}", roleName);
        return ResponseEntity.notFound().build();
    }
}
```

**Example** (`TaskController.java:35-50`):
```java
@PostMapping("/execute")
public ResponseEntity<TaskResult> executeTask(@RequestBody TaskRequest request) {
    log.info("API request to execute task for role: {}", request.roleName());

    try {
        TaskResult result = roleManager.executeTask(request);
        return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
        log.warn("Invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    } catch (Exception e) {
        log.error("Error executing task: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().build();
    }
}
```

**Exception → Status Code Mapping**:

| Exception Type | HTTP Status | When to Use |
|---------------|-------------|-------------|
| `IllegalArgumentException` | `400 Bad Request` | Invalid task parameters |
| `IllegalArgumentException` | `404 Not Found` | Unknown role (GET endpoints) |
| `Exception` (catch-all) | `500 Internal Server Error` | Unexpected errors |

**Guidelines**:
- ✅ Use try-catch in each controller method
- ✅ Log before returning error response
- ✅ Return empty body for errors (`.build()`)
- ✅ Catch specific exceptions first, then `Exception`
- ❌ Don't use `@ControllerAdvice` (keep it simple)
- ❌ Don't create custom error response DTOs (YAGNI)

---

## Exception Type Guidelines

### IllegalArgumentException

**When to use**:
- Null or blank parameters
- Unknown resource identifiers (role names)
- Out-of-range numeric values
- Invalid enum values
- Business rule violations

**Where to throw**:
- Model constructors
- Service method entry points
- Registry lookups

**Example error messages**:
```java
"Role name cannot be null or blank"
"Unknown role: XYZ. Available roles: [Developer, Tester, DevOps]"
"Temperature must be between 0.0 and 1.0"
```

### IllegalStateException

**When to use**:
- Agent returns null response
- Required configuration missing
- Invalid application state

**Where to throw**:
- Service layer when detecting invalid state
- After operations that should always succeed

**Example error messages**:
```java
"Agent returned null response"
"No LLM provider configured"
```

### RuntimeException

**When to use**:
- Wrapping checked exceptions
- External system failures (LLM, network)
- Unexpected errors during execution

**Where to throw**:
- BaseAgent (wraps LLM provider errors)
- Service layer (multi-agent failures)

**Example error messages**:
```java
"Failed to execute task: Connection timeout"
"Multi-agent task failed: LLM provider unavailable"
```

### Exception (catch-all)

**When to catch**:
- Controller layer as final safety net
- Prevents unhandled exceptions from reaching client

**Example**:
```java
catch (Exception e) {
    log.error("Unexpected error: {}", e.getMessage(), e);
    return ResponseEntity.internalServerError().build();
}
```

---

## Logging Standards

### Log Level Guidelines

#### DEBUG - Routine Operations
**When**: Method entry, normal flow, internal state
```java
log.debug("API request to list all roles");
log.debug("Built prompt for role {}: {} characters", config.role(), prompt.length());
log.debug("Agent {} processing task: {}", roleName, task);
```

#### INFO - Successful Operations
**When**: Business operations complete, important milestones
```java
log.info("Task completed successfully for role {} in {}ms", roleName, duration);
log.info("Registered agent: {}", roleName);
log.info("Multi-agent task completed: {} roles executed", count);
```

#### WARN - Validation Failures
**When**: Client errors (400, 404), expected failures
```java
log.warn("Role not found: {}", roleName);
log.warn("Invalid request: {}", e.getMessage());
log.warn("No agents registered! Check your configuration.");
```

**Note**: No stack trace needed (not a bug)

#### ERROR - System Failures
**When**: Unexpected errors, external system failures, 500 errors
```java
log.error("Task execution failed for role {}: {}", roleName, e.getMessage(), e);
log.error("Failed to create agent for role {}: {}", config.role(), e.getMessage(), e);
log.error("Multi-agent task failed: {}", e.getMessage(), e);
```

**Note**: Always include stack trace (`, e` parameter)

### What to Include in Error Logs

**Context Information**:
- Role name
- Task description (if relevant)
- Duration (for performance analysis)
- Input parameters that caused error

**Example**:
```java
log.error("Task execution failed for role {}: {}",
          request.roleName(), e.getMessage(), e);
```

**Full Stack Trace**:
- Always include for ERROR level
- Helps with debugging production issues

**Example**:
```java
log.error("Error executing task: {}", e.getMessage(), e);  // Note the ", e"
```

### Logging Configuration

**File**: `src/main/resources/application.yml`

```yaml
logging:
  level:
    root: INFO
    dev.adeengineer.platform: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

**Guidelines**:
- `DEBUG` for application packages during development
- `INFO` for root logger in production
- Simple console pattern for readability
- Consider JSON format for production log aggregation

---

## Test Patterns

### E2E Tests - HTTP Status Code Validation

**Pattern**: Assert status code only (no error body validation)

**Example** (`RoleDiscoveryE2ETest.java:45-55`):
```java
@Test
@DisplayName("Should return 404 for unknown role")
void shouldReturn404ForUnknownRole() {
    // Act
    ResponseEntity<RoleInfo> response = restTemplate.getForEntity(
        apiUrl("/roles/NonExistentRole"),
        RoleInfo.class
    );

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
}
```

**Example** (`DeveloperAgentE2ETest.java:85-100`):
```java
@Test
@DisplayName("Should handle invalid role gracefully")
void shouldHandleInvalidRoleGracefully() {
    // Arrange
    TaskRequest request = new TaskRequest(
        "NonExistentRole",
        "Some task",
        Map.of()
    );

    // Act
    ResponseEntity<TaskResult> response = restTemplate.postForEntity(
        apiUrl("/tasks/execute"),
        request,
        TaskResult.class
    );

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
```

### Unit Tests - Exception Validation

#### Pattern 1: Exception Type and Message

**Example** (`AgentRegistryTest.java:55-65`):
```java
@Test
@DisplayName("Should throw exception when retrieving non-existent agent")
void shouldThrowExceptionWhenAgentNotFound() {
    // When/Then
    assertThatThrownBy(() -> registry.getAgent("NonExistent"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown role: NonExistent")
        .hasMessageContaining("Available roles:");
}
```

**Guidelines**:
- ✅ Use `assertThatThrownBy()` for exception testing
- ✅ Validate exception type with `.isInstanceOf()`
- ✅ Validate error message with `.hasMessageContaining()`
- ✅ Check for context in error messages

#### Pattern 2: Controller Status Code Mapping

**Example** (`TaskControllerTest.java:75-90`):
```java
@Test
@DisplayName("POST /api/tasks/execute should return 400 on IllegalArgumentException")
void shouldReturn400OnInvalidRequest() throws Exception {
    // Given
    TaskRequest request = TestData.validTaskRequest();
    when(roleManager.executeTask(any(TaskRequest.class)))
        .thenThrow(new IllegalArgumentException("Unknown role"));

    // When/Then
    mockMvc.perform(post("/api/tasks/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
@DisplayName("POST /api/tasks/execute should return 500 on unexpected exception")
void shouldReturn500OnUnexpectedException() throws Exception {
    // Given
    TaskRequest request = TestData.validTaskRequest();
    when(roleManager.executeTask(any(TaskRequest.class)))
        .thenThrow(new RuntimeException("Unexpected error"));

    // When/Then
    mockMvc.perform(post("/api/tasks/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError());
}
```

**Guidelines**:
- ✅ Test each exception → status code mapping
- ✅ Mock service layer to throw exceptions
- ✅ Use `.andExpect(status().isBadRequest())` etc.

#### Pattern 3: Service Layer Error Handling

**Example** (`RoleManagerTest.java:95-115`):
```java
@Test
@DisplayName("Should throw exception when executing task with unknown role")
void shouldThrowExceptionWhenExecutingTaskWithUnknownRole() {
    // Given
    TaskRequest request = TestData.validTaskRequest();
    when(agentRegistry.getAgent(anyString()))
        .thenThrow(new IllegalArgumentException("Unknown role"));

    // When/Then
    assertThatThrownBy(() -> roleManager.executeTask(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown role");
}

@Test
@DisplayName("Should return failure result when agent throws runtime exception")
void shouldReturnFailureResultWhenAgentThrowsRuntimeException() {
    // Given
    TaskRequest request = TestData.validTaskRequest();
    MockAgent mockAgent = new MockAgent("Developer");
    mockAgent.setExceptionToThrow(new RuntimeException("LLM connection failed"));

    when(agentRegistry.getAgent("Developer")).thenReturn(mockAgent);

    // When
    TaskResult result = roleManager.executeTask(request);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).contains("LLM connection failed");
    assertThat(result.output()).isNull();
    assertThat(result.usage()).isNull();
}
```

**Guidelines**:
- ✅ Test propagation of validation errors (re-throw)
- ✅ Test conversion of execution errors to `TaskResult.failure()`
- ✅ Validate `success()`, `errorMessage()`, `output()`, `usage()` fields

---

## Design Decisions

### Decision 1: No @ControllerAdvice

**Rationale**:
- Application is simple with few endpoints
- Direct try-catch is easier to understand
- No need for global exception handling
- Each controller method has specific error handling needs

**Trade-offs**:
- ✅ Simple, predictable
- ✅ Easy to debug
- ✅ No hidden magic
- ❌ Some code duplication
- ❌ Not DRY across controllers

**When to reconsider**: If we add 10+ endpoints with identical error handling

### Decision 2: Empty Error Response Bodies

**Rationale**:
- Logs contain full error details
- Client can infer error from status code
- Don't leak implementation details to clients
- Simpler implementation (no error DTOs)

**Trade-offs**:
- ✅ Simple implementation
- ✅ Secure (no stack traces to client)
- ✅ Fast (no serialization overhead)
- ❌ Less helpful for API consumers
- ❌ Client can't display specific error messages

**When to reconsider**: If API consumers need structured error responses

### Decision 3: No Custom Exception Classes

**Rationale**:
- Standard Java exceptions are sufficient
- `IllegalArgumentException` clearly signals validation errors
- `RuntimeException` clearly signals execution errors
- No need for domain-specific exception hierarchy

**Trade-offs**:
- ✅ Simple, no extra classes
- ✅ Standard Java conventions
- ✅ Easy to understand
- ❌ Can't add custom fields to exceptions
- ❌ Can't distinguish business vs. technical errors programmatically

**When to reconsider**: If we need custom exception fields (e.g., error codes)

### Decision 4: TaskResult for Graceful Degradation

**Rationale**:
- Task execution failures should not crash the API
- Client can handle success/failure cases
- Allows partial results in multi-agent scenarios
- Better than throwing exceptions for expected failures

**Trade-offs**:
- ✅ Graceful degradation
- ✅ Client has control over error handling
- ✅ Enables partial results
- ❌ Client must check `success()` field
- ❌ Two ways to signal errors (exceptions + TaskResult)

**When to reconsider**: Never. This is a good pattern for task execution.

---

## Examples

### Example 1: Validation Error Flow

**Scenario**: Client sends task with unknown role

**Flow**:
```
1. Client → POST /api/tasks/execute with roleName="NonExistent"

2. TaskController.executeTask()
   ├─ Calls roleManager.executeTask(request)

3. RoleManager.executeTask()
   ├─ Calls agentRegistry.getAgent("NonExistent")

4. AgentRegistry.getAgent()
   ├─ Role not found in registry
   └─ Throws: IllegalArgumentException("Unknown role: NonExistent. Available roles: [Developer, Tester]")

5. RoleManager.executeTask()
   ├─ Catches IllegalArgumentException
   ├─ Logs: WARN "Invalid role requested: Unknown role: NonExistent..."
   └─ Re-throws IllegalArgumentException

6. TaskController.executeTask()
   ├─ Catches IllegalArgumentException
   ├─ Logs: WARN "Invalid request: Unknown role: NonExistent..."
   └─ Returns: ResponseEntity.badRequest().build()

7. Client ← 400 Bad Request (empty body)
```

**Logs**:
```
[WARN] Invalid role requested: Unknown role: NonExistent. Available roles: [Developer, Tester]
[WARN] Invalid request: Unknown role: NonExistent. Available roles: [Developer, Tester]
```

---

### Example 2: Execution Error Flow

**Scenario**: LLM provider times out during task execution

**Flow**:
```
1. Client → POST /api/tasks/execute with roleName="Developer"

2. TaskController.executeTask()
   ├─ Calls roleManager.executeTask(request)

3. RoleManager.executeTask()
   ├─ Calls agentRegistry.getAgent("Developer") → Success
   ├─ Calls agent.executeTask(request)

4. BaseAgent.executeTask()
   ├─ Calls llmProvider.generate(...)
   └─ LLM provider throws: SocketTimeoutException("Read timed out")

5. BaseAgent.executeTask()
   ├─ Catches Exception
   ├─ Logs: ERROR "Error executing task for role Developer: Read timed out" + stack trace
   └─ Throws: RuntimeException("Failed to execute task: Read timed out", e)

6. RoleManager.executeTask()
   ├─ Catches Exception (not IllegalArgumentException)
   ├─ Logs: ERROR "Task execution failed for role Developer: Failed to execute..." + stack trace
   └─ Returns: TaskResult.failure("Developer", "task", "Failed to execute task: Read timed out")

7. TaskController.executeTask()
   └─ Returns: ResponseEntity.ok(failureResult)

8. Client ← 200 OK with TaskResult{success=false, errorMessage="Failed to execute..."}
```

**Logs**:
```
[ERROR] Error executing task for role Developer: Read timed out
java.net.SocketTimeoutException: Read timed out
    at java.net.SocketInputStream.socketRead0(Native Method)
    ...

[ERROR] Task execution failed for role Developer: Failed to execute task: Read timed out
java.lang.RuntimeException: Failed to execute task: Read timed out
    at dev.adeengineer.platform.core.BaseAgent.executeTask(BaseAgent.java:38)
    ...
```

**Note**: Returns 200 with failure result (not 500) because the error is handled gracefully.

---

### Example 3: Multi-Agent Validation Error

**Scenario**: Client requests multi-agent task with one unknown role

**Flow**:
```
1. Client → POST /api/tasks/multi-agent with roleNames=["Developer", "NonExistent"]

2. TaskController.executeMultiAgentTask()
   ├─ Calls roleManager.executeMultiAgentTask(...)

3. RoleManager.executeMultiAgentTask()
   ├─ Validates all roles exist (loop)
   ├─ Calls agentRegistry.hasAgent("Developer") → true
   ├─ Calls agentRegistry.hasAgent("NonExistent") → false
   └─ Throws: IllegalArgumentException("Unknown role: NonExistent")

4. TaskController.executeMultiAgentTask()
   ├─ Catches IllegalArgumentException
   ├─ Logs: WARN "Invalid request: Unknown role: NonExistent"
   └─ Returns: ResponseEntity.badRequest().build()

5. Client ← 400 Bad Request (empty body)
```

**Logs**:
```
[WARN] Invalid multi-agent request: Unknown role: NonExistent
[WARN] Invalid request: Unknown role: NonExistent
```

---

## References

### Official Documentation

1. **Spring Framework - Exception Handling**
   - https://spring.io/guides/tutorials/rest
   - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html

2. **Spring Boot - Error Handling**
   - https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc
   - https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.error-handling

3. **HTTP Status Codes (RFC 9110)**
   - https://www.rfc-editor.org/rfc/rfc9110.html#name-status-codes
   - Section 15: Status Codes

4. **Effective Java (3rd Edition)**
   - Item 72: Favor the use of standard exceptions
   - Item 73: Throw exceptions appropriate to the abstraction
   - Item 75: Include failure-capture information in detail messages

### Internal Documentation

- **Test Execution Guide**: `doc/4-development/guide/test-execution-guide.md`
- **Spring Profiles Guide**: `doc/4-development/guide/spring-profiles-guide.md`
- **Developer Guide**: `doc/4-development/developer-guide.md`

### Best Practices Articles

1. **Error Handling Best Practices in REST APIs**
   - https://www.baeldung.com/exception-handling-for-rest-with-spring
   - https://www.rfc-editor.org/rfc/rfc7807 (Problem Details for HTTP APIs)

2. **Java Exception Handling Best Practices**
   - https://www.oracle.com/java/technologies/javase/codeconventions-statements.html

---

**Last Updated**: 2025-10-18
**Version**: 1.0.0
**Status**: Active
