package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.DigitalTwin;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les Digital Twins
 */
@ApplicationScoped
public class DigitalTwinRepository implements PanacheRepository<DigitalTwin> {
    
    /**
     * Trouve un Digital Twin par son process definition ID
     * @param processDefinitionId ID du processus BPMN
     * @return Optional du twin trouvé
     */
    public Optional<DigitalTwin> findByProcessDefinitionId(String processDefinitionId) {
        return find("processDefinitionId", processDefinitionId).firstResultOptional();
    }
    
    /**
     * Liste tous les twins actifs
     * @return Liste des twins avec state = ACTIVE
     */
    public List<DigitalTwin> findAllActive() {
        return find("state", DigitalTwin.TwinState.ACTIVE).list();
    }
    
    /**
     * Liste tous les twins d'un état spécifique
     * @param state État du twin
     * @return Liste des twins
     */
    public List<DigitalTwin> findByState(DigitalTwin.TwinState state) {
        return find("state", state).list();
    }
    
    /**
     * Vérifie si un twin existe pour ce processus
     * @param processDefinitionId ID du processus
     * @return true si existe
     */
    public boolean existsByProcessDefinitionId(String processDefinitionId) {
        return count("processDefinitionId", processDefinitionId) > 0;
    }
    
    /**
     * Compte le nombre de twins actifs
     * @return Nombre de twins actifs
     */
    public long countActive() {
        return count("state", DigitalTwin.TwinState.ACTIVE);
    }
}
