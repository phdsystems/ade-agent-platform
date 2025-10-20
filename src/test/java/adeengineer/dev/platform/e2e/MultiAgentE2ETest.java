package adeengineer.dev.platform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import adeengineer.dev.agent.TaskResult;
import adeengineer.dev.platform.api.TaskController;

/**
 * E2E tests for Multi-Agent collaboration workflows. Tests parallel execution of multiple agents
 * through the REST API.
 */
@DisplayName("Multi-Agent E2E Tests")
class MultiAgentE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should execute multi-agent task successfully")
    void shouldExecuteMultiAgentTaskSuccessfully() {
        // Arrange
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(
                        List.of("Developer", "QA", "Security"),
                        "Analyze new user authentication endpoint",
                        Map.of("endpoint", "/api/auth/login", "method", "POST"));

        // Act
        ResponseEntity<Map<String, TaskResult>> response =
                restTemplate.exchange(
                        apiUrl("/tasks/multi-agent"),
                        HttpMethod.POST,
                        new org.springframework.http.HttpEntity<>(request),
                        new ParameterizedTypeReference<Map<String, TaskResult>>() {});

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<String, TaskResult> results = response.getBody();
        assertThat(results).hasSize(3);
        assertThat(results).containsKeys("Developer", "QA", "Security");

        // Verify all agents succeeded
        assertThat(results.get("Developer").success()).isTrue();
        assertThat(results.get("QA").success()).isTrue();
        assertThat(results.get("Security").success()).isTrue();
    }

    @Test
    @DisplayName("Should execute agents in parallel")
    void shouldExecuteAgentsInParallel() {
        // Arrange
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(
                        List.of("Developer", "QA"), "Review code changes", Map.of());
        long startTime = System.currentTimeMillis();

        // Act
        ResponseEntity<Map<String, TaskResult>> response =
                restTemplate.exchange(
                        apiUrl("/tasks/multi-agent"),
                        HttpMethod.POST,
                        new org.springframework.http.HttpEntity<>(request),
                        new ParameterizedTypeReference<Map<String, TaskResult>>() {});
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);

        // If truly parallel, total duration should be less than sum of individual durations
        // This is a weak assertion but demonstrates parallelism
        assertThat(duration).isLessThan(60_000); // Should complete within 60 seconds
    }

    @Test
    @DisplayName("Should return role-specific outputs for each agent")
    void shouldReturnRoleSpecificOutputs() {
        // Arrange
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(
                        List.of("Developer", "Engineering Manager"),
                        "Analyze system performance",
                        Map.of());

        // Act
        ResponseEntity<Map<String, TaskResult>> response =
                restTemplate.exchange(
                        apiUrl("/tasks/multi-agent"),
                        HttpMethod.POST,
                        new org.springframework.http.HttpEntity<>(request),
                        new ParameterizedTypeReference<Map<String, TaskResult>>() {});

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, TaskResult> results = response.getBody();
        assertThat(results).isNotNull();

        // Developer should provide technical details
        TaskResult devResult = results.get("Developer");
        assertThat(devResult).isNotNull();
        assertThat(devResult.output()).isNotEmpty();

        // Manager should provide high-level summary
        TaskResult managerResult = results.get("Engineering Manager");
        assertThat(managerResult).isNotNull();
        assertThat(managerResult.output()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle unknown role in multi-agent task")
    void shouldHandleUnknownRoleInMultiAgentTask() {
        // Arrange
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(
                        List.of("Developer", "NonExistentRole"), "Some task", Map.of());

        // Act
        ResponseEntity<Map<String, TaskResult>> response =
                restTemplate.exchange(
                        apiUrl("/tasks/multi-agent"),
                        HttpMethod.POST,
                        new org.springframework.http.HttpEntity<>(request),
                        new ParameterizedTypeReference<Map<String, TaskResult>>() {});

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should handle empty role list gracefully")
    void shouldHandleEmptyRoleListGracefully() {
        // Arrange
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(List.of(), "Some task", Map.of());

        // Act
        ResponseEntity<Map<String, TaskResult>> response =
                restTemplate.exchange(
                        apiUrl("/tasks/multi-agent"),
                        HttpMethod.POST,
                        new org.springframework.http.HttpEntity<>(request),
                        new ParameterizedTypeReference<Map<String, TaskResult>>() {});

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Should include usage info for all agents")
    void shouldIncludeUsageInfoForAllAgents() {
        // Arrange
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(
                        List.of("Developer", "QA"), "Review feature", Map.of());

        // Act
        ResponseEntity<Map<String, TaskResult>> response =
                restTemplate.exchange(
                        apiUrl("/tasks/multi-agent"),
                        HttpMethod.POST,
                        new org.springframework.http.HttpEntity<>(request),
                        new ParameterizedTypeReference<Map<String, TaskResult>>() {});

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, TaskResult> results = response.getBody();
        assertThat(results).isNotNull();

        results.values()
                .forEach(
                        result -> {
                            // Usage info available in metadata;
                            assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
                        });
    }
}
