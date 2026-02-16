package mg.orange.workflow.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mg.orange.workflow.model.groupe.Groupe;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurGroupeDTO {

    private Long idUtilisateur;
    private String nom;
    private String trigram;
    private String email;
    private String numero;
    private Set<String> groupes;

    public UtilisateurGroupeDTO(Utilisateur utilisateur) {
        this.idUtilisateur = utilisateur.getIdUtilisateur();
        this.nom = utilisateur.getNom();
        this.trigram = utilisateur.getTrigram();
        this.email = utilisateur.getEmail();
        this.numero = utilisateur.getNumero();
        this.groupes = utilisateur.getGroupes()
                .stream()
                .map(Groupe::getNom)
                .collect(Collectors.toSet());
    }
}
