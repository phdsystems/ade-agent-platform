# ADR-001: Code Formatting Tool Selection - Spotless Maven Plugin

**Date:** 2025-10-20
**Status:** Proposed
**Decision Makers:** Development Team
**Context:** Need automated code formatting to resolve 76 Checkstyle violations

---

## TL;DR

**Decision**: Use Spotless Maven Plugin over Maven Formatter Plugin for automated code formatting. **Why**: Multi-language support, multiple formatter options (Google Java Format + Eclipse), import ordering, license headers, and active maintenance. **Trade-off**: Slightly more complex configuration → comprehensive formatting capabilities.

---

## Context and Problem Statement

The codebase currently has 76 Checkstyle violations (as of 2025-10-20) including:
- Line length violations (80 character limit)
- Missing Javadoc comments
- Missing package-info.java files
- Hidden field warnings
- TODO comments

**Problem:** Maven Checkstyle Plugin (`mvn checkstyle:check`) only validates code style but does not provide auto-fixing capabilities. We need an automated formatting tool to:
1. Fix existing violations automatically
2. Prevent future violations through pre-commit formatting
3. Maintain consistent code style across the team
4. Reduce manual formatting effort

**Candidates Evaluated:**
1. Maven Formatter Plugin (`net.revelc.code.formatter:formatter-maven-plugin`)
2. Spotless Maven Plugin (`com.diffplug.spotless:spotless-maven-plugin`)

---

## Decision Drivers

### Must-Have Requirements
- ✅ **Auto-fix capability** - Must automatically format Java code
- ✅ **Maven integration** - Must work with existing Maven build process
- ✅ **Checkstyle compatibility** - Should reduce/eliminate Checkstyle violations
- ✅ **CI/CD support** - Must support validation mode for continuous integration
- ✅ **Team adoption** - Should be easy to configure and use

### Nice-to-Have Requirements
- 🎯 Multi-language support (Java, YAML, XML, Markdown)
- 🎯 Import ordering and organization
- 🎯 License header management
- 🎯 Active community and maintenance
- 🎯 IDE-agnostic configuration
- 🎯 Flexible formatter options (Google Java Format, Eclipse, etc.)

---

## Options Considered

### Option 1: Maven Formatter Plugin

**Overview:**
```xml
<plugin>
    <groupId>net.revelc.code.formatter</groupId>
    <artifactId>formatter-maven-plugin</artifactId>
    <version>2.23.0</version>
    <configuration>
        <configFile>eclipse-formatter.xml</configFile>
    </configuration>
</plugin>
```

**Pros:**
- ✅ Simple configuration
- ✅ Lightweight and focused
- ✅ Eclipse formatter compatibility (familiar to Eclipse users)
- ✅ Well-established in Java ecosystem
- ✅ Lower learning curve

**Cons:**
- ❌ Java and XML only (no YAML, Markdown, JSON support)
- ❌ Single formatter option (Eclipse only)
- ❌ No import ordering capabilities
- ❌ No license header management
- ❌ Less active development
- ❌ Eclipse-specific configuration (IDE coupling)

**Commands:**
```bash
mvn formatter:format    # Auto-fix
mvn formatter:validate  # Check only (CI)
```

---

### Option 2: Spotless Maven Plugin (RECOMMENDED)

**Overview:**
```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.19.2</version>
                <style>AOSP</style>
            </googleJavaFormat>
            <removeUnusedImports/>
            <importOrder>
                <order>java,javax,jakarta,org,com,adeengineer</order>
            </importOrder>
            <formatAnnotations/>
        </java>
        <pom>
            <sortPom>
                <expandEmptyElements>false</expandEmptyElements>
            </sortPom>
        </pom>
    </configuration>
</plugin>
```

**Pros:**
- ✅ **Multi-language support** - Java, Kotlin, Scala, Groovy, SQL, JSON, YAML, Markdown, XML
- ✅ **Multiple formatter options** - Google Java Format, Eclipse, Spring, custom
- ✅ **Import ordering** - Configurable import organization
- ✅ **License header management** - Automated copyright headers
- ✅ **Active development** - Frequent updates, large community
- ✅ **IDE-agnostic** - Works with IntelliJ, Eclipse, VSCode
- ✅ **Comprehensive** - Trailing whitespace, line endings, custom regex replacements
- ✅ **Modern best practices** - Used by Spring Framework, Google, many major projects

**Cons:**
- ⚠️ More complex configuration (but more powerful)
- ⚠️ Larger plugin size (includes multiple formatters)
- ⚠️ Steeper learning curve initially

**Commands:**
```bash
mvn spotless:apply  # Auto-fix all files
mvn spotless:check  # Validate only (CI-friendly)
```

---

## Decision Outcome

