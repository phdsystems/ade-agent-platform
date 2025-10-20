package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Gatling load test for LLM provider endpoints.
 * Tests throughput, latency, and error rates under load.
 */
class LLMLoadTest extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("base.url", "http://localhost:8080")
  val users = Integer.getInteger("users", 10)
  val rampDuration = Integer.getInteger("ramp.duration", 60).seconds
  val testDuration = Integer.getInteger("test.duration", 300).seconds

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")

  // Test scenarios
  val generateScenario = scenario("LLM Generation")
    .during(testDuration) {
      exec(
        http("Generate Response")
          .post("/api/llm/generate")
          .body(StringBody(
            """{
              "prompt": "Explain quantum computing in one sentence",
              "temperature": 0.7,
              "maxTokens": 100
            }"""
          ))
          .check(status.is(200))
          .check(jsonPath("$.content").exists)
          .check(jsonPath("$.provider").exists)
      )
      .pause(1.second, 3.seconds)
    }

  val streamingScenario = scenario("LLM Streaming")
    .during(testDuration) {
      exec(
        http("Generate Streaming Response")
          .post("/api/llm/generate/stream")
          .body(StringBody(
            """{
              "prompt": "Write a short story about AI",
              "temperature": 0.8,
              "maxTokens": 200
            }"""
          ))
          .check(status.is(200))
      )
      .pause(2.seconds, 5.seconds)
    }

  val healthScenario = scenario("Health Check")
    .during(testDuration) {
      exec(
        http("Check Health")
          .get("/actuator/health")
          .check(status.is(200))
      )
      .pause(5.seconds, 10.seconds)
    }

  // Load profile: ramp up users over time
  setUp(
    generateScenario.inject(rampUsers(users).during(rampDuration)),
    streamingScenario.inject(rampUsers(users / 2).during(rampDuration)),
    healthScenario.inject(rampUsers(users / 5).during(rampDuration))
  )
  .protocols(httpProtocol)
  .assertions(
    global.responseTime.max.lt(5000), // Max response time under 5s
    global.successfulRequests.percent.gt(95) // 95% success rate
  )
}
