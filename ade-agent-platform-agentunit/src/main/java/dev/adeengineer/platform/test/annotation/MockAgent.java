package dev.adeengineer.platform.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a configured {@link dev.adeengineer.platform.test.mock.MockAgent} into a test field.
 *
 * <p>Must be used in conjunction with {@link AdeAgentTest} annotation on the test class.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @AdeAgentTest
 * class MyTest {
 *
 *     @MockAgent(name = "developer", capabilities = {"coding", "testing"})
 *     dev.adeengineer.platform.test.mock.MockAgent agent;
 *
 *     @Test
 *     void test() {
 *         TaskResult result = agent.executeTask(TestData.validTaskRequest());
 *         assertThat(result.success()).isTrue();
 *     }
 * }
 * }</pre>
 *
 * @see AdeAgentTest
 * @see dev.adeengineer.platform.test.mock.MockAgent
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockAgent {

    /**
     * The name of the mock agent.
     *
     * @return agent name
     */
    String name() default "test-agent";

    /**
     * The description of the mock agent.
     *
     * @return agent description
     */
    String description() default "";

    /**
     * The capabilities of the mock agent.
     *
     * @return array of capability strings
     */
    String[] capabilities() default {};
}
