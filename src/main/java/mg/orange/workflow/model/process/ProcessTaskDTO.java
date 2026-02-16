package mg.orange.workflow.model.process;

import java.util.List;

/**
 * DTO représentant une tâche d'un processus BPMN
 */
public class ProcessTaskDTO {

    private String id;
    private String name;
    private String type; // UserTask, ServiceTask, ScriptTask, etc.
    private String documentation;
    private List<String> assignedGroups;
    private List<String> assignedUsers;
    private List<String> potentialOwners;

    public ProcessTaskDTO() {
    }

    public ProcessTaskDTO(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public List<String> getAssignedGroups() {
        return assignedGroups;
    }

    public void setAssignedGroups(List<String> assignedGroups) {
        this.assignedGroups = assignedGroups;
    }

    public List<String> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(List<String> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }

    public List<String> getPotentialOwners() {
        return potentialOwners;
    }

    public void setPotentialOwners(List<String> potentialOwners) {
        this.potentialOwners = potentialOwners;
    }
}
