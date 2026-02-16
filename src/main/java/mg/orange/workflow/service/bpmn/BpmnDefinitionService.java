package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.bpmn.*;

import java.io.InputStream;
import java.util.List;

/**
 * Service pour la gestion des définitions BPMN (stockage, déploiement, récupération)
 */
public interface BpmnDefinitionService {

    /**
     * Sauvegarde une nouvelle définition BPMN en base de données
     * @param bpmnXml Le contenu XML du processus BPMN
     * @param createdBy L'utilisateur qui crée le processus
     * @return La définition sauvegardée
     */
    BpmnDefinition saveBpmnDefinition(String bpmnXml, String createdBy);

    /**
     * Déploie un processus BPMN vers le système de fichiers pour Kogito
     * @param processId L'ID du processus à déployer
     * @return Le chemin du fichier créé
     */
    String deployBpmnToFileSystem(String processId);

    /**
     * Récupère le XML BPMN d'un processus depuis la base de données
     * @param processId L'ID du processus
     * @return Le contenu XML
     */
    String getBpmnXml(String processId);

    /**
     * Récupère une définition BPMN par son ID de processus
     * @param processId L'ID du processus
     * @return La définition BPMN
     */
    BpmnDefinition getByProcessId(String processId);

    /**
     * Récupère toutes les définitions BPMN
     * @return Liste des définitions
     */
    List<BpmnDefinitionDTO> getAllDefinitions();

    /**
     * Met à jour une définition BPMN existante
     * @param processId L'ID du processus à mettre à jour
     * @param bpmnXml Le nouveau contenu XML
     * @return La définition mise à jour
     */
    BpmnDefinition updateBpmnDefinition(String processId, String bpmnXml);

    /**
     * Supprime une définition BPMN
     * @param processId L'ID du processus à supprimer
     * @return true si la suppression a réussi
     */
    boolean deleteBpmnDefinition(String processId);

    /**
     * Sauvegarde un fichier BPMN uploadé
     * @param inputStream Le flux du fichier uploadé
     * @param fileName Le nom du fichier
     * @param createdBy L'utilisateur qui upload le fichier
     * @return La définition créée
     */
    BpmnDefinition saveUploadedBpmn(InputStream inputStream, String fileName, String createdBy);

    // ======= NOUVELLES MÉTHODES VERSIONING SEMANTIQUE =======

    /**
     * Sauvegarde un brouillon BPMN sans validation (pour travaux en cours)
     * @param bpmnXml Contenu BPMN du brouillon (peut être invalide)
     * @param createdBy Utilisateur qui sauvegarde le brouillon
     * @param changeComment Commentaire décrivant les changements
     * @return Définition sauvegardée sans validation
     */
    BpmnDefinition saveBpmnDraft(String bpmnXml, String createdBy, String changeComment);

    /**
     * Valide un BPMN sans le sauvegarder
     * @param bpmnXml Contenu BPMN à valider
     * @return Résultat de la validation avec erreurs et avertissements
     */
    BpmnValidationResult validateBpmn(String bpmnXml);

    /**
     * Sauvegarde une nouvelle version BPMN avec validation et calcul automatique du versioning
     * @param bpmnXml Contenu BPMN de la nouvelle version (doit être valide)
     * @param createdBy Utilisateur qui crée la version
     * @param changeComment Commentaire décrivant les changements
     * @return Nouvelle version créée avec numéro semver automatique
     * @throws IllegalArgumentException si le BPMN est invalide
     */
    BpmnDefinition saveVersionWithValidation(String bpmnXml, String createdBy, String changeComment);

    /**
     * Sauvegarde une nouvelle version BPMN avec calcul automatique du versioning
     * @deprecated Utilisez saveVersionWithValidation pour plus de clarté
     * @param bpmnXml Contenu BPMN de la nouvelle version
     * @param createdBy Utilisateur qui crée la version
     * @param changeComment Commentaire décrivant les changements
     * @return Nouvelle version créée avec numéro semver automatique
     */
    BpmnDefinition saveVersion(String bpmnXml, String createdBy, String changeComment);

    /**
     * Crée une nouvelle version pour un processus existant
     * @param processId ID du processus à versionner
     * @param bpmnXml Nouveau contenu BPMN
     * @param createdBy Utilisateur créateur
     * @param changeComment Commentaire des changements
     * @return Nouvelle version du processus
     */
    BpmnDefinition createVersion(String processId, String bpmnXml, String createdBy, String changeComment);

    /**
     * Récupère l'historique complet des versions d'un processus
     * @param processId ID du processus
     * @return Liste des entrées d'historique classées par date décroissante
     */
    List<BpmnDefinitionHistoryDTO> getVersionHistory(String processId);

    /**
     * Active une version spécifique comme version courante
     * @param processId ID du processus
     * @param versionSemver Version à activer (format X.Y.Z)
     * @return Définition activée
     */
    BpmnDefinition activateVersion(String processId, String versionSemver);

    /**
     * Désactive une version spécifique (change le statut à INACTIVE)
     * @param processId ID du processus
     * @param versionSemver Version à désactiver (format X.Y.Z)
     * @return Entrée d'historique de la version désactivée
     * @throws IllegalStateException si c'est la dernière version active
     */
    BpmnDefinitionHistory deactivateVersion(String processId, String versionSemver);

    /**
     * Récupère les détails d'une version spécifique
     * @param processId ID du processus
     * @param versionSemver Version recherchée (format X.Y.Z)
     * @return Détails de la version ou null si inexistante
     */
    BpmnDefinitionHistoryDTO getVersionDetails(String processId, String versionSemver);

    /**
     * Génère un résumé des changements entre deux versions
     * @param processId ID du processus
     * @param fromVersion Version précédente (peut être null pour nouvelle création)
     * @param toVersion Version cible
     * @return Description textuelle des changements
     */
    String generateChangeSummary(String processId, String fromVersion, String toVersion);
}
