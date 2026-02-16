package mg.orange.workflow.model.digitaltwin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.kie.kogito.persistence.postgresql.hibernate.JsonBinaryType;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant un Digital Twin d'un processus BPMN
 * Un Digital Twin = Une réplique virtuelle d'un processus qui suit toutes ses instances
 * 
 * Relation: 1 Processus BPMN → 1 Digital Twin → N Instances virtuelles
 */
@Entity
@Table(name = "digital_twins", indexes = {
    @Index(name = "idx_digital_twins_process_id", columnList = "process_definition_id"),
    @Index(name = "idx_digital_twins_state", columnList = "state")
})

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalTwin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID unique du processus BPMN (ex: "demande_conge_v1", "achat_materiel_v2")
     * Un seul Twin par processus
     */
    @Column(name = "process_definition_id", nullable = false, unique = true, length = 255)
    private String processDefinitionId;
    
    /**
     * Définition XML BPMN complète du processus
     * Utilisé pour parsing, simulation, visualisation 3D
     */
    @Column(name = "bpmn_xml", nullable = false, columnDefinition = "TEXT")
    private String bpmnXml;
    
    /**
     * Date de création du Twin
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Date de dernière mise à jour
     * Mis à jour automatiquement par trigger SQL
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * État du Twin
     * - ACTIVE: Twin actif, suit les instances
     * - PAUSED: Twin en pause, ne se synchronise plus
     * - ARCHIVED: Twin archivé, historique conservé
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 50)
    @Builder.Default
    private TwinState state = TwinState.ACTIVE;
    
    /**
     * Configuration JSON du Twin
     * Exemple: {
     *   "prediction": { "enabled": true, "confidence_threshold": 0.70 },
     *   "anomaly_detection": { "deviation_threshold_percent": 25 },
     *   "simulation": { "default_iterations": 1000 }
     * }
     */

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode config = JsonNodeFactory.instance.objectNode();
    /**
     * Métriques globales calculées (cache JSON)
     * Exemple: {
     *   "avg_execution_time_ms": 45000,
     *   "success_rate": 0.92,
     *   "prediction_accuracy": 0.85,
     *   "total_instances": 1234,
     *   "active_instances": 45
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode metrics = JsonNodeFactory.instance.objectNode();
    
    /**
     * Callback avant insertion: initialiser les dates
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Callback avant update: mettre à jour updated_at
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * États possibles du Digital Twin
     */
    public enum TwinState {
        /**
         * ACTIVE: Twin actif, synchronisation en cours
         */
        ACTIVE,
        
        /**
         * PAUSED: Twin en pause, pas de synchronisation
         */
        PAUSED,
        
        /**
         * ARCHIVED: Twin archivé, conservation historique uniquement
         */
        ARCHIVED,
        
        /**
         * TEMPLATE: Configuration par défaut (process_definition_id="_DEFAULT_CONFIG_")
         */
        TEMPLATE
    }
}
