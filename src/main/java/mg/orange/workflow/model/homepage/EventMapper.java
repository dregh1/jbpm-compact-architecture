package mg.orange.workflow.model.homepage;

/**
 * Mapper pour convertir les entités Event en DTO et inversement
 */
public class EventMapper {

    /**
     * Convertit une entité Event en EventDTO
     */
    public static EventDTO toDTO(Event event) {
        if (event == null) {
            return null;
        }

        EventDTO dto = new EventDTO();
        dto.setId(event.id);
        dto.setUserId(event.getUserId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        dto.setEventType(event.getEventType());
        dto.setStatus(event.getStatus());
        dto.setPriority(event.getPriority());
        dto.setLocation(event.getLocation());
        dto.setColor(event.getColor());
        dto.setReminderMinutes(event.getReminderMinutes());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());
        dto.setCreatedBy(event.getCreatedBy());
        dto.setExternalId(event.getExternalId());

        // Champs spécifiques au type
        if (event instanceof TaskEvent) {
            TaskEvent taskEvent = (TaskEvent) event;
            dto.setTaskId(taskEvent.getTaskId());
            dto.setProcessId(taskEvent.getProcessId());
            dto.setProcessInstanceId(taskEvent.getProcessInstanceId());
            dto.setPriorityScore(taskEvent.getPriorityScore());
        } else if (event instanceof MeetingEvent) {
            MeetingEvent meetingEvent = (MeetingEvent) event;
            dto.setOrganizer(meetingEvent.getOrganizer());
            dto.setRoom(meetingEvent.getRoom());
            dto.setMeetingLink(meetingEvent.getMeetingLink());
            dto.setParticipants(meetingEvent.getParticipants());
            dto.setIsRecurring(meetingEvent.getIsRecurring());
            dto.setRecurrencePattern(meetingEvent.getRecurrencePattern());
        } else if (event instanceof DeadlineEvent) {
            DeadlineEvent deadlineEvent = (DeadlineEvent) event;
            dto.setRelatedEntityId(deadlineEvent.getRelatedEntityId());
            dto.setRelatedEntityType(deadlineEvent.getRelatedEntityType());
            dto.setIsMilestone(deadlineEvent.getIsMilestone());
        }

        return dto;
    }

    /**
     * Convertit un EventDTO en entité Event
     */
    public static Event toEntity(EventDTO dto) {
        if (dto == null) {
            return null;
        }

        Event event;

        switch (dto.getEventType().toUpperCase()) {
            case "TASK":
                TaskEvent taskEvent = new TaskEvent(dto.getUserId(), dto.getTitle(), dto.getDescription());
                taskEvent.setTaskId(dto.getTaskId());
                taskEvent.setProcessId(dto.getProcessId());
                taskEvent.setProcessInstanceId(dto.getProcessInstanceId());
                taskEvent.setPriorityScore(dto.getPriorityScore());
                event = taskEvent;
                break;

            case "MEETING":
                MeetingEvent meetingEvent = new MeetingEvent(dto.getUserId(), dto.getTitle(), dto.getDescription());
                meetingEvent.setOrganizer(dto.getOrganizer() != null ? dto.getOrganizer() : dto.getUserId());
                meetingEvent.setRoom(dto.getRoom());
                meetingEvent.setMeetingLink(dto.getMeetingLink());
                meetingEvent.setParticipants(dto.getParticipants() != null ? dto.getParticipants() : new java.util.ArrayList<>());
                meetingEvent.setIsRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false);
                meetingEvent.setRecurrencePattern(dto.getRecurrencePattern());
                event = meetingEvent;
                break;

            case "DEADLINE":
                DeadlineEvent deadlineEvent = new DeadlineEvent(dto.getUserId(), dto.getTitle(), dto.getDescription());
                deadlineEvent.setRelatedEntityId(dto.getRelatedEntityId());
                deadlineEvent.setRelatedEntityType(dto.getRelatedEntityType());
                deadlineEvent.setIsMilestone(dto.getIsMilestone() != null ? dto.getIsMilestone() : false);
                event = deadlineEvent;
                break;

            default:
                event = new Event();
        }

        // Champs communs supplémentaires
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        event.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
        event.setLocation(dto.getLocation());
        event.setColor(dto.getColor() != null ? dto.getColor() : "#3366CC");
        event.setReminderMinutes(dto.getReminderMinutes());
        event.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : dto.getUserId());
        event.setExternalId(dto.getExternalId());

        return event;
    }
}
