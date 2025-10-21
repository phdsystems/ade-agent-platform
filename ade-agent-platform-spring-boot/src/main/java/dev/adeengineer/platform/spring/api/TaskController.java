package dev.adeengineer.platform.spring.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.platform.core.RoleManager;
import lombok.extern.slf4j.Slf4j;

/** REST API controller for task execution. */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    /** Role manager for executing agent tasks. */
    private final RoleManager roleManager;

    /**
     * Constructs a new TaskController.
     *
     * @param manager Role manager for executing agent tasks
     */
    public TaskController(final RoleManager manager) {
        this.roleManager = manager;
    }

    /**
     * Execute a task with a specific role.
     *
     * <p>POST /api/tasks/execute
     *
     * @param request Task request containing role and task details
     * @return Response entity with task result
     */
    @PostMapping("/execute")
    public ResponseEntity<TaskResult> executeTask(@RequestBody final TaskRequest request) {
        log.info("API request to execute task for agent: {}", request.agentName());

        try {
            TaskResult result = roleManager.executeTask(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error executing task: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Execute a task with multiple roles in parallel.
     *
     * <p>POST /api/tasks/multi-agent
     *
     * @param request Multi-agent request with roles and task
     * @return Response entity with map of results by role
     */
    @PostMapping("/multi-agent")
    public ResponseEntity<Map<String, TaskResult>> executeMultiAgentTask(
            @RequestBody final MultiAgentRequest request) {
        log.info("API request for multi-agent task with {} roles", request.roleNames().size());

        try {
            Map<String, TaskResult> results =
                    roleManager.executeMultiAgentTask(
                            request.roleNames(), request.task(), request.context());
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error executing multi-agent task: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Request DTO for multi-agent tasks.
     *
     * @param roleNames List of role names to execute
     * @param task Task description
     * @param context Additional context for the task
     */
    public record MultiAgentRequest(
            List<String> roleNames, String task, Map<String, Object> context) {}
}
