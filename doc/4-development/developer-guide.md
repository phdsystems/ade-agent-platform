# Developer Guide - ade Agent Platform

**Date:** 2025-10-20
**Version:** 2.0

---

## TL;DR

**Setup**: Clone → `mvn install` → configure LLM provider → run. **Add new agent**: Create YAML config in domain directory → restart (no code changes). **Add new domain**: Create domain directory with agents/ → add domain-config.yaml → load via CLI. **Testing**: Unit tests (JUnit 5 + Mockito), Integration tests (E2E with real LLM), Load tests (Gatling). **Contributing**: Run `mvn spotless:apply` before commit, follow Conventional Commits, write tests, update docs.

---

## Quick Start

### Prerequisites

- **Java 21+** (with preview features enabled)
- **Maven 3.9+**
- **Git**
- **LLM Provider** (choose one):
  - Ollama with qwen2.5:0.5b (local, free)
  - Anthropic Claude API key
  - OpenAI GPT API key

### Installation

```bash
# Clone the repository
git clone https://github.com/phdsystems/software-engineer.git
cd software-engineer/ade-agent-platform

# Build (skip tests for faster initial setup)
mvn clean install -DskipTests
```

### Configuration

**Option 1: Ollama (Local, Free)**
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull model
ollama pull qwen2.5:0.5b

# Start Ollama server (runs on http://localhost:11434)
ollama serve
```

**Option 2: Anthropic Claude**
```bash
export ANTHROPIC_API_KEY=sk-ant-xxx
```

**Option 3: OpenAI GPT**
```bash
export OPENAI_API_KEY=sk-xxx
```

### Run the Platform

```bash
# Using Maven
mvn spring-boot:run

# Or using the built JAR
java -jar target/ade-agent-platform-0.2.0-SNAPSHOT.jar
```

### First Commands

**Spring Shell CLI:**
```bash
# List all available agents
shell:> list-agents

# List all loaded domains
shell:> list-domains

# Execute a task with an agent
shell:> execute --agent "SoftwareDeveloper" --task "Explain dependency injection"

# Describe an agent's capabilities
shell:> describe-agent "QAEngineer"

# Load a custom domain
shell:> load-domain /path/to/custom-domain
```

**REST API:**
```bash
# List all agents (via REST API)
curl http://localhost:8080/api/agents

# Execute a task
curl -X POST http://localhost:8080/api/tasks/execute \
  -H "Content-Type: application/json" \
  -d '{
    "agentName": "SoftwareDeveloper",
    "task": "Review this code for security issues",
    "context": {"language": "Java"}
  }'
```

For detailed setup instructions, see [local-setup-guide.md](local-setup-guide.md).

---

## Adding a New Agent (YAML-Based - No Code Required!)

The ade Agent Platform uses **YAML-driven configuration** for agents. You can add new agents without writing any Java code.

### Step 1: Create Agent Configuration File

Create a YAML file in your domain's `agents/` directory:

```yaml
# domains/software-engineer/agents/data-scientist.yaml
name: DataScientist
description: "Expert in data analysis, machine learning, and statistical modeling"
capabilities:
  - "Data cleaning and preprocessing"
  - "Statistical analysis and hypothesis testing"
  - "Machine learning model development"
  - "Data visualization and reporting"
  - "Python/R programming for data science"

llm:
  provider: "ollama"  # or "anthropic", "openai"
  model: "qwen2.5:0.5b"
  temperature: 0.7
  max_tokens: 2048

prompt_template: |
  You are an expert Data Scientist with deep knowledge in:
  - Statistical analysis and hypothesis testing
  - Machine learning algorithms (supervised and unsupervised)
  - Python libraries: pandas, numpy, scikit-learn, matplotlib
  - R programming and statistical modeling
  - Data visualization best practices

  Task: {task}

  Context: {context}

  Provide clear, technically accurate analysis with:
  1. Step-by-step methodology
  2. Code examples when applicable
  3. Statistical rigor
  4. Visualization recommendations

