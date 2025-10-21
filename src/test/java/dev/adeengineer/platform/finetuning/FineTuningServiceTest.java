package dev.adeengineer.platform.finetuning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.finetuning.FineTuningService.DatasetFormat;
import dev.adeengineer.platform.finetuning.FineTuningService.DatasetStats;
import dev.adeengineer.platform.finetuning.FineTuningService.FineTuningDataset;

/** Unit tests for FineTuningService. */
class FineTuningServiceTest {

    @TempDir Path tempDir;

    private FineTuningService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new FineTuningService(objectMapper);
    }

    @Test
    void shouldCreateDataset() {
        // When
        String datasetId = service.createDataset("test-dataset", DatasetFormat.OPENAI);

        // Then
        assertThat(datasetId).isNotNull();
        assertThat(service.listDatasets()).hasSize(1);

        FineTuningDataset dataset = service.listDatasets().get(0);
        assertThat(dataset.name()).isEqualTo("test-dataset");
        assertThat(dataset.format()).isEqualTo(DatasetFormat.OPENAI);
        assertThat(dataset.examples()).isEmpty();
    }

    @Test
    void shouldAddExample() {
        // Given
        String datasetId = service.createDataset("test-dataset", DatasetFormat.OPENAI);

        // When
        service.addExample(
                datasetId,
                "What is Java?",
                "Java is a programming language",
                Map.of("source", "manual", "quality", 5));

        // Then
        FineTuningDataset dataset = service.listDatasets().get(0);
        assertThat(dataset.examples()).hasSize(1);
        assertThat(dataset.examples().get(0).prompt()).isEqualTo("What is Java?");
        assertThat(dataset.examples().get(0).completion())
                .isEqualTo("Java is a programming language");
        assertThat(dataset.examples().get(0).metadata()).containsEntry("source", "manual");
    }

    @Test
    void shouldCaptureHighQualityInteractions() {
        // Given
        String datasetId = service.createDataset("test-dataset", DatasetFormat.ANTHROPIC);
        LLMResponse response =
                new LLMResponse(
                        "Java is a high-level programming language",
                        new UsageInfo(10, 50, 60, 0.001),
                        "openai",
                        "gpt-4");

        // When
        service.captureInteraction(datasetId, "Explain Java", response, 5);
        service.captureInteraction(datasetId, "Another question", response, 3);

        // Then
        FineTuningDataset dataset = service.listDatasets().get(0);
        assertThat(dataset.examples()).hasSize(1); // Only rating >= 4 captured
        assertThat(dataset.examples().get(0).prompt()).isEqualTo("Explain Java");
    }

    @Test
    void shouldExportToOpenAIFormat() throws IOException {
        // Given
        String datasetId = service.createDataset("openai-dataset", DatasetFormat.OPENAI);
        service.addExample(datasetId, "What is Spring?", "Spring is a framework", null);
        service.addExample(datasetId, "What is Maven?", "Maven is a build tool", null);

        Path outputPath = tempDir.resolve("openai-export.jsonl");

        // When
        int count = service.exportDataset(datasetId, outputPath);

        // Then
        assertThat(count).isEqualTo(2);
        assertThat(outputPath).exists();

        List<String> lines = Files.readAllLines(outputPath);
        assertThat(lines).hasSize(2);

        // Verify format
        Map<String, Object> firstExample = objectMapper.readValue(lines.get(0), Map.class);
        assertThat(firstExample).containsKey("messages");
        List<?> messages = (List<?>) firstExample.get("messages");
        assertThat(messages).hasSize(2);
    }

    @Test
    void shouldExportToAnthropicFormat() throws IOException {
        // Given
        String datasetId = service.createDataset("anthropic-dataset", DatasetFormat.ANTHROPIC);
        service.addExample(datasetId, "Explain testing", "Testing ensures code quality", null);

        Path outputPath = tempDir.resolve("anthropic-export.jsonl");

        // When
        int count = service.exportDataset(datasetId, outputPath);

        // Then
        assertThat(count).isEqualTo(1);
        assertThat(outputPath).exists();

        List<String> lines = Files.readAllLines(outputPath);
        assertThat(lines).hasSize(1);

        Map<String, Object> example = objectMapper.readValue(lines.get(0), Map.class);
        assertThat(example).containsKey("prompt");
        assertThat(example).containsKey("completion");
    }

    @Test
    void shouldExportToJSONLFormat() throws IOException {
        // Given
        String datasetId = service.createDataset("jsonl-dataset", DatasetFormat.JSONL);
        service.addExample(
                datasetId,
                "What is Docker?",
                "Docker is a containerization platform",
                Map.of("category", "DevOps", "difficulty", "medium"));

        Path outputPath = tempDir.resolve("jsonl-export.jsonl");

        // When
        int count = service.exportDataset(datasetId, outputPath);

        // Then
        assertThat(count).isEqualTo(1);
        assertThat(outputPath).exists();

        List<String> lines = Files.readAllLines(outputPath);
        Map<String, Object> example = objectMapper.readValue(lines.get(0), Map.class);
        assertThat(example).containsKey("prompt");
        assertThat(example).containsKey("completion");
        assertThat(example).containsKey("metadata");

        Map<String, Object> metadata = (Map<String, Object>) example.get("metadata");
        assertThat(metadata).containsEntry("category", "DevOps");
    }

    @Test
    void shouldCalculateDatasetStats() {
        // Given
        String datasetId = service.createDataset("stats-dataset", DatasetFormat.OPENAI);
        service.addExample(datasetId, "Short prompt", "Short completion", null);
        service.addExample(
                datasetId,
                "This is a longer prompt with more content to analyze",
                "This is also a longer completion with detailed information",
                null);

        // When
        DatasetStats stats = service.getStats(datasetId);

        // Then
        assertThat(stats.name()).isEqualTo("stats-dataset");
        assertThat(stats.totalExamples()).isEqualTo(2);
        assertThat(stats.totalPromptTokens()).isGreaterThan(0);
        assertThat(stats.totalCompletionTokens()).isGreaterThan(0);
        assertThat(stats.totalTokens()).isGreaterThan(0);
    }

    @Test
    void shouldListDatasets() {
        // When
        service.createDataset("dataset1", DatasetFormat.OPENAI);
        service.createDataset("dataset2", DatasetFormat.ANTHROPIC);
        service.createDataset("dataset3", DatasetFormat.JSONL);

        // Then
        List<FineTuningDataset> datasets = service.listDatasets();
        assertThat(datasets).hasSize(3);
        assertThat(datasets)
                .extracting(FineTuningDataset::name)
                .containsExactlyInAnyOrder("dataset1", "dataset2", "dataset3");
    }

    @Test
    void shouldDeleteDataset() {
        // Given
        String datasetId = service.createDataset("to-delete", DatasetFormat.OPENAI);
        assertThat(service.listDatasets()).hasSize(1);

        // When
        service.deleteDataset(datasetId);

        // Then
        assertThat(service.listDatasets()).isEmpty();
    }

    @Test
    void shouldThrowExceptionForNonExistentDataset() {
        // When/Then
        assertThatThrownBy(() -> service.addExample("nonexistent", "prompt", "completion", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dataset not found");
    }

    @Test
    void shouldThrowExceptionForExportOfNonExistentDataset() {
        // Given
        Path outputPath = tempDir.resolve("export.jsonl");

        // When/Then
        assertThatThrownBy(() -> service.exportDataset("nonexistent", outputPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dataset not found");
    }

    @Test
    void shouldEstimateTokensCorrectly() {
        // Given
        String datasetId = service.createDataset("token-test", DatasetFormat.OPENAI);

        // Add example with known length
        String prompt = "a".repeat(100); // 100 chars ~= 25 tokens (4 chars per token)
        String completion = "b".repeat(200); // 200 chars ~= 50 tokens

        service.addExample(datasetId, prompt, completion, null);

        // When
        DatasetStats stats = service.getStats(datasetId);

        // Then
        assertThat(stats.totalPromptTokens()).isEqualTo(25);
        assertThat(stats.totalCompletionTokens()).isEqualTo(50);
        assertThat(stats.totalTokens()).isEqualTo(75);
    }

    @Test
    void shouldHandleMultipleFormats() throws IOException {
        // Given
        String openaiId = service.createDataset("openai", DatasetFormat.OPENAI);
        String anthropicId = service.createDataset("anthropic", DatasetFormat.ANTHROPIC);
        String jsonlId = service.createDataset("jsonl", DatasetFormat.JSONL);

        service.addExample(openaiId, "Q1", "A1", null);
        service.addExample(anthropicId, "Q2", "A2", null);
        service.addExample(jsonlId, "Q3", "A3", Map.of("meta", "data"));

        // When
        Path openaiPath = tempDir.resolve("openai.jsonl");
        Path anthropicPath = tempDir.resolve("anthropic.jsonl");
        Path jsonlPath = tempDir.resolve("jsonl.jsonl");

        service.exportDataset(openaiId, openaiPath);
        service.exportDataset(anthropicId, anthropicPath);
        service.exportDataset(jsonlId, jsonlPath);

        // Then
        assertThat(openaiPath).exists();
        assertThat(anthropicPath).exists();
        assertThat(jsonlPath).exists();

        // Verify different formats
        Map<String, Object> openaiExample =
                objectMapper.readValue(Files.readString(openaiPath), Map.class);
        Map<String, Object> anthropicExample =
                objectMapper.readValue(Files.readString(anthropicPath), Map.class);
        Map<String, Object> jsonlExample =
                objectMapper.readValue(Files.readString(jsonlPath), Map.class);

        assertThat(openaiExample).containsKey("messages");
        assertThat(anthropicExample).containsKeys("prompt", "completion");
        assertThat(anthropicExample).doesNotContainKey("metadata");
        assertThat(jsonlExample).containsKeys("prompt", "completion", "metadata");
    }

    @Test
    void shouldPreserveMetadataInJSONLFormat() throws IOException {
        // Given
        String datasetId = service.createDataset("metadata-test", DatasetFormat.JSONL);

        Map<String, Object> metadata =
                Map.of(
                        "source",
                        "user-feedback",
                        "quality",
                        5,
                        "category",
                        "technical",
                        "verified",
                        true);

        service.addExample(datasetId, "Question", "Answer", metadata);

        Path outputPath = tempDir.resolve("metadata.jsonl");

        // When
        service.exportDataset(datasetId, outputPath);

        // Then
        Map<String, Object> exported =
                objectMapper.readValue(Files.readString(outputPath), Map.class);
        Map<String, Object> exportedMetadata = (Map<String, Object>) exported.get("metadata");

        assertThat(exportedMetadata).containsEntry("source", "user-feedback");
        assertThat(exportedMetadata).containsEntry("quality", 5);
        assertThat(exportedMetadata).containsEntry("category", "technical");
        assertThat(exportedMetadata).containsEntry("verified", true);
    }
}
