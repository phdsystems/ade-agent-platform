package adeengineer.dev.platform.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import adeengineer.dev.agent.AgentInfo;
import adeengineer.dev.platform.core.RoleManager;

import lombok.extern.slf4j.Slf4j;

/** REST API controller for role discovery and information. */
@Slf4j
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    /** Role manager for accessing role information. */
    private final RoleManager roleManager;

    /**
     * Constructs a new RoleController.
     *
     * @param manager Role manager for accessing role information
     */
    public RoleController(final RoleManager manager) {
        this.roleManager = manager;
    }

    /**
     * List all available roles.
     *
     * <p>GET /api/roles
     *
     * @return Response entity with list of role names
     */
    @GetMapping
    public ResponseEntity<List<String>> listRoles() {
        log.debug("API request to list all roles");
        List<String> roles = roleManager.listRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Get detailed information about all roles.
     *
     * <p>GET /api/roles/info
     *
     * @return Response entity with list of role information
     */
    @GetMapping("/info")
    public ResponseEntity<List<AgentInfo>> getRolesInfo() {
        log.debug("API request to get all roles info");
        List<AgentInfo> rolesInfo = roleManager.getRolesInfo();
        return ResponseEntity.ok(rolesInfo);
    }

    /**
     * Get information about a specific role.
     *
     * <p>GET /api/roles/{roleName}
     *
     * @param roleName Name of the role to describe
     * @return Response entity with role information
     */
    @GetMapping("/{roleName}")
    public ResponseEntity<AgentInfo> describeRole(@PathVariable final String roleName) {
        log.debug("API request to describe role: {}", roleName);

        try {
            AgentInfo roleInfo = roleManager.describeRole(roleName);
            return ResponseEntity.ok(roleInfo);
        } catch (IllegalArgumentException e) {
            log.warn("Role not found: {}", roleName);
            return ResponseEntity.notFound().build();
        }
    }
}
