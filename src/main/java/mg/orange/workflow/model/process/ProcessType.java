package mg.orange.workflow.model.process;

/**
 * Enum représentant les types de processus BPMN supportés
 */
public enum ProcessType {
    BPMN_2_0("BPMN2.0", "Business Process Model and Notation 2.0"),
    BPMN("BPMN", "Business Process Model and Notation"),
    XML("XML", "Extensible Markup Language");

    private final String code;
    private final String description;

    ProcessType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Retourne le type par défaut des processus
     */
    public static ProcessType getDefault() {
        return BPMN_2_0;
    }

    /**
     * Cherche un ProcessType par son code
     */
    public static ProcessType fromCode(String code) {
        for (ProcessType type : ProcessType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return getDefault();
    }
}