output_format: "technical"  # or "business", "raw"
enabled: true
```

### Step 2: Restart the Platform

```bash
# Stop the running platform (Ctrl+C)
# Restart
mvn spring-boot:run
```

The platform will automatically discover and register your new agent!

### Step 3: Verify Agent Loaded

```bash
shell:> list-agents
# Should show DataScientist in the list

shell:> describe-agent "DataScientist"
# Shows capabilities and configuration
```

### Step 4: Test Your Agent

```bash
shell:> execute --agent "DataScientist" --task "Explain linear regression"
```

### Alternative: Add Agent via API (Runtime)

You can also register agents dynamically without restart:

```bash
curl -X POST http://localhost:8080/api/agents/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DataScientist",
    "description": "Expert in data analysis",
    "capabilities": ["Statistical analysis", "ML modeling"],
    "llm": {
      "provider": "ollama",
      "model": "qwen2.5:0.5b",
      "temperature": 0.7,
      "maxTokens": 2048
    },
    "promptTemplate": "You are a Data Scientist...",
    "outputFormat": "technical",
    "enabled": true
  }'
```

---

## Project Structure

```
ade-agent-platform/
├── src/
│   ├── main/
│   │   ├── java/adeengineer/dev/platform/
│   │   │   ├── AdePlatformApplication.java  # Spring Boot main class
│   │   │   ├── api/                         # REST API controllers
│   │   │   │   └── AgentController.java
│   │   │   ├── cli/                         # Spring Shell CLI commands
│   │   │   │   └── AgentCommands.java
│   │   │   ├── config/                      # Spring configuration
│   │   │   │   ├── AppConfig.java           # Main app config
│   │   │   │   ├── AgentConfigLoader.java   # YAML config loader
│   │   │   │   └── package-info.java
│   │   │   ├── core/                        # Core platform components
│   │   │   │   ├── AgentRegistry.java       # Agent registration
│   │   │   │   ├── DomainManager.java       # Domain management
│   │   │   │   ├── DomainLoader.java        # Domain loading
│   │   │   │   ├── BaseAgent.java           # Base agent implementation
│   │   │   │   ├── ConfigurableAgent.java   # YAML-based agents
│   │   │   │   └── OutputFormatterRegistry.java
│   │   │   ├── formatters/                  # Output formatters
│   │   │   │   ├── TechnicalOutputFormatter.java
│   │   │   │   ├── BusinessOutputFormatter.java
│   │   │   │   └── RawOutputFormatter.java
│   │   │   ├── model/                       # Data models
│   │   │   │   └── DomainConfig.java
│   │   │   ├── orchestration/               # Multi-agent coordination
│   │   │   │   └── AgentOrchestrator.java
│   │   │   ├── template/                    # Agent templates
│   │   │   └── util/                        # Utilities
│   │   │       └── TaskResultUtils.java
│   │   └── resources/
│   │       ├── application.yml              # Main configuration
│   │       ├── application-dev.yml          # Dev profile
│   │       ├── application-prod.yml         # Production profile
│   │       └── banner.txt                   # ASCII art banner
│   └── test/
│       ├── java/adeengineer/dev/platform/
│       │   ├── core/                        # Unit tests
│       │   │   ├── AgentRegistryTest.java
│       │   │   ├── DomainManagerTest.java
│       │   │   └── BaseAgentTest.java
│       │   └── integration/                 # Integration tests
│       │       └── AgentE2ETest.java
│       └── resources/
│           └── test-agent-config.yaml       # Test configurations
├── domains/                                  # Domain definitions (external)
│   └── software-engineer/
│       ├── domain-config.yaml               # Domain metadata
│       └── agents/                          # Agent YAML configs
│           ├── software-developer.yaml
│           ├── qa-engineer.yaml
│           ├── devops-engineer.yaml
│           └── security-engineer.yaml
├── doc/                                      # SDLC documentation
│   ├── 1-planning/                          # Requirements
│   ├── 2-analysis/                          # Use cases
│   ├── 3-design/                            # Architecture & ADRs
│   │   └── decisions/                       # Architecture Decision Records
│   ├── 4-development/                       # Developer guides
│   ├── 5-testing/                           # Test plans
│   ├── 6-deployment/                        # Deployment guides
│   └── 7-maintenance/                       # Operations
├── CONTRIBUTING.md                          # Contributing guidelines
├── README.md                                # Project overview
└── pom.xml                                  # Maven configuration
```

### Key Directories

| Directory | Purpose |
|-----------|---------|
| `src/main/java/.../core/` | Core platform logic (agent registry, domain management) |
| `src/main/java/.../api/` | REST API endpoints |
| `src/main/java/.../cli/` | Spring Shell CLI commands |
| `src/main/java/.../config/` | Spring configuration & YAML loaders |
| `src/main/java/.../formatters/` | Output formatting strategies |
| `domains/` | External domain definitions (YAML-based) |
| `doc/` | SDLC documentation (7 phases) |
| `doc/3-design/decisions/` | Architecture Decision Records (ADRs) |

---

## Testing

The platform has three levels of testing: Unit, Integration (E2E), and Load tests.

### Unit Tests (Fast, Mocked Dependencies)

Unit tests use **JUnit 5** and **Mockito** for fast, isolated testing:

```java
@ExtendWith(MockitoExtension.class)
class AgentRegistryTest {

