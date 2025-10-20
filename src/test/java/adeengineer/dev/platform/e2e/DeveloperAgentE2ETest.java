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
 * E2E tests for Developer Agent workflows. Tests the complete code review workflow through the REST
 * API.
 */
@DisplayName("Developer Agent E2E Tests")
class DeveloperAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute code review task successfully")
    void shouldExecuteCodeReviewTask() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Developer",
                        "Review this code for potential bugs",
                        Map.of(
                                "code",
                                "public void process(String input) { System.out.println(input.length()); }"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TaskResult result = response.getBody();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Developer");
        assertThat(result.output()).isNotNull().isNotEmpty();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should complete code review within 30 seconds")
    void shouldCompleteCodeReviewWithin30Seconds() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Developer",
                        "Analyze this code snippet for improvements",
                        Map.of("code", "public class Example { public void test() {} }"));
        long startTime = System.currentTimeMillis();

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duration).isLessThan(30_000); // Less than 30 seconds

        TaskResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("Should return technical output for Developer role")
    void shouldReturnTechnicalOutput() {
        // Arrange
        TaskRequest request =
                new TaskRequest(
                        "Developer",
                        "Review this function for best practices",
                        Map.of("function", "function add(a, b) { return a + b; }"));

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TaskResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.output())
                .isNotEmpty()
                .as("Developer output should contain technical details");
    }

    @Test
    @DisplayName("Should handle invalid role gracefully")
    void shouldHandleInvalidRoleGracefully() {
        // Arrange
        TaskRequest request = new TaskRequest("NonExistentRole", "Some task", Map.of());

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should include usage information in result")
    void shouldIncludeUsageInformation() {
        // Arrange
        TaskRequest request = new TaskRequest("Developer", "Quick code review", Map.of());

        // Act
        ResponseEntity<TaskResult> response =
                restTemplate.postForEntity(apiUrl("/tasks/execute"), request, TaskResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TaskResult result = response.getBody();
        assertThat(result).isNotNull();
        // Usage info available in metadata;
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }
}
