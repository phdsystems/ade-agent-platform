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
 * E2E tests for Compliance Agent workflows. Tests regulatory compliance and audit workflows through
 * the REST API.
 */
@DisplayName("Compliance Agent E2E Tests")
class ComplianceAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute GDPR compliance review task successfully")
    void shouldExecuteGDPRReviewTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Compliance Officer",
                        "Review data handling for GDPR compliance",
                        Map.of("scope", "user data processing", "regulation", "GDPR"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Compliance Officer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute security audit task successfully")
    void shouldExecuteSecurityAuditTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Compliance Officer",
                        "Prepare SOC2 audit checklist",
                        Map.of("standard", "SOC2 Type II", "focus", "security controls"));

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
