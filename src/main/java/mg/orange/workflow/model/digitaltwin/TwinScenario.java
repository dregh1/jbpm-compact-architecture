package mg.orange.workflow.model.digitaltwin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mg.orange.workflow.model.user.Utilisateur;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Scénario What-If sauvegardé
 * Permet de simuler "que se passerait-il si..."
 */
@Entity
@Table(name = "twin_scenarios", indexes = {
    @Index(name = "idx_twin_scenarios_twin_id", columnList = "twin_id"),
    @Index(name = "idx_twin_scenarios_status", columnList = "status"),
    @Index(name = "idx_twin_scenarios_created_by", columnList = "created_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwinScenario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "twin_id", nullable = false)
    private DigitalTwin digitalTwin;
    
    /**
     * Nom du scénario
     */
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * Description détaillée
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * Paramètres du scénario (JSON)
     * Exemple: {
     *   "nbUsers": 5,
     *   "rules": { "auto_approve_threshold": 3 },
     *   "constraints": [ {"type": "max_duration", "value": 3600} ]
     * }
     */
    @Column(name = "parameters", nullable = false, columnDefinition = "jsonb")
    private String parameters;
    
    /**
     * Résultats de la simulation (JSON)
     * Exemple: {
     *   "avgTime": 300000,
     *   "confidence": [280000, 320000],
     *   "iterations": 1000,
     *   "successRate": 0.95,
     *   "bottlenecks": ["Task_Validation"]
     * }
     */
    @Column(name = "results", columnDefinition = "jsonb")
    private String results;
    
    /**
     * Utilisateur ayant créé le scénario
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Statut du scénario
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ScenarioStatus status = ScenarioStatus.PENDING;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * États possibles d'un scénario
     */
    public enum ScenarioStatus {
        PENDING,    // Créé, pas encore simulé
        RUNNING,    // Simulation en cours
        COMPLETED,  // Simulation terminée avec succès
        FAILED      // Simulation échouée
    }
}
