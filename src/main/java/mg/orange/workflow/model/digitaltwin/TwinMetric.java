package mg.orange.workflow.model.digitaltwin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Métrique calculée par le Digital Twin
 * KPIs, statistiques, scores de performance
 */
@Entity
@Table(name = "twin_metrics", indexes = {
    @Index(name = "idx_twin_metrics_twin_id", columnList = "twin_id"),
    @Index(name = "idx_twin_metrics_name", columnList = "metric_name"),
    @Index(name = "idx_twin_metrics_calculated", columnList = "calculated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwinMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "twin_id", nullable = false)
    private DigitalTwin digitalTwin;
    
    /**
     * Nom de la métrique
     */
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;
    
    /**
     * Valeur numérique
     */
    @Column(name = "metric_value", nullable = false, precision = 20, scale = 6)
    private BigDecimal metricValue;
    
    /**
     * Unité de mesure
     */
    @Column(name = "metric_unit", length = 50)
    private String metricUnit;
    
    /**
     * Date de calcul
     */
    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();
    
    /**
     * Début fenêtre temporelle (NULL = depuis début)
     */
    @Column(name = "window_start")
    private LocalDateTime windowStart;
    
    /**
     * Fin fenêtre temporelle (NULL = maintenant)
     */
    @Column(name = "window_end")
    private LocalDateTime windowEnd;
    
    @PrePersist
    protected void onCreate() {
        if (calculatedAt == null) {
            calculatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Noms de métriques standards
     */
    public static class MetricName {
        public static final String AVG_EXECUTION_TIME = "avg_execution_time";
        public static final String SUCCESS_RATE = "success_rate";
        public static final String PREDICTION_ACCURACY = "prediction_accuracy";
        public static final String BOTTLENECK_SCORE = "bottleneck_score";
        public static final String RESOURCE_UTILIZATION = "resource_utilization";
        public static final String VARIANT_COUNT = "variant_count";
        public static final String ANOMALY_RATE = "anomaly_rate";
    }
    
    /**
     * Unités de mesure standards
     */
    public static class MetricUnit {
        public static final String MILLISECONDS = "milliseconds";
        public static final String SECONDS = "seconds";
        public static final String PERCENTAGE = "percentage";
        public static final String COUNT = "count";
        public static final String SCORE = "score";
    }
}
