# Data Flow Diagrams - Role Manager App

**Document Type:** Design Artifact
**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Purpose**: Visual documentation of data transformations in Role Manager App. **Key diagrams**: 5-stage pipeline (INPUT → VALIDATION → ENRICHMENT → EXECUTION → OUTPUT), data format examples (JSON → TaskRequest → EnrichedTask → Prompt → LLMResponse → TaskResult), role-specific formatting. **Use this**: To understand data shape, transformations, validation, and serialization. **See also**: [Workflow Diagrams](workflow.md) for process sequences.

---

## Table of Contents

- [1. End-to-End Data Transformation Pipeline](#1-end-to-end-data-transformation-pipeline)
- [2. Data Transformation Details](#2-data-transformation-details)
- [3. Role-Specific Output Formatting](#3-role-specific-output-formatting)
- [4. Multi-Agent Data Aggregation](#4-multi-agent-data-aggregation)
- [References](#references)

---

## 1. End-to-End Data Transformation Pipeline

### End-to-End Data Transformation Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          ROLE MANAGER DATA FLOW PIPELINE                            │
└─────────────────────────────────────────────────────────────────────────────────────┘

INPUT                    VALIDATION              ENRICHMENT              EXECUTION
┌──────────────┐        ┌──────────────┐        ┌──────────────┐       ┌──────────────┐
│ Raw Request  │        │  Validated   │        │  Enriched    │       │ LLM Prompt   │
│              │        │  TaskRequest │        │  Task        │       │              │
│ - roleName   │───────>│              │───────>│              │──────>│ System:      │
│ - task       │ Parse  │ - roleName✓  │ Add    │ - task       │Build  │   {role ctx} │
│ - context    │        │ - task✓      │Context │ - context    │Prompt │ User:        │
│   (optional) │        │ - context    │        │ - roleInfo   │       │   {task}     │
│              │        │              │        │ - timestamp  │       │              │
└──────────────┘        └──────────────┘        └──────────────┘       └──────────────┘
      │                       │                        │                      │
      │ JSON                  │ Java Record            │ Java Record          │ String
      │                       │                        │                      │
      ▼                       ▼                        ▼                      ▼

PROCESSING              FORMATTING              OUTPUT                PERSISTENCE
┌──────────────┐        ┌──────────────┐        ┌──────────────┐       ┌──────────────┐
│ LLM Response │        │  Formatted   │        │ TaskResult   │       │ Audit Log    │
│              │        │  Output      │        │              │       │              │
│ - text       │───────>│              │───────>│ - taskId     │──────>│ - timestamp  │
│ - usage      │Format  │ Technical:   │Wrap    │ - roleName   │Store  │ - request    │
│ - finish     │Output  │   Markdown   │Result  │ - output     │Async  │ - response   │
│   reason     │        │ Executive:   │        │ - usage      │       │ - tokens     │
│              │        │   Summary    │        │ - timestamp  │       │ - cost       │
└──────────────┘        └──────────────┘        └──────────────┘       └──────────────┘
      │                       │                        │                      │
      │ LLMResponse           │ String                 │ TaskResult           │ AuditEntry
      │                       │                        │                      │
      ▼                       ▼                        ▼                      ▼

                        ┌──────────────────────────────────┐
                        │     RETURN TO CLIENT             │
                        │  - HTTP 200 OK                   │
                        │  - JSON: {output, usage, time}   │
                        └──────────────────────────────────┘
```

### Data Transformation Details

#### Stage 1: Input → Validation
```java
// Raw JSON
{
  "role": "Software Developer",
  "task": "Review PR #123",
  "context": {"repo": "my-app", "branch": "feature-x"}
}

// Transforms to
TaskRequest(
  roleName = "Software Developer",
  task = "Review PR #123",
  context = Map.of("repo", "my-app", "branch", "feature-x")
)
```

#### Stage 2: Validation → Enrichment
```java
// Adds role metadata
EnrichedTask(
  task = "Review PR #123",
  context = {...},
  roleInfo = RoleInfo(
    name = "Software Developer",
    capabilities = ["Code review", "Bug diagnosis"],
    promptTemplate = "You are an expert developer..."
  ),
  timestamp = Instant.now()
)
```

#### Stage 3: Enrichment → Execution
```java
// Builds role-specific prompt
"""
You are an expert Software Developer assisting with code reviews.

Task: Review PR #123

Context:
- Repository: my-app
- Branch: feature-x

Provide detailed technical analysis with:
- Specific file paths and line numbers
- Code examples
- Best practices
- Potential bugs or improvements

Format output as structured markdown.
"""
```

#### Stage 4: Processing → Formatting

**Developer Role** (Technical Format):
```markdown
## Code Review: PR #123

### Summary
3 issues found, 2 improvements suggested

### Issues
1. **Memory Leak** (src/service.java:45)
   - Current: `connection.open()` never closed
   - Fix: Use try-with-resources
   ```java
   try (Connection conn = connection.open()) { ... }
   ```
...
```

**Executive Role** (Summary Format):
```markdown
## PR #123 Review

✓ Overall Status: Approved with minor changes
⚠ Issues: 3 (2 critical, 1 minor)
📊 Risk: Low
⏱ Estimated Fix Time: 2 hours
```


---

## 2. Data Transformation Details

This section provides concrete code examples showing how data transforms at each stage of the pipeline.

### Data Transformation Details

#### Stage 1: Input → Validation
```java
// Raw JSON
{
  "role": "Software Developer",
  "task": "Review PR #123",
  "context": {"repo": "my-app", "branch": "feature-x"}
}

// Transforms to
TaskRequest(
  roleName = "Software Developer",
  task = "Review PR #123",
  context = Map.of("repo", "my-app", "branch", "feature-x")
)
```

#### Stage 2: Validation → Enrichment
```java
// Adds role metadata
EnrichedTask(
  task = "Review PR #123",
  context = {...},
  roleInfo = RoleInfo(
    name = "Software Developer",
    capabilities = ["Code review", "Bug diagnosis"],
    promptTemplate = "You are an expert developer..."
  ),
  timestamp = Instant.now()
)
```

#### Stage 3: Enrichment → Execution
```java
// Builds role-specific prompt
"""
You are an expert Software Developer assisting with code reviews.

Task: Review PR #123

Context:
- Repository: my-app
- Branch: feature-x

Provide detailed technical analysis with:
- Specific file paths and line numbers
- Code examples
- Best practices
- Potential bugs or improvements

Format output as structured markdown.
"""
```

#### Stage 4: Processing → Formatting

**Developer Role** (Technical Format):
```markdown
## Code Review: PR #123

### Summary
3 issues found, 2 improvements suggested

### Issues
1. **Memory Leak** (src/service.java:45)
   - Current: `connection.open()` never closed
   - Fix: Use try-with-resources
   ```java
   try (Connection conn = connection.open()) { ... }
   ```
...
```

**Executive Role** (Summary Format):
```markdown
## PR #123 Review

✓ Overall Status: Approved with minor changes
⚠ Issues: 3 (2 critical, 1 minor)
📊 Risk: Low
⏱ Estimated Fix Time: 2 hours
```

---

## 3. Role-Specific Output Formatting

Different stakeholder roles receive outputs formatted to match their communication style and technical depth.

### Technical Role Format (Developer, DevOps, QA, Security)

Technical roles receive detailed, code-heavy outputs with:
- Code blocks and syntax highlighting
- File paths and line numbers
- Technical jargon and terminology
- Detailed analysis and step-by-step instructions

**Example - Developer Code Review:**
**Developer Role** (Technical Format):
```markdown
## Code Review: PR #123

### Summary
3 issues found, 2 improvements suggested

### Issues
1. **Memory Leak** (src/service.java:45)
   - Current: `connection.open()` never closed
   - Fix: Use try-with-resources
   ```java
   try (Connection conn = connection.open()) { ... }
   ```
...

### Executive Role Format (CTO, Director, Sponsor)

Executive roles receive concise, high-level summaries with:
- One-page summaries
- Business value and ROI
- Risk assessments
- Strategic recommendations

**Example - Executive PR Review:**
**Executive Role** (Summary Format):
```markdown
## PR #123 Review

✓ Overall Status: Approved with minor changes
⚠ Issues: 3 (2 critical, 1 minor)
📊 Risk: Low
⏱ Estimated Fix Time: 2 hours
```

---

## 4. Multi-Agent Data Aggregation

When multiple agents process the same task, their outputs are aggregated into a unified result.

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

### Aggregated Output Example

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

## References

1. [Workflow Diagrams](workflow.md) - Process sequences and timing
2. [Workflow vs Data Flow Guide](../4-development/guide/workflow-vs-dataflow.md) - Understanding the difference
3. [Architecture Design](architecture.md) - Component design and patterns
4. [Data Model](data-model.md) - Record definitions and schemas
5. [API Design](api-design.md) - API contracts and request/response formats

---

*Last Updated: 2025-10-17*
