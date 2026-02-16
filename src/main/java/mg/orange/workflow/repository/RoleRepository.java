package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.role.Role;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RoleRepository implements PanacheRepository<Role> {

}
