package mg.orange.workflow.model.bpmn;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité historique immutable pour les versions BPMN
 * Stocke l'audit complet des changements de version selon Semantic Versioning
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "bpmn_definition_history",
       indexes = {
           @Index(name = "idx_bpmn_history_definition",
                  columnList = "bpmn_definition_id"),
           @Index(name = "idx_bpmn_history_version",
                  columnList = "version_semver"),
           @Index(name = "idx_bpmn_history_change_type",
                  columnList = "change_type"),
           @Index(name = "idx_bpmn_history_compound",
                  columnList = "bpmn_definition_id, version_semver, change_type")
       })
public class BpmnDefinitionHistory extends PanacheEntityBase {
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "bpmn_definition_id", nullable = false)
    private Long bpmnDefinitionId;

    @Column(name = "process_id", nullable = false, length = 255)
    private String processId;

    @Column(name = "version_semver", nullable = false, length = 20)
    private String versionSemver;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 10)
    private VersionChangeType changeType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "bpmn_xml", nullable = false, columnDefinition = "TEXT")
    private String bpmnXml;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "status", nullable = false, length = 50)
    private String status = STATUS_ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "change_comment")
    private String changeComment;

    /**
     * Builder pattern pour créer une nouvelle entrée d'historique
     * Réduit le nombre de paramètres du constructeur (de 10 à 0, validation dans Builder)
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long bpmnDefinitionId;
        private String processId;
        private String versionSemver;
        private VersionChangeType changeType;
        private String name;
        private String bpmnXml;
        private String filePath;
        private String status = STATUS_ACTIVE;
        private String createdBy;
        private String changeComment;

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

        public Builder bpmnXml(String bpmnXml) {
            this.bpmnXml = bpmnXml;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
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

        public BpmnDefinitionHistory build() {
            // Validation des champs requis
            if (bpmnDefinitionId == null) {
                throw new IllegalArgumentException("bpmnDefinitionId ne peut pas être nul");
            }
            if (processId == null || processId.trim().isEmpty()) {
                throw new IllegalArgumentException("processId ne peut pas être nul ou vide");
            }
            if (versionSemver == null || versionSemver.trim().isEmpty()) {
                throw new IllegalArgumentException("versionSemver ne peut pas être nul ou vide");
            }
            if (changeType == null) {
                throw new IllegalArgumentException("changeType ne peut pas être nul");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name ne peut pas être nul ou vide");
            }
            if (bpmnXml == null || bpmnXml.trim().isEmpty()) {
                throw new IllegalArgumentException("bpmnXml ne peut pas être nul ou vide");
            }
            if (status == null) {
                status = STATUS_ACTIVE;
            }

            return new BpmnDefinitionHistory(this);
        }
    }

    /**
     * Constructeur privé utilisé par le Builder
     * Permet seulement la création initiale, pas de modifications
     */
    private BpmnDefinitionHistory(Builder builder) {
        this.bpmnDefinitionId = builder.bpmnDefinitionId;
        this.processId = builder.processId;
        this.versionSemver = builder.versionSemver;
        this.changeType = builder.changeType;
        this.name = builder.name;
        this.bpmnXml = builder.bpmnXml;
        this.filePath = builder.filePath;
        this.status = builder.status;
        this.createdAt = LocalDateTime.now();
        this.createdBy = builder.createdBy;
        this.changeComment = builder.changeComment;
    }

    /**
     * Factory method pour créer facilement une entrée d'historique à partir d'une définition
     * NOTE: Adapté après ajout du champ versionSemver à BpmnDefinition
     */
    public static BpmnDefinitionHistory createFromDefinition(BpmnDefinition definition,
            String versionSemver, VersionChangeType changeType, String changeComment) {
        if (definition == null) {
            throw new IllegalArgumentException("La définition BPMN ne peut pas être nulle");
        }
        if (versionSemver == null || versionSemver.trim().isEmpty()) {
            throw new IllegalArgumentException("La version semver doit être définie");
        }

        return BpmnDefinitionHistory.builder()
            .bpmnDefinitionId(definition.getId())
            .processId(definition.getProcessId())
            .versionSemver(versionSemver)
            .changeType(changeType)
            .name(definition.getName())
            .bpmnXml(definition.getBpmnXml())
            .filePath(definition.getFilePath())
            .status(definition.getStatus())
            .createdBy(definition.getCreatedBy())
            .changeComment(changeComment)
            .build();
    }
}
