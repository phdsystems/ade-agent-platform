# Data Model - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## Domain Model

### AgentRole Interface
```java
public interface AgentRole {
    String getRoleName();
    String getDescription();
    List<String> getCapabilities();
    LLMResponse executeTask(String task, Map<String, Object> context);
    String formatOutput(LLMResponse response);
}
```

### TaskResult Record
```java
public record TaskResult(
    String taskId,
    String roleName,
    String task,
    String output,
    LLMUsage usage,
    Instant timestamp,
    TaskStatus status
) {}
```

### RoleConfig (YAML)
```yaml
role: Software Developer
description: "Technical code reviews and guidance"
capabilities:
  - Code review
  - Debugging
  - Refactoring
temperature: 0.7
max_tokens: 4096
prompt_template: "You are a Software Developer..."
```

---

*Last Updated: 2025-10-17*
