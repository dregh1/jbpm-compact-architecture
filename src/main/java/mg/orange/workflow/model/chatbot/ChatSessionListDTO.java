package mg.orange.workflow.model.chatbot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionListDTO {
    private Integer totalSessions;
    private List<ChatSessionDTO> sessions;
}
