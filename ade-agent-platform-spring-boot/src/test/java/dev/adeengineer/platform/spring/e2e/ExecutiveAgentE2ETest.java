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
 * E2E tests for Executive Agent workflows. Tests executive summaries and ROI analysis workflows
 * through the REST API.
 */
@DisplayName("Executive Agent E2E Tests")
class ExecutiveAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute executive summary task successfully")
    void shouldExecuteExecutiveSummaryTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Executive",
                        "Provide executive summary of multi-agent AI project",
                        Map.of("project", "role-manager-app", "audience", "C-level"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Executive");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute ROI analysis task successfully")
    void shouldExecuteROIAnalysisTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Executive",
                        "Analyze ROI for implementing multi-agent AI system",
                        Map.of("investment", "500k", "timeline", "6 months"));

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
