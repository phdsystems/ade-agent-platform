package adeengineer.dev.platform.api;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import adeengineer.dev.llm.LLMProvider;
import adeengineer.dev.llm.LLMProviderFactory;
import adeengineer.dev.platform.core.DomainManager;
import adeengineer.dev.platform.core.DomainManager.DomainMetadata;
import adeengineer.dev.platform.model.DomainConfig;

/** Unit tests for DomainController. */
class DomainControllerTest {

    private DomainManager domainManager;
    private LLMProviderFactory llmProviderFactory;
    private DomainController controller;
    private LLMProvider mockProvider;

    @BeforeEach
    void setUp() {
        domainManager = mock(DomainManager.class);
        llmProviderFactory = mock(LLMProviderFactory.class);
        controller = new DomainController(domainManager, llmProviderFactory);
        mockProvider = mock(LLMProvider.class);

        when(llmProviderFactory.getProviderWithFailover()).thenReturn(mockProvider);
    }

    @Test
    void testListDomains() {
        // Given
        when(domainManager.getLoadedDomains()).thenReturn(Set.of("domain1", "domain2"));
        when(domainManager.getTotalAgentCount()).thenReturn(10);

        // When
        ResponseEntity<Map<String, Object>> response = controller.listDomains();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.get("count"));
        assertEquals(10, body.get("totalAgents"));
        assertTrue(body.get("domains") instanceof Set);
    }

    @Test
    void testListDomains_Empty() {
        // Given
        when(domainManager.getLoadedDomains()).thenReturn(Set.of());
        when(domainManager.getTotalAgentCount()).thenReturn(0);

        // When
        ResponseEntity<Map<String, Object>> response = controller.listDomains();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.get("count"));
    }

    @Test
    void testGetAllDomainsInfo() {
        // Given
        DomainConfig config = createTestDomainConfig("test-domain");
        DomainMetadata metadata =
                new DomainMetadata(
                        config, "domains/test-domain", 5, System.currentTimeMillis(), true);

        when(domainManager.getAllDomainMetadata()).thenReturn(Map.of("test-domain", metadata));
        when(domainManager.getDomainHealth("test-domain")).thenReturn("HEALTHY");

        // When
        ResponseEntity<Map<String, DomainController.DomainInfo>> response =
                controller.getAllDomainsInfo();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, DomainController.DomainInfo> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertTrue(body.containsKey("test-domain"));

        DomainController.DomainInfo info = body.get("test-domain");
        assertEquals("test-domain", info.name());
        assertEquals("1.0.0", info.version());
        assertEquals(5, info.agentCount());
        assertEquals("HEALTHY", info.health());
    }

    @Test
    void testGetDomainInfo() {
        // Given
        DomainConfig config = createTestDomainConfig("test-domain");
        DomainMetadata metadata =
                new DomainMetadata(
                        config, "domains/test-domain", 3, System.currentTimeMillis(), true);

        when(domainManager.getDomainMetadata("test-domain")).thenReturn(metadata);
        when(domainManager.getDomainHealth("test-domain")).thenReturn("HEALTHY");

        // When
        ResponseEntity<DomainController.DomainInfo> response =
                controller.getDomainInfo("test-domain");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DomainController.DomainInfo info = response.getBody();
        assertNotNull(info);
        assertEquals("test-domain", info.name());
        assertEquals("1.0.0", info.version());
        assertEquals(3, info.agentCount());
        assertTrue(info.enabled());
    }

    @Test
    void testGetDomainInfo_NotFound() {
        // Given
        when(domainManager.getDomainMetadata("nonexistent")).thenReturn(null);

        // When
        ResponseEntity<DomainController.DomainInfo> response =
                controller.getDomainInfo("nonexistent");

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testLoadDomain() throws IOException {
        // Given
        String domainPath = "domains/test-domain";
        when(domainManager.loadDomain(eq(domainPath), eq(mockProvider))).thenReturn(5);

        DomainController.LoadDomainRequest request =
                new DomainController.LoadDomainRequest(domainPath);

        // When
        ResponseEntity<Map<String, Object>> response = controller.loadDomain(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(domainPath, body.get("domainPath"));
        assertEquals(5, body.get("agentsLoaded"));
        verify(domainManager).loadDomain(domainPath, mockProvider);
    }

    @Test
    void testLoadDomain_Failure() throws IOException {
        // Given
        String domainPath = "domains/bad-domain";
        when(domainManager.loadDomain(eq(domainPath), any()))
                .thenThrow(new IOException("Invalid domain configuration"));

        DomainController.LoadDomainRequest request =
                new DomainController.LoadDomainRequest(domainPath);

        // When
        ResponseEntity<Map<String, Object>> response = controller.loadDomain(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertTrue(body.get("error").toString().contains("Invalid domain configuration"));
    }

    @Test
    void testReloadDomain() throws IOException {
        // Given
        String domainName = "test-domain";
        when(domainManager.reloadDomain(eq(domainName), eq(mockProvider))).thenReturn(5);

        // When
        ResponseEntity<Map<String, Object>> response = controller.reloadDomain(domainName);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(domainName, body.get("domainName"));
        assertEquals(5, body.get("agentsLoaded"));
    }

    @Test
    void testReloadDomain_NotFound() throws IOException {
        // Given
        String domainName = "nonexistent";
        when(domainManager.reloadDomain(eq(domainName), any()))
                .thenThrow(new IllegalArgumentException("Domain not found"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.reloadDomain(domainName);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testReloadDomain_IOFailure() throws IOException {
        // Given
        String domainName = "test-domain";
        when(domainManager.reloadDomain(eq(domainName), any()))
                .thenThrow(new IOException("Failed to read domain.yaml"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.reloadDomain(domainName);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
    }

    @Test
    void testUnloadDomain() {
        // Given
        String domainName = "test-domain";
        when(domainManager.unloadDomain(domainName)).thenReturn(5);

        // When
        ResponseEntity<Map<String, Object>> response = controller.unloadDomain(domainName);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(domainName, body.get("domainName"));
        assertEquals(5, body.get("agentsRemoved"));
    }

    @Test
    void testUnloadDomain_NotFound() {
        // Given
        String domainName = "nonexistent";
        when(domainManager.unloadDomain(domainName))
                .thenThrow(new IllegalArgumentException("Domain not found"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.unloadDomain(domainName);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDiscoverDomains() throws IOException {
        // Given
        List<String> discoveredPaths = List.of("domains/domain1", "domains/domain2");
        when(domainManager.discoverDomains("domains")).thenReturn(discoveredPaths);

        // When
        ResponseEntity<Map<String, Object>> response = controller.discoverDomains();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.get("count"));
        assertTrue(body.get("domains") instanceof List);
    }

    @Test
    void testDiscoverDomains_Failure() throws IOException {
        // Given
        when(domainManager.discoverDomains("domains"))
                .thenThrow(new IOException("Directory not readable"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.discoverDomains();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("error"));
    }

    @Test
    void testGetDomainHealth() {
        // Given
        when(domainManager.getDomainHealth("test-domain")).thenReturn("HEALTHY");

        // When
        ResponseEntity<Map<String, String>> response = controller.getDomainHealth("test-domain");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("test-domain", body.get("domainName"));
        assertEquals("HEALTHY", body.get("health"));
    }

    @Test
    void testGetDomainHealth_NotLoaded() {
        // Given
        when(domainManager.getDomainHealth("nonexistent")).thenReturn("NOT_LOADED");

        // When
        ResponseEntity<Map<String, String>> response = controller.getDomainHealth("nonexistent");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("NOT_LOADED", body.get("health"));
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
