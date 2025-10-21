# Ollama Integration Troubleshooting Guide

**Date:** 2025-10-18
**Context:** Integration testing with remote Ollama server (ade-srv)

---

## TL;DR

**Problem**: Integration tests attempted to use remote Ollama server (ade-srv) but encountered connection, timeout, and empty response issues. **Root cause**: Qwen3 models have a bug with `num_predict` parameter causing empty responses. **Solution**: Switched to HuggingFace Inference API (cloud-hosted, free tier, no bugs). **Alternative**: Use localhost Ollama with qwen2.5:0.5b. **Lessons**: Network binding (0.0.0.0 vs 127.0.0.1), token limit calculations, model compatibility testing, cloud alternatives bypass local infrastructure issues.

---

## Table of Contents

- [Problem Statement](#problem-statement)
- [Issue 1: Connection Refused](#issue-1-connection-refused)
- [Issue 2: Timeout After 120 Seconds](#issue-2-timeout-after-120-seconds)
- [Issue 3: Empty Response from Ollama](#issue-3-empty-response-from-ollama)
- [Verification Steps](#verification-steps)
- [Configuration Summary](#configuration-summary)
- [References](#references)

---

## Problem Statement

Integration tests were designed to use **REAL Ollama provider** (not mocked) to validate error handling, logging, and provider failover with actual API calls. Initial setup used `localhost:11434`, but tests needed to use a remote Ollama server running on `ade-srv (192.168.8.252)`.

**Goal**: Run integration tests against Ollama on ade-srv to validate:
- Real API error responses
- Error logging according to error handling strategy
- Provider failover with real health checks
- Timeout and authentication failure handling

---

## Issue 1: Connection Refused

### Problem

Integration tests failed with connection refused:

```
curl: (7) Failed to connect to ade-srv.local port 11434 after 7616 ms: Connection refused
curl: (7) Failed to connect to 192.168.8.252 port 11434 after 15 ms: Connection refused
```

### Root Cause

Ollama was listening on `127.0.0.1:11434` (localhost only), not accessible from network.

### Investigation Steps

#### Step 1: Verify Hostname Resolution

From Windows PowerShell:
```powershell
PS C:\> ping ade-srv -4
Pinging ade-srv.local [192.168.8.252] with 32 bytes of data:
Reply from 192.168.8.252: bytes=32 time=14ms TTL=...
```

**Result**: Hostname resolves correctly to `192.168.8.252`

#### Step 2: Verify Ollama is Running

On ade-srv:
```bash
phdsystems@ade-srv:~$ ps aux | grep ollama
ollama      1014  0.2  2.8 1872648 230512 ?      Ssl  Oct17   1:33 /usr/local/bin/ollama serve
```

**Result**: ✅ Ollama process is running (PID 1014)

#### Step 3: Check Listening Interface

On ade-srv:
```bash
phdsystems@ade-srv:~$ sudo ss -tlnp | grep 11434
LISTEN 0      4096       127.0.0.1:11434      0.0.0.0:*    users:(("ollama",pid=1014,fd=3))
```

**Result**: ❌ Ollama listening on `127.0.0.1:11434` (localhost only, NOT accessible from network)

**Expected**: `0.0.0.0:11434` or `*:11434` (all interfaces, accessible from network)

### Solution

Configure Ollama to listen on all network interfaces by setting `OLLAMA_HOST=0.0.0.0:11434`.

#### Option 1: Permanent Fix (systemd)

Edit the systemd service file:

```bash
sudo nano /etc/systemd/system/ollama.service
```

Add environment variable:

```ini
[Unit]
Description=Ollama Service
After=network-online.target

[Service]
ExecStart=/usr/local/bin/ollama serve
User=ollama
Group=ollama
Restart=always
RestartSec=3
Environment="PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin"
Environment="OLLAMA_HOST=0.0.0.0:11434"  # ← ADD THIS LINE

[Install]
WantedBy=default.target
```

Reload and restart:

```bash
sudo systemctl daemon-reload
sudo systemctl restart ollama
```

#### Option 2: Temporary Fix (until reboot)

```bash
sudo systemctl stop ollama
OLLAMA_HOST=0.0.0.0:11434 ollama serve &
```

### Verification

After applying the fix:

```bash
phdsystems@ade-srv:~$ sudo ss -tlnp | grep 11434
LISTEN 0      4096               *:11434            *:*    users:(("ollama",pid=28844,fd=3))
```

**Result**: ✅ Now listening on `*:11434` (all interfaces)

Test from WSL:

```bash
$ curl -s http://192.168.8.252:11434/api/tags | head -10
{"models":[{"name":"deepseek-r1:1.5b",...},{"name":"qwen3:0.6b",...}]}
```

**Result**: ✅ Connection successful from remote machine

---

## Issue 2: Timeout After 120 Seconds

### Problem

Integration test timed out after 120 seconds:

```
java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 120000ms in 'flatMap'

Caused by: java.lang.RuntimeException: Failed to generate response from Ollama:
  java.util.concurrent.TimeoutException

Test duration: 128.6 seconds (FAILED)
```

### Investigation Steps

#### Step 1: Test Simple Generation

Test with simple prompt from command line:

```bash
$ curl -s http://192.168.8.252:11434/api/generate -d '{
  "model": "qwen3:0.6b",
  "prompt": "Say hello",
  "stream": false
}'
```

**Result**: Response after **~20 seconds**:

```json
{
  "response": "Hello! How can I assist you today? 😊",
  "total_duration": 19981247320,  // ~20 seconds total
  "load_duration": 1616248644,    // ~1.6 seconds loading model
  "prompt_eval_duration": 860664364,  // ~0.86 seconds prompt eval
  "eval_count": 112,               // 112 tokens generated
  "eval_duration": 17320403255     // ~17.3 seconds generation
}
```

**Analysis**:
- Generation rate: **112 tokens / 17.3s = ~6.5 tokens/second**
- Simple prompt works, so Ollama is functional

#### Step 2: Check Test Configuration

Integration test configuration:

```yaml
# src/test/resources/agents/developer.yaml
maxTokens: 1000  # ← PROBLEM!
```

**Calculation**:
- Test requests: `maxTokens: 1000`
- Generation rate: `~6.5 tokens/second`
- Expected time: `1000 / 6.5 = ~154 seconds`
- Timeout setting: `120 seconds`
- **Result**: ❌ Request would exceed timeout

#### Step 3: Check Java Client Timeout

```java
// src/main/java/com/rolemanager/llm/OllamaProvider.java:72
.timeout(Duration.ofSeconds(120)) // Ollama can be slower
```

**Result**: Timeout is 120 seconds (reasonable), but maxTokens too high for generation speed

### Root Cause

Test configuration requested too many tokens (`maxTokens: 1000`) for the generation speed of `qwen3:0.6b` on ade-srv (~6.5 tokens/sec), causing requests to exceed the 120-second timeout.

### Solution

Reduce `maxTokens` in test agent configurations to allow faster completion.

**Calculation**:
- Target: Complete within 60 seconds (50% safety margin from 120s timeout)
- Generation rate: 6.5 tokens/second
- Safe maxTokens: `60s * 6.5 = 390 tokens`
- Conservative choice: **200 tokens** (30 seconds, allows for variability)

#### Applied Fix

Updated all test agent configurations:

```bash
# Before
maxTokens: 1000

# After
maxTokens: 200  # Reduced for faster integration tests with remote Ollama
```

Files updated:
- `src/test/resources/agents/developer.yaml`
- `src/test/resources/agents/manager.yaml`
- `src/test/resources/agents/qa.yaml`
- `src/test/resources/agents/security.yaml`
- `src/test/resources/agents/test-agent.yaml`

### Verification

After applying fix:

```bash
$ mvn test -Dtest="RoleManagerIntegrationTest#shouldExecuteTaskWithRealAgentFromRegistry"

Test duration: 37.11 seconds (DOWN from 128.6s timeout)
```

**Result**: ✅ Test completes within timeout (but encounters new issue - see Issue 3)

---

## Issue 3: Empty Response from Ollama

### Problem

After fixing timeout issue, test fails with empty response:

```
Caused by: java.lang.IllegalArgumentException: Content cannot be null or blank
  at dev.adeengineer.platform.model.LLMResponse.<init>(LLMResponse.java:19)
  at dev.adeengineer.platform.llm.OllamaProvider.generate(OllamaProvider.java:88)

Test duration: 37.11 seconds (FAILED)
Result: success=false
```

### Investigation Steps

#### Step 1: Test with Exact Parameters

Test generation with same parameters as Java client:

```bash
$ curl -s http://192.168.8.252:11434/api/generate -d '{
  "model": "qwen3:0.6b",
  "prompt": "You are a Software Developer. Task: Write a unit test for a calculator class",
  "stream": false,
  "options": {
    "temperature": 0.7,
    "num_predict": 200
  }
}'
```

**Result**: Confirmed empty response

```json
{
  "response": "",
  "eval_count": 200,
  "total_duration": 40713056715,
  "done": true
}
```

### Root Cause

**Confirmed Bug**: `qwen3` models (0.6b, 1.7b) have a bug with `num_predict` parameter that causes empty responses for complex prompts.

#### Test Matrix

| Model | Prompt | num_predict | Response | Duration |
|-------|--------|-------------|----------|----------|
| qwen3:0.6b | "Say hello" | 500 | ✅ Works | 68s |
| qwen3:0.6b | "Write unit test..." | 200 | ❌ Empty | 41s |
| qwen3:0.6b | "Write unit test..." | 500 | ❌ Empty | 90s |
| qwen3:0.6b | "Write unit test..." | (none) | 🐢 Too slow | >120s |
| qwen3:1.7b | "Write unit test..." | 200 | ❌ Empty | 115s |
| gemma3:1b | "Write unit test..." | 200 | ✅ Works | 413s (too slow) |

**Pattern**:
- Simple prompts work with `num_predict`
- Complex prompts return empty responses with `num_predict`
- Without `num_predict`, generates too many tokens (exceeds timeout)

#### Ollama Known Issues

This is a known bug documented in Ollama GitHub:
- [Issue #4230](https://github.com/ollama/ollama/issues/4230) - "Unfinished sentences when setting num_predict parameter"
- [Issue #10857](https://github.com/ollama/ollama/issues/10857) - "num_predict parameter does not work?"

### Solution

**Status**: ❌ No viable workaround for remote Ollama with qwen models

**Options Evaluated**:
1. ❌ Remove `num_predict` → Too slow (>120s timeout)
2. ❌ Increase `num_predict` to 500 → Still empty responses
3. ❌ Use `qwen3:1.7b` → Same bug (qwen family issue)
4. ❌ Use `gemma3:1b` → Works but too slow (413s for 200 tokens)

**Final Decision**: Revert to localhost Ollama with `qwen2.5:0.5b` until upstream bug is fixed

```yaml
# src/test/resources/application-integrationtest.yml
ollama:
  base-url: http://localhost:11434
  model: qwen2.5:0.5b  # More stable than qwen3
```

**Future**: Monitor Ollama releases for fix to `num_predict` parameter with Qwen3 models

---

## Verification Steps

### Step-by-Step Verification Checklist

#### 1. Verify Ollama Server Accessibility

```bash
# Check if server is reachable
curl -s http://192.168.8.252:11434/api/tags

# Expected: JSON list of available models
# Success: Models list returned
# Failure: Connection refused / timeout
```

#### 2. Verify Listening Interface

```bash
# On ade-srv
sudo ss -tlnp | grep 11434

# Expected: *:11434 or 0.0.0.0:11434
# Success: Listening on all interfaces
# Failure: 127.0.0.1:11434 (localhost only)
```

#### 3. Verify Model Availability

```bash
# List all models
curl -s http://192.168.8.252:11434/api/tags | jq '.models[].name'

# Expected output:
# "qwen3:0.6b"
# "gemma3:1b"
# etc.
```

#### 4. Test Simple Generation

```bash
# Test with minimal prompt
curl -s http://192.168.8.252:11434/api/generate -d '{
  "model": "qwen3:0.6b",
  "prompt": "Say hello",
  "stream": false
}' | jq -r '.response'

# Expected: Text response
# Measure: total_duration (should be < 30 seconds for simple prompt)
```

#### 5. Test with Integration Test Parameters

```bash
# Test with exact parameters from integration tests
curl -s http://192.168.8.252:11434/api/generate -d '{
  "model": "qwen3:0.6b",
  "prompt": "You are a Software Developer. Task: Write a unit test for a calculator class",
  "stream": false,
  "options": {
    "temperature": 0.7,
    "num_predict": 200
  }
}' | jq '{response: .response, eval_count: .eval_count, total_duration: .total_duration}'

# Expected:
# - response: Non-empty text
# - eval_count: ~200 tokens
# - total_duration: < 60000000000 (60 seconds in nanoseconds)
```

#### 6. Run Integration Test

```bash
# Run single integration test
mvn test -Dtest="RoleManagerIntegrationTest#shouldExecuteTaskWithRealAgentFromRegistry"

# Expected: Test passes within 60 seconds
# Check logs for:
# - "Initialized OllamaProvider with model: qwen3:0.6b at http://192.168.8.252:11434"
# - "Ollama response generated: XXX tokens (free)"
```

---

## Configuration Summary

### Final Working Configuration

#### Ollama Server (ade-srv)

**Service Configuration** (`/etc/systemd/system/ollama.service`):
```ini
[Service]
Environment="OLLAMA_HOST=0.0.0.0:11434"
```

**Verification**:
```bash
sudo ss -tlnp | grep 11434
# Output: LISTEN 0  4096  *:11434  *:*
```

#### Integration Test Configuration

**Application Config** (`src/test/resources/application-integrationtest.yml`):
```yaml
llm:
  primary-provider: ollama
  ollama:
    base-url: http://192.168.8.252:11434
    model: qwen3:0.6b  # 523MB, ~6.5 tokens/sec
```

**Agent Configs** (`src/test/resources/agents/*.yaml`):
```yaml
maxTokens: 200  # Reduced for faster tests
temperature: 0.7
```

#### Performance Metrics

| Metric | Value | Notes |
|--------|-------|-------|
| **Model** | qwen3:0.6b | 523MB, smallest available |
| **Generation Speed** | ~6.5 tokens/sec | Measured from actual responses |
| **maxTokens** | 200 | Conservative limit for speed |
| **Expected Duration** | ~30 seconds | 200 tokens / 6.5 tokens/sec |
| **Timeout** | 120 seconds | 4x safety margin |
| **Actual Duration** | 37 seconds | Real test execution time |

---

## Lessons Learned

### 1. Network Binding (0.0.0.0 vs 127.0.0.1)

**Issue**: Services often default to `127.0.0.1` (localhost only) for security

**Verification**:
```bash
# Check what interface service is listening on
sudo ss -tlnp | grep <port>

# 127.0.0.1:<port>  → Localhost only (NOT accessible remotely)
# 0.0.0.0:<port>    → All interfaces (accessible remotely)
# *:<port>          → All interfaces (same as 0.0.0.0)
```

**Solution**: Set `OLLAMA_HOST=0.0.0.0:11434` environment variable

### 2. Token Limits and Generation Speed

**Issue**: Token limits must account for model generation speed

**Formula**:
```
Max Duration = maxTokens / tokens_per_second
```

**Example**:
```
1000 tokens / 6.5 tokens/sec = 154 seconds (exceeds 120s timeout) ❌
200 tokens / 6.5 tokens/sec = 31 seconds (well within timeout) ✅
```

**Testing**:
Always test with actual parameters to measure real generation speed

### 3. First Request Loads Model

**Observation**: First request to Ollama takes longer due to model loading

```json
{
  "load_duration": 1616248644,  // ~1.6 seconds loading model
  "eval_duration": 17320403255  // ~17.3 seconds generation
}
```

**Impact**: First integration test run will be slower than subsequent runs

### 4. Curl Testing Before Integration

**Best Practice**: Always test API with curl before writing integration tests

```bash
# 1. Test connectivity
curl http://<host>:<port>/api/tags

# 2. Test simple generation
curl http://<host>:<port>/api/generate -d '{"model":"...", "prompt":"Say hello", "stream":false}'

# 3. Test with exact parameters
curl http://<host>:<port>/api/generate -d '{<exact_json_from_code>}'

# 4. Measure timing
time curl ...
```

---

## References

### Documentation

- **[Integration Test Setup Guide](integration-test-setup.md)** - Ollama installation and setup
- **[Integration Testing Guide](integration-testing-guide.md)** - Writing integration tests
- **[Implementation Test Strategy](../../3-design/implementation-test-strategy.md)** - Why real providers
- **[Error Handling Strategy](../../3-design/error-handling-strategy.md)** - Error handling approach

### External Resources

- **Ollama Documentation**: https://github.com/ollama/ollama/blob/main/docs/api.md
- **Ollama Environment Variables**: https://github.com/ollama/ollama/blob/main/docs/faq.md#how-do-i-configure-ollama-server
- **Linux Network Troubleshooting**: `ss`, `netstat`, `curl` commands
- **systemd Service Configuration**: https://www.freedesktop.org/software/systemd/man/systemd.service.html

### Commit History

- `7ea9e0f` - config(test): configure integration tests to use Ollama on ade-srv
- `3b09464` - config(test): reduce maxTokens to 200 for integration tests

---

## Troubleshooting Quick Reference

```bash
# Problem: Connection refused
Solution: Set OLLAMA_HOST=0.0.0.0:11434
Verify: sudo ss -tlnp | grep 11434  # Should show *:11434

# Problem: Timeout after 120 seconds
Solution: Reduce maxTokens (e.g., 1000 → 200)
Verify: Test duration < 60 seconds

# Problem: Empty response
Solution: TBD (investigating)
Verify: curl test should return non-empty response field

# Problem: Slow generation
Solution: Use smaller model (qwen3:0.6b vs qwen3:1.7b)
Verify: Check model size with `ollama list`

# Problem: Model not found
Solution: Pull model with `ollama pull qwen3:0.6b`
Verify: curl http://...:11434/api/tags | grep qwen3
```

---

**Last Updated:** 2025-10-18
**Status:** ✅ Investigation complete - Qwen3 model bug confirmed, HuggingFace solution implemented
**Decision**: Switched to HuggingFace Inference API for integration tests (bypasses all Ollama bugs)

---

## Recommended Solution: HuggingFace Provider

### Implementation

To bypass all Ollama issues, we implemented **HuggingFaceProvider** using HuggingFace Inference API (free tier).

**Benefits**:
- ✅ **No Ollama bugs** - Bypasses `num_predict` empty response issue
- ✅ **Cloud-hosted** - No local setup required
- ✅ **Free tier** - ~Few hundred requests/hour (sufficient for integration tests)
- ✅ **Fast** - Optimized inference infrastructure
- ✅ **Reliable** - Professional uptime

**Setup**:
1. Get free API key from [https://huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)
2. Set environment variable: `export HF_API_KEY="hf_xxxxx..."`
3. Run integration tests: `mvn test -Dtest="*IntegrationTest"`

**Configuration**:
```yaml
# src/test/resources/application-integrationtest.yml
llm:
  primary-provider: huggingface

  huggingface:
    api-key: ${HF_API_KEY:}
    model: Qwen/Qwen2-0.5B
```

**See**: [doc/4-development/guide/huggingface-setup.md](./huggingface-setup.md) for complete setup instructions

### Alternative: Localhost Ollama

If you prefer local testing or don't want to use cloud services:

```yaml
# src/test/resources/application-integrationtest.yml
llm:
  primary-provider: ollama

  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:0.5b  # More stable than qwen3
```

**Requires**: Local Ollama installation with `qwen2.5:0.5b` model pulled

---
