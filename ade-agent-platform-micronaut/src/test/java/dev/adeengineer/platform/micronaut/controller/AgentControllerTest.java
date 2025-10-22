package dev.adeengineer.platform.micronaut.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;

import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.micronaut.controller.AgentController.AgentResponse;
import dev.adeengineer.platform.micronaut.controller.AgentController.AgentTask;
import dev.adeengineer.platform.micronaut.controller.AgentController.ErrorResponse;
import dev.adeengineer.platform.test.micronaut.BaseMicronautTest;
import dev.adeengineer.platform.test.micronaut.MicronautTestProperties;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

/**
 * Tests for AgentController REST API.
 *
 * <p>Uses AgentUnit framework with BaseMicronautTest for standardized testing.
 *
 * @since 0.2.0
 */
@MicronautTest
@DisplayName("AgentController REST API Tests")
class AgentControllerTest extends BaseMicronautTest
        implements MicronautTestProperties.MockLLMProperties {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject AgentRegistry agentRegistry;

    private AgentInfo testAgentInfo;
    private Agent mockAgent;

    @MockBean(AgentRegistry.class)
    AgentRegistry agentRegistry() {
        return org.mockito.Mockito.mock(AgentRegistry.class);
    }

    @BeforeEach
    void setUp() {
        super.setUpMicronautTest();

        // Create test agent info
        testAgentInfo =
                new AgentInfo(
                        "TestAgent",
                        "Test agent description",
                        List.of("capability1", "capability2"),
                        "test-provider");

        // Create mock agent
        mockAgent = org.mockito.Mockito.mock(Agent.class);
        when(mockAgent.getAgentInfo()).thenReturn(testAgentInfo);
    }

    @Test
    @DisplayName("GET /api/agents should return list of agents")
    void shouldListAllAgents() {
        // Given
        List<AgentInfo> agents =
                List.of(
                        new AgentInfo("Developer", "Dev agent", List.of("coding"), "ollama"),
                        new AgentInfo("QA", "QA agent", List.of("testing"), "ollama"),
                        new AgentInfo("Security", "Security agent", List.of("security"), "ollama"));
        when(agentRegistry.getAllAgentsInfo()).thenReturn(agents);

        // When
        HttpResponse<List> response = client.toBlocking().exchange("/api/agents", List.class);

        // Then
        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).hasSize(3);
    }

    @Test
    @DisplayName("GET /api/agents/{agentName} should return agent info when agent exists")
    void shouldGetAgentWhenExists() {
        // Given
        String agentName = "TestAgent";
        when(agentRegistry.hasAgent(agentName)).thenReturn(true);
        when(agentRegistry.getAgent(agentName)).thenReturn(mockAgent);

        // When
        HttpResponse<AgentInfo> response =
                client.toBlocking()
                        .exchange(HttpRequest.GET("/api/agents/" + agentName), AgentInfo.class);

        // Then
        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().name()).isEqualTo("TestAgent");
        assertThat(response.body().description()).isEqualTo("Test agent description");
        assertThat(response.body().capabilities()).hasSize(2);
    }

    @Test
    @DisplayName("GET /api/agents/{agentName} should return 404 when agent not found")
    void shouldReturn404WhenAgentNotFound() {
        // Given
        String agentName = "NonExistentAgent";
        List<String> availableAgents = List.of("Developer", "QA", "Security");
        when(agentRegistry.hasAgent(agentName)).thenReturn(false);
        when(agentRegistry.getAvailableAgents()).thenReturn(availableAgents);

        // When & Then
        HttpClientResponseException exception =
                assertThrows(
                        HttpClientResponseException.class,
                        () ->
                                client.toBlocking()
                                        .exchange(
                                                HttpRequest.GET("/api/agents/" + agentName),
                                                ErrorResponse.class));

        assertThat((Object) exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/agents/{agentName}/execute should execute agent task")
    void shouldExecuteAgentTask() {
        // Given
        String agentName = "TestAgent";
        when(agentRegistry.hasAgent(agentName)).thenReturn(true);
        when(agentRegistry.getAgent(agentName)).thenReturn(mockAgent);

        String taskDescription = "Write unit tests";
        AgentTask task = new AgentTask(taskDescription);

        // When
        HttpResponse<AgentResponse> response =
                client.toBlocking()
                        .exchange(
                                HttpRequest.POST("/api/agents/" + agentName + "/execute", task),
                                AgentResponse.class);

        // Then
        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().agentName()).isEqualTo(agentName);
        assertThat(response.body().task()).isEqualTo(taskDescription);
        assertThat(response.body().result()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/agents/{agentName}/execute should return 404 when agent not found")
    void shouldReturn404WhenExecutingNonExistentAgent() {
        // Given
        String agentName = "NonExistentAgent";
        List<String> availableAgents = List.of("Developer", "QA");
        when(agentRegistry.hasAgent(agentName)).thenReturn(false);
        when(agentRegistry.getAvailableAgents()).thenReturn(availableAgents);

        AgentTask task = new AgentTask("Some task");

        // When & Then
        HttpClientResponseException exception =
                assertThrows(
                        HttpClientResponseException.class,
                        () ->
                                client.toBlocking()
                                        .exchange(
                                                HttpRequest.POST(
                                                        "/api/agents/" + agentName + "/execute",
                                                        task),
                                                ErrorResponse.class));

        assertThat((Object) exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
