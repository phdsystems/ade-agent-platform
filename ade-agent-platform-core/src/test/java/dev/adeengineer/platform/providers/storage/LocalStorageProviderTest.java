package dev.adeengineer.platform.providers.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.adeengineer.storage.model.Document;
import dev.adeengineer.storage.model.StorageQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalStorageProvider.
 */
class LocalStorageProviderTest {

    @TempDir
    Path tempDir;

    private LocalStorageProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        provider = new LocalStorageProvider(tempDir.toString(), objectMapper);
    }

    @AfterEach
    void tearDown() {
        // Cleanup is handled by @TempDir
    }

    @Test
    void shouldStoreDocument() {
        // Given
        byte[] content = "Test document content".getBytes();
        // Use a generated ID since SDK Document validation requires non-null ID
        String generatedId = java.util.UUID.randomUUID().toString();
        Document doc = new Document(
                generatedId,
                content,
                "text/plain",
                Map.of("author", "test"),
                Instant.now(),
                content.length
        );

        // When & Then
        StepVerifier.create(provider.store(doc))
                .assertNext(stored -> {
                    assertEquals(generatedId, stored.id());
                    assertArrayEquals(content, stored.content());
                    assertEquals("text/plain", stored.contentType());
                    assertEquals("test", stored.metadata().get("author"));
                    assertNotNull(stored.createdAt());
                    assertEquals(content.length, stored.size());
                })
                .verifyComplete();
    }

    @Test
    void shouldStoreDocumentWithProvidedId() {
        // Given
        byte[] content = "Content with ID".getBytes();
        Document doc = new Document(
                "my-doc-id",
                content,
                "text/plain",
                Map.of(),
                Instant.now(),
                content.length
        );

        // When
        Document stored = provider.store(doc).block();

        // Then
        assertNotNull(stored);
        assertEquals("my-doc-id", stored.id());
    }

    @Test
    void shouldRetrieveDocument() {
        // Given
        byte[] content = "Retrieve test content".getBytes();
        Document doc = new Document(
                "retrieve-test",
                content,
                "application/json",
                Map.of("version", "1.0"),
                Instant.now(),
                content.length
        );

        provider.store(doc).block();

        // When & Then
        StepVerifier.create(provider.retrieve("retrieve-test"))
                .assertNext(retrieved -> {
                    assertEquals("retrieve-test", retrieved.id());
                    assertArrayEquals(content, retrieved.content());
                    assertEquals("application/json", retrieved.contentType());
                    assertEquals("1.0", retrieved.metadata().get("version"));
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNonExistentDocument() {
        // When & Then
        StepVerifier.create(provider.retrieve("non-existent"))
                .verifyComplete();
    }

    @Test
    void shouldDeleteDocument() {
        // Given
        byte[] content = "Delete me".getBytes();
        Document doc = new Document(
                "delete-test",
                content,
                "text/plain",
                Map.of(),
                Instant.now(),
                content.length
        );

        provider.store(doc).block();

        // When
        Boolean deleted = provider.delete("delete-test").block();

        // Then
        assertTrue(deleted);

        // Verify it's gone
        StepVerifier.create(provider.retrieve("delete-test"))
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistent() {
        // When & Then
        StepVerifier.create(provider.delete("non-existent"))
                .assertNext(result -> assertFalse(result))
                .verifyComplete();
    }

    @Test
    void shouldCheckDocumentExists() {
        // Given
        byte[] content = "Exists test".getBytes();
        Document doc = new Document(
                "exists-test",
                content,
                "text/plain",
                Map.of(),
                Instant.now(),
                content.length
        );

        provider.store(doc).block();

        // When & Then
        StepVerifier.create(provider.exists("exists-test"))
                .assertNext(exists -> assertTrue(exists))
                .verifyComplete();

        StepVerifier.create(provider.exists("non-existent"))
                .assertNext(exists -> assertFalse(exists))
                .verifyComplete();
    }

    @Test
    void shouldQueryDocumentsWithFilters() {
        // Given
        byte[] content1 = "Doc 1".getBytes();
        byte[] content2 = "Doc 2".getBytes();
        byte[] content3 = "Doc 3".getBytes();

        Document doc1 = new Document("1", content1, "text/plain",
                Map.of("category", "A"), Instant.now(), content1.length);
        Document doc2 = new Document("2", content2, "text/plain",
                Map.of("category", "B"), Instant.now(), content2.length);
        Document doc3 = new Document("3", content3, "text/plain",
                Map.of("category", "A"), Instant.now(), content3.length);

        provider.store(doc1).block();
        provider.store(doc2).block();
        provider.store(doc3).block();

        // When & Then - filter for category A
        StorageQuery query = new StorageQuery(Map.of("category", "A"), 10, 0, null, true);

        StepVerifier.create(provider.query(query))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldQueryWithLimit() {
        // Given
        for (int i = 0; i < 5; i++) {
            byte[] content = ("Content " + i).getBytes();
            Document doc = new Document(
                    "doc" + i,
                    content,
                    "text/plain",
                    Map.of(),
                    Instant.now(),
                    content.length
            );
            provider.store(doc).block();
        }

        // When & Then
        StorageQuery query = new StorageQuery(Map.of(), 3, 0, null, true);

        StepVerifier.create(provider.query(query))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void shouldQueryWithOffset() {
        // Given
        for (int i = 0; i < 5; i++) {
            byte[] content = ("Content " + i).getBytes();
            Document doc = new Document(
                    "doc" + i,
                    content,
                    "text/plain",
                    Map.of("index", i),
                    Instant.now(),
                    content.length
            );
            provider.store(doc).block();
        }

        // When & Then - skip first 2 documents
        StorageQuery query = new StorageQuery(Map.of(), 10, 2, null, true);

        StepVerifier.create(provider.query(query))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void shouldCalculateTotalSize() {
        // Given
        byte[] content1 = new byte[100];
        byte[] content2 = new byte[200];
        byte[] content3 = new byte[300];

        Document doc1 = new Document("1", content1, "application/octet-stream",
                Map.of(), Instant.now(), content1.length);
        Document doc2 = new Document("2", content2, "application/octet-stream",
                Map.of(), Instant.now(), content2.length);
        Document doc3 = new Document("3", content3, "application/octet-stream",
                Map.of(), Instant.now(), content3.length);

        provider.store(doc1).block();
        provider.store(doc2).block();
        provider.store(doc3).block();

        // When & Then
        StepVerifier.create(provider.getTotalSize())
                .assertNext(size -> assertEquals(600L, size))
                .verifyComplete();
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("local", provider.getProviderName());
    }

    @Test
    void shouldBeHealthyWhenStorageIsAccessible() {
        assertTrue(provider.isHealthy());
    }

    @Test
    void shouldPersistMetadataToJson() throws IOException {
        // Given
        byte[] content = "Metadata test".getBytes();
        Document doc = new Document(
                "metadata-test",
                content,
                "text/plain",
                Map.of("key", "value"),
                Instant.now(),
                content.length
        );

        provider.store(doc).block();

        // Then - verify metadata file exists
        Path metadataPath = tempDir.resolve("metadata-test.metadata.json");
        assertTrue(Files.exists(metadataPath));

        // Verify metadata can be read back
        Document metadata = objectMapper.readValue(metadataPath.toFile(), Document.class);
        assertEquals("metadata-test", metadata.id());
        assertEquals("value", metadata.metadata().get("key"));
    }

    @Test
    void shouldLoadExistingMetadataOnInitialization() throws IOException {
        // Given - create provider and store documents
        byte[] content1 = "Doc 1".getBytes();
        Document doc1 = new Document(
                "init-test-1",
                content1,
                "text/plain",
                Map.of(),
                Instant.now(),
                content1.length
        );
        provider.store(doc1).block();

        // When - create new provider instance (simulates restart)
        LocalStorageProvider newProvider = new LocalStorageProvider(tempDir.toString(), objectMapper);

        // Then - should be able to retrieve the document
        StepVerifier.create(newProvider.exists("init-test-1"))
                .assertNext(exists -> assertTrue(exists))
                .verifyComplete();
    }

    @Test
    void shouldHandleBinaryContent() {
        // Given
        byte[] binaryContent = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
        Document doc = new Document(
                "binary-test",
                binaryContent,
                "application/octet-stream",
                Map.of(),
                Instant.now(),
                binaryContent.length
        );

        // When
        provider.store(doc).block();
        Document retrieved = provider.retrieve("binary-test").block();

        // Then
        assertNotNull(retrieved);
        assertArrayEquals(binaryContent, retrieved.content());
    }

    @Test
    void shouldHandleLargeDocuments() {
        // Given
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        Document doc = new Document(
                "large-test",
                largeContent,
                "application/octet-stream",
                Map.of(),
                Instant.now(),
                largeContent.length
        );

        // When
        Document stored = provider.store(doc).block();

        // Then
        assertNotNull(stored);
        assertEquals(1024 * 1024, stored.size());

        Document retrieved = provider.retrieve("large-test").block();
        assertNotNull(retrieved);
        assertEquals(1024 * 1024, retrieved.content().length);
    }
}
