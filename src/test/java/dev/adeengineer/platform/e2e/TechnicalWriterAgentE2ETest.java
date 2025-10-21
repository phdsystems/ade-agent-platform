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
 * E2E tests for Technical Writer Agent workflows. Tests documentation creation workflows through
 * the REST API.
 */
@DisplayName("Technical Writer Agent E2E Tests")
class TechnicalWriterAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute API documentation task successfully")
    void shouldExecuteAPIDocumentationTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Technical Writer",
                        "Create API documentation for workflow execution endpoint",
                        Map.of("endpoint", "/api/workflows/execute", "format", "OpenAPI 3.0"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Technical Writer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute user guide task successfully")
    void shouldExecuteUserGuideTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Technical Writer",
                        "Write user guide for configuring agents",
                        Map.of("topic", "agent configuration", "audience", "developers"));

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
