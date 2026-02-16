package mg.orange.workflow.repository.digitaltwin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.digitaltwin.TwinProcessVariant;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les variants de processus (Process Mining)
 */
@ApplicationScoped
public class TwinProcessVariantRepository implements PanacheRepository<TwinProcessVariant> {
    
    /**
     * Trouve un variant par son hash
     * @param twinId ID du Digital Twin
     * @param variantHash Hash du variant
     * @return Optional du variant
     */
    public Optional<TwinProcessVariant> findByHash(Long twinId, String variantHash) {
        return find("digitalTwin.id = ?1 AND variantHash = ?2", twinId, variantHash)
                .firstResultOptional();
    }
    
    /**
     * Liste tous les variants d'un twin (triés par fréquence)
     * @param twinId ID du Digital Twin
     * @return Liste des variants
     */
    public List<TwinProcessVariant> findByTwinId(Long twinId) {
        return find("digitalTwin.id = ?1 ORDER BY frequency DESC", twinId).list();
    }
    
    /**
     * Liste les N variants les plus fréquents
     * @param twinId ID du Digital Twin
     * @param limit Nombre de variants
     * @return Liste des top variants
     */
    public List<TwinProcessVariant> findTopVariants(Long twinId, int limit) {
        return find("digitalTwin.id = ?1 ORDER BY frequency DESC", twinId)
                .page(0, limit)
                .list();
    }
    
    /**
     * Liste les variants rares (fréquence basse)
     * @param twinId ID du Digital Twin
     * @param maxFrequency Fréquence maximum
     * @return Liste des variants rares
     */
    public List<TwinProcessVariant> findRareVariants(Long twinId, int maxFrequency) {
        return find("digitalTwin.id = ?1 AND frequency <= ?2 ORDER BY frequency ASC", 
                    twinId, maxFrequency).list();
    }
    
    /**
     * Compte le nombre de variants différents
     * @param twinId ID du Digital Twin
     * @return Nombre de variants uniques
     */
    public long countVariants(Long twinId) {
        return count("digitalTwin.id", twinId);
    }
    
    /**
     * Calcule le pourcentage de couverture d'un variant
     * @param twinId ID du Digital Twin
     * @param variantId ID du variant
     * @return Pourcentage (0-100)
     */
    public double calculateVariantCoverage(Long twinId, Long variantId) {
        // Total des occurrences de tous les variants
        Long totalFrequency = getEntityManager()
                .createQuery("SELECT SUM(v.frequency) FROM TwinProcessVariant v WHERE v.digitalTwin.id = :twinId", Long.class)
                .setParameter("twinId", twinId)
                .getSingleResult();
        
        if (totalFrequency == null || totalFrequency == 0) {
            return 0.0;
        }
        
        // Fréquence du variant spécifique
        TwinProcessVariant variant = findById(variantId);
        if (variant == null) {
            return 0.0;
        }
        
        return (variant.getFrequency() * 100.0) / totalFrequency;
    }
}
