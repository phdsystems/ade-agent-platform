package dev.adeengineer.platform.finetuning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.adeengineer.llm.model.LLMResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Fine-tuning service for collecting training data and managing jobs.
 *
 * <p>Supports OpenAI, Anthropic, and open-source model fine-tuning formats.
 */
@Slf4j
public class FineTuningService {

    /** Minimum rating threshold for high-quality interactions. */
    private static final int MIN_QUALITY_RATING = 4;

    /** Estimated average characters per token for token counting. */
    private static final int CHARS_PER_TOKEN = 4;

    /** JSON object mapper for dataset serialization. */
    private final ObjectMapper mapper;

    /** Registry of fine-tuning datasets by name. */
    private final Map<String, FineTuningDataset> datasets = new HashMap<>();

    /**
     * Constructs a new FineTuningService.
     *
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     */
    public FineTuningService(final ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    /**
     * Create a new fine-tuning dataset.
     *
     * @param name Dataset name
     * @param format Dataset format (openai, anthropic, jsonl)
     * @return Dataset ID
     */
    public String createDataset(final String name, final DatasetFormat format) {
        String datasetId = UUID.randomUUID().toString();
        FineTuningDataset dataset =
                new FineTuningDataset(datasetId, name, format, new ArrayList<>(), Instant.now());

        datasets.put(datasetId, dataset);
        log.info("Created fine-tuning dataset: {} ({})", name, datasetId);

        return datasetId;
    }

    /**
     * Add a training example to a dataset.
     *
     * @param datasetId Dataset ID
     * @param prompt User prompt
     * @param completion Expected completion
     * @param metadata Optional metadata
     * @throws IllegalArgumentException if dataset not found
     */
    public void addExample(
            final String datasetId,
            final String prompt,
            final String completion,
            final Map<String, Object> metadata) {
        FineTuningDataset dataset = datasets.get(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found: " + datasetId);
        }

        TrainingExample example = new TrainingExample(prompt, completion, metadata, Instant.now());

        dataset.examples().add(example);
        log.debug(
                "Added example to dataset {}: {} chars",
                datasetId,
                prompt.length() + completion.length());
    }

    /**
     * Add examples from LLM interactions.
     *
     * <p>Only captures interactions with rating >= 4.
     *
     * @param datasetId Dataset ID
     * @param prompt Original prompt
     * @param response LLM response
     * @param rating Quality rating (1-5)
     * @throws IllegalArgumentException if dataset not found
     */
    public void captureInteraction(
            final String datasetId,
            final String prompt,
            final LLMResponse response,
            final int rating) {
        // Only capture high-quality interactions
        if (rating >= MIN_QUALITY_RATING) {
            Map<String, Object> metadata =
                    Map.of(
                            "provider", response.provider(),
                            "model", response.model(),
                            "rating", rating,
                            "tokens", response.usage().totalTokens());

            addExample(datasetId, prompt, response.content(), metadata);
        }
    }

    /**
     * Export dataset to file.
     *
     * @param datasetId Dataset ID
     * @param outputPath Output file path
     * @return Number of examples exported
     * @throws IOException if file write fails
     * @throws IllegalArgumentException if dataset not found
     */
    public int exportDataset(final String datasetId, final Path outputPath) throws IOException {
        FineTuningDataset dataset = datasets.get(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found: " + datasetId);
        }

        log.info("Exporting dataset {} to {}", dataset.name(), outputPath);

        switch (dataset.format()) {
            case OPENAI -> exportOpenAIFormat(dataset, outputPath);
            case ANTHROPIC -> exportAnthropicFormat(dataset, outputPath);
            case JSONL -> exportJSONLFormat(dataset, outputPath);
            default -> throw new IllegalStateException("Unknown format: " + dataset.format());
        }

        log.info("Exported {} examples", dataset.examples().size());
        return dataset.examples().size();
    }

    /**
     * Export in OpenAI fine-tuning format.
     *
     * @param dataset Dataset to export
     * @param outputPath Output file path
     * @throws IOException if file write fails
     */
    private void exportOpenAIFormat(final FineTuningDataset dataset, final Path outputPath)
            throws IOException {
        List<String> lines =
                dataset.examples().stream()
                        .map(
                                example -> {
                                    Map<String, Object> json =
                                            Map.of(
                                                    "messages",
                                                    List.of(
                                                            Map.of(
                                                                    "role",
                                                                    "user",
                                                                    "content",
                                                                    example.prompt()),
                                                            Map.of(
                                                                    "role",
                                                                    "assistant",
                                                                    "content",
                                                                    example.completion())));
                                    try {
                                        return mapper.writeValueAsString(json);
                                    } catch (Exception e) {
                                        log.error(
                                                "Failed to serialize example: {}", e.getMessage());
                                        return null;
                                    }
                                })
                        .filter(Objects::nonNull)
                        .toList();

        Files.write(
                outputPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Export in Anthropic fine-tuning format.
     *
     * @param dataset Dataset to export
     * @param outputPath Output file path
     * @throws IOException if file write fails
     */
    private void exportAnthropicFormat(final FineTuningDataset dataset, final Path outputPath)
            throws IOException {
        List<String> lines =
                dataset.examples().stream()
                        .map(
                                example -> {
                                    Map<String, Object> json =
                                            Map.of(
                                                    "prompt", example.prompt(),
                                                    "completion", example.completion());
                                    try {
                                        return mapper.writeValueAsString(json);
                                    } catch (Exception e) {
                                        log.error(
                                                "Failed to serialize example: {}", e.getMessage());
                                        return null;
                                    }
                                })
                        .filter(Objects::nonNull)
                        .toList();

        Files.write(
                outputPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Export in generic JSONL format.
     *
     * @param dataset Dataset to export
     * @param outputPath Output file path
     * @throws IOException if file write fails
     */
    private void exportJSONLFormat(final FineTuningDataset dataset, final Path outputPath)
            throws IOException {
        List<String> lines =
                dataset.examples().stream()
                        .map(
                                example -> {
                                    Map<String, Object> json = new HashMap<>();
                                    json.put("prompt", example.prompt());
                                    json.put("completion", example.completion());
                                    if (example.metadata() != null) {
                                        json.put("metadata", example.metadata());
                                    }
                                    try {
                                        return mapper.writeValueAsString(json);
                                    } catch (Exception e) {
                                        log.error(
                                                "Failed to serialize example: {}", e.getMessage());
                                        return null;
                                    }
                                })
                        .filter(Objects::nonNull)
                        .toList();

        Files.write(
                outputPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Get dataset statistics.
     *
     * @param datasetId Dataset ID
     * @return Dataset statistics
     * @throws IllegalArgumentException if dataset not found
     */
    public DatasetStats getStats(final String datasetId) {
        FineTuningDataset dataset = datasets.get(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found: " + datasetId);
        }

        int totalExamples = dataset.examples().size();
        long totalPromptTokens =
                dataset.examples().stream().mapToLong(e -> estimateTokens(e.prompt())).sum();
        long totalCompletionTokens =
                dataset.examples().stream().mapToLong(e -> estimateTokens(e.completion())).sum();

        return new DatasetStats(
                dataset.name(),
                totalExamples,
                totalPromptTokens,
                totalCompletionTokens,
                dataset.createdAt());
    }

    /**
     * List all datasets.
     *
     * @return List of all datasets
     */
    public List<FineTuningDataset> listDatasets() {
        return new ArrayList<>(datasets.values());
    }

    /**
     * Delete a dataset.
     *
     * @param datasetId Dataset ID to delete
     */
    public void deleteDataset(final String datasetId) {
        datasets.remove(datasetId);
        log.info("Deleted dataset: {}", datasetId);
    }

    /**
     * Estimate token count (rough approximation).
     *
     * @param text Text to estimate tokens for
     * @return Estimated token count
     */
    private long estimateTokens(final String text) {
        // Rough estimate: ~4 characters per token
        return text.length() / CHARS_PER_TOKEN;
    }

    /** Dataset format enum. */
    public enum DatasetFormat {
        /** OpenAI fine-tuning format. */
        OPENAI,
        /** Anthropic fine-tuning format. */
        ANTHROPIC,
        /** Generic JSONL format. */
        JSONL
    }

    /**
     * Fine-tuning dataset.
     *
     * @param id Unique dataset identifier
     * @param name Human-readable dataset name
     * @param format Export format for the dataset
     * @param examples List of training examples
     * @param createdAt Timestamp when dataset was created
     */
    public record FineTuningDataset(
            String id,
            String name,
            DatasetFormat format,
            List<TrainingExample> examples,
            Instant createdAt) {}

    /**
     * Training example.
     *
     * @param prompt User input prompt
     * @param completion Expected model completion
     * @param metadata Optional additional metadata
     * @param createdAt Timestamp when example was created
     */
    public record TrainingExample(
            String prompt, String completion, Map<String, Object> metadata, Instant createdAt) {}

    /**
     * Dataset statistics.
     *
     * @param name Dataset name
     * @param totalExamples Total number of examples
     * @param totalPromptTokens Total tokens in all prompts
     * @param totalCompletionTokens Total tokens in all completions
     * @param createdAt Dataset creation timestamp
     */
    public record DatasetStats(
            String name,
            int totalExamples,
            long totalPromptTokens,
            long totalCompletionTokens,
            Instant createdAt) {
        /**
         * Get total tokens across prompts and completions.
         *
         * @return Total token count
         */
        public long totalTokens() {
            return totalPromptTokens + totalCompletionTokens;
        }
    }
}
