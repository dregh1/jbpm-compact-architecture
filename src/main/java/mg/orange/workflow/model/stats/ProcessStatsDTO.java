package mg.orange.workflow.model.stats;

import java.util.List;
import java.util.Map;

public class ProcessStatsDTO {
    private Long totalInstances;
    private Long activeInstances;
    private Long completedInstances;
    private Long abortedInstances;
    private Long suspendedInstances;
    private Long errorInstances;
    private Double overallCompletionRate;
    private Map<String, Long> topProcesses;
    private List<BottleneckTask> bottleneckTasks;
    private List<ProcessAlert> activeAlerts;
    private Map<String, Long> instancesByProcess;
    private Map<String, Long> timelineStats;
    private Double averageDurationMs;
    private Long instancesLast24Hours;
    private Long instancesLast7Days;
    
    // Nouvelles métriques de déploiement
    private Long totalDeployed;
    private Long totalUndeployed;
    
    // Nouvelles métriques de validité
    private Long totalValid;
    private Long totalInvalid;
    
    // Métriques de versioning sémantique
    private Long totalUniqueVersions;
    private Map<String, Long> versionsByType; // MAJOR, MINOR, PATCH
    
    // Métriques d'historique
    private Long totalVersionsInHistory;
    private Double averageVersionsPerProcess;

    public static class BottleneckTask {
        private String taskName;
        private String processId;
        private Long stuckCount;
        private Long averageWaitingTimeMs;
        private String severity; // LOW, MEDIUM, HIGH

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

        public Long getStuckCount() {
            return stuckCount;
        }

        public void setStuckCount(Long stuckCount) {
            this.stuckCount = stuckCount;
        }

        public Long getAverageWaitingTimeMs() {
            return averageWaitingTimeMs;
        }

        public void setAverageWaitingTimeMs(Long averageWaitingTimeMs) {
            this.averageWaitingTimeMs = averageWaitingTimeMs;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }

    public static class ProcessAlert {
        private String processId;
        private String instanceId;
        private String alertType; // TIMEOUT, ERROR, BOTTLENECK
        private String message;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private Long durationExceededMs;
        private String taskName;

        public String getProcessId() {
            return processId;
        }

        public void setProcessId(String processId) {
            this.processId = processId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getAlertType() {
            return alertType;
        }

        public void setAlertType(String alertType) {
            this.alertType = alertType;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public Long getDurationExceededMs() {
            return durationExceededMs;
        }

        public void setDurationExceededMs(Long durationExceededMs) {
            this.durationExceededMs = durationExceededMs;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }
    }

    // Getters et Setters
    public Long getTotalInstances() {
        return totalInstances;
    }

    public void setTotalInstances(Long totalInstances) {
        this.totalInstances = totalInstances;
    }

    public Long getActiveInstances() {
        return activeInstances;
    }

    public void setActiveInstances(Long activeInstances) {
        this.activeInstances = activeInstances;
    }

    public Long getCompletedInstances() {
        return completedInstances;
    }

    public void setCompletedInstances(Long completedInstances) {
        this.completedInstances = completedInstances;
    }

    public Long getAbortedInstances() {
        return abortedInstances;
    }

    public void setAbortedInstances(Long abortedInstances) {
        this.abortedInstances = abortedInstances;
    }

    public Long getSuspendedInstances() {
        return suspendedInstances;
    }

    public void setSuspendedInstances(Long suspendedInstances) {
        this.suspendedInstances = suspendedInstances;
    }

    public Long getErrorInstances() {
        return errorInstances;
    }

    public void setErrorInstances(Long errorInstances) {
        this.errorInstances = errorInstances;
    }

    public Double getOverallCompletionRate() {
        return overallCompletionRate;
    }

    public void setOverallCompletionRate(Double overallCompletionRate) {
        this.overallCompletionRate = overallCompletionRate;
    }

    public Map<String, Long> getTopProcesses() {
        return topProcesses;
    }

    public void setTopProcesses(Map<String, Long> topProcesses) {
        this.topProcesses = topProcesses;
    }

    public List<BottleneckTask> getBottleneckTasks() {
        return bottleneckTasks;
    }

    public void setBottleneckTasks(List<BottleneckTask> bottleneckTasks) {
        this.bottleneckTasks = bottleneckTasks;
    }

    public List<ProcessAlert> getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(List<ProcessAlert> activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

    public Map<String, Long> getInstancesByProcess() {
        return instancesByProcess;
    }

    public void setInstancesByProcess(Map<String, Long> instancesByProcess) {
        this.instancesByProcess = instancesByProcess;
    }

    public Map<String, Long> getTimelineStats() {
        return timelineStats;
    }

    public void setTimelineStats(Map<String, Long> timelineStats) {
        this.timelineStats = timelineStats;
    }

    public Double getAverageDurationMs() {
        return averageDurationMs;
    }

    public void setAverageDurationMs(Double averageDurationMs) {
        this.averageDurationMs = averageDurationMs;
    }

    public Long getInstancesLast24Hours() {
        return instancesLast24Hours;
    }

    public void setInstancesLast24Hours(Long instancesLast24Hours) {
        this.instancesLast24Hours = instancesLast24Hours;
    }

    public Long getInstancesLast7Days() {
        return instancesLast7Days;
    }

    public void setInstancesLast7Days(Long instancesLast7Days) {
        this.instancesLast7Days = instancesLast7Days;
    }

    public Long getTotalDeployed() {
        return totalDeployed;
    }

    public void setTotalDeployed(Long totalDeployed) {
        this.totalDeployed = totalDeployed;
    }

    public Long getTotalUndeployed() {
        return totalUndeployed;
    }

    public void setTotalUndeployed(Long totalUndeployed) {
        this.totalUndeployed = totalUndeployed;
    }

    public Long getTotalValid() {
        return totalValid;
    }

    public void setTotalValid(Long totalValid) {
        this.totalValid = totalValid;
    }

    public Long getTotalInvalid() {
        return totalInvalid;
    }

    public void setTotalInvalid(Long totalInvalid) {
        this.totalInvalid = totalInvalid;
    }

    public Long getTotalUniqueVersions() {
        return totalUniqueVersions;
    }

    public void setTotalUniqueVersions(Long totalUniqueVersions) {
        this.totalUniqueVersions = totalUniqueVersions;
    }

    public Map<String, Long> getVersionsByType() {
        return versionsByType;
    }

    public void setVersionsByType(Map<String, Long> versionsByType) {
        this.versionsByType = versionsByType;
    }

    public Long getTotalVersionsInHistory() {
        return totalVersionsInHistory;
    }

    public void setTotalVersionsInHistory(Long totalVersionsInHistory) {
        this.totalVersionsInHistory = totalVersionsInHistory;
    }

    public Double getAverageVersionsPerProcess() {
        return averageVersionsPerProcess;
    }

    public void setAverageVersionsPerProcess(Double averageVersionsPerProcess) {
        this.averageVersionsPerProcess = averageVersionsPerProcess;
    }
}
