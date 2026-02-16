package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.bpmn.VersionChangeType;

/**
 * Service responsable de la gestion automatique du versioning sémantique BPMN
 * Analyse les différences entre versions pour classifier automatiquement le type de changement
 */
public interface VersionManagementService {

    /**
     * Analyse deux contenus BPMN et calcule le type de changement approprié
     * @param oldBpmnXml Contenu BPMN de la version actuelle
     * @param newBpmnXml Contenu BPMN de la nouvelle version
     * @return Type de changement selon Semantic Versioning (MAJOR/MINOR/PATCH)
     */
    VersionChangeType calculateChangeType(String oldBpmnXml, String newBpmnXml);

    /**
     * Compare deux BPMN et retourne le résultat détaillé de comparaison
     * @param oldBpmnXml BPMN ancien
     * @param newBpmnXml BPMN nouveau
     * @return Résultat détaillé de la comparaison avec classification automatique
     */
    BpmnComparisonResult compareBpmnVersions(String oldBpmnXml, String newBpmnXml);

    /**
     * Classe un résultat de comparaison selon les règles de Semantic Versioning
     * @param comparison Résultat de la comparaison des BPMN
     * @return Type de changement classifié
     */
    VersionChangeType classifyChangeType(BpmnComparisonResult comparison);

    /**
     * Vérifie si deux BPMN sont réellement différents
     * @param oldBpmnXml BPMN ancien
     * @param newBpmnXml BPMN nouveau
     * @return true si des changements significatifs ont été détectés
     */
    boolean hasSignificantChanges(String oldBpmnXml, String newBpmnXml);

    /**
     * Génère une description automatique des changements
     * @param comparison Résultat de comparaison
     * @return Description textuelle des changements
     */
    String generateChangeDescription(BpmnComparisonResult comparison);

    /**
     * Valide qu'une nouvelle version est compatible avec le versioning sémantique
     * @param currentVersion Version actuelle
     * @param newChangeType Type de changement prévu
     * @return true si valide, lève exception sinon
     * @throws IllegalArgumentException si le type de changement n'est pas approprié
     */
    boolean validateVersioningLogic(String currentVersion, VersionChangeType newChangeType);
}
