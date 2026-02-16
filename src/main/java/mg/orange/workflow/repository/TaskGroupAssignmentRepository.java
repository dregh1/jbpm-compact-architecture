package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.bpmn.TaskGroupAssignment;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Repository pour les affectations tâches-groupes
 */
@ApplicationScoped
public class TaskGroupAssignmentRepository implements PanacheRepository<TaskGroupAssignment> {

    /**
     * Trouve toutes les affectations pour un processus donné
     */
    public List<TaskGroupAssignment> findByProcessId(String processId) {
        return list("processId", processId);
    }

    /**
     * Trouve toutes les affectations pour une tâche donnée
     */
    public List<TaskGroupAssignment> findByProcessIdAndTaskId(String processId, String taskId) {
        return list("processId = ?1 and taskId = ?2", processId, taskId);
    }

    /**
     * Supprime toutes les affectations d'une tâche
     */
    public long deleteByProcessIdAndTaskId(String processId, String taskId) {
        return delete("processId = ?1 and taskId = ?2", processId, taskId);
    }

    /**
     * Supprime une affectation spécifique
     */
    public boolean deleteByProcessIdAndTaskIdAndGroupId(String processId, String taskId, Long groupId) {
        return delete("processId = ?1 and taskId = ?2 and groupe.idGroupe = ?3",
                     processId, taskId, groupId) > 0;
    }
}
