# Debugging Guide - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Quick debug**: Check logs → Verify API keys → Test with mock LLM → Check agent registration. **Common issues**: Missing API keys (set env vars), slow responses (check LLM provider status), agent not found (check @Component annotation). **Tools**: Spring Boot DevTools, IDE debugger, logging with SLF4J.

---

## Quick Commands Reference

### Build & Compilation

```bash
# Clean build
mvn clean compile

# Build with specific Java version
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn clean compile

# Build skipping tests
mvn clean package -DskipTests

# Show compilation errors only
mvn compile 2>&1 | grep -A 10 "ERROR"
```

### Testing

```bash
# Run all tests
mvn test

# Run only unit tests (fast)
mvn test -Dtest='!*IntegrationTest,!*E2ETest'

# Run specific test class
mvn test -Dtest=DeveloperAgentTest

# Run specific test method
mvn test -Dtest=DeveloperAgentTest#executeTask_shouldCallLLM

# Run tests with debug output
mvn test -X

# Run tests with coverage
mvn test jacoco:report
```

### Running the Application

```bash
# Run with Maven
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debug enabled
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Run JAR directly
java -jar target/role-manager-app-1.0.0.jar

# Run with environment variables
export ANTHROPIC_API_KEY=sk-ant-xxx
mvn spring-boot:run
```

### Dependency Management

```bash
# Show dependency tree
mvn dependency:tree

# Find specific dependency
mvn dependency:tree | grep anthropic

# Download sources and javadocs
mvn dependency:sources dependency:resolve -Dclassifier=javadoc

# Check for dependency updates
mvn versions:display-dependency-updates
```

### Log Analysis

```bash
# View application logs
tail -f logs/role-manager.log

# Filter for errors
grep ERROR logs/role-manager.log

# Filter for specific agent
grep "DeveloperAgent" logs/role-manager.log

# Show last 100 lines with errors
tail -100 logs/role-manager.log | grep -A 5 ERROR

# Count errors by type
grep ERROR logs/role-manager.log | cut -d':' -f3 | sort | uniq -c
```

### Git Operations

```bash
# Check status
git status

# View recent commits
git log --oneline -10

# Show changes in specific file
git diff src/main/java/com/rolemanager/agents/DeveloperAgent.java

# Commit with conventional commit format
git commit -m "feat(agents): add DeveloperAgent implementation"
```

---

## Common Issues and Solutions

### Issue 1: Application Won't Start

**Symptoms**: `mvn spring-boot:run` fails immediately

**Diagnosis**:
```bash
# Check for compilation errors
mvn compile 2>&1 | grep ERROR

# Check Spring Boot logs
mvn spring-boot:run 2>&1 | head -50
```

**Common Causes**:

1. **Missing API Keys**
   ```bash
   # Solution: Set environment variables
   export ANTHROPIC_API_KEY=sk-ant-xxx
   export OPENAI_API_KEY=sk-xxx
   ```

2. **Port Already in Use**
   ```bash
   # Check what's using port 8080
   lsof -i :8080

   # Solution: Kill the process or change port
   export SERVER_PORT=8081
   mvn spring-boot:run
   ```

3. **Invalid Configuration**
   ```bash
   # Check YAML syntax
   yamllint src/main/resources/application.yml

   # Verify property names match Java fields (camelCase)
   grep -r "agent_" src/main/resources/  # Should return nothing
   ```

---

### Issue 2: Agent Not Found

**Symptoms**: `IllegalArgumentException: Unknown role: Developer`

**Diagnosis**:
```bash
# Check if agent class has @Component annotation
grep -r "@Component" src/main/java/com/rolemanager/agents/

# Check Spring component scan logs
mvn spring-boot:run | grep "Mapped.*Agent"
```

**Solutions**:

1. **Missing @Component Annotation**
   ```java
   @Component  // Add this
   public class DeveloperAgent implements AgentRole {
   ```

2. **Agent Not in Scanned Package**
   ```java
   // Verify package structure
   // Should be: adeengineer.dev.platform.agents.DeveloperAgent

   // If in different package, add component scan:
   @SpringBootApplication
   @ComponentScan(basePackages = {"adeengineer.dev.platform", "com.custom.agents"})
   public class RoleManagerApplication {
   ```

3. **Check Agent Registration**
   ```bash
   # Enable debug logging
   mvn spring-boot:run -Dlogging.level.adeengineer.dev.platform.core.AgentRegistry=DEBUG
   ```

---

### Issue 3: LLM API Call Failing

**Symptoms**: `LLMProviderException: API call failed`

