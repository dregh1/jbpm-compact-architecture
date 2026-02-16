package mg.orange.workflow.repository;

import mg.orange.workflow.exception.ApiException;
import mg.orange.workflow.model.instances.ProcessInstanceSearchDTOs.SearchCriteria;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ProcessInstanceRepository {

    private static final Logger LOG = Logger.getLogger(ProcessInstanceRepository.class);

    // Constantes pour les colonnes fréquemment utilisées
    private static final String COLUMN_STATE = "state";
    private static final String COLUMN_START_TIME = "start_time";

    // Liste des colonnes autorisées pour le tri
    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "id", "process_id", "process_name", COLUMN_STATE, "business_key",
        COLUMN_START_TIME, "end_time", "last_update_time", "variables", "endpoint"
    );

    private final EntityManager entityManager;

    @Inject
    public ProcessInstanceRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Object[]> searchInstances(SearchCriteria criteria) {
        StringBuilder sql = buildSearchSql(criteria);
        Map<String, Object> parameters = buildSearchParameters(criteria);

        // Debug: afficher la requête SQL
        LOG.debug("SQL Query: " + sql.toString());
        LOG.debug("Parameters: " + parameters);

        try {
            Query query = entityManager.createNativeQuery(sql.toString());
            setQueryParameters(query, parameters);
            applyPagination(query, criteria);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            return results;

        } catch (Exception e) {
            LOG.error("Error executing query: " + e.getMessage(), e);
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, "Erreur lors de la recherche d'instances", "SEARCH_INSTANCES_FAILED");
        }
    }

    private StringBuilder buildSearchSql(SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, process_id, process_name, ").append(COLUMN_STATE).append(", business_key, ")
           .append(COLUMN_START_TIME).append(", end_time, last_update_time, CAST(variables AS TEXT) AS variables, endpoint ")
           .append("FROM processes WHERE 1=1 ");

        appendSearchConditions(sql, criteria);
        appendOrderBy(sql, criteria);

        return sql;
    }

    private void appendSearchConditions(StringBuilder sql, SearchCriteria criteria) {
        if (criteria.processId != null && !criteria.processId.trim().isEmpty()) {
            sql.append("AND process_id = :processId ");
        }
        if (criteria.state != null) {
            sql.append("AND ").append(COLUMN_STATE).append(" = :processState ");
        }
        if (criteria.businessKey != null && !criteria.businessKey.trim().isEmpty()) {
            sql.append("AND business_key = :businessKey ");
        }
        if (criteria.instanceId != null && !criteria.instanceId.trim().isEmpty()) {
            sql.append("AND id = :instanceId ");
        }
        if (criteria.startTimeFrom != null) {
            sql.append("AND ").append(COLUMN_START_TIME).append(" >= :startTimeFrom ");
        }
        if (criteria.startTimeTo != null) {
            sql.append("AND ").append(COLUMN_START_TIME).append(" <= :startTimeTo ");
        }
        if (criteria.endTimeFrom != null) {
            sql.append("AND end_time >= :endTimeFrom ");
        }
        if (criteria.endTimeTo != null) {
            sql.append("AND end_time <= :endTimeTo ");
        }
    }

    private void appendOrderBy(StringBuilder sql, SearchCriteria criteria) {
        String sortBy = getSafeColumnName(criteria.sortBy);
        String sortOrder = "DESC".equalsIgnoreCase(criteria.sortOrder) ? "DESC" : "ASC";
        sql.append("ORDER BY ").append(sortBy).append(" ").append(sortOrder);
    }

    private Map<String, Object> buildSearchParameters(SearchCriteria criteria) {
        Map<String, Object> parameters = new HashMap<>();

        if (criteria.processId != null && !criteria.processId.trim().isEmpty()) {
            parameters.put("processId", criteria.processId.trim());
        }
        if (criteria.state != null) {
            parameters.put("processState", criteria.state);
        }
        if (criteria.businessKey != null && !criteria.businessKey.trim().isEmpty()) {
            parameters.put("businessKey", criteria.businessKey.trim());
        }
        if (criteria.instanceId != null && !criteria.instanceId.trim().isEmpty()) {
            parameters.put("instanceId", criteria.instanceId.trim());
        }
        if (criteria.startTimeFrom != null) {
            parameters.put("startTimeFrom", criteria.startTimeFrom);
        }
        if (criteria.startTimeTo != null) {
            parameters.put("startTimeTo", criteria.startTimeTo);
        }
        if (criteria.endTimeFrom != null) {
            parameters.put("endTimeFrom", criteria.endTimeFrom);
        }
        if (criteria.endTimeTo != null) {
            parameters.put("endTimeTo", criteria.endTimeTo);
        }

        return parameters;
    }

    private void setQueryParameters(Query query, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private void applyPagination(Query query, SearchCriteria criteria) {
        int page = criteria.page != null ? criteria.page : 0;
        int size = criteria.size != null ? criteria.size : 20;
        query.setFirstResult(page * size);
        query.setMaxResults(size);
    }

    public long countInstances(SearchCriteria criteria) {
        StringBuilder sql = buildCountSql(criteria);
        Map<String, Object> parameters = buildCountParameters(criteria);

        try {
            Query query = entityManager.createNativeQuery(sql.toString());
            setQueryParameters(query, parameters);

            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            LOG.error("Error counting instances: " + e.getMessage(), e);
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, "Erreur lors du comptage d'instances", "COUNT_INSTANCES_FAILED");
        }
    }

    private StringBuilder buildCountSql(SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM processes WHERE 1=1 ");
        appendCountConditions(sql, criteria);
        return sql;
    }

    private void appendCountConditions(StringBuilder sql, SearchCriteria criteria) {
        appendSearchConditions(sql, criteria);
    }

    private Map<String, Object> buildCountParameters(SearchCriteria criteria) {
        return buildSearchParameters(criteria);
    }

    public Map<String, Object> getStatistics(SearchCriteria criteria) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Nombre total
            stats.put("totalInstances", countInstances(criteria));

            // Compter par état
            Query stateQuery = entityManager.createNativeQuery(
                "SELECT " + COLUMN_STATE + ", COUNT(*) FROM processes GROUP BY " + COLUMN_STATE
            );
            @SuppressWarnings("unchecked")
            List<Object[]> stateResults = stateQuery.getResultList();

            long active = 0;
            long completed = 0;
            long aborted = 0;
            long suspended = 0;
            for (Object[] row : stateResults) {
                Integer state = ((Number) row[0]).intValue();
                Long count = ((Number) row[1]).longValue();

                switch (state) {
                    case 1: active = count; break;
                    case 2: completed = count; break;
                    case 3: aborted = count; break;
                    case 4: suspended = count; break;
                    default: LOG.warn("Unknown process state: " + state); break;
                }
            }

            stats.put("activeInstances", active);
            stats.put("completedInstances", completed);
            stats.put("abortedInstances", aborted);
            stats.put("suspendedInstances", suspended);

            // Compter par processus
            Query processQuery = entityManager.createNativeQuery(
                "SELECT process_id, COUNT(*) FROM processes GROUP BY process_id"
            );
            @SuppressWarnings("unchecked")
            List<Object[]> processResults = processQuery.getResultList();

            Map<String, Long> byProcess = new HashMap<>();
            for (Object[] row : processResults) {
                String processId = (String) row[0];
                Long count = ((Number) row[1]).longValue();
                byProcess.put(processId, count);
            }
            stats.put("instancesByProcess", byProcess);

            // Durée moyenne (pour les instances terminées)
            Query avgQuery = entityManager.createNativeQuery(
                "SELECT AVG(EXTRACT(EPOCH FROM (end_time - " + COLUMN_START_TIME + ")) * 1000) " +
                "FROM processes WHERE " + COLUMN_STATE + " = 2 AND end_time IS NOT NULL AND " + COLUMN_START_TIME + " IS NOT NULL"
            );
            Object avgResult = avgQuery.getSingleResult();
            stats.put("averageDurationMs", avgResult != null ? ((Number) avgResult).doubleValue() : 0.0);

        } catch (Exception e) {
            LOG.error("Error getting statistics: " + e.getMessage(), e);
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération des statistiques", "GET_STATISTICS_FAILED");
        }

        return stats;
    }

    /**
     * Sécurise le nom de colonne pour le tri
     */
    private String getSafeColumnName(String sortBy) {
        if (sortBy == null) return COLUMN_START_TIME;

        // Nettoyer le nom de colonne
        String cleanSortBy = sortBy.trim().toLowerCase();

        // Vérifier si la colonne est autorisée
        if (ALLOWED_SORT_COLUMNS.contains(cleanSortBy)) {
            return cleanSortBy;
        }

        // Mapping des noms alternatifs
        Map<String, String> columnMapping = Map.of(
            "starttime", COLUMN_START_TIME,
            "endtime", "end_time",
            "processid", "process_id",
            "lastupdatetime", "last_update_time"
        );

        return columnMapping.getOrDefault(cleanSortBy, COLUMN_START_TIME);
    }
}
