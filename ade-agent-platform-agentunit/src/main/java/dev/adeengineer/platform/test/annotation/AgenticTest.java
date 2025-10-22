package dev.adeengineer.platform.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import dev.adeengineer.platform.test.extension.AgenticTestExtension;

/**
 * Annotation for AgenticBoot test classes.
 *
 * <p>Enables automatic mock injection and test infrastructure setup for agent-related tests.
 * Use with {@link MockLLM} and {@link MockAgent} annotations for automatic mock configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @AgenticTest
 * class MyAgentTest {
 *
 *     @MockLLM(responseContent = "Test response")
 *     MockLLMProvider llmProvider;
 *
 *     @MockAgent(name = "developer")
 *     MockAgent agent;
 *
 *     @Test
 *     void shouldExecuteTask() {
 *         // Mocks are automatically configured and ready to use
 *         TaskResult result = agent.executeTask(TestData.validTaskRequest());
 *         assertThat(result.success()).isTrue();
 *     }
 * }
 * }</pre>
 *
 * @see MockLLM
 * @see MockAgent
 * @see AgenticTestExtension
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AgenticTestExtension.class)
public @interface AgenticTest {

    /**
     * Whether to automatically configure mocks with sensible defaults.
     *
     * <p>When true (default), all {@link MockLLM} and {@link MockAgent} fields are automatically
     * instantiated and configured before each test.
     *
     * @return true to auto-configure mocks, false to disable
     */
    boolean autoConfigureMocks() default true;
}
