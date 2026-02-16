package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.user.SessionUtilisateur;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SessionRepository implements PanacheRepository<SessionUtilisateur> {

    /**
     * Trouve une session par son token
     */
    public Optional<SessionUtilisateur> findBySessionToken(String sessionToken) {
        return find("sessionToken", sessionToken).firstResultOptional();
    }

    /**
     * Trouve toutes les sessions actives d'un utilisateur
     */
    public List<SessionUtilisateur> findActiveSessionsByUserId(Long userId) {
        return list("utilisateur.id = ?1 and isActive = true and expiresAt > ?2",
                   userId, LocalDateTime.now());
    }

    /**
     * Trouve toutes les sessions actives
     */
    public List<SessionUtilisateur> findAllActiveSessions() {
        return list("isActive = true and expiresAt > ?1", LocalDateTime.now());
    }

    /**
     * Trouve les sessions expirées
     */
    public List<SessionUtilisateur> findExpiredSessions() {
        return list("expiresAt <= ?1", LocalDateTime.now());
    }

    /**
     * Désactive toutes les sessions d'un utilisateur
     */
    public void deactivateAllUserSessions(Long userId) {
        update("isActive = false where utilisateur.id = ?1", userId);
    }

    /**
     * Supprime les sessions expirées
     */
    public void deleteExpiredSessions() {
        delete("expiresAt <= ?1", LocalDateTime.now());
    }

    /**
     * Compte les sessions actives d'un utilisateur
     */
    public long countActiveSessionsByUser(Long userId) {
        return count("utilisateur.id = ?1 and isActive = true and expiresAt > ?2",
                    userId, LocalDateTime.now());
    }

    /**
     * Trouve une session par refresh token
     */
    public Optional<SessionUtilisateur> findByRefreshToken(String refreshToken) {
        return find("refreshToken", refreshToken).firstResultOptional();
    }
}