**Diagnosis**:
```bash
# Check API key is set
echo $ANTHROPIC_API_KEY

# Test API connectivity
curl -H "anthropic-version: 2023-06-01" \
     -H "x-api-key: $ANTHROPIC_API_KEY" \
     https://api.anthropic.com/v1/messages

# Check application logs for full error
grep "LLMProvider" logs/role-manager.log | tail -20
```

**Solutions**:

1. **Invalid API Key** (401 Unauthorized)
   ```bash
   # Verify key format
   echo $ANTHROPIC_API_KEY | grep "^sk-ant-"

   # Update to valid key
   export ANTHROPIC_API_KEY=sk-ant-your-valid-key
   ```

2. **Network Timeout** (Connection timeout)
   ```yaml
   # Increase timeout in application.yml
   llm:
     timeout-seconds: 60  # Increase from default 30
   ```

3. **Rate Limiting** (429 Too Many Requests)
   ```java
   // Add retry logic with exponential backoff
   // Already implemented in LLMProvider base class

   // Or reduce request rate in tests
   Thread.sleep(1000); // Between test requests
   ```

---

### Issue 4: Slow Response Times

**Symptoms**: Tasks taking > 30 seconds to complete

**Diagnosis**:
```bash
# Check LLM provider status
curl https://status.anthropic.com/

# Monitor response times in logs
grep "duration" logs/role-manager.log | tail -20

# Profile with JVM tools
jps  # Find PID
jstack <PID> > thread-dump.txt
```

**Solutions**:

1. **Large Prompts**
   ```java
   // Reduce prompt size in agent configuration
   // Check token usage in logs
   LOG.info("Tokens used: {}", response.getTotalTokens());

   // Solution: Shorten prompt templates
   ```

2. **High Temperature**
   ```yaml
   # Reduce temperature for faster responses
   agents:
     developer:
       temperature: 0.3  # Lower = faster but less creative
   ```

3. **Network Latency**
   ```bash
   # Check latency to LLM API
   ping api.anthropic.com
   traceroute api.anthropic.com

   # Consider switching to Ollama (local)
   export LLM_PROVIDER=ollama
   ```

---

### Issue 5: Tests Failing

**Symptoms**: `mvn test` shows failures

**Diagnosis**:
```bash
# Run tests with verbose output
mvn test -X

# Run specific failing test
mvn test -Dtest=DeveloperAgentTest#failingMethod

# Check test logs
cat target/surefire-reports/DeveloperAgentTest.txt
```

**Solutions**:

1. **Integration Tests Without API Keys**
   ```bash
   # Run only unit tests
   mvn test -Dtest='!*IntegrationTest'

   # Or set API keys for integration tests
   export ANTHROPIC_API_KEY=sk-ant-xxx
   mvn test
   ```

2. **Mocking Issues**
   ```java
   // ❌ Don't mock final methods
   when(provider.getClass().getSimpleName()).thenReturn("Mock");

   // ✅ Use real instances for testing
   LLMProvider provider = new AnthropicProvider("test-key", "model", 100, 0.7);
   ```

3. **Flaky Tests**
   ```bash
   # Run test multiple times
   mvn test -Dtest=FlakyTest -DfailIfNoTests=false -Dsurefire.rerunFailingTestsCount=3
   ```

---

## Debugging Techniques

### 1. Enable Debug Logging

**application-dev.yml**:
```yaml
logging:
  level:
    adeengineer.dev.platform: DEBUG
    adeengineer.dev.platform.llm: TRACE
    org.springframework: INFO
```

**Run with debug profile**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Use IDE Debugger

**IntelliJ IDEA**:
1. Set breakpoint in agent's `executeTask` method
2. Right-click on `RoleManagerApplication` → Debug
3. Set environment variables in Run Configuration
4. Step through code with F8

**VS Code**:
```json
// .vscode/launch.json
{
  "type": "java",
  "name": "Debug Role Manager",
  "request": "launch",
  "mainClass": "adeengineer.dev.platform.RoleManagerApplication",
  "env": {
    "ANTHROPIC_API_KEY": "sk-ant-xxx"
  }
}
```

### 3. Remote Debugging

**Start application with debug enabled**:
```bash
java -jar target/role-manager-app-1.0.0.jar \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

**Connect from IDE**:
- IntelliJ: Run → Edit Configurations → Add Remote JVM Debug
- Host: localhost, Port: 5005

### 4. Spring Boot DevTools

**Add to pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Benefits**:
- Automatic restart on code changes
- LiveReload for web pages
- Property defaults for development

### 5. Actuator Endpoints

**Enable in application.yml**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers
```

