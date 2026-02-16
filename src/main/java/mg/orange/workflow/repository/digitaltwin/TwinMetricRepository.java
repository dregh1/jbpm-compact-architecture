package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.TwinMetric;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les métriques du Digital Twin
 */
@ApplicationScoped
public class TwinMetricRepository implements PanacheRepository<TwinMetric> {
    
    /**
     * Récupère la dernière valeur d'une métrique
     * @param twinId ID du Digital Twin
     * @param metricName Nom de la métrique
     * @return Optional de la métrique la plus récente
     */
    public Optional<TwinMetric> findLatestMetric(Long twinId, String metricName) {
        return find("digitalTwin.id = ?1 AND metricName = ?2 ORDER BY calculatedAt DESC", 
                    twinId, metricName)
                .firstResultOptional();
    }
    
    /**
     * Liste toutes les métriques d'un twin (dernières valeurs)
     * @param twinId ID du Digital Twin
     * @return Liste des métriques uniques (la plus récente de chaque)
     */
    public List<TwinMetric> findLatestMetricsByTwin(Long twinId) {
        // Récupère la dernière valeur de chaque métrique
        return getEntityManager()
                .createQuery(
                    "SELECT m FROM TwinMetric m WHERE m.digitalTwin.id = :twinId " +
                    "AND m.calculatedAt = (SELECT MAX(m2.calculatedAt) FROM TwinMetric m2 " +
                    "WHERE m2.digitalTwin.id = m.digitalTwin.id AND m2.metricName = m.metricName) " +
                    "ORDER BY m.metricName",
                    TwinMetric.class)
                .setParameter("twinId", twinId)
                .getResultList();
    }
    
    /**
     * Liste l'historique d'une métrique
     * @param twinId ID du Digital Twin
     * @param metricName Nom de la métrique
     * @param limit Nombre de résultats
     * @return Liste des valeurs historiques
     */
    public List<TwinMetric> findMetricHistory(Long twinId, String metricName, int limit) {
        return find("digitalTwin.id = ?1 AND metricName = ?2 ORDER BY calculatedAt DESC",
                    twinId, metricName)
                .page(0, limit)
                .list();
    }
    
    /**
     * Liste les métriques sur une période
     * @param twinId ID du Digital Twin
     * @param from Date début
     * @param to Date fin
     * @return Liste des métriques
     */
    public List<TwinMetric> findMetricsInTimeRange(Long twinId, LocalDateTime from, LocalDateTime to) {
        return find("digitalTwin.id = ?1 AND calculatedAt >= ?2 AND calculatedAt <= ?3 ORDER BY calculatedAt ASC",
                    twinId, from, to).list();
    }
    
    /**
     * Supprime les métriques anciennes (conservation historique limitée)
     * @param before Date limite
     * @return Nombre de métriques supprimées
     */
    public long deleteMetricsBefore(LocalDateTime before) {
        return delete("calculatedAt < ?1", before);
    }
}
