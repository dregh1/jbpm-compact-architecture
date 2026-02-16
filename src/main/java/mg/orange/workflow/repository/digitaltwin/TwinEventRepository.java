package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.TwinEvent;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour les événements du Digital Twin
 */
@ApplicationScoped
public class TwinEventRepository implements PanacheRepository<TwinEvent> {
    
    /**
     * Liste les événements d'un twin
     * @param twinId ID du Digital Twin
     * @param limit Nombre maximum de résultats
     * @return Liste des événements (plus récents d'abord)
     */
    public List<TwinEvent> findByTwinId(Long twinId, int limit) {
        return find("digitalTwin.id = ?1 ORDER BY timestamp DESC", twinId)
                .page(0, limit)
                .list();
    }
    
    /**
     * Liste les événements d'une instance
     * @param instanceId ID de l'instance
     * @return Liste des événements
     */
    public List<TwinEvent> findByInstanceId(String instanceId) {
        return find("instanceId = ?1 ORDER BY timestamp ASC", instanceId).list();
    }
    
    /**
     * Liste les événements d'un twin sur une période
     * @param twinId ID du Digital Twin
     * @param from Date début
     * @param to Date fin
     * @return Liste des événements
     */
    public List<TwinEvent> findByTwinIdAndTimeRange(Long twinId, LocalDateTime from, LocalDateTime to) {
        return find("digitalTwin.id = ?1 AND timestamp >= ?2 AND timestamp <= ?3 ORDER BY timestamp ASC",
                    twinId, from, to).list();
    }
    
    /**
     * Liste les événements d'un type spécifique
     * @param twinId ID du Digital Twin
     * @param eventType Type d'événement
     * @param limit Nombre maximum
     * @return Liste des événements
     */
    public List<TwinEvent> findByTwinIdAndEventType(Long twinId, String eventType, int limit) {
        return find("digitalTwin.id = ?1 AND eventType = ?2 ORDER BY timestamp DESC", 
                    twinId, eventType)
                .page(0, limit)
                .list();
    }
    
    /**
     * Compte les événements d'un twin
     * @param twinId ID du Digital Twin
     * @return Nombre total d'événements
     */
    public long countByTwinId(Long twinId) {
        return count("digitalTwin.id", twinId);
    }
    
    /**
     * Supprime les événements anciens (nettoyage)
     * @param before Date limite
     * @return Nombre d'événements supprimés
     */
    public long deleteEventsBefore(LocalDateTime before) {
        return delete("timestamp < ?1", before);
    }
}
