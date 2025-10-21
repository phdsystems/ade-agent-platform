package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.RoleManager;
import dev.adeengineer.platform.testutil.TestData;

/**
 * Integration tests for RoleManager with real AgentRegistry and agents. Tests service layer
 * collaboration without HTTP layer.
 */
@DisplayName("Role Manager Integration Tests")
class RoleManagerIntegrationTest extends BaseIntegrationTest {

    @Autowired private RoleManager roleManager;

    @Autowired private AgentRegistry registry;

    @Test
    @DisplayName("Should execute task with real agent from registry")
    void shouldExecuteTaskWithRealAgentFromRegistry() {
        // Given
        TaskRequest request =
                new TaskRequest(
                        "Software Developer",
                        "Write a unit test for a calculator class",
                        Map.of("language", "Java"));

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Software Developer");
        assertThat(result.task()).isEqualTo("Write a unit test for a calculator class");
        assertThat(result.output()).isNotBlank();
        // Usage info available in metadata;
        assertThat(result.durationMs()).isPositive();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("Should list all registered roles")
    void shouldListAllRegisteredRoles() {
        // When
        List<String> roles = roleManager.listRoles();

        // Then
        assertThat(roles).isNotEmpty();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(4);

        // Verify expected roles are present
        assertThat(roles)
                .contains(
                        "Software Developer",
                        "QA Engineer",
                        "Security Engineer",
                        "Engineering Manager");

        // Verify each role can be described
        for (String roleName : roles) {
            AgentInfo roleInfo = roleManager.describeRole(roleName);
            assertThat(roleInfo.name()).isEqualTo(roleName);
            assertThat(roleInfo.description()).isNotBlank();
            assertThat(roleInfo.capabilities()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Should describe specific role from registry")
    void shouldDescribeSpecificRoleFromRegistry() {
        // When
        AgentInfo developerInfo = roleManager.describeRole("Software Developer");
        AgentInfo qaInfo = roleManager.describeRole("QA Engineer");

        // Then
        assertThat(developerInfo).isNotNull();
        assertThat(developerInfo.name()).isEqualTo("Software Developer");
        assertThat(developerInfo.description()).isNotBlank();
        assertThat(developerInfo.capabilities()).isNotEmpty();

        assertThat(qaInfo).isNotNull();
        assertThat(qaInfo.name()).isEqualTo("QA Engineer");
        assertThat(qaInfo.description()).isNotBlank();
        assertThat(qaInfo.capabilities()).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw exception for unknown role")
    void shouldThrowExceptionForUnknownRole() {
        // Given
        TaskRequest request = TestData.taskRequestWithRole("NonExistentRole");

        // When/Then
        assertThatThrownBy(() -> roleManager.executeTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown agent")
                .hasMessageContaining("NonExistentRole");
    }

    @Test
    @DisplayName("Should execute multiple tasks with same agent")
    void shouldExecuteMultipleTasksWithSameAgent() {
        // Given
        TaskRequest request1 =
                new TaskRequest("Software Developer", "Implement a REST API endpoint", Map.of());
        TaskRequest request2 =
                new TaskRequest("Software Developer", "Write integration tests", Map.of());

        // When
        TaskResult result1 = roleManager.executeTask(request1);
        TaskResult result2 = roleManager.executeTask(request2);

        // Then
        assertThat(result1.success()).isTrue();
        assertThat(result2.success()).isTrue();
        assertThat(result1.agentName()).isEqualTo("Software Developer");
        assertThat(result2.agentName()).isEqualTo("Software Developer");
        assertThat(result1.output()).isNotBlank();
        assertThat(result2.output()).isNotBlank();
    }

    @Test
    @DisplayName("Should execute tasks with different agents")
    void shouldExecuteTasksWithDifferentAgents() {
        // Given
        TaskRequest developerRequest =
                new TaskRequest("Software Developer", "Implement authentication", Map.of());
        TaskRequest qaRequest =
                new TaskRequest("QA Engineer", "Write test cases for authentication", Map.of());

        // When
        TaskResult developerResult = roleManager.executeTask(developerRequest);
        TaskResult qaResult = roleManager.executeTask(qaRequest);

        // Then
        assertThat(developerResult.success()).isTrue();
        assertThat(qaResult.success()).isTrue();
        assertThat(developerResult.agentName()).isEqualTo("Software Developer");
        assertThat(qaResult.agentName()).isEqualTo("QA Engineer");
        assertThat(developerResult.output()).isNotBlank();
        assertThat(qaResult.output()).isNotBlank();
    }

    @Test
    @DisplayName("Should execute multi-agent task with real agents")
    void shouldExecuteMultiAgentTaskWithRealAgents() {
        // Given
        List<String> roles = List.of("Software Developer", "QA Engineer");
        String task = "Design and test a new feature";
        Map<String, Object> context = Map.of("feature", "user authentication");

        // When
        Map<String, TaskResult> results = roleManager.executeMultiAgentTask(roles, task, context);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsKeys("Software Developer", "QA Engineer");

        TaskResult developerResult = results.get("Software Developer");
        TaskResult qaResult = results.get("QA Engineer");

        assertThat(developerResult.success()).isTrue();
        assertThat(qaResult.success()).isTrue();
        assertThat(developerResult.output()).isNotBlank();
        assertThat(qaResult.output()).isNotBlank();
    }

    @Test
    @DisplayName("Should throw exception for multi-agent task with unknown role")
    void shouldThrowExceptionForMultiAgentTaskWithUnknownRole() {
        // Given
        List<String> roles = List.of("Software Developer", "NonExistentRole");
        String task = "Test task";

        // When/Then
        assertThatThrownBy(() -> roleManager.executeMultiAgentTask(roles, task, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown agent");
    }

    @Test
    @DisplayName("Should handle task execution with context parameters")
    void shouldHandleTaskExecutionWithContextParameters() {
        // Given
        TaskRequest request =
                new TaskRequest(
                        "Software Developer",
                        "Optimize database query",
                        Map.of(
                                "database", "PostgreSQL",
                                "table", "users",
                                "priority", "high"));

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.output()).isNotBlank();
        // Usage info available in metadata;
    }

    @Test
    @DisplayName("Should return usage information for executed tasks")
    void shouldReturnUsageInformationForExecutedTasks() {
        // Given
        TaskRequest request = new TaskRequest("QA Engineer", "Review test coverage", Map.of());

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        // Usage info available in metadata
        assertThat(result.metadata()).isNotNull();
        assertThat((Integer) result.metadata().get("inputTokens")).isPositive();
        assertThat((Integer) result.metadata().get("outputTokens")).isPositive();
        assertThat((Integer) result.metadata().get("totalTokens")).isPositive();
        assertThat((Double) result.metadata().get("estimatedCost")).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Should measure task execution duration")
    void shouldMeasureTaskExecutionDuration() {
        // Given
        TaskRequest request =
                new TaskRequest("Engineering Manager", "Create project timeline", Map.of());

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.durationMs()).isPositive();
        assertThat(result.durationMs()).isLessThan(10000L); // Should complete within 10 seconds
    }
}
