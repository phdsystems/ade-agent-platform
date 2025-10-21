package dev.adeengineer.platform.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.core.DomainManager.DomainMetadata;

/** Unit tests for DomainManager. */
class DomainManagerTest {

    @TempDir Path tempDir;

    private DomainLoader domainLoader;
    private AgentRegistry agentRegistry;
    private DomainManager domainManager;
    private LLMProvider mockProvider;

    @BeforeEach
    void setUp() {
        domainLoader = mock(DomainLoader.class);
        agentRegistry = mock(AgentRegistry.class);
        domainManager = new DomainManager(domainLoader, agentRegistry);
        mockProvider = mock(LLMProvider.class);
    }

    @Test
    void testLoadDomain() throws IOException {
        // Given
        String domainPath = createTestDomain("test-domain");
        when(domainLoader.loadDomain(eq(domainPath), eq(mockProvider))).thenReturn(5);

        // When
        int agentCount = domainManager.loadDomain(domainPath, mockProvider);

        // Then
        assertEquals(5, agentCount);
        verify(domainLoader).loadDomain(domainPath, mockProvider);
        assertTrue(domainManager.isDomainLoaded("test-domain"));
        assertEquals(1, domainManager.getDomainCount());
        assertEquals(5, domainManager.getTotalAgentCount());
    }

    @Test
    void testLoadMultipleDomains() throws IOException {
        // Given
        String domain1 = createTestDomain("domain1");
        String domain2 = createTestDomain("domain2");
        when(domainLoader.loadDomain(eq(domain1), any())).thenReturn(3);
        when(domainLoader.loadDomain(eq(domain2), any())).thenReturn(4);

        // When
        domainManager.loadDomain(domain1, mockProvider);
        domainManager.loadDomain(domain2, mockProvider);

        // Then
        assertEquals(2, domainManager.getDomainCount());
        assertEquals(7, domainManager.getTotalAgentCount());
        assertTrue(domainManager.isDomainLoaded("domain1"));
        assertTrue(domainManager.isDomainLoaded("domain2"));
    }

    @Test
    void testGetLoadedDomains() throws IOException {
        // Given
        String domain1 = createTestDomain("domain1");
        String domain2 = createTestDomain("domain2");
        when(domainLoader.loadDomain(any(), any())).thenReturn(1);

        // When
        domainManager.loadDomain(domain1, mockProvider);
        domainManager.loadDomain(domain2, mockProvider);
        Set<String> loaded = domainManager.getLoadedDomains();

        // Then
        assertEquals(2, loaded.size());
        assertTrue(loaded.contains("domain1"));
        assertTrue(loaded.contains("domain2"));
    }

    @Test
    void testGetDomainMetadata() throws IOException {
        // Given
        String domainPath = createTestDomain("test-domain");
        when(domainLoader.loadDomain(any(), any())).thenReturn(3);

        // When
        domainManager.loadDomain(domainPath, mockProvider);
        DomainMetadata metadata = domainManager.getDomainMetadata("test-domain");

        // Then
        assertNotNull(metadata);
        assertEquals("test-domain", metadata.config().getName());
        assertEquals("1.0.0", metadata.config().getVersion());
        assertEquals(3, metadata.agentCount());
        assertTrue(metadata.enabled());
        assertEquals(domainPath, metadata.path());
        assertTrue(metadata.loadedTimestamp() > 0);
    }

    @Test
    void testGetDomainMetadata_NotFound() {
        // When
        DomainMetadata metadata = domainManager.getDomainMetadata("nonexistent");

        // Then
        assertNull(metadata);
    }

    @Test
    void testGetAllDomainMetadata() throws IOException {
        // Given
        String domain1 = createTestDomain("domain1");
        String domain2 = createTestDomain("domain2");
        when(domainLoader.loadDomain(any(), any())).thenReturn(1);

        // When
        domainManager.loadDomain(domain1, mockProvider);
        domainManager.loadDomain(domain2, mockProvider);
        Map<String, DomainMetadata> allMetadata = domainManager.getAllDomainMetadata();

        // Then
        assertEquals(2, allMetadata.size());
        assertTrue(allMetadata.containsKey("domain1"));
        assertTrue(allMetadata.containsKey("domain2"));
    }

    @Test
    void testUnloadDomain() throws IOException {
        // Given
        String domainPath = createTestDomain("test-domain");
        when(domainLoader.loadDomain(any(), any())).thenReturn(5);
        domainManager.loadDomain(domainPath, mockProvider);

        // When
        int removed = domainManager.unloadDomain("test-domain");

        // Then
        assertFalse(domainManager.isDomainLoaded("test-domain"));
        assertEquals(0, domainManager.getDomainCount());
    }

