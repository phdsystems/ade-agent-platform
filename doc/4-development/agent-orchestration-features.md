# Agent Orchestration Features - Role Manager App

**Date:** 2025-10-18
**Version:** 1.0

## Overview

Four new agent orchestration features have been implemented to enhance the Role Manager App's multi-agent capabilities:

1. **Multi-agent Parallel Execution** - Execute multiple agents concurrently using virtual threads
2. **Agent Chaining/Workflows** - DAG-based workflow orchestration with dependencies
3. **Prompt Template Management** - Dynamic prompt generation with variable substitution
4. **Model Fine-tuning Integration** - Training data collection and export

## Features Implemented

### 1. Multi-Agent Parallel Execution

**Location:** `src/main/java/com/rolemanager/orchestration/ParallelAgentExecutor.java`

**Purpose:** Execute multiple tasks concurrently across different agents using Java 21 virtual threads

**Key Methods:**
- `executeParallel(List<TaskRequest>)` - Execute all tasks concurrently
- `executeParallelWithLimit(int maxConcurrency)` - Limit concurrent executions
- `executeBatched(int batchSize)` - Process in batches
- `executeAndAggregate(Function<List<TaskResult>, R>)` - Execute and aggregate results

**Example Usage:**
```java
List<TaskRequest> tasks = List.of(
    new TaskRequest("Developer", "Implement feature X", null),
    new TaskRequest("QA Engineer", "Create test plan", null),
    new TaskRequest("DevOps", "Setup CI/CD", null)
);

List<TaskResult> results = parallelExecutor.executeParallel(tasks);
```

**Features:**
- Virtual thread-based execution (lightweight concurrency)
- Configurable concurrency limits
- Batch processing support
- Result aggregation
- Automatic timeout handling (5 minutes)
- Error handling with graceful degradation

**Tests:** `src/test/java/com/rolemanager/orchestration/ParallelAgentExecutorTest.java` (9 tests)

---

### 2. Agent Chaining/Workflows

**Location:** `src/main/java/com/rolemanager/orchestration/WorkflowEngine.java`

**Purpose:** Orchestrate complex multi-agent workflows with dependencies using DAG (Directed Acyclic Graph)

**Supported Patterns:**

#### Sequential Chain
```java
List<WorkflowStep> steps = List.of(
    new WorkflowStep("design", "Architect", "Design system"),
    new WorkflowStep("implement", "Developer", "Implement design"),
    new WorkflowStep("test", "QA", "Test implementation")
);

TaskResult result = engine.executeChain(steps);
```

#### DAG Workflow
```java
Workflow workflow = new Workflow("feature-development", List.of(
    new WorkflowStep("gather", "Analyst", "Gather requirements", List.of()),
    new WorkflowStep("designUI", "UX Designer", "Design UI", List.of("gather")),
    new WorkflowStep("designAPI", "Architect", "Design API", List.of("gather")),
    new WorkflowStep("implement", "Developer", "Implement", List.of("designUI", "designAPI")),
    new WorkflowStep("test", "QA", "Test", List.of("implement"))
));

WorkflowResult result = engine.executeWorkflow(workflow);
```

#### Fan-Out/Fan-In
```java
TaskResult result = engine.executeFanOutFanIn(
    "Review code from different perspectives",
    List.of("Security Expert", "Performance Expert", "UX Expert"),
    "Tech Lead"  // Aggregator
);
```

**Features:**
- Topological sorting using Kahn's algorithm
- Automatic dependency resolution
- Cycle detection
- Context passing between steps
- Early termination on failure
- Parallel step execution where possible

**Tests:** `src/test/java/com/rolemanager/orchestration/WorkflowEngineTest.java` (10 tests)

---

### 3. Prompt Template Management

**Location:** `src/main/java/com/rolemanager/template/PromptTemplateEngine.java`

**Purpose:** Dynamic prompt generation with Mustache-like template syntax

**Template Syntax:**

#### Variables
```
Hello {{name}}, your role is {{role}}.
```

#### Conditionals
```
{{#if premium}}
You are a premium user with extra features.
{{/if}}
```

#### Loops
```
Requirements:{{#each requirements}}
- {{item}}{{/each}}
```

