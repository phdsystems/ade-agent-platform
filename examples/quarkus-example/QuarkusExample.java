package dev.adeengineer.examples;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.agent.OutputFormatterRegistry;

import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.ConfigurableAgent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Example: Using ade-agent-platform-quarkus with CDI.
 *
 * <p>This demonstrates Quarkus integration with CDI dependency injection.
 * All core components are auto-configured as CDI beans.
 *
 * <p><b>Dependencies:</b>
 * <pre>
 * &lt;dependency&gt;
 *     &lt;groupId&gt;adeengineer.dev&lt;/groupId&gt;
 *     &lt;artifactId&gt;ade-agent-platform-quarkus&lt;/artifactId&gt;
 *     &lt;version&gt;0.2.0-SNAPSHOT&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <p><b>Run as native image:</b>
 * <pre>
 * ./mvnw package -Pnative
 * ./target/quarkus-example-runner
 * </pre>
 */
@Slf4j
public class QuarkusExample {

    @Inject
    AgentRegistry registry;

    @Inject
    OutputFormatterRegistry formatterRegistry;

    void onStart(@Observes StartupEvent ev) {
        log.info("=== Quarkus Example ===");
        log.info("Using ade-agent-platform-quarkus with CDI\n");

        try {
            // Create a simple LLM provider (mock for demo)
            LLMProvider llmProvider = new SimpleLLMProvider();

            // Create agent programmatically
            AgentConfig agentConfig = new AgentConfig(
                    "developer",
                    "Developer Agent",
                    "A software developer agent",
                    "You are an expert software developer.",
                    List.of(
                            new AgentConfig.Example(
                                    "Write a hello world function",
                                    "def hello_world():\n    print('Hello, World!')"
                            )
                    ),
                    Map.of("temperature", 0.7),
                    "gpt-4",
                    "raw",
                    null
            );

            Agent developer = new ConfigurableAgent(agentConfig, llmProvider, formatterRegistry);
            registry.registerAgent(developer);

            // Use the agent
            String task = "Write a simple Java hello world program";
            String result = developer.execute(task);

            log.info("Task: {}", task);
            log.info("Result: {}", result);
            log.info("\n✅ Success! Platform works with Quarkus CDI.");

        } catch (Exception e) {
            log.error("Error executing agent", e);
        }
    }

    /**
     * Simple mock LLM provider for demonstration.
     * In production, use a real provider (OpenAI, Anthropic, etc.)
     */
    static class SimpleLLMProvider implements LLMProvider {
        @Override
        public LLMResponse generate(String prompt, double temperature, int maxTokens) {
            String response = """
                    public class HelloWorld {
                        public static void main(String[] args) {
                            System.out.println("Hello, World!");
                        }
                    }
                    """;

            return new LLMResponse(
                    response,
                    new UsageInfo(50, 100, 150, 0.001),
                    "simple-provider",
                    "mock-model"
            );
        }

        @Override
        public String getProviderName() {
            return "simple-provider";
        }

        @Override
        public String getModel() {
            return "mock-model";
        }

        @Override
        public boolean isHealthy() {
            return true;
        }
    }
}
