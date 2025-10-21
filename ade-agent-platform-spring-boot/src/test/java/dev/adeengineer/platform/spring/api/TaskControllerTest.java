package dev.adeengineer.platform.spring.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.platform.core.RoleManager;
import dev.adeengineer.platform.testutil.TestData;

@WebMvcTest(TaskController.class)
@DisplayName("TaskController REST API Tests")
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private RoleManager roleManager;

    @Test
    @DisplayName("POST /api/tasks/execute should return task result on success")
    void shouldExecuteTaskSuccessfully() throws Exception {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        TaskResult result = TestData.successfulTaskResult();
        when(roleManager.executeTask(any(TaskRequest.class))).thenReturn(result);

        // When/Then
        mockMvc.perform(
                        post("/api/tasks/execute")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentName").value(result.agentName()))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/tasks/execute should return 400 on IllegalArgumentException")
    void shouldReturn400OnInvalidRequest() throws Exception {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        when(roleManager.executeTask(any(TaskRequest.class)))
                .thenThrow(new IllegalArgumentException("Unknown agent"));

        // When/Then
        mockMvc.perform(
                        post("/api/tasks/execute")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks/execute should return 500 on unexpected exception")
    void shouldReturn500OnUnexpectedException() throws Exception {
        // Given
        TaskRequest request = TestData.validTaskRequest();
        when(roleManager.executeTask(any(TaskRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When/Then
        mockMvc.perform(
                        post("/api/tasks/execute")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/tasks/multi-agent should return results for multiple roles")
    void shouldExecuteMultiAgentTaskSuccessfully() throws Exception {
        // Given
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(
                        List.of("Developer", "QA"), "Test task", Map.of());
        Map<String, TaskResult> results =
                Map.of(
                        "Developer", TestData.taskResultWithRole("Developer"),
                        "QA", TestData.taskResultWithRole("QA"));
        when(roleManager.executeMultiAgentTask(anyList(), anyString(), anyMap()))
                .thenReturn(results);

        // When/Then
        mockMvc.perform(
                        post("/api/tasks/multi-agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Developer").exists())
                .andExpect(jsonPath("$.QA").exists());
    }

    @Test
    @DisplayName("POST /api/tasks/multi-agent should return 400 on invalid role")
    void shouldReturn400OnInvalidRoleInMultiAgent() throws Exception {
        // Given
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(List.of("InvalidRole"), "Test task", Map.of());
        when(roleManager.executeMultiAgentTask(anyList(), anyString(), anyMap()))
                .thenThrow(new IllegalArgumentException("Unknown agent"));

        // When/Then
        mockMvc.perform(
                        post("/api/tasks/multi-agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks/multi-agent should return 500 on execution failure")
    void shouldReturn500OnMultiAgentExecutionFailure() throws Exception {
        // Given
        TaskController.MultiAgentRequest request =
                new TaskController.MultiAgentRequest(List.of("Developer"), "Test task", Map.of());
        when(roleManager.executeMultiAgentTask(anyList(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Execution failed"));

        // When/Then
        mockMvc.perform(
                        post("/api/tasks/multi-agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
