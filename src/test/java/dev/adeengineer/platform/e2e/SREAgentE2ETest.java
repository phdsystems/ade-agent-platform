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
 * E2E tests for Site Reliability Engineer Agent workflows. Tests incident response and SLO
 * management workflows through the REST API.
 */
@DisplayName("SRE Agent E2E Tests")
class SREAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute SLO definition task successfully")
    void shouldExecuteSLODefinitionTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Site Reliability Engineer",
                        "Define SLOs for agent task execution service",
                        Map.of("service", "task-execution", "target", "99.9% availability"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Site Reliability Engineer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute incident response task successfully")
    void shouldExecuteIncidentResponseTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Site Reliability Engineer",
                        "Create runbook for LLM provider failover incident",
                        Map.of("incident", "LLM provider down", "severity", "P1"));

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
