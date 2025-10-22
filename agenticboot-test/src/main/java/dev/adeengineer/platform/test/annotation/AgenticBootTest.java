package dev.adeengineer.platform.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import dev.adeengineer.platform.test.extension.AgenticBootTestExtension;

/**
 * Annotation for AgenticBoot test classes.
 *
 * <p>This is AgenticBoot's testing annotation, following the same pattern as:
 * <ul>
 *   <li>{@code @SpringBootTest} for Spring Boot
 *   <li>{@code @QuarkusTest} for Quarkus
 *   <li>{@code @MicronautTest} for Micronaut
 * </ul>
 *
 * <p>Enables automatic mock injection and test infrastructure setup for agent-related tests.
 * Use with {@link MockLLM} and {@link MockAgent} annotations for automatic mock configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @AgenticBootTest
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
 * @see AgenticBootTestExtension
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AgenticBootTestExtension.class)
public @interface AgenticBootTest {

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
