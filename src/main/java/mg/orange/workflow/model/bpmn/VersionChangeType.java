package mg.orange.workflow.model.bpmn;

/**
 * Types de changement de version selon Semantic Versioning 2.0
 */
public enum VersionChangeType {

    /**
     * Création initiale du processus
     */
    CREATED,

    /**
     * Changement majeur (breaking changes)
     * Incrémente X dans X.Y.Z
     */
    MAJOR,

    /**
     * Changement mineur (nouvelle fonctionnalité backward compatible)
     * Incrémente Y dans X.Y.Z
     */
    MINOR,

    /**
     * Changement de patch (corrections backward compatible)
     * Incrémente Z dans X.Y.Z
     */
    PATCH;

    /**
     * Récupère une instance depuis un String (insensible à la casse)
     */
    public static VersionChangeType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Type de changement ne peut pas être null ou vide");
        }

        switch (value.trim().toUpperCase()) {
            case "CREATED": return CREATED;
            case "MAJOR": return MAJOR;
            case "MINOR": return MINOR;
            case "PATCH": return PATCH;
            default:
                throw new IllegalArgumentException("Type de changement invalide: " + value +
                        ". Valeurs possibles: CREATED, MAJOR, MINOR, PATCH");
        }
    }
}
