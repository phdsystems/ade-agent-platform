# Test Framework Extraction Plan - AgentUnit

**Date:** 2025-10-21
**Version:** 1.0
**Feature Branch:** `feature/test-framework-agentunit`

## TL;DR

**Goal**: Extract duplicated test utilities into reusable `AgentUnit` module. **Problem**: MockAgent, TestData, TestLLMProvider duplicated across core and Spring Boot (3x copies, 100% identical). **Solution**: Create test-scoped dependency that all modules can use. **Benefit**: Single source of truth for test utilities, easier maintenance, consistent testing patterns.

---

## Table of Contents

1. [Problem Statement](#problem-statement)
2. [Current State Analysis](#current-state-analysis)
3. [Proposed Solution](#proposed-solution)
4. [Module Structure](#module-structure)
5. [Implementation Plan](#implementation-plan)
6. [Migration Strategy](#migration-strategy)
7. [Testing Strategy](#testing-strategy)
8. [Success Criteria](#success-criteria)
9. [Risks and Mitigation](#risks-and-mitigation)

---

## Problem Statement

### Code Duplication

**Duplicated Test Utilities** (3 locations):
- `ade-agent-platform-core/src/test/java/dev/adeengineer/platform/testutil/`
- `ade-agent-platform-spring-boot/src/test/java/dev/adeengineer/platform/spring/testutil/`
- `src/test/java/dev/adeengineer/platform/testutil/` (legacy)

**Files Duplicated** (100% identical except package name):
1. **MockAgent.java** (95 lines) - Mock implementation of Agent interface
2. **TestData.java** (134 lines) - Factory methods for test data
3. **TestLLMProvider.java** (118 lines) - Mock LLM provider for testing

**Impact**:
- **347 lines of duplicated code** across 3 locations
- Maintenance burden: Bug fixes must be applied 3 times
- Inconsistency risk: Changes may diverge over time
- No shared testing patterns across modules

### Usage Analysis

**Test Utility Usage** (ade-agent-platform-core):
- **MockAgent**: Used in 74 locations across 18 test files
- **TestData**: Used in integration and unit tests
- **TestLLMProvider**: Used for LLM-dependent tests

**Spring Boot Tests**:
- **BaseE2ETest.java**: Abstract base class for E2E tests (84 lines)
- **E2ETestConfiguration.java**: Spring test configuration
- Duplicates same utilities with different package

**Common Pattern**:
```java
@BeforeEach
void setUp() {
    registry = new AgentRegistry();
    mockAgent = new MockAgent("test-agent");
    mockLLMProvider = new TestLLMProvider();
}
```

This pattern appears in **38 setUp methods** across modules.

---

## Current State Analysis

### Test File Statistics

| Module | Test Files | Test Utilities | E2E Tests |
|--------|-----------|---------------|-----------|
| **core** | 20 | MockAgent, TestData, TestLLMProvider | 3 integration tests |
| **spring-boot** | 29 | MockAgent, TestData, TestLLMProvider (duplicated) | 15 E2E tests |
| **quarkus** | 0 | None | None |
| **micronaut** | 0 | None | None |
| **TOTAL** | 49 | 6 files (3 duplicated) | 18 tests |

### Test Coverage

```bash
# Core module: 227 unit tests
# Spring Boot module: 17 tests (3 LLM tests, 14 E2E tests disabled)
# Total: 244 tests
```

### Dependency Graph (Current)

```
ade-agent-platform-core/src/test
    └── testutil/
        ├── MockAgent.java
        ├── TestData.java
        └── TestLLMProvider.java

ade-agent-platform-spring-boot/src/test
    └── testutil/
        ├── MockAgent.java (DUPLICATE)
        ├── TestData.java (DUPLICATE)
        └── TestLLMProvider.java (DUPLICATE)
    └── e2e/
        ├── BaseE2ETest.java
        └── E2ETestConfiguration.java
```

### Pain Points

1. **Maintenance**: Bug fix in MockAgent requires 3 identical changes
2. **Inconsistency**: Spring Boot utilities diverge from core
3. **No Quarkus/Micronaut tests**: No test utilities = no test coverage
4. **No shared base classes**: E2E test patterns only in Spring Boot
5. **No test builders**: Verbose test setup code

---

## Proposed Solution

### Create `AgentUnit` Module

**Artifact**: `ade-agent-platform-agentunit`

**Purpose**: Reusable, framework-agnostic test utilities for all modules

**Scope**: `test` scope dependency (never used in production)

**Benefits**:
- ✅ Single source of truth for test utilities
- ✅ Framework-agnostic (works with JUnit, Spring Test, Quarkus Test, Micronaut Test)
- ✅ Eliminates 347 lines of duplication
- ✅ Enables consistent testing patterns across all modules
- ✅ Provides foundation for Quarkus/Micronaut test coverage

---

## Module Structure

### New Module: `AgentUnit` (`ade-agent-platform-agentunit`)

```
ade-agent-platform-agentunit/
├── pom.xml                                    # Test-scoped module
└── src/main/java/                             # Main source (for test utilities)
    └── dev/adeengineer/platform/test/
        ├── mock/                              # Mock implementations
        │   ├── MockAgent.java                 # Moved from testutil
        │   ├── MockLLMProvider.java           # Renamed from TestLLMProvider
        │   └── MockEmbeddingsProvider.java    # NEW
        ├── builder/                           # Test data builders
        │   ├── AgentConfigBuilder.java        # NEW - Fluent builder
        │   ├── TaskRequestBuilder.java        # NEW - Fluent builder
        │   └── TaskResultBuilder.java         # NEW - Fluent builder
        ├── factory/                           # Test data factories
        │   └── TestData.java                  # Moved from testutil
        ├── assertion/                         # Custom assertions
        │   ├── AgentAssertions.java           # NEW - AssertJ custom
        │   └── TaskResultAssertions.java      # NEW - AssertJ custom
        └── base/                              # Base test classes
            ├── BaseAgentTest.java             # NEW - Common setup
            ├── BaseIntegrationTest.java       # NEW - Integration patterns
            └── BaseE2ETest.java               # Extracted from Spring Boot
```

### Why `src/main/java` for Test Utilities?

Test framework utilities go in `src/main/java` (NOT `src/test/java`) because:
- They are **consumed by other modules' tests** as a dependency
- `src/test/java` code is NOT included in JAR artifacts
- Maven/Gradle can only import classes from `src/main/java`

**Dependency Declaration** (other modules):
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-agentunit</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>  <!-- Only available in test classpath -->
</dependency>
```

### Module Dependencies

```
ade-agent-platform-core
    └── (test scope) → ade-agent-platform-agentunit

ade-agent-platform-spring-boot
    └── (test scope) → ade-agent-platform-agentunit

ade-agent-platform-quarkus
    └── (test scope) → ade-agent-platform-agentunit

ade-agent-platform-micronaut
    └── (test scope) → ade-agent-platform-agentunit
```

---

## Implementation Plan

### Phase 1: Create Test Framework Module

**Tasks**:
1. ✅ Create `ade-agent-platform-agentunit/pom.xml`
   - Packaging: `jar`
   - Scope: Intended for `test` scope consumers
   - Dependencies: JUnit 5, AssertJ, Mockito, SDK interfaces

2. ✅ Move existing utilities to new package structure:
   - `MockAgent.java` → `dev.adeengineer.platform.test.mock.MockAgent`
   - `TestLLMProvider.java` → `dev.adeengineer.platform.test.mock.MockLLMProvider`
   - `TestData.java` → `dev.adeengineer.platform.test.factory.TestData`

3. ✅ Add to parent POM modules section

**Estimated Time**: 1 hour

### Phase 2: Create New Test Utilities

**New Utilities**:

1. **Fluent Builders** (following builder pattern):
   ```java
   // Example: AgentConfigBuilder
   AgentConfig config = AgentConfigBuilder.builder()
       .name("Developer")
       .description("Software development agent")
       .capabilities("coding", "testing")
       .temperature(0.7)
       .maxTokens(1000)
       .build();
   ```

2. **Custom Assertions** (AssertJ extensions):
   ```java
   // Example: AgentAssertions
   assertThat(taskResult)
       .isSuccessful()
       .hasAgentName("Developer")
       .hasOutputContaining("completed");
   ```

3. **Base Test Classes**:
   - `BaseAgentTest` - Common setup for agent tests
   - `BaseIntegrationTest` - Integration test patterns
   - `BaseE2ETest` - Framework-agnostic E2E base (extracted from Spring Boot)

**Estimated Time**: 3 hours

### Phase 3: Migrate Existing Tests

**Migration Steps** (per module):

1. Add AgentUnit dependency to `pom.xml`
2. Update imports:
   ```java
   // Old
   import dev.adeengineer.platform.testutil.MockAgent;

   // New
   import dev.adeengineer.platform.test.mock.MockAgent;
   ```
3. Delete duplicated testutil packages
4. Run tests to verify

**Migration Order**:
1. `ade-agent-platform-core` (20 test files)
2. `ade-agent-platform-spring-boot` (29 test files)
3. Legacy `src/test` (if still exists)

**Estimated Time**: 2 hours

### Phase 4: Documentation and Examples

**Deliverables**:
1. `ade-agent-platform-agentunit/README.md` - Usage guide
2. Example test class showing all utilities
3. Update `doc/4-development/testing-guide.md`

**Estimated Time**: 1 hour

---

## Migration Strategy

### Backward Compatibility

**Option 1: Clean Break** (RECOMMENDED)
- Remove duplicated utilities immediately
- Update all imports in one commit
- Fast, clean, forces consistency

**Option 2: Gradual Migration**
- Keep duplicates temporarily
- Deprecate with `@Deprecated` annotations
- Remove in 0.3.0 release

**Decision**: Choose **Option 1** because:
- Small codebase (49 test files total)
- All tests pass currently
- Can migrate and verify in single PR

### Migration Checklist

**Per Module**:
- [ ] Add `ade-agent-platform-agentunit` test dependency to `pom.xml`
- [ ] Find and replace imports:
  ```bash
  # Example for core module
  find ade-agent-platform-core/src/test -name "*.java" -exec sed -i \
    's/dev.adeengineer.platform.testutil/dev.adeengineer.platform.test.mock/g' {} \;
  ```
- [ ] Delete `src/test/java/.../testutil/` directories
- [ ] Run tests: `mvn clean test`
- [ ] Verify all tests pass

**Validation**:
```bash
# Ensure no duplicates remain
find . -path "*/testutil/*.java" -not -path "*/target/*"

# Should return: (empty - all migrated)
```

---

## Testing Strategy

### Validate Test Framework Module

1. **Unit Tests** for test utilities:
   - `MockAgentTest.java` - Verify mock behavior
   - `AgentConfigBuilderTest.java` - Test builder patterns
   - `AgentAssertionsTest.java` - Validate custom assertions

2. **Integration Tests**:
   - Use test framework utilities to test themselves
   - Dogfooding: Core tests validate framework utilities work

3. **Coverage Target**:
   - Test framework module: 90%+ coverage
   - Ensure utilities are reliable

### Regression Testing

**After Migration**:
```bash
# Run full test suite
mvn clean verify

# Expected results:
# - core: 227 tests passing
# - spring-boot: 17 tests passing (E2E still disabled)
# - test-framework: NEW tests passing
# - Total: 244+ tests passing
```

---

## Success Criteria

### Functional Requirements

- [ ] All 49 existing test files pass with new imports
- [ ] Zero duplicated test utilities across modules
- [ ] New builders and assertions functional
- [ ] Documentation complete

### Non-Functional Requirements

- [ ] Build time unchanged or improved
- [ ] Test execution time unchanged
- [ ] All modules can depend on test-framework
- [ ] Framework-agnostic (no Spring/Quarkus/Micronaut dependencies)

### Code Quality

- [ ] 90%+ test coverage in test-framework module
- [ ] Spotless formatting passes
- [ ] No compiler warnings
- [ ] Javadoc for all public APIs

---

## Risks and Mitigation

### Risk 1: Import Update Failures

**Risk**: Automated import replacement breaks tests

**Mitigation**:
1. Use version control (feature branch)
2. Migrate one module at a time
3. Run tests after each module migration
4. Rollback if failures occur

**Likelihood**: Low (simple package rename)

### Risk 2: Test Framework Circular Dependency

**Risk**: Test framework depends on core, core tests depend on framework

**Mitigation**:
- Test framework only depends on **SDK interfaces** (adeengineer.dev:ade-agent-sdk)
- Does NOT depend on platform-core
- Breaks circular dependency

**Dependency Chain**:
```
SDK (interfaces only)
    ↑
test-framework (mocks SDK interfaces)
    ↑
core (tests depend on test-framework)
```

### Risk 3: Framework-Specific Test Needs

**Risk**: Spring Boot E2E tests need Spring-specific utilities

**Mitigation**:
- Keep `BaseE2ETest` in Spring Boot module (framework-specific)
- Extract common patterns to test-framework `BaseE2ETest` (framework-agnostic)
- Allow both to coexist

**Example**:
```java
// Spring Boot E2E test
@SpringBootTest
public class DevOpsAgentE2ETest extends dev.adeengineer.platform.spring.e2e.BaseE2ETest {
    // Spring-specific features
}

// Quarkus E2E test (future)
@QuarkusTest
public class DevOpsAgentE2ETest extends dev.adeengineer.platform.test.base.BaseE2ETest {
    // Framework-agnostic patterns
}
```

---

## Implementation Phases Summary

| Phase | Description | Time | Deliverables |
|-------|-------------|------|--------------|
| **1** | Create test-framework module | 1h | Module structure, moved utilities |
| **2** | Create new utilities | 3h | Builders, assertions, base classes |
| **3** | Migrate existing tests | 2h | Updated imports, deleted duplicates |
| **4** | Documentation | 1h | README, guides, examples |
| **Total** | End-to-End Implementation | **7h** | Fully functional test framework |

---

## Next Steps

### Immediate Actions

1. **Review this plan** - Get approval on approach
2. **Create test-framework module** - Scaffold structure
3. **Move existing utilities** - Eliminate duplication
4. **Add new utilities** - Builders and assertions
5. **Migrate all tests** - Update imports, verify
6. **Document** - README and guides
7. **Commit and push** - Conventional commit messages (no AI attribution)
8. **Create PR** - Request review and merge

### Future Enhancements (Post-Merge)

- [ ] Add Quarkus-specific test utilities
- [ ] Add Micronaut-specific test utilities
- [ ] Create test containers integration (for real LLM testing)
- [ ] Add performance test utilities
- [ ] Create test data generators (for property-based testing)

---

## Appendix A: File Locations

### Files to Move

**From** `ade-agent-platform-core/src/test/java/dev/adeengineer/platform/testutil/`:
- `MockAgent.java` → `test-framework/src/main/java/dev/adeengineer/platform/test/mock/`
- `TestData.java` → `test-framework/src/main/java/dev/adeengineer/platform/test/factory/`
- `TestLLMProvider.java` → `test-framework/src/main/java/dev/adeengineer/platform/test/mock/MockLLMProvider.java`

**From** `ade-agent-platform-spring-boot/src/test/java/dev/adeengineer/platform/spring/e2e/`:
- `BaseE2ETest.java` → Keep in Spring Boot (Spring-specific)
- Extract common patterns → `test-framework/.../base/BaseE2ETest.java` (framework-agnostic)

### Files to Delete

**After migration**:
- `ade-agent-platform-core/src/test/java/dev/adeengineer/platform/testutil/` (entire directory)
- `ade-agent-platform-spring-boot/src/test/java/dev/adeengineer/platform/spring/testutil/` (entire directory)
- `src/test/java/dev/adeengineer/platform/testutil/` (legacy, if exists)

---

## Appendix B: Example Usage

### Before (Current)

```java
// ade-agent-platform-core/src/test/java/.../AgentRegistryTest.java
package dev.adeengineer.platform.core;

import dev.adeengineer.platform.testutil.MockAgent;
import dev.adeengineer.platform.testutil.TestData;

class AgentRegistryTest {
    @Test
    void shouldRegisterAgent() {
        AgentRegistry registry = new AgentRegistry();
        MockAgent agent = new MockAgent("test-agent");

        registry.registerAgent(agent);

        assertThat(registry.getAgent("test-agent")).isEqualTo(agent);
    }
}
```

### After (With Test Framework)

```java
// ade-agent-platform-core/src/test/java/.../AgentRegistryTest.java
package dev.adeengineer.platform.core;

import dev.adeengineer.platform.test.mock.MockAgent;
import dev.adeengineer.platform.test.factory.TestData;
import static dev.adeengineer.platform.test.assertion.AgentAssertions.assertThat;

class AgentRegistryTest {
    @Test
    void shouldRegisterAgent() {
        AgentRegistry registry = new AgentRegistry();
        MockAgent agent = new MockAgent("test-agent");

        registry.registerAgent(agent);

        assertThat(registry.getAgent("test-agent"))
            .isNotNull()
            .hasName("test-agent");
    }
}
```

### With Builders (New)

```java
import dev.adeengineer.platform.test.builder.AgentConfigBuilder;

@Test
void shouldCreateAgentFromConfig() {
    AgentConfig config = AgentConfigBuilder.builder()
        .name("Developer")
        .description("Software development agent")
        .capabilities("coding", "testing", "debugging")
        .temperature(0.7)
        .maxTokens(1000)
        .build();

    ConfigurableAgent agent = new ConfigurableAgent(config, mockLLMProvider, formatterRegistry);

    assertThat(agent).hasName("Developer");
}
```

---

*Last Updated: 2025-10-21*
