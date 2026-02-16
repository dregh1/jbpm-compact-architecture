package mg.orange.workflow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import mg.orange.workflow.model.notification.Notification;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {

    /**
     * Find all notifications for a specific user, ordered by creation date (newest first)
     * Limited to the last 100 notifications
     */
    public List<Notification> findByUserId(Long userId) {
        return find("utilisateur.idUtilisateur = ?1", Sort.descending("createdAt"), userId)
                .page(0, 100)
                .list();
    }

    /**
     * Find unread notifications for a specific user
     */
    public List<Notification> findUnreadByUserId(Long userId) {
        return find("utilisateur.idUtilisateur = ?1 AND isRead = false",
                    Sort.descending("createdAt"), userId)
                .list();
    }

    /**
     * Count unread notifications for a specific user
     */
    public long countUnreadByUserId(Long userId) {
        return count("utilisateur.idUtilisateur = ?1 AND isRead = false", userId);
    }

    /**
     * Mark a notification as read
     */
    public boolean markAsRead(Long notificationId, Long userId) {
        return update("isRead = true WHERE idNotification = ?1 AND utilisateur.idUtilisateur = ?2",
                     notificationId, userId) > 0;
    }

    /**
     * Mark all notifications as read for a user
     */
    public long markAllAsRead(Long userId) {
        return update("isRead = true WHERE utilisateur.idUtilisateur = ?1 AND isRead = false", userId);
    }

    /**
     * Delete a notification (only if it belongs to the user)
     */
    public boolean deleteByIdAndUserId(Long notificationId, Long userId) {
        return delete("idNotification = ?1 AND utilisateur.idUtilisateur = ?2",
                     notificationId, userId) > 0;
    }

    /**
     * Delete all notifications for a user
     */
    public long deleteAllByUserId(Long userId) {
        return delete("utilisateur.idUtilisateur", userId);
    }

    /**
     * Find a notification by ID and user ID (for security check)
     */
    public Notification findByIdAndUserId(Long notificationId, Long userId) {
        return find("idNotification = ?1 AND utilisateur.idUtilisateur = ?2",
                   notificationId, userId)
                .firstResult();
    }
}
