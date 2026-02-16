package mg.orange.workflow.service.process;

import mg.orange.workflow.model.process.ProcessDetailDTO;
import mg.orange.workflow.model.process.ProcessNodeDTO;
import mg.orange.workflow.model.process.ProcessTaskDTO;
import mg.orange.workflow.model.groupe.GroupeResponseDTO;
import mg.orange.workflow.model.process.DiagramInfoDTO;
import mg.orange.workflow.model.process.ProcessDTO;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ProcessService {

    /**
     * Récupère tous les processus disponibles
     * @return Liste de tous les processus
     */
    List<ProcessDTO> getAllProcesses();

    /**
     * Récupère tous les processus avec gestion intelligente multi-versions
     * <p>
     * RÈGLE: Pour chaque processus
     * - Si déployé dans Kogito → afficher la version déployée
     * - Sinon → afficher la dernière version active de l'historique
     * </p>
     * @return Liste complète des processus (déployés + non déployés) avec versions optimales
     */
    List<ProcessDTO> getAllProcessesWithVersionManagement();

    /**
     * Recherche des processus par nom (insensible à la casse)
     * @param name Le nom ou partie du nom à rechercher
     * @return Liste des processus correspondants
     */
    List<ProcessDTO> searchProcessesByName(String name);

    /**
     * Compte le nombre total de processus
     * @return Le nombre de processus
     */
    long countProcesses();

    /**
     * Récupère les détails complets d'un processus (métadonnées + XML + tâches + nœuds + diagramme)
     * @param processId L'ID du processus
     * @return Les détails complets du processus
     */
    ProcessDetailDTO getProcessDetail(String processId);

    /**
     * Récupère le contenu XML brut d'un processus BPMN
     * @param processId L'ID du processus
     * @return Le contenu XML du fichier .bpmn2
     */
    String getProcessDefinitionXml(String processId);

    /**
     * Récupère les informations du diagramme d'un processus
     * @param processId L'ID du processus
     * @return Les informations du diagramme (coordonnées, shapes, edges)
     */
    DiagramInfoDTO getProcessDiagram(String processId);

    /**
     * Récupère la liste des tâches d'un processus
     * @param processId L'ID du processus
     * @return Liste des tâches du processus
     */
    List<ProcessTaskDTO> getProcessTasks(String processId);

    /**
     * Récupère la liste de tous les nœuds d'un processus
     * @param processId L'ID du processus
     * @return Liste des nœuds du processus
     */
    List<ProcessNodeDTO> getProcessNodes(String processId);

    /**
     * Récupère la liste des groupes assignés à une tâche spécifique
     * @param processId L'ID du processus
     * @param taskId L'ID de la tâche
     * @return Liste des groupes assignés ou tableau vide si aucun
     */
    List<GroupeResponseDTO> getAssignedGroups(String processId, String taskId);

    /**
     * Upload a BPMN file to the resources directory
     * @param fileInputStream Input stream of the BPMN file
     * @param filename Name of the file (must end with .bpmn or .bpmn2)
     * @param overwrite If true, overwrite existing file; if false, throw exception if file exists
     * @return Response map with success message and filename
     * @throws Exception if validation fails or IO error occurs
     */
    Map<String, Object> uploadBpmn(InputStream fileInputStream, String filename, boolean overwrite) throws Exception;

    Map<String, Object> deleteBpmn(String filename) throws Exception;

    Map<String, Object> listBpmnFiles() throws Exception;

    /**
     * Récupère un processus par son ID
     * @param processId L'ID du processus
     * @return Le ProcessDTO ou null si non trouvé
     */
    ProcessDTO getProcessById(String processId);

    /**
     * Déploie un processus BPMN (change son statut à DEPLOYE)
     * @param processId L'ID du processus à déployer
     * @return Le ProcessDTO mis à jour
     * @throws IllegalStateException si le processus est déjà déployé
     */
    ProcessDTO deployProcess(String processId);

    /**
     * Retirer le déploiement d'un processus BPMN (change son statut à NON_DEPLOYE)
     * @param processId L'ID du processus à retirer du déploiement
     * @return Le ProcessDTO mis à jour
     * @throws IllegalStateException si le processus n'est pas déployé
     */
    ProcessDTO undeployProcess(String processId);

    /**
     * Valider et réparer un processus BPMN invalide
     * Valide le fichier BPMN sans le déployer et change son statut à VALIDE s'il passe la validation
     * @param processId L'ID du processus à valider
     * @return Le ProcessDTO mis à jour avec le nouveau statut
     * @throws IllegalArgumentException si le processus n'est pas trouvé
     * @throws IllegalStateException si le processus n'est pas en statut INVALIDE
     */
    ProcessDTO validateAndRepairProcess(String processId);
}
