# Operations Guide - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## TL;DR

**Daily ops**: Monitor metrics, check logs for errors. **Common tasks**: Add new role, update prompts, restart service. **Alerts**: High error rate, slow response time, LLM API failures. **Support**: Check logs first, then troubleshooting guide.

---

## Daily Operations

### Morning Checks

```bash
# Check service status
systemctl status role-manager

# Check logs for errors
journalctl -u role-manager -n 100 | grep ERROR

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep error
```

### Weekly Tasks

- Review LLM API costs
- Check agent performance metrics
- Review and rotate logs
- Update agent configurations if needed

---

## Common Operations

### Add New Role Agent

1. Create agent Java class (see Developer Guide)
2. Add configuration YAML
3. Restart service
4. Verify with `role-manager list-roles`

### Update Agent Prompt

1. Edit `config/agents/{role}.yaml`
2. Modify `prompt_template`
3. Reload config (or restart service)
4. Test with sample task

### View Agent History

```bash
role-manager history --role "Developer" --since "2025-10-01"
```

---

## Monitoring

### Key Metrics

- **Task Success Rate**: Should be > 95%
- **Response Time P95**: Should be < 30 seconds
- **LLM API Errors**: Should be < 1%
- **Token Usage**: Track for cost management

### Alerts

**Critical**:
- Service down
- Error rate > 10%
- All LLM providers failing

**Warning**:
- Response time P95 > 30 seconds
- LLM API error rate > 5%
- High token usage (cost spike)

---

## Log Management

**Log Locations**:
- Application logs: `/var/log/role-manager/app.log`
- Access logs: `/var/log/role-manager/access.log`
- Error logs: `/var/log/role-manager/error.log`

**Log Rotation**: Daily, keep 30 days

```bash
# View recent errors
tail -f /var/log/role-manager/error.log

# Search for specific role issues
grep "Developer" /var/log/role-manager/app.log | grep ERROR
```

---

## Backup and Recovery

### Backup

```bash
# Backup configuration
tar -czf config-backup-$(date +%Y%m%d).tar.gz config/

# Backup agent history (if persisted)
pg_dump role_manager_db > backup-$(date +%Y%m%d).sql
```

### Recovery

```bash
# Restore configuration
tar -xzf config-backup-20251017.tar.gz

# Restart service
systemctl restart role-manager
```

---

*Last Updated: 2025-10-17*
