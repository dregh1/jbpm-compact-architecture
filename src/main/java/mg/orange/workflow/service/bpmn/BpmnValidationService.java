package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.bpmn.BpmnValidationResult;

import java.io.InputStream;

public interface BpmnValidationService {

    /**
     * Valide le contenu BPMN passé en paramètre.
     *
     * @param bpmnContent Contenu du fichier BPMN sous forme de chaîne XML
     * @return Résultat détaillé de la validation
     */
    BpmnValidationResult validateBpmn(String bpmnContent);

    /**
     * Valide le fichier BPMN passé en paramètre.
     *
     * @param bpmnContent Contenu du fichier BPMN sous forme de chaîne XML
     * @return Résultat détaillé de la validation
     */
    public BpmnValidationResult validateBpmnFile(InputStream fileInputStream, String filename);
}
