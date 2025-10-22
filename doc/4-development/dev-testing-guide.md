# Testing Guide - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Unit tests**: Mock LLM providers for fast feedback (< 1 second per test). **Integration tests**: Use real LLM APIs to verify end-to-end agent behavior. **Coverage target**: 85%+ for all agent implementations. **Testing pattern**: AAA (Arrange, Act, Assert) with descriptive test names.

---

## Testing Strategy

### Test Pyramid

```
        /\
       /  \  E2E (Few, Slow, High Confidence)
      /____\
     /      \
    / Integ  \ (Some, Medium Speed)
   /__________\
  /            \
 /     Unit     \ (Many, Fast, Focused)
/________________\
```

**Distribution**:
- **80% Unit Tests**: Fast, isolated, mocked dependencies
- **15% Integration Tests**: Real LLM APIs, verify agent behavior
- **5% E2E Tests**: Full workflow validation

---

## Unit Testing

### Testing Agent Implementations

**Goal**: Verify agent logic without making LLM API calls

**Example: Testing DeveloperAgent**

```java
package dev.adeengineer.platform.agents;

import dev.adeengineer.platform.llm.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class DeveloperAgentTest {

    @Mock
    private LLMProvider mockLLM;

    private DeveloperAgent agent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        String promptTemplate = "You are a Developer. Task: %s";
        agent = new DeveloperAgent(mockLLM, promptTemplate);
    }

    @Test
    void getRoleName_shouldReturnDeveloper() {
        // Act
        String roleName = agent.getRoleName();

        // Assert
        assertThat(roleName).isEqualTo("Developer");
    }

    @Test
    void getCapabilities_shouldIncludeTechnicalSkills() {
        // Act
        var capabilities = agent.getCapabilities();

        // Assert
        assertThat(capabilities)
            .contains("Code review", "Architecture design", "Bug fixing");
    }

    @Test
    void executeTask_shouldCallLLMWithFormattedPrompt() {
        // Arrange
        String task = "Review PR #123";
        LLMResponse mockResponse = new LLMResponse(
            "Technical review output",
            "claude-3-5-sonnet-20241022",
            150,
            50,
            Map.of()
        );
        when(mockLLM.createCompletion(anyString(), anyInt(), anyDouble(), anyMap()))
            .thenReturn(mockResponse);

        // Act
        LLMResponse response = agent.executeTask(task, Map.of());

        // Assert
        assertThat(response.getText()).isEqualTo("Technical review output");
    }

    @Test
    void formatOutput_shouldIncludeTechnicalDetails() {
        // Arrange
        LLMResponse response = new LLMResponse(
            "File: src/Main.java\nIssue: Missing null check",
            "claude-3-5-sonnet-20241022",
            150,
            50,
            Map.of()
        );

        // Act
        String formatted = agent.formatOutput(response);

        // Assert
        assertThat(formatted)
            .contains("File:")
            .contains("Issue:");
    }
}
```

### Testing RoleManager Service

```java
package dev.adeengineer.platform.core;

import dev.adeengineer.platform.agents.DeveloperAgent;
import dev.adeengineer.platform.llm.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RoleManagerTest {

    private RoleManager roleManager;
    private AgentRegistry registry;

    @Mock
    private LLMProvider mockLLM;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registry = new AgentRegistry();
        roleManager = new RoleManager(registry);

        // Register test agent
        DeveloperAgent devAgent = new DeveloperAgent(mockLLM, "prompt");
        registry.registerAgent(devAgent);
    }

    @Test
    void executeTask_withValidRole_shouldReturnTaskResult() {
        // Arrange
        when(mockLLM.createCompletion(anyString(), anyInt(), anyDouble(), anyMap()))
            .thenReturn(new LLMResponse("Output", "model", 100, 50, Map.of()));

        // Act
        TaskResult result = roleManager.executeTask(
            "Developer",
            "Review code",
            Map.of()
        );

        // Assert
        assertThat(result.role()).isEqualTo("Developer");
        assertThat(result.output()).isEqualTo("Output");
        assertThat(result.success()).isTrue();
    }

    @Test
    void executeTask_withInvalidRole_shouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() ->
            roleManager.executeTask("InvalidRole", "Task", Map.of())
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown role: InvalidRole");
    }

    @Test
    void listRoles_shouldReturnAllRegisteredRoles() {
        // Act
        var roles = roleManager.listRoles();

        // Assert
        assertThat(roles).containsExactly("Developer");
    }
}
```

---

## Integration Testing

### Testing with Real LLM APIs

