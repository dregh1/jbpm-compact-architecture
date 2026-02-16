package mg.orange.workflow.model.homepage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO principal pour le tableau de bord utilisateur (homepage)
 * Contient toutes les données nécessaires à afficher sur la page d'accueil
 */
public class UserHomepageDTO {

    private DailyStats dailyStats;
    private List<PriorityTask> priorityTasks;
    private DayAgenda dayAgenda;

    public UserHomepageDTO() {
    }

    public UserHomepageDTO(DailyStats dailyStats, List<PriorityTask> priorityTasks, DayAgenda dayAgenda) {
        this.dailyStats = dailyStats;
        this.priorityTasks = priorityTasks;
        this.dayAgenda = dayAgenda;
    }

    // Getters and Setters
    public DailyStats getDailyStats() {
        return dailyStats;
    }

    public void setDailyStats(DailyStats dailyStats) {
        this.dailyStats = dailyStats;
    }

    public List<PriorityTask> getPriorityTasks() {
        return priorityTasks;
    }

    public void setPriorityTasks(List<PriorityTask> priorityTasks) {
        this.priorityTasks = priorityTasks;
    }

    public DayAgenda getDayAgenda() {
        return dayAgenda;
    }

    public void setDayAgenda(DayAgenda dayAgenda) {
        this.dayAgenda = dayAgenda;
    }

    /**
     * Statistiques quotidiennes de l'utilisateur
     */
    public static class DailyStats {
        private int totalTasks;        // Nombre total de tâches assignées aujourd'hui
        private int urgentTasks;       // Nombre de tâches urgentes
        private int completedTasks;    // Nombre de tâches terminées aujourd'hui
        private double workloadScore;  // Score de charge de travail (0-100)

        public DailyStats() {
        }

        public DailyStats(int totalTasks, int urgentTasks, int completedTasks, double workloadScore) {
            this.totalTasks = totalTasks;
            this.urgentTasks = urgentTasks;
            this.completedTasks = completedTasks;
            this.workloadScore = workloadScore;
        }

        // Getters and Setters
        public int getTotalTasks() {
            return totalTasks;
        }

        public void setTotalTasks(int totalTasks) {
            this.totalTasks = totalTasks;
        }

        public int getUrgentTasks() {
            return urgentTasks;
        }

        public void setUrgentTasks(int urgentTasks) {
            this.urgentTasks = urgentTasks;
        }

        public int getCompletedTasks() {
            return completedTasks;
        }

        public void setCompletedTasks(int completedTasks) {
            this.completedTasks = completedTasks;
        }

        public double getWorkloadScore() {
            return workloadScore;
        }

        public void setWorkloadScore(double workloadScore) {
            this.workloadScore = workloadScore;
        }
    }

    /**
     * Tâche prioritaire avec actions rapides
     */
    public static class PriorityTask {
        private String taskId;
        private String taskName;
        private String processId;
        private String processInstanceId;
        private LocalDateTime deadline;
        private int priorityScore;      // Score de priorité calculé (0-100)
        private String priorityLevel;   // "HIGH", "MEDIUM", "LOW"
        private List<String> quickActions; // Actions disponibles (claim, complete, etc.)

        public PriorityTask() {
        }

        public PriorityTask(String taskId, String taskName, String processId, String processInstanceId,
                          LocalDateTime deadline, int priorityScore, String priorityLevel, List<String> quickActions) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.processId = processId;
            this.processInstanceId = processInstanceId;
            this.deadline = deadline;
            this.priorityScore = priorityScore;
            this.priorityLevel = priorityLevel;
            this.quickActions = quickActions;
        }

        // Getters and Setters
        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getProcessId() {
            return processId;
        }

        public void setProcessId(String processId) {
            this.processId = processId;
        }

        public String getProcessInstanceId() {
            return processInstanceId;
        }

        public void setProcessInstanceId(String processInstanceId) {
            this.processInstanceId = processInstanceId;
        }

        public LocalDateTime getDeadline() {
            return deadline;
        }

        public void setDeadline(LocalDateTime deadline) {
            this.deadline = deadline;
        }

        public int getPriorityScore() {
            return priorityScore;
        }

        public void setPriorityScore(int priorityScore) {
            this.priorityScore = priorityScore;
        }