**Example Usage:**
```java
// Register template
PromptTemplate template = new PromptTemplate(
    "code-review",
    """
    You are a {{role}}.

    Task: {{task}}

    {{#if context}}
    Context: {{context}}
    {{/if}}

    Requirements:{{#each requirements}}
    - {{item}}{{/each}}
    """,
    Map.of()
);
engine.registerTemplate("code-review", template);

// Render with builder
String prompt = engine.template("code-review")
    .with("role", "Senior Developer")
    .with("task", "Review pull request #123")
    .with("context", "Security-critical code")
    .with("requirements", List.of("Check for SQL injection", "Verify input validation"))
    .build();
```

**Features:**
- Variable substitution: `{{variable}}`
- Conditional blocks: `{{#if condition}}...{{/if}}`
- Loop blocks: `{{#each list}}...{{/each}}`
- Nested structures support
- Builder pattern for easy usage
- Template registration and management
- Missing variable handling

**Tests:** `src/test/java/com/rolemanager/template/PromptTemplateEngineTest.java` (16 tests)

---

### 4. Model Fine-Tuning Integration

**Location:** `src/main/java/com/rolemanager/finetuning/FineTuningService.java`

**Purpose:** Collect training data from LLM interactions and export in various formats

**Supported Formats:**
- **OpenAI** - Messages format for GPT fine-tuning
- **Anthropic** - Prompt/completion format for Claude fine-tuning
- **JSONL** - Generic format with metadata

**Example Usage:**
```java
// Create dataset
String datasetId = service.createDataset("coding-assistance", DatasetFormat.OPENAI);

// Add examples manually
service.addExample(
    datasetId,
    "Explain Java streams",
    "Java streams provide a functional approach to processing collections...",
    Map.of("category", "java", "difficulty", "intermediate")
);

// Capture high-quality interactions (rating >= 4)
LLMResponse response = llmProvider.generate(prompt);
service.captureInteraction(datasetId, prompt, response, 5);

// Export dataset
Path outputPath = Path.of("training-data/coding-assistance.jsonl");
int count = service.exportDataset(datasetId, outputPath);

// Get statistics
DatasetStats stats = service.getStats(datasetId);
System.out.printf("Dataset: %s, Examples: %d, Total Tokens: %d%n",
    stats.name(), stats.totalExamples(), stats.totalTokens());
```

**Export Formats:**

#### OpenAI Format
```json
{"messages": [
  {"role": "user", "content": "What is Spring Boot?"},
  {"role": "assistant", "content": "Spring Boot is a framework..."}
]}
```

#### Anthropic Format
```json
{"prompt": "What is Spring Boot?", "completion": "Spring Boot is a framework..."}
```

#### JSONL Format
```json
{
  "prompt": "What is Spring Boot?",
  "completion": "Spring Boot is a framework...",
  "metadata": {"category": "java", "quality": 5}
}
```

**Features:**
- Dataset creation and management
- Quality-based interaction capture (rating threshold)
- Multiple export formats
- Token estimation
- Dataset statistics
- Metadata preservation (JSONL format)
- CRUD operations (create, read, update, delete)

**Tests:** `src/test/java/com/rolemanager/finetuning/FineTuningServiceTest.java` (16 tests)

---

## Integration with Existing System

All four features integrate seamlessly with the existing Role Manager App infrastructure:

- **AgentRegistry** - Used by ParallelAgentExecutor to look up agent configurations
- **LLMProvider** - Used for executing tasks and capturing responses
- **TaskRequest/TaskResult** - Reused existing model classes
- **UsageInfo** - Token tracking for fine-tuning datasets

## Test Coverage

**Total Tests: 51**

| Feature | Test Class | Tests | Coverage |
|---------|------------|-------|----------|
| Parallel Execution | ParallelAgentExecutorTest | 9 | Core functionality, error handling, concurrency |
| Workflows | WorkflowEngineTest | 10 | DAG, chains, fan-out/fan-in, cycle detection |
| Templates | PromptTemplateEngineTest | 16 | Variables, conditionals, loops, edge cases |
| Fine-tuning | FineTuningServiceTest | 16 | CRUD, export formats, statistics, metadata |

All tests use Mockito for mocking dependencies and AssertJ for fluent assertions.

## Usage Patterns

