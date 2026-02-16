package mg.orange.workflow.model.digitaltwin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Anomalie détectée par le Digital Twin
 * Comparaison réel vs prédit → si déviation > seuil → alerte
 */
@Entity
@Table(name = "twin_anomalies", indexes = {
    @Index(name = "idx_twin_anomalies_twin_id", columnList = "twin_id"),
    @Index(name = "idx_twin_anomalies_instance_id", columnList = "instance_id"),
    @Index(name = "idx_twin_anomalies_severity", columnList = "severity")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwinAnomaly {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "twin_id", nullable = false)
    private DigitalTwin digitalTwin;
    
    /**
     * ID de l'instance concernée
     */
    @Column(name = "instance_id", nullable = false, length = 255)
    private String instanceId;
    
    /**
     * Type d'anomalie
     */
    @Column(name = "anomaly_type", nullable = false, length = 100)
    private String anomalyType;
    
    /**
     * Gravité de l'anomalie
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AnomalySeverity severity;
    
    /**
     * Description détaillée de l'anomalie
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    /**
     * Données techniques de l'anomalie (JSON)
     * Exemple: {
     *   "expected": "Task_2",
     *   "actual": "Task_Error",
     *   "deviation": 45,  // %
     *   "predictedTime": 300000,
     *   "actualTime": 520000
     * }
     */
    @Column(name = "anomaly_data", columnDefinition = "jsonb")
    private String anomalyData;
    
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;
    
    /**
     * Date de résolution (NULL si non résolu)
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    /**
     * Action prise pour résoudre l'anomalie
     */
    @Column(name = "resolution_action", columnDefinition = "TEXT")
    private String resolutionAction;
    
    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
    
    /**
     * Vérifie si l'anomalie est résolue
     */
    @Transient
    public boolean isResolved() {
        return resolvedAt != null;
    }
    
    /**
     * Gravité de l'anomalie
     */
    public enum AnomalySeverity {
        LOW,       // Écart mineur, informatif
        MEDIUM,    // Écart notable, surveillance
        HIGH,      // Écart important, action recommandée
        CRITICAL   // Écart critique, action urgente
    }
    
    /**
     * Types d'anomalies standards
     */
    public static class AnomalyType {
        public static final String DEVIATION = "DEVIATION";                 // Écart de chemin
        public static final String TIMEOUT_RISK = "TIMEOUT_RISK";           // Risque de timeout
        public static final String UNEXPECTED_PATH = "UNEXPECTED_PATH";     // Chemin inattendu
        public static final String PERFORMANCE_DROP = "PERFORMANCE_DROP";   // Baisse de performance
        public static final String RESOURCE_OVERLOAD = "RESOURCE_OVERLOAD"; // Surcharge ressources
        public static final String DATA_QUALITY = "DATA_QUALITY";           // Problème qualité données
    }
}
