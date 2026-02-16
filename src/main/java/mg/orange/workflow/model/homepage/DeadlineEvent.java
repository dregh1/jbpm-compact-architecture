package mg.orange.workflow.model.homepage;

import lombok.*;

import jakarta.persistence.*;

/**
 * Entité pour les dates limites / deadlines
 */
@Entity
@DiscriminatorValue("DEADLINE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class DeadlineEvent extends Event {

    @Column(name = "related_entity_id")
    private String relatedEntityId; // ID de la tâche/projet associé

    @Column(name = "related_entity_type")
    private String relatedEntityType; // TASK, PROJECT, DOCUMENT, etc.

    @Column(name = "is_milestone")
    private Boolean isMilestone = false;

    public DeadlineEvent(String userId, String title, String description) {
        super();
        setUserId(userId);
        setTitle(title);
        setDescription(description);
        setEventType("DEADLINE");
    }
}
