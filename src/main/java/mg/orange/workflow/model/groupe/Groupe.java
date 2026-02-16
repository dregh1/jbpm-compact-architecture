package mg.orange.workflow.model.groupe;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.orange.workflow.model.user.Utilisateur;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "groupe")
public class Groupe extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_groupe")
    private long idGroupe;

    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String nom;

    @Column()
    private String description;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany
    @JoinTable(
        name = "groupe_utilisateur",
        joinColumns = @JoinColumn(name = "id_groupe"),
        inverseJoinColumns = @JoinColumn(name = "id_utilisateur")
    )
    private Set<Utilisateur> utilisateurs = new HashSet<>();

}
