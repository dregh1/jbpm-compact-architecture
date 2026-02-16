package mg.orange.workflow.model.groupe;

import lombok.Data;

@Data
public class GroupeResponseDTO {

    private long idGroupe;
    private String nom;
    private String description;

    public GroupeResponseDTO(Groupe groupe) {
        this.idGroupe = groupe.getIdGroupe();
        this.nom = groupe.getNom();
        this.description = groupe.getDescription();
    }

}