    @Mock
    private LlmOrchestrator mockLlmOrchestrator;

    @InjectMocks
    private AgentRegistry agentRegistry;

    @Test
    void registerAgent_shouldAddAgentToRegistry() {
        // Arrange
        AgentConfig config = AgentConfig.builder()
            .name("TestAgent")
            .description("Test agent")
            .build();

        // Act
        agentRegistry.registerAgent(config);

        // Assert
        assertThat(agentRegistry.getAgent("TestAgent")).isNotNull();
        assertThat(agentRegistry.listAgents()).hasSize(1);
    }

    @Test
    void getAgent_shouldReturnNull_whenAgentNotFound() {
        // Act & Assert
        assertThat(agentRegistry.getAgent("NonExistent")).isNull();
    }
}
```

**Run unit tests:**
```bash
# All unit tests
mvn test

# Specific test class
mvn test -Dtest=AgentRegistryTest

# Specific test method
mvn test -Dtest=AgentRegistryTest#registerAgent_shouldAddAgentToRegistry

# With coverage report
mvn test jacoco:report
# View: target/site/jacoco/index.html
```

### Integration Tests (Real LLM, Slower)

Integration tests (suffix: `*E2ETest.java`) use **real LLM providers** and test end-to-end workflows:

```java
@SpringBootTest
@ActiveProfiles("test")
class AgentE2ETest {

    @Autowired
    private DomainManager domainManager;

    @Test
    void executeTask_shouldReturnResponse_withRealLLM() {
        // Arrange
        String task = "Explain what is dependency injection";
        Map<String, Object> context = Map.of("language", "Java");

        // Act
        TaskResult result = domainManager.executeTask(
            "SoftwareDeveloper",
            task,
            context
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.output()).isNotEmpty();
        assertThat(result.success()).isTrue();
    }
}
```

**Run integration tests:**
```bash
# Run all E2E tests (requires LLM provider)
mvn verify

# Run only integration tests
mvn failsafe:integration-test

# Skip integration tests
mvn install -DskipITs
```

**Prerequisites for E2E tests:**
- Ollama running with `qwen2.5:0.5b` model, OR
- Valid API keys for Anthropic/OpenAI set as environment variables

### Load Tests (Performance Testing)

Load tests use **Gatling** to simulate high concurrency:

```bash
# Run Gatling load tests
mvn gatling:test

