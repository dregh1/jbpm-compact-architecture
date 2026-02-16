package mg.orange.workflow.model.bpmn;

import java.util.List;

// Résultat de validation
public class BpmnValidationResult {
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> infos;
    private final boolean valid;

    public BpmnValidationResult(List<String> errors, List<String> warnings, List<String> infos, boolean valid) {
        this.errors = errors;
        this.warnings = warnings;
        this.infos = infos;
        this.valid = valid;
    }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getInfos() { return infos; }
    public boolean isValid() { return valid; }

    public String getSummary() {
        return String.format("Validation: %s (Erreurs: %d, Avertissements: %d, Infos: %d)",
                valid ? "SUCCÈS" : "ÉCHEC", errors.size(), warnings.size(), infos.size());
    }
}
