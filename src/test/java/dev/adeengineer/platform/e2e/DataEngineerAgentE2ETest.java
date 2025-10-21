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
 * E2E tests for Data Engineer Agent workflows. Tests data pipeline design and ETL workflows through
 * the REST API.
 */
@DisplayName("Data Engineer Agent E2E Tests")
class DataEngineerAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute data pipeline design task successfully")
    void shouldExecuteDataPipelineDesignTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Data Engineer",
                        "Design ETL pipeline for agent usage metrics",
                        Map.of("source", "PostgreSQL", "destination", "data warehouse"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Data Engineer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute data quality task successfully")
    void shouldExecuteDataQualityTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Data Engineer",
                        "Define data quality checks for task results",
                        Map.of("table", "task_results", "checks", "completeness, accuracy"));

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
