package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.groupe.Groupe;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GroupeRepository implements PanacheRepository<Groupe> {
}
