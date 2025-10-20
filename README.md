<div align="center">

```
   ●───●
    ╲ ╱
     ●      ade Agent Platform
    ╱ ╲
   ●───●   Domain-Agnostic Multi-Agent AI System
```

</div>

---

# ade Agent Platform

A generic, domain-agnostic platform for building intelligent multi-agent AI systems across unlimited domains. Built on the **ade Agent SDK** framework.

## Overview

The **ade Agent Platform** is an enterprise-grade application platform that enables rapid deployment of multi-agent AI systems for any domain—software engineering, healthcare, legal, finance, and more. Using a plugin-based architecture, you can configure new agents and domains through YAML configuration files without writing code.

### What is ade?

**ade** = **a**gent **d**evelopment **e**nvironment (Primary) / **a**dvanced **d**omain **e**ngine (Secondary)

ade is PHD Systems' unified framework and platform for building production-ready, domain-agnostic multi-agent systems.

## Key Features

- **Unlimited Domains** - Support any domain through plugin architecture (software engineering, healthcare, legal, finance, etc.)
- **YAML-Driven Configuration** - Add new agents without code changes (30 minutes vs 4-6 hours)
- **Multi-Agent Orchestration** - Sequential, parallel, and hierarchical workflows
- **LLM Provider Flexibility** - Supports Anthropic Claude, OpenAI GPT, Ollama, and more
- **Production-Ready Infrastructure** - Circuit breaker, retry logic, caching, monitoring
- **Dual Interfaces** - REST API + Spring Shell CLI
- **Role-Specific Output Formats** - Technical, business, executive formatting
- **Built on ade Agent SDK** - Leverages 12+ reusable framework components

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+ or Gradle 8+
- Git
- Optional: Ollama server with qwen3:0.6b (or Anthropic/OpenAI API keys)

### Installation

```bash
git clone https://github.com/phdsystems/software-engineer.git
cd software-engineer/ade-agent-platform
mvn clean install -DskipTests
```

### Run the Platform

```bash
# Using Maven
mvn spring-boot:run

# Or using the built JAR
java -jar target/ade-agent-platform-0.2.0-SNAPSHOT.jar
```

### Usage Examples

#### Spring Shell CLI

```bash
# In the Spring Shell prompt:
shell:> list-roles
shell:> execute --role "Software Developer" --task "Review PR #123"
shell:> describe-role "QA Engineer"
shell:> load-domain /path/to/custom-domain
```

#### REST API

```bash
# List all agents
curl http://localhost:8080/api/roles

# Execute a task
curl -X POST http://localhost:8080/api/tasks/execute \
  -H "Content-Type: application/json" \
  -d '{
    "roleName": "Software Developer",
    "task": "Review PR #123 for security issues",
    "context": {"repo": "my-app", "branch": "feature/auth"}
  }'

# Multi-agent collaboration
curl -X POST http://localhost:8080/api/tasks/multi-agent \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Security audit for authentication system",
    "agents": ["Software Developer", "Security Engineer", "QA Engineer"]
  }'

# Load a new domain
curl -X POST http://localhost:8080/api/domains/load?path=domains/healthcare
```

## Architecture

### Three-Tier ade Ecosystem

```
┌─────────────────────────────────────┐
│     TIER 3: DOMAIN PLUGINS          │
│  (software-engineering, healthcare) │
└─────────────────────────────────────┘
                 ↓ Uses
┌─────────────────────────────────────┐
│   TIER 2: ade Agent Platform        │  ← You are here
│  (This application)                 │
│  • Agent Registry                   │
│  • Domain Loader                    │
│  • Multi-Agent Orchestration        │
│  • REST API + CLI                   │
└─────────────────────────────────────┘
                 ↓ Built on
┌─────────────────────────────────────┐
│     TIER 1: ade Agent SDK           │
│  (ade-agent, ade-agent-async, etc.) │
└─────────────────────────────────────┘
```

### Platform Components

```
ade Agent Platform
├── Agent Registry          # Manages all registered agents
├── Domain Loader           # Plugin system for loading domains
├── ConfigurableAgent       # Generic YAML-driven agent
├── Orchestration Engine    # Multi-agent workflows
├── LLM Provider Factory    # Anthropic, OpenAI, Ollama
├── Output Formatter        # Technical, Business, Executive
├── REST API                # HTTP interface
└── CLI (Spring Shell)      # Command-line interface
```

## Supported Domains

### 1. Software Engineering (13 agents)
```
domains/software-engineering/
├── developer.yaml
├── qa.yaml
├── security.yaml
├── devops.yaml
├── manager.yaml
└── ... (8 more)
```

**Agents:** Developer, QA Engineer, Security Engineer, DevOps, Engineering Manager, Product Owner, SRE, Data Engineer, Compliance, Executive, UI/UX Designer, Technical Writer, Customer Support

### 2. Healthcare (4 agents)
```
domains/healthcare/
├── triage.yaml
├── diagnostics.yaml
├── treatment.yaml
└── pharmacy.yaml
```

**Agents:** Triage Coordinator, Diagnostic Specialist, Treatment Planner, Pharmacy Assistant

### 3. Legal (4 agents)
```
domains/legal/
├── contract-analyst.yaml
├── compliance-officer.yaml
├── legal-researcher.yaml
└── litigation-strategist.yaml
```

**Agents:** Contract Analyst, Compliance Officer, Legal Researcher, Litigation Strategist

### Create Your Own Domain

**30-minute setup** - No code required!

