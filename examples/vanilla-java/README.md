# Vanilla Java Example

This example demonstrates using `ade-agent-platform-core` **without any framework** (no Spring, Quarkus, or Micronaut).

## Overview

The platform is truly framework-agnostic. All core classes are POJOs that can be instantiated directly:

- `AgentRegistry` - Manage agents
- `DomainLoader` - Load agents from YAML
- `LocalStorageProvider` - Store documents
- `InMemoryMemoryProvider` - Vector memory
- Provider implementations - All framework-agnostic

## Dependencies

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-core</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

**No Spring dependencies required!**

## Running the Example

```bash
# Compile
javac -cp "path/to/ade-agent-platform-core-0.2.0-SNAPSHOT.jar:..." VanillaJavaExample.java

# Run
java -cp ".:path/to/ade-agent-platform-core-0.2.0-SNAPSHOT.jar:..." dev.adeengineer.examples.VanillaJavaExample
```

## What This Example Shows

1. **Direct instantiation** - Create providers as POJOs
2. **Programmatic configuration** - Build agents without YAML
3. **Domain loading** - Load agents from files (optional)
4. **Zero framework overhead** - Pure Java, no DI container

## When to Use This Approach

- **Embedded systems** - Minimal dependencies
- **CLI tools** - No server needed
- **Batch processing** - Lightweight workers
- **Custom frameworks** - Integrate with your own DI
- **Learning** - Understand platform internals

## Comparison with Spring Boot

| Feature | Vanilla Java | Spring Boot |
|---------|--------------|-------------|
| Dependencies | Core only (~5MB) | Core + Spring (~50MB) |
| Startup time | Instant | 2-3 seconds |
| Memory | Minimal | Higher overhead |
| Auto-config | Manual setup | Automatic beans |
| REST API | Manual | Included |
| CLI | Manual | Spring Shell |

Choose based on your needs!
