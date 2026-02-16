package mg.orange.workflow.model.homepage;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA pour la gestion des événements utilisateur (Task, Meeting, Deadline, etc.)
 * Permet de gérer tout type d'événement via un modèle unifié avec polymorphisme
 */
@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_user_event", columnList = "user_id,event_date"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_event_status", columnList = "status")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "event_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Event extends PanacheEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_date")
    private LocalDateTime endTime;

    @Column(name = "event_type", nullable = false, insertable = false, updatable = false)
    private String eventType;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, CONFIRMED, CANCELLED, COMPLETED

    @Column(name = "priority")
    private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW

    @Column(name = "location")
    private String location;

    @Column(name = "color")
    private String color; // Pour le frontend (ex: #FF5733)

    @Column(name = "reminder_minutes")
    private Integer reminderMinutes; // Rappel X minutes avant

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false; // Marque si le rappel J-1 a été envoyé

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "external_id")
    private String externalId; // Pour lier avec les tâches BPMN/Kogito

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Vérifie si l'événement est en retard
     */
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(this.startTime) && !this.status.equals("COMPLETED");
    }

    /**
     * Vérifie si l'événement est proche (dans les 24h)
     */
    public boolean isUpcoming() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        return this.startTime.isAfter(LocalDateTime.now()) && this.startTime.isBefore(tomorrow);
    }

    /**
     * Calcule la durée en minutes
     */
    public long getDurationMinutes() {
        if (this.endTime != null) {
            return java.time.temporal.ChronoUnit.MINUTES.between(this.startTime, this.endTime);
        }
        return 0;
    }
}
