package mg.orange.workflow.model.bpmn;

import java.time.LocalDateTime;

/**
 * DTO pour les entrées de l'historique des définitions BPMN
 * Représente un évènement de versionnage pour audit et restauration
 */
public class BpmnDefinitionHistoryDTO {

    private Long id;
    private Long bpmnDefinitionId;
    private String processId;
    private String versionSemver;
    private VersionChangeType changeType;
    private String name;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;
    private String changeComment;

    // NOTE: BPMN XML volontairement exclu pour sécurité API
    // Pour récupération complète, utiliser le repository en backend

    public BpmnDefinitionHistoryDTO() {
    }

    @SuppressWarnings("java:S107")
    private BpmnDefinitionHistoryDTO(Long id, Long bpmnDefinitionId, String processId,
                                     String versionSemver, VersionChangeType changeType, String name,
                                     String status, LocalDateTime createdAt, String createdBy, String changeComment) {
        this.id = id;
        this.bpmnDefinitionId = bpmnDefinitionId;
        this.processId = processId;
        this.versionSemver = versionSemver;
        this.changeType = changeType;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.changeComment = changeComment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long bpmnDefinitionId;
        private String processId;
        private String versionSemver;
        private VersionChangeType changeType;
        private String name;
        private String status;
        private LocalDateTime createdAt;
        private String createdBy;
        private String changeComment;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder bpmnDefinitionId(Long bpmnDefinitionId) {
            this.bpmnDefinitionId = bpmnDefinitionId;
            return this;
        }

        public Builder processId(String processId) {
            this.processId = processId;
            return this;
        }

        public Builder versionSemver(String versionSemver) {
            this.versionSemver = versionSemver;
            return this;
        }

        public Builder changeType(VersionChangeType changeType) {
            this.changeType = changeType;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder changeComment(String changeComment) {
            this.changeComment = changeComment;
            return this;
        }

        public BpmnDefinitionHistoryDTO build() {
            return new BpmnDefinitionHistoryDTO(id, bpmnDefinitionId, processId, versionSemver,
                    changeType, name, status, createdAt, createdBy, changeComment);
        }
    }

    /**
     * Factory method pour créer un DTO depuis une entité historique
     */
    public static BpmnDefinitionHistoryDTO fromEntity(BpmnDefinitionHistory entity) {
        if (entity == null) {
            return null;
        }

        return builder()
            .id(entity.getId())
            .bpmnDefinitionId(entity.getBpmnDefinitionId())
            .processId(entity.getProcessId())
            .versionSemver(entity.getVersionSemver())
            .changeType(entity.getChangeType())
            .name(entity.getName())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .changeComment(entity.getChangeComment())
            .build();
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBpmnDefinitionId() {
        return bpmnDefinitionId;
    }

    public void setBpmnDefinitionId(Long bpmnDefinitionId) {
        this.bpmnDefinitionId = bpmnDefinitionId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getVersionSemver() {
        return versionSemver;
    }

    public void setVersionSemver(String versionSemver) {
        this.versionSemver = versionSemver;
    }

    public VersionChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(VersionChangeType changeType) {
        this.changeType = changeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChangeComment() {
        return changeComment;
    }

    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }
}
