package dev.adeengineer.platform.providers.memory;

import dev.adeengineer.embeddings.EmbeddingsProvider;
import dev.adeengineer.embeddings.model.Embedding;
import dev.adeengineer.memory.MemoryProvider;
import dev.adeengineer.memory.model.MemoryEntry;
import dev.adeengineer.memory.model.VectorSearchResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of MemoryProvider.
 * Suitable for development, testing, and small-scale deployments.
 *
 * <p>Features:
 * - Vector similarity search using cosine similarity
 * - Conversation history tracking via metadata
 * - Metadata filtering
 * - Thread-safe concurrent operations
 *
 * <p>Limitations:
 * - Not persistent (data lost on restart)
 * - Limited scalability (single-node only)
 * - Memory bounded by JVM heap size
 *
 * <p><b>Framework-agnostic:</b> This is a pure POJO with no Spring dependencies.
 * Can be used in any Java application.
 */
@Slf4j
public final class InMemoryMemoryProvider implements MemoryProvider {

    /** In-memory storage for all memory entries. */
    private final Map<String, MemoryEntry> memoryStore = new ConcurrentHashMap<>();

    /** Separate storage for embedding vectors (indexed by memory ID). */
    private final Map<String, List<Float>> vectorStore = new ConcurrentHashMap<>();

    /** Embeddings provider for vector similarity search. */
    private final EmbeddingsProvider embeddingsProvider;

    /**
     * Creates an in-memory memory provider.
     *
     * @param embeddings Embeddings provider for vector search
     */
    public InMemoryMemoryProvider(final EmbeddingsProvider embeddings) {
        this.embeddingsProvider = embeddings;
        log.info("Initialized InMemoryMemoryProvider");
    }

    /**
     * Store a memory entry.
     *
     * @param entry The memory entry to store
     * @return Mono emitting the stored entry with generated ID if needed
     */
    @Override
    public Mono<MemoryEntry> store(final MemoryEntry entry) {
        return Mono.fromCallable(() -> {
            // Generate ID if not provided
            final String id = entry.id() != null ? entry.id() : UUID.randomUUID().toString();

            // Create entry with ID and current timestamp
            final MemoryEntry storedEntry = new MemoryEntry(
                    id,
                    entry.content(),
                    entry.metadata(),
                    Instant.now(),
                    entry.importance()
            );

            // Store memory entry
            memoryStore.put(id, storedEntry);

            // Generate and store embedding asynchronously
            if (entry.content() != null && !entry.content().isBlank()) {
                embeddingsProvider.embed(entry.content())
                        .subscribe(embedding -> {
                            vectorStore.put(id, embedding.vector());
                            log.debug("Stored memory entry with embedding: {}", id);
                        });
            } else {
                log.debug("Stored memory entry without embedding: {}", id);
            }

            return storedEntry;
        });
    }

    /**
     * Retrieve a memory entry by ID.
     *
     * @param id The unique identifier
     * @return Mono emitting the memory entry, or empty if not found
     */
    @Override
    public Mono<MemoryEntry> retrieve(final String id) {
        final MemoryEntry entry = memoryStore.get(id);
        return entry != null ? Mono.just(entry) : Mono.empty();
    }

    /**
     * Search for similar memories using vector similarity.
     *
     * @param query The query text
     * @param topK Number of results to return
     * @param filters Optional metadata filters
     * @return Flux of search results ordered by relevance
     */
    @Override
    public Flux<VectorSearchResult> search(
            final String query,
            final int topK,
            final Map<String, Object> filters) {
        return embeddingsProvider.embed(query)
                .flatMapMany(queryEmbedding -> {
                    // Filter entries by metadata if filters provided
                    List<String> filteredIds = memoryStore.entrySet().stream()
                            .filter(e -> matchesFilters(e.getValue(), filters))
                            .map(Map.Entry::getKey)
                            .filter(vectorStore::containsKey)
                            .toList();

                    // Calculate cosine similarity for each entry
                    List<VectorSearchResult> results = filteredIds.stream()
                            .map(id -> {
                                MemoryEntry entry = memoryStore.get(id);
                                List<Float> vector = vectorStore.get(id);

                                double similarity = cosineSimilarity(
                                        queryEmbedding.vector(),
                                        vector
                                );

                                double distance = 1.0 - similarity;  // Convert similarity to distance

                                return new VectorSearchResult(entry, similarity, distance);
                            })
                            .sorted(Comparator.comparingDouble(VectorSearchResult::score).reversed())
                            .limit(topK)
                            .collect(Collectors.toList());

                    return Flux.fromIterable(results);
                });
    }

    /**
     * Delete a memory entry.
     *
     * @param id The unique identifier
     * @return Mono emitting true if deleted, false if not found
     */
    @Override
    public Mono<Boolean> delete(final String id) {
        boolean removed = memoryStore.remove(id) != null;
        if (removed) {
            vectorStore.remove(id);
            log.debug("Deleted memory entry: {}", id);
        }
        return Mono.just(removed);
    }

    /**
     * Get conversation history.
     *
     * @param conversationId The conversation identifier
     * @param limit Maximum number of entries to return
     * @return Flux of memory entries in chronological order
     */
    @Override
    public Flux<MemoryEntry> getConversationHistory(final String conversationId, final int limit) {
        List<MemoryEntry> history = memoryStore.values().stream()
                .filter(entry -> entry.metadata() != null &&
                        conversationId.equals(entry.metadata().get("conversation_id")))
                .sorted(Comparator.comparing(MemoryEntry::timestamp))
                .limit(limit)
                .collect(Collectors.toList());

        return Flux.fromIterable(history);
    }

    /**
     * Clear all memories (use with caution).
     *
     * @return Mono emitting the number of entries deleted
     */
    @Override
    public Mono<Long> clear() {
        long count = memoryStore.size();
        memoryStore.clear();
        vectorStore.clear();
        log.warn("Cleared all memory entries: {} deleted", count);
        return Mono.just(count);
    }

    /**
     * Get the provider name.
     *
     * @return Provider name
     */
    @Override
    public String getProviderName() {
        return "in-memory";
    }

    /**
     * Check if the provider is healthy and accessible.
     *
     * @return true if the provider is ready to use
     */
    @Override
    public boolean isHealthy() {
        return embeddingsProvider != null && embeddingsProvider.isHealthy();
    }

    /**
     * Calculate cosine similarity between two vectors.
     *
     * @param vec1 First vector
     * @param vec2 Second vector
     * @return Cosine similarity score (0.0 to 1.0)
     */
    private double cosineSimilarity(final List<Float> vec1, final List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have same dimensions");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * Check if memory entry matches all provided filters.
     *
     * @param entry The memory entry
     * @param filters Metadata filters to apply
     * @return true if entry matches all filters
     */
    private boolean matchesFilters(final MemoryEntry entry, final Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        if (entry.metadata() == null) {
            return false;
        }

        return filters.entrySet().stream()
                .allMatch(filter -> {
                    Object entryValue = entry.metadata().get(filter.getKey());
                    return entryValue != null && entryValue.equals(filter.getValue());
                });
    }
}
