package mg.orange.workflow.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurRegisterDTO {

    private String nom;
    private String trigram;
    private String email;
    private String role;
    private String numero;

}
