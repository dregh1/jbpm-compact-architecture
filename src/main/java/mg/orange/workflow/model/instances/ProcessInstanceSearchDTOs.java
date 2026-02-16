package mg.orange.workflow.model.instances;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTOs pour la recherche d'instances de processus
 */
public class ProcessInstanceSearchDTOs {

    /**
     * Critères de recherche pour les timers
     */
    public static class TimerSearchCriteria {
        public String processInstanceId;     // ID de l'instance (optionnel, pour filtrer par instance)
        public String timerId;               // ID du timer (optionnel)
        public String timerName;             // Nom du timer (optionnel)
        public Boolean activeOnly = true;    // Seulement les timers actifs
        public Integer page = 0;
        public Integer size = 20;
        public String sortBy = "dueDate";    // dueDate, createdDate, timerName
        public String sortOrder = "ASC";
    }

    /**
     * DTO pour un timer
     */
    public static class TimerInfo {
        public String id;                    // ID unique du timer
        public String processInstanceId;     // ID de l'instance de processus
        public String processId;             // ID du processus
        public String timerName;             // Nom du timer
        public String timerType;             // Type: boundary, intermediate, start

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime createdDate;    // Date de création

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime dueDate;        // Date d'échéance

        public Long delay;                   // Délai en millisecondes
        public String delayExpression;       // Expression du délai (ISO 8601)
        public Boolean active;               // Timer actif ?

        // Calculé
        public Long remainingTimeMs;         // Temps restant en ms
        public String remainingTimeFormatted; // Temps restant formaté
        public Boolean expiringSoon;         // Expire bientôt (< 1h) ?
        public String status;                // "ACTIVE", "EXPIRED", "CANCELLED"
    }

    /**
     * Réponse pour la recherche de timers
     */
    public static class TimerSearchResponse {
        public java.util.List<TimerInfo> content;
        public int page;
        public int size;
        public long totalElements;
        public int totalPages;
        public boolean first;
        public boolean last;

        // Statistiques
        public TimerStatistics statistics;
    }

    /**
     * Statistiques sur les timers
     */
    public static class TimerStatistics {
        public long totalTimers;
        public long activeTimers;
        public long expiredTimers;
        public long expiringSoonTimers;     // < 1 heure
        public Map<String, Long> timersByProcess;
        public Double averageDelayMs;
    }

    /**
     * Critères de recherche
     */
    public static class SearchCriteria {

        public String processId;              // ID du processus (ex: "orders", "orderItems")
        public Integer state;                 // État: 0=PENDING, 1=ACTIVE, 2=COMPLETED, 3=ABORTED, 4=SUSPENDED

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime startTimeFrom;   // Date de début >= cette date

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime startTimeTo;     // Date de début <= cette date

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime endTimeFrom;     // Date de fin >= cette date

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime endTimeTo;       // Date de fin <= cette date

        public String businessKey;            // Clé métier
        public String instanceId;             // ID de l'instance (recherche exacte)

        // Pagination
        public Integer page = 0;
        public Integer size = 20;

        // Tri
        public String sortBy = "startTime";   // Champ de tri: startTime, endTime, processId, state
        public String sortOrder = "DESC";     // ASC ou DESC
    }

    /**
     * Résultat de recherche d'une instance
     */
    public static class ProcessInstanceResult {

        public String id;                     // ID de l'instance
        public String processId;              // ID du processus
        public String processName;            // Nom du processus
        public Integer state;                 // État numérique
        public String stateName;              // État en texte (ACTIVE, COMPLETED, etc.)
        public String businessKey;            // Clé métier

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime startTime;       // Date de début

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime endTime;         // Date de fin

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        public LocalDateTime lastUpdateTime;  // Dernière mise à jour

        public Map<String, Object> variables; // Variables du processus
        public String endpoint;               // Endpoint API

        // Champs calculés
        public Long durationMs;               // Durée en millisecondes
        public String durationFormatted;      // Durée formatée (ex: "2h 30m")
        public Boolean isRunning;             // Instance en cours?
        public Boolean isCompleted;           // Instance terminée?
    }

    /**
     * Réponse paginée
     */
    public static class SearchResponse {

        public java.util.List<ProcessInstanceResult> content;
        public int page;
        public int size;
        public long totalElements;
        public int totalPages;
        public boolean first;
        public boolean last;

        // Statistiques optionnelles
        public SearchStatistics statistics;
    }

    /**
     * Statistiques sur les résultats
     */
    public static class SearchStatistics {

        public long totalInstances;
        public long activeInstances;
        public long completedInstances;
        public long abortedInstances;
        public long suspendedInstances;

        public Map<String, Long> instancesByProcess;  // Nombre d'instances par processus
        public Double averageDurationMs;              // Durée moyenne en ms
    }

    /**
     * Enum pour les états de processus
     */
    public enum ProcessState {
        PENDING(0, "En attente"),
        ACTIVE(1, "Actif"),
        COMPLETED(2, "Terminé"),
        ABORTED(3, "Abandonné"),
        SUSPENDED(4, "Suspendu");

        private final int code;
        private final String label;

        ProcessState(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static String getLabel(Integer code) {
            if (code == null) return "Inconnu";
            for (ProcessState state : values()) {
                if (state.code == code) {
                    return state.label;
                }
            }
            return "Inconnu (" + code + ")";
        }
    }
}