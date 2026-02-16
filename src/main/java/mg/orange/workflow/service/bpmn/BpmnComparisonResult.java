package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.bpmn.VersionChangeType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Résultat de comparaison entre deux versions BPMN
 * Permet de classifier automatiquement le type de changement de version
 */
public class BpmnComparisonResult {

    private final Set<String> addedElements;
    private final Set<String> removedElements;
    private final Set<String> modifiedElements;
    private final Map<String, ElementChange> elementChanges;
    private final Set<String> addedFlows;
    private final Set<String> removedFlows;
    private final Set<String> modifiedFlows;

    public BpmnComparisonResult() {
        this.addedElements = new HashSet<>();
        this.removedElements = new HashSet<>();
        this.modifiedElements = new HashSet<>();
        this.elementChanges = new HashMap<>();
        this.addedFlows = new HashSet<>();
        this.removedFlows = new HashSet<>();
        this.modifiedFlows = new HashSet<>();
    }

    /**
     * Ajoute un élément ajouté
     */
    public void addElement(String elementId, String elementType) {
        addedElements.add(elementId + ":" + elementType);
    }

    /**
     * Ajoute un élément supprimé
     */
    public void removeElement(String elementId, String elementType) {
        removedElements.add(elementId + ":" + elementType);
    }

    /**
     * Ajoute un élément modifié
     */
    public void modifyElement(String elementId, String elementType, String oldValue, String newValue) {
        modifiedElements.add(elementId + ":" + elementType);
        elementChanges.put(elementId, new ElementChange(oldValue, newValue));
    }

    /**
     * Ajoute un flux ajouté
     */
    public void addFlow(String flowId) {
        addedFlows.add(flowId);
    }

    /**
     * Ajoute un flux supprimé
     */
    public void removeFlow(String flowId) {
        removedFlows.add(flowId);
    }

    /**
     * Ajoute un flux modifié
     */
    public void modifyFlow(String flowId, String changeType) {
        modifiedFlows.add(flowId + ":" + changeType);
    }

    /**
     * Classe automatiquement le type de changement selon les règles semver
     */
    public VersionChangeType classifyChangeType() {
        // MAJOR : changements structuraux majeurs
        if (hasMajorChanges()) {
            return VersionChangeType.MAJOR;
        }

        // MINOR : nouveaux éléments ajoutés (sans suppression)
        if (hasMinorChanges()) {
            return VersionChangeType.MINOR;
        }

        // PATCH : modifications mineures ou rien
        return VersionChangeType.PATCH;
    }

    /**
     * Vérifie s'il y a des changements majeurs (breaking changes)
     */
    private boolean hasMajorChanges() {
        // Suppression d'éléments existants (tasks, gateways, events)
        if (!removedElements.isEmpty()) {
            return true;
        }

        // Suppression ou modification de flows (changement de structure)
        if (!removedFlows.isEmpty() || !modifiedFlows.isEmpty()) {
            return true;
        }

        // Modifications d'éléments critiques (processId, properties importantes)
        for (String modifiedElement : modifiedElements) {
            if (isCriticalElement(modifiedElement.split(":")[0])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Vérifie s'il y a des changements mineurs
     */
    private boolean hasMinorChanges() {
        // Nouveaux éléments ajoutés sans suppression
        return !addedElements.isEmpty();
    }

    /**
     * Vérifie si un élément est critique pour la compatibilité
     */
    private boolean isCriticalElement(String elementId) {
        // Liste des éléments considérés comme critiques
        // (tasks principales, gateways importants, process properties, etc.)
        return elementId.toLowerCase().contains("start") ||
               elementId.toLowerCase().contains("end") ||
               elementId.toLowerCase().contains("process") ||
               elementId.toLowerCase().contains("gateway");
    }

    /**
     * Vérifie si des changements ont été détectés
     */
    public boolean hasChanges() {
        return !addedElements.isEmpty() || !removedElements.isEmpty() ||
               !modifiedElements.isEmpty() || !addedFlows.isEmpty() ||
               !removedFlows.isEmpty() || !modifiedFlows.isEmpty();
    }

    /**
     * Génère un résumé des changements
     */
    public String generateSummary() {
        StringBuilder summary = new StringBuilder();

        if (!addedElements.isEmpty()) {
            summary.append(String.format("Ajouté %d élément(s): %s\n", addedElements.size(), addedElements));
        }

        if (!removedElements.isEmpty()) {
            summary.append(String.format("Supprimé %d élément(s): %s\n", removedElements.size(), removedElements));
        }

        if (!modifiedElements.isEmpty()) {
            summary.append(String.format("Modifié %d élément(s): %s\n", modifiedElements.size(), modifiedElements));
        }

        if (!addedFlows.isEmpty()) {
            summary.append(String.format("Ajouté %d flux: %s\n", addedFlows.size(), addedFlows));
        }

        if (!removedFlows.isEmpty()) {
            summary.append(String.format("Supprimé %d flux: %s\n", removedFlows.size(), removedFlows));
        }

        if (!modifiedFlows.isEmpty()) {
            summary.append(String.format("Modifié %d flux: %s\n", modifiedFlows.size(), modifiedFlows));
        }

        return summary.toString().trim();
    }

    // Getters
    public Set<String> getAddedElements() { return addedElements; }
    public Set<String> getRemovedElements() { return removedElements; }
    public Set<String> getModifiedElements() { return modifiedElements; }
    public Map<String, ElementChange> getElementChanges() { return elementChanges; }
    public Set<String> getAddedFlows() { return addedFlows; }
    public Set<String> getRemovedFlows() { return removedFlows; }
    public Set<String> getModifiedFlows() { return modifiedFlows; }

    /**
     * Classe interne pour représenter un changement d'élément
     */
    public static class ElementChange {
        public final String oldValue;
        public final String newValue;

        public ElementChange(String oldValue, String newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public String toString() {
            return String.format("'%s' → '%s'", oldValue, newValue);
        }
    }
}