```yaml
# domains/finance/domain.yaml
name: "finance"
version: "1.0.0"
description: "Financial analysis and advisory domain"
outputFormats:
  - technical
  - executive
  - regulatory
agentDirectory: "agents/"
enabled: true

# domains/finance/agents/financial-analyst.yaml
name: "Financial Analyst"
description: "Expert in financial analysis and investment strategies"
capabilities:
  - "Financial statement analysis"
  - "Investment recommendations"
  - "Risk assessment"
temperature: 0.3
maxTokens: 4096
outputFormat: "technical"
promptTemplate: |
  You are an expert Financial Analyst.
  Task: {task}
  Context: {context}
  Provide detailed financial analysis with data-driven insights.
```

Load your domain:
```bash
shell:> load-domain domains/finance
# Or via API
curl -X POST http://localhost:8080/api/domains/load?path=domains/finance
```

## Documentation

### Getting Started
- **[Local Setup Guide](doc/4-development/local-setup-guide.md)** - Installation and troubleshooting
- **[Project Analysis Summary](doc/project-analysis-summary.md)** - Comprehensive overview

### Architecture & Design
- **[ADE Branding Alignment](../doc/3-design/ade-branding-alignment.md)** - Product line architecture
- **[Architecture Design](doc/3-design/architecture.md)** - System architecture
- **[Generic Refactoring Plan](doc/3-design/generic-refactoring-plan.md)** - Plugin system design
- **[Workflow Diagrams](doc/3-design/workflow.md)** - Process sequences & timing
- **[Data Flow Diagrams](doc/3-design/dataflow.md)** - Data transformations
- **[API Design](doc/3-design/api-design.md)** - REST API documentation

### Development
- **[Developer Guide](doc/4-development/developer-guide.md)** - Building and extending the platform
- **[Migration Guide](doc/guide/migration-to-generic-summary.md)** - Migrating from legacy system
- **[Requirements](doc/1-planning/requirements.md)** - Functional and non-functional requirements

## Technology Stack

- **Java 21** - Latest LTS with preview features
- **Spring Boot 3.3.5** - Application framework
- **Spring Shell 3.3.3** - CLI interface
- **Spring WebFlux** - Reactive web support
- **ade Agent SDK** - Core agent framework (12+ modules)
- **Inference Orchestr8a** - LLM provider abstraction
- **Resilience4j** - Circuit breaker, retry logic
- **Caffeine / Redis** - Caching (local and distributed)
- **Micrometer + Prometheus** - Metrics and monitoring
- **Jackson** - JSON/YAML processing
- **Lombok** - Boilerplate reduction

## LLM Provider Support

- **Anthropic Claude** (claude-3-5-sonnet-20241022)
- **OpenAI GPT** (gpt-4-turbo-preview)
- **Ollama** (local deployment, qwen3:0.6b)
- **HuggingFace, vLLM, TGI** (configured, implementation pending)

## Building from Source

### Using Maven
```bash
# Build
mvn clean install

# Run tests
mvn test

# Run integration tests
mvn verify -P integration-test

# Package
mvn package
```

### Using Gradle
```bash
# Build
./gradlew clean build

# Run tests
./gradlew test

# Package
./gradlew bootJar
```

## Configuration

### Application Properties

```yaml
# src/main/resources/application.yml
agents:
  domain-plugin-enabled: true       # Enable plugin system
  domains-path: domains              # Domain plugins directory

llm:
  primary-provider: ollama           # Default provider

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: claude-3-5-sonnet-20241022

openai:
  api-key: ${OPENAI_API_KEY}
  model: gpt-4-turbo-preview

ollama:
  base-url: http://localhost:11434
  model: qwen3:0.6b
```

### Environment Variables

```bash
export ANTHROPIC_API_KEY="your-key-here"
export OPENAI_API_KEY="your-key-here"
export OLLAMA_BASE_URL="http://localhost:11434"
```

## Observability

### Health Checks
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

### Monitoring Features
- Agent execution time tracking
- LLM token usage monitoring
- Provider failover events
- Cache hit/miss rates
- Error rate tracking

## Migration from Legacy System

**v0.1.0 (role-manager-app):** Hardcoded 13 software engineering agent classes
**v0.2.0 (ade-agent-platform):** Generic plugin-based system

### Impact
- **Domains:** 1 → Unlimited
- **Code:** 5,000 → 3,500 lines (-30%)
- **New Agent Time:** 4-6 hours → 30 minutes (-87%)
- **Backward Compatibility:** 100%

See **[Migration Guide](doc/guide/migration-to-generic-summary.md)** for details.

Legacy code preserved in `/home/developer/software-engineer/software-engineering-agent/` for reference.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

## Versioning

- **v0.1.0** - Initial release (role-manager-app)
- **v0.2.0** - Generic plugin system, renamed to ade-agent-platform
- **v0.3.0** - Package rename (planned)
- **v1.0.0** - Production release (planned)

Uses [Semantic Versioning](https://semver.org/).

## License

MIT License

## Support

- **GitHub Issues:** https://github.com/phdsystems/software-engineer/issues
- **Documentation:** `doc/` directory
- **ade Branding Guide:** `../doc/3-design/ade-branding-alignment.md`

---

**Part of the PHD Systems ade Ecosystem**

```
ade Agent SDK (Framework)
    ↓
ade Agent Platform (Application) ← You are here
    ↓
Domain Plugins (Business Logic)
```

**Last Updated:** 2025-10-20
**Version:** 0.2.0-SNAPSHOT
**Status:** Active Development
