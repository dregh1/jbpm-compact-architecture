package mg.orange.workflow.model.homepage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la création/modification/réponse d'événements
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EventDTO {

    private Long id;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("startTime")
    private LocalDateTime startTime;

    @JsonProperty("endTime")
    private LocalDateTime endTime;

    @JsonProperty("eventType")
    private String eventType; // TASK, MEETING, DEADLINE, OTHER

    @JsonProperty("status")
    private String status; // PENDING, CONFIRMED, CANCELLED, COMPLETED

    @JsonProperty("priority")
    private String priority; // HIGH, MEDIUM, LOW

    @JsonProperty("location")
    private String location;

    @JsonProperty("color")
    private String color;

    @JsonProperty("reminderMinutes")
    private Integer reminderMinutes;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("externalId")
    private String externalId;

    // Propriétés spécifiques aux tâches
    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("processId")
    private String processId;

    @JsonProperty("processInstanceId")
    private String processInstanceId;

    @JsonProperty("priorityScore")
    private Integer priorityScore;

    // Propriétés spécifiques aux réunions
    @JsonProperty("organizer")
    private String organizer;

    @JsonProperty("room")
    private String room;

    @JsonProperty("meetingLink")
    private String meetingLink;

    @JsonProperty("participants")
    private List<String> participants;

    @JsonProperty("isRecurring")
    private Boolean isRecurring;

    @JsonProperty("recurrencePattern")
    private String recurrencePattern;

    // Propriétés spécifiques aux deadlines
    @JsonProperty("relatedEntityId")
    private String relatedEntityId;

    @JsonProperty("relatedEntityType")
    private String relatedEntityType;

    @JsonProperty("isMilestone")
    private Boolean isMilestone;

    /**
     * Validation basique
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty()
            && startTime != null
            && userId != null && !userId.trim().isEmpty()
            && eventType != null && !eventType.trim().isEmpty();
    }

    /**
     * Validation pour la création
     */
    public List<String> validateForCreation() {
        List<String> errors = new java.util.ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            errors.add("Title is required");
        }
        if (startTime == null) {
            errors.add("Start time is required");
        }
        if (userId == null || userId.trim().isEmpty()) {
            errors.add("User ID is required");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            errors.add("Event type is required");
        }

        // Validation spécifique au type
        if (eventType != null) {
            switch (eventType) {
                case "MEETING":
                    if (endTime == null) {
                        errors.add("End time is required for meetings");
                    }
                    break;
                case "TASK":
                    // Les tâches peuvent avoir une durée optionnelle
                    break;
            }
        }

        // Validation temporelle
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            errors.add("Start time must be before end time");
        }

        return errors;
    }
}
