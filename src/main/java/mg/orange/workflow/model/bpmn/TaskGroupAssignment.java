package mg.orange.workflow.model.bpmn;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.orange.workflow.model.groupe.Groupe;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant l'affectation d'un groupe à une tâche BPMN
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "task_group_assignment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"process_id", "task_id", "id_groupe"}))
public class TaskGroupAssignment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "process_id", nullable = false, length = 255)
    private String processId;

    @Column(name = "task_id", nullable = false, length = 255)
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_groupe", nullable = false)
    private Groupe groupe;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TaskGroupAssignment(String processId, String taskId, Groupe groupe) {
        this.processId = processId;
        this.taskId = taskId;
        this.groupe = groupe;
        this.createdAt = LocalDateTime.now();
    }
}
