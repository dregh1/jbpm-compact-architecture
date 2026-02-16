package mg.orange.workflow.model.homepage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour filtrer les événements
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EventFilterDTO {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("eventTypes")
    private List<String> eventTypes; // Filter par types (TASK, MEETING, DEADLINE, etc.)

    @JsonProperty("statuses")
    private List<String> statuses; // Filter par status

    @JsonProperty("priorities")
    private List<String> priorities; // Filter par priorité

    @JsonProperty("startDate")
    private LocalDate startDate;

    @JsonProperty("endDate")
    private LocalDate endDate;

    @JsonProperty("searchTerm")
    private String searchTerm; // Recherche dans title et description

    @JsonProperty("page")
    private Integer page = 0;

    @JsonProperty("pageSize")
    private Integer pageSize = 20;

    @JsonProperty("sortBy")
    private String sortBy = "startTime"; // startTime, priority, createdAt

    @JsonProperty("sortOrder")
    private String sortOrder = "ASC"; // ASC, DESC
}
