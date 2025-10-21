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
 * E2E tests for Customer Support Agent workflows. Tests issue triage and troubleshooting workflows
 * through the REST API.
 */
@DisplayName("Customer Support Agent E2E Tests")
class CustomerSupportAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute issue triage task successfully")
    void shouldExecuteIssueTriageTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Customer Support Specialist",
                        "Triage customer issue with agent timeout",
                        Map.of(
                                "issue",
                                "Agent task timeout after 30 seconds",
                                "user",
                                "enterprise customer"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Customer Support Specialist");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute FAQ creation task successfully")
    void shouldExecuteFAQCreationTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Customer Support Specialist",
                        "Create FAQ for common agent configuration questions",
                        Map.of("topic", "agent configuration", "questions", "5 most common"));

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
