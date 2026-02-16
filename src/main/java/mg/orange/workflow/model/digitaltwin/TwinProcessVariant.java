package mg.orange.workflow.model.digitaltwin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Variant de processus découvert (Process Mining)
 * Un variant = un chemin d'exécution unique
 * Exemple: Start → Task1 → GatewayYes → Task2 → End
 */
@Entity
@Table(name = "twin_process_variants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_twin_variant_hash", columnNames = {"twin_id", "variant_hash"})
    },
    indexes = {
        @Index(name = "idx_twin_variants_twin_id", columnList = "twin_id"),
        @Index(name = "idx_twin_variants_frequency", columnList = "frequency"),
        @Index(name = "idx_twin_variants_duration", columnList = "avg_duration")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwinProcessVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "twin_id", nullable = false)
    private DigitalTwin digitalTwin;
    
    /**
     * Hash SHA-256 du chemin pour déduplication
     * Calculé à partir de la séquence ordonnée de nœuds
     */
    @Column(name = "variant_hash", nullable = false, length = 64)
    private String variantHash;
    
    /**
     * Chemin d'exécution (JSON Array)
     * Exemple: ["Start", "UserTask_1", "Gateway_XOR", "Task_A", "Task_B", "End"]
     */
    @Column(name = "path", nullable = false, columnDefinition = "jsonb")
    private String path;
    
    /**
     * Nombre d'instances ayant suivi ce chemin
     */
    @Column(name = "frequency", nullable = false)
    @Builder.Default
    private Integer frequency = 1;
    
    /**
     * Durée moyenne d'exécution (millisecondes)
     */
    @Column(name = "avg_duration")
    private Long avgDuration;
    
    /**
     * Première fois où ce variant a été observé
     */
    @Column(name = "first_seen", nullable = false, updatable = false)
    private LocalDateTime firstSeen;
    
    /**
     * Dernière fois où ce variant a été observé
     */
    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;
    
    @PrePersist
    protected void onCreate() {
        firstSeen = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastSeen = LocalDateTime.now();
    }
    
    /**
     * Incrémente la fréquence du variant
     */
    @Transient
    public void incrementFrequency() {
        this.frequency++;
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Met à jour la durée moyenne avec une nouvelle durée
     * @param newDuration Nouvelle durée en millisecondes
     */
    @Transient
    public void updateAverageDuration(long newDuration) {
        if (avgDuration == null) {
            avgDuration = newDuration;
        } else {
            // Moyenne pondérée
            avgDuration = ((avgDuration * (frequency - 1)) + newDuration) / frequency;
        }
    }
}