# View report (auto-opens in browser)
# Or: target/gatling/results/*/index.html
```

**Load test scenarios:**
- Concurrent agent execution (50-100 users)
- Multi-agent orchestration stress test
- Cache hit rate measurement
- Circuit breaker behavior under load

For detailed load testing setup, see [doc/5-testing/performance-optimization-tests.md](../5-testing/performance-optimization-tests.md).

### Test Coverage Requirements

- **Minimum**: 50% line coverage (enforced by JaCoCo)
- **Target**: 70% for new features
- **Goal**: 80% for critical components (core/, config/)

**Check coverage:**
```bash
mvn verify
# Report: target/site/jacoco/index.html
```

### Test Naming Convention

Use descriptive test names with BDD-style format:

```
methodName_shouldExpectedBehavior_whenCondition()
```

**Examples:**
- `registerAgent_shouldAddToRegistry_whenValidConfig()`
- `getAgent_shouldReturnNull_whenAgentNotFound()`
- `executeTask_shouldThrowException_whenAgentDisabled()`

### Testing Best Practices

✅ **DO:**
- Write unit tests for all public methods
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Use `@SpringBootTest` for integration tests
- Mock external dependencies (LLM providers, databases)
- Use AssertJ for fluent assertions
- Test edge cases and error conditions
- Keep tests fast (unit tests < 100ms)

❌ **DON'T:**
- Commit commented-out tests
- Skip failing tests (fix them!)
- Write tests that depend on test execution order
- Use `Thread.sleep()` in tests
- Test implementation details (test behavior, not internals)

---

## Code Formatting and Quality

### Automated Code Formatting (Spotless)

**ALWAYS** format code before committing:

```bash
# Format all code
mvn spotless:apply

# Check formatting (CI uses this)
mvn spotless:check
```

**What Spotless Formats:**
- Java source files (Google Java Format - AOSP style, 100-char lines)
- Import ordering (java → javax → jakarta → org → com → adeengineer)
- Remove unused imports
- Trim trailing whitespace
- Ensure files end with newline
- POM file formatting

**Why it matters:**
- Prevents formatting debates
- Ensures consistent code style across team
- CI/CD will fail if code is not formatted
- Reduces git diff noise

For details, see:
- [CONTRIBUTING.md](../../CONTRIBUTING.md#code-formatting) - Complete formatting guide
- [ADR-001](../3-design/decisions/001-code-formatting-tool-selection.md) - Decision rationale

### Code Quality Checks (Checkstyle)

```bash
# Run Checkstyle validation
mvn checkstyle:check

# Generate HTML report
mvn checkstyle:checkstyle
# View: target/site/checkstyle.html
```

**Common violations to avoid:**
- Missing Javadoc on public classes/methods
- Missing `package-info.java` files
- Hidden field warnings (parameter names hiding instance fields)
- TODO comments (use issue tracker instead)

### Coding Standards

**Java Style:**
- **Style Guide**: Google Java Format (AOSP style)
- **Line Length**: 100 characters maximum
- **Indentation**: 4 spaces (no tabs)
- **Naming**:
  - `camelCase` for variables/methods
  - `PascalCase` for classes
  - `UPPER_SNAKE_CASE` for constants
  - Package names: lowercase, no underscores

**Import Ordering:**
```java
// 1. Java standard library
import java.util.List;
import java.util.Map;

// 2. javax/jakarta
import jakarta.validation.constraints.NotNull;

// 3. Third-party libraries (org, com)
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

// 4. Project imports
import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.platform.core.AgentRegistry;
```

**Lombok Usage:**

Use Lombok to reduce boilerplate:

```java
@Data                    // Getters, setters, toString, equals, hashCode
@Builder                 // Builder pattern
@Slf4j                   // Logger field
@RequiredArgsConstructor // Constructor for final fields
public class TaskResult {
    private final String taskId;
    private final String agentName;
    private String output;
    private boolean success;
}
```

**Javadoc Requirements:**
- All public classes must have class-level Javadoc
- All public methods must have Javadoc (unless self-explanatory)
- Each package must have `package-info.java`

```java
/**
 * Manages agent registration and lifecycle.
 *
 * <p>Provides centralized registry for all agents in the platform,
 * supporting dynamic registration and lookup.
 *
 * @since 0.1.0
 */
