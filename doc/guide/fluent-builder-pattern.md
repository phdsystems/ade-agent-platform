# Fluent Builder Pattern Guide

**Date:** 2025-10-21
**Version:** 1.0

## TL;DR

**Fluent Builders** make object creation readable and maintainable through method chaining. **Key benefit**: Self-documenting code that reads like English. **When to use**: Complex objects with multiple parameters, especially when some are optional. **Example**: `builder().name("Dev").temperature(0.7).build()` instead of `new Config("Dev", null, null, 0.7, null)`.

---

## Table of Contents

1. [What Are Fluent Builders?](#what-are-fluent-builders)
2. [Problem They Solve](#problem-they-solve)
3. [How They Work](#how-they-work)
4. [Benefits](#benefits)
5. [When to Use](#when-to-use)
6. [Implementation Pattern](#implementation-pattern)
7. [Examples](#examples)
8. [Best Practices](#best-practices)
9. [References](#references)

---

## What Are Fluent Builders?

Fluent builders are a **design pattern** that makes creating objects more readable and maintainable by using **method chaining**.

Instead of passing many parameters to a constructor (which is hard to read), you call methods that return "this" so you can chain them together, creating a fluent, English-like syntax.

---

## Problem They Solve

### Traditional Approach (Constructor with Many Parameters)

```java
// Hard to read - what does each parameter mean?
AgentConfig config = new AgentConfig(
    "Developer",                           // name
    "Software development agent",          // description
    List.of("coding", "testing"),         // capabilities
    0.7,                                  // temperature
    1000,                                 // maxTokens
    "You are a {role}. Task: {task}",    // promptTemplate
    "technical"                           // outputFormat
);
```

**Problems:**
- ❌ Have to remember parameter order
- ❌ Can't skip optional parameters without passing null
- ❌ Hard to read - what does 0.7 or 1000 mean?
- ❌ Easy to make mistakes (swap parameters of same type)
- ❌ Adding new parameters breaks all existing code
- ❌ IDE can't help much with unnamed parameters

---

### Fluent Builder Approach

```java
// Self-documenting and easy to read
AgentConfig config = AgentConfigBuilder.builder()
    .name("Developer")
    .description("Software development agent")
    .capabilities("coding", "testing", "debugging")
    .temperature(0.7)
    .maxTokens(1000)
    .promptTemplate("You are a {role}. Task: {task}")
    .outputFormat("technical")
    .build();
```

**Benefits:**
- ✅ Self-documenting - each parameter is clearly labeled
- ✅ No need to remember order
- ✅ Can skip optional parameters (builder provides defaults)
- ✅ Easy to read and maintain
- ✅ IDE auto-completion helps discover available options
- ✅ Adding new parameters doesn't break existing code
- ✅ Type-safe at compile time

---

## How They Work

### Core Mechanism: Returning `this`

Each builder method returns the builder object itself, enabling method chaining:

```java
public class AgentConfigBuilder {
    private String name = "TestAgent";        // Default value
    private double temperature = 0.7;         // Default value
    private int maxTokens = 500;              // Default value

    // Each method returns "this" for chaining
    public AgentConfigBuilder name(String name) {
        this.name = name;
        return this;  // ← Returns itself for chaining
    }

    public AgentConfigBuilder temperature(double temperature) {
        this.temperature = temperature;
        return this;  // ← Returns itself for chaining
    }

    public AgentConfigBuilder maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;  // ← Returns itself for chaining
    }

    // Final method builds the actual object
    public AgentConfig build() {
        return new AgentConfig(name, temperature, maxTokens, ...);
    }
}
```

### Method Chaining Flow

```java
AgentConfigBuilder.builder()  // Returns: AgentConfigBuilder instance
    .name("Dev")               // Returns: same AgentConfigBuilder instance
    .temperature(0.7)          // Returns: same AgentConfigBuilder instance
    .maxTokens(1000)           // Returns: same AgentConfigBuilder instance
    .build();                  // Returns: final AgentConfig object
```

---

## Benefits

### 1. **Readability** (Self-Documenting Code)

```java
// Before: What does each parameter mean?
new TaskRequest("Developer", "Write tests", Map.of("priority", "high"));

// After: Clear and obvious
TaskRequestBuilder.builder()
    .agentName("Developer")
    .task("Write tests")
    .context("priority", "high")
    .build();
```

### 2. **Flexibility** (Optional Parameters)

```java
// Before: Must pass nulls for optional parameters
new AgentConfig("Dev", "Description", null, 0.7, null, null, "technical");

// After: Only specify what you need
AgentConfigBuilder.builder()
    .name("Dev")
    .description("Description")
    .temperature(0.7)
    .outputFormat("technical")
    .build();  // Other params use sensible defaults
```

### 3. **Maintainability** (Easy to Extend)

```java
// Adding new optional parameters doesn't break existing code
AgentConfigBuilder.builder()
    .name("Dev")
    .build();  // Still works even if we add 10 new optional parameters
```

### 4. **Type Safety** (Compile-Time Checking)

```java
// Compiler catches type errors
AgentConfigBuilder.builder()
    .temperature("high")  // ← Compile error: expects double, not String
    .build();
```

### 5. **IDE Support** (Auto-Completion)

Type `.` after builder() and your IDE shows all available options with documentation.

---

## When to Use

### ✅ Use Fluent Builders When:

1. **Object has many parameters** (3+ parameters)
2. **Some parameters are optional** (not all are required)
3. **Parameters have same type** (easy to swap accidentally)
4. **Object creation is complex** (validation, derived values)
5. **Readability matters** (test code, configuration)

### ❌ Don't Use Fluent Builders When:

1. **Simple objects** (1-2 required parameters, no optional)
2. **Immutable value objects** (records/data classes work better)
3. **Performance-critical code** (extra object allocation)
4. **Objects created in tight loops** (use direct construction)

---

## Implementation Pattern

### Standard Fluent Builder Template

```java
public class ObjectBuilder {

    // 1. Private fields with defaults
    private String requiredField;
    private String optionalField = "default";
    private int numericField = 0;

    // 2. Private constructor (force use of builder() method)
    private ObjectBuilder() {}

    // 3. Static factory method
    public static ObjectBuilder builder() {
        return new ObjectBuilder();
    }

    // 4. Setter methods that return 'this'
    public ObjectBuilder requiredField(String value) {
        this.requiredField = value;
        return this;
    }

    public ObjectBuilder optionalField(String value) {
        this.optionalField = value;
        return this;
    }

    public ObjectBuilder numericField(int value) {
        this.numericField = value;
        return this;
    }

    // 5. Build method creates final object
    public MyObject build() {
        // Optional: validation
        if (requiredField == null) {
            throw new IllegalStateException("requiredField is required");
        }

        return new MyObject(requiredField, optionalField, numericField);
    }
}
```

---

## Examples

### Example 1: AgentConfigBuilder (from AgentUnit)

```java
AgentConfig config = AgentConfigBuilder.builder()
    .name("Developer")
    .description("Software development agent")
    .capabilities("coding", "testing", "debugging")
    .temperature(0.7)
    .maxTokens(1000)
    .promptTemplate("You are a {role}. Task: {task}")
    .outputFormat("technical")
    .build();
```

### Example 2: TaskRequestBuilder (from AgentUnit)

```java
TaskRequest request = TaskRequestBuilder.builder()
    .agentName("Developer")
    .task("Write unit tests for UserService")
    .context("priority", "high")
    .context("deadline", "2024-01-15")
    .build();
```

### Example 3: TaskResultBuilder (from AgentUnit)

```java
// Success result
TaskResult success = TaskResultBuilder.success()
    .agentName("Developer")
    .task("Write tests")
    .output("Tests written successfully")
    .metadata("totalTokens", 150)
    .durationMs(1000L)
    .build();

// Failure result
TaskResult failure = TaskResultBuilder.failure()
    .agentName("Developer")
    .task("Invalid task")
    .errorMessage("Task validation failed")
    .build();
```

### Example 4: Real-World Analogy (Sandwich)

Think of it like **ordering a custom sandwich**:

```java
// Bad way (constructor)
Sandwich s = new Sandwich("wheat", "turkey", "lettuce", "tomato", "mayo", "no cheese", "toasted");
// What order? Easy to mess up!

// Good way (fluent builder)
Sandwich s = SandwichBuilder.builder()
    .bread("wheat")
    .meat("turkey")
    .addVeggies("lettuce", "tomato")
    .sauce("mayo")
    .toasted(true)
    .build();
// Clear what each choice is!
```

---

## Best Practices

### 1. Provide Sensible Defaults

```java
private String name = "DefaultAgent";  // Default value
private double temperature = 0.7;      // Reasonable default

// User can override if needed
builder().name("CustomAgent").build();

// Or use defaults
builder().build();  // Uses "DefaultAgent" and 0.7
```

### 2. Validate in build() Method

```java
public AgentConfig build() {
    if (name == null || name.isEmpty()) {
        throw new IllegalStateException("name is required");
    }
    if (temperature < 0.0 || temperature > 1.0) {
        throw new IllegalStateException("temperature must be between 0.0 and 1.0");
    }
    return new AgentConfig(name, temperature, ...);
}
```

### 3. Support Multiple Ways to Set Values

```java
// Single value
.capability("coding")

// Multiple values (varargs)
.capabilities("coding", "testing", "debugging")

// List of values
.capabilities(List.of("coding", "testing"))

// Add to existing
.addCapability("new-skill")
```

### 4. Use Clear Method Names

```java
// ✅ Good: Clear intent
.temperature(0.7)
.maxTokens(1000)
.enabled(true)

// ❌ Bad: Unclear
.temp(0.7)
.max(1000)
.flag(true)
```

### 5. Private Constructor + Static Factory

```java
// Force users to use builder() method
private ObjectBuilder() {}

public static ObjectBuilder builder() {
    return new ObjectBuilder();
}

// ✅ Usage
ObjectBuilder.builder().build();

// ❌ Can't do this
new ObjectBuilder();  // Compile error
```

---

## References

### Books
- **Effective Java (3rd Edition)** by Joshua Bloch
  - Item 2: "Consider a builder when faced with many constructor parameters"

### Articles
- [Gang of Four Design Patterns](https://en.wikipedia.org/wiki/Design_Patterns) - Builder Pattern
- [Martin Fowler - Fluent Interface](https://martinfowler.com/bliki/FluentInterface.html)

### Real-World Examples
- **Lombok @Builder** - Generates fluent builders automatically
- **StringBuilder** in Java - Classic example of fluent API
- **AssertJ** - Fluent assertion library
- **MockMvc** in Spring - Fluent API for testing REST endpoints

### AgentUnit Implementation
- `ade-agent-platform-agentunit/src/main/java/dev/adeengineer/platform/test/builder/`
  - AgentConfigBuilder.java
  - TaskRequestBuilder.java
  - TaskResultBuilder.java

---

## Summary

**Fluent Builders** = A pattern that makes object creation:
- More **readable** (self-documenting)
- More **flexible** (skip optional params)
- Less **error-prone** (no parameter order confusion)
- More **maintainable** (easy to add new options)

That's why they're called "fluent" - the code reads like a **fluent sentence** describing what you're building!

**Key Principle**: Return `this` from each setter method to enable chaining.

**When to use**: Complex objects with 3+ parameters, especially when some are optional.

**AgentUnit provides**: 3 fluent builders for test data (AgentConfig, TaskRequest, TaskResult).

---

*Last Updated: 2025-10-21*
