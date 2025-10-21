package dev.adeengineer.platform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.adeengineer.agent.TaskRequest;
import dev.adeengineer.agent.TaskResult;

/**
 * E2E tests for UI/UX Designer Agent workflows. Tests interface design and accessibility workflows
 * through the REST API.
 */
@DisplayName("UI/UX Designer Agent E2E Tests")
class UIUXDesignerAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute interface design task successfully")
    void shouldExecuteInterfaceDesignTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "UI/UX Designer",
                        "Design interface for workflow visualization dashboard",
                        Map.of("component", "workflow dashboard", "platform", "web"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("UI/UX Designer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute accessibility review task successfully")
    void shouldExecuteAccessibilityReviewTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "UI/UX Designer",
                        "Review interface for WCAG 2.1 AA compliance",
                        Map.of("component", "task execution form", "standard", "WCAG 2.1 AA"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
    }
}
