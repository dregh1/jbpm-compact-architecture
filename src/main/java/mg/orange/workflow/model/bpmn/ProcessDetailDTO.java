package mg.orange.workflow.model.bpmn;

import mg.orange.workflow.model.process.DiagramInfoDTO;
import mg.orange.workflow.model.process.ProcessType;

import java.util.List;
import java.util.Map;

/**
 * DTO représentant les détails complets d'un processus BPMN
 */
public class ProcessDetailDTO {

    private String id;
    private String name;
    private String version;
    private ProcessType type;
    private List<String> roles;
    private Map<String, Object> metadata;
    private String bpmnXml;
    private List<ProcessTaskDTO> tasks;
    private List<ProcessNodeDTO> nodes;
    private DiagramInfoDTO diagramInfo;

    public ProcessDetailDTO() {
    }

    public ProcessDetailDTO(String id, String name, String version) {
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

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public List<ProcessTaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<ProcessTaskDTO> tasks) {
        this.tasks = tasks;
    }

    public List<ProcessNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<ProcessNodeDTO> nodes) {
        this.nodes = nodes;
    }

    public DiagramInfoDTO getDiagramInfo() {
        return diagramInfo;
    }

    public void setDiagramInfo(DiagramInfoDTO diagramInfo) {
        this.diagramInfo = diagramInfo;
    }
}
