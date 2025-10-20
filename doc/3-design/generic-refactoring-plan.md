# Generic Multi-Agent System Refactoring Plan

**Date:** 2025-10-20
**Version:** 1.0
**Status:** Design Phase

---

## TL;DR

**Goal**: Transform Role Manager from software engineering-specific to a **generic multi-agent AI framework** supporting any domain (healthcare, legal, finance, education, etc.). **Key changes**: Remove hardcoded agent classes (13 → 1), implement domain plugin system, make output formats extensible, eliminate SE assumptions. **Timeline**: 2-3 weeks. **Impact**: 70% infrastructure already reusable; refactor remaining 30% for full domain flexibility.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
3. [Target Architecture](#target-architecture)
4. [Refactoring Strategy](#refactoring-strategy)
5. [Implementation Phases](#implementation-phases)
6. [Migration Path](#migration-path)
7. [Risk Assessment](#risk-assessment)
8. [Success Metrics](#success-metrics)
9. [References](#references)

---

## 1. Executive Summary

### 1.1 Problem Statement

The Role Manager App is currently **hardcoded for software engineering workflows**:

- **13 hardcoded agent classes** (DeveloperAgent, QAAgent, etc.)
- **Software engineering assumptions** in prompts, capabilities, output formats
- **Factory method with switch statement** prevents dynamic agent registration
- **Output formats assume SE audiences** (technical=developers, business=managers, executive=CTO)

**Result:** Cannot be used for healthcare, legal, finance, or other domains without code changes.

### 1.2 Vision

Transform Role Manager into a **generic multi-agent AI framework** that:

- ✅ Supports **any domain** via configuration (no code changes)
- ✅ Enables **dynamic agent registration** at runtime
- ✅ Provides **domain plugin architecture** for packaging domain-specific agents
- ✅ Offers **extensible output formats** beyond software engineering
- ✅ Maintains **backward compatibility** with existing SE agents

### 1.3 Business Value

| Benefit | Impact |
|---------|--------|
| **Market Expansion** | From 1 domain (SE) to unlimited domains (healthcare, legal, finance, education, etc.) |
| **Faster Deployment** | Add new domains via config files, not code releases |
| **Reusable Infrastructure** | 70% already generic (workflow, concurrency, LLM layer) |
| **Lower Maintenance** | One codebase instead of forking per domain |
| **Competitive Advantage** | First-to-market generic multi-agent framework |

### 1.4 Timeline & Effort

| Phase | Duration | Effort (Dev Days) |
|-------|----------|-------------------|
| Phase 1: Core Refactoring | 1 week | 5 days |
| Phase 2: Domain Plugin System | 1 week | 5 days |
| Phase 3: Output Format Registry | 3 days | 3 days |
| Phase 4: Testing & Documentation | 4 days | 4 days |
| **Total** | **2-3 weeks** | **17 days** |

---

## 2. Current State Analysis

### 2.1 Component-Level Assessment

| Component | Generic? | Issues | Refactor Needed? |
|-----------|----------|--------|------------------|
| **WorkflowEngine** | ✅ Yes | None | No |
| **ParallelAgentExecutor** | ✅ Yes | None | No |
| **LLM Provider Layer** | ✅ Yes | None | No |
| **AgentRegistry** | ✅ Yes | None | No |
| **BaseAgent** | ⚠️ Partial | Assumes {task}/{context} structure | Minor |
| **Agent Implementations** | ❌ No | 13 hardcoded SE agent classes | **Yes** |
| **AppConfig.createAgent()** | ❌ No | Switch statement with hardcoded roles | **Yes** |
| **OutputFormatter** | ❌ No | Only technical/business/executive formats | **Yes** |
| **YAML Configs** | ⚠️ Partial | SE-specific prompts/capabilities | Replace |

**Verdict:** 70% generic infrastructure, 30% requires refactoring.

### 2.2 Critical Dependencies

**Hardcoded Agent Factory** (AppConfig.java:108-124):
```java
private Agent createAgent(AgentConfig config, LLMProvider llmProvider, OutputFormatter outputFormatter) {
    return switch (config.name()) {
        case "Software Developer" -> new DeveloperAgent(config, llmProvider, outputFormatter);
        case "QA Engineer" -> new QAAgent(config, llmProvider, outputFormatter);
        case "Security Engineer" -> new SecurityAgent(config, llmProvider, outputFormatter);
        case "Engineering Manager" -> new ManagerAgent(config, llmProvider, outputFormatter);
        default -> {
            log.warn("Unknown role: {}. Using base agent implementation.", config.name());
            yield new BaseAgent(config, llmProvider, outputFormatter) { };
        }
    };
}
```

**Problem:** Every new role requires:
1. Create new Java class extending BaseAgent
2. Modify AppConfig.java switch statement
3. Recompile and redeploy

### 2.3 Software Engineering Assumptions

**Location 1: Agent YAML Configs** (config/agents/developer.yaml):
```yaml
capabilities:
  - "Code review and quality assessment"
  - "Debugging and root cause analysis"
  - "Refactoring suggestions"

promptTemplate: |
  You are an expert Software Developer...
  Provide detailed technical analysis including:
  - Specific code issues with file paths and line numbers
  - Root cause analysis
  - Concrete fix suggestions with code examples
```

**Location 2: OutputFormatter** (OutputFormatter.java:37-42):
```java
return switch (formatType.toLowerCase()) {
    case "technical" -> formatTechnical(content, response);    // For developers
    case "business" -> formatBusiness(content, response);      // For managers
    case "executive" -> formatExecutive(content, response);    // For CTO
    default -> content;
};
```

**Location 3: Example Tasks** (throughout docs/tests):
- "Review PR #123"
- "Analyze test coverage"
- "Security vulnerability assessment"

### 2.4 What Already Works (Reusable Components)

✅ **WorkflowEngine**: DAG-based orchestration with topological sort
✅ **ParallelAgentExecutor**: Virtual thread-based concurrency
✅ **LLM Provider Abstraction**: 8+ providers with failover
✅ **AgentRegistry**: Thread-safe O(1) lookup service
✅ **REST API Controllers**: Generic TaskRequest/TaskResult DTOs
✅ **Spring Shell CLI**: Generic command structure
✅ **Circuit Breakers**: Resilience patterns
✅ **Caching Layer**: Provider-agnostic response caching

**Key Insight:** The infrastructure is domain-agnostic. Only the agents themselves are SE-specific.

---

## 3. Target Architecture

### 3.1 Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                       Role Manager Core                           │
│  (Domain-Agnostic Multi-Agent Framework)                          │
└──────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  SE Domain    │     │ Healthcare    │     │  Legal        │
│  Plugin       │     │  Domain       │     │  Domain       │
│               │     │  Plugin       │     │  Plugin       │
├───────────────┤     ├───────────────┤     ├───────────────┤
│ - Developer   │     │ - Diagnostics │     │ - Contract    │
│ - QA Engineer │     │ - Treatment   │     │ - Compliance  │
│ - Security    │     │ - Triage      │     │ - Research    │
│ - DevOps      │     │ - Pharmacy    │     │ - Litigation  │
│ ... (13 roles)│     │ ... (N roles) │     │ ... (N roles) │
└───────────────┘     └───────────────┘     └───────────────┘
```

### 3.2 Key Design Changes

#### 3.2.1 Single ConfigurableAgent Class

**Before (13 classes):**
```
agents/
├── DeveloperAgent.java
├── QAAgent.java
├── SecurityAgent.java
├── ManagerAgent.java
├── DevOpsAgent.java
├── ProductOwnerAgent.java
├── SREAgent.java
├── DataEngineerAgent.java
├── ComplianceAgent.java
├── ExecutiveAgent.java
├── UIUXDesignerAgent.java
├── TechnicalWriterAgent.java
└── CustomerSupportAgent.java
```

**After (1 class):**
```
core/
└── ConfigurableAgent.java   (single implementation, behavior via YAML)
```

**ConfigurableAgent.java** (NEW):
```java
@Component
public class ConfigurableAgent extends BaseAgent {

    public ConfigurableAgent(AgentConfig config,
                             LLMProvider llmProvider,
                             OutputFormatterRegistry formatterRegistry) {
        super(config, llmProvider, formatterRegistry);
    }

    // All behavior defined in BaseAgent
    // No role-specific logic in code
}
```

#### 3.2.2 Domain Plugin System

**Directory Structure:**
```
domains/
├── software-engineering/
│   ├── domain.yaml              # Domain metadata
│   └── agents/
│       ├── developer.yaml
│       ├── qa.yaml
│       ├── security.yaml
│       └── ... (13 agents)
│
├── healthcare/
│   ├── domain.yaml
│   └── agents/
│       ├── diagnostics.yaml
│       ├── treatment.yaml
│       ├── triage.yaml
│       └── pharmacy.yaml
│
└── legal/
    ├── domain.yaml
    └── agents/
        ├── contract.yaml
        ├── compliance.yaml
        ├── research.yaml
        └── litigation.yaml
```

**domain.yaml Format:**
```yaml
name: "healthcare"
version: "1.0.0"
description: "Healthcare domain with diagnostic, treatment, and triage agents"
outputFormats:
  - clinical
  - patient-friendly
  - administrative
agentDirectory: "agents/"
dependencies: []
```

#### 3.2.3 Dynamic Agent Registration

**Before (Hardcoded):**
```java
// AppConfig.java
private Agent createAgent(AgentConfig config, ...) {
    return switch (config.name()) {
        case "Software Developer" -> new DeveloperAgent(...);
        case "QA Engineer" -> new QAAgent(...);
        // ... hardcoded cases
        default -> throw new IllegalArgumentException("Unknown agent: " + config.name());
    };
}
```

**After (Dynamic):**
```java
// DomainLoader.java (NEW)
public void loadDomain(String domainPath) {
    DomainConfig domainConfig = loadDomainYaml(domainPath);

    // Register domain-specific output formats
    for (String formatName : domainConfig.outputFormats()) {
        formatterRegistry.registerFormat(formatName, new CustomOutputFormatter(formatName));
    }

    // Load all agent YAMLs in domain
    List<AgentConfig> agentConfigs = loadAgentConfigs(domainPath + "/agents/");

    // Create and register agents
    for (AgentConfig config : agentConfigs) {
        Agent agent = new ConfigurableAgent(config, llmProvider, formatterRegistry);
        agentRegistry.registerAgent(agent);
    }

    log.info("Loaded domain '{}' with {} agents", domainConfig.name(), agentConfigs.size());
}
```

#### 3.2.4 Output Format Registry

**Before (Enum-like):**
```java
public String format(LLMResponse response, String formatType) {
    return switch (formatType.toLowerCase()) {
        case "technical" -> formatTechnical(content, response);
        case "business" -> formatBusiness(content, response);
        case "executive" -> formatExecutive(content, response);
        default -> content;
    };
}
```

**After (Registry Pattern):**
```java
@Component
public class OutputFormatterRegistry {
    private final Map<String, OutputFormatStrategy> formatters = new ConcurrentHashMap<>();

    public void registerFormat(String name, OutputFormatStrategy formatter) {
        formatters.put(name.toLowerCase(), formatter);
        log.info("Registered output format: {}", name);
    }

    public String format(LLMResponse response, String formatType) {
        OutputFormatStrategy formatter = formatters.get(formatType.toLowerCase());
        if (formatter == null) {
            log.warn("Unknown format type: {}. Returning raw content.", formatType);
            return response.content();
        }
        return formatter.format(response);
    }

    @PostConstruct
    public void registerBuiltInFormats() {
        // Register default formats
        registerFormat("technical", new TechnicalOutputFormatter());
        registerFormat("business", new BusinessOutputFormatter());
        registerFormat("executive", new ExecutiveOutputFormatter());
        registerFormat("raw", new RawOutputFormatter());
    }
}
```

**OutputFormatStrategy Interface:**
```java
public interface OutputFormatStrategy {
    String format(LLMResponse response);
}
```

**Example Domain-Specific Formatter:**
```java
public class ClinicalOutputFormatter implements OutputFormatStrategy {
    @Override
    public String format(LLMResponse response) {
        StringBuilder output = new StringBuilder();
        output.append("## Clinical Assessment\n\n");
        output.append(response.content());
        output.append("\n\n---\n");
        output.append("**Provider:** ").append(response.provider()).append("\n");
        output.append("**Confidence Level:** High\n");
        return output.toString();
    }
}
```

### 3.3 Configuration Examples

#### Software Engineering Domain (Backward Compatible)

**domains/software-engineering/domain.yaml:**
```yaml
name: "software-engineering"
version: "1.0.0"
description: "Software engineering agents for development workflows"
outputFormats:
  - technical
  - business
  - executive
agentDirectory: "agents/"
dependencies: []
```

**domains/software-engineering/agents/developer.yaml:**
```yaml
name: "Software Developer"
description: "Expert software developer specializing in code review, debugging, refactoring"
capabilities:
  - "Code review and quality assessment"
  - "Debugging and root cause analysis"
  - "Refactoring suggestions"
  - "Test case generation"
temperature: 0.7
maxTokens: 4096
outputFormat: "technical"
promptTemplate: |
  You are an expert Software Developer with deep knowledge of software engineering best practices.

  Your task: {task}
  Additional context: {context}

  Provide detailed technical analysis including:
  - Specific code issues with file paths and line numbers
  - Root cause analysis
  - Concrete fix suggestions with code examples
```

#### Healthcare Domain (New)

**domains/healthcare/domain.yaml:**
```yaml
name: "healthcare"
version: "1.0.0"
description: "Healthcare domain with diagnostic, treatment, and triage agents"
outputFormats:
  - clinical
  - patient-friendly
  - administrative
agentDirectory: "agents/"
dependencies: []
```

**domains/healthcare/agents/diagnostics.yaml:**
```yaml
name: "Diagnostic Specialist"
description: "AI assistant specializing in medical diagnostics and differential diagnosis"
capabilities:
  - "Symptom analysis"
  - "Differential diagnosis generation"
  - "Test recommendation"
  - "Risk assessment"
temperature: 0.3
maxTokens: 2048
outputFormat: "clinical"
promptTemplate: |
  You are a Diagnostic Specialist AI assistant trained in evidence-based medicine.

  Patient Presentation: {task}
  Medical History: {context}

  Provide a structured clinical assessment:
  1. Chief Complaint Summary
  2. Differential Diagnosis (ordered by probability)
  3. Recommended Diagnostic Tests
  4. Red Flags / Urgent Considerations
  5. Clinical Reasoning

  Note: This is for educational purposes only. All recommendations should be reviewed by a licensed physician.
```

**domains/healthcare/agents/treatment.yaml:**
```yaml
name: "Treatment Planner"
description: "AI assistant for treatment planning and care coordination"
capabilities:
  - "Treatment protocol recommendation"
  - "Medication management"
  - "Care coordination"
  - "Patient education"
temperature: 0.2
maxTokens: 3072
outputFormat: "clinical"
promptTemplate: |
  You are a Treatment Planning specialist focused on evidence-based care protocols.

  Diagnosis: {task}
  Patient Context: {context}

  Develop a comprehensive treatment plan:
  1. Primary Treatment Approach
  2. Medication Regimen (if applicable)
  3. Non-Pharmacological Interventions
  4. Monitoring Parameters
  5. Follow-up Schedule
  6. Patient Education Points

  Note: Treatment plans must be reviewed and approved by a licensed physician.
```

#### Legal Domain (New)

**domains/legal/domain.yaml:**
```yaml
name: "legal"
version: "1.0.0"
description: "Legal research, contract analysis, and compliance agents"
outputFormats:
  - legal-memo
  - client-summary
  - regulatory-report
agentDirectory: "agents/"
dependencies: []
```

**domains/legal/agents/contract.yaml:**
```yaml
name: "Contract Analyst"
description: "AI assistant for contract review and risk assessment"
capabilities:
  - "Contract clause analysis"
  - "Risk identification"
  - "Compliance verification"
  - "Redlining suggestions"
temperature: 0.2
maxTokens: 4096
outputFormat: "legal-memo"
promptTemplate: |
  You are a Contract Analysis specialist with expertise in commercial law.

  Contract Review Request: {task}
  Contract Context: {context}

  Provide a structured legal analysis:
  1. Executive Summary
  2. Key Terms Identified
  3. Risk Assessment (High/Medium/Low for each clause)
  4. Non-Standard Provisions
  5. Recommended Revisions
  6. Compliance Considerations

  Note: This analysis is for informational purposes. Final review must be conducted by a licensed attorney.
```

---

## 4. Refactoring Strategy

### 4.1 Core Principles

1. **Backward Compatibility First**
   - Existing SE agents must work without changes
   - Existing REST API endpoints unchanged
   - Existing YAML configs compatible

2. **Incremental Migration**
   - Refactor in phases, not "big bang"
   - Keep existing code until new system proven
   - Run tests after each phase

3. **Plugin Architecture**
   - Domains are self-contained
   - No cross-domain dependencies
   - Hot-loading of domains (future)

4. **Configuration Over Code**
   - All domain-specific logic in YAML
   - Code is 100% generic
   - No recompilation for new domains

### 4.2 Migration Approach

```
Current State          Transition              Target State
─────────────          ──────────              ────────────

13 Agent Classes  →    ConfigurableAgent  →    1 Agent Class
     +                      +                       +
Hardcoded Factory      DomainLoader           Plugin System
     +                      +                       +
3 Output Formats       Registry Pattern       N Output Formats
```

### 4.3 Deprecation Strategy

**Phase 1: Add New System (Coexist)**
- Add ConfigurableAgent alongside existing agents
- Add DomainLoader alongside AppConfig factory
- Add OutputFormatterRegistry alongside OutputFormatter
- Run both systems in parallel

**Phase 2: Migrate Existing SE Domain**
- Create domains/software-engineering/ plugin
- Move existing YAML configs to plugin structure
- Test both old and new systems produce identical results

**Phase 3: Deprecate Old System**
- Mark existing agent classes as @Deprecated
- Add migration guide to docs
- Remove hardcoded factory in next major version

**Phase 4: Remove Old Code (Major Version Bump)**
- Delete 13 agent classes
- Remove switch statement factory
- Clean up deprecated code

---

## 5. Implementation Phases

### Phase 1: Core Generic Infrastructure (Week 1)

**Objectives:**
- Create ConfigurableAgent class
- Implement DomainLoader
- Create OutputFormatterRegistry
- Refactor BaseAgent for full genericity

**Tasks:**

| # | Task | Effort | File Changes |
|---|------|--------|--------------|
| 1.1 | Create OutputFormatStrategy interface | 1h | +1 new file |
| 1.2 | Extract existing formats to strategy classes | 2h | +3 new files |
| 1.3 | Implement OutputFormatterRegistry | 2h | +1 new file |
| 1.4 | Update BaseAgent to use registry | 1h | modify BaseAgent.java |
| 1.5 | Create ConfigurableAgent class | 1h | +1 new file |
| 1.6 | Create DomainConfig model | 1h | +1 new file |
| 1.7 | Implement DomainLoader | 4h | +1 new file |
| 1.8 | Update AppConfig to support both systems | 2h | modify AppConfig.java |
| 1.9 | Write unit tests | 4h | +5 test files |
| 1.10 | Write integration tests | 4h | +3 test files |

**Total: 22 hours (3 days)**

**Key Deliverables:**
- ✅ ConfigurableAgent.java (core/ConfigurableAgent.java)
- ✅ DomainLoader.java (core/DomainLoader.java)
- ✅ OutputFormatterRegistry.java (core/OutputFormatterRegistry.java)
- ✅ OutputFormatStrategy.java (core/OutputFormatStrategy.java)
- ✅ DomainConfig.java (model/DomainConfig.java)
- ✅ Tests passing for new system

### Phase 2: Domain Plugin System (Week 2)

**Objectives:**
- Create domain plugin directory structure
- Migrate SE agents to plugin format
- Support multiple domain loading
- Validate backward compatibility

**Tasks:**

| # | Task | Effort | File Changes |
|---|------|--------|--------------|
| 2.1 | Design domain directory structure | 1h | docs |
| 2.2 | Create DomainManager service | 3h | +1 new file |
| 2.3 | Implement domain discovery/scanning | 2h | update DomainLoader |
| 2.4 | Create domains/software-engineering/ | 2h | +1 domain.yaml |
| 2.5 | Move existing agent YAMLs to plugin | 1h | move 13 files |
| 2.6 | Create healthcare example domain | 3h | +5 YAML files |
| 2.7 | Create legal example domain | 3h | +4 YAML files |
| 2.8 | Implement domain-specific format loaders | 3h | update DomainLoader |
| 2.9 | Add REST API for domain management | 4h | +1 controller |
| 2.10 | Add CLI commands for domain operations | 2h | update CLI |
| 2.11 | Write domain plugin tests | 4h | +4 test files |
| 2.12 | Validate backward compatibility | 4h | update integration tests |

**Total: 32 hours (4 days)**

**Key Deliverables:**
- ✅ DomainManager.java (core/DomainManager.java)
- ✅ domains/software-engineering/ (backward compatible)
- ✅ domains/healthcare/ (example)
- ✅ domains/legal/ (example)
- ✅ DomainController.java (REST API for domain mgmt)
- ✅ All existing tests passing

### Phase 3: Output Format Extensions (Days 9-11)

**Objectives:**
- Make output formats fully extensible
- Create domain-specific format examples
- Document format creation guide

**Tasks:**

| # | Task | Effort | File Changes |
|---|------|--------|--------------|
| 3.1 | Create ClinicalOutputFormatter | 2h | +1 new file |
| 3.2 | Create PatientFriendlyOutputFormatter | 2h | +1 new file |
| 3.3 | Create LegalMemoOutputFormatter | 2h | +1 new file |
| 3.4 | Create ClientSummaryOutputFormatter | 2h | +1 new file |
| 3.5 | Add format validation | 2h | update registry |
| 3.6 | Add format metadata API | 2h | +1 endpoint |
| 3.7 | Write format creation guide | 3h | +1 doc file |
| 3.8 | Write format tests | 3h | +4 test files |

**Total: 18 hours (3 days)**

**Key Deliverables:**
- ✅ 4 new domain-specific formatters
- ✅ Format validation
- ✅ doc/guide/creating-output-formats.md
- ✅ All format tests passing

### Phase 4: Testing, Documentation & Migration (Days 12-15)

**Objectives:**
- Comprehensive testing
- Complete documentation
- Migration guide
- Performance validation

**Tasks:**

| # | Task | Effort | File Changes |
|---|------|--------|--------------|
| 4.1 | End-to-end testing (all domains) | 4h | +3 E2E tests |
| 4.2 | Performance testing (compare old vs new) | 3h | +1 Gatling test |
| 4.3 | Load testing (multi-domain) | 2h | update Gatling |
| 4.4 | Create migration guide | 4h | +1 doc file |
| 4.5 | Update architecture documentation | 3h | update docs |
| 4.6 | Create domain plugin developer guide | 4h | +1 doc file |
| 4.7 | Update API documentation | 2h | update api-design.md |
| 4.8 | Create example domain templates | 3h | +3 template files |
| 4.9 | Update README with generic description | 1h | update README.md |
| 4.10 | Code review and cleanup | 4h | refactor |

**Total: 30 hours (4 days)**

**Key Deliverables:**
- ✅ doc/guide/migration-to-generic.md
- ✅ doc/guide/creating-domain-plugins.md
- ✅ templates/domain-template/
- ✅ Updated architecture docs
- ✅ All tests passing (100% backward compatible)

---

## 6. Migration Path

### 6.1 For Existing Users (SE Domain)

**No Action Required**
- Existing REST API calls work unchanged
- Existing YAML configs work unchanged
- Existing CLI commands work unchanged
- All 13 SE agents available as before

**Behind the Scenes:**
- SE agents now loaded from domains/software-engineering/ plugin
- Uses ConfigurableAgent instead of hardcoded classes
- Functionally identical behavior

### 6.2 For New Domain Developers

**Step 1: Create Domain Structure**
```bash
mkdir -p domains/my-domain/agents/
```

**Step 2: Define Domain**
```yaml
# domains/my-domain/domain.yaml
name: "my-domain"
version: "1.0.0"
description: "My custom domain description"
outputFormats:
  - custom-format-1
  - custom-format-2
agentDirectory: "agents/"
```

**Step 3: Create Agent Configs**
```yaml
# domains/my-domain/agents/my-agent.yaml
name: "My Agent"
description: "Agent description"
capabilities:
  - "Capability 1"
  - "Capability 2"
temperature: 0.7
maxTokens: 2048
outputFormat: "custom-format-1"
promptTemplate: |
  You are {role}.
  Task: {task}
  Context: {context}

  Provide your analysis...
```

**Step 4: (Optional) Create Custom Output Formats**
```java
public class CustomFormat1 implements OutputFormatStrategy {
    @Override
    public String format(LLMResponse response) {
        return "Custom formatted: " + response.content();
    }
}
```

**Step 5: Load Domain**
```bash
# Auto-loaded on startup from domains/ directory
# Or via REST API:
POST /api/domains/load
{
  "domainPath": "domains/my-domain"
}
```

### 6.3 Deprecation Timeline

| Version | Changes | Status |
|---------|---------|--------|
| **v0.2.0** | Add generic system (coexist) | Phase 1-4 complete |
| **v0.3.0** | Mark old agents @Deprecated | Migration guide published |
| **v0.4.0** | Switch default to generic system | Old system opt-in |
| **v1.0.0** | Remove hardcoded agents | Breaking change |

**Recommendation:** Adopt generic system in v0.2.0, migrate by v0.4.0.

---

## 7. Risk Assessment

### 7.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Backward incompatibility** | Medium | High | Extensive integration tests, parallel systems |
| **Performance regression** | Low | Medium | Performance benchmarks (old vs new) |
| **YAML config complexity** | Medium | Low | Template generator, validation |
| **Output format conflicts** | Low | Low | Format registry with namespacing |
| **Domain loading failures** | Medium | Medium | Graceful fallback, detailed error messages |

### 7.2 Business Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **User migration resistance** | Low | Medium | Maintain backward compatibility |
| **Documentation gaps** | Medium | Medium | Comprehensive migration guide |
| **Support burden** | Low | Low | Clear examples, domain templates |

### 7.3 Mitigation Strategies

**Backward Compatibility Testing:**
```java
@Test
public void testBackwardCompatibility_SEAgents() {
    // Load old system
    Agent oldDeveloper = oldFactory.create("Software Developer");

    // Load new system
    Agent newDeveloper = domainLoader.loadAgent("software-engineering", "Software Developer");

    // Same task
    TaskRequest request = new TaskRequest("Software Developer", "Review PR #123", Map.of());

    // Results should be functionally equivalent
    TaskResult oldResult = oldDeveloper.executeTask(request);
    TaskResult newResult = newDeveloper.executeTask(request);

    assertThat(newResult.output()).isEqualToIgnoringWhitespace(oldResult.output());
}
```

**Performance Benchmarking:**
```java
@Test
public void performanceComparison_OldVsNew() {
    // Benchmark old system
    long oldDuration = benchmarkOldSystem(1000);

    // Benchmark new system
    long newDuration = benchmarkNewSystem(1000);

    // New system should not be >10% slower
    assertThat(newDuration).isLessThan(oldDuration * 1.1);
}
```

---

## 8. Success Metrics

### 8.1 Technical Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Backward Compatibility** | 100% | All existing tests pass unchanged |
| **Performance Impact** | <5% slower | JMH benchmarks (old vs new) |
| **Code Reduction** | -30% agent code | Lines of code in agents/ |
| **Test Coverage** | >80% | JaCoCo coverage report |
| **Domain Load Time** | <1s per domain | Startup time measurement |

### 8.2 Usability Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **New Domain Creation Time** | <30 min | Timed user study |
| **Documentation Completeness** | 100% | All sections complete |
| **Example Domains** | 3+ | SE, healthcare, legal |
| **Community Adoption** | 5+ domains in 6 months | GitHub PR tracking |

### 8.3 Business Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Market Expansion** | 3+ new domains | Deployed domains count |
| **Time to Market** | 80% reduction | New domain vs old fork approach |
| **Maintenance Cost** | 50% reduction | Dev hours per domain |

---

## 9. References

### 9.1 Design Patterns

- **Strategy Pattern**: OutputFormatStrategy for pluggable formatters
- **Registry Pattern**: AgentRegistry, OutputFormatterRegistry for runtime lookup
- **Plugin Architecture**: Domain plugins as self-contained modules
- **Factory Pattern**: DomainLoader creates agents dynamically
- **Template Method**: BaseAgent defines execution pipeline

### 9.2 Related Documentation

- [Architecture Design](architecture.md) - Current architecture
- [Developer Guide](../4-development/developer-guide.md) - Development setup
- [API Design](api-design.md) - REST API specification

### 9.3 External Resources

- [Spring Boot Plugin Architecture](https://www.baeldung.com/spring-plugin) - Plugin system examples
- [Martin Fowler: Plugin Pattern](https://martinfowler.com/articles/injection.html) - Dependency injection
- [Strategy Pattern](https://refactoring.guru/design-patterns/strategy) - Strategy pattern guide

---

## 10. Appendix

### 10.1 File Structure After Refactoring

```
role-manager-app/
├── src/main/java/com/rolemanager/
│   ├── core/
│   │   ├── BaseAgent.java                    (MODIFIED: use OutputFormatterRegistry)
│   │   ├── ConfigurableAgent.java            (NEW: single generic agent)
│   │   ├── AgentRegistry.java                (UNCHANGED)
│   │   ├── RoleManager.java                  (UNCHANGED)
│   │   ├── DomainLoader.java                 (NEW: loads domain plugins)
│   │   ├── DomainManager.java                (NEW: manages multiple domains)
│   │   ├── OutputFormatterRegistry.java      (NEW: registry for formats)
│   │   └── OutputFormatStrategy.java         (NEW: interface)
│   │
│   ├── formatters/                           (NEW: format implementations)
│   │   ├── TechnicalOutputFormatter.java     (NEW: extracted from OutputFormatter)
│   │   ├── BusinessOutputFormatter.java      (NEW: extracted)
│   │   ├── ExecutiveOutputFormatter.java     (NEW: extracted)
│   │   ├── RawOutputFormatter.java           (NEW: passthrough)
│   │   ├── ClinicalOutputFormatter.java      (NEW: healthcare)
│   │   ├── PatientFriendlyFormatter.java     (NEW: healthcare)
│   │   └── LegalMemoOutputFormatter.java     (NEW: legal)
│   │
│   ├── agents/                               (DEPRECATED: mark @Deprecated)
│   │   ├── DeveloperAgent.java               (@Deprecated)
│   │   ├── QAAgent.java                      (@Deprecated)
│   │   └── ... (11 more deprecated agents)
│   │
│   ├── api/
│   │   ├── RoleController.java               (UNCHANGED)
│   │   ├── TaskController.java               (UNCHANGED)
│   │   └── DomainController.java             (NEW: domain management API)
│   │
│   ├── config/
│   │   ├── AppConfig.java                    (MODIFIED: support both systems)
│   │   └── AgentConfigLoader.java            (MODIFIED: load from domains/)
│   │
│   └── model/
│       └── DomainConfig.java                 (NEW: domain metadata)
│
├── domains/                                   (NEW: domain plugins)
│   ├── software-engineering/
│   │   ├── domain.yaml
│   │   └── agents/
│   │       ├── developer.yaml                (MOVED from config/agents/)
│   │       ├── qa.yaml
│   │       └── ... (13 SE agents)
│   │
│   ├── healthcare/                           (NEW: example domain)
│   │   ├── domain.yaml
│   │   └── agents/
│   │       ├── diagnostics.yaml
│   │       ├── treatment.yaml
│   │       ├── triage.yaml
│   │       └── pharmacy.yaml
│   │
│   └── legal/                                (NEW: example domain)
│       ├── domain.yaml
│       └── agents/
│           ├── contract.yaml
│           ├── compliance.yaml
│           ├── research.yaml
│           └── litigation.yaml
│
├── templates/                                 (NEW: domain templates)
│   └── domain-template/
│       ├── domain.yaml.template
│       └── agents/
│           └── example-agent.yaml.template
│
└── doc/
    ├── 3-design/
    │   ├── generic-refactoring-plan.md       (THIS FILE)
    │   └── architecture.md                   (UPDATE: new architecture)
    │
    └── guide/                                (NEW: user guides)
        ├── migration-to-generic.md           (NEW: migration guide)
        ├── creating-domain-plugins.md        (NEW: plugin development)
        └── creating-output-formats.md        (NEW: format development)
```

### 10.2 Code Deletion Summary

**Files to Delete (v1.0.0):**
- src/main/java/com/rolemanager/agents/DeveloperAgent.java
- src/main/java/com/rolemanager/agents/QAAgent.java
- src/main/java/com/rolemanager/agents/SecurityAgent.java
- src/main/java/com/rolemanager/agents/ManagerAgent.java
- src/main/java/com/rolemanager/agents/DevOpsAgent.java
- src/main/java/com/rolemanager/agents/ProductOwnerAgent.java
- src/main/java/com/rolemanager/agents/SREAgent.java
- src/main/java/com/rolemanager/agents/DataEngineerAgent.java
- src/main/java/com/rolemanager/agents/ComplianceAgent.java
- src/main/java/com/rolemanager/agents/ExecutiveAgent.java
- src/main/java/com/rolemanager/agents/UIUXDesignerAgent.java
- src/main/java/com/rolemanager/agents/TechnicalWriterAgent.java
- src/main/java/com/rolemanager/agents/CustomerSupportAgent.java

**Total: 13 files deleted (1,500+ lines of code removed)**

### 10.3 API Changes

**New Endpoints (DomainController):**
```
GET  /api/domains                List all loaded domains
GET  /api/domains/{name}         Get domain details
POST /api/domains/load           Load domain from path
DELETE /api/domains/{name}       Unload domain
GET  /api/domains/{name}/agents  List agents in domain
```

**New Endpoints (FormatterController):**
```
GET  /api/formats                List available output formats
GET  /api/formats/{name}         Get format details
POST /api/formats/register       Register custom format
```

**Unchanged Endpoints:**
```
GET  /api/roles                  (unchanged)
GET  /api/roles/{name}           (unchanged)
POST /api/tasks/execute          (unchanged)
POST /api/tasks/multi-agent      (unchanged)
```

---

**Last Updated:** 2025-10-20
**Document Status:** Draft
**Review Date:** 2025-10-27
**Approved By:** [Pending Review]
