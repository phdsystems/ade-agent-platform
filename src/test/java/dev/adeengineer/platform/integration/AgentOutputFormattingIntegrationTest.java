package dev.adeengineer.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.adeengineer.agent.OutputFormatterRegistry;
import dev.adeengineer.agent.TaskRequest;
import dev.adeengineer.agent.TaskResult;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.RoleManager;

/**
 * Integration tests for agent output formatting. Tests how different agents format their outputs
 * based on configuration.
 */
@DisplayName("Agent Output Formatting Integration Tests")
class AgentOutputFormattingIntegrationTest extends BaseIntegrationTest {

    @Autowired private RoleManager roleManager;

    @Autowired private AgentRegistry registry;

    @Autowired private OutputFormatterRegistry formatterRegistry;

    @Test
    @DisplayName("Should format technical output for Developer agent")
    void shouldFormatTechnicalOutputForDeveloperAgent() {
        // Given
        TaskRequest request =
                new TaskRequest("Software Developer", "Write unit tests for a REST API", Map.of());

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.output()).isNotBlank();

        // Technical format should include metadata footer
        assertThat(result.output()).contains("Technical Details");
        assertThat(result.output()).contains("Provider:");
        assertThat(result.output()).contains("Model:");
        assertThat(result.output()).contains("Tokens:");
    }

    @Test
    @DisplayName("Should format technical output for QA agent")
    void shouldFormatTechnicalOutputForQAAgent() {
        // Given
        TaskRequest request =
                new TaskRequest("QA Engineer", "Create test strategy for microservices", Map.of());

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.output()).isNotBlank();

        // QA engineer also uses technical format
        assertThat(result.output()).contains("Technical Details");
        assertThat(result.output()).contains("Provider:");
        assertThat(result.output()).contains("Model:");
    }

    @Test
    @DisplayName("Should format technical output for Security agent")
    void shouldFormatTechnicalOutputForSecurityAgent() {
        // Given
        TaskRequest request =
                new TaskRequest(
                        "Security Engineer", "Review authentication implementation", Map.of());

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.output()).isNotBlank();

        // Security engineer uses technical format
        assertThat(result.output()).contains("Technical Details");
    }

    @Test
    @DisplayName("Should format business output for Manager agent")
    void shouldFormatBusinessOutputForManagerAgent() {
        // Given
        TaskRequest request =
                new TaskRequest("Engineering Manager", "Create sprint planning report", Map.of());

        // When
        TaskResult result = roleManager.executeTask(request);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.output()).isNotBlank();

        // Manager uses executive format (not business)
        assertThat(result.output()).contains("Executive Summary");
    }

    @Test
    @DisplayName("Should apply formatter with technical format type")
    void shouldApplyFormatterWithTechnicalFormatType() {
        // Given
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.01);
        LLMResponse response =
                new LLMResponse("Technical analysis content", usage, "test-provider", "test-model");

        // When
        String formatted = formatterRegistry.format(response, "technical");

        // Then
        assertThat(formatted).contains("Technical analysis content");
        assertThat(formatted).contains("Technical Details");
        assertThat(formatted).contains("Provider: test-provider");
        assertThat(formatted).contains("Model: test-model");
        assertThat(formatted).contains("Tokens: 300");
    }

    @Test
    @DisplayName("Should apply formatter with business format type")
    void shouldApplyFormatterWithBusinessFormatType() {
        // Given
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.01);
        LLMResponse response =
                new LLMResponse("Business insights content", usage, "test-provider", "test-model");

        // When
        String formatted = formatterRegistry.format(response, "business");

        // Then
        assertThat(formatted).contains("Business Summary");
        assertThat(formatted).contains("Business insights content");
        assertThat(formatted).contains("Resource Usage");
        assertThat(formatted).contains("Estimated Cost: $0.0100");
    }

    @Test
    @DisplayName("Should apply formatter with executive format type")
    void shouldApplyFormatterWithExecutiveFormatType() {
        // Given
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.01);
        LLMResponse response =
                new LLMResponse("Executive summary content", usage, "test-provider", "test-model");

        // When
        String formatted = formatterRegistry.format(response, "executive");

        // Then
        assertThat(formatted).contains("Executive Summary");
        assertThat(formatted).contains("Executive summary content");

        // Should NOT contain technical details or cost
        assertThat(formatted).doesNotContain("Technical Details");
        assertThat(formatted).doesNotContain("Resource Usage");
        assertThat(formatted).doesNotContain("Provider:");
        assertThat(formatted).doesNotContain("Model:");
    }

    @Test
    @DisplayName("Should return raw content for unknown format type")
    void shouldReturnRawContentForUnknownFormatType() {
        // Given
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.01);
        LLMResponse response = new LLMResponse("Raw content", usage, "test-provider", "test-model");

        // When
        String formatted = formatterRegistry.format(response, "unknown-format");

        // Then
        assertThat(formatted).isEqualTo("Raw content");
        assertThat(formatted).doesNotContain("Technical Details");
        assertThat(formatted).doesNotContain("Business Summary");
        assertThat(formatted).doesNotContain("Executive Summary");
    }

    @Test
    @DisplayName("Should handle null response gracefully")
    void shouldHandleNullResponseGracefully() {
        // When
        String formatted = formatterRegistry.format(null, "technical");

        // Then
        assertThat(formatted).isEmpty();
    }

    @Test
    @DisplayName("Should have different output formats for different agent types")
    void shouldHaveDifferentOutputFormatsForDifferentAgentTypes() {
        // Given
        TaskRequest developerRequest = new TaskRequest("Software Developer", "Code task", Map.of());
        TaskRequest managerRequest =
                new TaskRequest("Engineering Manager", "Management task", Map.of());

        // When
        TaskResult developerResult = roleManager.executeTask(developerRequest);
        TaskResult managerResult = roleManager.executeTask(managerRequest);

        // Then
        assertThat(developerResult.success()).isTrue();
        assertThat(managerResult.success()).isTrue();

        // Developer uses technical format
        assertThat(developerResult.output()).contains("Technical Details");
        assertThat(developerResult.output()).doesNotContain("Executive Summary");

        // Manager uses executive format
        assertThat(managerResult.output()).contains("Executive Summary");
        assertThat(managerResult.output()).doesNotContain("Technical Details");
    }

    @Test
    @DisplayName("Should preserve content when formatting")
    void shouldPreserveContentWhenFormatting() {
        // Given
        UsageInfo usage = new UsageInfo(50, 100, 150, 0.005);
        String originalContent = "Important content that must be preserved";
        LLMResponse response =
                new LLMResponse(originalContent, usage, "test-provider", "test-model");

        // When
        String technicalFormat = formatterRegistry.format(response, "technical");
        String businessFormat = formatterRegistry.format(response, "business");
        String executiveFormat = formatterRegistry.format(response, "executive");

        // Then - All formats should preserve the original content
        assertThat(technicalFormat).contains(originalContent);
        assertThat(businessFormat).contains(originalContent);
        assertThat(executiveFormat).contains(originalContent);
    }

    @Test
    @DisplayName("Should format cost correctly in business format")
    void shouldFormatCostCorrectlyInBusinessFormat() {
        // Given
        UsageInfo usage = new UsageInfo(1000, 2000, 3000, 0.123456);
        LLMResponse response = new LLMResponse("Content", usage, "test", "test");

        // When
        String formatted = formatterRegistry.format(response, "business");

        // Then
        assertThat(formatted).contains("Estimated Cost: $0.1235");
    }

    @Test
    @DisplayName("Should format tokens correctly in technical format")
    void shouldFormatTokensCorrectlyInTechnicalFormat() {
        // Given
        UsageInfo usage = new UsageInfo(1234, 5678, 6912, 0.01);
        LLMResponse response = new LLMResponse("Content", usage, "test-provider", "test-model");

        // When
        String formatted = formatterRegistry.format(response, "technical");

        // Then
        assertThat(formatted).contains("Tokens: 6912");
        assertThat(formatted).contains("Provider: test-provider");
        assertThat(formatted).contains("Model: test-model");
    }
}
