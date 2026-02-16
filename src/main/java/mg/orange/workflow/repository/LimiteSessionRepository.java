package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.entity.LimiteSession;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LimiteSessionRepository implements PanacheRepository<LimiteSession> {
}