    @Test
    void testUnloadDomain_NotFound() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class, () -> domainManager.unloadDomain("nonexistent"));
    }

    @Test
    void testReloadDomain() throws IOException {
        // Given
        String domainPath = createTestDomain("test-domain");
        when(domainLoader.loadDomain(eq(domainPath), any()))
                .thenReturn(3)
                .thenReturn(4); // Different count after reload

        domainManager.loadDomain(domainPath, mockProvider);

        // When
        int newCount = domainManager.reloadDomain("test-domain", mockProvider);

        // Then
        assertEquals(4, newCount);
        assertTrue(domainManager.isDomainLoaded("test-domain"));
        verify(domainLoader, times(2)).loadDomain(domainPath, mockProvider);
    }

    @Test
    void testReloadDomain_NotFound() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> domainManager.reloadDomain("nonexistent", mockProvider));
    }

    @Test
    void testDiscoverDomains() throws IOException {
        // Given
        Path domainsDir = tempDir.resolve("domains");
        Files.createDirectories(domainsDir);

        createTestDomain("domain1", domainsDir);
        createTestDomain("domain2", domainsDir);

        // Create a directory without domain.yaml (should be ignored)
        Files.createDirectories(domainsDir.resolve("not-a-domain"));

        // When
        List<String> discovered = domainManager.discoverDomains(domainsDir.toString());

        // Then
        assertEquals(2, discovered.size());
        assertTrue(discovered.stream().anyMatch(path -> path.contains("domain1")));
        assertTrue(discovered.stream().anyMatch(path -> path.contains("domain2")));
    }

    @Test
    void testDiscoverDomains_NoDomainsDirectory() throws IOException {
        // When
        List<String> discovered =
                domainManager.discoverDomains(tempDir.resolve("nonexistent").toString());

        // Then
        assertTrue(discovered.isEmpty());
    }

    @Test
    void testGetDomainHealth_Healthy() throws IOException {
        // Given
        String domainPath = createTestDomain("test-domain");
        when(domainLoader.loadDomain(any(), any())).thenReturn(3);
        domainManager.loadDomain(domainPath, mockProvider);

        // When
        String health = domainManager.getDomainHealth("test-domain");

        // Then
        assertEquals("HEALTHY", health);
    }

    @Test
    void testGetDomainHealth_NotLoaded() {
        // When
        String health = domainManager.getDomainHealth("nonexistent");

        // Then
        assertEquals("NOT_LOADED", health);
    }

    @Test
    void testGetDomainHealth_NoAgents() throws IOException {
        // Given
        String domainPath = createTestDomain("empty-domain");
        when(domainLoader.loadDomain(any(), any())).thenReturn(0);
        domainManager.loadDomain(domainPath, mockProvider);

        // When
        String health = domainManager.getDomainHealth("empty-domain");

        // Then
        assertEquals("NO_AGENTS", health);
    }

    @Test
    void testGetDomainHealth_Disabled() throws IOException {
        // Given
        String domainPath = createTestDomain("disabled-domain", false);
        when(domainLoader.loadDomain(any(), any())).thenReturn(3);
        domainManager.loadDomain(domainPath, mockProvider);

        // When
        String health = domainManager.getDomainHealth("disabled-domain");

        // Then
        assertEquals("DISABLED", health);
    }

    @Test
    void testDomainMetadataDisplayInfo() throws IOException {
        // Given
        String domainPath = createTestDomain("test-domain");
        when(domainLoader.loadDomain(any(), any())).thenReturn(5);
        domainManager.loadDomain(domainPath, mockProvider);

        // When
        DomainMetadata metadata = domainManager.getDomainMetadata("test-domain");
        String displayInfo = metadata.displayInfo();

        // Then
        assertTrue(displayInfo.contains("test-domain"));
        assertTrue(displayInfo.contains("1.0.0"));
        assertTrue(displayInfo.contains("5 agents"));
    }

    /**
     * Helper method to create a test domain directory structure.
     *
     * @param domainName Name of the domain
     * @return Path to the domain directory
     */
    private String createTestDomain(final String domainName) throws IOException {
        return createTestDomain(domainName, tempDir);
    }

    /**
     * Helper method to create a test domain directory structure.
     *
     * @param domainName Name of the domain
     * @param parentDir Parent directory for the domain
     * @return Path to the domain directory
     */
    private String createTestDomain(final String domainName, final Path parentDir)
            throws IOException {
        return createTestDomain(domainName, parentDir, true);
    }

    /**
     * Helper method to create a test domain directory structure.
     *
     * @param domainName Name of the domain
     * @param enabled Whether domain is enabled
     * @return Path to the domain directory
     */
    private String createTestDomain(final String domainName, final boolean enabled)
            throws IOException {
        return createTestDomain(domainName, tempDir, enabled);
    }

    /**
     * Helper method to create a test domain directory structure.
     *
     * @param domainName Name of the domain
     * @param parentDir Parent directory for the domain
     * @param enabled Whether domain is enabled
     * @return Path to the domain directory
     */
    private String createTestDomain(
            final String domainName, final Path parentDir, final boolean enabled)
            throws IOException {
        Path domainDir = parentDir.resolve(domainName);
        Files.createDirectories(domainDir);

        // Create domain.yaml
        String domainYaml =
                String.format(
                        """
                name: "%s"
                version: "1.0.0"
                description: "Test domain for %s"
                outputFormats:
                  - "technical"
                  - "business"
                agentDirectory: "agents/"
                enabled: %s
                """,
                        domainName, domainName, enabled);

        Files.writeString(domainDir.resolve("domain.yaml"), domainYaml);

        // Create agents directory
        Files.createDirectories(domainDir.resolve("agents"));

        return domainDir.toString();
    }
}
