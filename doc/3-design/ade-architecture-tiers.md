# ade Architecture: Three-Tier Separation

**Date:** 2025-10-20
**Version:** 1.0.0
**Status:** Accepted
**Decision:** Keep aiagent-sdk, inference-orchestr8a, and ade-agent-platform as separate repositories

---

## TL;DR

**Decision**: aiagent-sdk (framework), inference-orchestr8a (LLM library), and ade-agent-platform (application) MUST remain separate repositories. **Rationale**: Follows industry-standard tier separation (framework → library → application → plugins). **Benefits**: Reusability, clear dependencies, easier distribution, proper separation of concerns.

---

## Table of Contents

- [Context](#context)
- [Decision](#decision)
- [Architecture Overview](#architecture-overview)
- [Tier Responsibilities](#tier-responsibilities)
- [Rationale](#rationale)
- [Consequences](#consequences)
- [Alternatives Considered](#alternatives-considered)
- [References](#references)

---

## Context

The ade ecosystem consists of three major components:

1. **aiagent-sdk** - Pure agent abstractions and utilities (11 modules)
2. **inference-orchestr8a** - LLM provider abstraction and orchestration (3 modules)
3. **ade-agent-platform** - Multi-agent orchestration application

During refactoring, the question arose: **Should we move aiagent-sdk and inference-orchestr8a into ade-agent-platform?**

This document explains why they MUST remain separate.

---

## Decision

**We will keep aiagent-sdk, inference-orchestr8a, and ade-agent-platform as separate, independent repositories.**

### Directory Structure

```
/home/developer/software-engineer/
├── aiagent-sdk/                    # TIER 1: Framework (SDK)
│   ├── ade-agent-async/
│   ├── ade-agent-composition/
│   ├── ade-agent-monitoring/
│   └── ... (11 modules)
│
├── inference-orchestr8a/           # TIER 1.5: Infrastructure (LLM)
│   ├── inference-orchestr8a-core/
│   ├── inference-orchestr8a-kotlin/
│   └── inference-orchestr8a-scala/
│
├── ade-agent-platform/             # TIER 2: Application Platform
│   ├── src/main/java/
│   │   └── com/rolemanager/core/
│   │       ├── ConfigurableAgent.java
│   │       ├── DomainLoader.java
│   │       └── ...
│   └── pom.xml (depends on aiagent-sdk + inference-orchestr8a)
│
├── examples/                       # TIER 3: Domain Plugins
│   ├── software-engineering/
│   ├── healthcare/
│   └── legal/
│
└── software-engineering-agent/    # Legacy (archived)
```

---

## Architecture Overview

### Three-Tier Architecture

```
┌─────────────────────────────────────────┐
│ TIER 3: Domain Plugins (YAML)          │  ← Domain-specific knowledge
├─────────────────────────────────────────┤
│ TIER 2: ade-agent-platform              │  ← Application logic
├─────────────────────────────────────────┤
│ TIER 1.5: inference-orchestr8a          │  ← LLM infrastructure
├─────────────────────────────────────────┤
│ TIER 1: aiagent-sdk                     │  ← Framework abstractions
└─────────────────────────────────────────┘
```

### Dependency Flow

```
Domain Plugins (YAML configs)
    ↓ loaded by
ade-agent-platform (Application)
    ↓ depends on
inference-orchestr8a (LLM Library)
    ↓ depends on
aiagent-sdk (Framework)
    ↓ depends on
Java 21 + Spring Boot
```

---

## Tier Responsibilities

### TIER 1: aiagent-sdk (Framework Layer)

**Purpose:** Pure abstractions and utilities for building ANY agent system

**Modules:**
- `ade-agent` - Core agent interface (ZERO dependencies)
- `ade-agent-async` - Async/reactive agent execution
- `ade-agent-composition` - Agent composition patterns (sequential, parallel, hierarchical)
- `ade-agent-monitoring` - Metrics, tracing, observability
- `ade-agent-resilience` - Circuit breakers, retries, timeouts
- `ade-agent-security` - Authentication, authorization, encryption
- `ade-agent-rest` - REST API support
- `ade-agent-grpc` - gRPC support
- `ade-agent-serialization` - JSON/XML/Protobuf serialization
- `ade-agent-discovery` - Service discovery
- `ade-agent-streaming` - Streaming support
- `ade-agent-spring-boot-starter` - Spring Boot autoconfiguration

**Key Characteristics:**
- ✅ Domain-agnostic
- ✅ Reusable across ANY agent application
- ✅ No LLM dependencies
- ✅ Minimal dependencies (pure Java 21)

**Analogy:** Like Spring Framework - provides infrastructure, not business logic

**Who Uses It:**
- ade-agent-platform (multi-agent orchestration)
- Other agent applications (rule-based agents, script agents, etc.)
- Third-party projects building agent systems

---

### TIER 1.5: inference-orchestr8a (Infrastructure Layer)

**Purpose:** LLM provider abstraction and orchestration

**Modules:**
- `inference-orchestr8a-core` - Java support for 8 LLM providers
- `inference-orchestr8a-kotlin` - Kotlin coroutine extensions
- `inference-orchestr8a-scala` - Scala functional extensions

**Providers Supported:**
- Anthropic Claude
- OpenAI GPT
- Ollama (local models)
- HuggingFace
- VLLM
- BentoML
- RayServe
- TextGenerationInference

**Key Characteristics:**
- ✅ LLM-agnostic (supports multiple providers)
- ✅ Production-ready (circuit breakers, caching, failover)
- ✅ Multi-language (Java, Kotlin, Scala)
- ✅ Reusable for ANY LLM-powered application

**Analogy:** Like JDBC - provides database abstraction layer

**Who Uses It:**
- ade-agent-platform (for LLM-powered agents)
- Chatbot applications
- Document processing systems
- Any application needing LLM integration

---

### TIER 2: ade-agent-platform (Application Layer)

**Purpose:** Multi-agent orchestration with domain plugin system

**Key Components:**
- `ConfigurableAgent` - Generic YAML-driven agent
- `DomainLoader` - Loads domain plugins at runtime
- `RoleManager` - Orchestrates multi-agent workflows
- `OutputFormatterRegistry` - Role-specific output formatting

**Key Characteristics:**
- ✅ Specific use case (multi-agent orchestration)
- ✅ Depends on aiagent-sdk + inference-orchestr8a
- ✅ Domain-agnostic platform (domains are plugins)
- ✅ Not meant to be a library (it's an application)

**Analogy:** Like a Spring Boot application - uses frameworks to build specific functionality

**Who Uses It:**
- Software engineering teams (via software-engineering domain)
- Healthcare organizations (via healthcare domain)
- Legal firms (via legal domain)

---

### TIER 3: Domain Plugins (Configuration Layer)

**Purpose:** Domain-specific agent configurations (YAML only, no code)

**Examples:**
- `examples/software-engineering/` - 13 software dev agents
- `examples/healthcare/` - 4 healthcare agents
- `examples/legal/` - 4 legal agents

**Key Characteristics:**
- ✅ Pure YAML configuration
- ✅ No code changes needed
- ✅ Loaded at runtime by ade-agent-platform
- ✅ Domain-specific knowledge

---

## Rationale

### 1. Reusability

**Current (Separate):**
```
Project A: Uses aiagent-sdk for rule-based agents ✅
Project B: Uses inference-orchestr8a for chatbot ✅
Project C: Uses ade-agent-platform for orchestration ✅
```

**If Combined (aiagent-sdk inside ade-agent-platform):**
```
Project A: Must depend on entire ade-agent-platform to use SDK ❌
Project B: Must depend on entire ade-agent-platform to use LLM lib ❌
Project C: Only project that benefits ❌
```

**Verdict:** Separation enables maximum reusability.

---

### 2. Dependency Management

**Current (Separate):**
```xml
<!-- ade-agent-platform/pom.xml -->
<dependency>
    <groupId>com.phdsystems</groupId>
    <artifactId>ade-agent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.phdsystems</groupId>
    <artifactId>inference-orchestr8a-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**If Combined:**
```
ade-agent-platform/
├── aiagent-sdk/          ← Now internal modules
├── inference-orchestr8a/ ← Can't version independently
└── src/
```

**Problems:**
- ❌ Can't version SDK separately from platform
- ❌ Breaking changes in SDK force platform release
- ❌ Other projects can't depend on SDK without platform

**Verdict:** Separation enables independent versioning and clear dependencies.

---

### 3. Distribution and Publishing

**Current (Separate):**
- Publish `ade-agent:1.0.0` to Maven Central
- Publish `inference-orchestr8a-core:1.0.0` to Maven Central
- Publish `ade-agent-platform:0.2.0` to Maven Central
- Users depend on what they need

**If Combined:**
- Publish `ade-agent-platform:0.2.0` (includes everything)
- Users must download entire platform to use SDK
- Cannot consume SDK independently

**Verdict:** Separation enables granular dependency management.

---

### 4. Separation of Concerns

**aiagent-sdk:**
- **Concern:** "What is an agent?" (abstractions)
- **Scope:** Any agent system
- **Dependencies:** Minimal (Java 21, optional Jackson)

**inference-orchestr8a:**
- **Concern:** "How do we talk to LLMs?" (infrastructure)
- **Scope:** Any LLM-powered application
- **Dependencies:** HTTP clients, LLM SDKs

**ade-agent-platform:**
- **Concern:** "How do we orchestrate multiple agents?" (application logic)
- **Scope:** Multi-agent workflows
- **Dependencies:** aiagent-sdk + inference-orchestr8a

**Verdict:** Each tier has distinct responsibility. Merging would violate single responsibility principle.

---

### 5. Industry Standards

**Comparison with established architectures:**

| Our Architecture | Industry Example |
|------------------|-----------------|
| aiagent-sdk | Spring Framework |
| inference-orchestr8a | JDBC Drivers |
| ade-agent-platform | Spring Boot Application |
| Domain Plugins | application.yml configs |

**Examples of Tier Separation:**

**Spring Ecosystem:**
```
spring-framework/          (Framework - separate repo)
spring-data-jpa/          (Library - separate repo)
my-spring-boot-app/       (Application - separate repo)
```

**Python Ecosystem:**
```
langchain/                (Framework - separate repo)
openai-python/            (Library - separate repo)
my-agent-app/             (Application - separate repo)
```

**Verdict:** Industry best practice is to keep framework, libraries, and applications separate.

---

## Consequences

### Positive Consequences

1. **Clear Boundaries**
   - Each repository has single, well-defined purpose
   - Easy to understand what each component does

2. **Independent Evolution**
   - SDK can evolve without changing platform
   - Platform can evolve without breaking SDK
   - LLM library can add providers independently

3. **Reusability**
   - Other projects can use SDK for non-LLM agents
   - Other projects can use LLM library for non-agent use cases
   - Platform remains focused on orchestration

4. **Easier Testing**
   - Test SDK independently (unit tests)
   - Test LLM library independently (integration tests)
   - Test platform with mocked dependencies

5. **Better Documentation**
   - Each repository has focused documentation
   - Users only read docs for what they need

6. **Simplified Contributions**
   - Contributors to SDK don't need to understand platform
   - Contributors to platform don't need to understand LLM internals

---

### Negative Consequences

1. **Multiple Repositories**
   - Need to manage 3 separate repos
   - More complex local development setup

   **Mitigation:** Use Maven parent POM or Git submodules

2. **Version Synchronization**
   - Must ensure compatible versions across tiers
   - Breaking changes require coordinated releases

   **Mitigation:** Semantic versioning + compatibility matrix

3. **Cross-Repo Changes**
   - Feature spanning multiple tiers requires multiple PRs
   - Testing requires local builds of dependencies

   **Mitigation:** Integration tests + CI/CD pipeline

---

## Alternatives Considered

### Alternative 1: Monorepo (Everything in ade-agent-platform)

```
ade-agent-platform/
├── aiagent-sdk/
├── inference-orchestr8a/
└── src/
```

**Pros:**
- ✅ Simpler local development (everything in one place)
- ✅ Easier to make cross-cutting changes

**Cons:**
- ❌ SDK not reusable by other projects
- ❌ LLM library trapped in platform
- ❌ Violates separation of concerns
- ❌ Larger deployment artifact
- ❌ Can't version independently

**Decision:** **REJECTED** - Cons outweigh pros

---

### Alternative 2: Merge Only SDK (Keep LLM library separate)

```
ade-agent-platform/
├── aiagent-sdk/
└── src/

inference-orchestr8a/ (separate)
```

**Pros:**
- ✅ Slightly simpler than current setup

**Cons:**
- ❌ SDK still not reusable
- ❌ Inconsistent (why merge SDK but not LLM lib?)
- ❌ Still violates tier separation

**Decision:** **REJECTED** - No clear benefit

---

### Alternative 3: Current Architecture (Separate Repos)

```
aiagent-sdk/              (Framework)
inference-orchestr8a/     (Library)
ade-agent-platform/       (Application)
```

**Pros:**
- ✅ Clear tier separation
- ✅ Maximum reusability
- ✅ Independent versioning
- ✅ Follows industry standards
- ✅ Better separation of concerns

**Cons:**
- ❌ Multiple repositories to manage
- ❌ More complex local dev setup

**Decision:** **ACCEPTED** - Benefits far outweigh costs

---

## Implementation Guidelines

### For SDK Developers (aiagent-sdk)

1. **Keep dependencies minimal** - Only pure Java 21 + optional Jackson
2. **No LLM dependencies** - SDK should work with ANY agent implementation
3. **Document all interfaces** - Other projects depend on these
4. **Semantic versioning** - Breaking changes = major version bump

### For LLM Library Developers (inference-orchestr8a)

1. **Support multiple providers** - Don't lock into single LLM vendor
2. **Production patterns** - Circuit breakers, caching, failover
3. **Multi-language support** - Java, Kotlin, Scala
4. **Provider-agnostic API** - Easy to switch providers

### For Platform Developers (ade-agent-platform)

1. **Consume, don't duplicate** - Use SDK abstractions, don't reimplement
2. **Domain-agnostic platform** - Domain knowledge in YAML, not code
3. **Plugin-based architecture** - Domains loaded at runtime
4. **Clear configuration** - Make it easy to override defaults

### For Domain Plugin Developers

1. **YAML-only** - No Java/Kotlin/Scala code
2. **Follow examples/** structure - Consistent domain layouts
3. **Document capabilities** - Clear description of what agents do
4. **Test configurations** - Ensure prompts produce expected outputs

---

## References

### Internal Documentation

- [ade Branding Alignment](./ade-branding-alignment.md) - Three-tier architecture definition
- [Generic Refactoring Plan](./generic-refactoring-plan.md) - Plugin system migration
- [aiagent-sdk README](../../aiagent-sdk/README.md) - SDK architecture
- [inference-orchestr8a README](../../inference-orchestr8a/README.md) - LLM library docs
- [ade-agent-platform README](../../ade-agent-platform/README.md) - Platform docs

### Industry References

- [Spring Framework Architecture](https://docs.spring.io/spring-framework/reference/) - Framework vs. application separation
- [JDBC Specification](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/) - Driver abstraction pattern
- [Semantic Versioning](https://semver.org/) - Versioning strategy
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) - Tier separation principles

---

## Decision Matrix

| Criterion | Monorepo | SDK Merged | Separate (Current) |
|-----------|----------|------------|-------------------|
| Reusability | ❌ Poor | ❌ Poor | ✅ Excellent |
| Versioning | ❌ Coupled | ❌ Coupled | ✅ Independent |
| Distribution | ❌ Monolithic | ❌ Bloated | ✅ Granular |
| SoC | ❌ Violated | ❌ Violated | ✅ Clear |
| Local Dev | ✅ Simple | ✅ Simple | ⚠️ Complex |
| Industry Alignment | ❌ Unusual | ❌ Unusual | ✅ Standard |
| **Score** | **1/6** | **1/6** | **5/6** |

**Winner:** Separate repositories (current architecture)

---

## Conclusion

The three-tier architecture with separate repositories is the **correct architectural decision** for the ade ecosystem.

**Key Principles:**
1. **Framework ≠ Application** - aiagent-sdk is a framework, not part of the platform
2. **Library ≠ Application** - inference-orchestr8a is a library, not part of the platform
3. **Tier Separation** - Each tier has distinct responsibility and scope
4. **Reusability First** - Maximize potential for reuse across projects

**Final Architecture:**
```
aiagent-sdk (TIER 1)
    ↓
inference-orchestr8a (TIER 1.5)
    ↓
ade-agent-platform (TIER 2)
    ↓
Domain Plugins (TIER 3)
```

**This architecture MUST be preserved.**

---

**Last Updated:** 2025-10-20
**Version:** 1.0.0
**Status:** Accepted
**Authors:** PHD Systems Architecture Team
