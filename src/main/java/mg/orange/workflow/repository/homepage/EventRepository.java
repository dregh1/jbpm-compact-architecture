package mg.orange.workflow.repository.homepage;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import mg.orange.workflow.model.homepage.Event;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour la gestion des événements
 * Utilise Panache pour les requêtes HQL/JPQL
 */
@ApplicationScoped
public class EventRepository implements PanacheRepository<Event> {

    private static final Logger LOG = Logger.getLogger(EventRepository.class);

    /**
     * Trouve tous les événements d'un utilisateur
     */
    public List<Event> findByUserId(String userId) {
        return find("userId", userId).list();
    }

    /**
     * Trouve les événements d'un utilisateur pour une date donnée
     */
    public List<Event> findByUserIdAndDate(String userId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return find("userId = ?1 AND startTime >= ?2 AND startTime <= ?3 ORDER BY startTime ASC",
                userId, startOfDay, endOfDay).list();
    }

    /**
     * Trouve les événements à venir pour un utilisateur
     */
    public List<Event> findUpcomingEvents(String userId, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(days);

        return find("userId = ?1 AND startTime >= ?2 AND startTime <= ?3 AND status != 'CANCELLED' ORDER BY startTime ASC",
                userId, now, future).list();
    }

    /**
     * Trouve les événements en retard
     */
    public List<Event> findOverdueEvents(String userId) {
        LocalDateTime now = LocalDateTime.now();

        return find("userId = ?1 AND startTime < ?2 AND status = 'PENDING' ORDER BY startTime DESC",
                userId, now).list();
    }

    /**
     * Trouve les événements par type
     */
    public List<Event> findByUserIdAndEventType(String userId, String eventType) {
        return find("userId = ?1 AND eventType = ?2 ORDER BY startTime ASC",
                userId, eventType).list();
    }

    /**
     * Trouve les événements par statut
     */
    public List<Event> findByUserIdAndStatus(String userId, String status) {
        return find("userId = ?1 AND status = ?2 ORDER BY startTime ASC",
                userId, status).list();
    }

    /**
     * Trouve les événements par priorité
     */
    public List<Event> findByUserIdAndPriority(String userId, String priority) {
        return find("userId = ?1 AND priority = ?2 ORDER BY startTime ASC",
                userId, priority).list();
    }

    /**
     * Recherche les événements par mot-clé
     */
    public List<Event> searchByKeyword(String userId, String keyword) {
        return find("userId = ?1 AND (LOWER(title) LIKE ?2 OR LOWER(description) LIKE ?3) ORDER BY startTime ASC",
                userId, "%" + keyword.toLowerCase() + "%", "%" + keyword.toLowerCase() + "%").list();
    }

    /**
     * Compte les événements par type pour un utilisateur
     */
    public long countByUserIdAndEventType(String userId, String eventType) {
        return count("userId = ?1 AND eventType = ?2", userId, eventType);
    }

    /**
     * Trouve tous les événements d'un utilisateur avec filtres avancés
     */
    public List<Event> findWithFilters(String userId, List<String> eventTypes, List<String> statuses,
                                       LocalDateTime startDate, LocalDateTime endDate, String sortBy, String sortOrder) {
        StringBuilder query = new StringBuilder("userId = ?1");
        List<Object> params = new java.util.ArrayList<>();
        params.add(userId);

        int paramIndex = 2;

        if (eventTypes != null && !eventTypes.isEmpty()) {
            query.append(" AND eventType IN (");
            for (int i = 0; i < eventTypes.size(); i++) {
                if (i > 0) query.append(",");
                query.append("?").append(paramIndex++);
                params.add(eventTypes.get(i));
            }
            query.append(")");
        }

        if (statuses != null && !statuses.isEmpty()) {
            query.append(" AND status IN (");
            for (int i = 0; i < statuses.size(); i++) {
                if (i > 0) query.append(",");
                query.append("?").append(paramIndex++);
                params.add(statuses.get(i));
            }
            query.append(")");
        }

        if (startDate != null) {
            query.append(" AND startTime >= ?").append(paramIndex++);
            params.add(startDate);
        }

        if (endDate != null) {
            query.append(" AND startTime <= ?").append(paramIndex++);
            params.add(endDate);
        }

        // Ajouter le tri
        String orderBy = sortBy != null ? sortBy : "startTime";
        String order = "DESC".equals(sortOrder) ? "DESC" : "ASC";
        query.append(" ORDER BY ").append(orderBy).append(" ").append(order);

        return find(query.toString(), params.toArray()).list();
    }

    /**
     * Supprime les événements annulés plus anciens que X jours
     */
    public void deleteCancelledEventsOlderThan(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        delete("status = 'CANCELLED' AND createdAt < ?1", cutoff);
    }

    /**
     * Récupère le nombre total d'événements pour un utilisateur
     */
    public long countByUserId(String userId) {
        return count("userId", userId);
    }

    /**
     * Récupère le compte des événements par statut et type
     */
    public long countByUserIdAndStatusAndEventType(String userId, String status, String eventType) {
        return count("userId = ?1 AND status = ?2 AND eventType = ?3", userId, status, eventType);
    }

    /**
     * Compte les événements par priorité
     */
    public long countByUserIdAndPriority(String userId, String priority) {
        return count("userId = ?1 AND priority = ?2", userId, priority);
    }

    /**
     * Récupère les événements à rappeler (J-1) qui n'ont pas encore envoyé de rappel
     */
    public List<Event> findEventsForReminder(LocalDateTime from, LocalDateTime to) {
        return find("startTime >= ?1 AND startTime <= ?2 AND (status IS NULL OR status NOT IN ('CANCELLED')) AND (reminderSent IS NULL OR reminderSent = false)",
                from, to).list();
    }
}
