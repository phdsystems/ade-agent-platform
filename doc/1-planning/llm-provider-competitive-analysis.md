# LLM Provider Library - Competitive Analysis

**Document Type:** Planning - Market Research
**Date:** 2025-10-18
**Version:** 1.0
**Status:** Draft

---

## TL;DR

**Key findings**: JVM LLM integration space has 3 major players (LangChain4j, Spring AI, Xef.ai) but gaps exist. **Our differentiators**: JVM-wide support (Java + Kotlin + Scala), lightweight (8 providers, no heavy framework), production patterns (circuit breaker, caching, streaming), provider diversity (VLLM, BentoML, RayServe missing from competitors). **Market opportunity**: Medium - Spring AI dominates Spring ecosystem, LangChain4j leads RAG/vector, Xef.ai targets Kotlin/Scala, but comprehensive JVM provider library niche is open. **Positioning**: "Production-ready LLM provider infrastructure for the JVM - idiomatic APIs for Java, Kotlin, Scala - framework-optional, enterprise-grade patterns."

---

## Table of Contents

1. [Market Overview](#1-market-overview)
2. [Competitor Analysis](#2-competitor-analysis)
3. [Feature Comparison Matrix](#3-feature-comparison-matrix)
4. [Differentiation Strategy](#4-differentiation-strategy)
5. [Target Audience](#5-target-audience)
6. [Positioning Statement](#6-positioning-statement)
7. [Go-to-Market Strategy](#7-go-to-market-strategy)
8. [References](#8-references)

---

## 1. Market Overview

### Java LLM Integration Landscape (2025)

The Java AI/LLM ecosystem has matured significantly in 2025, with two dominant frameworks:

1. **LangChain4j** (20+ providers, 30+ vector stores) - Framework-heavy, RAG-focused
2. **Spring AI** (6 providers) - Spring Boot native, official Spring project

**Market characteristics**:
- **Growing demand**: Microsoft telemetry shows hundreds of enterprises using LangChain4j in production (July 2025)
- **Framework lock-in**: Most solutions require Spring Boot or heavy framework adoption
- **Provider fragmentation**: No single library supports all LLM providers comprehensively
- **Enterprise adoption**: Fortune 500 companies deploying Java LLM applications
- **Community activity**: LangChain4j 1.0 released May 2025, Spring AI under active development

### Market Gaps

**Gap 1: Lightweight Provider Library**
- LangChain4j requires full framework (agents, RAG, chains)
- Spring AI locked to Spring Boot
- **Opportunity**: Standalone provider abstraction without framework bloat

**Gap 2: Production Patterns**
- Most libraries focus on "happy path" integration
- Missing: Circuit breakers, caching, fallback strategies
- **Opportunity**: Enterprise-grade resilience patterns built-in

**Gap 3: Provider Diversity**
- Commercial providers well-supported (OpenAI, Anthropic)
- Open-source deployment platforms underserved (VLLM, BentoML, RayServe)
- **Opportunity**: First-class support for self-hosted LLMs

---

## 2. Competitor Analysis

### 2.1 LangChain4j

**Overview**: Open-source Java port of Python's LangChain, simplifies LLM integration for Java applications.

**Key Stats**:
- **Release**: 1.0.0 in May 2025
- **Providers**: 20+ (OpenAI, Anthropic, Google Gemini, Azure, AWS, Cohere, Mistral, local models)
- **Vector Stores**: 30+ (Pinecone, Weaviate, Chroma, etc.)
- **GitHub Stars**: Several thousand (exact number not disclosed)
- **Backing**: Microsoft partnership announced July 2025

**Strengths**:
- ✅ Comprehensive ecosystem (RAG, agents, tool calling, MCP support)
- ✅ Excellent vector database support (30+ options)
- ✅ Spring Boot integration via `langchain4j-spring-boot`
- ✅ Active community and Microsoft backing
- ✅ Good documentation and examples

**Weaknesses**:
- ⚠️ Heavy framework - not suitable for lightweight use cases
- ⚠️ Opinionated architecture (chains, agents required)
- ⚠️ Complex API surface (high learning curve)
- ⚠️ Missing providers: VLLM, BentoML, RayServe
- ⚠️ No built-in circuit breaker or caching patterns

**Target Audience**: Enterprises building RAG applications with vector databases

**Pricing**: Free (open-source, Apache 2.0)

**Market Position**: Market leader for RAG/vector use cases

---

### 2.2 Spring AI

**Overview**: Official Spring project for AI/LLM integration, designed for Spring Boot applications.

**Key Stats**:
- **Release**: Under active development (not 1.0 yet as of early 2025)
- **Providers**: 6 (OpenAI, Anthropic, Microsoft Azure, Amazon Bedrock, Google Vertex, Ollama)
- **Maintainer**: VMware/Spring Team (Mark Pollack, Chris Tzolov)
- **Integration**: Native Spring Boot autoconfiguration

**Strengths**:
- ✅ Official Spring project (trusted brand)
- ✅ Excellent Spring Boot integration (autoconfiguration, properties)
- ✅ Spring Data-like abstraction (familiar to Spring developers)
- ✅ Built-in observability (Micrometer metrics/traces)
- ✅ Cascading fallback patterns
- ✅ Good documentation and simplicity

**Weaknesses**:
- ⚠️ Spring Boot required (not usable in non-Spring projects)
- ⚠️ Limited provider support (6 vs LangChain4j's 20+)
- ⚠️ Missing providers: VLLM, BentoML, RayServe, HuggingFace Inference
- ⚠️ RAG/vector support weaker than LangChain4j
- ⚠️ Not yet 1.0 (API stability concerns)

**Target Audience**: Spring Boot shops building LLM-powered applications

**Pricing**: Free (open-source, Apache 2.0)

**Market Position**: Dominant in Spring ecosystem, growing

---

### 2.3 Xef.ai

**Overview**: Modern AI library for Kotlin and Scala, bringing LLMs and image generation to JVM applications.

**Key Stats**:
- **Release**: Active development (2025)
- **Languages**: Kotlin and Scala (JVM-native)
- **Providers**: OpenAI, Anthropic, Cohere, others
- **Maintainer**: Xebia/community
- **Philosophy**: Idiomatic interfaces per language

**Strengths**:
- ✅ Kotlin-first design (coroutines, Flow)
- ✅ Scala functional programming support
- ✅ Idiomatic per-language APIs
- ✅ Modern JVM approach (not Java-centric)
- ✅ Image generation support

**Weaknesses**:
- ⚠️ No Java support (Kotlin/Scala only)
- ⚠️ Limited provider count vs LangChain4j
- ⚠️ Missing self-hosted providers (VLLM, BentoML, RayServe)
- ⚠️ No production patterns (circuit breaker, caching)
- ⚠️ Smaller community than LangChain4j/Spring AI

**Target Audience**: Kotlin and Scala developers building AI applications

**Pricing**: Free (open-source)

**Market Position**: Growing in Kotlin/Scala ecosystem

**Relevance**: Direct competitor for JVM-wide library (Kotlin + Scala)

---

### 2.4 LiteLLM (Python - Reference)

**Overview**: Python library/proxy for calling 100+ LLM APIs using OpenAI format.

**Key Stats**:
- **Language**: Python (not Java, but instructive comparison)
- **Providers**: 100+ (most comprehensive)
- **Format**: OpenAI Chat Completions API standard

**Strengths**:
- ✅ Massive provider support (100+)
- ✅ Unified API format (OpenAI standard)
- ✅ Proxy server mode (language-agnostic via HTTP)

**Weaknesses**:
- ⚠️ Python-only (no Java library)
- ⚠️ Requires separate proxy deployment for Java use

**Market Position**: Python ecosystem leader

**Relevance to Java**: Proves market demand for multi-provider abstraction

---

### 2.4 Azure OpenAI SDK (Java)

**Overview**: Microsoft's official Java SDK for Azure OpenAI Service.

**Strengths**:
- ✅ Enterprise support (Microsoft backing)
- ✅ Production-grade (used in Azure services)

**Weaknesses**:
- ⚠️ Azure-only (vendor lock-in)
- ⚠️ No multi-provider abstraction

**Market Position**: Niche (Azure customers only)

---

## 3. Feature Comparison Matrix

| Feature | **LangChain4j** | **Spring AI** | **Xef.ai** | **inference-engine-jvm** (Ours) |
|---------|-----------------|---------------|------------|------------------------------|
| **Language Support** |
| Java | ✅ | ✅ | ❌ | ✅ **Core** |
| Kotlin | ✅ (via Java) | ✅ (via Java) | ✅ **Native** | ✅ **Idiomatic extensions** |
| Scala | ⚠️ (via Java) | ⚠️ (via Java) | ✅ **Native** | ✅ **Idiomatic extensions** |
| **Provider Support** |
| OpenAI | ✅ | ✅ | ✅ | ✅ |
| Anthropic Claude | ✅ | ✅ | ✅ | ✅ |
| Google Gemini | ✅ | ✅ | ⚠️ | ❌ (roadmap) |
| Azure OpenAI | ✅ | ✅ | ❌ | ❌ (roadmap) |
| AWS Bedrock | ✅ | ✅ | ❌ | ❌ (roadmap) |
| HuggingFace Inference | ✅ | ❌ | ⚠️ | ✅ |
| Ollama (local) | ✅ | ✅ | ⚠️ | ✅ |
| VLLM | ❌ | ❌ | ❌ | ✅ **Unique** |
| BentoML | ❌ | ❌ | ❌ | ✅ **Unique** |
| RayServe | ❌ | ❌ | ❌ | ✅ **Unique** |
| Text Generation Inference | ❌ | ❌ | ❌ | ✅ **Unique** |
| **Total Providers** | 20+ | 6 | ~5 | 8 (4 unique) |
| **Framework Requirements** |
| Spring Boot required? | ❌ (optional) | ✅ (required) | ❌ | ❌ (optional) |
| Standalone usage | ⚠️ (complex) | ❌ | ✅ | ✅ **Advantage** |
| Framework-free | ⚠️ | ❌ | ✅ | ✅ **Advantage** |
| **Enterprise Patterns** |
| Circuit breaker | ❌ | ❌ | ❌ | ✅ (Resilience4j) **Unique** |
| Redis caching | ❌ | ❌ | ❌ | ✅ **Unique** |
| Streaming support | ✅ | ✅ | ✅ | ✅ |
| Retry logic | ⚠️ (manual) | ⚠️ (manual) | ✅ (built-in) **Advantage** |
| Fallback providers | ⚠️ | ✅ | ✅ (decorator) |
| **Advanced Features** |
| RAG support | ✅ | ⚠️ | ❌ (out of scope) |
| Vector databases | ✅ (30+) | ⚠️ (limited) | ❌ (out of scope) |
| Agent framework | ✅ | ⚠️ | ❌ (out of scope) |
| Tool calling / MCP | ✅ | ✅ | ❌ (out of scope) |
| **Developer Experience** |
| Learning curve | ⚠️ High | ✅ Low | ✅ Low |
| Documentation | ✅ | ✅ | 🚧 (TBD) |
| Examples | ✅ | ✅ | 🚧 (TBD) |
| Community size | ✅ Large | ✅ Growing | ❌ None (new) |
| **Integration** |
| Spring Boot | ✅ | ✅ | ✅ (optional) |
| Quarkus | ✅ | ❌ | ✅ (planned) |
| Micronaut | ⚠️ | ❌ | ✅ (planned) |
| **Production Readiness** |
| Enterprise adoption | ✅ (Microsoft) | ✅ (VMware) | 🚧 (PHD Systems) |
| 1.0 release | ✅ (May 2025) | ❌ (pre-1.0) | ❌ (planned) |
| Observability | ⚠️ | ✅ (Micrometer) | ✅ (Micrometer) |
| **Licensing** |
| License | Apache 2.0 | Apache 2.0 | Apache 2.0 |
| Commercial support | ❌ | ⚠️ (VMware) | 🚧 (PHD Systems) |

### Legend
- ✅ Full support
- ⚠️ Partial support / limitations
- ❌ Not supported
- 🚧 Planned / in development

---

## 4. Differentiation Strategy

### Our Unique Value Propositions

#### 1. **Lightweight, Framework-Optional** ✅

**Problem**: LangChain4j and Spring AI force framework adoption
- LangChain4j: Heavy ecosystem (agents, RAG, chains)
- Spring AI: Spring Boot required

**Our Solution**: Pure provider abstraction, Spring Boot integration optional
```java
// Standalone usage (no Spring)
LLMProvider provider = new AnthropicProvider(apiKey, model);
String response = provider.generate("Hello");

// Spring Boot usage (autoconfiguration)
@Autowired LLMProvider provider;
```

**Benefit**: Use in any Java project (Spring, Quarkus, Micronaut, plain Java)

---

#### 2. **Production-Grade Resilience Patterns** ✅

**Problem**: Competitors assume happy-path LLM calls

**Our Solution**: Circuit breaker, caching, retry built-in
```java
LLMProvider resilient = new ResilientLLMProvider(
    new CachedLLMProvider(
        new AnthropicProvider(...)
    )
);
```

**Benefit**: Production-ready from day 1, no boilerplate

---

#### 3. **Self-Hosted LLM Support** ✅

**Problem**: LangChain4j/Spring AI focus on commercial APIs (OpenAI, Anthropic)

**Our Solution**: First-class support for open-source deployment platforms
- VLLM (high-throughput inference)
- BentoML (model serving)
- RayServe (scalable deployments)
- Text Generation Inference (HuggingFace)

**Benefit**: Cost savings (self-hosted), data privacy (on-prem)

**Use case**: Enterprise with Llama 3.1 70B deployed on VLLM
```java
LLMProvider vllm = new VLLMProvider(
    "http://internal-llm-cluster:8000",
    "meta-llama/Meta-Llama-3.1-70B"
);
```

---

#### 4. **Simple, Focused API** ✅

**Problem**: LangChain4j has complex API (chains, agents, prompts)

**Our Solution**: Single interface, clean abstractions
```java
public interface LLMProvider {
    String generate(String prompt);
    Flux<String> generateStreaming(String prompt);
}
```

**Benefit**: 5-minute learning curve, minimal boilerplate

---

### Competitive Positioning Matrix

```
                    High Framework Integration
                              │
                              │
          Spring AI ┌─────────┼─────────┐
                    │         │         │
                    │         │         │
                    │         │         │
Limited  ───────────┼─────────┼─────────┼─────────  Comprehensive
Providers           │         │         │           Providers
                    │         │  Our    │
                    │         │  Library│  LangChain4j
                    │         │    ✅   │
                    └─────────┼─────────┘
                              │
                    Low Framework Integration
```

**Our Position**: High provider diversity, low framework coupling

---

## 5. Target Audience

### Primary Audience

**Persona 1: Enterprise Java Architect**
- **Profile**: Building LLM-powered features in existing Java applications
- **Pain**: Doesn't want to adopt LangChain4j framework, Spring AI requires Spring Boot migration
- **Need**: Lightweight provider abstraction, works with any framework
- **Value**: Drop-in integration, minimal dependencies

**Persona 2: Platform Engineer**
- **Profile**: Managing self-hosted LLM infrastructure (VLLM, BentoML)
- **Pain**: No Java libraries support VLLM/BentoML, manual HTTP clients required
- **Need**: First-class support for open-source LLM platforms
- **Value**: Cost savings (avoid OpenAI API costs), data privacy

**Persona 3: DevOps / SRE**
- **Profile**: Ensuring production reliability of LLM integrations
- **Pain**: LLM APIs fail, no circuit breaker or caching patterns
- **Need**: Resilience patterns (circuit breaker, retry, fallback)
- **Value**: Reduced outages, lower API costs (caching)

### Secondary Audience

**Persona 4: Spring Boot Developer (migrating from Spring AI)**
- **Profile**: Using Spring AI, frustrated by limited providers
- **Pain**: Spring AI doesn't support VLLM, HuggingFace Inference
- **Need**: Spring Boot autoconfiguration + more providers
- **Value**: Familiar DX, expanded provider support

**Persona 5: Startup CTO**
- **Profile**: Building MVP with LLM features, budget-conscious
- **Pain**: OpenAI API costs too high, needs self-hosted option
- **Need**: Support for Ollama (dev) → VLLM (production) migration
- **Value**: Cost control, no vendor lock-in

---

## 6. Positioning Statement

### Tagline
**"Production-ready LLM provider infrastructure for the JVM - Java, Kotlin, Scala"**

### Full Positioning Statement

**For** JVM architects and platform engineers building LLM-powered applications in Java, Kotlin, or Scala,

**Who** need reliable, framework-agnostic provider abstraction with idiomatic APIs and support for self-hosted LLMs,

**inference-engine-jvm** is an open-source library

**That** provides 8+ provider integrations with built-in circuit breakers, caching, streaming, and language-specific extensions,

**Unlike** LangChain4j (Java-centric, framework-heavy), Spring AI (Spring Boot-only), and Xef.ai (no Java support, no production patterns),

**Our library** supports Java (core) + Kotlin (coroutines) + Scala (functional), works standalone or with any framework, includes first-class support for VLLM/BentoML/RayServe, and embeds production resilience patterns.

---

### Messaging Framework

| Audience | Message | Proof Point |
|----------|---------|-------------|
| **Enterprise Architects** | "Integrate LLMs without framework lock-in" | Supports Spring, Quarkus, Micronaut, standalone |
| **Platform Engineers** | "First-class support for self-hosted LLMs" | Only library with VLLM, BentoML, RayServe |
| **DevOps / SRE** | "Production-ready resilience out of the box" | Built-in circuit breaker, caching, retry |
| **Spring Developers** | "More providers than Spring AI, familiar DX" | 8 providers vs 6, Spring Boot autoconfiguration |
| **Startup CTOs** | "Control costs with self-hosted LLMs" | Ollama (dev) → VLLM (production) path |

---

## 7. Go-to-Market Strategy

### Phase 1: Launch (Month 1)

**Goals**:
- Establish credibility
- Early adopter feedback
- Initial GitHub stars (target: 50+)

**Tactics**:
1. **Documentation Launch**
   - README.md with quick start
   - Provider-specific guides (8 providers)
   - Example projects (Spring Boot, standalone)

2. **Community Announcements**
   - Reddit r/java post: "New library: inference-engine-jvm"
   - HackerNews Show HN: "inference-engine-jvm - Provider abstraction for Java"
   - Java Weekly newsletter submission

3. **Content Marketing**
   - Blog post: "Why we built inference-engine-jvm (and why you should use it)"
   - Comparison article: "LangChain4j vs Spring AI vs inference-engine-jvm"

**Success Metrics**:
- 50+ GitHub stars
- 3+ external contributors
- 100+ Maven Central downloads

---

### Phase 2: Growth (Months 2-6)

**Goals**:
- Expand provider support
- Grow community
- Enterprise adoption

**Tactics**:
1. **Provider Expansion**
   - Add Google Gemini (close gap with LangChain4j)
   - Add AWS Bedrock (enterprise demand)
   - Add Azure OpenAI (Microsoft customers)

2. **Enterprise Outreach**
   - Case study: PHD Systems internal usage
   - Java User Group presentations
   - Conference talks (JavaOne, Devoxx)

3. **Integration Guides**
   - Spring Boot starter
   - Quarkus extension
   - Micronaut integration

**Success Metrics**:
- 500+ GitHub stars
- 5+ enterprise users
- 2,000+ Maven Central downloads/month

---

### Phase 3: Ecosystem (Months 6-12)

**Goals**:
- Ecosystem integrations
- Community-driven development
- Industry recognition

**Tactics**:
1. **Ecosystem Partnerships**
   - LangChain4j collaboration (provider sharing)
   - Spring AI compatibility layer
   - Quarkus official extension

2. **Advanced Features**
   - OpenTelemetry tracing
   - Cost tracking per provider
   - Multi-region failover

3. **Industry Recognition**
   - Submit to Apache Incubator (if traction exists)
   - JavaOne presentation acceptance
   - Baeldung tutorial feature

**Success Metrics**:
- 2,000+ GitHub stars
- 10+ enterprise case studies
- 10,000+ Maven Central downloads/month

---

## 8. References

### Competitor Research

1. **LangChain4j**
   - GitHub: https://github.com/langchain4j/langchain4j
   - Documentation: https://docs.langchain4j.dev/
   - Microsoft Partnership: https://devblogs.microsoft.com/java/microsoft-and-langchain4j-a-partnership-for-secure-enterprise-grade-java-ai-applications/

2. **Spring AI**
   - Documentation: https://docs.spring.io/spring-ai/reference/
   - Baeldung Guide: https://www.baeldung.com/spring-ai-configure-multiple-llms

3. **LiteLLM** (Python reference)
   - GitHub: https://github.com/BerriAI/litellm
   - Documentation: https://docs.litellm.ai/docs/providers

### Market Analysis

4. **Evolution of Java Ecosystem for Integrating AI** (Inside.java, Jan 2025)
   - URL: https://inside.java/2025/01/29/evolution-of-java-ecosystem-for-integrating-ai/

5. **Spring AI vs LangChain4j Comparison** (HackerNoon)
   - URL: https://hackernoon.com/springai-vs-langchain4j-the-real-world-llm-battle-for-java-devs

6. **Java News Roundup: LangChain4j 1.0** (InfoQ, May 2025)
   - URL: https://www.infoq.com/news/2025/05/java-news-roundup-may12-2025/

### Technology Standards

7. **OpenAI Chat Completions API**
   - URL: https://platform.openai.com/docs/api-reference/chat

8. **Resilience4j Circuit Breaker**
   - GitHub: https://github.com/resilience4j/resilience4j

---

## Appendices

### A. Detailed Provider Comparison

| Provider | LangChain4j | Spring AI | Ours | Notes |
|----------|-------------|-----------|------|-------|
| **Commercial APIs** |
| OpenAI GPT | ✅ | ✅ | ✅ | All support |
| Anthropic Claude | ✅ | ✅ | ✅ | All support |
| Google Gemini | ✅ | ✅ | 🚧 | Roadmap |
| AWS Bedrock | ✅ | ✅ | 🚧 | Roadmap |
| Azure OpenAI | ✅ | ✅ | 🚧 | Roadmap |
| Cohere | ✅ | ❌ | ❌ | Low priority |
| Mistral AI | ✅ | ❌ | ❌ | Low priority |
| **Open Source / Self-Hosted** |
| Ollama | ✅ | ✅ | ✅ | All support |
| HuggingFace Inference | ✅ | ❌ | ✅ | **Our advantage** |
| VLLM | ❌ | ❌ | ✅ | **Unique to us** |
| BentoML | ❌ | ❌ | ✅ | **Unique to us** |
| RayServe | ❌ | ❌ | ✅ | **Unique to us** |
| Text Gen Inference | ❌ | ❌ | ✅ | **Unique to us** |
| LocalAI | ⚠️ | ❌ | 🚧 | Roadmap |

**Key**: ✅ Supported | ⚠️ Partial | ❌ Not supported | 🚧 Planned

---

### B. SWOT Analysis

#### Strengths
- ✅ Unique provider support (VLLM, BentoML, RayServe)
- ✅ Production-ready patterns (circuit breaker, caching)
- ✅ Framework-agnostic (works anywhere)
- ✅ Clean, simple API (low learning curve)
- ✅ PHD Systems internal adoption (proven in production)

#### Weaknesses
- ⚠️ No community (yet)
- ⚠️ No brand recognition
- ⚠️ Missing advanced features (RAG, vector DBs)
- ⚠️ Smaller provider count than LangChain4j (8 vs 20+)

#### Opportunities
- ✅ Self-hosted LLM trend (cost savings, privacy)
- ✅ Enterprise Java shops avoiding framework adoption
- ✅ Spring AI frustration (limited providers)
- ✅ LangChain4j complexity backlash

#### Threats
- ⚠️ LangChain4j adds VLLM support (copies our differentiator)
- ⚠️ Spring AI reaches 1.0, becomes dominant
- ⚠️ LiteLLM adds Java SDK (direct competitor)
- ⚠️ OpenAI SDK becomes multi-provider standard

---

### C. FAQ: Why Another Library?

**Q: Why not just use LangChain4j?**

A: LangChain4j is excellent for RAG/agent use cases, but forces framework adoption. If you just need multi-provider LLM calls without RAG, LangChain4j is overkill (30+ dependencies, complex API).

**Q: Why not just use Spring AI?**

A: Spring AI requires Spring Boot. If you're on Quarkus, Micronaut, or plain Java, Spring AI won't work. Also, Spring AI lacks VLLM/BentoML support (critical for self-hosted).

**Q: Can I use this with LangChain4j or Spring AI?**

A: Yes! You can use our provider layer as a lower-level abstraction:
```java
LLMProvider provider = new VLLMProvider(...);
// Wrap in LangChain4j ChatLanguageModel adapter
ChatLanguageModel langchainModel = new LLMProviderAdapter(provider);
```

**Q: What if I need RAG or vector databases?**

A: Use LangChain4j or Spring AI for RAG. Our library focuses on provider abstraction, not higher-level patterns.

**Q: Is this just a wrapper around HTTP clients?**

A: Yes, but with critical production patterns: circuit breakers, caching, retry logic, streaming, fallback. Most teams rewrite this boilerplate for every project.

---

**Document End**

*Last Updated: 2025-10-18*
*Version: 1.0*
*Next Review: After extraction decision*
