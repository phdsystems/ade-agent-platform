package adeengineer.dev.platform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

/**
 * E2E tests for Manager Agent workflows. Tests the complete metrics generation workflow through the
 * REST API.
 */
@DisplayName("Manager Agent E2E Tests")
class ManagerAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute metrics generation task successfully")
    void shouldExecuteMetricsGenerationTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Engineering Manager",
                        "Summarize team velocity metrics for Sprint 23",
                        Map.of("sprint", "Sprint 23", "team", "Platform Engineering"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Engineering Manager");
        assertThat(result.output()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should return executive summary format for Manager role")
    void shouldReturnExecutiveSummaryFormat() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Engineering Manager",
                        "Provide status update on Q4 deliverables",
                        Map.of("quarter", "Q4", "year", "2025"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TaskResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.output())
                .isNotEmpty()
                .as("Manager output should provide high-level summary");
    }

    @Test
    @DisplayName("Should complete metrics task within 30 seconds")
    void shouldCompleteMetricsTaskWithin30Seconds() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Engineering Manager", "Generate weekly team performance report", Map.of());
        long startTime = System.currentTimeMillis();

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duration).isLessThan(30_000);

        TaskResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
    }
}
