package mg.orange.workflow.model.homepage;

import lombok.*;

import jakarta.persistence.*;

/**
 * Entité pour les tâches BPMN/Kogito gérées en tant qu'événements
 */
@Entity
@DiscriminatorValue("TASK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class TaskEvent extends Event {

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "process_id")
    private String processId;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "priority_score")
    private Integer priorityScore; // Score calculé (0-100)

    public TaskEvent(String userId, String title, String description) {
        super();
        setUserId(userId);
        setTitle(title);
        setDescription(description);
        setEventType("TASK");
    }
}
