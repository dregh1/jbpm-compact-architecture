package mg.orange.workflow.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.*;

@Entity
@Table(name = "limite_session")
public class LimiteSession extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Integer limite;

    public LimiteSession() {}

    public LimiteSession(Integer limite) {
        this.limite = limite;
    }
}
