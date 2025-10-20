# Integration Test Setup Guide

## TL;DR

**Integration tests use REAL LLM providers** (Ollama locally) to validate error handling, logging, and failover behavior. **Prerequisites**: Install and run Ollama with `qwen2.5:0.5b` model. **Quick start**: `ollama pull qwen2.5:0.5b && ollama serve`.

---

## Overview

Unlike E2E tests which mock LLM providers, integration tests use **real providers** to validate:
- ✅ Actual error responses from LLM APIs
- ✅ Error logging according to error handling strategy
- ✅ Provider failover with real health checks
- ✅ Timeout and rate limit handling
- ✅ Authentication failures

## Prerequisites

### 1. Install Ollama

**Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**macOS:**
```bash
brew install ollama
```

**Windows:**
Download from https://ollama.com/download

### 2. Pull the Test Model

Integration tests use `qwen2.5:0.5b` (small, fast model):

```bash
ollama pull qwen2.5:0.5b
```

### 3. Start Ollama Server

```bash
ollama serve
```

Verify it's running:
```bash
curl http://localhost:11434/api/tags
```

Expected output: JSON list of available models

## Running Integration Tests

### With Ollama Running

```bash
# Run all integration tests
mvn test -Dtest="*IntegrationTest"

# Run specific test class
mvn test -Dtest="RoleManagerIntegrationTest"

# Run single test method
mvn test -Dtest="RoleManagerIntegrationTest#shouldExecuteTaskWithRealAgentFromRegistry"
```

### Without Ollama

Integration tests will fail if Ollama is not running. You'll see:

```
WARN  OllamaProvider - Ollama health check failed: Connection refused
WARN  LLMProviderFactory - Provider ollama is not healthy, trying next
ERROR AnthropicProvider - Error calling Anthropic API: 401 Unauthorized
```

This demonstrates the error handling and failover logic, but tests will fail since no valid provider is available.

## Configuration

Integration test configuration: `src/test/resources/application-integrationtest.yml`

```yaml
llm:
  primary-provider: ollama

  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:0.5b  # Small, fast model for testing

  # Fallback providers (invalid keys to test error handling)
  anthropic:
    api-key: invalid-key-for-testing
  openai:
    api-key: invalid-key-for-testing
```

### Logging

Integration tests run with DEBUG logging to validate error handling:

```yaml
logging:
  level:
    adeengineer.dev.platform: DEBUG
    adeengineer.dev.platform.llm: DEBUG
```

This lets you see:
- Provider health checks
- Failover decisions
- Error propagation through layers
- Request/response logging

## Test Structure

### BaseIntegrationTest

Uses **real Ollama provider** (no mocks):

```java
@SpringBootTest
@ActiveProfiles("integrationtest")
public abstract class BaseIntegrationTest {
    // No mocks - uses real Ollama from config
}
```

### BaseProviderFailoverTest

Special case for failover testing (uses mocks):

```java
@SpringBootTest
@ActiveProfiles("integrationtest")
public abstract class BaseProviderFailoverTest {
    @MockBean
    protected AnthropicProvider anthropicProvider;
    @MockBean
    protected OpenAIProvider openAIProvider;
    @MockBean
    protected OllamaProvider ollamaProvider;
}
```

Only `LLMProviderFailoverIntegrationTest` extends this to test failover logic.

## Validating Error Handling Strategy

Integration tests validate the error handling strategy from `doc/3-design/error-handling-strategy.md`:

### 1. Provider Layer Errors

```
ERROR AnthropicProvider - Error calling Anthropic API: 401 Unauthorized
```
✅ Validates: Provider catches HTTP errors, logs with context

### 2. Agent Layer Errors

```
ERROR BaseAgent - Error executing task for role Software Developer:
      Failed to generate response from Anthropic
```
✅ Validates: Agent wraps provider errors, adds role context

### 3. Service Layer Errors

```
ERROR RoleManager - Task execution failed for role Software Developer:
      Failed to execute task
```
✅ Validates: Service layer propagates errors, logs at boundaries

### 4. Failover Behavior

```
WARN  LLMProviderFactory - Provider ollama is not healthy, trying next
DEBUG LLMProviderFactory - Selected provider: anthropic
```
✅ Validates: Automatic failover based on health checks

## Troubleshooting

### Ollama Not Running

**Error:**
```
Connection refused: localhost/127.0.0.1:11434
```

**Solution:**
```bash
ollama serve
```

### Model Not Available

**Error:**
```
model 'qwen2.5:0.5b' not found
```

**Solution:**
```bash
ollama pull qwen2.5:0.5b
```

### Tests Timeout

**Error:**
```
Test timed out after 10 seconds
```

**Cause:** Large model taking too long to respond

**Solution:** Use smaller model (qwen2.5:0.5b is recommended)

### Port Already in Use

**Error:**
```
bind: address already in use
```

**Solution:**
```bash
# Find process using port 11434
lsof -i :11434

# Kill it
kill -9 <PID>

# Restart Ollama
ollama serve
```

## Performance Considerations

### Model Size vs Speed

| Model | Size | Speed | Quality |
|-------|------|-------|---------|
| qwen2.5:0.5b | 352 MB | ⚡ Very Fast (~100ms) | Basic |
| qwen2.5:1.5b | 1.1 GB | ⚡ Fast (~200ms) | Good |
| llama3.2:3b | 1.7 GB | 🐢 Slow (~500ms) | Better |

**Recommendation:** Use `qwen2.5:0.5b` for fast integration tests

### Test Execution Time

- Config loading tests: ~50ms (no LLM calls)
- Provider tests: ~100ms (1 LLM call)
- Multi-agent tests: ~500ms (4 parallel LLM calls)

Total integration test suite: ~2 minutes with Ollama

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Integration Tests

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Install Ollama
        run: curl -fsSL https://ollama.com/install.sh | sh

      - name: Start Ollama
        run: |
          ollama serve &
          sleep 5

      - name: Pull test model
        run: ollama pull qwen2.5:0.5b

      - name: Run integration tests
        run: mvn test -Dtest="*IntegrationTest"
```

### Skip Integration Tests

To run only unit tests (no Ollama required):

```bash
mvn test -Dtest="!*IntegrationTest,!*E2ETest"
```

## See Also

- **[Integration Testing Guide](integration-testing-guide.md)** - Writing integration tests
- **[Error Handling Strategy](../../3-design/error-handling-strategy.md)** - Error handling patterns
- **[Implementation Test Strategy](../../3-design/implementation-test-strategy.md)** - Overall testing strategy for implementation phase

---

**Last Updated:** 2025-10-18
**Maintainer:** Development Team
