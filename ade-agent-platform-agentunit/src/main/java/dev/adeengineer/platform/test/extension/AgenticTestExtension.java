package dev.adeengineer.platform.test.extension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import dev.adeengineer.platform.test.annotation.AgenticTest;
import dev.adeengineer.platform.test.annotation.MockAgent;
import dev.adeengineer.platform.test.annotation.MockLLM;
import dev.adeengineer.platform.test.mock.MockLLMProvider;

/**
 * JUnit 5 extension that provides automatic mock injection for AgenticBoot tests.
 *
 * <p>This extension is automatically registered when using the {@link AgenticTest} annotation.
 * It handles:
 *
 * <ul>
 *   <li>Injection of {@link MockLLMProvider} for fields annotated with {@link MockLLM}
 *   <li>Injection of {@link dev.adeengineer.platform.test.mock.MockAgent} for fields annotated
 *       with {@link MockAgent}
 *   <li>Configuration of mocks based on annotation parameters
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * @AgenticTest
 * class MyTest {
 *     @MockLLM(responseContent = "Test")
 *     MockLLMProvider llm;  // Automatically injected
 *
 *     @MockAgent(name = "developer")
 *     MockAgent agent;  // Automatically injected
 * }
 * }</pre>
 */
public class AgenticTestExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Class<?> testClass = testInstance.getClass();

        // Check if auto-configuration is enabled
        AgenticTest agenticTest = testClass.getAnnotation(AgenticTest.class);
        if (agenticTest == null || !agenticTest.autoConfigureMocks()) {
            return;
        }

        // Inject @MockLLM fields
        injectMockLLMProviders(testInstance, testClass);

        // Inject @MockAgent fields
        injectMockAgents(testInstance, testClass);
    }

    /**
     * Injects MockLLMProvider instances into fields annotated with @MockLLM.
     *
     * @param testInstance the test instance
     * @param testClass the test class
     * @throws IllegalAccessException if field cannot be accessed
     */
    private void injectMockLLMProviders(Object testInstance, Class<?> testClass)
            throws IllegalAccessException {
        for (Field field : getAllFields(testClass)) {
            if (field.isAnnotationPresent(MockLLM.class)) {
                MockLLM annotation = field.getAnnotation(MockLLM.class);

                // Create MockLLMProvider with configuration from annotation
                MockLLMProvider provider =
                        new MockLLMProvider(
                                annotation.providerName(), annotation.model(), annotation.healthy());
                provider.withResponseContent(annotation.responseContent());

                // Inject into field
                field.setAccessible(true);
                field.set(testInstance, provider);
            }
        }
    }

    /**
     * Injects MockAgent instances into fields annotated with @MockAgent.
     *
     * @param testInstance the test instance
     * @param testClass the test class
     * @throws IllegalAccessException if field cannot be accessed
     */
    private void injectMockAgents(Object testInstance, Class<?> testClass)
            throws IllegalAccessException {
        for (Field field : getAllFields(testClass)) {
            if (field.isAnnotationPresent(MockAgent.class)) {
                MockAgent annotation = field.getAnnotation(MockAgent.class);

                // Create MockAgent with configuration from annotation
                String name = annotation.name();
                String description =
                        annotation.description().isEmpty()
                                ? "Mock agent for " + name
                                : annotation.description();

                List<String> capabilities =
                        annotation.capabilities().length > 0
                                ? Arrays.asList(annotation.capabilities())
                                : List.of("capability1", "capability2");

                dev.adeengineer.platform.test.mock.MockAgent mockAgent =
                        new dev.adeengineer.platform.test.mock.MockAgent(
                                name, description, capabilities);

                // Inject into field
                field.setAccessible(true);
                field.set(testInstance, mockAgent);
            }
        }
    }

    /**
     * Gets all fields from the class hierarchy, including private fields from superclasses.
     *
     * @param clazz the class to get fields from
     * @return list of all fields
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new java.util.ArrayList<>();
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
