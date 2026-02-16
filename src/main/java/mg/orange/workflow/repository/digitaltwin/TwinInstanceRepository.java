package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.TwinInstance;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les instances virtuelles du Digital Twin
 */
@ApplicationScoped
public class TwinInstanceRepository implements PanacheRepository<TwinInstance> {
    
    @Inject
    EntityManager em;
    
    /**
     * Trouve une instance virtuelle par son ID réel Kogito
     * @param realInstanceId ID instance Kogito
     * @return Optional de l'instance virtuelle
     */
    public Optional<TwinInstance> findByRealInstanceId(String realInstanceId) {
        return find("realInstanceId", realInstanceId).firstResultOptional();
    }
    
    /**
     * Trouve toutes les instances par ID réel (peut y avoir plusieurs twins)
     * @param realInstanceId ID instance Kogito
     * @return Liste des instances virtuelles
     */
    public List<TwinInstance> findAllByRealInstanceId(String realInstanceId) {
        return find("realInstanceId", realInstanceId).list();
    }
    
    /**
     * Trouve une instance par twin et realInstanceId
     * @param twinId ID du twin
     * @param realInstanceId ID instance réelle
     * @return Optional de l'instance
     */
    public Optional<TwinInstance> findByTwinIdAndRealInstanceId(Long twinId, String realInstanceId) {
        return find("digitalTwin.id = ?1 AND realInstanceId = ?2", twinId, realInstanceId)
                .firstResultOptional();
    }
    
    /**
     * Liste toutes les instances d'un twin spécifique
     * @param twinId ID du Digital Twin
     * @return Liste des instances
     */
    public List<TwinInstance> findByTwinId(Long twinId) {
        return find("digitalTwin.id", twinId).list();
    }
    
    /**
     * Liste les instances actives d'un twin
     * @param twinId ID du Digital Twin
     * @return Liste des instances non complétées
     */
    public List<TwinInstance> findActiveTwinInstances(Long twinId) {
        return find("digitalTwin.id = ?1 AND completedAt IS NULL", twinId).list();
    }
    
    /**
     * Liste toutes les instances actives (tous twins confondus)
     * @return Liste des instances en cours
     */
    public List<TwinInstance> findAllActive() {
        return find("completedAt IS NULL").list();
    }
    
    /**
     * Compte les instances actives d'un twin
     * @param twinId ID du Digital Twin
     * @return Nombre d'instances actives
     */
    public long countActiveTwinInstances(Long twinId) {
        return count("digitalTwin.id = ?1 AND completedAt IS NULL", twinId);
    }
    
    /**
     * Compte le total d'instances d'un twin
     * @param twinId ID du Digital Twin
     * @return Nombre total d'instances
     */
    public long countTotalTwinInstances(Long twinId) {
        return count("digitalTwin.id", twinId);
    }
    
    /**
     * Trouve une instance par twin et realInstanceId
     * @param twinId ID du twin
     * @param realInstanceId ID instance réelle
     * @return Optional de l'instance
     */
    public Optional<TwinInstance> findByTwinAndRealInstance(Long twinId, String realInstanceId) {
        return find("digitalTwin.id = ?1 AND realInstanceId = ?2", twinId, realInstanceId)
                .firstResultOptional();
    }
}