**Goal**: Verify agent behavior with actual LLM providers

**Setup**:
```bash
# Set API keys
export ANTHROPIC_API_KEY=sk-ant-xxx
export OPENAI_API_KEY=sk-xxx
```

**Example: Integration Test**

```java
package dev.adeengineer.platform.integration;

import dev.adeengineer.platform.core.RoleManager;
import dev.adeengineer.platform.core.TaskResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class DeveloperAgentIntegrationTest {

    @Autowired
    private RoleManager roleManager;

    @Test
    void executeTask_withRealLLM_shouldReturnTechnicalOutput() {
        // Arrange
        String task = "Explain the Singleton design pattern in Java";

        // Act
        TaskResult result = roleManager.executeTask(
            "Developer",
            task,
            Map.of()
        );

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.output())
            .isNotEmpty()
            .containsIgnoringCase("singleton")
            .containsIgnoringCase("instance");
        assertThat(result.tokensUsed()).isGreaterThan(0);
    }

    @Test
    void executeTask_withRealLLM_shouldCompleteWithin30Seconds() {
        // Arrange
        String task = "Review this code snippet";
        long startTime = System.currentTimeMillis();

        // Act
        TaskResult result = roleManager.executeTask(
            "Developer",
            task,
            Map.of("code", "public void test() {}")
        );
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(duration).isLessThan(30_000); // < 30 seconds
    }
}
```

**Test Configuration** (`src/test/resources/application-test.properties`):

```properties
# Integration test configuration
llm.default-provider=anthropic
anthropic.api-key=${ANTHROPIC_API_KEY}
anthropic.model=claude-3-5-sonnet-20241022
anthropic.max-tokens=2048
anthropic.temperature=0.7

# Reduce timeouts for faster failures
llm.timeout-seconds=30
llm.max-retries=2
```

---

## E2E Testing

### Full Workflow Tests

**Goal**: Verify complete user workflows with mocked LLM responses

**Example: Multi-Agent Collaboration**

```java
package dev.adeengineer.platform.e2e;

import dev.adeengineer.platform.core.RoleManager;
import dev.adeengineer.platform.core.TaskResult;
import dev.adeengineer.platform.llm.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class MultiAgentWorkflowE2ETest {

    @Autowired
    private RoleManager roleManager;

    @MockBean
    private LLMProvider llmProvider;

    @Test
    void multiAgentCollaboration_shouldCombineAllPerspectives() {
        // Arrange
        when(llmProvider.createCompletion(contains("Developer"), anyInt(), anyDouble(), anyMap()))
            .thenReturn(new LLMResponse("Technical review output", "mock", 100, 50, Map.of()));

        when(llmProvider.createCompletion(contains("QA"), anyInt(), anyDouble(), anyMap()))
            .thenReturn(new LLMResponse("Testing strategy output", "mock", 100, 50, Map.of()));

        when(llmProvider.createCompletion(contains("Security"), anyInt(), anyDouble(), anyMap()))
            .thenReturn(new LLMResponse("Security analysis output", "mock", 100, 50, Map.of()));

        // Act
        List<TaskResult> results = roleManager.executeMultiAgent(
            List.of("Developer", "QA", "Security"),
            "Analyze new user authentication feature",
            Map.of()
        );

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results.get(0).output()).contains("Technical review");
        assertThat(results.get(1).output()).contains("Testing strategy");
        assertThat(results.get(2).output()).contains("Security analysis");
    }
}
```

### Automated E2E Testing

**Goal**: Verify complete application workflows through REST API with mocked LLM responses

**Test Suite**: Located in `src/test/java/com/rolemanager/e2e/`

**Coverage**:
- **22 E2E tests** covering:
  - Developer Agent workflows (5 tests)
  - Manager Agent workflows (3 tests)
  - QA Agent workflows (3 tests)
  - Multi-agent collaboration (6 tests)
  - Role discovery API endpoints (5 tests)

**Current Status**: All 22 tests passing (100% pass rate) ✅

#### Running E2E Tests

**Important**: E2E tests require the `e2etest` Spring profile to use mocked LLM providers and test agent configurations.

**Option 1: Run all E2E tests (Fastest for E2E development)**
```bash
# Quick E2E test execution (~17 seconds)
mvn test-compile failsafe:integration-test -Dspring.profiles.active=e2etest

# What it does:
# - Compiles code
# - Runs only E2E tests (*E2ETest.java files)
# - Skips unit tests and coverage checks
# - Uses mocked LLM providers (no API calls)

# Use when: Developing/debugging E2E tests
```

