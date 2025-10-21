package dev.adeengineer.examples;

import adeengineer.dev.agent.Agent;
import adeengineer.dev.agent.AgentConfig;
import adeengineer.dev.agent.OutputFormatterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.adeengineer.llm.LLMProvider;
import dev.adeengineer.llm.model.LLMResponse;
import dev.adeengineer.llm.model.UsageInfo;
import dev.adeengineer.platform.core.AgentRegistry;
import dev.adeengineer.platform.core.ConfigurableAgent;
import dev.adeengineer.platform.core.DomainLoader;
import dev.adeengineer.platform.providers.storage.LocalStorageProvider;

import java.util.List;
import java.util.Map;

/**
 * Example: Using ade-agent-platform-core WITHOUT any framework.
 *
 * <p>This demonstrates the platform's framework-agnostic nature.
 * No Spring, Quarkus, or Micronaut required - just plain Java.
 *
 * <p><b>Dependencies:</b>
 * <pre>
 * &lt;dependency&gt;
 *     &lt;groupId&gt;adeengineer.dev&lt;/groupId&gt;
 *     &lt;artifactId&gt;ade-agent-platform-core&lt;/artifactId&gt;
 *     &lt;version&gt;0.2.0-SNAPSHOT&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
public class VanillaJavaExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Vanilla Java Example ===");
        System.out.println("Using ade-agent-platform-core WITHOUT any framework\n");

        // 1. Create dependencies (POJOs - no framework required)
        AgentRegistry registry = new AgentRegistry();
        OutputFormatterRegistry formatterRegistry = new OutputFormatterRegistry();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        LocalStorageProvider storage = new LocalStorageProvider("./data/storage", objectMapper);

        // 2. Create a simple LLM provider (mock for demo)
        LLMProvider llmProvider = new SimpleLLMProvider();

        // 3. Create agents programmatically (without YAML)
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

        // 4. Use the agent
        String task = "Write a simple Java hello world program";
        String result = developer.execute(task);

        System.out.println("Task: " + task);
        System.out.println("Result: " + result);
        System.out.println("\n✅ Success! Platform works without any framework.");

        // 5. Optional: Load domains from YAML files
        DomainLoader domainLoader = new DomainLoader(registry, formatterRegistry);

        // Uncomment to load domains:
        // int agentCount = domainLoader.loadAllDomains("./domains", llmProvider);
        // System.out.println("Loaded " + agentCount + " agents from domains");
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
