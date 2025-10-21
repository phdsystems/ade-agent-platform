package adeengineer.dev.platform.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.OutputFormatterRegistry;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;
import adeengineer.dev.llm.LLMProvider;
import adeengineer.dev.llm.model.LLMResponse;
import adeengineer.dev.llm.model.UsageInfo;
import adeengineer.dev.platform.config.AgentConfigLoader;

/** Integration tests for domain loading system. */
class DomainLoadingIntegrationTest {

    @TempDir Path tempDir;

    private AgentRegistry agentRegistry;
    private AgentConfigLoader agentConfigLoader;
    private OutputFormatterRegistry formatterRegistry;
    private LLMProvider mockLLMProvider;
    private DomainLoader domainLoader;

    @BeforeEach
    void setUp() {
        agentRegistry = new AgentRegistry();
        agentConfigLoader = new AgentConfigLoader("config/agents");
        formatterRegistry = new OutputFormatterRegistry();

        mockLLMProvider = mock(LLMProvider.class);

        // Mock LLM provider responses
        UsageInfo usage = new UsageInfo(10, 20, 30, 0.0001);
        LLMResponse mockResponse =
                new LLMResponse("Test response", usage, "mock-provider", "mock-model");
        when(mockLLMProvider.generate(anyString(), anyDouble(), anyInt())).thenReturn(mockResponse);

        domainLoader = new DomainLoader(agentRegistry, agentConfigLoader, formatterRegistry);
    }

    @Test
    void testLoadSingleDomain() throws IOException {
        // Create test domain
        Path domainPath = createTestDomain(tempDir, "healthcare", true);

        // Load domain
        int agentCount = domainLoader.loadDomain(domainPath.toString(), mockLLMProvider);

        // Verify agents loaded
        assertThat(agentCount).isEqualTo(1);
        assertThat(agentRegistry.getAgentCount()).isEqualTo(1);

        Agent agent = agentRegistry.getAgent("Healthcare Test Agent");
        assertThat(agent).isNotNull();
        assertThat(agent.getName()).isEqualTo("Healthcare Test Agent");
    }

    @Test
    void testLoadMultipleDomains() throws IOException {
        // Create multiple test domains
        Path domain1 = createTestDomain(tempDir.resolve("healthcare"), "healthcare", true);
        Path domain2 = createTestDomain(tempDir.resolve("legal"), "legal", true);

        // Load both domains
        int count1 = domainLoader.loadDomain(domain1.toString(), mockLLMProvider);
        int count2 = domainLoader.loadDomain(domain2.toString(), mockLLMProvider);

        // Verify agents loaded
        assertThat(count1).isEqualTo(1);
        assertThat(count2).isEqualTo(1);
        assertThat(agentRegistry.getAgentCount()).isEqualTo(2);
    }

    @Test
    void testLoadAllDomains() throws IOException {
        // Create domains directory with multiple domains
        Path domainsDir = tempDir.resolve("domains");
        Files.createDirectories(domainsDir);

        createTestDomain(domainsDir.resolve("healthcare"), "healthcare", true);
        createTestDomain(domainsDir.resolve("legal"), "legal", true);
        createTestDomain(domainsDir.resolve("finance"), "finance", true);

        // Load all domains
        int totalAgents = domainLoader.loadAllDomains(domainsDir.toString(), mockLLMProvider);

        // Verify all agents loaded
        assertThat(totalAgents).isEqualTo(3);
        assertThat(agentRegistry.getAgentCount()).isEqualTo(3);
    }

    @Test
    void testDisabledDomainNotLoaded() throws IOException {
        // Create disabled domain
        Path domainPath = createTestDomain(tempDir, "disabled-domain", false);

        // Attempt to load
        int agentCount = domainLoader.loadDomain(domainPath.toString(), mockLLMProvider);

        // Verify no agents loaded
        assertThat(agentCount).isZero();
        assertThat(agentRegistry.getAgentCount()).isZero();
    }

    @Test
    void testExecuteTaskOnLoadedAgent() throws IOException {
        // Create and load domain
        Path domainPath = createTestDomain(tempDir, "test", true);
        domainLoader.loadDomain(domainPath.toString(), mockLLMProvider);

        // Get agent
        Agent agent = agentRegistry.getAgent("Healthcare Test Agent");
        assertThat(agent).isNotNull();

        // Execute task
        TaskRequest request =
                new TaskRequest(
                        "Healthcare Test Agent",
                        "Diagnose patient",
                        Map.of("symptoms", "fever, cough"));

        TaskResult result = agent.executeTask(request);

        // Verify result
        assertThat(result.success()).isTrue();
        assertThat(result.agentName()).isEqualTo("Healthcare Test Agent");
        assertThat(result.task()).isEqualTo("Diagnose patient");
        assertThat(result.output()).contains("Test response");
    }

    @Test
    void testConfigurableAgentVsLegacyAgent() throws IOException {
        // Create domain and load with new system
        Path domainPath = createTestDomain(tempDir, "test", true);
        domainLoader.loadDomain(domainPath.toString(), mockLLMProvider);

        Agent newSystemAgent = agentRegistry.getAgent("Healthcare Test Agent");

        // Verify it's a ConfigurableAgent
        assertThat(newSystemAgent).isInstanceOf(ConfigurableAgent.class);

        // Execute task and verify behavior
        TaskRequest request = new TaskRequest("Healthcare Test Agent", "Test task", Map.of());

        TaskResult result = newSystemAgent.executeTask(request);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isNotNull();
    }

    @Test
    void testDomainConfigValidation() throws IOException {
        // Create domain with invalid config (missing name)
        Path domainPath = tempDir.resolve("invalid-domain");
        Files.createDirectories(domainPath);

        String invalidDomainYaml =
                """
                version: "1.0.0"
                description: "Invalid domain - missing name"
                outputFormats: []
                agentDirectory: "agents/"
                """;

        Files.writeString(domainPath.resolve("domain.yaml"), invalidDomainYaml);

        // Attempt to load should fail gracefully
        int agentCount = domainLoader.loadDomain(domainPath.toString(), mockLLMProvider);

        // Should load 0 agents due to validation failure
        assertThat(agentCount).isZero();
    }

    /** Helper method to create a test domain structure. */
    private Path createTestDomain(Path basePath, String domainName, boolean enabled)
            throws IOException {
        Files.createDirectories(basePath);
        Path agentsDir = basePath.resolve("agents");
        Files.createDirectories(agentsDir);

        // Create domain.yaml
        String domainYaml =
                String.format(
                        """
                name: "%s"
                version: "1.0.0"
                description: "Test domain for %s"
                outputFormats:
                  - technical
                  - business
                agentDirectory: "agents/"
                dependencies: []
                enabled: %s
                """,
                        domainName, domainName, enabled);

        Files.writeString(basePath.resolve("domain.yaml"), domainYaml);

        // Create agent YAML
        String agentYaml =
                String.format(
                        """
                name: "Healthcare Test Agent"
                description: "Test agent for %s domain"
                capabilities:
                  - "Testing"
                  - "Validation"
                temperature: 0.5
                maxTokens: 1024
                outputFormat: "technical"
                promptTemplate: |
                  You are a test agent.
                  Task: {task}
                  Context: {context}
                """,
                        domainName);

        Files.writeString(agentsDir.resolve("test-agent.yaml"), agentYaml);

        return basePath;
    }
}
