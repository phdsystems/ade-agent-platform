package dev.adeengineer.platform.cli;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.LLMProviderFactory;
import dev.adeengineer.platform.core.DomainManager;
import dev.adeengineer.platform.core.DomainManager.DomainMetadata;
import dev.adeengineer.platform.model.DomainConfig;

/** Unit tests for DomainCommands. */
class DomainCommandsTest {

    private DomainManager domainManager;
    private LLMProviderFactory llmProviderFactory;
    private DomainCommands commands;
    private LLMProvider mockProvider;

    @BeforeEach
    void setUp() {
        domainManager = mock(DomainManager.class);
        llmProviderFactory = mock(LLMProviderFactory.class);
        commands = new DomainCommands(domainManager, llmProviderFactory);
        mockProvider = mock(LLMProvider.class);

        when(llmProviderFactory.getProviderWithFailover()).thenReturn(mockProvider);
    }

    @Test
    void testListDomains() {
        // Given
        DomainConfig config = createTestDomainConfig("domain1");
        DomainMetadata metadata =
                new DomainMetadata(config, "domains/domain1", 5, System.currentTimeMillis(), true);

        when(domainManager.getLoadedDomains()).thenReturn(Set.of("domain1"));
        when(domainManager.getDomainMetadata("domain1")).thenReturn(metadata);
        when(domainManager.getTotalAgentCount()).thenReturn(5);

        // When
        String result = commands.listDomains();

        // Then
        assertTrue(result.contains("Loaded Domains (1)"));
        assertTrue(result.contains("domain1"));
        assertTrue(result.contains("v1.0.0"));
        assertTrue(result.contains("5 agents"));
        assertTrue(result.contains("Total agents: 5"));
    }

    @Test
    void testListDomains_Empty() {
        // Given
        when(domainManager.getLoadedDomains()).thenReturn(Set.of());

        // When
        String result = commands.listDomains();

        // Then
        assertEquals("No domains loaded.", result);
    }

    @Test
    void testShowDomain() {
        // Given
        DomainConfig config = createTestDomainConfig("test-domain");
        DomainMetadata metadata =
                new DomainMetadata(
                        config, "domains/test-domain", 3, System.currentTimeMillis(), true);

        when(domainManager.getDomainMetadata("test-domain")).thenReturn(metadata);
        when(domainManager.getDomainHealth("test-domain")).thenReturn("HEALTHY");

        // When
        String result = commands.showDomain("test-domain");

        // Then
        assertTrue(result.contains("Domain: test-domain"));
        assertTrue(result.contains("Version: 1.0.0"));
        assertTrue(result.contains("Agents: 3"));
        assertTrue(result.contains("Health: HEALTHY"));
        assertTrue(result.contains("Enabled: Yes"));
    }

    @Test
    void testShowDomain_NotFound() {
        // Given
        when(domainManager.getDomainMetadata("nonexistent")).thenReturn(null);

        // When
        String result = commands.showDomain("nonexistent");

        // Then
        assertTrue(result.contains("Error: Domain not found: nonexistent"));
    }

    @Test
    void testShowDomains() {
        // Given
        DomainConfig config1 = createTestDomainConfig("domain1");
        DomainConfig config2 = createTestDomainConfig("domain2");

        DomainMetadata metadata1 =
                new DomainMetadata(config1, "domains/domain1", 3, System.currentTimeMillis(), true);
        DomainMetadata metadata2 =
                new DomainMetadata(config2, "domains/domain2", 4, System.currentTimeMillis(), true);

        when(domainManager.getAllDomainMetadata())
                .thenReturn(Map.of("domain1", metadata1, "domain2", metadata2));
        when(domainManager.getDomainHealth(any())).thenReturn("HEALTHY");

        // When
        String result = commands.showDomains();

        // Then
        assertTrue(result.contains("Loaded Domains (2)"));
        assertTrue(result.contains("domain1"));
        assertTrue(result.contains("domain2"));
    }

    @Test
    void testShowDomains_Empty() {
        // Given
        when(domainManager.getAllDomainMetadata()).thenReturn(Map.of());

        // When
        String result = commands.showDomains();

        // Then
        assertEquals("No domains loaded.", result);
    }

    @Test
    void testLoadDomain() throws IOException {
        // Given
        String domainPath = "domains/test-domain";
        when(domainManager.loadDomain(eq(domainPath), eq(mockProvider))).thenReturn(5);

        // When
        String result = commands.loadDomain(domainPath);

        // Then
        assertTrue(result.contains("✓ Domain loaded successfully"));
        assertTrue(result.contains("5 agents"));
        assertTrue(result.contains(domainPath));
        verify(domainManager).loadDomain(domainPath, mockProvider);
    }

    @Test
    void testLoadDomain_Failure() throws IOException {
        // Given
        String domainPath = "domains/bad-domain";
        when(domainManager.loadDomain(eq(domainPath), any()))
                .thenThrow(new IOException("Invalid configuration"));

        // When
        String result = commands.loadDomain(domainPath);

        // Then
        assertTrue(result.contains("Error: Failed to load domain"));
        assertTrue(result.contains("Invalid configuration"));
    }

