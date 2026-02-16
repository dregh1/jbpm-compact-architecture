package mg.orange.workflow.model.process;

/**
 * DTO représentant un nœud (élément) d'un processus BPMN
 */
public class ProcessNodeDTO {

    private String id;
    private String name;
    private String type; // StartEvent, EndEvent, Task, Gateway, etc.
    private String subType; // Pour les types spécifiques (UserTask, ExclusiveGateway, etc.)

    public ProcessNodeDTO() {
    }

    public ProcessNodeDTO(String id, String name, String type) {
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

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }
}