**Option 2: Run full verification (Recommended for CI/CD)**
```bash
# Complete test suite with coverage (~54 seconds)
mvn clean verify -Dspring.profiles.active=e2etest

# What it does:
# - Runs unit tests (168 tests)
# - Runs E2E tests (22 tests)
# - Generates coverage report
# - Validates coverage thresholds (50% minimum)
# - Fails build if any tests fail

# Use when: Before committing, in CI/CD pipeline
```

**Option 3: Run specific E2E test class**
```bash
# Single E2E test class
mvn test-compile failsafe:integration-test \
  -Dspring.profiles.active=e2etest \
  -Dit.test=DeveloperAgentE2ETest

# Multiple E2E test classes
mvn test-compile failsafe:integration-test \
  -Dspring.profiles.active=e2etest \
  -Dit.test=DeveloperAgentE2ETest,ManagerAgentE2ETest

# With pattern matching
mvn test-compile failsafe:integration-test \
  -Dspring.profiles.active=e2etest \
  -Dit.test=*AgentE2ETest
```

**Option 4: Run only unit tests (Skip E2E)**
```bash
# Unit tests only (~20 seconds)
mvn test

# What it does:
# - Runs only unit tests
# - Excludes *E2ETest.java files (configured in maven-surefire-plugin)
# - No Spring Boot application context startup

# Use when: Testing business logic changes
```

#### Maven Failsafe vs Surefire

**Surefire Plugin** (`mvn test`):
- Runs unit tests during `test` phase
- Excludes `**/*E2ETest.java` files
- Fails build immediately on test failure

**Failsafe Plugin** (`mvn failsafe:integration-test`):
- Runs E2E/integration tests during `integration-test` phase
- Includes `**/*E2ETest.java` files only
- Does NOT fail build during test execution (allows cleanup)
- Checks results during `verify` phase

#### Spring Profiles for Testing

E2E tests use the `e2etest` profile to configure test-specific behavior:

**Profile Configuration** (`src/test/resources/application-e2etest.yml`):
```yaml
spring:
  profiles:
    active: e2etest

llm:
  default-provider: mock  # No real API calls

agent:
  config-path: classpath:config/agents-test.yml  # Test agent configs
```

**Why profiles are important**:
- Production: Uses `@Profile("!e2etest")` to autowire agents from YAML configs
- E2E Tests: Excludes autowiring, manually creates test agents with `@TestConfiguration`
- Prevents ApplicationContext conflicts between component scanning and manual bean creation

See [Spring Profiles and E2E Testing Guide](guide/spring-profiles-e2e-testing.md) for detailed explanation.

#### Test Reports

E2E test results are generated in:
- HTML reports: `target/failsafe-reports/*.html`
- XML reports: `target/failsafe-reports/*.xml`
- Text reports: `target/failsafe-reports/*.txt`
- Summary: `target/failsafe-reports/failsafe-summary.xml`

**View test results**:
```bash
# Check test summary
cat target/failsafe-reports/failsafe-summary.xml

# View detailed results for a test class
cat target/failsafe-reports/dev.adeengineer.platform.e2e.DeveloperAgentE2ETest.txt
```

### Manual E2E Testing

**Goal**: Quick validation of full application workflows without writing automated tests

**When to use**:
- During local development for rapid feedback
- Before implementing automated E2E tests
- Testing new features end-to-end
- Troubleshooting integration issues

**Prerequisites**:
```bash
# Ensure application is built
mvn clean install -DskipTests
```

#### Option 1: REST API Testing

**1. Start the application**:
```bash
mvn spring-boot:run

# Or using the JAR:
java -jar target/role-manager-app-0.1.0-SNAPSHOT.jar
```

**2. Test endpoints with curl**:

```bash
# List all available roles
curl http://localhost:8080/api/roles

# Get role information
curl http://localhost:8080/api/roles/Developer

# Execute a task for a specific role
curl -X POST http://localhost:8080/api/tasks/execute \
  -H "Content-Type: application/json" \
  -d '{
    "roleName": "Developer",
    "task": "Review this code for potential bugs",
    "context": {
      "code": "public void process(String input) { System.out.println(input.length()); }"
    }
  }'

# Execute multi-agent task
curl -X POST http://localhost:8080/api/tasks/multi-agent \
  -H "Content-Type: application/json" \
  -d '{
    "roleNames": ["Developer", "QA", "Security"],
    "task": "Analyze new authentication feature",
    "context": {}
  }'

# Check health status
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/metrics
```