    @Test
    void testReloadDomain() throws IOException {
        // Given
        String domainName = "test-domain";
        when(domainManager.reloadDomain(eq(domainName), eq(mockProvider))).thenReturn(5);

        // When
        String result = commands.reloadDomain(domainName);

        // Then
        assertTrue(result.contains("✓ Domain reloaded successfully"));
        assertTrue(result.contains("5 agents"));
        assertTrue(result.contains(domainName));
    }

    @Test
    void testReloadDomain_NotFound() throws IOException {
        // Given
        String domainName = "nonexistent";
        when(domainManager.reloadDomain(eq(domainName), any()))
                .thenThrow(new IllegalArgumentException("Domain not found"));

        // When
        String result = commands.reloadDomain(domainName);

        // Then
        assertTrue(result.contains("Error: Domain not found"));
    }

    @Test
    void testReloadDomain_IOFailure() throws IOException {
        // Given
        String domainName = "test-domain";
        when(domainManager.reloadDomain(eq(domainName), any()))
                .thenThrow(new IOException("Read error"));

        // When
        String result = commands.reloadDomain(domainName);

        // Then
        assertTrue(result.contains("Error: Failed to reload domain"));
        assertTrue(result.contains("Read error"));
    }

    @Test
    void testUnloadDomain() {
        // Given
        String domainName = "test-domain";
        when(domainManager.unloadDomain(domainName)).thenReturn(5);

        // When
        String result = commands.unloadDomain(domainName);

        // Then
        assertTrue(result.contains("✓ Domain unloaded successfully"));
        assertTrue(result.contains(domainName));
        assertTrue(result.contains("Agents removed: 5"));
    }

    @Test
    void testUnloadDomain_NotFound() {
        // Given
        String domainName = "nonexistent";
        when(domainManager.unloadDomain(domainName))
                .thenThrow(new IllegalArgumentException("Domain not found"));

        // When
        String result = commands.unloadDomain(domainName);

        // Then
        assertTrue(result.contains("Error: Domain not found"));
    }

    @Test
    void testDiscoverDomains() throws IOException {
        // Given
        List<String> discovered = List.of("domains/domain1", "domains/domain2");
        when(domainManager.discoverDomains("domains")).thenReturn(discovered);
        when(domainManager.isDomainLoaded("domain1")).thenReturn(true);
        when(domainManager.isDomainLoaded("domain2")).thenReturn(false);

        // When
        String result = commands.discoverDomains("domains");

        // Then
        assertTrue(result.contains("Discovered Domains (2)"));
        assertTrue(result.contains("domains/domain1"));
        assertTrue(result.contains("[LOADED]"));
        assertTrue(result.contains("domains/domain2"));
        assertTrue(result.contains("To load a domain"));
    }

    @Test
    void testDiscoverDomains_Empty() throws IOException {
        // Given
        when(domainManager.discoverDomains("domains")).thenReturn(List.of());

        // When
        String result = commands.discoverDomains("domains");

        // Then
        assertTrue(result.contains("No domains found in: domains"));
    }

    @Test
    void testDiscoverDomains_Failure() throws IOException {
        // Given
        when(domainManager.discoverDomains("domains"))
                .thenThrow(new IOException("Directory not readable"));

        // When
        String result = commands.discoverDomains("domains");

        // Then
        assertTrue(result.contains("Error: Failed to discover domains"));
        assertTrue(result.contains("Directory not readable"));
    }

    @Test
    void testDomainHealth_Healthy() {
        // Given
        when(domainManager.getDomainHealth("test-domain")).thenReturn("HEALTHY");

        // When
        String result = commands.domainHealth("test-domain");

        // Then
        assertTrue(result.contains("Domain: test-domain"));
        assertTrue(result.contains("Health: HEALTHY"));
        assertTrue(result.contains("✓"));
    }

    @Test
    void testDomainHealth_NotLoaded() {
        // Given
        when(domainManager.getDomainHealth("test-domain")).thenReturn("NOT_LOADED");

        // When
        String result = commands.domainHealth("test-domain");

        // Then
        assertTrue(result.contains("Health: NOT_LOADED"));
        assertTrue(result.contains("(not loaded)"));
    }

    @Test
    void testDomainHealth_Disabled() {
        // Given
        when(domainManager.getDomainHealth("test-domain")).thenReturn("DISABLED");

        // When
        String result = commands.domainHealth("test-domain");

        // Then
        assertTrue(result.contains("Health: DISABLED"));
        assertTrue(result.contains("(disabled in config)"));
    }

    @Test
    void testDomainHealth_NoAgents() {
        // Given
        when(domainManager.getDomainHealth("test-domain")).thenReturn("NO_AGENTS");

        // When
        String result = commands.domainHealth("test-domain");

        // Then
        assertTrue(result.contains("Health: NO_AGENTS"));
        assertTrue(result.contains("⚠"));
    }

    /** Helper method to create a test DomainConfig. */
    private DomainConfig createTestDomainConfig(final String name) {
        return DomainConfig.builder()
                .name(name)
                .version("1.0.0")
                .description("Test domain")
                .outputFormats(List.of("technical", "business"))
                .agentDirectory("agents/")
                .enabled(true)
                .build();
    }
}
