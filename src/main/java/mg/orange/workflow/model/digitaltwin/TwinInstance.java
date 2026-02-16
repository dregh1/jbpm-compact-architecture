package mg.orange.workflow.model.digitaltwin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Instance virtuelle suivie par le Digital Twin
 * Chaque instance réelle Kogito a sa copie virtuelle dans le Twin
 * 
 * Synchronisation: Instance réelle (Kogito) ←→ Instance virtuelle (Twin)
 */
@Entity
@Table(name = "twin_instances", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_twin_real_instance", columnNames = {"twin_id", "real_instance_id"})
    },
    indexes = {
        @Index(name = "idx_twin_instances_twin_id", columnList = "twin_id"),
        @Index(name = "idx_twin_instances_real_id", columnList = "real_instance_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwinInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Référence au Digital Twin parent
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "twin_id", nullable = false)
    private DigitalTwin digitalTwin;
    
    /**
     * ID de l'instance réelle Kogito (table processes.id)
     * Ex: "inst-42", "abc123-def456-..."
     */
    @Column(name = "real_instance_id", nullable = false, length = 255)
    private String realInstanceId;
    
    /**
     * État actuel de l'instance (JSON)
     * Structure: {
     *   "activeNodes": ["Task_1", "Gateway_2"],
     *   "executedActivities": ["Start", "Task_1", "Gateway_1"],
     *   "variables": { "employeeId": "alice", "days": 5, "approved": true },
     *   "currentPosition": { "x": 350, "y": 200 }
     * }
     */
    @Column(name = "current_state", columnDefinition = "text")
    private String currentState;


    /**
     * Timestamp de la dernière synchronisation avec l'instance réelle
     */
    @Column(name = "sync_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime syncTimestamp = LocalDateTime.now();
    /**
     * État prédit de l'instance (JSON)
     * Structure: {
     *   "predictedPath": ["Task_2", "Gateway_3", "Task_4", "End"],
     *   "estimatedTimeRemaining": 18000,  // ms
     *   "confidence": 0.87,
     *   "probabilities": {
     *     "Gateway_3_yes": 0.92,
     *     "Gateway_3_no": 0.08
     *   }
     * }
     */
    @Column(name = "predicted_state", columnDefinition = "text")
    private String predictedState;

    /**
     * Date de création de l'instance virtuelle
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Date de complétion de l'instance
     * NULL si l'instance est encore en cours
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * Callback avant insertion
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        syncTimestamp = LocalDateTime.now();
    }
    
    /**
     * Callback avant update
     */
    @PreUpdate
    protected void onUpdate() {
        syncTimestamp = LocalDateTime.now();
    }
    
    /**
     * Vérifie si l'instance est active (non complétée)
     */
    @Transient
    public boolean isActive() {
        return completedAt == null;
    }
    
    /**
     * Calcule le temps écoulé depuis le début (en millisecondes)
     */
    @Transient
    public long getElapsedTimeMs() {
        if (createdAt == null) return 0;
        LocalDateTime end = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(createdAt, end).toMillis();
    }
}
