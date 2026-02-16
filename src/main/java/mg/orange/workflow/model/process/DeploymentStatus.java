package mg.orange.workflow.model.process;

/**
 * État de déploiement d'un processus BPMN
 */
public enum DeploymentStatus {
    VALIDE("Valide - Prêt à déployer"),
    INVALIDE("Invalide - Contient des erreurs"),
    DEPLOYE("Déployé"),
    NON_DEPLOYE("Non déployé");

    private final String label;

    DeploymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
