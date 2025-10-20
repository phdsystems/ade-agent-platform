# Troubleshooting Guide - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Quick diagnosis**: Check logs → Check LLM API status → Check config → Restart. **Common issues**: LLM API failures (check keys), slow response (check timeouts), invalid role (check registry). **Escalation**: After 30 min → contact dev team.

---

## Common Issues

### Issue: Service Won't Start

**Symptoms**: `systemctl status role-manager` shows failed

**Diagnosis**:
```bash
journalctl -u role-manager -n 50
```

**Common Causes**:
1. Missing API keys → Set ANTHROPIC_API_KEY or OPENAI_API_KEY
2. Port already in use → Change SERVER_PORT
3. Invalid configuration → Check application.yml syntax

**Solution**:
```bash
# Set API keys
export ANTHROPIC_API_KEY=sk-ant-xxx

# Restart
systemctl restart role-manager
```

---

### Issue: "Unknown role" Error

**Symptoms**: User gets "Unknown LLM provider: XYZ"

**Diagnosis**:
```bash
role-manager list-roles
```

**Cause**: Role not registered or typo in role name

**Solution**: Use exact role name from `list-roles` output

---

### Issue: Slow Response Time

**Symptoms**: Tasks take > 1 minute

**Diagnosis**:
```bash
# Check LLM provider status
curl https://status.anthropic.com/

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep duration
```

**Common Causes**:
1. LLM API latency → Switch provider or wait
2. Large prompts → Reduce context size
3. Network issues → Check connectivity

---

### Issue: LLM API 401 Unauthorized

**Symptoms**: "AuthenticationError: Invalid API key"

**Solution**:
```bash
# Verify API key is set
echo $ANTHROPIC_API_KEY

# Update if needed
export ANTHROPIC_API_KEY=sk-ant-new-key

# Restart
systemctl restart role-manager
```

---

### Issue: High LLM Costs

**Diagnosis**:
```bash
# Check token usage
role-manager metrics --metric llm_tokens_total
```

**Solutions**:
1. Implement caching for common queries
2. Reduce max_tokens in agent configs
3. Switch to cheaper LLM provider (Ollama local)

---

## Emergency Procedures

### Service Degradation

1. Check service status
2. Review error logs
3. Restart service
4. If still failing, rollback to previous version

### LLM Provider Outage

1. Confirm outage: Check provider status page
2. Switch to backup provider:
   ```bash
   export LLM_PROVIDER=openai
   systemctl restart role-manager
   ```
3. Monitor and switch back when primary recovers

---

## Getting Help

**Self-Service**:
1. Check this troubleshooting guide
2. Review logs
3. Check LLM provider status pages

**Escalation**:
- Slack: #role-manager-support
- Email: devops@company.com
- PagerDuty (critical issues only)

**Provide**:
- Error message
- Recent logs
- Steps to reproduce
- Expected vs actual behavior

---

*Last Updated: 2025-10-17*
