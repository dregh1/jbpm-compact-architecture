package mg.orange.workflow.model.process;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la requête de création d'une nouvelle version BPMN
 */
public class CreateVersionRequestDTO {

    @NotBlank(message = "Le contenu BPMN est requis")
    private String bpmnXml;

    @NotBlank(message = "Le créateur est requis")
    private String createdBy;

    private String changeComment;

    public CreateVersionRequestDTO() {
    }

    public CreateVersionRequestDTO(String bpmnXml, String createdBy, String changeComment) {
        this.bpmnXml = bpmnXml;
        this.createdBy = createdBy;
        this.changeComment = changeComment;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChangeComment() {
        return changeComment;
    }

    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }
}
