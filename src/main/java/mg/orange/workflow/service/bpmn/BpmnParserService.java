package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.process.DiagramInfoDTO;
import mg.orange.workflow.model.process.ProcessNodeDTO;
import mg.orange.workflow.model.process.ProcessTaskDTO;

import java.util.List;

/**
 * Service pour parser et extraire des informations des fichiers BPMN
 */
public interface BpmnParserService {

    /**
     * Extrait toutes les tâches d'un processus BPMN
     * @param bpmnXml Le contenu XML du processus
     * @return Liste des tâches
     */
    List<ProcessTaskDTO> parseProcessTasks(String bpmnXml);

    /**
     * Extrait tous les nœuds (activités, events, gateways) d'un processus
     * @param bpmnXml Le contenu XML du processus
     * @return Liste des nœuds
     */
    List<ProcessNodeDTO> parseProcessNodes(String bpmnXml);

    /**
     * Extrait les informations du diagramme (coordonnées, shapes, edges)
     * @param bpmnXml Le contenu XML du processus
     * @return Informations du diagramme
     */
    DiagramInfoDTO parseDiagramInfo(String bpmnXml);

    /**
     * Valide un fichier BPMN
     * @param bpmnXml Le contenu XML à valider
     * @return true si valide, false sinon
     */
    boolean validateBpmn(String bpmnXml);

    /**
     * Extrait l'ID du processus depuis le XML BPMN
     * @param bpmnXml Le contenu XML du processus
     * @return L'ID du processus
     */
    String extractProcessId(String bpmnXml);

    /**
     * Extrait le nom du processus depuis le XML BPMN
     * @param bpmnXml Le contenu XML du processus
     * @return Le nom du processus
     */
    String extractProcessName(String bpmnXml);
}
