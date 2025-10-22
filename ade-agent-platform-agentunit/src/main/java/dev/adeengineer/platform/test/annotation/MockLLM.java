package dev.adeengineer.platform.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a configured {@link dev.adeengineer.platform.test.mock.MockLLMProvider} into a test
 * field.
 *
 * <p>Must be used in conjunction with {@link AgenticBootTest} annotation on the test class.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @AgenticBootTest
 * class MyTest {
 *
 *     @MockLLM(responseContent = "Success response")
 *     MockLLMProvider llmProvider;
 *
 *     @Test
 *     void test() {
 *         LLMResponse response = llmProvider.generate("prompt", 0.7, 100);
 *         assertThat(response.content()).isEqualTo("Success response");
 *     }
 * }
 * }</pre>
 *
 * @see AgenticBootTest
 * @see dev.adeengineer.platform.test.mock.MockLLMProvider
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockLLM {

    /**
     * The response content to return from LLM calls.
     *
     * @return response content
     */
    String responseContent() default "Test LLM response";

    /**
     * The provider name for the mock LLM.
     *
     * @return provider name
     */
    String providerName() default "test-provider";

    /**
     * The model name for the mock LLM.
     *
     * @return model name
     */
    String model() default "test-model";

    /**
     * Whether the mock LLM provider should report as healthy.
     *
     * @return true if healthy, false otherwise
     */
    boolean healthy() default true;
}