**3. Test with Postman or Insomnia**:

Import the following collection for easier testing:

```json
{
  "name": "Role Manager API",
  "requests": [
    {
      "name": "List Roles",
      "method": "GET",
      "url": "http://localhost:8080/api/roles"
    },
    {
      "name": "Execute Task",
      "method": "POST",
      "url": "http://localhost:8080/api/tasks/execute",
      "body": {
        "roleName": "Developer",
        "task": "Review PR #123",
        "context": {}
      }
    }
  ]
}
```

#### Option 2: Spring Shell CLI Testing

**1. Start the interactive shell**:
```bash
mvn spring-boot:run

# Wait for Spring Shell prompt to appear
shell:>
```

**2. Test CLI commands**:

```bash
# List all available roles
shell:> list-roles

# Describe a specific role
shell:> describe-role Developer

# Execute a task
shell:> execute --role "Developer" --task "Review this code for bugs"

# Execute with context
shell:> execute --role "QA" --task "Generate test cases" --context "feature=login"

# Execute multi-agent collaboration
shell:> multi-agent --roles "Developer,QA,Security" --task "Analyze authentication feature"

# Get help on commands
shell:> help
shell:> help execute
```

#### Option 3: Test Scenarios (Complete Workflows)

**Scenario 1: Code Review Workflow**
```bash
# 1. Start application
mvn spring-boot:run

# 2. In another terminal, execute code review
curl -X POST http://localhost:8080/api/tasks/execute \
  -H "Content-Type: application/json" \
  -d '{
    "roleName": "Developer",
    "task": "Review this PR for code quality",
    "context": {
      "prNumber": "123",
      "files": ["src/Main.java", "src/Service.java"]
    }
  }'

# 3. Verify response contains technical details
# Expected: Code analysis, file-specific feedback, recommendations
```

**Scenario 2: Multi-Agent Analysis**
```bash
# Execute security review with multiple perspectives
curl -X POST http://localhost:8080/api/tasks/multi-agent \
  -H "Content-Type: application/json" \
  -d '{
    "roleNames": ["Developer", "Security", "QA"],
    "task": "Analyze new user authentication endpoint",
    "context": {
      "endpoint": "/api/auth/login",
      "method": "POST"
    }
  }'

# Expected: Developer (technical), Security (vulnerabilities), QA (test cases)
```

**Scenario 3: Manager Metrics Request**
```bash
# Request executive summary
curl -X POST http://localhost:8080/api/tasks/execute \
  -H "Content-Type: application/json" \
  -d '{
    "roleName": "Engineering Manager",
    "task": "Summarize team velocity metrics",
    "context": {
      "sprint": "Sprint 23",
      "team": "Platform Engineering"
    }
  }'

# Expected: High-level summary, no technical jargon
```

#### Verification Checklist

After manual testing, verify:

- [ ] Application starts without errors
- [ ] All 13 roles are available (`/api/roles`)
- [ ] Task execution returns results within 30 seconds
- [ ] Error messages are clear and helpful
- [ ] Different roles produce role-appropriate output
- [ ] Multi-agent tasks execute in parallel
- [ ] Response includes usage/token information
- [ ] Health endpoint shows application is healthy

#### Common Issues

**Port Already in Use**:
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

**LLM Provider Not Configured**:
```bash
# Set API keys before starting
export ANTHROPIC_API_KEY=sk-ant-xxx
mvn spring-boot:run

# Or use Ollama (no API key needed)
# Ensure Ollama is running with qwen3:0.6b model
ollama pull qwen3:0.6b
```

**Slow Response Times**:
```bash
# Check if LLM provider is responding
curl -I https://api.anthropic.com/v1/messages

# Reduce max tokens for faster responses
# Edit application.properties:
# anthropic.max-tokens=1024
```

---

## Test Data Management

### Mock LLM Responses

**Location**: `src/test/resources/mock-responses/`

**developer-response.json**:
```json
{
  "text": "## Code Review\n\nFile: src/Main.java:15\nIssue: Missing null check\nSeverity: Medium\n\nRecommendation: Add null validation before accessing object properties.",
  "model": "mock-model",
  "promptTokens": 150,
  "completionTokens": 75
}
```

**Loading Mock Responses**:
```java
public class MockResponseLoader {
    public static LLMResponse loadResponse(String filename) throws IOException {
        String content = Files.readString(
            Path.of("src/test/resources/mock-responses/" + filename)
        );
        // Parse JSON and return LLMResponse
    }
}
```

---

## Running Tests

### Maven Commands (Primary)

