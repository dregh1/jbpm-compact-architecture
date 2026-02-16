package mg.orange.workflow.model.digitaltwin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mg.orange.workflow.model.user.Utilisateur;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Optimisation suggérée par le Digital Twin
 * Le Twin analyse et propose des améliorations (validation humaine requise)
 */
@Entity
@Table(name = "twin_optimizations", indexes = {
    @Index(name = "idx_twin_optimizations_twin_id", columnList = "twin_id"),
    @Index(name = "idx_twin_optimizations_status", columnList = "status"),
    @Index(name = "idx_twin_optimizations_type", columnList = "optimization_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwinOptimization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "twin_id", nullable = false)
    private DigitalTwin digitalTwin;
    
    /**
     * Type d'optimisation
     */
    @Column(name = "optimization_type", nullable = false, length = 100)
    private String optimizationType;
    
    /**
     * Description humaine de l'optimisation
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    /**
     * Estimation de l'impact (JSON)
     * Exemple: {
     *   "timeSaved": 120000,  // ms
     *   "costReduction": 15,  // %
     *   "qualityIncrease": 5  // %
     * }
     */
    @Column(name = "impact_estimation", columnDefinition = "jsonb")
    private String impactEstimation;
    
    /**
     * Données détaillées de la suggestion (JSON)
     * Exemple: {
     *   "targetNode": "Task_Validation",
     *   "action": "add_user",
     *   "newUserCount": 3,
     *   "currentBottleneckScore": 8.5
     * }
     */
    @Column(name = "suggested_data", columnDefinition = "jsonb")
    private String suggestedData;
    
    /**
     * Statut de l'optimisation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private OptimizationStatus status = OptimizationStatus.SUGGESTED;
    
    @Column(name = "suggested_at", nullable = false, updatable = false)
    private LocalDateTime suggestedAt;
    
    /**
     * Date d'application (NULL si pas encore appliqué)
     */
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
    
    /**
     * Utilisateur ayant appliqué l'optimisation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by")
    private Utilisateur appliedBy;
    
    /**
     * Raison du rejet (si status = REJECTED)
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @PrePersist
    protected void onCreate() {
        suggestedAt = LocalDateTime.now();
    }
    
    /**
     * États possibles d'une optimisation
     */
    public enum OptimizationStatus {
        SUGGESTED,  // Proposée par le Twin
        APPROVED,   // Validée par un humain
        APPLIED,    // Appliquée au processus
        REJECTED    // Refusée
    }
    
    /**
     * Types d'optimisation standards
     */
    public static class OptimizationType {
        public static final String RESOURCE_ALLOCATION = "RESOURCE_ALLOCATION";
        public static final String BOTTLENECK_REMOVAL = "BOTTLENECK_REMOVAL";
        public static final String RULE_CHANGE = "RULE_CHANGE";
        public static final String TASK_REORDER = "TASK_REORDER";
        public static final String PARALLELIZATION = "PARALLELIZATION";
        public static final String AUTOMATION = "AUTOMATION";
    }
}