**Chosen Option:** Spotless Maven Plugin

### Rationale

1. **Comprehensive Solution**: Spotless addresses not just code formatting but also:
   - Import organization (reduces "unused import" Checkstyle violations)
   - Trailing whitespace removal
   - Line ending normalization
   - Multi-file type support (future-proof for YAML configs, Markdown docs)

2. **Flexibility**: Supports both Google Java Format and Eclipse formatter:
   - Can start with Eclipse formatter for minimal disruption
   - Can migrate to Google Java Format for stricter, more opinionated formatting
   - Configuration is transparent and version-controlled

3. **Industry Adoption**: Used by major projects:
   - Spring Framework
   - Google Guava
   - Apache projects
   - Numerous open-source projects

4. **Active Maintenance**: Regular updates and bug fixes (last release: 2024)

5. **Future-Proofing**: When we add:
   - YAML configuration files
   - Markdown documentation
   - Kotlin or Groovy (if needed)
   - License headers

   Spotless already handles these without additional plugins.

6. **CI/CD Integration**: `spotless:check` provides clean pass/fail for pipelines

---

## Implementation Plan

### Phase 1: Initial Setup (Week 1)
1. Add Spotless plugin to `pom.xml`
2. Configure Google Java Format with AOSP style (100-character lines)
3. Configure import ordering
4. Run `mvn spotless:apply` to format entire codebase
5. Verify with `mvn checkstyle:check` - validate violations reduced

### Phase 2: CI/CD Integration (Week 1)
1. Add `mvn spotless:check` to CI pipeline
2. Document formatting commands in `CONTRIBUTING.md`
3. Add pre-commit hook recommendation (optional)

### Phase 3: Team Onboarding (Week 2)
1. Team training on `mvn spotless:apply` usage
2. IDE configuration guide (optional but helpful)
3. Address questions and edge cases

---

## Configuration Details

### Recommended Spotless Configuration

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <!-- Google Java Format with AOSP (Android) style = 100 char lines -->
            <googleJavaFormat>
                <version>1.19.2</version>
                <style>AOSP</style> <!-- Use AOSP for 100-char, or GOOGLE for 80-char -->
            </googleJavaFormat>

            <!-- Remove unused imports -->
            <removeUnusedImports/>

            <!-- Import ordering -->
            <importOrder>
                <order>java,javax,jakarta,org,com,adeengineer</order>
            </importOrder>

            <!-- Format annotations consistently -->
            <formatAnnotations/>
        </java>

        <!-- Format POM files -->
        <pom>
            <sortPom>
                <expandEmptyElements>false</expandEmptyElements>
            </sortPom>
        </pom>

        <!-- Future: Add when needed -->
        <!-- <yaml><prettier/></yaml> -->
        <!-- <markdown><prettier/></markdown> -->
    </configuration>

    <executions>
        <execution>
            <goals>
                <goal>check</goal> <!-- Fail build if not formatted -->
            </goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
```

**Key Configuration Choices:**

1. **Google Java Format (AOSP style)**
   - AOSP style = 100 character line limit (vs Checkstyle's 80)
   - Trade-off: Update Checkstyle to 100 chars OR use Google style (80 chars)
   - Recommendation: Update Checkstyle to 100 chars (modern standard)

2. **Import Ordering**
   - Standard Java imports first
   - Framework imports (javax, jakarta, org, com)
   - Project imports last (adeengineer)

3. **Execution Phase**
   - Runs during `verify` phase
   - Fails build if code not formatted
   - Encourages developers to run `spotless:apply` before commit

---

## Addressing Checkstyle Violations

### Violations Spotless Will Fix Automatically

| Checkstyle Violation | Spotless Solution |
|----------------------|-------------------|
| Line length (80 chars) | Google Java Format (AOSP = 100 chars) |
| Import ordering | `<importOrder>` configuration |
| Trailing whitespace | Automatic removal |
| Inconsistent indentation | Google Java Format normalization |
| Line endings (CRLF/LF) | Normalization to LF |

### Violations Requiring Manual Fix

| Checkstyle Violation | Manual Action Required |
|----------------------|------------------------|
| Missing Javadoc | Add Javadoc comments manually |
| Missing package-info.java | Create package-info.java files |
| Hidden field warnings | Rename parameters or fields |
| TODO comments | Resolve or track in issue tracker |
| Design issues (extension without Javadoc) | Add Javadoc or make classes final |

**Expected Result:**
- **Before:** 76 Checkstyle violations
- **After Spotless:** ~30-40 violations (mostly Javadoc and design issues)
- **After manual fixes:** 0 violations

---

## Alternative Configurations Considered

### Alternative 1: Eclipse Formatter (Instead of Google Java Format)

```xml
<java>
    <eclipse>
        <file>eclipse-formatter.xml</file>
    </eclipse>
    <removeUnusedImports/>
    <importOrder>
        <order>java,javax,jakarta,org,com,adeengineer</order>
    </importOrder>
