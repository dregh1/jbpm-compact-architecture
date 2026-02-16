package mg.orange.workflow.repository.chatbot;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import mg.orange.workflow.model.chatbot.ChatSession;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChatSessionRepository implements PanacheRepositoryBase<ChatSession, Long> {
    
    /**
     * Find all sessions for a specific user
     */
    public List<ChatSession> findByUserId(String userId) {
        return list("userId = ?1 ORDER BY updatedAt DESC", userId);
    }
    
    /**
     * Find active sessions for a specific user
     */
    public List<ChatSession> findActiveSessionsByUserId(String userId) {
        return list("userId = ?1 AND isActive = true ORDER BY updatedAt DESC", userId);
    }
    
    /**
     * Find a specific session by ID and user ID
     */
    public Optional<ChatSession> findByIdAndUserId(Long id, String userId) {
        return find("id = ?1 AND userId = ?2", id, userId).firstResultOptional();
    }
    
    /**
     * Count total sessions for a user
     */
    public long countByUserId(String userId) {
        return count("userId = ?1", userId);
    }
    
    /**
     * Count active sessions for a user
     */
    public long countActiveSessionsByUserId(String userId) {
        return count("userId = ?1 AND isActive = true", userId);
    }
}
