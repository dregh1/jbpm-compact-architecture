package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.groupeUtilisateur.GroupeUtilisateur;
import mg.orange.workflow.model.user.Utilisateur;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.stream.Collectors;


@ApplicationScoped
public class GroupeUtilisateurRepository implements PanacheRepository<GroupeUtilisateur> {
    
    public List<Long> findGroupesByUtilisateur(Long idUtilisateur) {
        return find("idUtilisateur", idUtilisateur)
                .list()
                .stream()
                .map(GroupeUtilisateur::getIdGroupe)
                .collect(Collectors.toList());
    }

    public List<Utilisateur> findUsersWithoutGroup() {
        return getEntityManager()
                .createQuery("SELECT u FROM Utilisateur u WHERE u.groupes IS EMPTY", Utilisateur.class)
                .getResultList();
    }

    public void removeUserFromAllGroups(Long idUtilisateur) {
        delete("idUtilisateur", idUtilisateur);
    }
}
