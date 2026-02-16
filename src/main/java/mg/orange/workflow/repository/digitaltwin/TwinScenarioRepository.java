package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.TwinScenario;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Repository pour les scénarios What-If
 */
@ApplicationScoped
public class TwinScenarioRepository implements PanacheRepository<TwinScenario> {
    
    /**
     * Liste les scénarios d'un twin
     * @param twinId ID du Digital Twin
     * @return Liste des scénarios
     */
    public List<TwinScenario> findByTwinId(Long twinId) {
        return find("digitalTwin.id = ?1 ORDER BY createdAt DESC", twinId).list();
    }
    
    /**
     * Liste les scénarios par statut
     * @param twinId ID du Digital Twin
     * @param status Statut du scénario
     * @return Liste des scénarios
     */
    public List<TwinScenario> findByTwinIdAndStatus(Long twinId, TwinScenario.ScenarioStatus status) {
        return find("digitalTwin.id = ?1 AND status = ?2 ORDER BY createdAt DESC", twinId, status).list();
    }
    
    /**
     * Liste les scénarios créés par un utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste des scénarios
     */
    public List<TwinScenario> findByCreatedBy(Long userId) {
        return find("createdBy.id = ?1 ORDER BY createdAt DESC", userId).list();
    }
    
    /**
     * Compte les scénarios d'un twin
     * @param twinId ID du Digital Twin
     * @return Nombre de scénarios
     */
    public long countByTwinId(Long twinId) {
        return count("digitalTwin.id", twinId);
    }
}