### Pattern 1: Parallel Code Review
```java
List<TaskRequest> reviews = List.of(
    new TaskRequest("Security Expert", "Review for vulnerabilities", context),
    new TaskRequest("Performance Expert", "Review for bottlenecks", context),
    new TaskRequest("UX Expert", "Review user experience", context)
);

List<TaskResult> results = parallelExecutor.executeParallel(reviews);
```

### Pattern 2: Sequential Development Workflow
```java
Workflow devWorkflow = new Workflow("feature-dev", List.of(
    new WorkflowStep("requirements", "Analyst", "Gather requirements"),
    new WorkflowStep("design", "Architect", "Design solution", List.of("requirements")),
    new WorkflowStep("implement", "Developer", "Implement", List.of("design")),
    new WorkflowStep("test", "QA", "Test", List.of("implement")),
    new WorkflowStep("deploy", "DevOps", "Deploy", List.of("test"))
));

WorkflowResult result = engine.executeWorkflow(devWorkflow);
```

### Pattern 3: Template-Based Task Generation
```java
PromptTemplate taskTemplate = new PromptTemplate(
    "agent-task",
    """
    You are a {{role}}.

    Task: {{task}}

    {{#if previousOutput}}
    Previous step output: {{previousOutput}}
    {{/if}}

    Please complete this task following best practices.
    """,
    Map.of()
);

engine.registerTemplate("agent-task", taskTemplate);

String prompt = engine.template("agent-task")
    .with("role", "Developer")
    .with("task", "Implement authentication")
    .with("previousOutput", designResult.output())
    .build();
```

### Pattern 4: Continuous Fine-Tuning Data Collection
```java
// Setup dataset
String datasetId = fineTuningService.createDataset("production-qa", DatasetFormat.OPENAI);

// During production usage
void handleUserQuery(String query) {
    LLMResponse response = llmProvider.generate(query);

    // Present to user and collect feedback
    int rating = getUserRating();

    // Automatically capture high-quality interactions
    fineTuningService.captureInteraction(datasetId, query, response, rating);
}

// Periodically export for fine-tuning
scheduler.scheduleAtFixedRate(() -> {
    DatasetStats stats = fineTuningService.getStats(datasetId);
    if (stats.totalExamples() >= 1000) {
        Path path = Path.of("training-data/batch-" + Instant.now() + ".jsonl");
        fineTuningService.exportDataset(datasetId, path);
        // Upload to fine-tuning platform
    }
}, 0, 1, TimeUnit.DAYS);
```

## Performance Considerations

### Parallel Execution
- Uses Java 21 virtual threads (Project Loom) for lightweight concurrency
- Can handle thousands of concurrent tasks with minimal overhead
- 5-minute timeout per task prevents hanging operations
- Batching support for rate-limited APIs

### Workflows
- Topological sorting is O(V + E) where V = steps, E = dependencies
- Parallel execution of independent steps
- Context passing adds minimal overhead
- Early termination on failure saves resources

### Templates
- Regex-based processing is cached per render
- O(n) complexity where n = template length
- Minimal memory overhead
- Template registration is one-time cost

### Fine-Tuning
- Token estimation is approximate (4 chars/token)
- In-memory dataset storage (consider persistent storage for large datasets)
- Export is streaming (doesn't load all examples in memory)
- Statistics calculation is O(n) where n = example count

## Future Enhancements

Potential improvements for future iterations:

1. **Parallel Execution:**
   - Priority-based task scheduling
   - Resource pooling and limits
   - Metrics and monitoring integration

2. **Workflows:**
   - Workflow persistence and resumption
   - Visual workflow designer
   - Dynamic workflow modification

3. **Templates:**
   - Template validation and linting
   - Template inheritance
   - Custom function support

4. **Fine-Tuning:**
   - Persistent storage (database)
   - Automatic dataset splitting (train/val/test)
   - Quality metrics and analysis
   - Direct API integration with fine-tuning platforms

---

## References

- **Java Virtual Threads:** https://openjdk.org/jeps/444
- **OpenAI Fine-tuning:** https://platform.openai.com/docs/guides/fine-tuning
- **Anthropic Fine-tuning:** https://docs.anthropic.com/claude/docs/fine-tuning
- **Mustache Templates:** https://mustache.github.io/
- **DAG Algorithms:** Introduction to Algorithms (CLRS)

---

*Last Updated: 2025-10-18*
