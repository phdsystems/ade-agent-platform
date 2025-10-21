package dev.adeengineer.platform.test.micronaut;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.test.mock.MockLLMProvider;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

/**
 * Base class for Micronaut-based integration tests.
 *
 * <p>Provides common setup for testing with Micronaut DI container. Subclasses should be annotated
 * with {@code @MicronautTest}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @MicronautTest
 * class MyMicronautServiceTest extends BaseMicronautTest {
 *
 *     @Inject
 *     MyService myService;
 *
 *     @Test
 *     void shouldExecuteTask() {
 *         configureMockLLM("Micronaut test response");
 *         String result = myService.process("test input");
 *         assertThat(result).isEqualTo("Micronaut test response");
 *     }
 * }
 * }</pre>
 *
 * <p><b>Note:</b> Micronaut DI requires the {@code @MicronautTest} annotation on the test class.
 * This base class does not include it to allow subclasses to customize the annotation (e.g.,
 * {@code @MicronautTest(startApplication = false)} for faster tests).
 */
public abstract class BaseMicronautTest {

    /** Mock LLM provider for testing without real API calls */
    protected MockLLMProvider mockLLMProvider;

    /**
     * Sets up common test infrastructure before each test.
     *
     * <p>Initializes MockLLMProvider with sensible defaults.
     *
     * <p>Note: Subclasses should call this method if they override @BeforeEach.
     */
    @BeforeEach
    protected void setUpMicronautTest() {
        mockLLMProvider = new MockLLMProvider();
    }

    /**
     * Configures the mock LLM provider with a custom response.
     *
     * @param responseContent content to return from LLM calls
     */
    protected void configureMockLLM(String responseContent) {
        mockLLMProvider.withResponseContent(responseContent);
    }

    /**
     * Configures the mock LLM provider to throw an exception.
     *
     * @param exception exception to throw
     */
    protected void configureMockLLMToFail(RuntimeException exception) {
        mockLLMProvider.withException(exception);
    }
}
