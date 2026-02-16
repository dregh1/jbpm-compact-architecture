package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.TwinOptimization;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Repository pour les optimisations suggérées
 */
@ApplicationScoped
public class TwinOptimizationRepository implements PanacheRepository<TwinOptimization> {
    
    /**
     * Liste les optimisations d'un twin
     * @param twinId ID du Digital Twin
     * @return Liste des optimisations
     */
    public List<TwinOptimization> findByTwinId(Long twinId) {
        return find("digitalTwin.id = ?1 ORDER BY suggestedAt DESC", twinId).list();
    }
    
    /**
     * Liste les optimisations par statut
     * @param twinId ID du Digital Twin
     * @param status Statut de l'optimisation
     * @return Liste des optimisations
     */
    public List<TwinOptimization> findByTwinIdAndStatus(Long twinId, TwinOptimization.OptimizationStatus status) {
        return find("digitalTwin.id = ?1 AND status = ?2 ORDER BY suggestedAt DESC", twinId, status).list();
    }
    
    /**
     * Liste les optimisations suggérées (non traitées)
     * @param twinId ID du Digital Twin
     * @return Liste des optimisations SUGGESTED
     */
    public List<TwinOptimization> findPendingOptimizations(Long twinId) {
        return find("digitalTwin.id = ?1 AND status = ?2 ORDER BY suggestedAt DESC", 
                    twinId, TwinOptimization.OptimizationStatus.SUGGESTED).list();
    }
    
    /**
     * Liste les optimisations par type
     * @param twinId ID du Digital Twin
     * @param optimizationType Type d'optimisation
     * @return Liste des optimisations
     */
    public List<TwinOptimization> findByType(Long twinId, String optimizationType) {
        return find("digitalTwin.id = ?1 AND optimizationType = ?2 ORDER BY suggestedAt DESC",
                    twinId, optimizationType).list();
    }
    
    /**
     * Compte les optimisations pendantes d'un twin
     * @param twinId ID du Digital Twin
     * @return Nombre d'optimisations SUGGESTED
     */
    public long countPending(Long twinId) {
        return count("digitalTwin.id = ?1 AND status = ?2", 
                     twinId, TwinOptimization.OptimizationStatus.SUGGESTED);
    }
}
