# Coding Standards - Role Manager App

**Date:** 2025-10-17

---

## Java Style

- **Style Guide**: Google Java Style Guide
- **Line Length**: 120 characters max
- **Indentation**: 4 spaces (no tabs)
- **Naming**: camelCase for variables/methods, PascalCase for classes

---

## Code Organization

```java
// Order: static imports → regular imports
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.springframework.stereotype.Component;

// Order: constants → fields → constructor → public methods → private methods
@Component
public class ExampleClass {
    private static final int CONSTANT = 10;

    private final Dependency dep;

    public ExampleClass(Dependency dep) {
        this.dep = dep;
    }

    public void publicMethod() { }

    private void privateHelper() { }
}
```

---

## Lombok Usage

```java
@Data
@Builder
public class TaskResult {
    private String taskId;
    private String output;
}
```

---

## Testing Standards

- **Coverage**: Minimum 80%
- **Naming**: `methodName_shouldExpectedBehavior_whenCondition()`
- **AAA Pattern**: Arrange, Act, Assert

```java
@Test
void executeTask_shouldReturnFormattedOutput_whenValidRole() {
    // Arrange
    AgentRole agent = new DeveloperAgent(...);

    // Act
    String result = agent.executeTask("task", Map.of());

    // Assert
    assertThat(result).isNotEmpty();
}
```

---

*Last Updated: 2025-10-17*
