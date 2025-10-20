# Workflow Diagrams - Role Manager App

**Document Type:** Design Artifact
**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Purpose**: Visual documentation of process flows and execution sequences in Role Manager App. **Key diagrams**: Single-agent execution (13 steps, 12-28s), multi-agent collaboration (parallel), error handling (exponential backoff), provider failover (circuit breaker), configuration loading. **Use this**: To understand timing, orchestration, sequencing, and error handling. **See also**: [Data Flow Diagrams](dataflow.md) for data transformations.

---

## Table of Contents

- [1. Single-Agent Task Execution Sequence](#1-single-agent-task-execution-sequence)
- [2. Multi-Agent Collaboration Workflow](#2-multi-agent-collaboration-workflow)
- [3. Error Handling and Retry Logic](#3-error-handling-and-retry-logic)
- [4. LLM Provider Failover Flow](#4-llm-provider-failover-flow)
- [5. Configuration Loading Workflow](#5-configuration-loading-workflow)
- [6. Performance Characteristics](#6-performance-characteristics)
- [References](#references)

---

## 1. Single-Agent Task Execution Sequence

### Sequence Diagram

```
┌──────┐         ┌─────────────┐      ┌────────────────┐      ┌─────────┐      ┌─────────────┐      ┌─────────────┐
│ User │         │ REST API /  │      │  RoleManager   │      │  Agent  │      │ LLMProvider │      │  External   │
│      │         │     CLI     │      │   (Service)    │      │ Registry│      │  (Factory)  │      │  LLM API    │
└──┬───┘         └──────┬──────┘      └───────┬────────┘      └────┬────┘      └──────┬──────┘      └──────┬──────┘
   │                    │                     │                     │                  │                    │
   │ 1. POST /api/v1/execute                 │                     │                  │                    │
   │ {role: "Developer", task: "Review PR"}  │                     │                  │                    │
   ├───────────────────>│                     │                     │                  │                    │
   │                    │                     │                     │                  │                    │
   │                    │ 2. executeTask(role, task, context)       │                  │                    │
   │                    ├────────────────────>│                     │                  │                    │
   │                    │                     │                     │                  │                    │
   │                    │                     │ 3. getAgent("Developer")              │                    │
   │                    │                     ├────────────────────>│                  │                    │
   │                    │                     │                     │                  │                    │
   │                    │                     │ 4. return DeveloperAgent              │                    │
   │                    │                     │<────────────────────┤                  │                    │
   │                    │                     │                     │                  │                    │
   │                    │                     │ 5. agent.executeTask(task, context)   │                    │
   │                    │                     ├───────────────────────────────────────>│                    │
   │                    │                     │                     │                  │                    │
   │                    │                     │                     │  6. getPromptTemplate()              │
   │                    │                     │                     │<─────────────────┤                    │
   │                    │                     │                     │                  │                    │
   │                    │                     │                     │  7. buildPrompt(task, context)       │
   │                    │                     │                     │  (Inject role-specific instructions) │
   │                    │                     │                     │                  │                    │
   │                    │                     │                     │  8. llm.createCompletion(prompt)     │
   │                    │                     │                     │  ────────────────┼───────────────────>│
   │                    │                     │                     │                  │                    │
   │                    │                     │                     │                  │  9. LLM Processing │
   │                    │                     │                     │                  │     (10-25 seconds)│
   │                    │                     │                     │                  │<───────────────────┤
   │                    │                     │                     │                  │                    │
   │                    │                     │                     │  10. LLMResponse │                    │
   │                    │                     │                     │  <───────────────┤                    │
   │                    │                     │                     │                  │                    │
   │                    │                     │  11. agent.formatOutput(response)     │                    │
   │                    │                     │                     │  (Role-specific formatting)          │
   │                    │                     │<────────────────────┤                  │                    │
   │                    │                     │                     │                  │                    │
   │                    │  12. TaskResult     │                     │                  │                    │
   │                    │  (formatted output) │                     │                  │                    │
   │                    │<────────────────────┤                     │                  │                    │
   │                    │                     │                     │                  │                    │
   │  13. JSON Response │                     │                     │                  │                    │
   │  {output, usage}   │                     │                     │                  │                    │
   │<───────────────────┤                     │                     │                  │                    │
   │                    │                     │                     │                  │                    │
```

### Timing Breakdown

| Step | Description | Typical Duration |
|------|-------------|------------------|
| 1-2 | Request validation | 10-50ms |
| 3-4 | Agent lookup | 1-5ms |
| 5-7 | Prompt building | 10-50ms |
| 8 | HTTP request to LLM | 100-500ms |
| 9 | LLM processing | 10-25 seconds |
| 10 | Response parsing | 50-200ms |
| 11 | Output formatting | 100-500ms |
| 12-13 | Response serialization | 10-50ms |
| **Total** | **End-to-end** | **12-28 seconds** |

---

## 2. Multi-Agent Collaboration Workflow

### Parallel Execution Flow

```
┌──────┐          ┌─────────────┐                    ┌──────────────────────────────────┐
│ User │          │ RoleManager │                    │    Agent Execution (Parallel)    │
└──┬───┘          └──────┬──────┘                    └──────────────────────────────────┘
   │                     │
   │ POST /api/v1/execute-multi                      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
   │ roles: [Dev, QA, Security]                      │  Developer   │  │      QA      │  │   Security   │
   ├────────────────────>│                           │    Agent     │  │    Agent     │  │    Agent     │
   │                     │                           └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
   │                     │ 1. Validate all roles            │                 │                 │
   │                     │    exist                         │                 │                 │
   │                     │                                  │                 │                 │
   │                     │ 2. Create CompletableFuture      │                 │                 │
   │                     │    for each agent                │                 │                 │
   │                     │                                  │                 │                 │
   │                     │ 3. executeAsync()                │                 │                 │
   │                     ├─────────────────────────────────>│                 │                 │
   │                     │                                  │                 │                 │
   │                     │ 4. executeAsync()                                  │                 │
   │                     ├────────────────────────────────────────────────────>│                 │
   │                     │                                  │                 │                 │
   │                     │ 5. executeAsync()                                                    │
   │                     ├─────────────────────────────────────────────────────────────────────>│
   │                     │                                  │                 │                 │
   │                     │                   ┌──────────────▼─────────────────▼─────────────────▼─┐
   │                     │                   │    All agents execute in parallel (~25 seconds)   │
   │                     │                   │    - Each builds role-specific prompt             │
   │                     │                   │    - Each calls LLM independently                 │
   │                     │                   │    - Each formats output per role style           │
   │                     │                   └──────────────┬─────────────────┬─────────────────┬─┘
   │                     │                                  │                 │                 │
   │                     │ 6. TaskResult (Dev)              │                 │                 │
   │                     │<─────────────────────────────────┤                 │                 │
   │                     │                                  │                 │                 │
   │                     │ 7. TaskResult (QA)                                 │                 │
   │                     │<────────────────────────────────────────────────────┤                 │
   │                     │                                  │                 │                 │
   │                     │ 8. TaskResult (Security)                                             │
   │                     │<─────────────────────────────────────────────────────────────────────┤
   │                     │                                  │                 │                 │
   │                     │ 9. aggregateResults()            │                 │                 │
   │                     │    - Merge all outputs           │                 │                 │
   │                     │    - Calculate total usage       │                 │                 │
   │                     │    - Add cross-role summary      │                 │                 │
   │                     │                                  │                 │                 │
   │ 10. Combined Result │                                  │                 │                 │
   │ {                  │                                  │                 │                 │
   │   developer: {...},│                                  │                 │                 │
   │   qa: {...},       │                                  │                 │                 │
   │   security: {...}, │                                  │                 │                 │
   │   summary: "..."   │                                  │                 │                 │
   │ }                  │                                  │                 │                 │
   │<────────────────────┤                                  │                 │                 │
   │                     │                                  │                 │                 │
```

### Aggregation Logic

```java
MultiAgentResult aggregateResults(List<TaskResult> results) {
    return new MultiAgentResult(
        results = results,
        summary = generateCrossRoleSummary(results),
        totalUsage = sumUsage(results),
        executionTime = maxExecutionTime(results),
        consensus = identifyConsensus(results),
        conflicts = identifyConflicts(results)
    );
}
```

**Output Example**:
```json
{
  "results": {
    "developer": {
      "output": "Technical review: 3 code issues found...",
      "usage": {"inputTokens": 1200, "outputTokens": 800}
    },
    "qa": {
      "output": "Test coverage analysis: 65% coverage, missing edge cases...",
      "usage": {"inputTokens": 1100, "outputTokens": 600}
    },
    "security": {
      "output": "Security assessment: 1 high-severity vulnerability (SQL injection)...",
      "usage": {"inputTokens": 1300, "outputTokens": 700}
    }
  },
  "summary": "All three roles identified concerns. Priority: Fix SQL injection (Security), then address code issues (Developer), finally improve test coverage (QA).",
  "totalUsage": {"inputTokens": 3600, "outputTokens": 2100},
  "executionTime": "26 seconds"
}
```

---

## 3. Error Handling and Retry Logic

### Retry Flow with Exponential Backoff

```
┌─────────────┐
│ Agent       │
│ executeTask │
└──────┬──────┘
       │
       │ 1. Call LLM API
       ▼
┌─────────────────┐
│ LLMProvider     │
│ createCompletion│
└──────┬──────────┘
       │
       │ 2. HTTP Request
       ▼
┌─────────────────────────────────────────────────────────────┐
│                   LLM API (External)                        │
└──────┬──────────────────────────────────────────────────────┘
       │
       ├──────────────────────────────────────────────────────┐
       │                                                      │
       ▼ SUCCESS                                             ▼ FAILURE
┌─────────────────┐                                   ┌─────────────────┐
│ Return Response │                                   │ Exception Thrown│
└─────────────────┘                                   └──────┬──────────┘
                                                             │
                                                             ▼
                                                      ┌─────────────────────┐
                                                      │ Retry Handler       │
                                                      │ - Attempt count: 0  │
                                                      └──────┬──────────────┘
                                                             │
                  ┌──────────────────────────────────────────┼──────────────────────────────────────────┐
                  │                                          │                                          │
                  ▼ Retryable (429, 500, 503)                ▼ Non-Retryable (400, 401)                ▼ Timeout
           ┌─────────────────┐                        ┌─────────────────┐                      ┌─────────────────┐
           │ Wait & Retry    │                        │ Fail Fast       │                      │ Wait & Retry    │
           │                 │                        │                 │                      │                 │
           │ Attempt 1:      │                        │ - Log error     │                      │ Attempt 1:      │
           │   Wait 1s       │                        │ - Return error  │                      │   Wait 2s       │
           └────────┬────────┘                        │   to user       │                      └────────┬────────┘
                    │                                 └─────────────────┘                               │
                    ▼                                                                                   ▼
           ┌─────────────────┐                                                                 ┌─────────────────┐
           │ Retry Call      │                                                                 │ Retry Call      │
           └────────┬────────┘                                                                 └────────┬────────┘
                    │                                                                                   │
                    ├──────────────┬──────────────┐                                                    ├──────────────┐
                    ▼ SUCCESS      ▼ FAILURE      │                                                    ▼ SUCCESS      ▼ FAILURE
              ┌──────────┐  ┌─────────────┐      │                                              ┌──────────┐  ┌─────────────┐
              │ Return   │  │ Attempt 2:  │      │                                              │ Return   │  │ Attempt 2:  │
              │ Response │  │   Wait 2s   │      │                                              │ Response │  │   Wait 4s   │
              └──────────┘  └──────┬──────┘      │                                              └──────────┘  └──────┬──────┘
                                   │             │                                                                     │
                                   ▼             │                                                                     ▼
                            ┌─────────────┐      │                                                              ┌─────────────┐
                            │ Retry Call  │      │                                                              │ Retry Call  │
                            └──────┬──────┘      │                                                              └──────┬──────┘
                                   │             │                                                                     │
                                   ├─────────┬───┘                                                                     ├─────────┐
                                   ▼ SUCCESS ▼ FAILURE                                                                 ▼ SUCCESS ▼ FAILURE
                             ┌──────────┐ ┌─────────────┐                                                        ┌──────────┐ ┌─────────────┐
                             │ Return   │ │ Attempt 3:  │                                                        │ Return   │ │ Max Retries │
                             │ Response │ │   Wait 4s   │                                                        │ Response │ │ Exceeded    │
                             └──────────┘ └──────┬──────┘                                                        └──────────┘ └──────┬──────┘
                                                 │                                                                                    │
                                                 ▼                                                                                    ▼
                                          ┌─────────────┐                                                                     ┌─────────────┐
                                          │ Retry Call  │                                                                     │ Return Error│
                                          └──────┬──────┘                                                                     │ to User     │
                                                 │                                                                             └─────────────┘
                                                 ├─────────┬────────────┐
                                                 ▼ SUCCESS ▼ FAILURE    │
                                           ┌──────────┐ ┌──────────────┴──┐
                                           │ Return   │ │ All Retries     │
                                           │ Response │ │ Failed - Return │
                                           └──────────┘ │ Error to User   │
                                                        └─────────────────┘
```

### Retry Configuration

```java
RetryConfig retryConfig = RetryConfig.builder()
    .maxAttempts(3)
    .waitDuration(Duration.ofSeconds(1))
    .backoffMultiplier(2.0)  // Exponential: 1s, 2s, 4s
    .retryableExceptions(
        HttpTimeoutException.class,
        HttpServerErrorException.class,  // 5xx
        RateLimitException.class         // 429
    )
    .nonRetryableExceptions(
        AuthenticationException.class,   // 401
        InvalidRequestException.class    // 400
    )
    .build();
```

### Error Categories

| Error Type | HTTP Status | Action | Retry |
|------------|-------------|--------|-------|
| **Rate Limit** | 429 | Wait (exponential backoff) | ✅ Yes (3 attempts) |
| **Server Error** | 500, 502, 503 | Retry immediately | ✅ Yes (3 attempts) |
| **Timeout** | - | Increase timeout, retry | ✅ Yes (3 attempts) |
| **Auth Error** | 401, 403 | Check API key | ❌ No (fail fast) |
| **Invalid Request** | 400 | Validate input | ❌ No (fail fast) |
| **Model Not Found** | 404 | Check model name | ❌ No (fail fast) |

---

## 4. LLM Provider Failover Flow

### Provider Failover Decision Tree

```
┌─────────────────────────────────────────────────────────────────────┐
│                    LLM Provider Selection Flow                      │
└─────────────────────────────────────────────────────────────────────┘

                          ┌─────────────────┐
                          │ Task Execution  │
                          │   Request       │
                          └────────┬────────┘
                                   │
                                   ▼
                     ┌─────────────────────────┐
                     │ LLMProviderFactory      │
                     │ getPrimaryProvider()    │
                     └────────┬────────────────┘
                              │
                              ▼
                  ┌───────────────────────────┐
                  │ Primary Provider          │
                  │ (Anthropic - Claude)      │
                  └───────┬───────────────────┘
                          │
                          │ Call API
                          ▼
              ┌───────────────────────────────┐
              │   Primary Provider Response   │
              └───────┬───────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼ SUCCESS     ▼ RETRYABLE   ▼ FAILURE (Non-Retryable)
   ┌─────────┐   ┌────────────┐   ┌───────────────────────────┐
   │ Return  │   │ Retry with │   │ Check Failover Enabled?   │
   │ Result  │   │ Exponential│   └───────┬───────────────────┘
   └─────────┘   │ Backoff    │           │
                 └─────┬──────┘           │
                       │                  ├──────────────┬─────────────────┐
                       ▼                  ▼ YES          ▼ NO               │
                  ┌─────────┐      ┌────────────┐   ┌────────────┐        │
                  │ All     │      │ Fallback to│   │ Return     │        │
                  │ Retries │      │ Secondary  │   │ Error      │        │
                  │ Failed  │      │ Provider   │   └────────────┘        │
                  └────┬────┘      └─────┬──────┘                         │
                       │                 │                                │
                       └─────────────────┼────────────────────────────────┘
                                         │
                                         ▼
                              ┌─────────────────────┐
                              │ Secondary Provider  │
                              │ (OpenAI - GPT-4)    │
                              └──────────┬──────────┘
                                         │
                                         │ Call API
                                         ▼
                              ┌─────────────────────┐
                              │ Secondary Response  │
                              └──────────┬──────────┘
                                         │
                          ┌──────────────┼──────────────┐
                          │              │              │
                          ▼ SUCCESS      ▼ RETRYABLE    ▼ FAILURE
                     ┌─────────┐    ┌────────────┐  ┌───────────────────┐
                     │ Return  │    │ Retry with │  │ Fallback to       │
                     │ Result  │    │ Exponential│  │ Tertiary Provider │
                     │ (Mark   │    │ Backoff    │  └────────┬──────────┘
                     │ Primary │    └─────┬──────┘           │
                     │ as Down)│          │                  │
                     └─────────┘          ▼                  ▼
                                     ┌─────────┐      ┌─────────────────┐
                                     │ All     │      │ Tertiary        │
                                     │ Retries │      │ Provider        │
                                     │ Failed  │      │ (Ollama - Local)│
                                     └────┬────┘      └────────┬────────┘
                                          │                    │
                                          └────────────┬───────┘
                                                       │
                                                       ▼
                                              ┌────────────────┐
                                              │ All Providers  │
                                              │ Failed         │
                                              │ Return Error   │
                                              └────────────────┘
```

### Provider Priority Configuration

```yaml
llm:
  providers:
    - name: anthropic
      priority: 1
      enabled: true
      timeout: 30s
      max_retries: 3
    - name: openai
      priority: 2
      enabled: true
      timeout: 30s
      max_retries: 3
    - name: ollama
      priority: 3
      enabled: false  # Local fallback only
      timeout: 60s
      max_retries: 1

  failover:
    enabled: true
    circuit_breaker:
      failure_threshold: 5        # Open circuit after 5 failures
      timeout: 30s                # Try again after 30s
      success_threshold: 2        # Close circuit after 2 successes
```

---

## 5. Configuration Loading Workflow

### Application Startup Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Application Startup Flow                          │
└──────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │ Spring Boot         │
                    │ Application.main()  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Load application.yml│
                    │ - Server config     │
                    │ - LLM provider URLs │
                    │ - Agent config dir  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Load Environment    │
                    │ Variables           │
                    │ - ANTHROPIC_API_KEY │
                    │ - OPENAI_API_KEY    │
                    │ - LLM_PROVIDER      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Initialize Spring   │
                    │ Context             │
                    │ - @Component scan   │
                    │ - @Autowired inject │
                    └──────────┬──────────┘
                               │
                               ├─────────────────────────────┐
                               │                             │
                               ▼                             ▼
                    ┌──────────────────┐         ┌──────────────────┐
                    │ Create           │         │ Create Agent     │
                    │ LLMProviderFactory│         │ Registry         │
                    │ - Anthropic      │         │                  │
                    │ - OpenAI         │         └────────┬─────────┘
                    │ - Ollama         │                  │
                    └────────┬─────────┘                  │
                             │                            │
                             │                            ▼
                             │                 ┌──────────────────────┐
                             │                 │ Load Agent Configs   │
                             │                 │ config/agents/*.yaml │
                             │                 └────────┬─────────────┘
                             │                          │
                             │                          │ For each YAML file
                             │                          ▼
                             │                 ┌──────────────────────┐
                             │                 │ Parse Agent Config   │
                             │                 │ - role               │
                             │                 │ - description        │
                             │                 │ - capabilities       │
                             │                 │ - prompt_template    │
                             │                 │ - temperature        │
                             │                 │ - max_tokens         │
                             │                 └────────┬─────────────┘
                             │                          │
                             └──────────┬───────────────┘
                                        │
                                        ▼
                             ┌──────────────────────┐
                             │ Instantiate Agents   │
                             │ - DeveloperAgent     │
                             │ - QAAgent            │
                             │ - SecurityAgent      │
                             │ - ... (all 13)       │
                             └────────┬─────────────┘
                                      │
                                      ▼
                             ┌──────────────────────┐
                             │ Register Agents      │
                             │ registry.put(name,   │
                             │   agent)             │
                             └────────┬─────────────┘
                                      │
                                      ▼
                             ┌──────────────────────┐
                             │ Validate Registry    │
                             │ - All 13 agents?     │
                             │ - No duplicates?     │
                             │ - Valid configs?     │
                             └────────┬─────────────┘
                                      │
                                      ▼
                             ┌──────────────────────┐
                             │ Start Web Server     │
                             │ localhost:8080       │
                             └────────┬─────────────┘
                                      │
                                      ▼
                             ┌──────────────────────┐
                             │ Application Ready    │
                             │ - Health: /actuator  │
                             │ - API: /api/v1/...   │
                             └──────────────────────┘
```

### Configuration File Locations

```
role-manager-app/
├── application.yml                    # Main config
├── application-dev.yml                # Dev overrides
├── application-prod.yml               # Prod overrides
└── config/agents/                     # Agent configs
    ├── developer.yaml
    ├── qa-engineer.yaml
    ├── security-engineer.yaml
    ├── devops-engineer.yaml
    ├── architect.yaml
    ├── manager.yaml
    ├── product-owner.yaml
    ├── it-operations.yaml
    ├── legal.yaml
    ├── finance.yaml
    ├── director.yaml
    ├── cto.yaml
    └── sponsor.yaml
```

---

## 6. Performance Characteristics

### Response Time Targets

| Operation | Target | Max Acceptable |
|-----------|--------|----------------|
| **Single Agent Task** | <15s | 30s |
| **Multi-Agent (3 roles)** | <25s | 45s |
| **Role List** | <100ms | 500ms |
| **Role Description** | <50ms | 200ms |
| **Config Reload** | <2s | 5s |

### Throughput Targets

| Metric | Target |
|--------|--------|
| **Concurrent Tasks** | 10+ |
| **Requests/Minute** | 100+ |
| **Agent Registry Lookup** | <5ms |
| **Prompt Building** | <50ms |

### Resource Usage

| Component | CPU | Memory | Network |
|-----------|-----|--------|---------|
| **RoleManager Service** | ~5% | 50MB | - |
| **AgentRegistry** | <1% | 10MB | - |
| **LLMProvider (per call)** | ~2% | 20MB | 1-5 KB/s |
| **Total Application** | ~15% | 256MB | Varies |

---

## References

1. [Data Flow Diagrams](dataflow.md) - Data transformations and formats
2. [Workflow vs Data Flow Guide](../4-development/guide/workflow-vs-dataflow.md) - Understanding the difference
3. [Architecture Design](architecture.md) - Component design and patterns
4. [API Design](api-design.md) - REST endpoints and CLI commands
5. [Use Cases](../2-analysis/use-cases.md) - Primary use cases

---

*Last Updated: 2025-10-17*
