package mg.orange.workflow.model.bpmn;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour les définitions BPMN
 */
public class BpmnDefinitionDTO {

    private Long id;
    private String processId;
    private String name;
    private String version;
    private String versionSemver;
    private String status;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<BpmnDefinitionHistoryDTO> history;

    public BpmnDefinitionDTO() {
        // Constructeur par défaut requis pour la sérialisation/désérialisation et l'instanciation par les frameworks ORM
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getVersionSemver() {
        return versionSemver;
    }

    public void setVersionSemver(String versionSemver) {
        this.versionSemver = versionSemver;
    }

    public List<BpmnDefinitionHistoryDTO> getHistory() {
        return history;
    }

    public void setHistory(List<BpmnDefinitionHistoryDTO> history) {
        this.history = history;
    }
}
