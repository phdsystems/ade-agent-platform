# HuggingFace Provider Setup Guide

**Purpose**: Configure HuggingFace Inference API for integration testing
**SDLC Phase**: Phase 4 - Development (Testing Setup)
**Last Updated**: 2025-10-18

---

## TL;DR

**⚠️ LIMITATION DISCOVERED**: HuggingFace free tier Inference API does NOT support generative text models (Qwen, Phi, Gemma, etc.). Free tier only supports task-specific models (summarization, classification). **For integration tests, use localhost Ollama with qwen2.5:0.5b instead**. See doc/4-development/guide/integration-test-setup.md.

---

## ⚠️ Important Limitation (Discovered 2025-10-18)

**HuggingFace Free Tier Inference API does NOT support generative text models** needed for our use case.

**What Works** (Free Tier):
- ✅ Task-specific models: Summarization, classification, NER, translation
- ✅ Example: `facebook/bart-large-cnn` (summarization)

**What Doesn'Human: commit what you can. And let me know what else to do to run locally

---

## Why HuggingFace?

### Problem Solved

Remote Ollama on ade-srv has **Qwen3 model bugs** with `num_predict` parameter causing empty responses. Local Ollama requires manual setup on each developer machine.

### HuggingFace Advantages

- ✅ **Cloud-hosted** - No local setup required
- ✅ **Free tier** - No cost for integration testing workload
- ✅ **No bugs** - Bypasses Ollama `num_predict` issues
- ✅ **Fast** - Optimized inference infrastructure
- ✅ **Reliable** - Professional uptime and availability

### Trade-offs

- ⚠️ **Rate limits** - ~Few hundred requests/hour (fine for integration tests)
- ⚠️ **Internet required** - Not offline-capable
- ⚠️ **Cold starts** - First request may take ~20s for model loading

---

## Setup Instructions

### Step 1: Get HuggingFace API Key (Free)

1. **Create account** (if you don't have one):
   - Go to [https://huggingface.co/join](https://huggingface.co/join)
   - Sign up with email or GitHub

2. **Generate API token**:
   - Go to [https://huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)
   - Click **"New token"**
   - Name: `integration-testing` (or any name you prefer)
   - Role: **Read** (sufficient for Inference API)
   - Click **"Generate token"**
   - **Copy the token** (you won't see it again!)

3. **Verify token format**:
   ```
   hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```
   Should start with `hf_` followed by 37 characters

### Step 2: Set Environment Variable

#### Linux/macOS (WSL)

**Temporary (current session only)**:
```bash
export HF_API_KEY="hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

**Permanent** (add to `~/.bashrc` or `~/.zshrc`):
```bash
echo 'export HF_API_KEY="hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"' >> ~/.bashrc
source ~/.bashrc
```

#### Windows PowerShell

**Temporary (current session only)**:
```powershell
$env:HF_API_KEY="hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

**Permanent** (user environment variable):
```powershell
[System.Environment]::SetEnvironmentVariable('HF_API_KEY', 'hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', 'User')
```

#### IntelliJ IDEA / IDE

Add to run configuration environment variables:
```
HF_API_KEY=hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Step 3: Verify Configuration

```bash
# Check environment variable is set
echo $HF_API_KEY

# Expected output: hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Step 4: Run Integration Tests

```bash
# Run all integration tests
mvn test -Dtest="*IntegrationTest"

# Run specific integration test
mvn test -Dtest="RoleManagerIntegrationTest"
```

**Expected behavior**:
- First request may take ~20 seconds (model cold start)
- Subsequent requests are faster (~5-10 seconds)
- Test should complete successfully within 60 seconds

---

## Troubleshooting

### Issue: "HuggingFace health check failed: API key not configured"

**Cause**: `HF_API_KEY` environment variable not set

**Solution**:
```bash
# Verify environment variable
echo $HF_API_KEY

# If empty, set it
export HF_API_KEY="hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

### Issue: "401 Unauthorized" or "Invalid API key"

**Cause**: API key is incorrect or expired

**Solution**:
1. Verify token at [https://huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)
2. Generate new token if needed
3. Update environment variable

### Issue: "429 Too Many Requests" or "Rate limit exceeded"

**Cause**: Exceeded free tier rate limits (~few hundred requests/hour)

**Solutions**:
1. **Wait** - Rate limits reset hourly
2. **Upgrade to PRO** - $9/month for 20× more credits
3. **Use Ollama locally** - Switch to localhost Ollama temporarily:
   ```yaml
   # src/test/resources/application-integrationtest.yml
   llm:
     primary-provider: ollama  # Temporarily use local Ollama
   ```

### Issue: "Model loading timeout" or slow first request

**Cause**: HuggingFace cold start (model not loaded)

**Expected behavior**: First request takes ~20 seconds
**Workaround**: Run a warmup request before running full test suite

```bash
# Warmup request
curl -X POST "https://api-inference.huggingface.co/models/Qwen/Qwen2-0.5B" \
  -H "Authorization: Bearer $HF_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"inputs": "test", "parameters": {"max_new_tokens": 5}}'
```

### Issue: "Service Unavailable" or "Model is loading"

**Cause**: Model is being loaded on HuggingFace servers

**Solution**: Wait ~30 seconds and retry

---

## Configuration Details

### Default Configuration

```yaml
# src/test/resources/application-integrationtest.yml
llm:
  primary-provider: huggingface

  huggingface:
    api-key: ${HF_API_KEY:}
    model: Qwen/Qwen2-0.5B  # Small, fast model
```

### Alternative Models

If `Qwen/Qwen2-0.5B` has issues, try:

1. **`google/gemma-2-2b-it`** - Instruction-tuned, higher quality
2. **`deepseek-ai/DeepSeek-R1-Distill-Qwen-1.5B`** - Slightly larger, more capable
3. **`microsoft/phi-4`** - Microsoft's small model

**To change model**:
```yaml
huggingface:
  model: google/gemma-2-2b-it
```

---

## Performance Expectations

### Typical Response Times

| Scenario | Duration | Notes |
|----------|----------|-------|
| **First request (cold start)** | 15-25s | Model loading time |
| **Subsequent requests** | 5-15s | Model already loaded |
| **Integration test (200 tokens)** | 10-20s | Acceptable for testing |

### Rate Limits (Free Tier)

- **Requests**: ~Few hundred per hour
- **Sufficient for**: Integration testing, development
- **Not suitable for**: Production workloads, load testing

---

## Switching Between Providers

### Use HuggingFace (Cloud, no local setup)

```yaml
llm:
  primary-provider: huggingface
```

**Best for**: CI/CD, developers without local Ollama

### Use Ollama (Local, offline-capable)

```yaml
llm:
  primary-provider: ollama
```

**Best for**: Offline development, unlimited requests

---

## Security Best Practices

### DO ✅

- Store API key in environment variables
- Add `.env` to `.gitignore`
- Use **Read** role tokens (not Write)
- Rotate tokens periodically

### DON'T ❌

- Commit API keys to git
- Share tokens publicly
- Use Write/Admin tokens for testing
- Hardcode tokens in code

---

## References

- [HuggingFace Inference API Docs](https://huggingface.co/docs/api-inference/en/index)
- [HuggingFace Pricing](https://huggingface.co/pricing)
- [Token Management](https://huggingface.co/settings/tokens)
- [Qwen2-0.5B Model Card](https://huggingface.co/Qwen/Qwen2-0.5B)

---

**Last Updated**: 2025-10-18
**Status**: ✅ Production-ready
**Recommended**: Use HuggingFace for integration tests to avoid Ollama bugs
