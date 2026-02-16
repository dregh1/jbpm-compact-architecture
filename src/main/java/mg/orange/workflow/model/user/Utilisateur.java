package mg.orange.workflow.model.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.orange.workflow.model.groupe.Groupe;
import mg.orange.workflow.model.role.Role;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "utilisateur")
public class Utilisateur extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private long idUtilisateur;

    public Long getId() {
        return idUtilisateur;
    }

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true, length = 3)
    private String trigram;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String mdp;

    private String numero;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "id_role")
    private Role role;

    public String getRoles() {
        return role != null ? role.getNom() : null; // Assuming Role has getNom()
    }

    @ManyToMany(mappedBy = "utilisateurs")
    private Set<Groupe> groupes = new HashSet<>();

    public String getNomComplet() {
        return nom; // Assuming nom is the full name, adjust if needed
    }

}
