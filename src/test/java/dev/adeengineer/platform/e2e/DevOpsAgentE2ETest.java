package dev.adeengineer.platform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

/**
 * E2E tests for DevOps Agent workflows. Tests CI/CD pipeline design and deployment automation
 * workflows through the REST API.
 */
@DisplayName("DevOps Agent E2E Tests")
class DevOpsAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute CI/CD pipeline design task successfully")
    void shouldExecuteCICDPipelineTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "DevOps Engineer",
                        "Design a CI/CD pipeline for a Java Spring Boot application",
                        Map.of("project", "role-manager-app", "platform", "GitHub Actions"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("DevOps Engineer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute deployment strategy task successfully")
    void shouldExecuteDeploymentStrategyTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "DevOps Engineer",
                        "Recommend deployment strategy for production rollout",
                        Map.of("environment", "production", "downtime tolerance", "zero"));

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
