package mg.orange.workflow.model.homepage;

import lombok.*;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité pour les réunions
 */
@Entity
@DiscriminatorValue("MEETING")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class MeetingEvent extends Event {

    @Column(name = "organizer")
    private String organizer;

    @Column(name = "room")
    private String room;

    @Column(name = "meeting_link")
    private String meetingLink; // URL pour meeting virtuel

    @ElementCollection
    @CollectionTable(name = "meeting_participants", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "participant")
    private List<String> participants = new ArrayList<>();

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "recurrence_pattern")
    private String recurrencePattern; // DAILY, WEEKLY, MONTHLY

    public MeetingEvent(String userId, String title, String description) {
        super();
        setUserId(userId);
        setTitle(title);
        setDescription(description);
        setEventType("MEETING");
        setOrganizer(userId);
    }
}
