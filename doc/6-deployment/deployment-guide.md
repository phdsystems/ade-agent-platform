# Deployment Guide - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Deployment**: Docker container or JAR on server. **Requirements**: Java 21, API keys in env vars. **Steps**: Build JAR → Set env vars → Run. **Monitoring**: Prometheus metrics, health check endpoint. **Rollback**: Keep previous JAR, restart with old version.

---

## Prerequisites

- Java 21+ installed
- API keys for Anthropic/OpenAI
- 4GB RAM minimum
- Network access to LLM APIs

---

## Deployment Steps

### Option 1: JAR Deployment

```bash
# Build
mvn clean package -DskipTests

# Set environment variables
export ANTHROPIC_API_KEY=sk-ant-xxx
export OPENAI_API_KEY=sk-xxx

# Run
java -jar target/role-manager-app-1.0.0.jar
```

### Option 2: Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/role-manager-app-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
docker build -t role-manager-app .
docker run -e ANTHROPIC_API_KEY=xxx -p 8080:8080 role-manager-app
```

---

## Configuration

### Environment Variables

```bash
# LLM Provider
export LLM_PROVIDER=anthropic          # or openai, ollama
export ANTHROPIC_API_KEY=sk-ant-xxx
export OPENAI_API_KEY=sk-xxx

# Application
export SERVER_PORT=8080
export LOG_LEVEL=INFO
```

### application-prod.yml

```yaml
role-manager:
  agents:
    config-dir: /opt/role-manager/config/agents

llm:
  timeout-seconds: 60
  max-retries: 3

server:
  port: 8080
```

---

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "llmProvider": {
      "status": "UP"
    }
  }
}
```

---

## Monitoring

**Prometheus Metrics**:
- `role_manager_tasks_total{role="Developer"}` - Task count per role
- `role_manager_task_duration_seconds{role="Developer"}` - Task latency
- `role_manager_llm_tokens_total{provider="anthropic"}` - Token usage
- `role_manager_errors_total` - Error count

**Access metrics**: `http://localhost:8080/actuator/prometheus`

---

## Rollback Procedure

```bash
# Stop current version
systemctl stop role-manager

# Restore previous JAR
cp role-manager-app-1.0.0.jar.backup role-manager-app-1.0.0.jar

# Start previous version
systemctl start role-manager

# Verify
curl http://localhost:8080/actuator/health
```

---

*Last Updated: 2025-10-17*