#### Run All Tests
```bash
mvn clean test
```

#### Run All Tests with Coverage Report (Auto-Generated)
```bash
mvn clean test

# Coverage report automatically generated at:
# target/site/jacoco/index.html
```

#### View Coverage Report
```bash
# Open in browser (macOS)
open target/site/jacoco/index.html

# Open in browser (Linux)
xdg-open target/site/jacoco/index.html

# Or navigate to:
# target/site/jacoco/index.html
```

#### Run Specific Test Class
```bash
# Single test class
mvn test -Dtest=AgentRegistryTest

# Multiple test classes
mvn test -Dtest=AgentRegistryTest,RoleManagerTest

# With pattern matching
mvn test -Dtest=*ControllerTest
```

#### Run Specific Test Method
```bash
# Single test method
mvn test -Dtest=AgentRegistryTest#shouldRegisterSingleAgent

# Multiple methods from same class
mvn test -Dtest=AgentRegistryTest#shouldRegisterSingleAgent+shouldRetrieveAgentByRoleName
```

#### Run Only Unit Tests (Fast)
```bash
# Exclude integration and E2E tests
mvn test -Dtest='!*IntegrationTest,!*E2ETest'

# Or use test groups (if configured)
mvn test -Dgroups=unit
```

#### Run Only Integration Tests (Requires API Keys)
```bash
# Set API keys first
export ANTHROPIC_API_KEY=sk-ant-xxx
export OPENAI_API_KEY=sk-xxx

# Run integration tests
mvn test -Dtest='*IntegrationTest'
```

#### Verify Coverage Threshold
```bash
# Check if coverage meets minimum requirements (70%)
mvn test jacoco:check

# Will fail if coverage is below threshold configured in pom.xml
```

#### Run Tests with Detailed Output
```bash
# Show test output in console
mvn test -X

# Or with info level
mvn test --info
```

#### Clean and Rebuild Before Testing
```bash
# Full clean build with tests
mvn clean compile test

# Skip tests during build, run separately
mvn clean install -DskipTests
mvn test
```

### Multi-Module Maven Projects

#### Understanding Maven Command Syntax

In multi-module projects (like ade-agent-platform), Maven commands have special options for targeting specific modules:

**Basic Syntax:**
```bash
mvn [lifecycle-phase] [options] [redirects]
```

**Common Options Explained:**

| Option | Syntax | Purpose | Example |
|--------|--------|---------|---------|
| **-pl** | `-pl module-name` | Select specific module(s) | `-pl ade-agent-platform-spring-boot` |
| **-am** | `-am` | Also build required modules | `-pl spring-boot -am` |
| **-amd** | `-amd` | Also build dependent modules | `-pl core -amd` |
| **-D** | `-Dproperty=value` | Set system property | `-Dtest=RoleControllerTest` |
| **-X** | `-X` | Enable debug output | `mvn test -X` |
| **-e** | `-e` | Show full error stacktraces | `mvn test -e` |
| **-q** | `-q` | Quiet mode (errors only) | `mvn test -q` |
| **-T** | `-T 4` | Parallel build (4 threads) | `mvn clean install -T 4` |

#### Module Selection Examples

```bash
# Test only Spring Boot module (fastest)
mvn clean test -pl ade-agent-platform-spring-boot

# Test Spring Boot + all dependencies (core, agentunit)
mvn clean test -pl ade-agent-platform-spring-boot -am

# Test all modules that depend on core
mvn clean test -pl ade-agent-platform-core -amd

# Test multiple specific modules
mvn clean test -pl ade-agent-platform-core,ade-agent-platform-spring-boot

# Test everything except Quarkus
mvn clean test -pl '!ade-agent-platform-quarkus'
```

#### Advanced Test Selection

**Run single test in specific module:**
```bash
mvn clean test -pl ade-agent-platform-spring-boot -Dtest=RoleControllerTest
```

**Breakdown:**
- `mvn` - Maven command
- `clean` - Delete target/ directory (fresh build)
- `test` - Run unit tests
- `-pl ade-agent-platform-spring-boot` - Only this module
- `-Dtest=RoleControllerTest` - Only this test class

**Run single test method in specific module:**
```bash
mvn test -pl ade-agent-platform-spring-boot \
  -Dtest=RoleControllerTest#shouldListAllRoles
```

**Run tests matching pattern in specific module:**
```bash
# All controller tests in Spring Boot module
mvn test -pl ade-agent-platform-spring-boot -Dtest='*ControllerTest'

# All E2E tests in Spring Boot module
mvn test -pl ade-agent-platform-spring-boot -Dtest='*E2ETest'
```

