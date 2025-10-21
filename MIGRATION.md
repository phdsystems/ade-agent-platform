# Migration Guide - Framework-Agnostic Platform

**Version:** 0.2.0
**Date:** 2025-10-21

## Overview

Starting with version `0.2.0`, ade-agent-platform has been refactored into a **multi-module, framework-agnostic** architecture.

### What Changed

**Old Structure (≤ 0.1.x):**
```
ade-agent-platform (single JAR with Spring Boot bundled)
```

**New Structure (≥ 0.2.0):**
```
ade-agent-platform-parent/
├── ade-agent-platform-core         (framework-agnostic)
└── ade-agent-platform-spring-boot  (Spring Boot integration)
```

## Benefits

✅ **Use any framework** - Spring Boot, Quarkus, Micronaut, or none
✅ **Smaller dependencies** - Core module has no Spring (5MB vs 50MB)
✅ **Faster startup** - No Spring overhead in vanilla Java usage
✅ **True infrastructure** - Platform is now truly framework-agnostic

## Migration Steps

### For Spring Boot Users

**Before (0.1.x):**
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform</artifactId>
    <version>0.1.0</version>
</dependency>
```

**After (0.2.0):**
```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-spring-boot</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

**Code Changes:**

1. **Update imports** (only if using config classes directly):
   ```java
   // Before
   import dev.adeengineer.platform.config.AppConfig;

   // After
   import dev.adeengineer.platform.spring.config.AppConfig;
   ```

2. **Provider usage unchanged:**
   ```java
   // Still works exactly the same
   @Autowired
   private AgentRegistry registry;

   @Autowired
   private DomainLoader loader;
   ```

### For Vanilla Java Users (New!)

Now you can use the platform **without any framework**:

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform-core</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

**Usage:**
```java
// Direct instantiation - no Spring required
AgentRegistry registry = new AgentRegistry();
OutputFormatterRegistry formatters = new OutputFormatterRegistry();
DomainLoader loader = new DomainLoader(registry, formatters);

// Use the platform
int agentCount = loader.loadAllDomains("./domains", llmProvider);
```

See `examples/vanilla-java/` for complete example.

## Package Changes

### Spring Boot Module

**Old packages:**
- `dev.adeengineer.platform.config.*`
- `dev.adeengineer.platform.api.*`
- `dev.adeengineer.platform.cli.*`

**New packages:**
- `dev.adeengineer.platform.spring.config.*`
- `dev.adeengineer.platform.spring.api.*`
- `dev.adeengineer.platform.spring.cli.*`

### Core Module (Unchanged)

These packages remain the same:
- `dev.adeengineer.platform.core.*`
- `dev.adeengineer.platform.providers.*`
- `dev.adeengineer.platform.orchestration.*`
- `dev.adeengineer.platform.template.*`

## Breaking Changes

### Maven Coordinates

| Component | Old Artifact | New Artifact |
|-----------|--------------|--------------|
| Spring Boot apps | `ade-agent-platform` | `ade-agent-platform-spring-boot` |
| Vanilla Java apps | N/A | `ade-agent-platform-core` |

### Package Renames

Only affects code that directly imports Spring configuration classes:

```java
// Before
import dev.adeengineer.platform.config.ProvidersAutoConfiguration;

// After
import dev.adeengineer.platform.spring.config.ProvidersAutoConfiguration;
```

**Note:** Most applications don't import config classes directly, so this won't affect you.

## Compatibility Matrix

| Version | Framework | Artifact |
|---------|-----------|----------|
| 0.1.x | Spring Boot only | `ade-agent-platform` |
| 0.2.0+ | Spring Boot | `ade-agent-platform-spring-boot` |
| 0.2.0+ | Vanilla Java | `ade-agent-platform-core` |
| 0.2.0+ | Quarkus (future) | `ade-agent-platform-quarkus` |
| 0.2.0+ | Micronaut (future) | `ade-agent-platform-micronaut` |

## Testing Your Migration

### Spring Boot Applications

1. Update `pom.xml` with new artifact
2. Update imports if using config classes
3. Run tests:
   ```bash
   mvn clean test
   ```
4. Verify auto-configuration works:
   ```bash
   mvn spring-boot:run
   ```

### Vanilla Java Applications

1. Add `ade-agent-platform-core` dependency
2. Remove Spring dependencies
3. Instantiate providers directly (see example)
4. Test:
   ```bash
   mvn clean test
   ```

## Troubleshooting

### Issue: Missing Spring beans

**Symptom:** `NoSuchBeanDefinitionException`

**Solution:** Ensure you're using `ade-agent-platform-spring-boot`, not `ade-agent-platform-core`

### Issue: Package not found

**Symptom:** `package dev.adeengineer.platform.config does not exist`

**Solution:** Update import to `dev.adeengineer.platform.spring.config`

### Issue: Want to use without Spring

**Solution:** Switch to `ade-agent-platform-core` and instantiate beans manually

## Getting Help

- **GitHub Issues:** https://github.com/phdsystems/ade-agent-platform/issues
- **Examples:** See `examples/` directory
- **Documentation:** See `doc/` directory

## Rollback

If you need to rollback to 0.1.x:

```xml
<dependency>
    <groupId>adeengineer.dev</groupId>
    <artifactId>ade-agent-platform</artifactId>
    <version>0.1.0</version>
</dependency>
```

However, we recommend migrating to benefit from the framework-agnostic architecture.
