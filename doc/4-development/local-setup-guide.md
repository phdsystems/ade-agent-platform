# Local Setup Guide - Role Manager App

**Quick start guide for setting up and running the Role Manager App locally**

**Last Updated:** 2025-10-17
**Version:** 1.0.0

---

## TL;DR

**Quick setup**: Clone repo → Install Java 21 & Gradle → Configure Ollama → Run `./gradlew bootRun`. **Default config**: Uses Ollama (qwen3:0.6b) on ade-srv:11434, no API keys needed. **Test**: `list-roles`, `execute --role "Developer" --task "Hello"`. **Alternative**: Set ANTHROPIC_API_KEY or OPENAI_API_KEY for cloud LLMs.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Configuration Options](#configuration-options)
- [Testing the Application](#testing-the-application)
- [Troubleshooting](#troubleshooting)
- [Next Steps](#next-steps)

---

## Prerequisites

### Required Software

1. **Java 21+**
   ```bash
   # Check version
   java -version

   # Should show: openjdk version "21" or higher
   ```

   **Install on Ubuntu/Debian:**
   ```bash
   sudo apt update
   sudo apt install openjdk-21-jdk
   ```

   **Install on macOS:**
   ```bash
   brew install openjdk@21
   ```

   **Install on Windows:**
   - Download from [Adoptium](https://adoptium.net/)
   - Or use: `winget install EclipseAdoptium.Temurin.21.JDK`

2. **Gradle** (optional - wrapper included)
   ```bash
   # Check if Gradle is installed
   gradle --version
   ```

   Not required if using `./gradlew` (Gradle wrapper).

3. **Git**
   ```bash
   git --version
   ```

### Optional but Recommended

**Local Inference Engine** (for free local LLM)
- **Default**: Ollama server at `ade-srv:11434` with `qwen3:0.6b` model
- **Alternatives**: vLLM, Text Generation Inference (TGI), Ray Serve, BentoML
- See [Inference Engine Options](#inference-engine-options) for detailed comparison and setup

---

## Quick Start

### 1. Clone the Repository

```bash
cd /path/to/your/projects
git clone https://github.com/phdsystems/software-engineer.git
cd software-engineer/role-manager-app
```

### 2. Build the Application

```bash
# Linux/macOS
./gradlew clean build -x test

# Windows
.\gradlew.bat clean build -x test
```

**Expected output:**
```
BUILD SUCCESSFUL in 30s
```

### 3. Run the Application

```bash
# Linux/macOS
./gradlew bootRun

# Windows
.\gradlew.bat bootRun
```

**Expected output:**
```
2025-10-17 12:00:00 - Starting Role Manager Application...
2025-10-17 12:00:05 - Initialized AnthropicProvider with model: claude-3-5-sonnet-20241022
2025-10-17 12:00:05 - Initialized OpenAIProvider with model: gpt-4-turbo-preview
2025-10-17 12:00:05 - Initialized OllamaProvider with model: qwen3:0.6b at http://ade-srv:11434
2025-10-17 12:00:10 - Registered agent: Software Developer
2025-10-17 12:00:10 - Registered agent: QA Engineer
2025-10-17 12:00:10 - Registered agent: Security Engineer
2025-10-17 12:00:10 - Registered agent: Engineering Manager
2025-10-17 12:00:10 - Registry validation: 4 agents registered
2025-10-17 12:00:15 - Role Manager Application started successfully
```

### 4. Test with CLI

In the Spring Shell prompt:

```bash
shell:> list-roles
Available Roles (4):
========================================
  - Engineering Manager
  - QA Engineer
  - Security Engineer
  - Software Developer

shell:> execute --role "Software Developer" --task "Say hello and introduce yourself"
```

---

## Detailed Setup

### Step 1: Verify Prerequisites

```bash
# Check Java version
java -version
# Must be 21 or higher

# Check Git
git --version

# Optional: Check Gradle
gradle --version
```

### Step 2: Clone and Navigate

```bash
# Clone the repository
git clone https://github.com/phdsystems/software-engineer.git

# Navigate to the app directory
cd software-engineer/role-manager-app

# Verify you're in the right place
ls -la
# Should see: build.gradle.kts, src/, config/, gradle/
```

### Step 3: Understand the Default Configuration

The app is pre-configured to use **Ollama** as the primary LLM provider:

**File:** `src/main/resources/application.yml`
```yaml
llm:
  primary-provider: ollama

  ollama:
    base-url: http://ade-srv:11434
    model: qwen3:0.6b
```

**This means:**
- No API keys required
- Uses local Qwen 3 0.6b model on ade-srv
- Free to use (no cloud costs)
- Falls back to OpenAI/Anthropic if Ollama is unavailable

### Step 4: Build the Application

```bash
# Clean build (recommended for first time)
./gradlew clean build -x test

# Build should take 1-3 minutes
```

**What happens:**
1. Downloads Gradle 9.1.0 (if not cached)
2. Downloads dependencies (Spring Boot, Jackson, etc.)
3. Compiles Java code
4. Creates executable JAR in `build/libs/`

**If build fails:**
- Check Java version: `java -version` (must be 21+)
- Clear Gradle cache: `rm -rf ~/.gradle/caches`
- Try again with `--refresh-dependencies`

### Step 5: Run the Application

**Option A: Using Gradle (Development)**
```bash
./gradlew bootRun
```

**Option B: Using the JAR (Production)**
```bash
# Build the JAR first
./gradlew bootJar

# Run the JAR
java -jar build/libs/role-manager-app-0.1.0-SNAPSHOT.jar
```

**Option C: With Custom Configuration**
```bash
# Override Ollama URL
OLLAMA_BASE_URL=http://localhost:11434 ./gradlew bootRun

# Use Anthropic instead
export ANTHROPIC_API_KEY="your-key"
./gradlew bootRun
```

### Step 6: Verify the Application Started

**Check the logs for:**
```
✅ "Role Manager Application started successfully"
✅ "Initialized OllamaProvider with model: qwen3:0.6b"
✅ "4 agents registered"
✅ Server running on port 8080
```

**Health check:**
```bash
curl http://localhost:8080/actuator/health

# Expected response:
{"status":"UP"}
```

---

## Inference Engine Options

The Role Manager App supports both **cloud-based APIs** and **local inference engines**. This section compares available inference engines and provides setup instructions.

### Comparison Table

| Engine | Type | Speed | Memory | Best For | Difficulty |
|--------|------|-------|--------|----------|------------|
| **Ollama** | Local | Medium | 2-8 GB | Easy setup, development | ⭐ Easy |
| **vLLM** | Local | Very Fast | 4-16 GB | Production, high throughput | ⭐⭐ Moderate |
| **TGI** | Local | Fast | 4-16 GB | HuggingFace models, production | ⭐⭐ Moderate |
| **Ray Serve** | Local | Fast | 8-32 GB | Multi-model, distributed | ⭐⭐⭐ Advanced |
| **BentoML** | Local | Medium | 4-16 GB | MLOps, auto-scaling | ⭐⭐⭐ Advanced |
| **Anthropic** | Cloud | Fast | N/A | Latest models, no setup | ⭐ Easy |
| **OpenAI** | Cloud | Fast | N/A | GPT-4, reliable | ⭐ Easy |
| **HuggingFace** | Cloud | Medium | N/A | Open models, hosted | ⭐ Easy |

### Detailed Comparison

#### Ollama (Default)
**What it is:** User-friendly local inference engine optimized for ease of use.

**Pros:**
- ✅ Easiest setup (single binary, one command)
- ✅ Excellent model library (Llama, Mistral, Qwen, CodeLlama, etc.)
- ✅ Automatic model downloads
- ✅ Good performance for development
- ✅ Low resource requirements (runs on laptops)

**Cons:**
- ❌ Not the fastest inference
- ❌ Limited batch processing
- ❌ No built-in scaling

**When to use:**
- Development and testing
- Single-user applications
- Quick prototyping
- Resource-constrained environments

**Setup:** See [Option 1: Ollama](#option-1-ollama-default) below

---

#### vLLM
**What it is:** High-performance inference engine with PagedAttention for efficient memory management.

**Pros:**
- ✅ High-throughput inference optimized for production workloads
- ✅ Efficient memory usage (PagedAttention algorithm)
- ✅ Continuous batching for concurrent requests
- ✅ OpenAI-compatible API
- ✅ Production-ready

**Cons:**
- ❌ More complex setup
- ❌ GPU required for best performance
- ❌ Higher memory baseline

**When to use:**
- Production deployments
- High request volume (>100 req/min)
- Need maximum throughput
- Have GPU resources available

**Performance**: vLLM uses PagedAttention and continuous batching to optimize throughput. See [vLLM benchmarks](https://github.com/vllm-project/vllm) for performance comparisons.

**Setup:** See [Option 5: vLLM](#option-5-vllm) below

---

#### Text Generation Inference (TGI)
**What it is:** HuggingFace's production-grade inference server.

**Pros:**
- ✅ Excellent HuggingFace model support
- ✅ Tensor parallelism (multi-GPU)
- ✅ Streaming support
- ✅ Flash Attention integration
- ✅ Good documentation

**Cons:**
- ❌ Requires Docker
- ❌ GPU recommended
- ❌ Complex configuration

**When to use:**
- Using HuggingFace models
- Need multi-GPU support
- Production with streaming
- Enterprise deployments

**Setup:** See [Option 6: Text Generation Inference (TGI)](#option-6-text-generation-inference-tgi) below

---

#### Ray Serve
**What it is:** Scalable ML model serving framework for distributed deployments.

**Pros:**
- ✅ Horizontal scaling
- ✅ Multi-model serving
- ✅ Advanced routing/load balancing
- ✅ Integrated with Ray ecosystem
- ✅ A/B testing support

**Cons:**
- ❌ Complex setup
- ❌ Steep learning curve
- ❌ Higher operational overhead

**When to use:**
- Large-scale production
- Multiple models simultaneously
- Need distributed inference
- Complex routing requirements

**Setup:** See [Option 7: Ray Serve](#option-7-ray-serve) below

---

#### BentoML
**What it is:** End-to-end ML serving platform with MLOps features.

**Pros:**
- ✅ Full MLOps lifecycle
- ✅ Auto-scaling
- ✅ Multiple frameworks (PyTorch, TensorFlow, etc.)
- ✅ Good monitoring/observability
- ✅ Cloud deployment support

**Cons:**
- ❌ More abstraction layers
- ❌ Requires learning BentoML framework
- ❌ Overhead for simple use cases

**When to use:**
- Need full MLOps platform
- Managing model lifecycle
- Multi-framework support
- Cloud-native deployments

**Setup:** See [Option 8: BentoML](#option-8-bentoml) below

---

### Quick Decision Guide

**Choose Ollama if:**
- 🎯 You're just getting started
- 🎯 Development/testing environment
- 🎯 Want simplest setup
- 🎯 Limited resources (laptop, small VM)

**Choose vLLM if:**
- 🎯 Need maximum speed
- 🎯 Production deployment
- 🎯 High request volume
- 🎯 Have GPU available

**Choose TGI if:**
- 🎯 Using HuggingFace models
- 🎯 Need multi-GPU support
- 🎯 Want streaming responses
- 🎯 Enterprise requirements

**Choose Ray Serve if:**
- 🎯 Large-scale distributed system
- 🎯 Multiple models
- 🎯 Complex routing needs
- 🎯 Already using Ray ecosystem

**Choose BentoML if:**
- 🎯 Need full MLOps platform
- 🎯 Managing model lifecycle
- 🎯 Multi-framework support
- 🎯 Cloud deployment integration

**Choose Cloud API (Anthropic/OpenAI) if:**
- 🎯 Want zero infrastructure
- 🎯 Need latest/proprietary models
- 🎯 Low to medium request volume
- 🎯 Cost acceptable

---

## Configuration Options

### Option 1: Ollama (Default)

**Current configuration** - already set up:
```yaml
llm:
  primary-provider: ollama
  ollama:
    base-url: http://ade-srv:11434
    model: qwen3:0.6b
```

**No changes needed!** Just run the app.

**To install Ollama locally:**
```bash
# Linux
curl -fsSL https://ollama.com/install.sh | sh

# macOS
brew install ollama

# Windows
# Download from https://ollama.com/download
```

**Start Ollama and pull a model:**
```bash
# Start server (runs on port 11434)
ollama serve

# In another terminal, pull a model
ollama pull qwen2.5:0.5b     # Smallest (0.5B params)
ollama pull qwen2.5:3b       # Balanced (3B params)
ollama pull llama3.2:3b      # Meta's Llama 3.2
ollama pull mistral:7b       # Good for code

# List downloaded models
ollama list
```

**Update application.yml to use local Ollama:**
```yaml
llm:
  primary-provider: ollama
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:0.5b
```

### Option 2: Use Anthropic Claude (Cloud)

**Set environment variable:**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

**Update application.yml:**
```yaml
llm:
  primary-provider: anthropic  # Change from ollama
```

**Or override at runtime:**
```bash
ANTHROPIC_API_KEY="sk-ant-..." ./gradlew bootRun
```

### Option 3: Use OpenAI GPT (Cloud)

**Set environment variable:**
```bash
export OPENAI_API_KEY="sk-..."
```

**Update application.yml:**
```yaml
llm:
  primary-provider: openai  # Change from ollama
```

### Option 4: HuggingFace Inference API (Cloud)

**Set environment variable:**
```bash
export HUGGINGFACE_API_KEY="hf_..."
```

**Update application.yml:**
```yaml
llm:
  primary-provider: huggingface
  huggingface:
    api-key: ${HUGGINGFACE_API_KEY}
    model: meta-llama/Llama-3.2-3B-Instruct
```

**Note:** Free tier has limited requests. See [HuggingFace Setup Guide](guide/huggingface-setup.md).

---

### Option 5: vLLM

**What you need:**
- GPU with 8+ GB VRAM (CUDA 11.8+)
- Python 3.8-3.11

**Install vLLM:**
```bash
# Create virtual environment
python -m venv vllm-env
source vllm-env/bin/activate

# Install vLLM (requires CUDA)
pip install vllm

# Or use Docker
docker pull vllm/vllm-openai:latest
```

**Start vLLM server:**
```bash
# Option 1: Direct Python
python -m vllm.entrypoints.openai.api_server \
  --model meta-llama/Llama-3.2-3B-Instruct \
  --dtype auto \
  --api-key dummy \
  --port 8000

# Option 2: Docker
docker run --gpus all \
  -p 8000:8000 \
  vllm/vllm-openai:latest \
  --model meta-llama/Llama-3.2-3B-Instruct \
  --dtype auto
```

**Configure Role Manager App:**

vLLM is now natively supported with its own provider:
```yaml
llm:
  primary-provider: vllm
  vllm:
    base-url: http://localhost:8000
    api-key: dummy  # vLLM doesn't require real key
    model: meta-llama/Llama-3.2-3B-Instruct
```

Or via environment variables:
```bash
export VLLM_BASE_URL=http://localhost:8000
export VLLM_API_KEY=dummy
./gradlew bootRun
```

**Test vLLM:**
```bash
curl http://localhost:8000/v1/models
curl -X POST http://localhost:8000/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3.2-3B-Instruct",
    "prompt": "Hello, world!",
    "max_tokens": 50
  }'
```

**Performance tips:**
- Use `--tensor-parallel-size` for multi-GPU
- Adjust `--max-model-len` based on VRAM
- Enable `--trust-remote-code` for custom models

---

### Option 6: Text Generation Inference (TGI)

**What you need:**
- Docker
- GPU with 8+ GB VRAM (recommended)

**Install and start TGI:**
```bash
# Pull TGI Docker image
docker pull ghcr.io/huggingface/text-generation-inference:latest

# Start TGI server
docker run --gpus all \
  -p 8080:80 \
  -v $HOME/.cache/huggingface:/data \
  ghcr.io/huggingface/text-generation-inference:latest \
  --model-id meta-llama/Llama-3.2-3B-Instruct \
  --num-shard 1 \
  --max-total-tokens 4096
```

**For CPU-only (slower):**
```bash
docker run \
  -p 8080:80 \
  -v $HOME/.cache/huggingface:/data \
  ghcr.io/huggingface/text-generation-inference:latest \
  --model-id meta-llama/Llama-3.2-3B-Instruct
```

**Configure Role Manager App:**

TGI is now natively supported with its own provider:
```yaml
llm:
  primary-provider: tgi
  tgi:
    base-url: http://localhost:8080
    api-key: dummy
    model: meta-llama/Llama-3.2-3B-Instruct
```

Or via environment variables:
```bash
export TGI_BASE_URL=http://localhost:8080
export TGI_API_KEY=dummy
./gradlew bootRun
```

**Test TGI:**
```bash
curl http://localhost:8080/health
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3.2-3B-Instruct",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 50
  }'
```

**Multi-GPU setup:**
```bash
# Use --num-shard for tensor parallelism
docker run --gpus all \
  -p 8080:80 \
  ghcr.io/huggingface/text-generation-inference:latest \
  --model-id meta-llama/Llama-3.2-70B-Instruct \
  --num-shard 4  # Distribute across 4 GPUs
```

---

### Option 7: Ray Serve

**What you need:**
- Python 3.8+
- Ray installed

**Install Ray Serve:**
```bash
pip install ray[serve] transformers torch
```

**Create Ray Serve deployment (save as `serve_llm.py`):**
```python
from ray import serve
from transformers import pipeline
import requests

@serve.deployment(num_replicas=2)
class LLMModel:
    def __init__(self):
        self.model = pipeline(
            "text-generation",
            model="meta-llama/Llama-3.2-3B-Instruct",
            device_map="auto"
        )

    def __call__(self, request):
        prompt = request.query_params.get("prompt")
        result = self.model(prompt, max_new_tokens=100)
        return {"text": result[0]["generated_text"]}

serve.run(LLMModel.bind(), route_prefix="/generate")
```

**Start Ray Serve:**
```bash
# Start Ray
ray start --head

# Deploy the model
python serve_llm.py
```

**Configure Role Manager App:**

Ray Serve is now natively supported:
```yaml
llm:
  primary-provider: rayserve
  rayserve:
    base-url: http://localhost:8000
    model: meta-llama/Llama-3.2-3B-Instruct
```

Or via environment variables:
```bash
export RAYSERVE_BASE_URL=http://localhost:8000
./gradlew bootRun
```

---

### Option 8: BentoML

**What you need:**
- Python 3.8+
- BentoML installed

**Install BentoML:**
```bash
pip install bentoml transformers torch
```

**Create BentoML service (save as `service.py`):**
```python
import bentoml
from transformers import pipeline

@bentoml.service(
    resources={"gpu": 1},
    traffic={"timeout": 300}
)
class LLMService:
    def __init__(self):
        self.model = pipeline(
            "text-generation",
            model="meta-llama/Llama-3.2-3B-Instruct"
        )

    @bentoml.api
    def generate(self, prompt: str) -> str:
        result = self.model(prompt, max_new_tokens=100)
        return result[0]["generated_text"]
```

**Build and serve:**
```bash
# Build BentoML service
bentoml build

# Serve locally
bentoml serve service:LLMService --port 3000
```

**Configure Role Manager App:**

BentoML is now natively supported:
```yaml
llm:
  primary-provider: bentoml
  bentoml:
    base-url: http://localhost:3000
    model: meta-llama/Llama-3.2-3B-Instruct
```

Or via environment variables:
```bash
export BENTOML_BASE_URL=http://localhost:3000
./gradlew bootRun
```

---

### Environment Variables Reference

| Variable | Purpose | Default |
|----------|---------|---------|
| `OLLAMA_BASE_URL` | Ollama server URL | `http://ade-srv:11434` |
| `VLLM_BASE_URL` | vLLM server URL | `http://localhost:8000` |
| `VLLM_API_KEY` | vLLM API key (usually dummy) | `dummy` |
| `TGI_BASE_URL` | Text Generation Inference server URL | `http://localhost:8080` |
| `TGI_API_KEY` | TGI API key (usually dummy) | `dummy` |
| `RAYSERVE_BASE_URL` | Ray Serve server URL | `http://localhost:8000` |
| `BENTOML_BASE_URL` | BentoML server URL | `http://localhost:3000` |
| `ANTHROPIC_API_KEY` | Anthropic API key | (none) |
| `OPENAI_API_KEY` | OpenAI API key | (none) |
| `HUGGINGFACE_API_KEY` | HuggingFace API key | (none) |

---

## Testing the Application

### CLI Testing

**1. List available roles:**
```bash
shell:> list-roles
```

**2. Get role information:**
```bash
shell:> describe-role "Software Developer"
```

**3. Execute a task:**
```bash
shell:> execute --role "Software Developer" --task "Review this code quality"
```

**4. Show all roles with capabilities:**
```bash
shell:> show-roles
```

### REST API Testing

**1. List roles (GET):**
```bash
curl http://localhost:8080/api/roles
```

**Expected response:**
```json
["Engineering Manager","QA Engineer","Security Engineer","Software Developer"]
```

**2. Get role info (GET):**
```bash
curl http://localhost:8080/api/roles/info
```

**3. Execute task (POST):**
```bash
curl -X POST http://localhost:8080/api/tasks/execute \
  -H "Content-Type: application/json" \
  -d '{
    "roleName": "Software Developer",
    "task": "Explain what this app does",
    "context": {}
  }'
```

**Expected response:**
```json
{
  "roleName": "Software Developer",
  "task": "Explain what this app does",
  "output": "This is a multi-agent AI system...",
  "usage": {
    "inputTokens": 150,
    "outputTokens": 200,
    "totalTokens": 350,
    "estimatedCost": 0.0
  },
  "timestamp": "2025-10-17T12:00:00Z",
  "durationMs": 2500,
  "success": true,
  "errorMessage": null
}
```

**4. Multi-agent execution (POST):**
```bash
curl -X POST http://localhost:8080/api/tasks/multi-agent \
  -H "Content-Type: application/json" \
  -d '{
    "roleNames": ["Software Developer", "QA Engineer"],
    "task": "Review code quality",
    "context": {}
  }'
```

### Health and Monitoring

**Health check:**
```bash
curl http://localhost:8080/actuator/health
```

**Metrics:**
```bash
curl http://localhost:8080/actuator/metrics
```

**Prometheus metrics:**
```bash
curl http://localhost:8080/actuator/prometheus
```

---

## Troubleshooting

### Issue: "Could not find or load main class GradleWrapperMain"

**Cause:** Corrupted gradle-wrapper.jar

**Solution:**
```bash
# Pull latest changes (contains fixed wrapper)
git pull origin feature/spring-boot-version

# Or download manually
curl -L https://raw.githubusercontent.com/gradle/gradle/v9.1.0/gradle/wrapper/gradle-wrapper.jar \
  -o gradle/wrapper/gradle-wrapper.jar
```

### Issue: "java.lang.UnsupportedClassVersionError"

**Cause:** Java version too old

**Solution:**
```bash
# Check version
java -version

# Must show 21 or higher
# Install Java 21 if needed
```

### Issue: Ollama connection refused

**Symptoms:**
```
Error calling Ollama API: Connection refused
```

**Solution:**
```bash
# Check if Ollama server is running
curl http://ade-srv:11434/api/tags

# If not accessible, update application.yml to use different provider:
llm:
  primary-provider: anthropic  # or openai
```

### Issue: No agents registered

**Symptoms:**
```
Registry validation: 0 agents registered
```

**Cause:** Agent YAML files not found

**Solution:**
```bash
# Check config directory exists
ls -la config/agents/

# Should see:
# developer.yaml
# qa.yaml
# security.yaml
# manager.yaml

# If missing, pull latest code
git pull origin feature/spring-boot-version
```

### Issue: Port 8080 already in use

**Symptoms:**
```
Port 8080 is already in use
```

**Solution:**
```bash
# Option 1: Stop other service on port 8080
lsof -ti:8080 | xargs kill -9

# Option 2: Use different port
SERVER_PORT=8081 ./gradlew bootRun

# Or update application.yml:
server:
  port: 8081
```

### Issue: Build fails with "Could not resolve dependencies"

**Solution:**
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches

# Retry build with refresh
./gradlew clean build -x test --refresh-dependencies
```

### Issue: Spring Shell commands not working

**Symptoms:**
- Commands like `list-roles` not recognized
- Prompt doesn't show `shell:>`

**Solution:**
```bash
# Ensure you're running the app, not just building
./gradlew bootRun

# Wait for "Application started successfully"
# Then interact with the shell prompt
```

---

## Next Steps

### 1. Add More Agents

Create new agent YAML files in `config/agents/`:

```yaml
# config/agents/devops.yaml
role: "DevOps Engineer"
description: "DevOps engineer specializing in CI/CD and infrastructure"
capabilities:
  - "Pipeline optimization"
  - "Infrastructure as code"
  - "Deployment automation"
temperature: 0.6
maxTokens: 4096
outputFormat: "technical"
promptTemplate: |
  You are an expert DevOps Engineer.
  Task: {task}
  Context: {context}
```

Restart the app to load the new agent.

### 2. Customize Agents

Edit existing YAML files in `config/agents/` to:
- Change temperature (0.0-1.0)
- Modify prompt templates
- Adjust max tokens
- Update capabilities

### 3. Switch LLM Providers

Update `application.yml`:
```yaml
llm:
  primary-provider: anthropic  # Change to: anthropic, openai, or ollama
```

### 4. Deploy to Production

See [Deployment Guide](../6-deployment/deployment-guide.md) for:
- Docker deployment
- Environment configuration
- Production best practices

### 5. Run Tests

```bash
# Unit tests only
./gradlew test

# All tests (including integration)
./gradlew build

# Generate coverage report
./gradlew jacocoTestReport
```

### 6. Enable Configuration Cache

Speed up builds:
```bash
# Add to gradle.properties
org.gradle.configuration-cache=true

# Or use CLI flag
./gradlew bootRun --configuration-cache
```

---

## Additional Resources

### Documentation

- **[Developer Guide](developer-guide.md)** - Full development workflow
- **[API Design](../3-design/api-design.md)** - REST API specification
- **[Architecture](../3-design/architecture.md)** - System design and patterns
- **[Deployment Guide](../6-deployment/deployment-guide.md)** - Production deployment

### External Links

- **[Spring Boot Documentation](https://spring.io/projects/spring-boot)**
- **[Spring Shell Documentation](https://spring.io/projects/spring-shell)**
- **[Ollama Documentation](https://ollama.ai/)**
- **[Anthropic API Docs](https://docs.anthropic.com/)**
- **[OpenAI API Docs](https://platform.openai.com/docs/)**

### Getting Help

- **GitHub Issues:** [Report bugs or request features](https://github.com/phdsystems/software-engineer/issues)
- **Developer Guide:** Check troubleshooting section
- **Community:** Join discussions in project repository

---

## Summary

You now have the Role Manager App running locally! Key points:

✅ **Default setup**: Uses Ollama (qwen3:0.6b) on ade-srv - no API keys needed
✅ **Quick start**: `./gradlew bootRun` → Test with CLI or REST API
✅ **Flexible**: Switch LLM providers via environment variables or config
✅ **Extensible**: Add new agents by creating YAML files
✅ **Production-ready**: Build JAR for deployment

Happy coding! 🚀

---

**Last Updated:** 2025-10-17
**Version:** 1.0.0
**Maintainer:** Development Team