#### Shell Redirection

**`2>&1` - Merge error and output streams:**

```bash
# Without redirection (errors go to stderr, output to stdout)
mvn test | grep "ERROR"  # Won't catch compilation errors

# With redirection (everything to stdout)
mvn test 2>&1 | grep "ERROR"  # Catches all errors
```

**Common redirection patterns:**

```bash
# Capture all output to file
mvn test 2>&1 | tee test-output.log

# Filter for errors only
mvn test 2>&1 | grep -E "(ERROR|FAILURE)"

# Show only test summary
mvn test 2>&1 | grep "Tests run:"

# Save errors to file, still show in console
mvn test 2>&1 | tee /dev/stderr | grep ERROR > errors.log
```

#### Performance Optimization

**Parallel module builds:**
```bash
# Build 4 modules in parallel (if independent)
mvn clean install -T 4

# Use 1 thread per CPU core
mvn clean install -T 1C
```

**Skip expensive operations:**
```bash
# Skip tests (build only)
mvn clean install -DskipTests

# Skip tests AND compilation
mvn clean install -Dmaven.test.skip=true

# Skip Spotless formatting checks
mvn test -Dspotless.check.skip=true

# Skip all checks, tests only
mvn test -DskipChecks
```

**Offline mode (use cached dependencies):**
```bash
# Don't check for updates
mvn test -o

# Or
mvn test --offline
```

#### Complete Command Examples

**Debug single test failure:**
```bash
# 1. Run with full debug output
mvn clean test -pl ade-agent-platform-spring-boot \
  -Dtest=RoleControllerTest#shouldDescribeRole \
  -X 2>&1 | tee debug.log

# 2. Check Spring context loading
mvn test -pl ade-agent-platform-spring-boot \
  -Dtest=RoleControllerTest \
  -Dlogging.level.org.springframework=DEBUG

# 3. Run with error stacktraces
mvn test -pl ade-agent-platform-spring-boot \
  -Dtest=RoleControllerTest -e
```

**Quick iterative testing:**
```bash
# First: Clean build with dependencies
mvn clean install -am -pl ade-agent-platform-spring-boot -DskipTests

# Then: Run tests repeatedly (no clean)
mvn test -pl ade-agent-platform-spring-boot -Dtest=RoleControllerTest

# Faster: Skip compilation if no code changes
mvn surefire:test -pl ade-agent-platform-spring-boot \
  -Dtest=RoleControllerTest
```

**CI/CD pipeline commands:**
```bash
# Full build with all checks (slow but thorough)
mvn clean install -T 1C

# Quick feedback (unit tests only)
mvn clean test -T 4 -Dtest='!*IntegrationTest,!*E2ETest'

# Verify with coverage
mvn clean verify jacoco:report jacoco:check
```

#### Module Structure Reference

```
ade-agent-platform/
├── ade-agent-platform-parent     (POM - parent)
├── ade-agent-platform-agentunit  (Test framework)
├── ade-agent-platform-core       (Core logic)
├── ade-agent-platform-spring-boot (Spring integration)
├── ade-agent-platform-quarkus    (Quarkus integration)
└── ade-agent-platform-micronaut  (Micronaut integration)
```

**Dependency chain:**
- `spring-boot` → depends on → `core` → depends on → `agentunit`
- `quarkus` → depends on → `core` → depends on → `agentunit`
- `micronaut` → depends on → `core` → depends on → `agentunit`

**Testing implications:**
```bash
# Test core: Only tests core (no framework modules)
mvn test -pl ade-agent-platform-core

# Test spring-boot: Needs core and agentunit built
mvn test -pl ade-agent-platform-spring-boot -am

# Test everything: All modules sequentially
mvn clean test

# Test everything in parallel: Faster
mvn clean test -T 4
```

### Gradle Commands (Alternative)

**Note**: Gradle wrapper needs to be fixed first if corrupted.

#### Fix Gradle Wrapper (If Needed)
```bash
# Download fresh Gradle wrapper
gradle wrapper --gradle-version 8.5

# Or manually download gradle-wrapper.jar to gradle/wrapper/
```

#### Run Tests with Gradle
```bash
# All tests
./gradlew clean test

# Specific test class
./gradlew test --tests AgentRegistryTest

# With coverage
./gradlew test jacocoTestReport

# Coverage report location:
# build/reports/jacoco/test/html/index.html
```

### Coverage Reports

#### Report Locations

