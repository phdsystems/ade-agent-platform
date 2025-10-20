# Contributing to ade Agent Platform

Thank you for your interest in contributing to the ade Agent Platform! This document provides guidelines and best practices for contributing to this project.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Formatting](#code-formatting)
- [Code Quality Standards](#code-quality-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Documentation](#documentation)

---

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Collaborate openly and transparently
- Help maintain a positive community

---

## Getting Started

### Prerequisites

- **Java 21** (with preview features enabled)
- **Maven 3.9+**
- **Git**
- **IDE** (IntelliJ IDEA, Eclipse, or VSCode recommended)

### Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd ade-agent-platform

# Build the project
mvn clean install

# Run tests
mvn test

# Run the application
mvn spring-boot:run
```

For detailed setup instructions, see [doc/4-development/local-setup-guide.md](doc/4-development/local-setup-guide.md).

---

## Development Workflow

1. **Create a feature branch** from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** following our coding standards

3. **Format your code** (see [Code Formatting](#code-formatting))

4. **Run tests** to ensure nothing breaks
   ```bash
   mvn test
   ```

5. **Commit your changes** with a clear message (see [Commit Message Guidelines](#commit-message-guidelines))

6. **Push to your branch** and create a Pull Request

---

## Code Formatting

We use **Spotless Maven Plugin** with **Google Java Format (AOSP style)** for automated code formatting.

### Quick Reference

| Command | Purpose | When to Use |
|---------|---------|-------------|
| `mvn spotless:apply` | Auto-format all code | Before committing |
| `mvn spotless:check` | Validate formatting | CI/CD pipeline |
| `mvn checkstyle:check` | Check code quality | After formatting |

### Before Committing Code

**ALWAYS run Spotless before committing:**

```bash
# Format all Java files
mvn spotless:apply

# Verify formatting is correct
mvn spotless:check
```

### Formatting Standards

Our code formatting follows these rules:

1. **Line Length**: 100 characters (AOSP style)
2. **Indentation**: 4 spaces (no tabs)
3. **Import Ordering**:
   ```
   java.*
   javax.*
   jakarta.*
   org.*
   com.*
   adeengineer.*
   ```
4. **Trailing Whitespace**: Automatically removed
5. **File Endings**: Files end with newline

### What Spotless Formats

- ✅ Java source files (`.java`)
- ✅ POM files (`pom.xml`)
- ✅ Import statements (ordering + removing unused)
- ✅ Whitespace and line endings
- ✅ Annotation formatting

### CI/CD Integration

Our build pipeline runs `mvn spotless:check` during the `verify` phase. If your code is not formatted, the build will fail.

**To avoid CI/CD failures:**
- Always run `mvn spotless:apply` locally before pushing
- Commit the formatted code

### IDE Integration (Optional but Recommended)

#### IntelliJ IDEA

1. **Install Google Java Format Plugin:**
   - Settings → Plugins → Marketplace
   - Search "google-java-format"
   - Install and restart

2. **Configure Plugin:**
   - Settings → google-java-format Settings
   - Enable "Enable google-java-format"
   - Select "Android Open Source Project (AOSP) style"

3. **Format on Save (Optional):**
   - Settings → Tools → Actions on Save
   - Enable "Reformat code"

#### Eclipse

1. **Import Formatter:**
   - Download [Google Java Format for Eclipse](https://github.com/google/google-java-format#eclipse)
   - Window → Preferences → Java → Code Style → Formatter
   - Import → Select downloaded XML
   - Apply

#### VSCode

1. **Install Extension:**
   ```bash
   ext install richardwillis.vscode-google-java-format
   ```

2. **Configure Settings:**
   ```json
   {
     "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
     "editor.formatOnSave": true
   }
   ```

### Troubleshooting Formatting Issues

**Problem: Spotless check fails after applying**
```bash
# Solution: Clean and reapply
mvn clean
mvn spotless:apply
mvn spotless:check
```

**Problem: Large diff after formatting**
```bash
# This is normal on first run. Commit the formatted code separately:
git add .
git commit -m "style: apply Spotless code formatting

Applied Google Java Format (AOSP style) across entire codebase.
Line length: 100 characters, import ordering enforced."
```

**Problem: Merge conflicts with formatting**
```bash
# 1. Resolve conflicts manually
# 2. Then reapply formatting
mvn spotless:apply
git add .
git commit
```

### Formatting Configuration Details

The Spotless plugin is configured in `pom.xml` (lines 328-375):

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.19.2</version>
                <style>AOSP</style> <!-- 100-char lines -->
            </googleJavaFormat>
            <removeUnusedImports/>
            <importOrder>
                <order>java,javax,jakarta,org,com,adeengineer</order>
            </importOrder>
            <formatAnnotations/>
            <trimTrailingWhitespace/>
            <endWithNewline/>
        </java>
        <pom>
            <sortPom/>
        </pom>
    </configuration>
</plugin>
```

For the full rationale behind this choice, see [ADR-001: Code Formatting Tool Selection](doc/3-design/decisions/001-code-formatting-tool-selection.md).

---

## Code Quality Standards

### Checkstyle

We use Checkstyle with `sun_checks.xml` to enforce code quality standards.

```bash
# Run Checkstyle
mvn checkstyle:check

# Generate HTML report
mvn checkstyle:checkstyle
# Report: target/site/checkstyle.html
```

**Note:** Checkstyle runs automatically during the `verify` phase but only reports warnings (does not fail the build).

### Common Checkstyle Violations to Avoid

1. **Missing Javadoc** - All public classes, methods, and fields must have Javadoc
2. **Missing package-info.java** - Each package must have a package-info.java file
3. **Hidden Field Warnings** - Avoid parameter names that hide instance fields
4. **TODO Comments** - Use issue tracker instead of TODO comments in code

### Code Coverage

We use JaCoCo for code coverage with a minimum threshold of 50%.

```bash
# Run tests with coverage
mvn test

# View coverage report
open target/site/jacoco/index.html
```

**Coverage Requirements:**
- Minimum: 50% line coverage
- Target: 70%+ for new features
- Goal: 80%+ for critical components

---

## Testing Guidelines

### Test Types

1. **Unit Tests** (`*Test.java`)
   - Test individual components in isolation
   - Use Mockito for mocking dependencies
   - Fast execution (<100ms per test)

2. **Integration Tests** (`*E2ETest.java`)
   - Test component interactions
   - Run with `mvn verify` (Failsafe plugin)
   - May require external services (Redis, Ollama)

3. **Load Tests** (`simulations/*`)
   - Gatling-based performance tests
   - Run with `mvn gatling:test`

### Running Tests

```bash
# Unit tests only
mvn test

# Unit + Integration tests
mvn verify

# Skip tests (use sparingly)
mvn install -DskipTests

# Run specific test
mvn test -Dtest=YourTestClass

# Run load tests
mvn gatling:test
```

### Test Guidelines

- ✅ Write tests for all new features
- ✅ Maintain or improve code coverage
- ✅ Use descriptive test names: `shouldDoSomething_WhenCondition()`
- ✅ Use AssertJ for assertions (fluent API)
- ✅ Mock external dependencies
- ❌ Don't commit commented-out tests
- ❌ Don't skip failing tests

For detailed testing strategies, see [doc/5-testing/test-plan.md](doc/5-testing/test-plan.md).

---

## Commit Message Guidelines

We follow **Conventional Commits** format for clear, structured commit messages.

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

| Type | Purpose | Example |
|------|---------|---------|
| `feat` | New feature | `feat(agent): add sentiment analysis capability` |
| `fix` | Bug fix | `fix(cache): resolve Redis connection timeout` |
| `docs` | Documentation | `docs(guide): update setup instructions` |
| `refactor` | Code refactoring | `refactor(loader): extract domain validation logic` |
| `test` | Add/update tests | `test(agent): add unit tests for ConfigurableAgent` |
| `chore` | Maintenance | `chore(deps): update Spring Boot to 3.3.5` |
| `perf` | Performance | `perf(cache): optimize LLM response caching` |
| `style` | Code formatting | `style: apply Spotless code formatting` |
| `ci` | CI/CD changes | `ci(github): add Checkstyle validation step` |

### Scope (Optional)

The scope indicates which part of the codebase is affected:
- `agent` - Agent-related changes
- `llm` - LLM provider changes
- `config` - Configuration changes
- `cli` - CLI interface changes
- `cache` - Caching logic
- `docs` - Documentation

### Examples

**Good:**
```
feat(agent): add multi-agent orchestration support

Implemented parallel task execution for multiple agents.
Supports up to 5 concurrent agents with shared context.

Closes #123
```

**Good:**
```
fix(cache): prevent Redis connection leak

Added proper connection cleanup in error scenarios.
Configured connection pool timeout to 5 seconds.
```

**Good (simple):**
```
docs(readme): fix broken links
```

**Bad:**
```
updated stuff
```

**Bad:**
```
Fixed bug
```

**Bad:**
```
WIP - not done yet
```

### Commit Message Rules

- ✅ Use imperative mood: "add feature" not "added feature"
- ✅ Keep first line under 72 characters
- ✅ Add detailed explanation in body if needed
- ✅ Reference issue numbers: `Closes #123`, `Fixes #456`
- ❌ Don't include AI attribution (e.g., "Generated with Claude Code")
- ❌ Don't end subject line with period
- ❌ Don't commit WIP (work-in-progress) to main branch

### Why No AI Attribution?

Per our coding standards, we don't include AI attribution in commit messages because:
- Commits reflect human authorship and decision-making
- AI is a tool like an IDE or compiler
- Keeps git history clean and professional
- Attribution belongs in project documentation, not every commit

For more details, see the global [~/.claude/CLAUDE.md](~/.claude/CLAUDE.md) guidelines.

---

## Pull Request Process

### Before Creating a PR

1. ✅ **Format your code**: `mvn spotless:apply`
2. ✅ **Run tests**: `mvn verify`
3. ✅ **Check code quality**: `mvn checkstyle:check`
4. ✅ **Update documentation** if needed
5. ✅ **Rebase on main** to avoid merge conflicts

### PR Title Format

Use the same format as commit messages:

```
<type>(<scope>): <description>
```

**Examples:**
- `feat(agent): add sentiment analysis agent`
- `fix(cache): resolve Redis timeout issue`
- `docs(contributing): add formatting guidelines`

### PR Description Template

```markdown
## Summary
Brief description of what this PR does.

## Changes
- Change 1
- Change 2
- Change 3

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] Code formatted with `mvn spotless:apply`
- [ ] Tests pass (`mvn verify`)
- [ ] Checkstyle violations addressed
- [ ] Documentation updated
- [ ] CHANGELOG.md updated (if applicable)

## Related Issues
Closes #123
```

### PR Review Process

1. **Automated Checks**: CI/CD runs tests, formatting, and Checkstyle
2. **Code Review**: At least one approval required
3. **Address Feedback**: Make requested changes
4. **Merge**: Squash and merge to main (maintain clean history)

### PR Guidelines

- ✅ Keep PRs focused and reasonably sized (<500 lines preferred)
- ✅ Link to related issues
- ✅ Respond to review comments promptly
- ✅ Update PR when main branch changes
- ❌ Don't force-push after review starts (unless requested)
- ❌ Don't merge your own PRs without approval

---

## Documentation

### When to Update Documentation

Update documentation when:
- ✅ Adding new features (update requirements, architecture, API design)
- ✅ Changing architecture (update architecture.md, component-specifications.md)
- ✅ Modifying APIs (update api-design.md)
- ✅ Changing deployment process (update deployment-guide.md)
- ✅ Discovering common issues (update troubleshooting-guide.md)

### Documentation Structure

```
doc/
├── 1-planning/              # Requirements, stakeholders
├── 2-analysis/              # Use cases, feasibility
├── 3-design/                # Architecture, API, data models
│   └── decisions/           # Architecture Decision Records (ADRs)
├── 4-development/           # Developer guides, coding standards
├── 5-testing/               # Test plans and strategies
├── 6-deployment/            # Deployment procedures
└── 7-maintenance/           # Operations, troubleshooting
```

### Documentation Standards

All documentation must follow:

1. **File Naming**: Use `kebab-case.md` (e.g., `developer-guide.md`)
2. **Header Section**: Include date, version
3. **TL;DR Section**: Required for all guide documents
4. **Main Content**: Hierarchical structure, code examples
5. **Footer**: Last updated date

For detailed documentation standards, see [~/.claude/CLAUDE.md](~/.claude/CLAUDE.md).

### Architecture Decision Records (ADRs)

When making significant architectural decisions, create an ADR in `doc/3-design/decisions/`:

**Format:** `NNN-decision-name.md`

**Example ADRs:**
- [001-code-formatting-tool-selection.md](doc/3-design/decisions/001-code-formatting-tool-selection.md)

See existing ADRs for template and structure.

---

## Getting Help

- **Documentation**: Check [doc/](doc/) directory
- **Issues**: Search existing issues or create new one
- **Discussions**: Use GitHub Discussions for questions
- **Debugging**: See [doc/4-development/debugging-guide.md](doc/4-development/debugging-guide.md)

---

## Quick Command Reference

```bash
# Build
mvn clean install

# Format code
mvn spotless:apply

# Run tests
mvn test                    # Unit tests
mvn verify                  # Unit + Integration tests

# Code quality
mvn spotless:check          # Check formatting
mvn checkstyle:check        # Check code quality

# Run application
mvn spring-boot:run

# Load testing
mvn gatling:test
```

---

Thank you for contributing to ade Agent Platform! 🚀
