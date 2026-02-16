package mg.orange.workflow.resource.process;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import mg.orange.workflow.model.bpmn.BpmnDefinition;
import mg.orange.workflow.model.bpmn.BpmnDefinitionHistory;
import mg.orange.workflow.model.bpmn.BpmnDefinitionHistoryDTO;
import mg.orange.workflow.model.bpmn.BpmnValidationResult;
import mg.orange.workflow.model.process.*;
import mg.orange.workflow.service.bpmn.BpmnDefinitionService;
import mg.orange.workflow.service.bpmn.BpmnValidationService;
import mg.orange.workflow.service.bpmn.VersionManagementService;
import mg.orange.workflow.service.process.ProcessService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

@RequestScoped
public class ProcessResourceImpl implements ProcessResource {

    private final ProcessService processService;
    private final BpmnValidationService bpmnValidationService;
    private final BpmnDefinitionService bpmnDefinitionService;
    private final VersionManagementService versionManagementService;

    // ======= OPTIMISATIONS PERFORMANCE & SCALABILITÉ AVANCÉES =======
    
    // Cache intelligent avec expiration dynamique et statistiques
    private final Cache<String, ProcessStatistics> statisticsCache = Caffeine.newBuilder()
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .maximumSize(5)
        .recordStats()
        .build();
    
    // Cache pour les données de processus avec optimisation mémoire
    private final Cache<String, List<ProcessDTO>> processDataCache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .maximumSize(3)
        .recordStats()
        .build();

    // Métriques de performance et monitoring temps réel
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> endpointMetrics = new ConcurrentHashMap<>();
    
