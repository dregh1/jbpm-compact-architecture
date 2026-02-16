package mg.orange.workflow.repository;

import io.quarkus.scheduler.Scheduled;
import mg.orange.workflow.model.stats.ProcessStatsDTO;
import mg.orange.workflow.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProcessStatsRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProcessStatsRepository.class);

    private final EntityManager entityManager;
    private final AlertService alertService;

    @Inject
    public ProcessStatsRepository(EntityManager entityManager, AlertService alertService) {
        this.entityManager = entityManager;
        this.alertService = alertService;
    }

    /**
     * Récupère toutes les statistiques avancées pour le dashboard
     */
    public ProcessStatsDTO getAdvancedDashboardStats() {
        logger.info("Début de la récupération des statistiques avancées du dashboard");

        try {
            ProcessStatsDTO stats = new ProcessStatsDTO();

            // Stats de base
            logger.debug("Récupération des statistiques de base");
            ProcessStatsDTO basicStats = getBasicStats();
            stats.setTotalInstances(basicStats.getTotalInstances());
            stats.setActiveInstances(basicStats.getActiveInstances());
            stats.setCompletedInstances(basicStats.getCompletedInstances());
            stats.setAbortedInstances(basicStats.getAbortedInstances());
            stats.setSuspendedInstances(basicStats.getSuspendedInstances());
            stats.setErrorInstances(basicStats.getErrorInstances());
            stats.setInstancesByProcess(basicStats.getInstancesByProcess());
            stats.setAverageDurationMs(basicStats.getAverageDurationMs());
            stats.setInstancesLast24Hours(basicStats.getInstancesLast24Hours());
            stats.setInstancesLast7Days(basicStats.getInstancesLast7Days());

            // Nouvelles métriques
            logger.debug("Calcul du taux de complétion global");
            stats.setOverallCompletionRate(calculateOverallCompletionRate());

            logger.debug("Récupération du top 5 des processus les plus utilisés");
            stats.setTopProcesses(getTopProcesses());

            logger.debug("Détection des tâches problématiques (Problèmes Critiques)");
            stats.setBottleneckTasks(detectBottleneckTasks());

            logger.debug("Génération des alertes de processus");
            stats.setActiveAlerts(generateProcessAlerts());

            logger.debug("Récupération des statistiques temporelles");
            stats.setTimelineStats(getTimelineStats("daily"));

            logger.info("Statistiques avancées récupérées avec succès");
            return stats;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques avancées: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Taux de complétion global des processus
     */
    private Double calculateOverallCompletionRate() {
        try {
            String sql = "SELECT " +
                "ROUND(COUNT(CASE WHEN state = 2 THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0), 2) " +
                "FROM processes";

            Query query = entityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result == null) return 0.0;

            // Gestion plus robuste du type de retour
            return Double.parseDouble(result.toString());

        } catch (Exception e) {
            logger.error("Error calculating completion rate", e);
            return 0.0;
        }
    }

    /**
     * Top 5 des processus les plus utilisés
     */
    @SuppressWarnings("unchecked")
    public Map<String, Long> getTopProcesses() {
        logger.info("🔍 Récupération du top 5 des processus les plus utilisés");

        String sql = "SELECT process_id, COUNT(*) as cnt FROM process_instances GROUP BY process_id ORDER BY cnt DESC LIMIT 5";

        Map<String, Long> topProcesses = new LinkedHashMap<>();
        
        try {
            logger.info("🔄 Exécution SQL: {}", sql);
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = (List<Object[]>) query.getResultList();
            logger.info("  ✅ Résultats: {} lignes", results.size());

            if (!results.isEmpty()) {
                for (Object[] row : results) {
                    String processId = row[0] != null ? row[0].toString() : "unknown";
                    Long count = extractLong(row[1]);
                    logger.info("    Process: {} = {} instances", processId, count);
                    topProcesses.put(processId, count);
                }
                logger.info("📊 Succès! topProcesses size: {}", topProcesses.size());
                return topProcesses;
            } else {
                logger.warn("⚠️ Aucun résultat retourné!");
            }
        } catch (Exception e) {
            logger.error("❌ ERREUR: {}", e.getMessage(), e);
        }

        logger.error("❌ Retour d'une map vide");
        return topProcesses;
    }

    /**
     * Détection des tâches problématiques - Première tâche non-complétée après la dernière tâche complétée
     * Identifie précisément la tâche qui cause l'échec dans les processus en erreur
     * Sévérité basée sur le ratio : moitié (MEDIUM), plus de moitié (HIGH), tous (CRITICAL)
     */
    @SuppressWarnings("unchecked")
    public List<ProcessStatsDTO.BottleneckTask> detectBottleneckTasks() {
        logger.debug("Détection des tâches qui causent des échecs de processus (première après dernière completed)");

        // CTE pour identifier la première tâche problématique après la dernière tâche complétée
        String sql = "WITH last_completed_tasks AS ( " +
            "    SELECT " +
            "        t.process_instance_id, " +
            "        MAX(t.started) as last_completed_time " +
            "    FROM tasks t " +
            "    JOIN processes p ON t.process_instance_id = p.id " +
            "    WHERE p.state = 5 " +
            "      AND t.state = 'Completed' " +
            "      AND p.start_time >= NOW() - INTERVAL '30 days' " +
            "    GROUP BY t.process_instance_id " +
            "), " +
            "first_failed_tasks AS ( " +
            "    SELECT DISTINCT ON (t.process_instance_id) " +
            "        t.process_instance_id, " +
            "        t.name, " +
            "        t.process_id, " +
            "        t.started " +
            "    FROM tasks t " +
            "    JOIN processes p ON t.process_instance_id = p.id " +
            "    LEFT JOIN last_completed_tasks lct ON t.process_instance_id = lct.process_instance_id " +
            "    WHERE p.state = 5 " +
            "      AND t.state != 'Completed' " +
            "      AND p.start_time >= NOW() - INTERVAL '30 days' " +
            "      AND (lct.last_completed_time IS NULL OR t.started > lct.last_completed_time) " +
            "    ORDER BY t.process_instance_id, t.started ASC " +
            "), " +
            "all_task_instances AS ( " +
            "    SELECT " +
            "        t.name, " +
            "        t.process_id, " +
            "        COUNT(DISTINCT t.process_instance_id) as total_instances " +
            "    FROM tasks t " +
            "    JOIN processes p ON t.process_instance_id = p.id " +
            "    WHERE p.start_time >= NOW() - INTERVAL '30 days' " +
            "    GROUP BY t.name, t.process_id " +
            ") " +
            "SELECT " +
            "    fft.name as task_name, " +
            "    fft.process_id, " +
            "    COUNT(DISTINCT fft.process_instance_id) as failed_instances, " +
            "    COALESCE(ati.total_instances, 0) as total_instances, " +
            "    ROUND(COUNT(DISTINCT fft.process_instance_id) * 100.0 / " +
            "          NULLIF(COALESCE(ati.total_instances, 0), 0), 2) as failure_rate " +
            "FROM first_failed_tasks fft " +
            "LEFT JOIN all_task_instances ati ON fft.name = ati.name AND fft.process_id = ati.process_id " +
            "GROUP BY fft.name, fft.process_id, ati.total_instances " +
            "HAVING COUNT(DISTINCT fft.process_instance_id) >= 2 " +
            "  AND COUNT(DISTINCT fft.process_instance_id) >= COALESCE(ati.total_instances, 0) * 0.5 " +
            "ORDER BY failure_rate DESC, failed_instances DESC " +
            "LIMIT 10";

        Map<String, Long> topProcesses = new LinkedHashMap<>();
        
        try {
            logger.debug("Vérification de l'existence de la table tasks");
            // Vérifier si la table existe
            try {
                Query checkQuery = entityManager.createNativeQuery("SELECT COUNT(*) FROM tasks");
                Object checkResult = checkQuery.getSingleResult();
                logger.debug("Table tasks accessible: {} enregistrements", checkResult);
            } catch (Exception tableCheckError) {
                logger.warn("Table tasks inaccessible: {}", tableCheckError.getMessage());
                return new ArrayList<>();
            }

            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = query.getResultList();
            logger.debug("Tâches problématiques détectées: {} résultats", results.size());

            return results.stream()
                .map(row -> {
                    ProcessStatsDTO.BottleneckTask bottleneck = new ProcessStatsDTO.BottleneckTask();
                    bottleneck.setTaskName((String) row[0]);
                    bottleneck.setProcessId((String) row[1]);
                    
                    long failedInstances = extractLong(row[2]);
                    long totalInstances = extractLong(row[3]);
                    double failureRate = extractDouble(row[4]);
                    
                    bottleneck.setStuckCount(failedInstances);  // Nombre de processus en échec
                    bottleneck.setAverageWaitingTimeMs(0L);     // Non utilisé
                    
                    // Déterminer la sévérité basée sur le ratio d'échec
                    if (failureRate >= 95.0) {  // ≥95% = tous/presque tous échouent
                        bottleneck.setSeverity("CRITICAL");
                    } else if (failureRate >= 75.0) {  // 75-95% = plus que la moitié
                        bottleneck.setSeverity("HIGH");
                    } else {  // 50-75% = la moitié
                        bottleneck.setSeverity("MEDIUM");
                    }

                    logger.debug("Tâche problématique: {} sur {} - {}% échecs ({}/{} processus) - {}",
                        bottleneck.getTaskName(),
                        bottleneck.getProcessId(),
                        failureRate,
                        failedInstances,
                        totalInstances,
                        bottleneck.getSeverity());

                    return bottleneck;
                })
                .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la détection des tâches problématiques: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Génère la liste des alertes actives à afficher dans le dashboard
     * Simplifié: uniquement les processus en erreur (state=5)
     */
    private List<ProcessStatsDTO.ProcessAlert> generateProcessAlerts() {
        logger.debug("Génération des alertes de processus (erreurs uniquement)");
        return alertService.generateAllAlerts();
    }


    /**
     * Extrait un Long de façon sécurisée depuis un Object
     */
    private long extractLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof BigInteger) return ((BigInteger) obj).longValue();
        if (obj instanceof BigDecimal) return ((BigDecimal) obj).longValue();
        return Long.parseLong(obj.toString());
    }

    /**
     * Extrait un Double de façon sécurisée depuis un Object
     */
    private double extractDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        if (obj instanceof BigDecimal) return ((BigDecimal) obj).doubleValue();
        return Double.parseDouble(obj.toString());
    }

    /**
     * Récupère les statistiques temporelles (par période)
      Sauvegarde une alerte en base (délégué à AlertService)
     */
    public void saveAlertToDatabase(ProcessStatsDTO.ProcessAlert alert) {
        alertService.saveAlertToDatabase(alert);
    }

    /**
     * Résout une alerte (délégué à AlertService)
     */
    public void resolveAlert(String instanceId, String alertType) {
        alertService.resolveAlert(instanceId, alertType);
    }

    /**
     * Résout toutes les alertes d'une instance (délégué à AlertService)
     */
    public void resolveAllAlertsForInstance(String instanceId) {
        alertService.resolveAllAlertsForInstance(instanceId);
    }

    /**
     * Nettoyage des anciennes alertes (via AlertService)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    public void cleanupOldAlerts() {
        logger.info("🧹 Nettoyage des alertes résolues depuis plus de 30 jours...");
        // Nettoyage via AlertService si nécessaire
    }

    /**
     * Statistiques temporelles pour graphiques
     */
    @SuppressWarnings("unchecked")
    public Map<String, Long> getTimelineStats(String period) {
        logger.debug("Récupération des statistiques temporelles pour la période: {}", period);

        String sql;

        switch (period.toUpperCase()) {
            case "HOURLY":
                sql = "SELECT " +
                    "TO_CHAR(DATE_TRUNC('hour', start_time), 'YYYY-MM-DD HH24:00') as time_period, " +
                    "COUNT(*) as count " +
                    "FROM processes " +
                    "WHERE start_time >= NOW() - INTERVAL '24 hours' " +
                    "GROUP BY DATE_TRUNC('hour', start_time) " +
                    "ORDER BY time_period";
                break;
            case "WEEKLY":
                sql = "SELECT " +
                    "TO_CHAR(DATE_TRUNC('week', start_time), 'YYYY-\"W\"IW') as time_period, " +
                    "COUNT(*) as count " +
                    "FROM processes " +
                    "WHERE start_time >= NOW() - INTERVAL '12 weeks' " +
                    "GROUP BY DATE_TRUNC('week', start_time) " +
                    "ORDER BY time_period";
                break;
            default: // DAILY
                sql = "SELECT " +
                    "TO_CHAR(DATE_TRUNC('day', start_time), 'YYYY-MM-DD') as time_period, " +
                    "COUNT(*) as count " +
                    "FROM processes " +
                    "WHERE start_time >= NOW() - INTERVAL '30 days' " +
                    "GROUP BY DATE_TRUNC('day', start_time) " +
                    "ORDER BY time_period";
        }

        try {
            logger.debug("Exécution de la requête temporelle: {}", sql.substring(0, Math.min(100, sql.length())));
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = (List<Object[]>) query.getResultList();
            logger.debug("Statistiques temporelles récupérées: {} résultats", results.size());

            return results.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> ((BigInteger) row[1]).longValue()
                ));
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques temporelles: {}", e.getMessage(), e);
            // Retourner une map vide en cas d'erreur
            return new HashMap<>();
        }
    }

    private ProcessStatsDTO getBasicStats() {
        ProcessStatsDTO stats = new ProcessStatsDTO();

        try {
            logger.debug("Exécution de la requête SQL pour les statistiques de base");
            // Vérifier d'abord si la table processes existe
            try {
                Query checkTableQuery = entityManager.createNativeQuery("SELECT COUNT(*) FROM processes");
                Object tableCheck = checkTableQuery.getSingleResult();
                logger.debug("Table processes existe et contient {} enregistrements", tableCheck);
            } catch (Exception tableCheckError) {
                logger.warn("La table processes pourrait ne pas exister ou être vide: {}", tableCheckError.getMessage());
                // Continuer malgré l'erreur de vérification
            }

            // Requête unique optimisée pour toutes les statistiques de base
            String sql = "SELECT " +
                "COUNT(*) as total, " +
                "COUNT(CASE WHEN state = 1 THEN 1 END) as active, " +
                "COUNT(CASE WHEN state = 2 THEN 1 END) as completed, " +
                "COUNT(CASE WHEN state = 3 THEN 1 END) as aborted, " +
                "COUNT(CASE WHEN state = 4 THEN 1 END) as suspended, " +
                "COUNT(CASE WHEN state = 5 THEN 1 END) as error, " +
                "COUNT(CASE WHEN start_time >= CURRENT_TIMESTAMP - INTERVAL '24 HOURS' THEN 1 END) as last_24h, " +
                "COUNT(CASE WHEN start_time >= CURRENT_TIMESTAMP - INTERVAL '7 DAYS' THEN 1 END) as last_7d " +
                "FROM processes";

            Query query = entityManager.createNativeQuery(sql);
            Object resultObj = query.getSingleResult();
            logger.debug("Résultat de la requête SQL obtenu: {}", resultObj != null ? "non-null" : "null");

            // Vérification de nullité et de type pour éviter NullPointerException et ClassCastException
            if (resultObj == null || !(resultObj instanceof Object[])) {
                logger.warn("Aucun résultat valide retourné par la requête de statistiques de base, utilisation des valeurs par défaut");
                handleStatsError(stats, new RuntimeException("Invalid or null results returned from basic stats query"));
                return stats;
            }

            Object[] result = (Object[]) resultObj;

            // Vérification de la longueur du tableau pour éviter ArrayIndexOutOfBoundsException
            if (result.length < 8) {
                logger.warn("Résultat incomplet retourné par la requête de statistiques de base ({} éléments au lieu de 8), utilisation des valeurs par défaut", result.length);
                handleStatsError(stats, new RuntimeException("Incomplete results returned from basic stats query"));
                return stats;
            }

            // Extraction des résultats
            stats.setTotalInstances(extractLong(result[0]));
            stats.setActiveInstances(extractLong(result[1]));
            stats.setCompletedInstances(extractLong(result[2]));
            stats.setAbortedInstances(extractLong(result[3]));
            stats.setSuspendedInstances(extractLong(result[4]));
            stats.setErrorInstances(extractLong(result[5]));
            stats.setInstancesLast24Hours(extractLong(result[6]));
            stats.setInstancesLast7Days(extractLong(result[7]));

            // Métriques supplémentaires
            logger.debug("Récupération des instances par processus");
            stats.setInstancesByProcess(getInstancesByProcess());
            logger.debug("Calcul de la durée moyenne");
            stats.setAverageDurationMs(getAverageDuration());
            logger.debug("Statistiques de base récupérées avec succès: total={}", stats.getTotalInstances());

        } catch (Exception e) {
            // Fallback en cas d'erreur
            logger.error("Erreur lors de la récupération des statistiques de base: {}", e.getMessage(), e);
            handleStatsError(stats, e);
        }

        return stats;
    }
    /**
     * Récupère la répartition des instances par processus
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> getInstancesByProcess() {
        String sql = "SELECT process_id, process_name, COUNT(*) as count FROM processes GROUP BY process_id, process_name";

        try {
            Query query = entityManager.createNativeQuery(sql, Object[].class);
            List<Object[]> results = query.getResultList();

            Map<String, Long> byProcess = new HashMap<>();
            for (Object[] row : results) {
                String processId = (String) row[0];
                String processName = (String) row[1];
                String key = processName != null ? processName : processId;
                Long count = extractLong(row[2]);
                byProcess.put(key, count);
            }

            return byProcess;
        } catch (Exception e) {
            // Retourner une map vide en cas d'erreur
            return new HashMap<>();
        }
    }

    /**
     * Calcule la durée moyenne d'exécution
     */
    private Double getAverageDuration() {
        String sql = "SELECT AVG(EXTRACT(EPOCH FROM (end_time - start_time)) * 1000) " +
                     "FROM processes " +
                     "WHERE state = 2 " +
                     "AND end_time IS NOT NULL " +
                     "AND start_time IS NOT NULL " +
                     "AND end_time > start_time";

        try {
            Query query = entityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result instanceof BigDecimal bigDecimal) {
                return bigDecimal.doubleValue();
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Gestion des erreurs avec valeurs par défaut
     */
    private void handleStatsError(ProcessStatsDTO stats, Exception e) {
        logger.error("Erreur lors du calcul des statistiques de base: {}", e.getMessage());

        // Valeurs par défaut en cas d'erreur
        stats.setTotalInstances(0L);
        stats.setActiveInstances(0L);
        stats.setCompletedInstances(0L);
        stats.setAbortedInstances(0L);
        stats.setSuspendedInstances(0L);
        stats.setErrorInstances(0L);
        stats.setInstancesLast24Hours(0L);
        stats.setInstancesLast7Days(0L);
        stats.setInstancesByProcess(new HashMap<>());
        stats.setAverageDurationMs(0.0);
    }
}