@Component
public class AgentRegistry {
    /**
     * Registers a new agent with the platform.
     *
     * @param config Agent configuration including name, capabilities
     * @throws IllegalArgumentException if agent name already exists
     */
    public void registerAgent(AgentConfig config) {
        // Implementation
    }
}
```

### Code Organization

**Class Structure Order:**
```java
// 1. Package declaration
package adeengineer.dev.platform.core;

// 2. Imports

// 3. Class Javadoc
/**
 * ...
 */
// 4. Annotations
@Slf4j
@Component
public class ExampleClass {
    // 5. Constants
    private static final int MAX_RETRIES = 3;

    // 6. Fields (grouped by type)
    private final Dependency dependency;
    private String state;

    // 7. Constructor(s)
    public ExampleClass(Dependency dependency) {
        this.dependency = dependency;
    }

    // 8. Public methods
    public void publicMethod() { }

    // 9. Package-private methods
    void packageMethod() { }

    // 10. Protected methods
    protected void protectedMethod() { }

    // 11. Private methods
    private void privateHelper() { }

    // 12. Inner classes/enums
    private static class InnerClass { }
}
```

---

## Common Development Tasks

### Build and Run

```bash
# Clean build
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Run the platform
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build executable JAR
mvn package
java -jar target/ade-agent-platform-0.2.0-SNAPSHOT.jar
```

### Testing

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=AgentRegistryTest

# Run specific test method
mvn test -Dtest=AgentRegistryTest#registerAgent_shouldAddToRegistry_whenValidConfig

# Run integration tests
mvn verify

# Skip integration tests
mvn install -DskipITs

# Run with coverage
mvn verify jacoco:report
open target/site/jacoco/index.html
```

### Code Quality

```bash
# Format code
mvn spotless:apply

# Check formatting
mvn spotless:check

# Run Checkstyle
mvn checkstyle:check

# Full quality check
mvn verify spotless:check checkstyle:check
```

### Debugging

```bash
# Debug mode (port 5005)
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Then connect your IDE debugger to localhost:5005
```

### Working with Profiles

```bash
# Dev profile (verbose logging, dev LLM settings)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Test profile
mvn test -Dspring.profiles.active=test
```

### Switch LLM Providers

```bash
# Use Ollama (default)
export LLM_PROVIDER=ollama
mvn spring-boot:run

# Use Anthropic Claude
export ANTHROPIC_API_KEY=sk-ant-xxx
export LLM_PROVIDER=anthropic
mvn spring-boot:run

# Use OpenAI GPT
export OPENAI_API_KEY=sk-xxx
export LLM_PROVIDER=openai
mvn spring-boot:run
```

### Dependency Management

```bash
# Show dependency tree
mvn dependency:tree

# Check for updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates
```

### Performance Monitoring

```bash
# Run load tests
mvn gatling:test

# Access actuator endpoints (when running)
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

---

## Contributing Workflow

### Before You Commit

**Pre-commit Checklist:**
```bash
# 1. Format code
mvn spotless:apply

# 2. Run tests
mvn verify

# 3. Check code quality
mvn checkstyle:check

# 4. Verify build succeeds
mvn clean install
```

### Commit Message Format

Use **Conventional Commits** format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Examples:**
```bash
feat(agent): add sentiment analysis agent
fix(cache): resolve Redis connection timeout
docs(guide): update developer-guide with Spotless info
refactor(loader): extract domain validation logic
test(registry): add unit tests for agent registration
```

See [CONTRIBUTING.md](../../CONTRIBUTING.md#commit-message-guidelines) for complete guidelines.

### Pull Request Process

1. Create feature branch: `git checkout -b feature/your-feature-name`
2. Make changes following coding standards
3. Run pre-commit checklist (above)
4. Push and create Pull Request
5. Address review feedback
6. Squash and merge after approval

See [CONTRIBUTING.md](../../CONTRIBUTING.md#pull-request-process) for details.

---

## Troubleshooting

### Common Issues

**Build Failures:**

```bash
# Clean build artifacts
mvn clean

