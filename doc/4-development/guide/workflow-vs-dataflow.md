# Workflow vs Data Flow - Understanding the Difference

**Document Type:** Developer Guide
**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Workflow** = "What happens when" (process sequence, timing, actors). **Data Flow** = "What becomes what" (data transformations, state changes, formats). **Key difference**: Workflow tracks execution steps; Data Flow tracks data shape changes. **Both needed**: Workflow for timing/orchestration, Data Flow for understanding transformations. **In Role Manager**: Workflow shows 13-step sequence; Data Flow shows 5 transformation stages.

---

## Table of Contents

- [Overview](#overview)
- [What is Workflow?](#what-is-workflow)
- [What is Data Flow?](#what-is-data-flow)
- [Key Differences](#key-differences)
- [Visual Comparison](#visual-comparison)
- [When to Use Each](#when-to-use-each)
- [In the Role Manager App](#in-the-role-manager-app)
- [References](#references)

---

## Overview

When documenting software systems, developers often need to visualize two different aspects:

1. **Workflow (Control Flow)** - The sequence of operations and who performs them
2. **Data Flow** - How data transforms as it moves through the system

While related, these are **distinct concepts** that answer different questions. Understanding the difference helps you:

- Choose the right diagram type for your documentation needs
- Communicate system behavior more effectively
- Debug issues faster (timing vs transformation problems)
- Design better systems (process vs data architecture)

---

## What is Workflow?

### Definition

**Workflow** (also called **Control Flow**) describes the **sequence of steps** in a process, including:

- What operations happen
- In what order they happen
- Who (which actor/component) performs each operation
- When operations occur (timing, duration)
- Decision points and branching logic

### Focus

Workflow answers:
- "What happens first? Then what?"
- "Who does what?"
- "How long does each step take?"
- "What are the decision points?"

### Visual Representation

Common diagram types:
- **Sequence Diagrams** - Show interactions between actors over time
- **Flowcharts** - Show decision logic and branching
- **Activity Diagrams** - Show parallel activities and swimlanes
- **Process Flow Diagrams** - Show end-to-end process steps

### Example: Single-Agent Workflow

```
Step 1: User sends request (0ms)
    ↓
Step 2: API validates request (10-50ms)
    ↓
Step 3: RoleManager looks up agent (1-5ms)
    ↓
Step 4: Agent builds prompt (10-50ms)
    ↓
Step 5: Agent calls LLM API (100-500ms network latency)
    ↓
Step 6: LLM processes request (10-25 seconds)
    ↓
Step 7: Agent formats output (100-500ms)
    ↓
Step 8: System returns result (10-50ms)
```

### Key Characteristics

- **Temporal** - Organized by time/sequence
- **Actor-focused** - Shows which component does what
- **Timing-aware** - Includes durations and latency
- **Control-oriented** - Shows decision points and branching
- **Observable** - Can be traced in logs and metrics

---

## What is Data Flow?

### Definition

**Data Flow** describes how **data changes shape and state** as it moves through the system, including:

- Input data formats
- Transformation operations
- Intermediate data states
- Output data formats
- Data type changes

### Focus

Data Flow answers:
- "What format is the data at each stage?"
- "How does the data transform?"
- "What gets added or removed?"
- "What's the structure at each step?"

### Visual Representation

Common diagram types:
- **Data Flow Diagrams (DFD)** - Show data movement and transformations
- **Pipeline Diagrams** - Show data processing stages
- **State Transition Diagrams** - Show data state changes
- **Entity-Relationship Diagrams** - Show data relationships

### Example: Single-Agent Data Flow

```
Raw JSON Request
    ↓ (parse & validate)
TaskRequest Record {roleName, task, context}
    ↓ (enrich with metadata)
EnrichedTask {task, roleInfo, timestamp, context}
    ↓ (build prompt string)
String Prompt "You are expert {role}. Task: {task}..."
    ↓ (LLM processing)
LLMResponse {text, usage, finishReason}
    ↓ (format for role)
String FormattedOutput (Markdown with code blocks)
    ↓ (wrap in result object)
TaskResult {roleName, output, usage, timestamp}
```

### Key Characteristics

- **Structural** - Organized by data shape/state
- **Type-focused** - Shows data types and formats
- **Transformation-aware** - Shows how data changes
- **Content-oriented** - Shows what's in the data
- **Traceable** - Can be verified with assertions

---

## Key Differences

| Aspect | Workflow (Control Flow) | Data Flow |
|--------|------------------------|-----------|
| **Primary Focus** | Process sequence and timing | Data transformation and state |
| **Answers** | "What happens when?" | "What becomes what?" |
| **Vertical Axis** | Time / execution order | Data states / formats |
| **Shows** | Steps, actors, decisions | Data shapes, transformations |
| **Diagrams** | Sequence, flowchart, activity | DFD, pipeline, transformation |
| **Variables** | Operations, timing, actors | Data types, formats, content |
| **Debug Focus** | "Why is it slow?" "What step failed?" | "Why is data wrong?" "What format?" |
| **Design Focus** | Orchestration, coordination | Data modeling, serialization |
| **Metrics** | Response time, throughput | Data size, transformation cost |
| **Examples** | "Step 5 takes 25 seconds" | "JSON becomes TaskRequest record" |

---

## Visual Comparison

### Workflow View (Control Flow)

```
┌──────┐      ┌─────────┐      ┌────────────┐      ┌────────┐
│ User │─────>│   API   │─────>│ RoleManager│─────>│  Agent │
└──────┘      └─────────┘      └────────────┘      └────────┘
  (0ms)         (50ms)             (5ms)            (25s)

Shows: WHO does WHAT and WHEN
```

### Data Flow View (Data Transformation)

```
JSON          TaskRequest      EnrichedTask      LLMResponse
{role, task}  Record           +metadata         {text, usage}
      ↓             ↓                 ↓                 ↓
   Parse        Enrich          Build Prompt      Format

Shows: WHAT data becomes WHAT at each stage
```

---

## When to Use Each

### Use Workflow Diagrams When:

✅ You need to understand **timing and performance**
- "Why is this request taking so long?"
- "Where are the bottlenecks?"
- "What's the critical path?"

✅ You need to understand **sequencing and orchestration**
- "What happens first?"
- "What depends on what?"
- "Can these run in parallel?"

✅ You need to understand **actor responsibilities**
- "Which component handles authentication?"
- "Who calls the LLM API?"
- "What's the entry point?"

✅ You're debugging **process failures**
- "Where did the request fail?"
- "Which step timed out?"
- "What's the retry logic?"

### Use Data Flow Diagrams When:

✅ You need to understand **data transformations**
- "How does JSON become a Java object?"
- "What format does the LLM expect?"
- "What gets added at each stage?"

✅ You need to understand **data validation**
- "What fields are required?"
- "When is validation performed?"
- "What's the schema at this point?"

✅ You need to understand **data serialization**
- "How is data stored?"
- "What format is sent over the wire?"
- "What's the API contract?"

✅ You're debugging **data issues**
- "Why is this field null?"
- "Why is the format wrong?"
- "What data got lost?"

### Use Both Together When:

🎯 **Comprehensive system documentation**
- Show both process flow AND data transformations
- Helps new developers understand the complete picture

🎯 **API documentation**
- Show request/response flow (workflow) AND data schemas (dataflow)

🎯 **Debugging complex issues**
- Isolate whether problem is timing (workflow) or data (dataflow)

---

## In the Role Manager App

### Workflow Documentation

**File:** [`doc/3-design/workflow.md`](../../3-design/workflow.md)

**Contains:**
1. **Single-Agent Task Execution Sequence** (13 steps, 12-28s)
   - User → API → RoleManager → Registry → Agent → LLM → Format → Response
   - Timing breakdown for each step

2. **Multi-Agent Collaboration Workflow** (parallel execution)
   - 3 agents execute simultaneously using CompletableFuture
   - Result aggregation and consensus detection

3. **Error Handling and Retry Logic** (exponential backoff)
   - Retry flow with 1s, 2s, 4s delays
   - Retryable vs non-retryable error classification

4. **LLM Provider Failover Flow** (circuit breaker)
   - Primary (Anthropic) → Secondary (OpenAI) → Tertiary (Ollama)
   - Circuit breaker state transitions

5. **Configuration Loading Workflow** (application startup)
   - Spring Boot initialization sequence
   - Agent registration and validation

**Use for:** Understanding timing, orchestration, error handling, startup sequence

### Data Flow Documentation

**File:** [`doc/3-design/dataflow.md`](../../3-design/dataflow.md)

**Contains:**
1. **End-to-End Data Transformation Pipeline** (5 stages)
   - INPUT: Raw JSON → Validated TaskRequest
   - ENRICHMENT: TaskRequest → EnrichedTask (+metadata)
   - EXECUTION: EnrichedTask → String Prompt
   - PROCESSING: LLM Response → Formatted Output
   - OUTPUT: Formatted Output → TaskResult (+audit)

2. **Data Transformation Details** (with code examples)
   - JSON → Java Records
   - Records → Enriched objects
   - Objects → LLM prompts
   - Responses → Role-specific formats

3. **Output Format Examples**
   - Developer: Technical markdown with code blocks
   - Manager: Business metrics and summaries
   - Executive: One-page executive summaries

4. **Multi-Agent Data Aggregation**
   - Merge multiple agent outputs
   - Generate cross-role summary
   - Identify consensus and conflicts

**Use for:** Understanding data transformations, formats, serialization, validation

---

## Practical Examples from Role Manager

### Example 1: Performance Debugging

**Problem:** "Single-agent requests are taking 45 seconds (SLA is 30s)"

**Solution Approach:**
1. ✅ **Check Workflow diagram** (timing breakdown)
   - Find which step takes longest
   - Likely Step 6: LLM Processing (10-25s normal)
   - If >30s, check LLM provider health

2. ❌ **Data Flow diagram won't help**
   - Data transformations are fast (<1s total)
   - This is a timing issue, not a data issue

**Answer:** Look at workflow.md → Single-Agent Task Execution → Timing Breakdown

---

### Example 2: Data Validation Debugging

**Problem:** "Getting 400 errors when executing tasks"

**Solution Approach:**
1. ❌ **Workflow diagram won't help directly**
   - Shows the request fails at validation step
   - But doesn't show WHAT's wrong with the data

2. ✅ **Check Data Flow diagram** (data transformations)
   - Stage 1: Input → Validation
   - Check required fields: roleName, task
   - Check data types and format

**Answer:** Look at dataflow.md → Data Transformation Details → Stage 1

---

### Example 3: Adding a New Agent

**Problem:** "How do I add a new role agent?"

**Solution Approach:**
1. ✅ **Check Workflow diagram** (configuration loading)
   - Understand startup sequence
   - See where agents are registered
   - Understand validation checks

2. ✅ **Check Data Flow diagram** (agent config format)
   - See YAML → AgentConfig transformation
   - Understand required fields
   - See how prompts are built

**Answer:** Use both workflow.md and dataflow.md for complete picture

---

### Example 4: Multi-Agent Output Issues

**Problem:** "Multi-agent results aren't aggregating correctly"

**Solution Approach:**
1. ✅ **Check Workflow diagram** (multi-agent orchestration)
   - See parallel execution using CompletableFuture
   - Understand aggregateResults() timing
   - Check if all agents completed

2. ✅ **Check Data Flow diagram** (data aggregation)
   - See MultiAgentResult structure
   - Understand merge logic
   - Check consensus/conflict detection

**Answer:** Use both workflow.md (parallel execution) and dataflow.md (aggregation logic)

---

## Best Practices

### When Creating Documentation

1. **Separate concerns**
   - Don't mix workflow and dataflow in same diagram
   - Create focused, single-purpose diagrams

2. **Link them together**
   - Reference related diagrams ("See dataflow.md for data formats")
   - Show relationships ("Step 5 transforms data as shown in Stage 3")

3. **Choose the right level of detail**
   - Workflow: Show timing, not data internals
   - Dataflow: Show data shape, not timing

### When Reading Documentation

1. **Identify your question type**
   - Timing question → workflow.md
   - Data question → dataflow.md
   - Both → check both docs

2. **Start with the right diagram**
   - Don't force a workflow diagram to answer data questions
   - Use the tool designed for your problem

3. **Follow cross-references**
   - Documents reference each other
   - Complete picture requires both views

---

## Common Mistakes to Avoid

### ❌ Mixing Workflow and Data Flow

**Bad:**
```
User sends JSON {role: "Dev"}
  ↓ (50ms parsing)
TaskRequest(roleName="Dev", task="...")
  ↓ (5ms lookup)
Agent found
  ↓ (25s processing)
LLMResponse(text="...", usage={tokens: 1000})
```

**Problem:** Confuses timing with data. Hard to read either aspect.

**Better:** Separate into:
- Workflow: User → Parse (50ms) → Lookup (5ms) → Process (25s)
- Dataflow: JSON → TaskRequest → EnrichedTask → LLMResponse

### ❌ Using Wrong Diagram for Question

**Bad:**
- Using workflow diagram to debug "Why is field X missing?"
- Using dataflow diagram to debug "Why is this slow?"

**Better:**
- Data question → dataflow.md
- Timing question → workflow.md

### ❌ Over-detailed Diagrams

**Bad:**
- Workflow showing every data field at every step
- Dataflow showing timing for every transformation

**Better:**
- Workflow: Focus on steps, actors, timing
- Dataflow: Focus on data shape, not timing

---

## Summary Table

| Question | Use | File |
|----------|-----|------|
| How long does this take? | Workflow | workflow.md |
| What's the sequence? | Workflow | workflow.md |
| Who does what? | Workflow | workflow.md |
| What format is the data? | Data Flow | dataflow.md |
| How does data transform? | Data Flow | dataflow.md |
| What fields are required? | Data Flow | dataflow.md |
| Where are bottlenecks? | Workflow | workflow.md |
| Why is data wrong? | Data Flow | dataflow.md |
| How do agents collaborate? | Workflow | workflow.md |
| How is output formatted? | Data Flow | dataflow.md |

---

## References

### Role Manager Documentation

1. [**Workflow Diagrams**](../../3-design/workflow.md) - Process sequences, timing, orchestration
2. [**Data Flow Diagrams**](../../3-design/dataflow.md) - Data transformations, formats, schemas
3. [**Architecture Design**](../../3-design/architecture.md) - Component design and patterns
4. [**Developer Guide**](../developer-guide.md) - Setup and development workflow

### External Resources

1. **Workflow/Process Modeling:**
   - [UML Sequence Diagrams](https://www.uml-diagrams.org/sequence-diagrams.html)
   - [BPMN Process Modeling](https://www.bpmn.org/)

2. **Data Flow Modeling:**
   - [Data Flow Diagrams (DFD)](https://en.wikipedia.org/wiki/Data-flow_diagram)
   - [Structured Analysis and Design](https://en.wikipedia.org/wiki/Structured_analysis)

3. **Software Documentation:**
   - [C4 Model](https://c4model.com/) - Context, Containers, Components, Code
   - [Arc42 Architecture Documentation](https://arc42.org/)

---

*Last Updated: 2025-10-17*
