package mg.orange.workflow.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mg.orange.workflow.model.role.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurDTO {

    private Long idUtilisateur;
    private String nom;
    private String trigram;
    private String email;
    private String numero;
    private Role role;

}