**Maven**:
- HTML Report: `target/site/jacoco/index.html`
- XML Report: `target/site/jacoco/jacoco.xml`
- CSV Report: `target/site/jacoco/jacoco.csv`
- Test Results: `target/surefire-reports/`

**Gradle**:
- HTML Report: `build/reports/jacoco/test/html/index.html`
- XML Report: `build/reports/jacoco/test/jacocoTestReport.xml`
- Test Results: `build/test-results/test/`

#### Current Coverage Status

```bash
# View summary from command line
cat target/site/jacoco/index.html | grep "Total"

# Or check the report file directly
# Current: 32% overall coverage
# - model: 100%
# - api: 100%
# - core: 71%
```

### Test Execution Times

**Fast Tests** (Unit tests only):
```bash
# ~20 seconds for all 168 tests
mvn clean test
```

**With Coverage** (Adds ~5 seconds):
```bash
# ~25 seconds total
mvn clean test jacoco:report
```

### Continuous Integration

#### GitHub Actions / CI Pipeline
```yaml
# .github/workflows/test.yml
- name: Run tests with coverage
  run: mvn clean test jacoco:report

- name: Upload coverage to Codecov
  run: bash <(curl -s https://codecov.io/bash)
```

---

## Best Practices

### Test Naming Convention

```java
// Pattern: methodName_scenario_expectedBehavior

@Test
void executeTask_withValidInput_shouldReturnSuccessfulResult() { }

@Test
void executeTask_withNullTask_shouldThrowIllegalArgumentException() { }

@Test
void formatOutput_withTechnicalContent_shouldIncludeCodeBlocks() { }
```

### AAA Pattern (Arrange, Act, Assert)

```java
@Test
void example() {
    // Arrange - Set up test data and mocks
    String input = "test";
    when(mock.method()).thenReturn("result");

    // Act - Execute the code under test
    String result = service.process(input);

    // Assert - Verify the outcome
    assertThat(result).isEqualTo("expected");
}
```

### Test Independence

```java
// ✅ Good: Each test sets up its own data
@BeforeEach
void setUp() {
    agent = new DeveloperAgent(mockLLM, "prompt");
}

// ❌ Bad: Tests share mutable state
private static DeveloperAgent sharedAgent; // Don't do this
```

### Descriptive Assertions

```java
// ✅ Good: Clear assertion messages
assertThat(result.output())
    .as("Developer output should include file paths")
    .contains("File:", "Line:");

// ❌ Bad: No context
assertTrue(result.output().contains("File:"));
```

---

## Coverage Requirements

### Minimum Coverage Targets

- **Overall**: 85%+
- **Agent Implementations**: 90%+ (critical business logic)
- **Core Services**: 85%+
- **Utilities**: 80%+

### Excluded from Coverage

- Configuration classes
- Data transfer objects (DTOs)
- Spring Boot main class

**pom.xml Configuration**:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/config/**</exclude>
            <exclude>**/RoleManagerApplication.class</exclude>
        </excludes>
    </configuration>
</plugin>
```

---

## Troubleshooting Tests

### Diagnostic Commands

#### Quick Test Summary
```bash
# Get test results summary
mvn test 2>&1 | grep -E "(Tests run|BUILD SUCCESS|ERROR)" | tail -10

# Alternative with Maven wrapper
./mvnw test 2>&1 | grep -E "(Tests run|BUILD SUCCESS|ERROR)" | tail -10
```

#### Check for Running Maven Processes
```bash
# See if Maven is still running in background
ps aux | grep -i mvn | grep -v grep

# Wait and check again (useful after ctrl+c)
sleep 10 && ps aux | grep -i mvn | grep -v grep
```

#### Check for Running Gradle Processes
```bash
# See if Gradle daemon is running
ps aux | grep gradle

# Kill Gradle daemon if stuck
./gradlew --stop
```

#### Test Compilation Only
```bash
# Verify tests compile without running them
./mvnw test-compile -q && echo "Compilation successful"

# With Maven
mvn test-compile
```

#### Find Test Failures with Context
```bash
# Show detailed failure information
mvn test 2>&1 | grep -B5 -A10 "FAILURE\|ERROR" | head -50

# Show specific failed test methods
mvn test 2>&1 | grep -A15 "Failures: 1" | grep -A10 "Failed tests:"

# Show all test output (very verbose)
mvn test 2>&1 | less
```

#### Run Tests with Surefire Directly
```bash
# Bypass some Maven lifecycle phases
./mvnw surefire:test -DskipTests=false 2>&1 | grep -E "(Tests run|BUILD)"

