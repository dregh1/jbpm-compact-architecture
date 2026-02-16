package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.bpmn.BpmnDefinitionHistory;
import mg.orange.workflow.model.bpmn.VersionChangeType;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'historique des définitions BPMN
 */
@ApplicationScoped
public class BpmnDefinitionHistoryRepository implements PanacheRepository<BpmnDefinitionHistory> {

    /**
     * Trouve l'historique pour une définition BPMN spécifique
     */
    public List<BpmnDefinitionHistory> findByBpmnDefinitionId(Long bpmnDefinitionId) {
        return list("bpmnDefinitionId", bpmnDefinitionId);
    }

    /**
     * Trouve l'historique pour un processId spécifique
     */
    public List<BpmnDefinitionHistory> findByProcessId(String processId) {
        return list("processId", processId);
    }

    /**
     * Trouve une entrée historique par son ID
     */
    public Optional<BpmnDefinitionHistory> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    /**
     * Trouve les entrées historiques par version semver
     */
    public List<BpmnDefinitionHistory> findByVersionSemver(String versionSemver) {
        return list("versionSemver", versionSemver);
    }

    /**
     * Trouve les entrées historiques par type de changement
     */
    public List<BpmnDefinitionHistory> findByChangeType(VersionChangeType changeType) {
        return list("changeType", changeType);
    }

    /**
     * Compte le nombre d'entrées pour une définition BPMN
     */
    public long countByBpmnDefinitionId(Long bpmnDefinitionId) {
        return count("bpmnDefinitionId", bpmnDefinitionId);
    }

    /**
     * Vérifie l'existence d'une version pour une définition (pour contraintes unique compound)
     */
    public boolean existsByBpmnDefinitionIdAndVersion(Long bpmnDefinitionId, String versionSemver) {
        return count("bpmnDefinitionId = ?1 and versionSemver = ?2", bpmnDefinitionId, versionSemver) > 0;
    }

    /**
     * Trouve la dernière entrée historique pour une définition (la plus récente)
     */
    public Optional<BpmnDefinitionHistory> findLatestByBpmnDefinitionId(Long bpmnDefinitionId) {
        return find("bpmnDefinitionId = ?1 order by createdAt desc", bpmnDefinitionId)
                .firstResultOptional();
    }

    /**
     * Supprime tout l'historique pour une définition BPMN
     * ATTENTION: Utiliser avec précaution
     */
    public void deleteByBpmnDefinitionId(Long bpmnDefinitionId) {
        delete("bpmnDefinitionId", bpmnDefinitionId);
    }

    // ======= MÉTHODES POUR GESTION MULTI-VERSIONS =======

    /**
     * Trouve la dernière version ACTIVE d'un processus
     * Utilisée pour afficher la version la plus récente disponible quand le processus n'est pas déployé
     * @param processId ID du processus
     * @return La version la plus récente avec status ACTIVE, triée par version semver décroissante
     */
    public Optional<BpmnDefinitionHistory> findLatestActiveVersionByProcessId(String processId) {
        return find("processId = ?1 and status = ?2 order by versionSemver desc", 
                    processId, "ACTIVE")
               .firstResultOptional();
    }

    /**
     * Trouve une version spécifique d'un processus par version semver
     * Utilisée pour récupérer les détails d'une version déployée
     * @param processId ID du processus
     * @param versionSemver Version sémantique recherchée (format X.Y.Z)
     * @return L'entrée d'historique correspondante
     */
    public Optional<BpmnDefinitionHistory> findByProcessIdAndVersionSemver(
            String processId, String versionSemver) {
        return find("processId = ?1 and versionSemver = ?2", 
                    processId, versionSemver)
               .firstResultOptional();
    }

    /**
     * Liste toutes les versions ACTIVE d'un processus, triées par date décroissante
     * @param processId ID du processus
     * @return Liste des versions actives, la plus récente en premier
     */
    public List<BpmnDefinitionHistory> findActiveVersionsByProcessId(String processId) {
        return list("processId = ?1 and status = ?2 order by createdAt desc", 
                    processId, "ACTIVE");
    }

    /**
     * Compte le nombre de versions actives pour un processus
     * Utile pour déterminer si un processus a plusieurs versions disponibles
     * @param processId ID du processus
     * @return Nombre de versions avec status ACTIVE
     */
    public long countActiveVersionsByProcessId(String processId) {
        return count("processId = ?1 and status = ?2", processId, "ACTIVE");
    }

    /**
     * Récupère tous les processIds distincts présents dans l'historique
     * Utilisé pour identifier tous les processus qui ont des versions enregistrées
     * @return Liste de tous les IDs de processus uniques
     */
    public List<String> findDistinctProcessIds() {
        return find("SELECT DISTINCT h.processId FROM BpmnDefinitionHistory h")
                .project(String.class)
                .list();
    }
}
