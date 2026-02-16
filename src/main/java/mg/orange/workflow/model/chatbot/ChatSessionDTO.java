package mg.orange.workflow.model.chatbot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDTO {
    private String sessionId;
    private Integer messageCount;
    private String lastMessage;
}
