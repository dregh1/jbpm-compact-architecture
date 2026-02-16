package mg.orange.workflow.model.bpmn;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant une définition de processus BPMN stockée en base de données
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "bpmn_definition")
public class BpmnDefinition extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "process_id", unique = true, nullable = false, length = 255)
    private String processId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "version_semver", nullable = true, length = 20)
    private String versionSemver;

    @Column(name = "bpmn_xml", nullable = false, columnDefinition = "TEXT")
    private String bpmnXml;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "status", length = 50)
    private String status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Statuts possibles pour un processus BPMN
     */
    public static class Status {
        private Status() {
            // utility class, prevent instantiation
        }
        public static final String ACTIVE = "ACTIVE";
        public static final String INACTIVE = "INACTIVE";
        public static final String PENDING_DEPLOYMENT = "PENDING_DEPLOYMENT";
    }
}
