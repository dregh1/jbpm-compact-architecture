package mg.orange.workflow.model.digitaltwin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Événement capturé par le Digital Twin
 * Historique complet pour Process Mining et apprentissage ML
 */
@Entity
@Table(name = "twin_events", indexes = {
    @Index(name = "idx_twin_events_twin_id", columnList = "twin_id"),
    @Index(name = "idx_twin_events_timestamp", columnList = "timestamp"),
    @Index(name = "idx_twin_events_instance_id", columnList = "instance_id"),
    @Index(name = "idx_twin_events_type", columnList = "event_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwinEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "twin_id", nullable = false)
    private DigitalTwin digitalTwin;
    
    /**
     * ID instance concernée (NULL pour événements globaux)
     */
    @Column(name = "instance_id", length = 255)
    private String instanceId;
    
    /**
     * Type d'événement
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    /**
     * Données complètes de l'événement (JSON)
     */
    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private String eventData;
    
    /**
     * Date/heure de l'événement
     */
    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    /**
     * Types d'événements standards
     */
    public static class EventType {
        public static final String INSTANCE_STARTED = "INSTANCE_STARTED";
        public static final String INSTANCE_COMPLETED = "INSTANCE_COMPLETED";
        public static final String TASK_COMPLETED = "TASK_COMPLETED";
        public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
        public static final String GATEWAY_REACHED = "GATEWAY_REACHED";
        public static final String ERROR_OCCURRED = "ERROR_OCCURRED";
        public static final String TIMEOUT_DETECTED = "TIMEOUT_DETECTED";
        public static final String VARIABLE_UPDATED = "VARIABLE_UPDATED";
    }
}
