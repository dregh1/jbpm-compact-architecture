package mg.orange.workflow.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurPasswordDTO {

    private String mdp;
    private String newmdp;

}
