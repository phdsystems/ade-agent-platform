package dev.adeengineer.platform.quarkus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentInfo;

import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.test.quarkus.BaseQuarkusTest;
import dev.adeengineer.platform.test.quarkus.QuarkusTestProfiles;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Tests for AgentResource REST API.
 *
 * <p>Uses AgentUnit framework with BaseQuarkusTest for standardized testing.
 *
 * @since 0.2.0
 */
@QuarkusTest
@TestProfile(QuarkusTestProfiles.MockLLMProfile.class)
@DisplayName("AgentResource REST API Tests")
class AgentResourceTest extends BaseQuarkusTest {

    @InjectMock AgentRegistry agentRegistry;

    private AgentInfo testAgentInfo;
    private Agent mockAgent;

    @BeforeEach
    void setUp() {
        super.setUpQuarkusTest();

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

        // When/Then
        given().when()
                .get("/api/agents")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("$", hasSize(3))
                .body("[0].name", equalTo("Developer"))
                .body("[1].name", equalTo("QA"))
                .body("[2].name", equalTo("Security"));
    }

    @Test
    @DisplayName("GET /api/agents/{agentName} should return agent info when agent exists")
    void shouldGetAgentWhenExists() {
        // Given
        String agentName = "TestAgent";
        when(agentRegistry.hasAgent(agentName)).thenReturn(true);
        when(agentRegistry.getAgent(agentName)).thenReturn(mockAgent);

        // When/Then
        given().when()
                .get("/api/agents/{agentName}", agentName)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("name", equalTo("TestAgent"))
                .body("description", equalTo("Test agent description"))
                .body("capabilities", hasSize(2))
                .body("capabilities[0]", equalTo("capability1"))
                .body("capabilities[1]", equalTo("capability2"));
    }

    @Test
    @DisplayName("GET /api/agents/{agentName} should return 404 when agent not found")
    void shouldReturn404WhenAgentNotFound() {
        // Given
        String agentName = "NonExistentAgent";
        List<String> availableAgents = List.of("Developer", "QA", "Security");
        when(agentRegistry.hasAgent(agentName)).thenReturn(false);
        when(agentRegistry.getAvailableAgents()).thenReturn(availableAgents);

        // When/Then
        given().when()
                .get("/api/agents/{agentName}", agentName)
                .then()
                .statusCode(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", equalTo("Agent not found: NonExistentAgent"))
                .body("availableAgents", hasSize(3))
                .body("availableAgents[0]", equalTo("Developer"));
    }

    @Test
    @DisplayName("POST /api/agents/{agentName}/execute should execute agent task")
    void shouldExecuteAgentTask() {
        // Given
        String agentName = "TestAgent";
        when(agentRegistry.hasAgent(agentName)).thenReturn(true);
        when(agentRegistry.getAgent(agentName)).thenReturn(mockAgent);

        String taskDescription = "Write unit tests";
        AgentResource.AgentTask task = new AgentResource.AgentTask(taskDescription);

        // When/Then
        given().contentType(MediaType.APPLICATION_JSON)
                .body(task)
                .when()
                .post("/api/agents/{agentName}/execute", agentName)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("agentName", equalTo(agentName))
                .body("task", equalTo(taskDescription))
                .body("result", notNullValue());
    }

    @Test
    @DisplayName("POST /api/agents/{agentName}/execute should return 404 when agent not found")
    void shouldReturn404WhenExecutingNonExistentAgent() {
        // Given
        String agentName = "NonExistentAgent";
        List<String> availableAgents = List.of("Developer", "QA");
        when(agentRegistry.hasAgent(agentName)).thenReturn(false);
        when(agentRegistry.getAvailableAgents()).thenReturn(availableAgents);

        AgentResource.AgentTask task = new AgentResource.AgentTask("Some task");

        // When/Then
        given().contentType(MediaType.APPLICATION_JSON)
                .body(task)
                .when()
                .post("/api/agents/{agentName}/execute", agentName)
                .then()
                .statusCode(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", equalTo("Agent not found: NonExistentAgent"))
                .body("availableAgents", hasSize(2));
    }
}
