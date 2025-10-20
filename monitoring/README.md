# Monitoring Setup Guide

This directory contains configuration files for comprehensive monitoring of the Role Manager App using Prometheus and Grafana.

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Running Role Manager App on port 8080

### 1. Start Monitoring Stack

```bash
cd monitoring
docker-compose up -d
```

This will start:
- **Prometheus** on port 9090 (metrics collection)
- **Grafana** on port 3000 (visualization)
- **AlertManager** on port 9093 (alerting)

### 2. Access Grafana

1. Open http://localhost:3000
2. Login with default credentials: `admin` / `admin`
3. Import the dashboard: Configuration → Dashboards → Import → Upload `grafana-dashboard.json`

### 3. Verify Metrics Collection

1. Open http://localhost:9090
2. Navigate to Status → Targets
3. Verify `role-manager-app` target is UP

## Architecture

```
┌─────────────────┐         ┌────────────┐         ┌─────────┐
│ Role Manager    │ metrics │ Prometheus │ scrape  │ Grafana │
│ App :8080       ├────────►│   :9090    ├────────►│  :3000  │
│ /actuator/      │         │            │         │         │
│ prometheus      │         └──────┬─────┘         └─────────┘
└─────────────────┘                │
                                   │ alerts
                                   ▼
                            ┌──────────────┐
                            │ AlertManager │
                            │    :9093     │
                            └──────────────┘
```

## Metrics Collected

### LLM Provider Metrics

- `llm_requests_total` - Total requests by provider
- `llm_response_time_seconds` - Response latency histogram
- `llm_errors_total` - Error count by provider and type
- `llm_tokens_total` - Token usage (input/output)
- `llm_cost_usd_total` - Cost tracking per provider

### Cache Metrics

- `cache_hits_total` - Cache hit count
- `cache_misses_total` - Cache miss count
- `cache_evictions_total` - Cache evictions

### Circuit Breaker Metrics

- `resilience4j_circuitbreaker_state` - Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j_circuitbreaker_calls_total` - Total calls by result
- `resilience4j_circuitbreaker_buffered_calls` - Buffered calls in sliding window

### Connection Pool Metrics

- `reactor_netty_connection_provider_total_connections` - Total connections
- `reactor_netty_connection_provider_active_connections` - Active connections
- `reactor_netty_connection_provider_idle_connections` - Idle connections

### JVM Metrics

- `jvm_memory_used_bytes` - JVM memory usage
- `jvm_threads_live` - Active thread count
- `jvm_gc_pause_seconds` - GC pause time

## Alerts

Configured alerts in `alerts.yml`:

| Alert | Severity | Condition | Duration |
|-------|----------|-----------|----------|
| LLMHighErrorRate | warning | >10% error rate | 5m |
| CircuitBreakerOpen | critical | Circuit breaker open | 2m |
| LLMHighLatency | warning | P95 > 5s | 5m |
| LowCacheHitRate | info | <30% hit rate | 10m |
| ConnectionPoolNearCapacity | warning | >80% utilization | 5m |
| HighDailyCost | warning | >$100/day | - |
| HighMemoryUsage | critical | >90% heap usage | 5m |

## Dashboard Panels

The Grafana dashboard includes 10 panels:

1. **LLM Request Rate** - Requests per second by provider
2. **LLM Response Time (p95)** - 95th percentile latency
3. **LLM Error Rate** - Errors per second by type
4. **Cache Hit Rate** - Percentage of cache hits
5. **Circuit Breaker State** - Current state per provider
6. **Token Usage by Provider** - Token consumption rate
7. **Cost per Provider** - Hourly cost tracking
8. **HTTP Connection Pool** - Connection pool utilization
9. **JVM Memory Usage** - Heap memory usage
10. **Streaming vs Non-Streaming** - Request distribution

## Load Testing with Gatling

### Run Load Test

```bash
mvn gatling:test -Dgatling.simulationClass=simulations.LLMLoadTest
```

### Configure Load Test

System properties:

- `base.url` - Base URL (default: http://localhost:8080)
- `users` - Number of concurrent users (default: 10)
- `ramp.duration` - Ramp-up duration in seconds (default: 60)
- `test.duration` - Test duration in seconds (default: 300)

Example:

```bash
mvn gatling:test \
  -Dbase.url=http://localhost:8080 \
  -Dusers=50 \
  -Dramp.duration=120 \
  -Dtest.duration=600
```

### Load Test Scenarios

1. **LLM Generation** - Regular text generation requests
2. **LLM Streaming** - Streaming response requests
3. **Health Check** - Health endpoint monitoring

### Load Test Assertions

- Max response time < 5 seconds
- Success rate > 95%

## Configuration

### Prometheus Configuration

Edit `prometheus.yml` to:
- Adjust scrape intervals (default: 15s)
- Add additional targets
- Configure alert manager endpoints

### Grafana Dashboard

Edit `grafana-dashboard.json` to:
- Customize panel layouts
- Add new metrics
- Adjust alert thresholds

### Alert Rules

Edit `alerts.yml` to:
- Add new alert rules
- Adjust thresholds
- Configure notification channels

## Docker Compose Setup

Create `docker-compose.yml` in this directory:

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alerts.yml:/etc/prometheus/alerts.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    networks:
      - monitoring

  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
    networks:
      - monitoring

volumes:
  prometheus-data:
  grafana-data:

networks:
  monitoring:
    driver: bridge
```

Create `alertmanager.yml`:

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'cluster']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'

receivers:
  - name: 'default'
    # Configure your notification channel here
    # Example: email, slack, pagerduty, webhook
```

## Troubleshooting

### Metrics Not Appearing

1. Verify app is running: `curl http://localhost:8080/actuator/health`
2. Check metrics endpoint: `curl http://localhost:8080/actuator/prometheus`
3. Verify Prometheus target: http://localhost:9090/targets

### High Memory Usage

1. Check heap size: JVM metrics panel
2. Review cache size: `llm.cache.max-size` in application.yml
3. Monitor connection pool: Connection pool panel

### Circuit Breaker Always Open

1. Check provider health: `/actuator/health`
2. Review error logs: `docker logs role-manager-app`
3. Adjust circuit breaker thresholds in application.yml

### Load Test Failures

1. Verify base URL is correct
2. Check provider availability
3. Review timeout settings in application.yml
4. Monitor metrics during test execution

## Best Practices

1. **Set up alerts** - Configure AlertManager with notification channels
2. **Monitor costs** - Track LLM provider costs in Grafana
3. **Review dashboards daily** - Check for anomalies and trends
4. **Run load tests regularly** - Validate performance under load
5. **Adjust thresholds** - Fine-tune alert thresholds based on traffic patterns
6. **Archive metrics** - Configure Prometheus retention and remote storage

## References

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Gatling Documentation](https://gatling.io/docs/)
- [Resilience4j Metrics](https://resilience4j.readme.io/docs/micrometer)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

*Last Updated: 2025-10-18*
