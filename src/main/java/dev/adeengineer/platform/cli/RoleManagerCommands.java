package dev.adeengineer.platform.cli;

import java.util.HashMap;
import java.util.List;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.agent.TaskRequest;
import adeengineer.dev.agent.TaskResult;

import dev.adeengineer.platform.core.RoleManager;
import lombok.extern.slf4j.Slf4j;

/** Spring Shell CLI commands for the Role Manager application. */
@Slf4j
@ShellComponent
public class RoleManagerCommands {

    /** Separator line width for formatted output. */
    private static final int SEPARATOR_WIDTH = 60;

    /** Short separator line width for list output. */
    private static final int SHORT_SEPARATOR_WIDTH = 40;

    /** Role manager for executing tasks and retrieving role information. */
    private final RoleManager roleManager;

    /**
     * Constructs a new RoleManagerCommands.
     *
     * @param manager Role manager instance
     */
    public RoleManagerCommands(final RoleManager manager) {
        this.roleManager = manager;
    }

    /**
     * Execute a task with a specific role.
     *
     * <p>Usage: execute --role "Software Developer" --task "Review PR #123"
     *
     * @param role Role name (e.g., 'Software Developer')
     * @param task Task description
     * @return Formatted task result
     */
    @ShellMethod(key = "execute", value = "Execute a task with a specific role agent")
    public String executeTask(
            @ShellOption(help = "Role name (e.g., 'Software Developer')") final String role,
            @ShellOption(help = "Task description") final String task) {
        log.info("CLI: Executing task for role: {}", role);

        try {
            TaskRequest request = new TaskRequest(role, task, new HashMap<>());
            TaskResult result = roleManager.executeTask(request);

            if (result.success()) {
                return formatSuccessResult(result);
            } else {
                return formatErrorResult(result);
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List all available roles.
     *
     * <p>Usage: list-roles
     *
     * @return Formatted list of available roles
     */
    @ShellMethod(key = "list-roles", value = "List all available role agents")
    public String listRoles() {
        log.info("CLI: Listing all roles");

        List<String> roles = roleManager.listRoles();

        if (roles.isEmpty()) {
            return "No roles registered.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Available Roles (").append(roles.size()).append("):\n");
        sb.append("=".repeat(SHORT_SEPARATOR_WIDTH)).append("\n");

        for (String role : roles) {
            sb.append("  - ").append(role).append("\n");
        }

        return sb.toString();
    }

    /**
     * Describe a specific role.
     *
     * <p>Usage: describe-role "QA Engineer"
     *
     * @param role Role name to describe
     * @return Formatted role information
     */
    @ShellMethod(key = "describe-role", value = "Get detailed information about a specific role")
    public String describeRole(@ShellOption(help = "Role name to describe") final String role) {
        log.info("CLI: Describing role: {}", role);

        try {
            AgentInfo info = roleManager.describeRole(role);
            return formatAgentInfo(info);

        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Show all roles with their capabilities.
     *
     * <p>Usage: show-roles
     *
     * @return Formatted list of all roles with capabilities
     */
    @ShellMethod(key = "show-roles", value = "Show all roles with their capabilities")
    public String showRoles() {
        log.info("CLI: Showing all roles with capabilities");

        List<AgentInfo> rolesInfo = roleManager.getRolesInfo();

        if (rolesInfo.isEmpty()) {
            return "No roles registered.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Registered Roles (").append(rolesInfo.size()).append("):\n");
        sb.append("=".repeat(SEPARATOR_WIDTH)).append("\n\n");

        for (AgentInfo info : rolesInfo) {
            sb.append(formatAgentInfo(info));
            sb.append("\n").append("-".repeat(SEPARATOR_WIDTH)).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Format a successful task result.
     *
     * @param result Task result to format
     * @return Formatted success message
     */
    private String formatSuccessResult(final TaskResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(SEPARATOR_WIDTH)).append("\n");
        sb.append("Task Result - ").append(result.agentName()).append("\n");
        sb.append("=".repeat(SEPARATOR_WIDTH)).append("\n\n");
        sb.append(result.output()).append("\n\n");
        sb.append("=".repeat(SEPARATOR_WIDTH)).append("\n");
        sb.append("Duration: ").append(result.durationMs()).append("ms\n");

        // Get usage info from metadata
        if (result.metadata() != null) {
            Integer totalTokens = (Integer) result.metadata().get("totalTokens");
            Double estimatedCost = (Double) result.metadata().get("estimatedCost");
            if (totalTokens != null) {
                sb.append("Tokens: ").append(totalTokens).append("\n");
            }
            if (estimatedCost != null) {
                sb.append("Cost: $").append(String.format("%.4f", estimatedCost)).append("\n");
            }
        }
        sb.append("=".repeat(SEPARATOR_WIDTH)).append("\n");

        return sb.toString();
    }

    /**
     * Format an error task result.
     *
     * @param result Failed task result
     * @return Formatted error message
     */
    private String formatErrorResult(final TaskResult result) {
        return "Task failed: " + result.errorMessage();
    }

    /**
     * Format role information.
     *
     * @param info Role information to format
     * @return Formatted role details
     */
    private String formatAgentInfo(final AgentInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(info.name()).append("\n");
        sb.append("Description: ").append(info.description()).append("\n");
        sb.append("Output Format: ").append(info.outputFormat()).append("\n");
        sb.append("Capabilities:\n");

        for (String capability : info.capabilities()) {
            sb.append("  - ").append(capability).append("\n");
        }

        return sb.toString();
    }
}
