package dev.adeengineer.platform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.adeengineer.agent.AgentInfo;

/**
 * E2E tests for Role Discovery API endpoints. Tests listing, querying, and discovering available
 * roles.
 */
@DisplayName("Role Discovery E2E Tests")
class RoleDiscoveryE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should list all available roles")
    void shouldListAllAvailableRoles() {
        // Act
        ResponseEntity<List<String>> response =
                restTemplate.exchange(
                        apiUrl("/roles"),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<String>>() {});

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<String> roles = response.getBody();
        assertThat(roles).isNotEmpty();
        assertThat(roles).contains("Developer", "QA", "Security");
    }

    @Test
    @DisplayName("Should get detailed info for all roles")
    void shouldGetDetailedInfoForAllRoles() {
        // Act
        ResponseEntity<List<AgentInfo>> response =
                restTemplate.exchange(
                        apiUrl("/roles/info"),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<AgentInfo>>() {});

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<AgentInfo> rolesInfo = response.getBody();
        assertThat(rolesInfo).isNotEmpty();

        AgentInfo firstRole = rolesInfo.get(0);
        assertThat(firstRole.name()).isNotNull();
        assertThat(firstRole.description()).isNotNull();
        assertThat(firstRole.capabilities()).isNotNull();
    }

    @Test
    @DisplayName("Should describe specific role")
    void shouldDescribeSpecificRole() {
        // Act
        ResponseEntity<AgentInfo> response =
                restTemplate.getForEntity(apiUrl("/roles/Developer"), AgentInfo.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AgentInfo roleInfo = response.getBody();
        assertThat(roleInfo.name()).isEqualTo("Developer");
        assertThat(roleInfo.description()).isNotEmpty();
        assertThat(roleInfo.capabilities()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return 404 for unknown role")
    void shouldReturn404ForUnknownRole() {
        // Act
        ResponseEntity<AgentInfo> response =
                restTemplate.getForEntity(apiUrl("/roles/NonExistentRole"), AgentInfo.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should verify health endpoint is accessible")
    void shouldVerifyHealthEndpointAccessible() {
        // Act
        ResponseEntity<String> response =
                restTemplate.getForEntity(actuatorUrl("/health"), String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}
