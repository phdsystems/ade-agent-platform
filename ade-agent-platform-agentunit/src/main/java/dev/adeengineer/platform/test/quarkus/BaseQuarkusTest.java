package dev.adeengineer.platform.test.quarkus;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.platform.test.mock.MockLLMProvider;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Base class for Quarkus-based integration tests.
 *
 * <p>Provides common setup for testing with Quarkus CDI container. Subclasses should be annotated
 * with {@code @QuarkusTest}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @QuarkusTest
 * class MyQuarkusServiceTest extends BaseQuarkusTest {
 *
 *     @Inject
 *     MyService myService;
 *
 *     @Test
 *     void shouldExecuteTask() {
 *         configureMockLLM("Quarkus test response");
 *         String result = myService.process("test input");
 *         assertThat(result).isEqualTo("Quarkus test response");
 *     }
 * }
 * }</pre>
 *
 * <p><b>Note:</b> Quarkus CDI requires the {@code @QuarkusTest} annotation on the test class. This
 * base class does not include it to allow subclasses to choose between {@code @QuarkusTest} and
 * {@code @QuarkusIntegrationTest}.
 */
public abstract class BaseQuarkusTest {

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
    protected void setUpQuarkusTest() {
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
