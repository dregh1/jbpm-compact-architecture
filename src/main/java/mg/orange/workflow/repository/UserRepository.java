package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import mg.orange.workflow.model.user.Utilisateur;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheRepository<Utilisateur> {

    public Utilisateur findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public List<Utilisateur> searchUsers(String trigram) {
        if (trigram == null || trigram.isEmpty()) {
            return findAll().list();
        }
        String pattern = "%" + trigram.toLowerCase() + "%";
        return list("LOWER(trigram) LIKE ?1", pattern);
    }

    /**
     * Recherche paginée des utilisateurs avec possibilité de filtrage par trigram
     * @param trigram Le trigram à rechercher (optionnel)
     * @param page Le numéro de page (commence à 0)
     * @param size Le nombre d'éléments par page
     * @param sortField Le champ sur lequel trier
     * @param sortDirection La direction du tri (asc ou desc)
     * @return Liste paginée des utilisateurs
     */
    public List<Utilisateur> searchUsersPaginated(String trigram, int page, int size, String sortField, String sortDirection) {
        Page pageRequest = Page.of(page, size);

        // Construire le tri dynamiquement
        Sort sort;
        if ("desc".equalsIgnoreCase(sortDirection)) {
            sort = Sort.by(sortField).descending();
        } else {
            sort = Sort.by(sortField).ascending();
        }

        if (trigram == null || trigram.trim().isEmpty()) {
            return findAll(sort).page(pageRequest).list();
        } else {
            String pattern = "%" + trigram.toLowerCase().trim() + "%";
            return find("LOWER(trigram) LIKE ?1 OR LOWER(nom) LIKE ?1 OR LOWER(email) LIKE ?1 OR LOWER(numero) LIKE ?1", sort, pattern)
                    .page(pageRequest)
                    .list();
        }
    }

    /**
     * Compte le nombre total d'utilisateurs correspondant au critère de recherche
     * @param trigram Le trigram à rechercher (optionnel)
     * @return Le nombre total d'utilisateurs
     */
    public long countUsers(String trigram) {
        if (trigram == null || trigram.trim().isEmpty()) {
            return count();
        } else {
            String pattern = "%" + trigram.toLowerCase().trim() + "%";
            return count("LOWER(trigram) LIKE ?1 OR LOWER(nom) LIKE ?1 OR LOWER(email) LIKE ?1 OR LOWER(numero) LIKE ?1", pattern);
        }
    }

    public Utilisateur findByTrigram(String trigram) {
        if (trigram == null || trigram.isEmpty()) {
            throw new IllegalStateException("Trigram invalide");
        }
        return find("trigram", trigram).firstResult();
    }

    public Response deleteUserById(long id) {
        Utilisateur user = findById(id);
        if (user != null) {
            delete(user);
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Utilisateur avec l'id " + id + " n'existe pas.")
                    .build();
        }
    }
}
