# API Design - Role Manager App

**Date:** 2025-10-17
**Version:** 1.0

---

## REST API Endpoints

### Execute Task
```
POST /api/v1/execute
Content-Type: application/json

Request:
{
  "role": "Software Developer",
  "task": "Review PR #123",
  "context": {
    "repository": "org/repo",
    "branch": "feature/x"
  }
}

Response:
{
  "taskId": "uuid",
  "role": "Software Developer",
  "output": "# Code Review...",
  "usage": {
    "inputTokens": 1234,
    "outputTokens": 567,
    "cost": 0.05
  },
  "timestamp": "2025-10-17T10:30:00Z"
}
```

### Multi-Agent Execution
```
POST /api/v1/execute-multi
Content-Type: application/json

Request:
{
  "roles": ["Developer", "QA", "Security"],
  "task": "Analyze PR #123",
  "context": {...}
}

Response:
{
  "taskId": "uuid",
  "results": [
    {"role": "Developer", "output": "..."},
    {"role": "QA", "output": "..."},
    {"role": "Security", "output": "..."}
  ],
  "aggregated": "Combined analysis...",
  "totalUsage": {...}
}
```

### List Roles
```
GET /api/v1/roles

Response:
{
  "roles": [
    {
      "name": "Software Developer",
      "description": "...",
      "capabilities": ["code review", "debugging", ...]
    },
    ...
  ]
}
```

### Describe Role
```
GET /api/v1/roles/{roleName}

Response:
{
  "name": "Software Developer",
  "description": "Provides technical code reviews...",
  "capabilities": [...],
  "exampleTasks": [
    "Review PR #123",
    "Debug NullPointerException",
    ...
  ]
}
```

---

## CLI Commands

```bash
# Execute task
role-manager execute --role "Developer" --task "Review PR #123"

# Multi-agent
role-manager execute --roles "Developer,QA" --task "Analyze PR #123"

# List roles
role-manager list-roles

# Describe role
role-manager describe-role "QA Engineer"

# View history
role-manager history --role "Developer" --limit 10
```

---

*Last Updated: 2025-10-17*