# Force re-run even if code unchanged
mvn clean surefire:test
```

#### Debug Specific Test
```bash
# Run single test with full stack traces
mvn test -Dtest=RoleManagerTest -e

# Run with debug logging
mvn test -Dtest=RoleManagerTest -X

# Run and suspend for debugger attachment
mvn test -Dtest=RoleManagerTest -Dmaven.surefire.debug
```

### Integration Tests Failing with 401 Unauthorized

**Cause**: Missing or invalid API keys

**Solution**:
```bash
# Verify API keys are set
echo $ANTHROPIC_API_KEY

# Set valid keys
export ANTHROPIC_API_KEY=sk-ant-your-real-key
export OPENAI_API_KEY=sk-your-real-key

# Verify they're available to Maven
mvn help:system | grep -i api_key
```

### Tests Hanging or Timing Out

**Cause**: LLM API not responding or network issues

**Solution**:
```bash
# 1. Check if tests are actually hung
ps aux | grep java | grep surefire

# 2. Check network connectivity
curl -I https://api.anthropic.com/v1/messages

# 3. Reduce timeout in application-test.properties
```

```properties
# application-test.properties
llm.timeout-seconds=10
llm.connect-timeout-seconds=5
```

### Build Stuck or Won't Stop

**Cause**: Background processes not terminating

**Solution**:
```bash
# Find Maven/Java processes
ps aux | grep -E "java|maven"

# Kill specific Maven process
kill -9 <PID>

# Kill all Maven processes (use with caution)
pkill -9 -f maven

# Kill all Java processes running tests
pkill -9 -f surefire
```

### Tests Pass Locally But Fail in CI

**Cause**: Environment differences, timing issues, or flaky tests

**Diagnostics**:
```bash
# 1. Check Java version matches CI
java -version

# 2. Run tests multiple times to detect flakiness
for i in {1..10}; do mvn test && echo "Pass $i" || echo "Fail $i"; done

# 3. Run with CI-like environment
mvn clean test -B  # -B = batch mode (no color, timestamps)

# 4. Check for test order dependencies
mvn test -Dsurefire.runOrder=random
```

### Coverage Report Not Generated

**Cause**: JaCoCo plugin not executing or report goal not triggered

**Solution**:
```bash
# Verify JaCoCo is configured
mvn help:effective-pom | grep -A10 jacoco

# Generate report explicitly
mvn jacoco:report

# Full clean build with coverage
mvn clean test jacoco:report

# Check if report exists
ls -lh target/site/jacoco/index.html
```

### Compilation Errors During Test Build

**Cause**: Missing dependencies or syntax errors in test code

**Diagnostics**:
```bash
# Show detailed compilation errors
mvn test-compile 2>&1 | grep -A5 "COMPILATION ERROR"

# Check if main code compiles
mvn compile

# Check if issue is test-specific
mvn clean compile test-compile

# Verify dependencies are downloaded
mvn dependency:tree | grep -i test
```

### Mock Not Working

**Cause**: Trying to mock final methods, classes, or static methods

**Solution**:
```java
// Option 1: Use real instances with test configuration
LLMProvider provider = new AnthropicProvider("test-key", "test-model", 100, 0.7);

// Option 2: Use PowerMock for final classes (add dependency)
@PrepareForTest({FinalClass.class})

// Option 3: Refactor to use interfaces instead of concrete classes
```

### Spring Context Failed to Load in Tests

**Cause**: Missing beans, circular dependencies, or configuration issues

**Diagnostics**:
```bash
# Run test with full Spring debug
mvn test -Dtest=ControllerTest -Dlogging.level.org.springframework=DEBUG

# Check which beans are loaded
mvn test -Dtest=ControllerTest -Ddebug
```

**Solution**:
```java
// Add minimal test configuration
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(locations = "classpath:application-test.yml")
class MyTest {
    // Test methods
}
```

### Specific Test Class Not Found

**Cause**: Class name typo or package mismatch

**Diagnostics**:
```bash
# List all test classes
find src/test -name "*Test.java"

# Search for specific test
find src/test -name "RoleManagerTest.java"

# Check Maven sees the test
mvn test -Dtest=RoleManagerTest -X 2>&1 | grep "RoleManagerTest"
```

---

## References

1. [Test Plan](../5-testing/test-plan.md)
2. [Developer Guide](developer-guide.md)
3. [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
4. [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
5. [AssertJ Documentation](https://assertj.github.io/doc/)

---

*Last Updated: 2025-10-22*
