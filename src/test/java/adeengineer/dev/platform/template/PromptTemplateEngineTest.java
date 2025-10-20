package adeengineer.dev.platform.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import adeengineer.dev.platform.template.PromptTemplateEngine.PromptTemplate;

/** Unit tests for PromptTemplateEngine. */
class PromptTemplateEngineTest {

    private PromptTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PromptTemplateEngine();
    }

    @Test
    void shouldSubstituteSimpleVariables() {
        // Given
        String template = "Hello {{name}}, your role is {{role}}.";
        Map<String, Object> variables =
                Map.of(
                        "name", "Alice",
                        "role", "Developer");

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).isEqualTo("Hello Alice, your role is Developer.");
    }

    @Test
    void shouldHandleMissingVariables() {
        // Given
        String template = "Hello {{name}}, your age is {{age}}.";
        Map<String, Object> variables = Map.of("name", "Bob");

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).isEqualTo("Hello Bob, your age is .");
    }

    @Test
    void shouldProcessConditionals() {
        // Given
        String template = "Hello {{name}}!{{#if premium}} You are a premium user.{{/if}}";

        Map<String, Object> premiumUser = Map.of("name", "Alice", "premium", true);
        Map<String, Object> regularUser = Map.of("name", "Bob", "premium", false);

        // When
        String premiumResult = engine.render(template, premiumUser);
        String regularResult = engine.render(template, regularUser);

        // Then
        assertThat(premiumResult).contains("You are a premium user");
        assertThat(regularResult).doesNotContain("You are a premium user");
    }

    @Test
    void shouldProcessLoops() {
        // Given
        String template = "Users:{{#each users}} {{name}}{{/each}}";
        Map<String, Object> variables =
                Map.of(
                        "users",
                        List.of(
                                Map.of("name", "Alice"),
                                Map.of("name", "Bob"),
                                Map.of("name", "Charlie")));

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).contains("Alice");
        assertThat(result).contains("Bob");
        assertThat(result).contains("Charlie");
    }

    @Test
    void shouldHandleNestedTemplateStructures() {
        // Given
        String template =
                """
                Task: {{task}}
                Role: {{role}}
                {{#if context}}
                Context provided: {{context}}
                {{/if}}
                Requirements:{{#each requirements}}
                - {{item}}{{/each}}
                """;

        Map<String, Object> variables =
                Map.of(
                        "task", "Implement feature",
                        "role", "Developer",
                        "context", "Previous design completed",
                        "requirements",
                                List.of("Use Java 21", "Follow SOLID principles", "Add tests"));

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).contains("Task: Implement feature");
        assertThat(result).contains("Role: Developer");
        assertThat(result).contains("Context provided: Previous design completed");
        assertThat(result).contains("Use Java 21");
        assertThat(result).contains("Follow SOLID principles");
        assertThat(result).contains("Add tests");
    }

    @Test
    void shouldRegisterAndRenderTemplate() {
        // Given
        PromptTemplate template =
                new PromptTemplate("greeting", "Hello {{name}}, welcome to {{system}}!", Map.of());
        engine.registerTemplate("greeting", template);

        Map<String, Object> variables =
                Map.of(
                        "name", "Alice",
                        "system", "Role Manager");

        // When
        String result = engine.render("greeting", variables);

        // Then
        assertThat(result).isEqualTo("Hello Alice, welcome to Role Manager!");
    }

    @Test
    void shouldThrowExceptionForUnknownTemplate() {
        // Given
        Map<String, Object> variables = Map.of("name", "Alice");

        // When/Then
        assertThatThrownBy(() -> engine.render("nonexistent", variables))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    void shouldLoadMultipleTemplates() {
        // Given
        Map<String, String> templates =
                Map.of(
                        "welcome", "Welcome {{name}}!",
                        "goodbye", "Goodbye {{name}}!",
                        "task", "Task: {{task}}");

        // When
        engine.loadTemplates(templates);

        // Then
        assertThat(engine.getTemplateNames())
                .containsExactlyInAnyOrder("welcome", "goodbye", "task");
    }

    @Test
    void shouldUseTemplateBuilder() {
        // Given
        PromptTemplate template =
                new PromptTemplate(
                        "code-review", "Review this {{language}} code for {{focus}}", Map.of());
        engine.registerTemplate("code-review", template);

        // When
        String result =
                engine.template("code-review")
                        .with("language", "Java")
                        .with("focus", "security")
                        .build();

        // Then
        assertThat(result).isEqualTo("Review this Java code for security");
    }

    @Test
    void shouldHandleEmptyString() {
        // Given
        String template = "Status: {{#if status}}{{status}}{{/if}}";
        Map<String, Object> variables = Map.of("status", "");

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).isEqualTo("Status:");
    }

    @Test
    void shouldHandleEmptyCollection() {
        // Given
        String template = "Items:{{#each items}} {{item}}{{/each}}";
        Map<String, Object> variables = Map.of("items", List.of());

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).isEqualTo("Items:");
    }

    @Test
    void shouldEvaluateNonBooleanConditionals() {
        // Given
        String template = "{{#if value}}Value exists{{/if}}";

        // When
        String stringResult = engine.render(template, Map.of("value", "text"));
        String numberResult = engine.render(template, Map.of("value", 123));
        String listResult = engine.render(template, Map.of("value", List.of("item")));

        // Then
        assertThat(stringResult).contains("Value exists");
        assertThat(numberResult).contains("Value exists");
        assertThat(listResult).contains("Value exists");
    }

    @Test
    void shouldTrimWhitespace() {
        // Given
        String template = "\n\n  Hello {{name}}  \n\n";
        Map<String, Object> variables = Map.of("name", "Alice");

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).isEqualTo("Hello Alice");
    }

    @Test
    void shouldHandleMultilineTemplates() {
        // Given
        String template =
                """
                You are a {{role}}.

                Task: {{task}}

                {{#if context}}
                Context:
                {{context}}
                {{/if}}

                Please provide your response.
                """;

        Map<String, Object> variables =
                Map.of(
                        "role", "Senior Developer",
                        "task", "Code review",
                        "context", "This is a critical security fix");

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).contains("You are a Senior Developer");
        assertThat(result).contains("Task: Code review");
        assertThat(result).contains("This is a critical security fix");
    }

    @Test
    void shouldHandleComplexWorkflowTemplate() {
        // Given
        String template =
                """
                Workflow: {{workflowName}}

                Steps:{{#each steps}}
                - {{name}}: {{task}} (assigned to {{role}}){{/each}}

                {{#if dependencies}}
                Note: Some steps have dependencies that must be completed first.
                {{/if}}
                """;

        Map<String, Object> variables =
                Map.of(
                        "workflowName",
                        "Feature Development",
                        "steps",
                        List.of(
                                Map.of(
                                        "name",
                                        "Design",
                                        "task",
                                        "Create architecture",
                                        "role",
                                        "Architect"),
                                Map.of(
                                        "name",
                                        "Implement",
                                        "task",
                                        "Write code",
                                        "role",
                                        "Developer"),
                                Map.of("name", "Test", "task", "Run tests", "role", "QA")),
                        "dependencies",
                        true);

        // When
        String result = engine.render(template, variables);

        // Then
        assertThat(result).contains("Workflow: Feature Development");
        assertThat(result).contains("Design: Create architecture (assigned to Architect)");
        assertThat(result).contains("Implement: Write code (assigned to Developer)");
        assertThat(result).contains("Test: Run tests (assigned to QA)");
        assertThat(result).contains("Note: Some steps have dependencies");
    }
}