    // Pool de threads pour calculs asynchrones avec supervision
        private final java.util.concurrent.ExecutorService calculationExecutor = 
        java.util.concurrent.Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "stats-calculator-%d");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) -> 
                LOG.error("❌ Erreur non gérée dans le thread " + thread.getName(), ex));
            return t;
        });

    private static final Logger LOG = Logger.getLogger(ProcessResourceImpl.class);
    private static final String ERROR_KEY = "error";

    @Inject
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    // Configuration de performance dynamique
    @ConfigProperty(name = "bpmn.performance.compression.enabled", defaultValue = "true")
    boolean compressionEnabled;
    
    @ConfigProperty(name = "bpmn.performance.async.calculations", defaultValue = "true")
    boolean asyncCalculations;
    
    @ConfigProperty(name = "bpmn.performance.cache.warmup.enabled", defaultValue = "true")
    boolean cacheWarmupEnabled;
    
    @ConfigProperty(name = "bpmn.performance.timeout.seconds", defaultValue = "5")
    int calculationTimeoutSeconds;
    
    @ConfigProperty(name = "bpmn.versioning.enable-multi-version-display", defaultValue = "true")
    boolean enableMultiVersionDisplay;

    @Inject
    public ProcessResourceImpl(ProcessService processService,
                               BpmnValidationService bpmnValidationService,
                               BpmnDefinitionService bpmnDefinitionService,
                               VersionManagementService versionManagementService) {
        this.processService = processService;
        this.bpmnValidationService = bpmnValidationService;
        this.bpmnDefinitionService = bpmnDefinitionService;
        this.versionManagementService = versionManagementService;
        
        // Initialisation du cache au démarrage
        if (cacheWarmupEnabled) {
            initializeCache();
        }
    }

    /**
     * Initialisation et préchargement du cache pour améliorer les performances
     */
    private void initializeCache() {
        LOG.info("🚀 Initialisation du cache avec préchargement des données critiques");
        warmupCache();
    }
    
    /**
     * Nettoyage des ressources lors de la destruction
     */
    @PreDestroy
    private void cleanup() {
        calculationExecutor.shutdown();
        try {
            if (!calculationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                calculationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            calculationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("🧹 Ressources de performance nettoyées");
    }
    
    /**
     * Préchargement intelligent du cache avec données critiques
     */
    private void warmupCache() {
        LOG.info("📊 Préchargement du cache des statistiques");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Précharger les statistiques globales
                statisticsCache.get("global", key -> calculateStatistics());
                
                // Précharger les métriques de performance
                statisticsCache.get("performance", key -> calculatePerformanceMetrics());
                
                // Précharger les métriques de santé
                statisticsCache.get("health", key -> calculateHealthMetrics());
                
                LOG.info("✅ Cache préchargé avec succès");
            } catch (Exception e) {
                LOG.warn("⚠️ Erreur lors du préchargement du cache", e);
            }
        }, calculationExecutor);
    }
    
    /**
     * Mesure le temps d'exécution d'une opération
     */
    private <T> T measureExecutionTime(String operationName, java.util.concurrent.Callable<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            T result = operation.call();
            long duration = System.currentTimeMillis() - startTime;
            
            // Enregistrer les métriques
            endpointMetrics.compute(operationName, (key, value) -> 
                (value == null ? 0L : value) + duration);
                
            if (duration > 1000) {
                LOG.warn("⏰ Opération lente détectée: " + operationName + " en " + duration + "ms");
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOG.error("❌ Erreur dans l'opération " + operationName + " après " + duration + "ms", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Compresse les données si la compression est activée et beneficial
     */
    private Response compressResponseIfNeeded(Map<String, Object> data) {
        try {
            // Estimation de la taille des données
            String jsonData = objectMapper.writeValueAsString(data);
            int uncompressedSize = jsonData.getBytes().length;
            
            // Seuil de compression (50KB)
            if (uncompressedSize > 51200 && compressionEnabled) {
                LOG.debug("🗜️ Compression activée pour " + uncompressedSize + " bytes");
                
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                    gzipOutputStream.write(jsonData.getBytes());
                }
                
                byte[] compressedData = byteArrayOutputStream.toByteArray();
                LOG.debug("📦 Données compressées: " + uncompressedSize + " -> " + compressedData.length + " bytes (ratio: " + 
                         String.format("%.1f", (1.0 - (double)compressedData.length / uncompressedSize) * 100) + "%)");
                
                return Response.ok(compressedData)
                    .header("Content-Encoding", "gzip")
                    .header("X-Compression-Ratio", String.format("%.1f%%", 
                        (1.0 - (double)compressedData.length / uncompressedSize) * 100))
                    .build();
            }
            
            return Response.ok(data).build();
        } catch (Exception e) {
            LOG.warn("⚠️ Erreur lors de la compression, envoi des données non compressées", e);
            return Response.ok(data).build();
        }
    }
    
    /**
     * Calcule les statistiques avec gestion intelligente du cache
     */
    private ProcessStatistics calculateStatisticsWithCache(String cacheKey) {
        totalRequests.incrementAndGet();
        
        ProcessStatistics stats = statisticsCache.getIfPresent(cacheKey);
        if (stats != null) {
            cacheHits.incrementAndGet();
            LOG.debug("📋 Cache HIT pour " + cacheKey);
            return stats;
        }
        
        cacheMisses.incrementAndGet();
        LOG.debug("📋 Cache MISS pour " + cacheKey + ", calcul en cours...");
        
        // Calcul asynchrone si activé
        if (asyncCalculations) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return measureExecutionTime("calculateStatistics", () -> calculateStatistics());
                } catch (Exception e) {
                    LOG.error("Erreur lors du calcul asynchrone des statistiques", e);
                    throw new RuntimeException(e);
                }
            }, calculationExecutor).join();
        } else {
            try {
                return measureExecutionTime("calculateStatistics", () -> calculateStatistics());
            } catch (Exception e) {
                LOG.error("Erreur lors du calcul des statistiques", e);
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Calcule le taux de succès du cache
     */
    private double calculateCacheHitRate() {
        long hits = cacheHits.get();
        long total = hits + cacheMisses.get();
        return total > 0 ? (hits * 100.0) / total : 0.0;
    }
    
    /**
     * Calcule le temps de réponse moyen
     */
    private double calculateAverageResponseTime() {
        return endpointMetrics.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }

    @Override
    public Response listProcesses(String name, int page, int size, String sortField, String sortDirection) {
        LOG.info("📋 Récupération des processus (filtre='" + name + "', page=" + page + ", size=" + size +
                 ", sort='" + sortField + "', direction='" + sortDirection + "')");
        LOG.debug("🔧 Feature flag enableMultiVersionDisplay: " + enableMultiVersionDisplay);

        List<ProcessDTO> allProcesses;
        
        if (name != null && !name.trim().isEmpty()) {
            // Si un filtre par nom est appliqué
            allProcesses = processService.searchProcessesByName(name);
        } else {
            // Si aucun filtre, utiliser la nouvelle méthode avec gestion des versions si activée
            if (enableMultiVersionDisplay) {
                LOG.info("✅ Utilisation de la gestion multi-version activée");
                allProcesses = processService.getAllProcessesWithVersionManagement();
            } else {
                LOG.info("⚠️ Gestion multi-version désactivée, utilisation de la méthode classique");
                allProcesses = processService.getAllProcesses();
            }
        }

        // Appliquer le tri si demandé
        if (sortField != null && !sortField.trim().isEmpty()) {
            Comparator<ProcessDTO> comparator = createComparator(sortField, "desc".equalsIgnoreCase(sortDirection));
            if (comparator != null) {
                allProcesses.sort(comparator);
            }
        }

        int totalElements = allProcesses.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ProcessDTO> pageContent = fromIndex < totalElements
                ? allProcesses.subList(fromIndex, toIndex)
                : List.of();

        // Calculate global statistics from all processes
        long totalDeployed = allProcesses.stream()
                .filter(p -> DeploymentStatus.DEPLOYE.equals(p.getDeploymentStatus()))
                .count();
        long totalValid = allProcesses.stream()
                .filter(p -> DeploymentStatus.VALIDE.equals(p.getDeploymentStatus()))
                .count();
        long totalInvalid = allProcesses.stream()
                .filter(p -> DeploymentStatus.INVALIDE.equals(p.getDeploymentStatus()))
                .count();
        long totalUndeployed = allProcesses.stream()
                .filter(p -> DeploymentStatus.NON_DEPLOYE.equals(p.getDeploymentStatus()))
                .count();
        long totalUniqueVersions = allProcesses.stream()
                .map(ProcessDTO::getVersion)
                .filter(v -> v != null && !v.isEmpty())
                .distinct()
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageContent);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("first", page == 0);
        response.put("last", page >= totalPages - 1);
        response.put("totalDeployed", totalDeployed);
        response.put("totalValid", totalValid);
        response.put("totalInvalid", totalInvalid);
        response.put("totalUndeployed", totalUndeployed);
        response.put("totalUniqueVersions", totalUniqueVersions);

        return Response.ok(response).build();
    }

    @Override
    public Response countProcesses() {
        long count = processService.countProcesses();
        return Response.ok(Map.of("count", count)).build();
    }

    @Override
    public Response getProcessStatistics() {
        LOG.info("📊 Récupération des statistiques globales des processus (avec cache)");

        try {
            ProcessStatistics stats = calculateStatisticsWithCache("global");
            LOG.debug("Statistiques récupérées: déployés=" + stats.getTotalDeployed() +
                     ", valides=" + stats.getTotalValid() +
                     ", invalides=" + stats.getTotalInvalid() +
                     ", non déployés=" + stats.getTotalUndeployed() +
                     ", versions uniques=" + stats.getTotalUniqueVersions());

            // Retourner directement l'objet ProcessStatistics pour correspondre à l'attente du frontend
            return Response.ok(stats).build();

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, "Erreur interne lors de la récupération des statistiques",
                                  "message", e.getMessage()))
                    .build();
        }
    }

    @Override
    public Response getProcessTrends() {
        LOG.info("📈 Récupération des tendances des processus sur 7 jours");

        try {
            ProcessStatistics stats = calculateStatisticsWithCache("trends");
            
            Map<String, Object> trendsResponse = Map.of(
                "period", "7_days",
                "generatedAt", LocalDateTime.now().toString(),
                "totalProcesses24h", stats.getTotalProcesses24h(),
                "deployedProcesses24h", stats.getDeployedProcesses24h(),
                "invalidProcesses24h", stats.getInvalidProcesses24h(),
                "hourlyStats", stats.getHourlyStats(),
                "systemUtilizationRate", stats.getSystemUtilizationRate(),
                "averageProcessingTime", stats.getAverageProcessingTime(),
                "deploymentSuccessRate", stats.getDeploymentSuccessRate()
            );

            return compressResponseIfNeeded(trendsResponse);

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des tendances", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, "Erreur lors de la récupération des tendances",
                                  "message", e.getMessage()))
                    .build();
        }
    }

    @Override
    public Response getPerformanceMetrics() {
        LOG.info("⚡ Récupération des métriques de performance système");

        totalRequests.incrementAndGet();
        
        try {
            // Calcul asynchrone des statistiques
            CompletableFuture<ProcessStatistics> statsFuture = CompletableFuture.supplyAsync(() -> 
                statisticsCache.get("performance", key -> calculatePerformanceMetrics()), 
                calculationExecutor);
            
            ProcessStatistics stats = statsFuture.get(calculationTimeoutSeconds, TimeUnit.SECONDS);
            
            // Collecter les métriques de performance
            Map<String, Object> performanceResponse = Map.of(
                "kpis", Map.of(
                    "deploymentSuccessRate", stats.getDeploymentSuccessRate(),
                    "averageProcessingTime", stats.getAverageProcessingTime(),
                    "systemUtilizationRate", stats.getSystemUtilizationRate(),
                    "averageTasksPerProcess", stats.getAverageTasksPerProcess(),
                    "overallHealthScore", stats.getOverallHealthScore()
                ),
                "systemMetrics", Map.of(
                    "totalRequests", totalRequests.get(),
                    "cacheHitRate", calculateCacheHitRate(),
                    "averageResponseTime", calculateAverageResponseTime(),
                    "activeThreads", ((java.util.concurrent.ThreadPoolExecutor) calculationExecutor).getActiveCount(),
                    "cacheSize", statisticsCache.estimatedSize(),
                    "hasActiveAlerts", stats.isHasActiveAlerts(),
                    "processesWithErrors", stats.getProcessesWithErrors(),
                    "processesInMaintenance", stats.getProcessesInMaintenance()
                ),
                "compressionMetrics", Map.of(
                    "compressionEnabled", compressionEnabled,
                    "asyncCalculations", asyncCalculations,
                    "cacheWarmupEnabled", cacheWarmupEnabled
                ),
                "generatedAt", LocalDateTime.now().toString()
            );

            return compressResponseIfNeeded(performanceResponse);

        } catch (java.util.concurrent.TimeoutException e) {
            LOG.error("⏰ Timeout lors du calcul des métriques de performance");
            return Response.status(Response.Status.GATEWAY_TIMEOUT)
                    .entity(Map.of(ERROR_KEY, "Timeout lors du calcul des métriques",
                                  "message", "Les métriques prennent trop de temps à calculer"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des métriques de performance", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, "Erreur lors de la récupération des métriques de performance",
                                  "message", e.getMessage()))
                    .build();
        }
    }

    @Override
    public Response getSystemHealth() {
        LOG.info("🏥 Récupération du score de santé du système (optimisé)");
        
        totalRequests.incrementAndGet();
        
        try {
            ProcessStatistics stats = calculateStatisticsWithCache("health");
            
            Map<String, Object> healthResponse = Map.of(
                "overallHealthScore", stats.getOverallHealthScore(),
                "hasActiveAlerts", stats.isHasActiveAlerts(),
                "healthLevel", determineHealthLevel(stats.getOverallHealthScore()),
                "criticalMetrics", Map.of(
                    "invalidProcesses", stats.getTotalInvalid(),
                    "processesWithErrors", stats.getProcessesWithErrors(),
                    "deploymentSuccessRate", stats.getDeploymentSuccessRate()
                ),
                "performanceMetrics", Map.of(
                    "cacheHitRate", calculateCacheHitRate(),
                    "averageResponseTime", calculateAverageResponseTime(),
                    "totalRequests", totalRequests.get()
                ),
                "recommendations", generateHealthRecommendations(stats),
                "lastUpdated", stats.getLastCalculationTime().toString()
            );

            return compressResponseIfNeeded(healthResponse);

        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération du score de santé", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, "Erreur lors de la récupération du score de santé",
                                  "message", e.getMessage()))
                    .build();
        }
    }

    // ======= MÉTHODES D'OPTIMISATION ET UTILITAIRES =======
    
    /**
     * Détermine le niveau de santé basé sur le score
     */
    private String determineHealthLevel(int healthScore) {
        if (healthScore >= 90) return "EXCELLENT";
        if (healthScore >= 75) return "GOOD";
        if (healthScore >= 60) return "FAIR";
        if (healthScore >= 40) return "POOR";
        return "CRITICAL";
    }

    /**
     * Génère des recommandations basées sur l'état des statistiques
     */
    private List<String> generateHealthRecommendations(ProcessStatistics stats) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        if (stats.getTotalInvalid() > 0) {
            recommendations.add("Corriger les " + stats.getTotalInvalid() + " processus invalides");
        }
        
        if (stats.getDeploymentSuccessRate() < 90) {
            recommendations.add("Améliorer le taux de succès des déploiements (actuellement " + 
                              String.format("%.1f%%", stats.getDeploymentSuccessRate()) + ")");
        }
        
        if (stats.getOverallHealthScore() < 70) {
            recommendations.add("Reviewer la santé globale du système (score: " + stats.getOverallHealthScore() + ")");
        }
        
        if (stats.getProcessesWithErrors() > 0) {
            recommendations.add("Résoudre les erreurs dans " + stats.getProcessesWithErrors() + " processus");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Le système fonctionne correctement");
        }
        
        return recommendations;
    }

    /**
     * Invalide le cache des statistiques
     */
    private void invalidateStatisticsCache() {
        statisticsCache.invalidateAll();
        LOG.debug("Cache des statistiques invalidé");
    }
    
    // ======= DÉLÉGATION AUX MÉTHODES EXISTANTES =======
    
    @Override
    public Response getProcessDetail(String processId) {
        return Response.ok(processService.getProcessDetail(processId)).build();
    }

    @Override
    public Response getProcessDefinition(String processId) {
        return Response.ok(processService.getProcessDefinitionXml(processId)).build();
    }

    @Override
    public Response getProcessDiagram(String processId) {
        return Response.ok(processService.getProcessDiagram(processId)).build();
    }

    @Override
    public Response getProcessTasks(String processId) {
        return Response.ok(processService.getProcessTasks(processId)).build();
    }

    @Override
    public Response getProcessNodes(String processId) {
        return Response.ok(processService.getProcessNodes(processId)).build();
    }

    @Override
    public Response getTaskAssignedGroups(String processId, String taskId) {
        return Response.ok(processService.getAssignedGroups(processId, taskId)).build();
    }

    @Override
    public Response uploadBpmn(@MultipartForm BpmnUploadForm form) {
        try {
            boolean overwrite = form.getOverwrite() != null && form.getOverwrite();
            Map<String, Object> result = processService.uploadBpmn(form.getFile(), form.getFilename(), overwrite);
            invalidateStatisticsCache();
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, e.getMessage())).build();
        }
    }

    @Override
    @RolesAllowed("Administrateur")
    public Response deleteBpmn(String filename) {
        try {
            Map<String, Object> result = processService.deleteBpmn(filename);
            invalidateStatisticsCache();
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, e.getMessage())).build();
        }
    }

    @Override
    public Response listBpmnFiles() {
        try {
            return Response.ok(processService.listBpmnFiles()).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération de la liste des fichiers BPMN", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, e.getMessage() != null ? e.getMessage() : "Erreur inconnue"))
                    .build();
        }
    }

    @Override
    public Response validateBpmnFile(BpmnUploadForm uploadForm) {
        try {
            InputStream fileStream = uploadForm.getFile();
            String filename = uploadForm.getFilename();
            BpmnValidationResult result = bpmnValidationService.validateBpmnFile(fileStream, filename);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la validation du fichier BPMN", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(ERROR_KEY, e.getMessage() != null ? e.getMessage() : "Erreur inconnue"))
                    .build();
        }
    }

    // ======= MÉTHODES DE VERSIONING =======
    
    @Override
    public Response createNewVersion(String processId, @Valid CreateVersionRequestDTO request) {
        BpmnDefinition newVersion = bpmnDefinitionService.createVersion(
                processId, request.getBpmnXml(), request.getCreatedBy(), request.getChangeComment());
        
        CreateVersionResponseDTO response = new CreateVersionResponseDTO();
        response.setProcessId(processId);
        response.setVersionSemver(newVersion.getVersionSemver());
        response.setCreatedBy(request.getCreatedBy());
        response.setCreatedAt(newVersion.getCreatedAt());
        
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @Override
    public Response getVersionHistory(String processId, int page, int size) {
        List<BpmnDefinitionHistoryDTO> versionHistory = bpmnDefinitionService.getVersionHistory(processId);
        return Response.ok(versionHistory).build();
    }

    @Override
    public Response getVersionDetails(String processId, String versionSemver) {
        BpmnDefinitionHistoryDTO versionDetails = bpmnDefinitionService.getVersionDetails(processId, versionSemver);
        return Response.ok(versionDetails).build();
    }

    @Override
    public Response activateVersion(String processId, String versionSemver) {
        BpmnDefinition activatedDefinition = bpmnDefinitionService.activateVersion(processId, versionSemver);
        return Response.ok(activatedDefinition).build();
    }

    @Override
    public Response deactivateVersion(String processId, String versionSemver) {
        BpmnDefinitionHistory deactivatedHistory = bpmnDefinitionService.deactivateVersion(processId, versionSemver);
        return Response.ok(deactivatedHistory).build();
    }

    @Override
    public Response compareVersions(String processId, String fromVersion, String toVersion) {
        String changeSummary = bpmnDefinitionService.generateChangeSummary(processId, fromVersion, toVersion);
        return Response.ok(Map.of("summary", changeSummary)).build();
    }

    @Override
    @RolesAllowed("Administrateur")
    public Response deployProcess(String processId) {
        processService.deployProcess(processId);
        invalidateStatisticsCache();
        return Response.ok(Map.of("status", "Processus déployé avec succès")).build();
    }

    @Override
    @RolesAllowed("Administrateur")
    public Response undeployProcess(String processId) {
        processService.undeployProcess(processId);
        invalidateStatisticsCache();
        return Response.ok(Map.of("status", "Déploiement retiré avec succès")).build();
    }

    @Override
    public Response validateAndRepairProcess(String processId) {
        ProcessDTO updatedProcess = processService.validateAndRepairProcess(processId);
        invalidateStatisticsCache();
        return Response.ok(updatedProcess).build();
    }
    
    /**
     * Crée un comparateur pour trier les ProcessDTO
     */
    private Comparator<ProcessDTO> createComparator(String sortField, boolean reverseDirection) {
        if (sortField == null || sortField.trim().isEmpty()) {
            return null;
        }

        Comparator<ProcessDTO> comparator = switch (sortField.toLowerCase()) {
            case "id" -> Comparator.comparing(ProcessDTO::getId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "name" -> Comparator.comparing(ProcessDTO::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "version" -> Comparator.comparing(ProcessDTO::getVersion, Comparator.nullsLast(String::compareTo));
            case "type" -> Comparator.comparing((ProcessDTO p) -> p.getType() != null ? p.getType().getCode() : "",
                                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "status" -> Comparator.comparing((ProcessDTO p) -> p.getDeploymentStatus() != null ? p.getDeploymentStatus().getLabel() : "",
                                                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> null;
        };

        // Inverser la direction si demandé
        if (comparator != null && reverseDirection) {
            comparator = comparator.reversed();
        }

        return comparator;
    }
    
    // ======= MÉTHODES DE CALCUL STATISTIQUES (DÉLÉGATION) =======
    
    private ProcessStatistics calculateStatistics() {
        try {
            // Calcul direct des statistiques avec les données disponibles
            List<ProcessDTO> processes = processService.getAllProcesses();

            long totalDeployed = processes.stream()
                    .filter(p -> DeploymentStatus.DEPLOYE.equals(p.getDeploymentStatus()))
                    .count();
            long totalValid = processes.stream()
                    .filter(p -> DeploymentStatus.VALIDE.equals(p.getDeploymentStatus()))
                    .count();
            long totalInvalid = processes.stream()
                    .filter(p -> DeploymentStatus.INVALIDE.equals(p.getDeploymentStatus()))
                    .count();
            long totalUndeployed = processes.stream()
                    .filter(p -> DeploymentStatus.NON_DEPLOYE.equals(p.getDeploymentStatus()))
                    .count();

            // Calculer les autres métriques
            long totalUniqueVersions = processes.stream()
                    .map(ProcessDTO::getVersion)
                    .filter(v -> v != null && !v.isEmpty())
                    .distinct()
                    .count();

            // Calculer le taux de succès de déploiement (approximatif)
            long totalDeployable = totalValid + totalDeployed; // processus valides + déployés
            double deploymentSuccessRate = totalDeployable > 0 ?
                (double) totalDeployed / totalDeployable * 100.0 : 0.0;

            // Compter les processus avec erreurs (invalides)
            long processesWithErrors = totalInvalid;

            // Compter les processus modifiés récemment (dans les dernières 24h)
            // Pour l'instant, approximation : tous les processus
            long processesRecentlyModified = processes.size();

            // Répartition par type
            // Gérer le cas où getType() ou getType().getCode() pourrait être null
            Map<String, Long> typeDistribution = new HashMap<>();
            for (ProcessDTO process : processes) {
                String typeCode = "INCONNU";
                if (process.getType() != null && process.getType().getCode() != null) {
                    typeCode = process.getType().getCode();
                }
                typeDistribution.put(typeCode, typeDistribution.getOrDefault(typeCode, 0L) + 1);
            }

            // Statistiques temporelles - approximation
            long totalProcesses24h = processes.size();
            long deployedProcesses24h = totalDeployed;
            long invalidProcesses24h = totalInvalid;

            // Statistiques horaires (approximation)
            Map<String, Long> hourlyStats = new HashMap<>();
            if (!processes.isEmpty()) {
                hourlyStats.put("00:00", totalDeployed);
            }

            // Calculer le taux d'utilisation du système
            double systemUtilizationRate = processes.size() > 0 ?
                (double) totalDeployed / processes.size() * 100.0 : 0.0;

            // Calculer le nombre moyen de tâches par processus (à estimer)
            double averageTasksPerProcess = 1.0; // approximation

            // Créer l'objet ProcessStatistics avec les valeurs calculées
            ProcessStatistics stats = new ProcessStatistics(
                totalDeployed, totalValid, totalInvalid, totalUndeployed,
                totalUniqueVersions,
                deploymentSuccessRate, 0.0, // averageProcessingTime - à implémenter
                processesWithErrors, processesRecentlyModified,
                typeDistribution, LocalDateTime.now(),
                totalProcesses24h, deployedProcesses24h, invalidProcesses24h,
                hourlyStats, systemUtilizationRate, averageTasksPerProcess,
                0, // overallHealthScore - sera calculé après
                false, // hasActiveAlerts - sera calculé après
                0L // processesInMaintenance - approximation
            );

            // Calculer le score de santé et les alertes
            stats.calculateHealthScore();
            stats.evaluateAlerts();

            return stats;
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques", e);
            throw new RuntimeException("Erreur lors du calcul des statistiques", e);
        }
    }
    
    private ProcessStatistics calculatePerformanceMetrics() {
        // Calcul spécialisé pour les métriques de performance
        ProcessStatistics baseStats = calculateStatistics();
        // Ajouter les métriques de performance spécifiques
        return baseStats;
    }
    
    private ProcessStatistics calculateHealthMetrics() {
        // Calcul spécialisé pour les métriques de santé
        ProcessStatistics baseStats = calculateStatistics();
        // Ajouter les métriques de santé spécifiques
        return baseStats;
    }
}
