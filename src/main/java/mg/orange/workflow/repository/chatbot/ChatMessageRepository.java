package mg.orange.workflow.repository.chatbot;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import mg.orange.workflow.model.chatbot.ChatMessage;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ChatMessageRepository implements PanacheRepositoryBase<ChatMessage, Long> {
    
    /**
     * Find all messages for a specific session
     */
    public List<ChatMessage> findBySessionId(Long sessionId) {
        return list("sessionId = ?1 ORDER BY createdAt ASC, id ASC", sessionId);
    }
    
    /**
     * Count messages in a session
     */
    public long countBySessionId(Long sessionId) {
        return count("sessionId = ?1", sessionId);
    }
    
    /**
     * Delete all messages for a session
     */
    public long deleteBySessionId(Long sessionId) {
        return delete("sessionId = ?1", sessionId);
    }
}
