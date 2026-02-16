package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.TwinAnomaly;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Repository pour les anomalies détectées
 */
@ApplicationScoped
public class TwinAnomalyRepository implements PanacheRepository<TwinAnomaly> {
    
    /**
     * Liste les anomalies d'un twin
     * @param twinId ID du Digital Twin
     * @return Liste des anomalies
     */
    public List<TwinAnomaly> findByTwinId(Long twinId) {
        return find("digitalTwin.id = ?1 ORDER BY detectedAt DESC", twinId).list();
    }
    
    /**
     * Liste les anomalies non résolues d'un twin
     * @param twinId ID du Digital Twin
     * @return Liste des anomalies actives
     */
    public List<TwinAnomaly> findUnresolvedByTwinId(Long twinId) {
        return find("digitalTwin.id = ?1 AND resolvedAt IS NULL ORDER BY detectedAt DESC", twinId).list();
    }
    
    /**
     * Liste les anomalies d'une instance spécifique
     * @param instanceId ID de l'instance
     * @return Liste des anomalies
     */
    public List<TwinAnomaly> findByInstanceId(String instanceId) {
        return find("instanceId = ?1 ORDER BY detectedAt DESC", instanceId).list();
    }
    
    /**
     * Liste les anomalies par gravité
     * @param twinId ID du Digital Twin
     * @param severity Gravité
     * @return Liste des anomalies
     */
    public List<TwinAnomaly> findBySeverity(Long twinId, TwinAnomaly.AnomalySeverity severity) {
        return find("digitalTwin.id = ?1 AND severity = ?2 ORDER BY detectedAt DESC", 
                    twinId, severity).list();
    }
    
    /**
     * Liste les anomalies critiques non résolues
     * @param twinId ID du Digital Twin
     * @return Liste des anomalies CRITICAL actives
     */
    public List<TwinAnomaly> findUnresolvedCritical(Long twinId) {
        return find("digitalTwin.id = ?1 AND severity = ?2 AND resolvedAt IS NULL ORDER BY detectedAt DESC",
                    twinId, TwinAnomaly.AnomalySeverity.CRITICAL).list();
    }
    
    /**
     * Compte les anomalies non résolues
     * @param twinId ID du Digital Twin
     * @return Nombre d'anomalies actives
     */
    public long countUnresolved(Long twinId) {
        return count("digitalTwin.id = ?1 AND resolvedAt IS NULL", twinId);
    }
    
    /**
     * Compte les anomalies par gravité
     * @param twinId ID du Digital Twin
     * @param severity Gravité
     * @return Nombre d'anomalies
     */
    public long countBySeverity(Long twinId, TwinAnomaly.AnomalySeverity severity) {
        return count("digitalTwin.id = ?1 AND severity = ?2 AND resolvedAt IS NULL", twinId, severity);
    }
}
