package mg.orange.workflow.model.process;

import mg.orange.workflow.model.bpmn.VersionChangeType;

import java.util.List;
import java.util.Map;

/**
 * DTO représentant la définition d'un processus Kogito
 */
public class ProcessDTO {

    private String id;
    private String name;
    private String version;
    private VersionChangeType versionChangeType;
    private ProcessType type;
    private List<String> roles;
    private Map<String, Object> metadata;
    private DeploymentStatus deploymentStatus;
    
    public ProcessDTO() {
    }
    
    public ProcessDTO(String id, String name, String version) {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public VersionChangeType getVersionChangeType() {
        return versionChangeType;
    }

    public void setVersionChangeType(VersionChangeType versionChangeType) {
        this.versionChangeType = versionChangeType;
    }

    public ProcessType getType() {
        return type;
    }

    public void setType(ProcessType type) {
        this.type = type;
    }

    /**
     * Setter pour compatibilité avec les chaînes de caractères (conversion auto)
     */
    public void setType(String type) {
        this.type = ProcessType.fromCode(type != null ? type : ProcessType.getDefault().getCode());
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public DeploymentStatus getDeploymentStatus() {
        return deploymentStatus;
    }

    public void setDeploymentStatus(DeploymentStatus deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }
}
