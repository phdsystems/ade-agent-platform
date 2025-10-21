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
 * E2E tests for Product Owner Agent workflows. Tests user story creation and backlog management
 * workflows through the REST API.
 */
@DisplayName("Product Owner Agent E2E Tests")
class ProductOwnerAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute user story creation task successfully")
    void shouldExecuteUserStoryCreationTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Product Owner",
                        "Create user stories for multi-agent workflow feature",
                        Map.of("feature", "workflow orchestration", "priority", "high"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Product Owner");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute backlog prioritization task successfully")
    void shouldExecuteBacklogPrioritizationTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Product Owner",
                        "Prioritize backlog items for next sprint",
                        Map.of("sprint", "Sprint 3", "capacity", "40 points"));

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
