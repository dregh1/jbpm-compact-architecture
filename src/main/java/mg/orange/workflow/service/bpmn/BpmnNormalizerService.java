package mg.orange.workflow.service.bpmn;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service de normalisation BPMN pour corriger automatiquement les erreurs courantes
 * de conformité au schéma BPMN 2.0 avant parsing par Camunda BPM.
 */
@ApplicationScoped
public interface BpmnNormalizerService {

    /**
     * Normalise un BPMN XML en corrigeant les erreurs courantes de conformité.
     *
     * @param bpmnXml Le BPMN XML à normaliser
     * @return Le BPMN XML normalisé
     */
    String normalizeBpmn(String bpmnXml);

    /**
     * Vérifie si un BPMN XML nécessite une normalisation.
     *
     * @param bpmnXml Le BPMN XML à vérifier
     * @return true si une normalisation est nécessaire, false sinon
     */
    boolean needsNormalization(String bpmnXml);
}
