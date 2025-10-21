package dev.adeengineer.platform.util;

import adeengineer.dev.agent.TaskResult;

/**
 * Utility methods for type-safe access to TaskResult metadata.
 *
 * <p>Provides convenient accessors for common metadata fields.
 */
public final class TaskResultUtils {

    private TaskResultUtils() {
        // Utility class
    }

    /**
     * Get input tokens from metadata.
     *
     * @param result Task result
     * @return Input tokens, or 0 if not present
     */
    public static int getInputTokens(final TaskResult result) {
        if (result.metadata() == null) {
            return 0;
        }
        Object value = result.metadata().get("inputTokens");
        return value instanceof Integer ? (Integer) value : 0;
    }

    /**
     * Get output tokens from metadata.
     *
     * @param result Task result
     * @return Output tokens, or 0 if not present
     */
    public static int getOutputTokens(final TaskResult result) {
        if (result.metadata() == null) {
            return 0;
        }
        Object value = result.metadata().get("outputTokens");
        return value instanceof Integer ? (Integer) value : 0;
    }

    /**
     * Get total tokens from metadata.
     *
     * @param result Task result
     * @return Total tokens, or 0 if not present
     */
    public static int getTotalTokens(final TaskResult result) {
        if (result.metadata() == null) {
            return 0;
        }
        Object value = result.metadata().get("totalTokens");
        return value instanceof Integer ? (Integer) value : 0;
    }

    /**
     * Get estimated cost from metadata.
     *
     * @param result Task result
     * @return Estimated cost, or 0.0 if not present
     */
    public static double getEstimatedCost(final TaskResult result) {
        if (result.metadata() == null) {
            return 0.0;
        }
        Object value = result.metadata().get("estimatedCost");
        return value instanceof Double ? (Double) value : 0.0;
    }

    /**
     * Check if metadata contains usage information.
     *
     * @param result Task result
     * @return true if usage info is present
     */
    public static boolean hasUsageInfo(final TaskResult result) {
        return result.metadata() != null && result.metadata().containsKey("totalTokens");
    }
}