**Useful endpoints**:
```bash
# Health check
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/metrics

# List registered beans
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans'

# Change log level at runtime
curl -X POST http://localhost:8080/actuator/loggers/adeengineer.dev.platform \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## Logging Best Practices

### Use SLF4J with Lombok

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeveloperAgent implements AgentRole {

    @Override
    public LLMResponse executeTask(String task, Map<String, Object> context) {
        log.debug("Executing task for Developer role: {}", task);

        try {
            LLMResponse response = llm.createCompletion(prompt, maxTokens, temperature, Map.of());
            log.info("Task completed. Tokens used: {}", response.getTotalTokens());
            return response;
        } catch (LLMProviderException e) {
            log.error("LLM API call failed for task: {}", task, e);
            throw e;
        }
    }
}
```

### Structured Logging

```java
// Use MDC for contextual logging
import org.slf4j.MDC;

MDC.put("role", roleName);
MDC.put("taskId", taskId);
log.info("Processing task");
// Log output: [role=Developer] [taskId=123] Processing task
MDC.clear();
```

### Log Levels

- **TRACE**: Very detailed, method entry/exit
- **DEBUG**: Detailed info for debugging (prompt content, token counts)
- **INFO**: General informational messages (task started, completed)
- **WARN**: Potentially harmful situations (high token usage, slow response)
- **ERROR**: Error events that might still allow app to continue

---

## Performance Profiling

### 1. JVM Monitoring

```bash
# Monitor heap usage
jstat -gc <PID> 1000

# Create heap dump
jmap -dump:live,format=b,file=heap.bin <PID>

# Analyze with VisualVM
jvisualvm
```

### 2. Spring Boot Metrics

```bash
# Response time metrics
curl http://localhost:8080/actuator/metrics/http.server.requests | jq

# JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq

# Custom metrics (if implemented)
curl http://localhost:8080/actuator/metrics/role.manager.task.duration | jq
```

### 3. Profiling Tools

**VisualVM**:
- CPU profiling: Find slow methods
- Memory profiling: Detect memory leaks
- Thread analysis: Find deadlocks

**YourKit**:
- More advanced profiling
- Method-level timing
- Memory allocation tracking

---

## Environment-Specific Debugging

### Development Environment

```yaml
# application-dev.yml
spring:
  devtools:
    restart:
      enabled: true

logging:
  level:
    adeengineer.dev.platform: DEBUG

llm:
  default-provider: ollama  # Use local for faster development
  timeout-seconds: 60
```

### Testing Environment

```yaml
# application-test.yml
llm:
  timeout-seconds: 10  # Faster failures in tests
  max-retries: 1

logging:
  level:
    adeengineer.dev.platform: INFO
```

### Production Environment

```yaml
# application-prod.yml
logging:
  level:
    adeengineer.dev.platform: WARN
    adeengineer.dev.platform.llm: INFO
  file:
    name: /var/log/role-manager/app.log

llm:
  timeout-seconds: 30
  max-retries: 3
```

---

## Troubleshooting Checklist

When debugging an issue, work through this checklist:

- [ ] Check application logs for errors
- [ ] Verify environment variables are set (API keys)
- [ ] Confirm agent is registered (check @Component)
- [ ] Test with mock LLM provider to isolate issue
- [ ] Check LLM provider status page
- [ ] Verify network connectivity to LLM APIs
- [ ] Check configuration YAML syntax
- [ ] Review recent code changes
- [ ] Run tests to verify functionality
- [ ] Check resource usage (CPU, memory)
- [ ] Review Spring Boot Actuator metrics
- [ ] Enable DEBUG logging for problematic component
- [ ] Use IDE debugger to step through code
- [ ] Check for version conflicts in dependencies

---

## Getting Help

**Self-Service Resources**:
1. This debugging guide
2. [Troubleshooting Guide](../7-maintenance/troubleshooting-guide.md)
3. [Developer Guide](developer-guide.md)
4. Application logs: `logs/role-manager.log`

**Team Resources**:
- Slack: #role-manager-dev
- Code reviews: Ask teammates for second opinion
- Documentation: Check architecture and API design docs

**External Resources**:
- Spring Boot Docs: https://spring.io/projects/spring-boot
- Anthropic API Status: https://status.anthropic.com/
- OpenAI API Status: https://status.openai.com/
- Stack Overflow: Tag questions with `spring-boot`, `java`, `llm`

---

## References

1. [Developer Testing Guide](dev-testing-guide.md)
2. [Troubleshooting Guide](../7-maintenance/troubleshooting-guide.md)
3. [Spring Boot Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
4. [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)

---

*Last Updated: 2025-10-17*
