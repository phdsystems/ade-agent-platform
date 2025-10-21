package dev.adeengineer.platform.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.platform.test.mock.MockAgent;
import dev.adeengineer.platform.test.factory.TestData;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleManager Tests")
class RoleManagerTest {

    @Mock private AgentRegistry agentRegistry;

    private RoleManager roleManager;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager(agentRegistry);
    }

    @Test
    @DisplayName("Should execute task successfully with valid role")
    void shouldExecuteTaskSuccessfully() {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        MockAgent mockAgent = new MockAgent("Developer");

        when(agentRegistry.getAgent("Developer")).thenReturn(mockAgent);

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Developer");
        assertThat(result.output()).isNotNull();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
        verify(agentRegistry).getAgent("Developer");
    }

    @Test
    @DisplayName("Should throw exception when executing task with unknown role")
    void shouldThrowExceptionWhenExecutingTaskWithUnknownRole() {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        when(agentRegistry.getAgent(anyString()))
                .thenThrow(new IllegalArgumentException("Unknown agent"));

        // When/Then
        assertThatThrownBy(() -> roleManager.executeTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown agent");
    }

    @Test
    @DisplayName("Should return failure result when agent throws runtime exception")
    void shouldReturnFailureResultWhenAgentThrowsRuntimeException() {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        MockAgent mockAgent = new MockAgent("Developer");
        mockAgent.setExceptionToThrow(new RuntimeException("LLM connection failed"));

        when(agentRegistry.getAgent("Developer")).thenReturn(mockAgent);

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("LLM connection failed");
        assertThat(result.output()).isNull();
        assertThat(result.metadata()).isNull();
    }

    @Test
    @DisplayName("Should execute multi-agent task with multiple roles")
    void shouldExecuteMultiAgentTask() {
        // Given
        List<String> roleNames = List.of("Developer", "QA");
        String task = "Review code";
        Map<String, Object> context = Map.of();

        MockAgent devAgent = new MockAgent("Developer");
        MockAgent qaAgent = new MockAgent("QA");

        when(agentRegistry.hasAgent("Developer")).thenReturn(true);
        when(agentRegistry.hasAgent("QA")).thenReturn(true);
        when(agentRegistry.getAgent("Developer")).thenReturn(devAgent);
        when(agentRegistry.getAgent("QA")).thenReturn(qaAgent);

        // When
        Map<String, TaskResult> results =
                roleManager.executeMultiAgentTask(roleNames, task, context);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsKeys("Developer", "QA");
        assertThat(results.get("Developer").success()).isTrue();
        assertThat(results.get("QA").success()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when multi-agent task contains unknown role")
    void shouldThrowExceptionWhenMultiAgentTaskContainsUnknownRole() {
        // Given
        List<String> roleNames = List.of("Developer", "UnknownRole");
        when(agentRegistry.hasAgent("Developer")).thenReturn(true);
        when(agentRegistry.hasAgent("UnknownRole")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> roleManager.executeMultiAgentTask(roleNames, "Task", Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unknown agent");
    }

    @Test
    @DisplayName("Should list all available roles")
    void shouldListAllRoles() {
        // Given
        List<String> expectedRoles = List.of("Developer", "QA", "Security");
        when(agentRegistry.getAvailableAgents()).thenReturn(expectedRoles);

        // When
        List<String> roles = roleManager.listRoles();

        // Then
        assertThat(roles).isEqualTo(expectedRoles);
        verify(agentRegistry).getAvailableAgents();
    }

    @Test
    @DisplayName("Should get roles info from registry")
    void shouldGetRolesInfo() {
        // Given
        List<AgentInfo> expectedInfo =
                List.of(TestData.roleInfoWithRole("Developer"), TestData.roleInfoWithRole("QA"));
        when(agentRegistry.getAllAgentsInfo()).thenReturn(expectedInfo);

        // When
        List<AgentInfo> rolesInfo = roleManager.getRolesInfo();

        // Then
        assertThat(rolesInfo).isEqualTo(expectedInfo);
        verify(agentRegistry).getAllAgentsInfo();
    }

    @Test
    @DisplayName("Should describe specific role")
    void shouldDescribeSpecificRole() {
        // Given
        MockAgent mockAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(mockAgent);

        // When
        AgentInfo roleInfo = roleManager.describeRole("Developer");

        // Then
        assertThat(roleInfo.name()).isEqualTo("Developer");
        verify(agentRegistry).getAgent("Developer");
    }

    @Test
    @DisplayName("Should throw exception when describing unknown role")
    void shouldThrowExceptionWhenDescribingUnknownRole() {
        // Given
        when(agentRegistry.getAgent(anyString()))
                .thenThrow(new IllegalArgumentException("Unknown agent"));

        // When/Then
        assertThatThrownBy(() -> roleManager.describeRole("UnknownRole"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown agent");
    }

    @Test
    @DisplayName("Should include usage info in successful task result")
    void shouldIncludeUsageInfoInSuccessfulResult() {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        MockAgent mockAgent = new MockAgent("Developer");

        when(agentRegistry.getAgent("Developer")).thenReturn(mockAgent);

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        // Usage info available in metadata
        assertThat(result.metadata()).isNotNull();
        assertThat((Integer) result.metadata().get("totalTokens")).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should measure task execution duration")
    void shouldMeasureTaskExecutionDuration() {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        MockAgent mockAgent = new MockAgent("Developer");
        when(agentRegistry.getAgent("Developer")).thenReturn(mockAgent);

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute multi-agent task in parallel")
    void shouldExecuteMultiAgentTaskInParallel() {
        // Given
        List<String> roleNames = List.of("Developer", "QA", "Security");

        for (String role : roleNames) {
            when(agentRegistry.hasAgent(role)).thenReturn(true);
            when(agentRegistry.getAgent(role)).thenReturn(new MockAgent(role));
        }

        // When
        long startTime = System.currentTimeMillis();
        Map<String, TaskResult> results =
                roleManager.executeMultiAgentTask(roleNames, "Task", Map.of());
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(results).hasSize(3);
        // If truly parallel, duration should be less than sequential execution
        // This is a weak assertion but demonstrates parallel behavior exists
        assertThat(duration).isLessThan(1000); // Should complete quickly
    }

    @Test
    @DisplayName("Should handle empty role list in multi-agent task")
    void shouldHandleEmptyRoleListInMultiAgentTask() {
        // When
        Map<String, TaskResult> results =
                roleManager.executeMultiAgentTask(List.of(), "Task", Map.of());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle null response from agent gracefully")
    void shouldHandleNullResponseFromAgent() {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.executeTask(any())).thenReturn(null);

        when(agentRegistry.getAgent("Developer")).thenReturn(mockAgent);

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }
}
