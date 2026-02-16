package mg.orange.workflow.model.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_utilisateur")
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Entité représentant une session utilisateur")
public class SessionUtilisateur extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    @Schema(description = "ID unique de la session")
    private Long sessionId;

    @Column(name = "session_token", nullable = false, unique = true)
    @Schema(description = "Token unique de la session")
    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @Schema(description = "Utilisateur propriétaire de la session")
    private Utilisateur utilisateur;

    @Column(name = "ip_address")
    @Schema(description = "Adresse IP du client lors de la connexion")
    private String ipAddress;

    @Column(name = "user_agent")
    @Schema(description = "User-Agent du navigateur/client")
    private String userAgent;

    @Column(name = "device_info")
    @Schema(description = "Informations sur l'appareil")
    private String deviceInfo;

    @Column(name = "location_info")
    @Schema(description = "Informations de localisation (optionnel)")
    private String locationInfo;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Date de création de la session")
    private LocalDateTime createdAt;

    @Column(name = "last_activity", nullable = false)
    @Schema(description = "Dernière activité de la session")
    private LocalDateTime lastActivity;

    @Column(name = "expires_at", nullable = false)
    @Schema(description = "Date d'expiration de la session")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Schema(description = "Statut actif de la session")
    private Boolean isActive = true;

    @Column(name = "refresh_token")
    @Schema(description = "Token de rafraîchissement JWT")
    private String refreshToken;

    @Column(name = "access_token")
    @Schema(description = "Token d'accès JWT actuel")
    private String accessToken;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = createdAt;
        if (sessionToken == null) {
            sessionToken = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastActivity = LocalDateTime.now();
    }

    /**
     * Vérifie si la session est expirée
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Met à jour la dernière activité
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Invalide la session
     */
    public void invalidate() {
        this.isActive = false;
        this.expiresAt = LocalDateTime.now();
    }

    /**
     * Prolonge la session
     */
    public void extendExpiration(LocalDateTime newExpiration) {
        this.expiresAt = newExpiration;
        this.updateActivity();
    }
}