        public String getPriorityLevel() {
            return priorityLevel;
        }

        public void setPriorityLevel(String priorityLevel) {
            this.priorityLevel = priorityLevel;
        }

        public List<String> getQuickActions() {
            return quickActions;
        }

        public void setQuickActions(List<String> quickActions) {
            this.quickActions = quickActions;
        }
    }

    /**
     * Agenda personnalisé du jour avec calendrier et tâches
     */
    public static class DayAgenda {
        private List<AgendaEvent> events;
        private List<MilestoneDate> milestoneDates;
        private List<OptimizedSchedule> optimizedSchedule;

        public DayAgenda() {
        }

        public DayAgenda(List<AgendaEvent> events, List<MilestoneDate> milestoneDates, List<OptimizedSchedule> optimizedSchedule) {
            this.events = events;
            this.milestoneDates = milestoneDates;
            this.optimizedSchedule = optimizedSchedule;
        }

        // Getters and Setters
        public List<AgendaEvent> getEvents() {
            return events;
        }

        public void setEvents(List<AgendaEvent> events) {
            this.events = events;
        }

        public List<MilestoneDate> getMilestoneDates() {
            return milestoneDates;
        }

        public void setMilestoneDates(List<MilestoneDate> milestoneDates) {
            this.milestoneDates = milestoneDates;
        }

        public List<OptimizedSchedule> getOptimizedSchedule() {
            return optimizedSchedule;
        }

        public void setOptimizedSchedule(List<OptimizedSchedule> optimizedSchedule) {
            this.optimizedSchedule = optimizedSchedule;
        }
    }

    /**
     * Événement dans l'agenda
     */
    public static class AgendaEvent {
        private String eventId;
        private String title;
        private String type; // "TASK", "MEETING", "DEADLINE", "MILESTONE"
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String description;
        private String status;

        public AgendaEvent() {
        }

        public AgendaEvent(String eventId, String title, String type, LocalDateTime startTime,
                         LocalDateTime endTime, String description, String status) {
            this.eventId = eventId;
            this.title = title;
            this.type = type;
            this.startTime = startTime;
            this.endTime = endTime;
            this.description = description;
            this.status = status;
        }

        // Getters and Setters
        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Date marquante dans le calendrier
     */
    public static class MilestoneDate {
        private LocalDateTime date;
        private String title;
        private String type; // "DEADLINE", "REVIEW", "APPROVAL", "DELIVERY"
        private String priority; // "HIGH", "MEDIUM", "LOW"
        private String description;

        public MilestoneDate() {
        }

        public MilestoneDate(LocalDateTime date, String title, String type, String priority, String description) {
            this.date = date;
            this.title = title;
            this.type = type;
            this.priority = priority;
            this.description = description;
        }

        // Getters and Setters
        public LocalDateTime getDate() {
            return date;
        }

        public void setDate(LocalDateTime date) {
            this.date = date;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Planning optimisé généré automatiquement
     */
    public static class OptimizedSchedule {
        private String timeSlot; // "09:00-10:00", "14:00-15:30", etc.
        private String taskName;
        private String taskId;
        private int estimatedDurationMinutes;
        private String optimizationReason; // Pourquoi cette tâche est planifiée à ce moment

        public OptimizedSchedule() {
        }

        public OptimizedSchedule(String timeSlot, String taskName, String taskId,
                               int estimatedDurationMinutes, String optimizationReason) {
            this.timeSlot = timeSlot;
            this.taskName = taskName;
            this.taskId = taskId;
            this.estimatedDurationMinutes = estimatedDurationMinutes;
            this.optimizationReason = optimizationReason;
        }

        // Getters and Setters
        public String getTimeSlot() {
            return timeSlot;
        }

        public void setTimeSlot(String timeSlot) {
            this.timeSlot = timeSlot;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public int getEstimatedDurationMinutes() {
            return estimatedDurationMinutes;
        }

        public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
            this.estimatedDurationMinutes = estimatedDurationMinutes;
        }

        public String getOptimizationReason() {
            return optimizationReason;
        }

        public void setOptimizationReason(String optimizationReason) {
            this.optimizationReason = optimizationReason;
        }
    }
}