package dev.adeengineer.platform.providers.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.adeengineer.storage.StorageProvider;
import dev.adeengineer.storage.model.Document;
import dev.adeengineer.storage.model.StorageQuery;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Local filesystem implementation of StorageProvider. Stores documents and artifacts on local disk
 * with metadata indexing.
 *
 * <p>Features: - Persistent file storage - Metadata-based querying - Automatic directory creation -
 * Size tracking
 *
 * <p>Use cases: - Development and testing - Small-scale deployments - Artifact archival - Document
 * management
 */
@Slf4j
public final class LocalStorageProvider implements StorageProvider {

    /** Base directory for document storage. */
    private final Path storageRoot;

    /** In-memory metadata index for fast queries. */
    private final Map<String, Document> metadataIndex = new ConcurrentHashMap<>();

    /** JSON object mapper for metadata serialization. */
    private final ObjectMapper objectMapper;

    /**
     * Creates a local storage provider.
     *
     * @param storagePath Base directory path for storage
     * @param mapper JSON mapper for metadata
     * @throws IOException if storage directory cannot be created
     */
    public LocalStorageProvider(final String storagePath, final ObjectMapper mapper)
            throws IOException {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath();
        this.objectMapper = mapper;

        // Create storage directory if it doesn't exist
        if (!Files.exists(storageRoot)) {
            Files.createDirectories(storageRoot);
            log.info("Created storage directory: {}", storageRoot);
        }

        // Load existing metadata index
        loadMetadataIndex();
        log.info("Initialized LocalStorageProvider with root: {}", storageRoot);
    }

    /**
     * Store a document.
     *
     * @param document The document to store
     * @return Mono emitting the stored document with generated ID if needed
     */
    @Override
    public Mono<Document> store(final Document document) {
        return Mono.fromCallable(
                () -> {
                    // Generate ID if not provided
                    final String id =
                            document.id() != null ? document.id() : UUID.randomUUID().toString();

                    // Create document path
                    final Path docPath = storageRoot.resolve(id);
                    final Path metadataPath = storageRoot.resolve(id + ".metadata.json");

                    // Write document content
                    Files.write(docPath, document.content());

                    // Update document with actual timestamp and size
                    final Document storedDoc =
                            new Document(
                                    id,
                                    document.content(),
                                    document.contentType(),
                                    document.metadata(),
                                    Instant.now(),
                                    document.content().length);

                    // Write metadata
                    objectMapper.writeValue(metadataPath.toFile(), storedDoc);

                    // Update in-memory index
                    metadataIndex.put(id, storedDoc);

                    log.debug("Stored document: {} ({} bytes)", id, storedDoc.size());
                    return storedDoc;
                });
    }

    /**
     * Retrieve a document by ID.
     *
     * @param id The document identifier
     * @return Mono emitting the document, or empty if not found
     */
    @Override
    public Mono<Document> retrieve(final String id) {
        return Mono.fromCallable(
                () -> {
                    final Document metadata = metadataIndex.get(id);
                    if (metadata == null) {
                        return null;
                    }

                    final Path docPath = storageRoot.resolve(id);
                    if (!Files.exists(docPath)) {
                        log.warn("Document file missing for ID: {}", id);
                        return null;
                    }

                    final byte[] content = Files.readAllBytes(docPath);

                    return new Document(
                            metadata.id(),
                            content,
                            metadata.contentType(),
                            metadata.metadata(),
                            metadata.createdAt(),
                            metadata.size());
                });
    }

    /**
     * Delete a document.
     *
     * @param id The document identifier
     * @return Mono emitting true if deleted, false if not found
     */
    @Override
    public Mono<Boolean> delete(final String id) {
        return Mono.fromCallable(
                () -> {
                    final Path docPath = storageRoot.resolve(id);
                    final Path metadataPath = storageRoot.resolve(id + ".metadata.json");

                    boolean deleted = false;

                    if (Files.exists(docPath)) {
                        Files.delete(docPath);
                        deleted = true;
                    }

                    if (Files.exists(metadataPath)) {
                        Files.delete(metadataPath);
                        deleted = true;
                    }

                    if (deleted) {
                        metadataIndex.remove(id);
                        log.debug("Deleted document: {}", id);
                    }

                    return deleted;
                });
    }

    /**
     * Query documents.
     *
     * @param query The query parameters
     * @return Flux of matching documents
     */
    @Override
    public Flux<Document> query(final StorageQuery query) {
        // Collect results to list first to avoid stream consumption issues
        Stream<Document> stream = metadataIndex.values().stream();

        // Apply filters
        if (query.filters() != null && !query.filters().isEmpty()) {
            stream = stream.filter(doc -> matchesFilters(doc, query.filters()));
        }

        // Apply offset BEFORE limit (skip first N, then take limit)
        if (query.offset() > 0) {
            stream = stream.skip(query.offset());
        }

        // Apply limit
        if (query.limit() > 0) {
            stream = stream.limit(query.limit());
        }

        // Convert to list and return as Flux
        final java.util.List<Document> results = stream.toList();
        return Flux.fromIterable(results);
    }

    /**
     * Check if a document exists.
     *
     * @param id The document identifier
     * @return Mono emitting true if exists, false otherwise
     */
    @Override
    public Mono<Boolean> exists(final String id) {
        return Mono.just(metadataIndex.containsKey(id));
    }

    /**
     * Get total storage size used.
     *
     * @return Mono emitting total bytes used
     */
    @Override
    public Mono<Long> getTotalSize() {
        return Mono.fromCallable(
                () -> metadataIndex.values().stream().mapToLong(Document::size).sum());
    }

    /**
     * Get the provider name.
     *
     * @return Provider name
     */
    @Override
    public String getProviderName() {
        return "local";
    }

    /**
     * Check if the provider is healthy and accessible.
     *
     * @return true if the provider is ready to use
     */
    @Override
    public boolean isHealthy() {
        return Files.exists(storageRoot) && Files.isWritable(storageRoot);
    }

    /**
     * Load metadata index from disk.
     *
     * @throws IOException if metadata cannot be read
     */
    private void loadMetadataIndex() throws IOException {
        if (!Files.exists(storageRoot)) {
            return;
        }

        try (Stream<Path> paths = Files.list(storageRoot)) {
            paths.filter(path -> path.toString().endsWith(".metadata.json"))
                    .forEach(
                            metadataPath -> {
                                try {
                                    Document doc =
                                            objectMapper.readValue(
                                                    metadataPath.toFile(), Document.class);
                                    metadataIndex.put(doc.id(), doc);
                                } catch (IOException e) {
                                    log.warn("Failed to load metadata: {}", metadataPath, e);
                                }
                            });
        }

        log.info("Loaded {} documents from metadata index", metadataIndex.size());
    }

    /**
     * Check if document matches all provided filters.
     *
     * @param document The document
     * @param filters Metadata filters to apply
     * @return true if document matches all filters
     */
    private boolean matchesFilters(final Document document, final Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        if (document.metadata() == null) {
            return false;
        }

        return filters.entrySet().stream()
                .allMatch(
                        filter -> {
                            Object docValue = document.metadata().get(filter.getKey());
                            return docValue != null && docValue.equals(filter.getValue());
                        });
    }
}
