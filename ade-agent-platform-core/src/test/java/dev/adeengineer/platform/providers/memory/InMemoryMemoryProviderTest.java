package dev.adeengineer.platform.providers.memory;

import dev.adeengineer.embeddings.EmbeddingsProvider;
import dev.adeengineer.embeddings.model.Embedding;
import dev.adeengineer.memory.model.MemoryEntry;
import dev.adeengineer.memory.model.VectorSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InMemoryMemoryProvider.
 */
@ExtendWith(MockitoExtension.class)
class InMemoryMemoryProviderTest {

    @Mock
    private EmbeddingsProvider embeddingsProvider;

    private InMemoryMemoryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new InMemoryMemoryProvider(embeddingsProvider);
    }

    @Test
    void shouldStoreMemoryEntry() {
        // Given
        MemoryEntry entry = new MemoryEntry(
                null,
                "Test memory content",
                Map.of("type", "test"),
                Instant.now(),
                0.8
        );

        // Mock embedding response
        when(embeddingsProvider.embed(anyString())).thenReturn(
                Mono.just(new Embedding(
                        List.of(0.1f, 0.2f, 0.3f),
                        "test-model",
                        3
                ))
        );

        // When & Then
        StepVerifier.create(provider.store(entry))
                .assertNext(stored -> {
                    assertNotNull(stored.id());
                    assertEquals("Test memory content", stored.content());
                    assertEquals(0.8, stored.importance());
                    assertEquals("test", stored.metadata().get("type"));
                    assertNotNull(stored.timestamp());
                })
                .verifyComplete();
    }

    @Test
    void shouldRetrieveMemoryById() {
        // Given
        MemoryEntry entry = new MemoryEntry(
                "test-id",
                "Test content",
                Map.of(),
                Instant.now(),
                0.5
        );

        when(embeddingsProvider.embed(anyString())).thenReturn(
                Mono.just(new Embedding(List.of(0.1f), "test", 1))
        );

        // Store first
        provider.store(entry).block();

        // When & Then
        StepVerifier.create(provider.retrieve("test-id"))
                .assertNext(retrieved -> {
                    assertEquals("test-id", retrieved.id());
                    assertEquals("Test content", retrieved.content());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        // When & Then
        StepVerifier.create(provider.retrieve("non-existent"))
                .verifyComplete();
    }

    @Test
    void shouldDeleteMemoryEntry() {
        // Given
        MemoryEntry entry = new MemoryEntry(
                "delete-test",
                "Content to delete",
                Map.of(),
                Instant.now(),
                0.5
        );

        when(embeddingsProvider.embed(anyString())).thenReturn(
                Mono.just(new Embedding(List.of(0.1f), "test", 1))
        );

        provider.store(entry).block();

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
    void shouldSearchByVectorSimilarity() {
        // Given
        when(embeddingsProvider.embed("document 1")).thenReturn(
                Mono.just(new Embedding(List.of(1.0f, 0.0f), "test", 2))
        );
        when(embeddingsProvider.embed("document 2")).thenReturn(
                Mono.just(new Embedding(List.of(0.8f, 0.2f), "test", 2))
        );
        when(embeddingsProvider.embed("query")).thenReturn(
                Mono.just(new Embedding(List.of(0.9f, 0.1f), "test", 2))
        );

        MemoryEntry entry1 = new MemoryEntry("1", "document 1", Map.of(), Instant.now(), 0.5);
        MemoryEntry entry2 = new MemoryEntry("2", "document 2", Map.of(), Instant.now(), 0.5);

        provider.store(entry1).block();
        provider.store(entry2).block();

        // Wait for async embedding to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        StepVerifier.create(provider.search("query", 2, Map.of()))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldFilterByMetadata() {
        // Given
        when(embeddingsProvider.embed(anyString())).thenReturn(
                Mono.just(new Embedding(List.of(0.5f, 0.5f), "test", 2))
        );

        MemoryEntry entry1 = new MemoryEntry(
                "1", "content 1", Map.of("category", "A"), Instant.now(), 0.5
        );
        MemoryEntry entry2 = new MemoryEntry(
                "2", "content 2", Map.of("category", "B"), Instant.now(), 0.5
        );

        provider.store(entry1).block();
        provider.store(entry2).block();

        // Wait for async embedding
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then - filter for category A
        StepVerifier.create(provider.search("query", 10, Map.of("category", "A")))
                .assertNext(result -> assertEquals("1", result.entry().id()))
                .verifyComplete();
    }

    @Test
    void shouldGetConversationHistory() {
        // Given
        when(embeddingsProvider.embed(anyString())).thenReturn(
                Mono.just(new Embedding(List.of(0.1f), "test", 1))
        );

        MemoryEntry msg1 = new MemoryEntry(
                "msg1", "Hello", Map.of("conversation_id", "conv1"), Instant.now(), 0.5
        );
        MemoryEntry msg2 = new MemoryEntry(
                "msg2", "Hi there", Map.of("conversation_id", "conv1"), Instant.now().plusMillis(1), 0.5
        );
        MemoryEntry msg3 = new MemoryEntry(
                "msg3", "Different conversation", Map.of("conversation_id", "conv2"), Instant.now(), 0.5
        );

        provider.store(msg1).block();
        provider.store(msg2).block();
        provider.store(msg3).block();

        // When & Then
        StepVerifier.create(provider.getConversationHistory("conv1", 10))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldLimitConversationHistory() {
        // Given
        when(embeddingsProvider.embed(anyString())).thenReturn(
                Mono.just(new Embedding(List.of(0.1f), "test", 1))
        );

        for (int i = 0; i < 5; i++) {
            MemoryEntry msg = new MemoryEntry(
                    "msg" + i,
                    "Message " + i,
                    Map.of("conversation_id", "conv1"),
                    Instant.now().plusMillis(i),
                    0.5
            );
            provider.store(msg).block();
        }

        // When & Then
        StepVerifier.create(provider.getConversationHistory("conv1", 3))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void shouldClearAllMemories() {
        // Given
        when(embeddingsProvider.embed(anyString())).thenReturn(
                Mono.just(new Embedding(List.of(0.1f), "test", 1))
        );

        for (int i = 0; i < 3; i++) {
            MemoryEntry entry = new MemoryEntry(
                    "id" + i, "content " + i, Map.of(), Instant.now(), 0.5
            );
            provider.store(entry).block();
        }

        // When
        Long count = provider.clear().block();

        // Then
        assertEquals(3, count);

        // Verify all are gone
        StepVerifier.create(provider.retrieve("id0"))
                .verifyComplete();
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("in-memory", provider.getProviderName());
    }

    @Test
    void shouldBeHealthyWhenEmbeddingsProviderIsHealthy() {
        when(embeddingsProvider.isHealthy()).thenReturn(true);
        assertTrue(provider.isHealthy());
    }

    @Test
    void shouldBeUnhealthyWhenEmbeddingsProviderIsUnhealthy() {
        when(embeddingsProvider.isHealthy()).thenReturn(false);
        assertFalse(provider.isHealthy());
    }
}
