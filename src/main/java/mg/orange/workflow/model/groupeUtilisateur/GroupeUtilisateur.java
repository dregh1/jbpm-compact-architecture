package mg.orange.workflow.model.groupeUtilisateur;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

class GroupeUtilisateurId implements Serializable {
    private Long idGroupe;
    private Long idUtilisateur;
    
    public GroupeUtilisateurId() {}
    
    public GroupeUtilisateurId(Long idGroupe, Long idUtilisateur) {
        this.idGroupe = idGroupe;
        this.idUtilisateur = idUtilisateur;
    }
     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupeUtilisateurId that = (GroupeUtilisateurId) o;
        return Objects.equals(idGroupe, that.idGroupe) && 
               Objects.equals(idUtilisateur, that.idUtilisateur);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(idGroupe, idUtilisateur);
    }
}

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(GroupeUtilisateurId.class)
@Table(name = "groupe_utilisateur") 
public class GroupeUtilisateur {
    
    @Id
    @Column(name = "id_groupe")
    private Long idGroupe;
    
    @Id
    @Column(name = "id_utilisateur")
    private Long idUtilisateur;

    public GroupeUtilisateur(Long idGroupe, Long idUtilisateur) {
        this.idGroupe = idGroupe;
        this.idUtilisateur = idUtilisateur;
    }
    
}