</java>
```

**When to use:**
- Team prefers Eclipse IDE
- Existing Eclipse formatter config
- Need custom formatting rules

### Alternative 2: Spring Java Format

```xml
<java>
    <spring/>
    <removeUnusedImports/>
</java>
```

**When to use:**
- Spring Boot application (we are!)
- Want Spring-style formatting conventions

**Consideration:** Spring format is very opinionated. Google Java Format AOSP is more widely adopted.

---

## Consequences

### Positive Consequences

1. ✅ **Automated Formatting**: Eliminates manual formatting effort
2. ✅ **Consistent Style**: Entire codebase formatted uniformly
3. ✅ **Reduced Checkstyle Violations**: ~50% reduction immediately
4. ✅ **Future-Proof**: Supports multiple languages as project grows
5. ✅ **CI/CD Ready**: `spotless:check` prevents unformatted code from merging
6. ✅ **Team Efficiency**: Developers focus on logic, not formatting debates

### Negative Consequences

1. ⚠️ **Initial Disruption**: Large formatting commit will affect all files
   - **Mitigation**: Apply formatting in separate commit, clearly labeled
   - **Mitigation**: Communicate to team before applying

2. ⚠️ **Git Blame Pollution**: Formatting commit obscures previous changes
   - **Mitigation**: Use `git blame --ignore-rev` with formatting commit SHA
   - **Mitigation**: Document formatting commit in `.git-blame-ignore-revs`

3. ⚠️ **Learning Curve**: Team needs to learn `mvn spotless:apply`
   - **Mitigation**: Document in CONTRIBUTING.md
   - **Mitigation**: Add to IDE run configurations

4. ⚠️ **Build Time**: Adds ~5-10 seconds to Maven build
   - **Mitigation**: Acceptable trade-off for automated formatting
   - **Mitigation**: Only runs in `verify` phase, not `compile`

---

## Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Team rejects formatting style | High | Low | Allow voting period, provide configuration alternatives |
| Breaking changes in formatter | Medium | Low | Pin specific Google Java Format version |
| IDE conflicts | Low | Medium | Provide IDE configuration guides |
| Large initial commit | Low | High | Communicate clearly, use `.git-blame-ignore-revs` |

---

## Compliance and Standards

### Checkstyle Configuration Update Required

**Current:** `sun_checks.xml` (80-character line limit)

**Recommended Update:**
```xml
<!-- checkstyle.xml -->
<module name="LineLength">
    <property name="max" value="100"/>
    <property name="ignorePattern" value="^package.*|^import.*|a href|href|http://|https://|ftp://"/>
</module>
```

**Rationale:**
- Google Java Format (AOSP) uses 100 characters
- Modern monitors support wider lines
- Industry standard moving from 80 → 100/120 chars
- Spring Framework uses 120 chars

**Alternative:** Keep 80 chars and use Google Java Format GOOGLE style
```xml
<googleJavaFormat>
    <version>1.19.2</version>
    <style>GOOGLE</style> <!-- 80 char limit -->
</googleJavaFormat>
```

---

## Related Decisions

- **ADR-002 (Future):** Checkstyle Configuration Update (80 → 100 char limit)
- **ADR-003 (Future):** Pre-commit Hook Strategy
- **ADR-004 (Future):** License Header Standards

---

## References

### Official Documentation
- [Spotless Maven Plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven)
- [Google Java Format](https://github.com/google/google-java-format)
- [Maven Formatter Plugin](https://github.com/revelc/formatter-maven-plugin)
- [Checkstyle Documentation](https://checkstyle.sourceforge.io/)

### Industry Examples
- [Spring Framework Formatting](https://github.com/spring-projects/spring-framework/blob/main/src/checkstyle/checkstyle.xml)
- [Google Style Guide](https://google.github.io/styleguide/javaguide.html)

### Comparative Analysis
- [Spotless vs Formatter Plugin Discussion](https://stackoverflow.com/questions/tagged/spotless)
- [Code Formatting Best Practices (Baeldung)](https://www.baeldung.com/java-google-java-format)

---

## Approval and Sign-Off

| Role | Name | Approval | Date |
|------|------|----------|------|
| Tech Lead | TBD | [ ] Approved | YYYY-MM-DD |
| Senior Developer | TBD | [ ] Approved | YYYY-MM-DD |
| DevOps Engineer | TBD | [ ] Approved | YYYY-MM-DD |

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-20 | AI Assistant | Initial ADR proposal |

---

*Last Updated: 2025-10-20*
