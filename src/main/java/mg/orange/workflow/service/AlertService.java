package mg.orange.workflow.service;

import mg.orange.workflow.model.stats.ProcessStatsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service centralisé pour la gestion des alertes de processus
 * Responsabilités:
 * - Détection des alertes (goulots, erreurs)
 * - Sauvegarde et résolution des alertes
 * - Récupération des alertes actives
 * SIMPLIFIÉ: Gère uniquement les processus en erreur (state=5)
 */
@ApplicationScoped
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    private final EntityManager entityManager;

    @Inject
    public AlertService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Génère toutes les alertes pour le dashboard
     * Uniquement les processus en erreur (state=5)
     */
    public List<ProcessStatsDTO.ProcessAlert> generateAllAlerts() {
        logger.info("🔍 === GENERATION DES ALERTES (ERREURS UNIQUEMENT) ===");
        
        List<ProcessStatsDTO.ProcessAlert> errorAlerts = detectErrorAlerts();
        
        logger.info("✅ === FIN GENERATION: {} alertes d'erreur ===", errorAlerts.size());
        return errorAlerts;
    }

    /**
     * Détection des processus en erreur (state=5)
     * Une alerte par instance de processus en erreur
     */
    @SuppressWarnings("unchecked")
    private List<ProcessStatsDTO.ProcessAlert> detectErrorAlerts() {
        String sql = "SELECT " +
            "pi.id as instance_id, " +
            "pi.process_id, " +
            "pi.start_time, " +
            "pi.end_time " +
            "FROM processes pi " +
            "WHERE pi.state = 5 " +
            "AND pi.end_time >= NOW() - INTERVAL '24 hours' " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM process_alerts pa " +
            "    WHERE pa.instance_id = pi.id " +
            "    AND pa.alert_type = 'ERROR' " +
            "    AND pa.resolved_time IS NULL " +
            ")";

        try {
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = (List<Object[]>) query.getResultList();

            List<ProcessStatsDTO.ProcessAlert> errorAlerts = results.stream()
                .map(row -> {
                    ProcessStatsDTO.ProcessAlert alert = new ProcessStatsDTO.ProcessAlert();
                    alert.setInstanceId((String) row[0]);
                    alert.setProcessId((String) row[1]);
                    alert.setAlertType("ERROR");
                    alert.setMessage("❌ Processus terminé en erreur");
                    alert.setSeverity("CRITICAL");
                    return alert;
                })
                .toList();

            // Sauvegarder toutes les nouvelles alertes
            errorAlerts.forEach(this::saveAlertToDatabase);

            logger.info("✅ Alertes erreur détectées: {}", errorAlerts.size());
            return errorAlerts;

        } catch (Exception e) {
            logger.error("❌ Erreur détection error alerts: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère toutes les alertes actives (non résolues)
     */
    @SuppressWarnings("unchecked")
    public List<ProcessStatsDTO.ProcessAlert> getActiveAlerts() {
        String sql = "SELECT " +
            "pa.id, pa.instance_id, pa.alert_type, pa.severity, pa.message, pa.created_time, " +
            "pi.process_id " +
            "FROM process_alerts pa " +
            "LEFT JOIN processes pi ON pa.instance_id = pi.id " +
            "WHERE pa.resolved_time IS NULL " +
            "ORDER BY pa.created_time DESC";

        try {
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = (List<Object[]>) query.getResultList();

            return results.stream()
                .map(row -> {
                    ProcessStatsDTO.ProcessAlert alert = new ProcessStatsDTO.ProcessAlert();
                    alert.setInstanceId((String) row[1]);
                    alert.setAlertType((String) row[2]);
                    alert.setSeverity((String) row[3]);
                    alert.setMessage((String) row[4]);
                    alert.setProcessId((String) row[6]);
                    return alert;
                })
                .toList();

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la récupération des alertes actives: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Sauvegarde une alerte en base de données
     */
    @Transactional
    public void saveAlertToDatabase(ProcessStatsDTO.ProcessAlert alert) {
        try {
            String insertSql = "INSERT INTO process_alerts (id, instance_id, alert_type, severity, message, created_time) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

            Query query = entityManager.createNativeQuery(insertSql);
            query.setParameter(1, generateAlertId());
            query.setParameter(2, alert.getInstanceId());
            query.setParameter(3, alert.getAlertType());
            query.setParameter(4, alert.getSeverity());
            query.setParameter(5, alert.getMessage());

            query.executeUpdate();
            logger.debug("✅ Alerte sauvegardée: {} - {}", alert.getAlertType(), alert.getMessage());

        } catch (Exception e) {
            logger.warn("⚠️ Erreur sauvegarde alerte: {}", e.getMessage());
        }
    }

    /**
     * Résout une alerte spécifique
     */
    @Transactional
    public void resolveAlert(String instanceId, String alertType) {
        try {
            String updateSql = "UPDATE process_alerts " +
                "SET resolved_time = NOW() " +
                "WHERE instance_id = ? AND alert_type = ? AND resolved_time IS NULL";

            Query query = entityManager.createNativeQuery(updateSql);
            query.setParameter(1, instanceId);
            query.setParameter(2, alertType);

            int updated = query.executeUpdate();
            logger.info("✅ Alertes résolues: {} alerte(s) pour {}", updated, instanceId);

        } catch (Exception e) {
            logger.warn("⚠️ Erreur résolution alerte: {}", e.getMessage());
        }
    }

    /**
     * Résout toutes les alertes d'une instance
     */
    @Transactional
    public void resolveAllAlertsForInstance(String instanceId) {
        try {
            String updateSql = "UPDATE process_alerts " +
                "SET resolved_time = NOW() " +
                "WHERE instance_id = ? AND resolved_time IS NULL";

            Query query = entityManager.createNativeQuery(updateSql);
            query.setParameter(1, instanceId);

            int updated = query.executeUpdate();
            logger.info("✅ Toutes les alertes résolues pour {}: {} alerte(s)", instanceId, updated);

        } catch (Exception e) {
            logger.warn("⚠️ Erreur résolution alertes instance: {}", e.getMessage());
        }
    }

    /**
     * Génère un ID unique pour une alerte
     */
    private String generateAlertId() {
        return "alert_" + UUID.randomUUID().toString();
    }
}
