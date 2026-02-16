package mg.orange.workflow.model.chatbot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    private String response;
    private String sessionId;
    private List<String> suggestions;
}
