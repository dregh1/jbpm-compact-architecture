package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.bpmn.BpmnDefinition;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les définitions BPMN
 * Support étendu pour les recherches par version sémantique
 */
@ApplicationScoped
public class BpmnDefinitionRepository implements PanacheRepository<BpmnDefinition> {

    /**
     * Trouve une définition BPMN par son ID de processus
     */
    public Optional<BpmnDefinition> findByProcessId(String processId) {
        return find("processId", processId).firstResultOptional();
    }

    /**
     * Vérifie si un processus existe déjà
     */
    public boolean existsByProcessId(String processId) {
        return count("processId", processId) > 0;
    }

    /**
     * Supprime une définition par son ID de processus
     */
    public boolean deleteByProcessId(String processId) {
        return delete("processId", processId) > 0;
    }

    // ======= EXTENSIONS VERSIONNING SÉMANTIQUE =======

    /**
     * Trouve toutes les définitions BPMN avec une version semver spécifique
     * @param versionSemver Version sémantique (format X.Y.Z)
     * @return Liste des définitions ayant cette version
     */
    public List<BpmnDefinition> findByVersionSemver(String versionSemver) {
        return list("versionSemver", versionSemver);
    }

    /**
     * Trouve une définition par processId et version semver
     * @param processId ID du processus
     * @param versionSemver Version sémantique recherchée
     * @return Définition trouvée ou Optional.empty()
     */
    public Optional<BpmnDefinition> findByProcessIdAndVersionSemver(String processId, String versionSemver) {
        return find("processId = ?1 and versionSemver = ?2", processId, versionSemver).firstResultOptional();
    }

    /**
     * Vérifie l'existence d'une version semver pour un processus
     * @param processId ID du processus
     * @param versionSemver Version sémantique à vérifier
     * @return true si la version existe
     */
    public boolean existsByProcessIdAndVersionSemver(String processId, String versionSemver) {
        return count("processId = ?1 and versionSemver = ?2", processId, versionSemver) > 0;
    }

    /**
     * Trouve toutes les versions d'un processus
     * @param processId ID du processus
     * @return Liste des définitions triées par version décroissante (plus récente en premier)
     */
    public List<BpmnDefinition> findVersionsByProcessId(String processId) {
        return find("processId = ?1 order by versionSemver desc", processId).list();
    }

    /**
     * Trouve la version active d'un processus (celle sans version semver définie ou la plus récente)
     * @param processId ID du processus
     * @return Version active ou null si aucune version
     */
    public Optional<BpmnDefinition> findActiveVersion(String processId) {
        // Recherche d'abord la version active (celle sans version semver)
        Optional<BpmnDefinition> active = find("processId = ?1 and (versionSemver is null or versionSemver = '')", processId).firstResultOptional();
        if (active.isPresent()) {
            return active;
        }

        // Si pas de version "active" explicite, prendre la plus récente par version semver
        return find("processId = ?1 order by versionSemver desc", processId).firstResultOptional();
    }

    /**
     * Compte le nombre total de versions pour un processus
     * @param processId ID du processus
     * @return Nombre de versions
     */
    public long countVersionsByProcessId(String processId) {
        return count("processId", processId);
    }

    /**
     * Recherche toutes les définitions BPMN qui n'ont pas de version semver définie
     * (utile pour migration des données existantes)
     * @return Liste des définitions sans version semver
     */
    public List<BpmnDefinition> findDefinitionsWithoutVersionSemver() {
        return list("versionSemver is null or versionSemver = ''");
    }

    /**
     * Recherche toutes les définitions BPMN qui ont une version semver définie
     * @return Liste des définitions avec version semver
     */
    public List<BpmnDefinition> findDefinitionsWithVersionSemver() {
        return list("versionSemver is not null and versionSemver != ''");
    }

    /**
     * Trouve les processus qui utilisent une version semver spécifique
     * @param versionSemver Version sémantique
     * @return Liste des processus utilisant cette version
     */
    public List<BpmnDefinition> findProcessesByVersionSemver(String versionSemver) {
        return list("versionSemver", versionSemver);
    }
}
