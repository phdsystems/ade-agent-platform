package dev.adeengineer.platform.spring.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

/**
 * E2E tests for QA Agent workflows. Tests the complete test plan generation workflow through the
 * REST API.
 */
@DisplayName("QA Agent E2E Tests")
class QAAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute test plan generation task successfully")
    void shouldExecuteTestPlanGenerationTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "QA",
                        "Generate test cases for user login feature",
                        Map.of("feature", "login", "type", "authentication"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("QA");
        assertThat(result.output()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should return test-focused output for QA role")
    void shouldReturnTestFocusedOutput() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "QA",
                        "Create test strategy for API endpoint",
                        Map.of("endpoint", "/api/users", "method", "POST"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TaskResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.output())
                .isNotEmpty()
                .as("QA output should include test cases and scenarios");
    }

    @Test
    @DisplayName("Should complete test plan task within 30 seconds")
    void shouldCompleteTestPlanTaskWithin30Seconds() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "QA",
                        "Generate acceptance criteria for feature",
                        Map.of("feature", "password reset"));
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