# Clear local Maven cache
rm -rf ~/.m2/repository/adeengineer

# Rebuild
mvn clean install
```

**Spotless Failures:**

```bash
# If spotless:check fails after applying
mvn clean
mvn spotless:apply
mvn spotless:check
```

**Integration Test Failures:**

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not running
ollama serve

# Check if model is pulled
ollama pull qwen2.5:0.5b
```

**Port Already in Use:**

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

For more troubleshooting, see [doc/7-maintenance/troubleshooting-guide.md](../7-maintenance/troubleshooting-guide.md).

---

## Additional Resources

### Project Documentation

1. **Architecture & Design:**
   - [Architecture Overview](../3-design/architecture.md) - System architecture
   - [Workflow Diagrams](../3-design/workflow.md) - Process sequences
   - [Data Flow Diagrams](../3-design/dataflow.md) - Data transformations
   - [Workflow vs Data Flow Guide](guide/workflow-vs-dataflow.md) - Understanding the difference
   - [API Design](../3-design/api-design.md) - REST API specifications
   - [Error Handling Strategy](../3-design/error-handling-strategy.md)

2. **Development Guides:**
   - [Local Setup Guide](local-setup-guide.md) - Detailed setup instructions
   - [Coding Standards](coding-standards.md) - Code style guide (deprecated, see this file)
   - [Debugging Guide](debugging-guide.md) - Debugging techniques
   - [Integration Testing Guide](guide/integration-testing-guide.md)
   - [Ollama Integration Troubleshooting](guide/ollama-integration-troubleshooting.md)

3. **Testing:**
   - [Test Plan](../5-testing/test-plan.md) - Testing strategy
   - [Test Coverage Report](../5-testing/test-coverage-report.md)
   - [Performance Optimization Tests](../5-testing/performance-optimization-tests.md)

4. **Decision Records:**
   - [ADR-001: Code Formatting Tool Selection](../3-design/decisions/001-code-formatting-tool-selection.md)

5. **Contributing:**
   - [CONTRIBUTING.md](../../CONTRIBUTING.md) - Complete contributing guide
   - [README.md](../../README.md) - Project overview

### External Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Shell Documentation](https://docs.spring.io/spring-shell/docs/current/reference/htmlsingle/)
- [Google Java Format](https://github.com/google/google-java-format)
- [Spotless Plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Gatling Documentation](https://gatling.io/docs/gatling/)

### Getting Help

- **Questions?** Check [doc/7-maintenance/troubleshooting-guide.md](../7-maintenance/troubleshooting-guide.md)
- **Bugs?** Create an issue on GitHub
- **Feature Requests?** Open a discussion on GitHub
- **Need Help?** Contact the development team

---

## Quick Command Reference

```bash
# Build & Run
mvn clean install              # Full build with tests
mvn clean install -DskipTests  # Build without tests
mvn spring-boot:run            # Run the platform

# Code Quality
mvn spotless:apply             # Format code (ALWAYS before commit!)
mvn spotless:check             # Check formatting
mvn checkstyle:check           # Check code quality

# Testing
mvn test                       # Unit tests
mvn verify                     # Unit + Integration tests
mvn gatling:test               # Load tests
mvn verify jacoco:report       # Coverage report

# Debugging
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Profiles
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Dependencies
mvn dependency:tree            # Show dependency tree
mvn versions:display-dependency-updates  # Check for updates
```

---

*Last Updated: 2025-10-20*
*Version: 2.0*
