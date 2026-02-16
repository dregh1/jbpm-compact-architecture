package mg.orange.workflow.model.process;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse de création d'une nouvelle version BPMN
 */
public class CreateVersionResponseDTO {

    private String processId;
    private String versionSemver;
    private String changeType;
    private String changeDescription;
    private String previousVersion;
    private LocalDateTime createdAt;
    private String createdBy;
    private String changeComment;

    public CreateVersionResponseDTO() {
    }

    public CreateVersionResponseDTO(String processId, String versionSemver, String changeType,
                                    String changeDescription, String previousVersion,
                                    LocalDateTime createdAt, String createdBy, String changeComment) {
        this.processId = processId;
        this.versionSemver = versionSemver;
        this.changeType = changeType;
        this.changeDescription = changeDescription;
        this.previousVersion = previousVersion;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.changeComment = changeComment;
    }

    // Getters
    public String getProcessId() {
        return processId;
    }

    public String getVersionSemver() {
        return versionSemver;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getChangeComment() {
        return changeComment;
    }

    // Setters
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public void setVersionSemver(String versionSemver) {
        this.versionSemver = versionSemver;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }
}
