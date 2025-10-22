# AgentUnit Documentation Overview

**Date:** 2025-10-22
**Version:** 1.0
**Module:** ade-agent-platform-agentunit

---

## Quick Navigation

### By User Type
- **First-Time User** → [README.md](../README.md) → [Utilities vs Annotations Guide](guide/utilities-vs-annotations-testing.md)
- **Framework Developer** → [Utilities vs Annotations Guide](guide/utilities-vs-annotations-testing.md) → Migration Guide section
- **Test Author** → [README.md Usage Section](../README.md#usage) → [Best Practices](guide/utilities-vs-annotations-testing.md#best-practices)
- **Contributor** → [README.md Contributing](../README.md#contributing) → [Common Pitfalls](guide/utilities-vs-annotations-testing.md#common-pitfalls)

### By Task
- **Getting Started** → [README.md](../README.md)
- **Understanding Design Philosophy** → [Utilities vs Annotations Guide](guide/utilities-vs-annotations-testing.md)
- **Migrating from Mockito** → [Migration Guide](guide/utilities-vs-annotations-testing.md#migration-guide)
- **Learning Best Practices** → [Best Practices](guide/utilities-vs-annotations-testing.md#best-practices)
- **Troubleshooting** → [Common Pitfalls](guide/utilities-vs-annotations-testing.md#common-pitfalls)

---

## Documentation Structure

```
ade-agent-platform-agentunit/
├── README.md                                    # Main documentation (482 lines)
└── doc/
    ├── overview.md                              # This file - documentation index
    └── guide/
        └── utilities-vs-annotations-testing.md  # Core philosophy guide (803 lines)
```

---

## File Catalog

### Root Documentation

| File | Size | Purpose | Read Time |
|------|------|---------|-----------|
| [README.md](../README.md) | 482 lines | Main module documentation, API reference, usage examples | 15 min |

**Contents:**
- Module overview and purpose
- Complete API documentation for all utilities
- Usage examples for all features
- Maven dependency configuration
- Migration guide from duplicated utilities

**When to read:**
- ✅ First time using AgentUnit
- ✅ Looking for API reference
- ✅ Need usage examples
- ✅ Adding AgentUnit to a module

---

### Guide Documentation

| File | Size | Purpose | Read Time |
|------|------|---------|-----------|
| [utilities-vs-annotations-testing.md](guide/utilities-vs-annotations-testing.md) | 803 lines | Philosophy guide comparing utilities-based vs annotation-based testing | 25 min |

**Contents:**
- Core design philosophy
- Detailed comparison tables (4 aspects)
- Real code examples (3 scenarios)
- Migration guide (4 steps)
- Best practices (5 patterns)
- Common pitfalls (4 mistakes)
- References (9 sources)

**When to read:**
- ✅ Understanding why AgentUnit uses utilities
- ✅ Migrating from Mockito/Spring Test
- ✅ Deciding between approaches
- ✅ Learning testing best practices
- ✅ Contributing to AgentUnit

---

## Use Case-Driven Paths

### Path 1: New Developer Onboarding

**Goal:** Get productive with AgentUnit in 30 minutes

1. **Read README.md (15 min)**
   - Overview and purpose
   - Maven dependency
   - Example test section

2. **Skim Utilities vs Annotations Guide (10 min)**
   - Read TL;DR section
   - Scan code examples
   - Review quick reference tables

3. **Write First Test (5 min)**
   ```java
   @Test
   void myFirstTest() {
       MockLLMProvider provider = new MockLLMProvider()
           .withResponseContent("Test");

       MockAgent agent = new MockAgent("test-agent");
       TaskResult result = agent.executeTask(TestData.validTaskRequest());

       assertThat(result.success()).isTrue();
   }
   ```

---

### Path 2: Migrating from Mockito

**Goal:** Convert existing Mockito tests to AgentUnit

1. **Read Migration Guide (10 min)**
   - [Migration Guide Section](guide/utilities-vs-annotations-testing.md#migration-guide)
   - Focus on 4-step conversion process

2. **Review Code Examples (5 min)**
   - Before/after comparisons
   - [Code Examples Section](guide/utilities-vs-annotations-testing.md#code-examples)

3. **Apply to Your Tests (varies)**
   - Replace `@Mock` with `new MockXxx()`
   - Replace `@InjectMocks` with constructors
   - Replace `when().thenReturn()` with fluent API

4. **Verify with Best Practices (5 min)**
   - [Best Practices Section](guide/utilities-vs-annotations-testing.md#best-practices)
   - Check for common pitfalls

---

### Path 3: Framework Integration (Quarkus/Micronaut)

**Goal:** Integrate AgentUnit with DI frameworks

1. **Read Framework-Specific Section in README (5 min)**
   - [Quarkus Test Utilities](../README.md#quarkus-test-utilities)
   - [Micronaut Test Utilities](../README.md#micronaut-test-utilities)

2. **Understand Hybrid Approach (5 min)**
   - [Hybrid Approach Section](guide/utilities-vs-annotations-testing.md#hybrid-approach-recommended)
   - Framework annotations at boundaries only

3. **Review Framework Integration Example (3 min)**
   - [Example 3: Framework Integration](guide/utilities-vs-annotations-testing.md#example-3-framework-integration)

4. **Implement in Your Tests (varies)**
   - Use framework annotations for real services
   - Use AgentUnit utilities for test data

---

### Path 4: Contributing to AgentUnit

**Goal:** Add new test utilities or features

1. **Read Contributing Section (3 min)**
   - [Contributing Section](../README.md#contributing)

2. **Understand Core Philosophy (10 min)**
   - [Core Philosophy Section](guide/utilities-vs-annotations-testing.md#core-philosophy)
   - No framework dependencies in utilities

3. **Review Best Practices (10 min)**
   - [Best Practices Section](guide/utilities-vs-annotations-testing.md#best-practices)
   - Follow existing patterns

4. **Check Common Pitfalls (5 min)**
   - [Common Pitfalls Section](guide/utilities-vs-annotations-testing.md#common-pitfalls)
   - Avoid these mistakes

---

## Quick Reference by Topic

### Testing Approach
- **Philosophy** → [Core Philosophy](guide/utilities-vs-annotations-testing.md#core-philosophy)
- **Comparison** → [Detailed Comparison](guide/utilities-vs-annotations-testing.md#detailed-comparison)
- **When to Use** → [When to Use Each Approach](guide/utilities-vs-annotations-testing.md#when-to-use-each-approach)

### Getting Started
- **Installation** → [Maven Dependency](../README.md#maven-dependency)
- **First Test** → [Example Test](../README.md#example-test)
- **Usage Examples** → [Available Test Utilities](../README.md#available-test-utilities)

### API Reference
- **MockAgent** → [MockAgent Section](../README.md#mockagent)
- **MockLLMProvider** → [MockLLMProvider Section](../README.md#mocklllmprovider)
- **TestData** → [TestData Section](../README.md#testdata)
- **Builders** → [Fluent Builders Section](../README.md#fluent-builders)
- **Assertions** → [Custom Assertions Section](../README.md#custom-assertions)
- **Base Classes** → [Base Test Classes Section](../README.md#base-test-classes)

### Framework Integration
- **Quarkus** → [Quarkus Test Utilities](../README.md#quarkus-test-utilities)
- **Micronaut** → [Micronaut Test Utilities](../README.md#micronaut-test-utilities)
- **Spring Boot** → [When to Use Annotation-Based](guide/utilities-vs-annotations-testing.md#use-annotation-based-when)

### Migration
- **From Mockito** → [Migration Guide](guide/utilities-vs-annotations-testing.md#migration-guide)
- **From Duplicated Utilities** → [Migration from Duplicated Utilities](../README.md#migration-from-duplicated-utilities)

### Best Practices & Troubleshooting
- **Best Practices** → [Best Practices](guide/utilities-vs-annotations-testing.md#best-practices)
- **Common Pitfalls** → [Common Pitfalls](guide/utilities-vs-annotations-testing.md#common-pitfalls)

---

## Documentation Statistics

| Metric | Value |
|--------|-------|
| **Total Documentation Files** | 2 files |
| **Total Lines** | 1,285 lines |
| **Total Reading Time** | ~40 minutes |
| **Guide Documents** | 1 file |
| **API References** | 1 file (README) |
| **Code Examples** | 15+ examples |
| **Comparison Tables** | 5 tables |
| **Reference Sources** | 9 authoritative sources |

---

## Contributing to Documentation

When adding or updating documentation:

1. **Follow CLAUDE.md Standards**
   - Use kebab-case for filenames
   - Add TL;DR to guide documents
   - Include references section
   - Add metadata (date, version)

2. **Update This Index**
   - Add new files to file catalog
   - Update statistics
   - Add to appropriate use case paths
   - Update quick reference links

3. **Maintain Quality**
   - Fact-check all claims
   - Include code examples
   - Add reading time estimates
   - Keep TOC updated for files >500 lines

---

## Related Documentation

### Parent Project
- **ade Agent Platform** → `/home/developer/ade-agent-platform/README.md`
- **ade Agent SDK** → `/home/developer/ade-agent-platform/ade-agent/README.md`

### Implementation Modules
- **Core Module** → `/home/developer/ade-agent-platform/ade-agent-platform-core/`
- **Spring Boot Module** → `/home/developer/ade-agent-platform/ade-agent-platform-spring-boot/`
- **Quarkus Module** → `/home/developer/ade-agent-platform/ade-agent-platform-quarkus/`
- **Micronaut Module** → `/home/developer/ade-agent-platform/ade-agent-platform-micronaut/`

---

## Quick Start Summary

**For Busy Developers (5 minutes):**

1. **Add dependency** to `pom.xml`:
   ```xml
   <dependency>
       <groupId>adeengineer.dev</groupId>
       <artifactId>ade-agent-platform-agentunit</artifactId>
       <scope>test</scope>
   </dependency>
   ```

2. **Write test** using utilities:
   ```java
   @Test
   void test() {
       MockLLMProvider llm = new MockLLMProvider();
       MockAgent agent = new MockAgent("test");
       TaskResult result = agent.executeTask(TestData.validTaskRequest());
       assertThat(result).isSuccessful();
   }
   ```

3. **Read more** when needed:
   - API details → [README.md](../README.md)
   - Philosophy → [Utilities vs Annotations](guide/utilities-vs-annotations-testing.md)

---

**Last Updated:** 2025-10-22
**Version:** 1.0
