package dev.adeengineer.platform.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.adeengineer.agent.AgentInfo;
import dev.adeengineer.platform.core.RoleManager;
import dev.adeengineer.platform.testutil.TestData;

@WebMvcTest(RoleController.class)
@DisplayName("RoleController REST API Tests")
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private RoleManager roleManager;

    @Test
    @DisplayName("GET /api/roles should return list of roles")
    void shouldListAllRoles() throws Exception {
        // Given
        List<String> roles = List.of("Developer", "QA", "Security");
        when(roleManager.listRoles()).thenReturn(roles);

        // When/Then
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Developer"))
                .andExpect(jsonPath("$[1]").value("QA"))
                .andExpect(jsonPath("$[2]").value("Security"));
    }

    @Test
    @DisplayName("GET /api/roles should return empty list when no roles")
    void shouldReturnEmptyListWhenNoRoles() throws Exception {
        // Given
        when(roleManager.listRoles()).thenReturn(List.of());

        // When/Then
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/roles/info should return detailed role information")
    void shouldReturnRolesInfo() throws Exception {
        // Given
        List<AgentInfo> rolesInfo =
                List.of(TestData.roleInfoWithRole("Developer"), TestData.roleInfoWithRole("QA"));
        when(roleManager.getRolesInfo()).thenReturn(rolesInfo);

        // When/Then
        mockMvc.perform(get("/api/roles/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roleName").value("Developer"))
                .andExpect(jsonPath("$[1].roleName").value("QA"));
    }

    @Test
    @DisplayName("GET /api/roles/{roleName} should return specific role info")
    void shouldDescribeRole() throws Exception {
        // Given
        AgentInfo roleInfo = TestData.validAgentInfo();
        when(roleManager.describeRole("Developer")).thenReturn(roleInfo);

        // When/Then
        mockMvc.perform(get("/api/roles/Developer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleName").value("Developer"))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.capabilities").isArray());
    }

    @Test
    @DisplayName("GET /api/roles/{roleName} should return 404 when role not found")
    void shouldReturn404WhenRoleNotFound() throws Exception {
        // Given
        when(roleManager.describeRole(anyString()))
                .thenThrow(new IllegalArgumentException("Unknown agent"));

        // When/Then
        mockMvc.perform(get("/api/roles/NonExistent")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/roles/{roleName} should handle special characters in role name")
    void shouldHandleSpecialCharactersInRoleName() throws Exception {
        // Given
        AgentInfo roleInfo = TestData.roleInfoWithRole("Senior Developer");
        when(roleManager.describeRole("Senior Developer")).thenReturn(roleInfo);

        // When/Then
        mockMvc.perform(get("/api/roles/Senior Developer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleName").value("Senior Developer"));
    }
}
