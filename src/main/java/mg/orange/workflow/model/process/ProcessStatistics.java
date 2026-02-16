package mg.orange.workflow.model.process;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Classe encapsulant les statistiques globales des processus BPMN
 * Version améliorée avec métriques étendues pour une meilleure visibilité
 */
@Data
@NoArgsConstructor
public class ProcessStatistics {

    // ===== MÉTRIQUES DE BASE (conservées pour compatibilité) =====
    
    /**
     * Nombre de processus déployés dans Kogito
     */
    private long totalDeployed;

    /**
     * Nombre de processus valides (prêts à être déployés)
     */
    private long totalValid;

    /**
     * Nombre de processus invalides (contenant des erreurs)
     */
    private long totalInvalid;

    /**
     * Nombre de processus non déployés
     */
    private long totalUndeployed;

    /**
     * Nombre de versions uniques
     */
    private long totalUniqueVersions;

    // ===== NOUVELLES MÉTRIQUES DE PERFORMANCE =====
    
    /**
     * Taux de succès des déploiements en pourcentage
     */
    private double deploymentSuccessRate;

    /**
     * Temps moyen de traitement des processus (en secondes)
     */
    private double averageProcessingTime;

    /**
     * Nombre de processus contenant des erreurs actives
     */
    private long processesWithErrors;

    /**
     * Nombre de processus modifiés récemment (24h)
     */
    private long processesRecentlyModified;

    /**
     * Répartition des processus par type
     */
    private Map<String, Long> typeDistribution;

    // ===== MÉTRIQUES TEMPORELLES =====
    
    /**
     * Timestamp de la dernière mise à jour des statistiques
     */
    private LocalDateTime lastCalculationTime;

    /**
     * Total des processus actifs sur 24h
     */
    private long totalProcesses24h;

    /**
     * Processus déployés sur 24h
     */
    private long deployedProcesses24h;

    /**
     * Processus invalides sur 24h
     */
    private long invalidProcesses24h;

    /**
     * Statistiques par heure (format: "HH:mm" -> count)
     */
    private Map<String, Long> hourlyStats;

    // ===== MÉTRIQUES MÉTIER AVANCÉES =====
    
    /**
     * Taux d'utilisation du système (%)
     */
    private double systemUtilizationRate;

    /**
     * Nombre moyen de tâches par processus
     */
    private double averageTasksPerProcess;

    /**
     * Score de santé global des processus (0-100)
     */
    private int overallHealthScore;

    /**
     * Indique si des alertes sont actives
     */
    private boolean hasActiveAlerts;

    /**
     * Nombre de processus en maintenance
     */
    private long processesInMaintenance;

    /**
     * Constructeur avancé avec toutes les métriques
     */
    public ProcessStatistics(long totalDeployed, long totalValid, long totalInvalid, 
                           long totalUndeployed, long totalUniqueVersions,
                           double deploymentSuccessRate, double averageProcessingTime,
                           long processesWithErrors, long processesRecentlyModified,
                           Map<String, Long> typeDistribution, LocalDateTime lastCalculationTime,
                           long totalProcesses24h, long deployedProcesses24h, long invalidProcesses24h,
                           Map<String, Long> hourlyStats, double systemUtilizationRate,
                           double averageTasksPerProcess, int overallHealthScore,
                           boolean hasActiveAlerts, long processesInMaintenance) {
        this.totalDeployed = totalDeployed;
        this.totalValid = totalValid;
        this.totalInvalid = totalInvalid;
        this.totalUndeployed = totalUndeployed;
        this.totalUniqueVersions = totalUniqueVersions;
        this.deploymentSuccessRate = deploymentSuccessRate;
        this.averageProcessingTime = averageProcessingTime;
        this.processesWithErrors = processesWithErrors;
        this.processesRecentlyModified = processesRecentlyModified;
        this.typeDistribution = typeDistribution;
        this.lastCalculationTime = lastCalculationTime;
        this.totalProcesses24h = totalProcesses24h;
        this.deployedProcesses24h = deployedProcesses24h;
        this.invalidProcesses24h = invalidProcesses24h;
        this.hourlyStats = hourlyStats;
        this.systemUtilizationRate = systemUtilizationRate;
        this.averageTasksPerProcess = averageTasksPerProcess;
        this.overallHealthScore = overallHealthScore;
        this.hasActiveAlerts = hasActiveAlerts;
        this.processesInMaintenance = processesInMaintenance;
    }

    /**
     * Calcule le score de santé global automatiquement
     */
    public void calculateHealthScore() {
        if (totalDeployed + totalValid + totalInvalid + totalUndeployed == 0) {
            this.overallHealthScore = 100;
            return;
        }

        // Formule de calcul du score de santé
        double healthScore = 100.0;
        
        // Pénalité pour les processus invalides
        if (totalInvalid > 0) {
            healthScore -= (totalInvalid * 10.0 / (totalDeployed + totalValid + totalInvalid + totalUndeployed));
        }
        
        // Pénalité pour les processus avec erreurs
        if (processesWithErrors > 0) {
            healthScore -= (processesWithErrors * 5.0);
        }
        
        // Bonus pour le taux de succès élevé
        if (deploymentSuccessRate > 90) {
            healthScore += 5;
        }

        this.overallHealthScore = (int) Math.max(0, Math.min(100, healthScore));
    }

    /**
     * Détermine si le système a des alertes actives
     */
    public void evaluateAlerts() {
        this.hasActiveAlerts = (
            totalInvalid > 0 || 
            processesWithErrors > 0 || 
            overallHealthScore < 70 ||
            deploymentSuccessRate < 80
        );
    }

    /**
     * Obtient un résumé lisible des statistiques
     */
    public String getSummary() {
        return String.format(
            "Statistiques BPMN - Total: %d | Déployés: %d | Valides: %d | Invalides: %d | Score santé: %d%%",
            getTotalProcesses(), totalDeployed, totalValid, totalInvalid, overallHealthScore
        );
    }

    /**
     * Calcule le nombre total de processus
     */
    public long getTotalProcesses() {
        return totalDeployed + totalValid + totalInvalid + totalUndeployed;
    }
}
