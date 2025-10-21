package dev.adeengineer.platform.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.testutil.TestData;

@DisplayName("UsageInfo Model Tests")
class UsageInfoTest {

    @Test
    @DisplayName("Should create valid UsageInfo with all fields")
    void shouldCreateValidUsageInfo() {
        // Given
        int inputTokens = 100;
        int outputTokens = 200;
        int totalTokens = 300;
        double estimatedCost = 0.01;

        // When
        UsageInfo usage = new UsageInfo(inputTokens, outputTokens, totalTokens, estimatedCost);

        // Then
        assertThat(usage.inputTokens()).isEqualTo(inputTokens);
        assertThat(usage.outputTokens()).isEqualTo(outputTokens);
        assertThat(usage.totalTokens()).isEqualTo(totalTokens);
        assertThat(usage.estimatedCost()).isEqualTo(estimatedCost);
    }

    @Test
    @DisplayName("Should throw exception when inputTokens is negative")
    void shouldThrowExceptionWhenInputTokensIsNegative() {
        // When/Then
        assertThatThrownBy(() -> new UsageInfo(-1, 100, 100, 0.01))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token counts cannot be negative");
    }

    @Test
    @DisplayName("Should throw exception when outputTokens is negative")
    void shouldThrowExceptionWhenOutputTokensIsNegative() {
        // When/Then
        assertThatThrownBy(() -> new UsageInfo(100, -1, 100, 0.01))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token counts cannot be negative");
    }

    @ParameterizedTest
    @CsvSource({"-1, -1", "-100, -200", "0, -1", "-1, 0"})
    @DisplayName("Should throw exception when both token counts are negative or one is negative")
    void shouldThrowExceptionWhenTokenCountsAreNegative(int inputTokens, int outputTokens) {
        // When/Then
        assertThatThrownBy(() -> new UsageInfo(inputTokens, outputTokens, 0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token counts cannot be negative");
    }

    @Test
    @DisplayName("Should accept zero tokens")
    void shouldAcceptZeroTokens() {
        // When
        UsageInfo usage = new UsageInfo(0, 0, 0, 0.0);

        // Then
        assertThat(usage.inputTokens()).isZero();
        assertThat(usage.outputTokens()).isZero();
        assertThat(usage.totalTokens()).isZero();
        assertThat(usage.estimatedCost()).isZero();
    }

    @Test
    @DisplayName("Should accept large token counts")
    void shouldAcceptLargeTokenCounts() {
        // When
        UsageInfo usage = new UsageInfo(1000000, 500000, 1500000, 15.0);

        // Then
        assertThat(usage.inputTokens()).isEqualTo(1000000);
        assertThat(usage.outputTokens()).isEqualTo(500000);
        assertThat(usage.totalTokens()).isEqualTo(1500000);
    }

    @Test
    @DisplayName("Should accept negative cost")
    void shouldAcceptNegativeCost() {
        // Note: No validation on cost - it can be negative (e.g., credits)
        // When
        UsageInfo usage = new UsageInfo(100, 200, 300, -0.01);

        // Then
        assertThat(usage.estimatedCost()).isEqualTo(-0.01);
    }

    @Test
    @DisplayName("Should accept zero cost")
    void shouldAcceptZeroCost() {
        // When
        UsageInfo usage = new UsageInfo(100, 200, 300, 0.0);

        // Then
        assertThat(usage.estimatedCost()).isZero();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.001, 0.01, 0.1, 1.0, 10.0, 100.0})
    @DisplayName("Should accept various cost values")
    void shouldAcceptVariousCostValues(double cost) {
        // When
        UsageInfo usage = new UsageInfo(100, 200, 300, cost);

        // Then
        assertThat(usage.estimatedCost()).isEqualTo(cost);
    }

    @Test
    @DisplayName("Should support record equality")
    void shouldSupportRecordEquality() {
        // Given
        UsageInfo usage1 = TestData.validUsageInfo();
        UsageInfo usage2 =
                new UsageInfo(
                        usage1.inputTokens(),
                        usage1.outputTokens(),
                        usage1.totalTokens(),
                        usage1.estimatedCost());

        // Then
        assertThat(usage1).isEqualTo(usage2);
        assertThat(usage1.hashCode()).isEqualTo(usage2.hashCode());
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        // Given
        UsageInfo usage = TestData.validUsageInfo();

        // Then
        String toString = usage.toString();
        assertThat(toString).contains("100"); // inputTokens
        assertThat(toString).contains("200"); // outputTokens
        assertThat(toString).contains("300"); // totalTokens
    }

    @Test
    @DisplayName("totalTokens can be inconsistent with sum of input and output")
    void totalTokensCanBeInconsistent() {
        // Note: There's no validation that totalTokens == inputTokens + outputTokens
        // When
        UsageInfo usage = new UsageInfo(100, 200, 999, 0.01);

        // Then
        assertThat(usage.totalTokens()).isEqualTo(999);
        assertThat(usage.inputTokens() + usage.outputTokens()).isNotEqualTo(usage.totalTokens());
    }
}
