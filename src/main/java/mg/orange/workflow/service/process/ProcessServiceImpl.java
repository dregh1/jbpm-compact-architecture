package mg.orange.workflow.service.process;

import mg.orange.workflow.exception.ApiException;
import mg.orange.workflow.model.bpmn.*;
import mg.orange.workflow.model.groupe.GroupeResponseDTO;
import mg.orange.workflow.model.process.*;
import mg.orange.workflow.repository.BpmnDefinitionHistoryRepository;
import mg.orange.workflow.repository.BpmnDefinitionRepository;
import mg.orange.workflow.repository.TaskGroupAssignmentRepository;
import mg.orange.workflow.service.bpmn.BpmnDefinitionService;
import mg.orange.workflow.service.bpmn.BpmnParserService;
import mg.orange.workflow.service.bpmn.BpmnValidationService;
import org.jboss.logging.Logger;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.Processes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProcessServiceImpl implements ProcessService {

    @Inject
    Processes processes;

    @Inject
    BpmnParserService bpmnParserService;

    @Inject
    TaskGroupAssignmentRepository taskGroupAssignmentRepository;

    @Inject
    BpmnValidationService bpmnValidationService;

    @Inject
    BpmnDefinitionService bpmnDefinitionService;

    @Inject
    BpmnDefinitionHistoryRepository bpmnDefinitionHistoryRepository;

    @Inject
    BpmnDefinitionRepository bpmnDefinitionRepository;

    @Inject
    mg.orange.workflow.service.bpmn.BpmnNormalizerService bpmnNormalizerService;

    private static final Logger LOG = Logger.getLogger(ProcessServiceImpl.class);
    private static final String BPMN_PATH = "src/main/resources/";

    @Override
    public List<ProcessDTO> getAllProcesses() {
        List<ProcessDTO> processDefinitions = new ArrayList<>();

        LOG.info("Récupération de la liste de tous les processus (Kogito + base de données)...");

        // Récupérer les processus déployés dans Kogito (status = DEPLOYE)
        Set<String> deployedProcessIds = new HashSet<>(processes.processIds());
        processes.processIds().forEach(processId -> {
            Process<?> process = processes.processById(processId);
            if (process != null) {
                ProcessDTO dto = mapToDTO(process);
                dto.setDeploymentStatus(DeploymentStatus.DEPLOYE);

                // Essayer de récupérer la version sémantique depuis la base
                try {
                    BpmnDefinition definition = bpmnDefinitionService.getByProcessId(processId);
                    if (definition != null && definition.getVersionSemver() != null) {
                        dto.setVersion(definition.getVersionSemver());
                        // Déterminer le type de changement basé sur la version
                        dto.setVersionChangeType(determineVersionChangeType(definition.getVersionSemver()));
                    } else {
                        // Si pas de version sémantique, créer une version par défaut 1.0.0
                        dto.setVersion("1.0.0");
                        dto.setVersionChangeType(VersionChangeType.MAJOR);
                        LOG.debug("Version par défaut 1.0.0 assignée au processus déployé: " + processId);
                    }
                } catch (Exception e) {
                    // En cas d'erreur, utiliser la version par défaut
                    dto.setVersion("1.0.0");
                    dto.setVersionChangeType(VersionChangeType.MAJOR);
                    LOG.debug("Erreur lors de la récupération de version pour " + processId + ", utilisation de 1.0.0");
                }

                processDefinitions.add(dto);
            }
        });

        // Récupérer aussi les définitions BPMN sauvegardées en base (versioning) mais pas déployées
        try {
            List<BpmnDefinitionDTO> databaseDefinitions = bpmnDefinitionService.getAllDefinitions();

            for (BpmnDefinitionDTO def : databaseDefinitions) {
                // Si le processus n'est pas déployé dans Kogito
                if (!deployedProcessIds.contains(def.getProcessId())) {
                    LOG.info("[BPMN DB] Ajout processId=" + def.getProcessId() + ", name=" + def.getName() + ", version=" + def.getVersionSemver());
                    ProcessDTO dto = new ProcessDTO();
                    dto.setId(def.getProcessId());
                    dto.setName(def.getName()); // Correction: getName() au lieu de getProcessName()
                    dto.setVersion(def.getVersionSemver() != null ? def.getVersionSemver() : "1.0.0"); // Version sémantique avec fallback
                    dto.setType(ProcessType.getDefault()); // BPMN2.0 par défaut
                    // Déterminer automatiquement le statut (VALIDE ou INVALIDE)
                    BpmnDefinition fullDef = bpmnDefinitionService.getByProcessId(def.getProcessId());
                    String bpmnXml = fullDef != null ? fullDef.getBpmnXml() : null;
                    dto.setDeploymentStatus(determineDeploymentStatus(def.getProcessId(), bpmnXml));
                    processDefinitions.add(dto);
                }
            }

            LOG.info("Récupérés " + processDefinitions.size() + " processus (déployés + base)");
        } catch (Exception e) {
            LOG.warn("Erreur lors de la récupération des définitions BPMN en base: " + e.getMessage());
            // Continuer sans les processus de la base en cas d'erreur
        }

        return processDefinitions;
    }

    @Override
    public List<ProcessDTO> getAllProcessesWithVersionManagement() {
        List<ProcessDTO> processDefinitions = new ArrayList<>();
        Map<String, ProcessDTO> processMap = new HashMap<>();

        LOG.info("🔍 Récupération intelligente des processus avec gestion multi-versions");

        // ÉTAPE 1: Récupérer les processus déployés dans Kogito
        Set<String> deployedProcessIds = new HashSet<>(processes.processIds());
        
        for (String processId : deployedProcessIds) {
            Process<?> process = processes.processById(processId);
            if (process == null) continue;

            ProcessDTO dto = mapToDTO(process);
            dto.setDeploymentStatus(DeploymentStatus.DEPLOYE);

            // Chercher la version déployée dans l'historique
            try {
                // Récupérer la version actuelle depuis BpmnDefinition
                BpmnDefinition currentDef = bpmnDefinitionService.getByProcessId(processId);
                String deployedVersion = currentDef != null && currentDef.getVersionSemver() != null 
                    ? currentDef.getVersionSemver() 
                    : "1.0.0";

                // Chercher cette version dans l'historique pour avoir tous les détails
                Optional<BpmnDefinitionHistory> deployedHistory = 
                    bpmnDefinitionHistoryRepository.findByProcessIdAndVersionSemver(
                        processId, deployedVersion);

                if (deployedHistory.isPresent()) {
                    BpmnDefinitionHistory history = deployedHistory.get();
                    dto.setVersion(history.getVersionSemver());
                    dto.setVersionChangeType(history.getChangeType());
                    LOG.debug("✅ Processus déployé " + processId + " - version " + deployedVersion + " trouvée");
                } else {
                    // Pas d'historique, utiliser la version de base
                    dto.setVersion(deployedVersion);
                    dto.setVersionChangeType(determineVersionChangeType(deployedVersion));
                    LOG.debug("⚠️ Processus déployé " + processId + " - pas d'historique, version par défaut: " + deployedVersion);
                }
            } catch (Exception e) {
                dto.setVersion("1.0.0");
                dto.setVersionChangeType(VersionChangeType.MAJOR);
                LOG.warn("❌ Erreur récupération version pour " + processId + ": " + e.getMessage());
            }

            processMap.put(processId, dto);
        }

        // ÉTAPE 2: Récupérer les processus NON déployés depuis l'historique
        try {
            // Récupérer tous les processIds uniques de l'historique
            List<BpmnDefinitionHistory> allHistoryEntries = 
                bpmnDefinitionHistoryRepository.listAll();
            
            // Grouper par processId
            Map<String, List<BpmnDefinitionHistory>> historyByProcess = 
                allHistoryEntries.stream()
                    .collect(Collectors.groupingBy(BpmnDefinitionHistory::getProcessId));

            for (Map.Entry<String, List<BpmnDefinitionHistory>> entry : historyByProcess.entrySet()) {
                String processId = entry.getKey();
                
                // Si déjà traité comme processus déployé, ignorer
                if (processMap.containsKey(processId)) {
                    continue;
                }

                // Trouver la dernière version ACTIVE
                Optional<BpmnDefinitionHistory> latestActive = 
                    entry.getValue().stream()
                        .filter(h -> "ACTIVE".equals(h.getStatus()))
                        .max(Comparator.comparing(BpmnDefinitionHistory::getVersionSemver));

                if (latestActive.isPresent()) {
                    BpmnDefinitionHistory history = latestActive.get();
                    
                    ProcessDTO dto = new ProcessDTO();
                    dto.setId(processId);
                    dto.setName(history.getName());
                    dto.setVersion(history.getVersionSemver());
                    dto.setVersionChangeType(history.getChangeType());
                    dto.setType(ProcessType.getDefault());
                    
                    // Déterminer automatiquement le statut (VALIDE ou INVALIDE)
                    dto.setDeploymentStatus(determineDeploymentStatus(processId, history.getBpmnXml()));
                    
                    processMap.put(processId, dto);
                    LOG.debug(" Processus non déployé " + processId + " - dernière version active: " + history.getVersionSemver());
                }
            }

            // ÉTAPE 3: Fallback - processus dans BpmnDefinition mais pas dans History ou sans version active
            List<BpmnDefinition> allDefinitions = bpmnDefinitionRepository.listAll();
            for (BpmnDefinition def : allDefinitions) {
                if (!processMap.containsKey(def.getProcessId())) {
                    LOG.infof(" Processus trouvé dans BpmnDefinition mais pas dans la liste actuelle: %s (%s)", def.getProcessId(), def.getName());
                    
                    // Vérifier s'il existe une version active dans l'historique
                    boolean hasActiveVersion = bpmnDefinitionHistoryRepository.findByProcessId(def.getProcessId())
                        .stream()
                        .anyMatch(h -> BpmnDefinition.Status.ACTIVE.equals(h.getStatus()));
                    
                    if (!hasActiveVersion) {
                        LOG.infof(" Aucune version active trouvée pour %s, création d'une entrée avec statut par défaut", def.getProcessId());
                        
                        ProcessDTO dto = new ProcessDTO();
                        dto.setId(def.getProcessId());
                        dto.setName(def.getName() != null ? def.getName() : def.getProcessId());
                        dto.setVersion(def.getVersionSemver() != null ? def.getVersionSemver() : "1.0.0");
                        dto.setVersionChangeType(determineVersionChangeType(dto.getVersion()));
                        dto.setType(ProcessType.getDefault());
                        
                        // Déterminer automatiquement le statut (VALIDE ou INVALIDE)
                        dto.setDeploymentStatus(determineDeploymentStatus(def.getProcessId(), def.getBpmnXml()));
                        
                        processMap.put(def.getProcessId(), dto);
                        LOG.infof(" Processus %s ajouté avec statut %s", def.getProcessId(), dto.getDeploymentStatus());
                    } else {
                        LOG.infof(" Une version active existe déjà pour %s dans l'historique, on laisse la logique existante gérer", def.getProcessId());
                    }
                }
            }

        } catch (Exception e) {
            LOG.error(" Erreur lors de la récupération des versions non déployées", e);
            LOG.error("❌ Erreur lors de la récupération des versions non déployées", e);
        }

        processDefinitions.addAll(processMap.values());
        LOG.info("✅ Total: " + processDefinitions.size() + " processus récupérés (déployés + non déployés)");

        return processDefinitions;
    }

    @Override
    public List<ProcessDTO> searchProcessesByName(String name) {
        LOG.info("🔍 Recherche de processus par nom : " + name);

        if (name == null || name.trim().isEmpty()) {
            // Utiliser la méthode avec gestion des versions si disponible
            return getAllProcessesWithVersionManagement();
        }

        String searchTerm = name.toLowerCase().trim();

        // Effectuer la recherche sur la liste avec gestion des versions
        return getAllProcessesWithVersionManagement().stream()
                .filter(process -> {
                    String processName = process.getName() != null ?
                            process.getName().toLowerCase() : "";
                    String processId = process.getId() != null ?
                            process.getId().toLowerCase() : "";

                    // Recherche dans le nom ET l'ID du processus
                    return processName.contains(searchTerm) ||
                            processId.contains(searchTerm);
                })
                .collect(Collectors.toList());
    }


    @Override
    public long countProcesses() {
        LOG.info("Comptage du nombre total de processus...");
        return processes.processIds().size();
    }

    @Override
    public ProcessDetailDTO getProcessDetail(String processId) {
        LOG.info("Récupération des détails complets du processus : " + processId);

        if (processId == null || processId.trim().isEmpty()) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                                   "L'ID du processus est requis",
                                   "INVALID_PROCESS_ID");
        }

        // Essayer d'abord de récupérer depuis Kogito (processus déployés)
        Process<?> process = processes.processById(processId);

        String bpmnXml = null;
        ProcessDetailDTO detail = new ProcessDetailDTO();

        if (process != null) {
            // Processus déployé trouvé dans Kogito
            LOG.info("✅ Processus déployé trouvé dans Kogito: " + processId);
            detail.setId(process.id());
            detail.setName(process.name());
            detail.setVersion(process.version());
            detail.setType(process.type());

            // Récupérer le XML depuis les ressources déployées
            try {
                bpmnXml = getProcessDefinitionXml(processId);
                LOG.debug("XML BPMN récupéré depuis ressources déployées pour: " + processId);
            } catch (Exception e) {
                LOG.warn("⚠️ Impossible de récupérer le XML depuis les ressources pour " + processId + ": " + e.getMessage());
                // Continuer avec les infos de base
            }
        } else {
            // Processus non déployé - chercher dans la base de données
            LOG.info("⚠️ Processus non trouvé dans Kogito, recherche dans la base de données...");

            try {
                BpmnDefinition bpmnDefinition = bpmnDefinitionService.getByProcessId(processId);

                if (bpmnDefinition == null) {
                    LOG.warn("Aucune définition BPMN trouvée en base pour: " + processId);
                    throw new ApiException(Response.Status.NOT_FOUND,
                                           "Processus non trouvé avec l'ID: " + processId,
                                           "PROCESS_NOT_FOUND");
                }

                LOG.info("✅ Processus non déployé trouvé en base: " + processId +
                         " (id=" + bpmnDefinition.getId() + ", name=" + bpmnDefinition.getName() + ")");

                detail.setId(bpmnDefinition.getProcessId());
                detail.setName(bpmnDefinition.getName() != null ? bpmnDefinition.getName() : bpmnDefinition.getProcessId());
                detail.setVersion(bpmnDefinition.getVersionSemver() != null ?
                                  bpmnDefinition.getVersionSemver() : bpmnDefinition.getVersion());
                detail.setType(ProcessType.getDefault()); // Type par défaut pour les processus non déployés

                // Récupérer et valider le XML depuis la base de données
                bpmnXml = bpmnDefinition.getBpmnXml();

                if (bpmnXml == null || bpmnXml.trim().isEmpty()) {
                    LOG.warn("⚠️ XML BPMN null ou vide en base pour le processus: " + processId);
                    // Retourner quand même les infos de base sans parsing
                    detail.setTasks(new ArrayList<>());
                    detail.setNodes(new ArrayList<>());
                    return detail;
                }

                LOG.debug("XML BPMN récupéré depuis base (longueur: " + bpmnXml.length() + " caractères)");

            } catch (ApiException e) {
                throw e; // Re-throw API exceptions
            } catch (Exception e) {
                LOG.error("❌ Erreur lors de la récupération du processus en base: " + e.getMessage(), e);
                throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR,
                                       "Erreur lors de la récupération du processus: " + e.getMessage(),
                                       "PROCESS_RETRIEVAL_ERROR");
            }
        }

        // Parser le XML BPMN et ajouter les détails avec gestion d'erreur robuste
        if (bpmnXml != null && !bpmnXml.trim().isEmpty()) {
            detail.setBpmnXml(bpmnXml);

            try {
                LOG.debug("Début du parsing BPMN pour: " + processId);

                // Validation basique du XML avant parsing
                if (!isValidXml(bpmnXml)) {
                    LOG.warn("⚠️ XML BPMN invalide détecté pour " + processId + ", tentative de normalisation");
                    bpmnXml = normalizeBpmnXml(bpmnXml);
                }

                // Parser et ajouter les tâches avec fallback
                try {
                    List<ProcessTaskDTO> tasks = bpmnParserService.parseProcessTasks(bpmnXml);
                    detail.setTasks(tasks != null ? tasks : new ArrayList<>());
                    LOG.debug("Tâches parsées: " + detail.getTasks().size());
                } catch (Exception e) {
                    LOG.warn("⚠️ Erreur lors du parsing des tâches pour " + processId + ": " + e.getMessage());
                    detail.setTasks(new ArrayList<>()); // Liste vide en fallback
                }

                // Parser et ajouter les nœuds avec fallback
                try {
                    List<ProcessNodeDTO> nodes = bpmnParserService.parseProcessNodes(bpmnXml);
                    detail.setNodes(nodes != null ? nodes : new ArrayList<>());
                    LOG.debug("Nœuds parsés: " + detail.getNodes().size());
                } catch (Exception e) {
                    LOG.warn("⚠️ Erreur lors du parsing des nœuds pour " + processId + ": " + e.getMessage());
                    detail.setNodes(new ArrayList<>()); // Liste vide en fallback
                }

                // Parser et ajouter les informations du diagramme avec fallback
                try {
                    DiagramInfoDTO diagramInfo = bpmnParserService.parseDiagramInfo(bpmnXml);
                    detail.setDiagramInfo(diagramInfo);
                    LOG.debug("Informations diagramme parsées");
                } catch (Exception e) {
                    LOG.warn("⚠️ Erreur lors du parsing du diagramme pour " + processId + ": " + e.getMessage());
                    detail.setDiagramInfo(null); // Null en fallback
                }

                LOG.info("✅ Détails du processus " + processId + " parsés avec succès " +
                         "(tâches: " + detail.getTasks().size() +
                         ", nœuds: " + detail.getNodes().size() + ")");

            } catch (Exception e) {
                LOG.error("❌ Erreur critique lors du parsing BPMN pour " + processId + ": " + e.getMessage(), e);
                // En cas d'erreur critique, retourner quand même l'objet avec les listes vides
                detail.setTasks(new ArrayList<>());
                detail.setNodes(new ArrayList<>());
                detail.setDiagramInfo(null);
            }
        } else {
            LOG.warn("⚠️ Aucun XML BPMN disponible pour le parsing des détails de: " + processId);
            detail.setTasks(new ArrayList<>());
            detail.setNodes(new ArrayList<>());
            detail.setDiagramInfo(null);
        }

        return detail;
    }

    @Override
    public String getProcessDefinitionXml(String processId) {
        LOG.info("Récupération du XML BPMN pour le processus : " + processId);

        // Vérifier d'abord si le processus est déployé dans Kogito
        Process<?> process = processes.processById(processId);
        
        if (process != null) {
            // Processus déployé - charger depuis les ressources
            LOG.info("✅ Processus déployé trouvé dans Kogito: " + processId);
            
            // Essayer d'abord de charger le fichier avec le nom basé sur l'ID du processus
            String resourcePath = "/" + processId + ".bpmn2";

            try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
                if (inputStream != null) {
                    LOG.info("Fichier BPMN trouvé directement: " + resourcePath);
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                LOG.warn("Erreur lors de la lecture du fichier direct: " + resourcePath, e);
            }

            // Si le fichier direct n'existe pas, scanner tous les fichiers BPMN
            LOG.info("Fichier direct non trouvé, recherche dans tous les fichiers BPMN...");
            String foundFileContent = findBpmnFileContentByProcessId(processId);

            if (foundFileContent != null) {
                LOG.info("Fichier BPMN trouvé par scan pour le processus: " + processId);
                return foundFileContent;
            }
        } else {
            // Processus non déployé - charger depuis la base de données
            LOG.info("⚠️ Processus non trouvé dans Kogito, recherche dans la base de données...");
            
            try {
                BpmnDefinition bpmnDefinition = bpmnDefinitionService.getByProcessId(processId);
                
                if (bpmnDefinition != null) {
                    LOG.info("📦 BpmnDefinition trouvé: id=" + bpmnDefinition.getId() + ", processId=" + bpmnDefinition.getProcessId());
                    
                    if (bpmnDefinition.getBpmnXml() != null) {
                        String xml = bpmnDefinition.getBpmnXml();
                        LOG.info("✅ XML BPMN récupéré depuis la base de données pour: " + processId + " (longueur: " + xml.length() + " caractères)");
                        LOG.debug("Premier 200 caractères du XML: " + xml.substring(0, Math.min(200, xml.length())));
                        return xml;
                    } else {
                        LOG.warn("⚠️ BpmnDefinition trouvé mais bpmnXml est null");
                    }
                } else {
                    LOG.warn("⚠️ Aucun BpmnDefinition trouvé pour processId: " + processId);
                }
            } catch (Exception e) {
                LOG.error("❌ Erreur lors de la récupération du BPMN depuis la base: " + e.getMessage(), e);
            }
        }

        LOG.error("❌ Aucun fichier BPMN trouvé pour le processus: " + processId);
        throw new ApiException(Response.Status.NOT_FOUND,
                               "Définition BPMN non trouvée pour le processus: " + processId,
                               "BPMN_FILE_NOT_FOUND");
    }

    @Override
    public DiagramInfoDTO getProcessDiagram(String processId) {
        LOG.info("Récupération du diagramme pour le processus : " + processId);

        String bpmnXml = getProcessDefinitionXml(processId);
        return bpmnParserService.parseDiagramInfo(bpmnXml);
    }

    @Override
    public List<ProcessTaskDTO> getProcessTasks(String processId) {
        LOG.info("Récupération des tâches du processus : " + processId);

        String bpmnXml = getProcessDefinitionXml(processId);
        return bpmnParserService.parseProcessTasks(bpmnXml);
    }

    @Override
    public List<ProcessNodeDTO> getProcessNodes(String processId) {
        LOG.info("Récupération des nœuds du processus : " + processId);

        String bpmnXml = getProcessDefinitionXml(processId);
        return bpmnParserService.parseProcessNodes(bpmnXml);
    }

    @Override
    public List<GroupeResponseDTO> getAssignedGroups(String processId, String taskId) {
        LOG.info("Récupération des groupes assignés pour la tâche : " + processId + "/" + taskId);

        // Vérifier que le processus existe
        Process<?> process = processes.processById(processId);
        if (process == null) {
            throw new ApiException(Response.Status.NOT_FOUND,
                                   "Processus non trouvé avec l'ID: " + processId,
                                   "PROCESS_NOT_FOUND");
        }

        return taskGroupAssignmentRepository.findByProcessIdAndTaskId(processId, taskId)
                .stream()
                .map(assignment -> new GroupeResponseDTO(assignment.getGroupe()))
                .collect(Collectors.toList());
    }

    /**
     * Convertit un Process Kogito en DTO
     * @param process Le processus Kogito
     * @return Le DTO correspondant
     */
    private ProcessDTO mapToDTO(Process<?> process) {
        ProcessDTO dto = new ProcessDTO();
        dto.setId(process.id());
        dto.setName(process.name());
        dto.setVersion(process.version());
        dto.setType(process.type());
        //eto asiana an'ilay metadonnées si necessaire (any aoriana)
        return dto;
    }

    @Override
    public Map<String, Object> uploadBpmn(InputStream fileInputStream, String filename, boolean overwrite) throws Exception {
        if (fileInputStream == null)
            throw new IllegalArgumentException("Fichier requis");

        if (filename == null || !(filename.endsWith(".bpmn") || filename.endsWith(".bpmn2")))
            throw new IllegalArgumentException("Le fichier doit avoir l'extension .bpmn ou .bpmn2");

        // Lire le contenu en mémoire pour pouvoir le réutiliser
        byte[] fileContent = fileInputStream.readAllBytes();
        String bpmnContent = new String(fileContent, StandardCharsets.UTF_8);

        validateBpmn(bpmnContent);

        // Extract process ID from BPMN content to check for duplicates
        String processId = extractProcessIdFromBpmn(bpmnContent);
        if (processId != null) {
            // Check for and remove duplicate process files
            removeDuplicateProcessFiles(processId, filename);
        }
        
        // Validation du contenu
        BpmnValidationResult validationResult = 
            bpmnValidationService.validateBpmn(bpmnContent);
        
        if (!validationResult.isValid()) {
            String errorMessage = "BPMN invalide:\n" + 
                String.join("\n", validationResult.getErrors());
            throw new IllegalArgumentException(errorMessage);
        }
        
        // Sauvegarder le fichier
        Path targetPath = Paths.get(BPMN_PATH, filename);
        boolean fileExists = Files.exists(targetPath);

        // Check if file exists and handle based on overwrite flag
        if (fileExists && !overwrite) {
            throw new IllegalStateException("Un fichier avec ce nom existe déjà");
        }

        try (FileWriter writer = new FileWriter(targetPath.toFile())) {
            writer.write(bpmnContent);
        }

    
        Files.write(targetPath, fileContent);
    
        Map<String, Object> response = new HashMap<>();
        if (fileExists && overwrite) {
            response.put("message", "Fichier BPMN mis à jour avec succès. Veuillez redémarrer le serveur Quarkus pour appliquer les modifications.");
            response.put("updated", true);
            response.put("restartRequired", true);
        } else {
            response.put("message", "Fichier BPMN importé avec succès");
            response.put("updated", false);
        }
        response.put("filename", filename);

        LOG.info("Fichier BPMN " + (fileExists ? "mis à jour" : "créé") + ": " + filename);

        response.put("validation", Map.of(
            "valid", true,
            "warnings", validationResult.getWarnings(),
            "infos", validationResult.getInfos()
        ));
    
        return response;
    }


    @Override
    public Map<String, Object> deleteBpmn(String filename) throws Exception {
        if (!filename.endsWith(".bpmn") && !filename.endsWith(".bpmn2"))
            throw new IllegalArgumentException("Le fichier doit avoir l'extension .bpmn ou .bpmn2");

        Path targetPath = Paths.get(BPMN_PATH, filename);

        // If file doesn't exist directly, try to find it by process ID
        if (!Files.exists(targetPath)) {
            // Extract processId from filename (remove .bpmn2 or .bpmn extension)
            String processId = filename.replace(".bpmn2", "").replace(".bpmn", "");
            String foundFilename = findBpmnFileByProcessId(processId);

            if (foundFilename != null) {
                targetPath = Paths.get(BPMN_PATH, foundFilename);
                filename = foundFilename;
            } else {
                throw new IllegalStateException("Fichier non trouvé pour le processus: " + processId);
            }
        }

        Files.delete(targetPath);
        LOG.info("Fichier BPMN supprimé: " + filename);
        return Map.of(
            "success", true,
            "message", "BPMN supprimé avec succès. Veuillez redémarrer le serveur Quarkus pour appliquer les modifications.",
            "filename", filename,
            "restartRequired", true
        );
    }

    /**
     * Find BPMN file content by process ID by reading all BPMN files and checking their process definitions
     * @param processId The process ID to search for
     * @return The content of the BPMN file containing the process, or null if not found
     */
    private String findBpmnFileContentByProcessId(String processId) {
        File resourceDir = new File(BPMN_PATH);
        File[] bpmnFiles = resourceDir.listFiles((dir, name) -> name.endsWith(".bpmn") || name.endsWith(".bpmn2"));

        if (bpmnFiles == null) {
            return null;
        }

        for (File bpmnFile : bpmnFiles) {
            try {
                String content = Files.readString(bpmnFile.toPath(), StandardCharsets.UTF_8);
                // Check for process id="processId" in the XML
                if (content.contains("process id=\"" + processId + "\"") ||
                    content.contains("process id='" + processId + "'") ||
                    content.contains("<bpmn2:process id=\"" + processId + "\"") ||
                    content.contains("<bpmn2:process id='" + processId + "'") ||
                    content.contains("<bpmn:process id=\"" + processId + "\"") ||
                    content.contains("<bpmn:process id='" + processId + "'")) {
                    LOG.info("Found BPMN file containing process ID '" + processId + "': " + bpmnFile.getName());
                    return content;
                }
            } catch (Exception e) {
                LOG.warn("Error reading file " + bpmnFile.getName() + ": " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * Find BPMN file by process ID by reading all BPMN files and checking their process definitions
     */
    private String findBpmnFileByProcessId(String processId) {
        File resourceDir = new File(BPMN_PATH);
        File[] bpmnFiles = resourceDir.listFiles((dir, name) -> name.endsWith(".bpmn") || name.endsWith(".bpmn2"));

        if (bpmnFiles == null) {
            return null;
        }

        for (File bpmnFile : bpmnFiles) {
            try {
                String content = Files.readString(bpmnFile.toPath(), StandardCharsets.UTF_8);
                // Simple check: look for process id="processId" in the XML
                if (content.contains("process id=\"" + processId + "\"") ||
                    content.contains("process id='" + processId + "'") ||
                    content.contains("<bpmn2:process id=\"" + processId + "\"") ||
                    content.contains("<bpmn2:process id='" + processId + "'")) {
                    return bpmnFile.getName();
                }
            } catch (Exception e) {
                LOG.warn("Error reading file " + bpmnFile.getName() + ": " + e.getMessage());
            }
        }

        return null;
    }

    @Override
    public Map<String, Object> listBpmnFiles() {
        File resourceDir = new File(BPMN_PATH);
        File[] bpmnFiles = resourceDir.listFiles((dir, name) -> name.endsWith(".bpmn") || name.endsWith(".bpmn2"));

        if (bpmnFiles == null)
            return Map.of("files", new String[0], "count", 0);

        String[] filenames = new String[bpmnFiles.length];
        for (int i = 0; i < bpmnFiles.length; i++) {
            filenames[i] = bpmnFiles[i].getName();
        }

        return Map.of("files", filenames, "count", filenames.length);
    }
    
    private void validateBpmn(String content) {
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("Le contenu BPMN est requis");

        // Vérifier que c'est bien du XML valide
    
        // Validation XML de base
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(content)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Le fichier n'est pas un XML valide : " + e.getMessage());
        }

        // Vérifier la structure BPMN en utilisant XPath pour une validation plus robuste
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));

            // Créer un XPath pour naviguer dans le document avec namespaces
            javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xpathFactory.newXPath();

            // Définir les namespaces BPMN courants
            javax.xml.xpath.XPathExpression exprDefinitions = xpath.compile("//*[local-name()='definitions']");
            javax.xml.xpath.XPathExpression exprProcess = xpath.compile("//*[local-name()='process']");

            // Vérifier la présence des éléments definitions et process
            boolean hasDefinitions = exprDefinitions.evaluate(doc, javax.xml.xpath.XPathConstants.NODE) != null;
            boolean hasProcess = exprProcess.evaluate(doc, javax.xml.xpath.XPathConstants.NODE) != null;

            if (!hasDefinitions) {
                throw new IllegalArgumentException("Le fichier BPMN ne contient pas d'élément 'definitions' valide");
            }

            if (!hasProcess) {
                throw new IllegalArgumentException("Le fichier BPMN ne contient pas d'élément 'process' valide");
            }

            // Vérifier la présence du namespace BPMN standard dans l'élément definitions
            Element definitionsElement = (Element) exprDefinitions.evaluate(doc, javax.xml.xpath.XPathConstants.NODE);
            if (definitionsElement != null) {
                String namespaceURI = definitionsElement.getAttribute("xmlns:bpmn");
                if (namespaceURI == null || !namespaceURI.contains("bpmn")) {
                    // Essayer avec xmlns sans préfixe
                    namespaceURI = definitionsElement.getAttribute("xmlns");
                    if (namespaceURI == null || !namespaceURI.contains("bpmn")) {
                        LOG.warn("Avertissement : namespace BPMN standard non trouvé, mais le fichier sera accepté");
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            LOG.warn("Erreur lors de la validation avancée BPMN, tentative de validation basique : " + e.getMessage());
            // Fallback vers une validation basique si XPath échoue
            performBasicBpmnValidation(content);
        }
    }

    /**
     * Validation BPMN basique en cas d'échec de la validation XPath
     */
    private void performBasicBpmnValidation(String content) {
        String lowerContent = content.toLowerCase();

        // Vérifications basiques sans supprimer tous les espaces
        boolean hasDefinitions = lowerContent.contains("<definitions") ||
                                lowerContent.contains("<bpmn:definitions") ||
                                lowerContent.contains("<bpmn2:definitions");

        boolean hasProcess = lowerContent.contains("<process") ||
                            lowerContent.contains("<bpmn:process") ||
                            lowerContent.contains("<bpmn2:process");

        if (!hasDefinitions) {
            throw new IllegalArgumentException("Le fichier BPMN ne contient pas d'élément 'definitions'");
        }

        if (!hasProcess) {
            throw new IllegalArgumentException("Le fichier BPMN ne contient pas d'élément 'process'");
        }
    }

    /**
     * Extract process ID from BPMN content
     * @param bpmnContent The BPMN XML content
     * @return The process ID, or null if not found
     */
    private String extractProcessIdFromBpmn(String bpmnContent) {
        if (bpmnContent == null || bpmnContent.trim().isEmpty()) {
            return null;
        }

        try {
            // Try to parse with XPath for accurate extraction
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(bpmnContent)));

            javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xpathFactory.newXPath();

            // Look for process element and extract id attribute
            javax.xml.xpath.XPathExpression exprProcess = xpath.compile("//*[local-name()='process']/@id");
            String processId = (String) exprProcess.evaluate(doc, javax.xml.xpath.XPathConstants.STRING);

            if (processId != null && !processId.trim().isEmpty()) {
                LOG.debug("Extracted process ID from BPMN: " + processId);
                return processId.trim();
            }
        } catch (Exception e) {
            LOG.warn("Error parsing BPMN with XPath, falling back to regex: " + e.getMessage());
        }

        // Fallback: use regex to extract process ID
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<(?:bpmn2?:)?process[^>]*id\\s*=\\s*[\"']([^\"']+)[\"']",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(bpmnContent);

        if (matcher.find()) {
            String processId = matcher.group(1);
            LOG.debug("Extracted process ID with regex: " + processId);
            return processId;
        }

        LOG.warn("Could not extract process ID from BPMN content");
        return null;
    }

    /**
     * Remove duplicate process files that contain the same process ID
     * @param processId The process ID to check for duplicates
     * @param currentFilename The filename being uploaded (to exclude from deletion)
     */
    private void removeDuplicateProcessFiles(String processId, String currentFilename) {
        File resourceDir = new File(BPMN_PATH);
        File[] bpmnFiles = resourceDir.listFiles((dir, name) ->
            (name.endsWith(".bpmn") || name.endsWith(".bpmn2")) && !name.equals(currentFilename)
        );

        if (bpmnFiles == null) {
            return;
        }

        for (File bpmnFile : bpmnFiles) {
            try {
                String content = Files.readString(bpmnFile.toPath(), StandardCharsets.UTF_8);

                // Check if this file contains the same process ID
                if (content.contains("process id=\"" + processId + "\"") ||
                    content.contains("process id='" + processId + "'") ||
                    content.contains("<bpmn2:process id=\"" + processId + "\"") ||
                    content.contains("<bpmn2:process id='" + processId + "'") ||
                    content.contains("<bpmn:process id=\"" + processId + "\"") ||
                    content.contains("<bpmn:process id='" + processId + "'")) {

                    // Delete the duplicate file
                    boolean deleted = bpmnFile.delete();
                    if (deleted) {
                        LOG.info("Supprimé fichier BPMN dupliqué contenant le process ID '" + processId + "': " + bpmnFile.getName());
                    } else {
                        LOG.warn("Impossible de supprimer le fichier dupliqué: " + bpmnFile.getName());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Erreur lors de la vérification du fichier " + bpmnFile.getName() + ": " + e.getMessage());
            }
        }
    }
    
    public BpmnValidationResult previewValidation(String bpmnContent) {
        return bpmnValidationService.validateBpmn(bpmnContent);
    }

    @Override
    public ProcessDTO getProcessById(String processId) {
        LOG.info("Récupération du processus par ID : " + processId);

        // Chercher d'abord dans les processus déployés dans Kogito
        Process<?> kogitoProcess = processes.processById(processId);
        if (kogitoProcess != null) {
            ProcessDTO dto = mapToDTO(kogitoProcess);
            dto.setDeploymentStatus(DeploymentStatus.DEPLOYE);
            return dto;
        }

        // Si non trouvé dans Kogito, chercher dans les définitions BPMN versionnées en base
        try {
            BpmnDefinition bpmnDefinition = bpmnDefinitionService.getByProcessId(processId);
            if (bpmnDefinition != null) {
                ProcessDTO dto = new ProcessDTO();
                dto.setId(bpmnDefinition.getProcessId());
                dto.setName(bpmnDefinition.getName());
                dto.setVersion(bpmnDefinition.getVersionSemver() != null ? 
                              bpmnDefinition.getVersionSemver() : bpmnDefinition.getVersion());
                dto.setType(ProcessType.getDefault());
                
                // Déterminer automatiquement le statut (VALIDE ou INVALIDE)
                dto.setDeploymentStatus(determineDeploymentStatus(processId, bpmnDefinition.getBpmnXml()));
                
                LOG.info("✅ Processus non déployé trouvé en base: " + processId + 
                         " - Statut: " + dto.getDeploymentStatus());
                return dto;
            }
        } catch (Exception e) {
            LOG.warn("❌ Erreur lors de la recherche du processus en base : " + e.getMessage());
        }

        LOG.info("⚠️ Processus non trouvé avec l'ID : " + processId);
        return null;
    }

    @Override
    public ProcessDTO deployProcess(String processId) {
        LOG.info("Déploiement du processus : " + processId);

        ProcessDTO process = getProcessById(processId);
        if (process == null) {
            throw new IllegalArgumentException("Processus '" + processId + "' non trouvé");
        }

        if (process.getDeploymentStatus() == DeploymentStatus.DEPLOYE) {
            throw new IllegalStateException("Le processus '" + processId + "' est déjà déployé");
        }

        // Vérifier que le processus est VALIDE avant de permettre le déploiement
        if (process.getDeploymentStatus() == DeploymentStatus.INVALIDE) {
            throw new IllegalStateException("Le processus '" + processId + "' contient des erreurs et ne peut pas être déployé");
        }

        if (process.getDeploymentStatus() != DeploymentStatus.VALIDE) {
            throw new IllegalStateException("Seuls les processus avec le statut VALIDE peuvent être déployés");
        }

        // Dans le contexte Kogito, le déploiement réel se fait au moment du démarrage du serveur
        // avec les fichiers BPMN présents dans les ressources. Cette méthode marque simplement
        // le processus comme déployé, mais le déploiement effectif nécessite généralement
        // un redémarrage du serveur Quarkus.

        // Créer un nouveau ProcessDTO avec le statut mis à jour
        ProcessDTO updatedProcess = new ProcessDTO();
        updatedProcess.setId(process.getId());
        updatedProcess.setName(process.getName());
        updatedProcess.setVersion(process.getVersion());
        updatedProcess.setType(process.getType());
        updatedProcess.setRoles(process.getRoles());
        updatedProcess.setMetadata(process.getMetadata());
        updatedProcess.setDeploymentStatus(DeploymentStatus.DEPLOYE);

        LOG.info("Processus marqué comme déployé : " + processId + ". Pour un déploiement complet, un redémarrage du serveur peut être nécessaire.");

        return updatedProcess;
    }

    @Override
    public ProcessDTO undeployProcess(String processId) {
        LOG.info("Retrait du déploiement du processus : " + processId);

        ProcessDTO process = getProcessById(processId);
        if (process == null) {
            throw new IllegalArgumentException("Processus '" + processId + "' non trouvé");
        }

        if (process.getDeploymentStatus() == DeploymentStatus.NON_DEPLOYE) {
            throw new IllegalStateException("Le processus '" + processId + "' n'est pas déployé");
        }

        // Sauvegarder le BPMN en base avant suppression
        try {
            BpmnDefinition bpmnDef = bpmnDefinitionService.getByProcessId(processId);
            if (bpmnDef != null) {
                BpmnDefinitionHistory history = BpmnDefinitionHistory.builder()
                    .bpmnDefinitionId(bpmnDef.getId())
                    .processId(bpmnDef.getProcessId())
                    .versionSemver(bpmnDef.getVersionSemver())
                    .changeType(VersionChangeType.MAJOR) // à adapter selon votre logique
                    .name(bpmnDef.getName())
                    .bpmnXml(bpmnDef.getBpmnXml())
                    .filePath(null)
                    .status("ARCHIVED")
                    // .createdAt supprimé, géré dans le constructeur
                    .createdBy("system")
                    .changeComment("Retrait du déploiement, sauvegarde avant suppression physique")
                    .build();
                bpmnDefinitionHistoryRepository.persist(history);
                LOG.info("BPMN sauvegardé en base avant suppression : " + processId);
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de la sauvegarde du BPMN avant suppression : " + e.getMessage());
        }

        // Suppression physique du fichier BPMN
        try {
            BpmnDefinition bpmnDef = bpmnDefinitionService.getByProcessId(processId);
            if (bpmnDef != null) {
                String fileName = bpmnDef.getProcessId() + ".bpmn";
                Path filePath = Paths.get(BPMN_PATH, fileName);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    LOG.info("Fichier BPMN supprimé physiquement : " + filePath);
                } else {
                    LOG.warn("Fichier BPMN introuvable pour suppression : " + filePath);
                }
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de la suppression physique du fichier BPMN : " + e.getMessage());
        }

        // Déterminer le nouveau statut basé sur la validation BPMN
        try {
            BpmnDefinition bpmnDef = bpmnDefinitionService.getByProcessId(processId);
            if (bpmnDef != null && bpmnDef.getBpmnXml() != null) {
                DeploymentStatus newStatus = determineDeploymentStatus(processId, bpmnDef.getBpmnXml());
                process.setDeploymentStatus(newStatus);
                LOG.info("Processus retiré du déploiement avec statut : " + newStatus);
            } else {
                process.setDeploymentStatus(DeploymentStatus.NON_DEPLOYE);
                LOG.info("Processus marqué comme non déployé (pas de BPMN en base)");
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de la détermination du nouveau statut : " + e.getMessage());
            process.setDeploymentStatus(DeploymentStatus.NON_DEPLOYE);
        }

        // Ajout d'un message pour le frontend
        process.setMetadata(Map.of("notification", "Le processus a été retiré du déploiement et archivé. Un redémarrage du serveur est nécessaire pour finaliser le retrait."));

        return process;
    }

    /**
     * Détermine le type de changement de version basé sur le numéro de version sémantique
     * @param version La version sémantique (format X.Y.Z)
     * @return Le type de changement (MAJOR, MINOR, PATCH)
     */
    private VersionChangeType determineVersionChangeType(String version) {
        if (version == null || version.trim().isEmpty()) {
            return VersionChangeType.MAJOR;
        }

        try {
            String[] parts = version.trim().split("\\.");
            if (parts.length != 3) {
                return VersionChangeType.MAJOR;
            }

            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);

            // Logique de détermination du type de changement basée sur la version actuelle
            // Pour une version donnée, on ne peut pas déterminer si c'est MAJOR/MINOR/PATCH
            // sans connaître la version précédente. Ici on utilise une heuristique simple.
            if (patch > 0) {
                return VersionChangeType.PATCH;
            } else if (minor > 0) {
                return VersionChangeType.MINOR;
            } else {
                return VersionChangeType.MAJOR;
            }
        } catch (Exception e) {
            LOG.debug("Erreur lors de la détermination du type de changement pour: " + version, e);
            return VersionChangeType.MAJOR;
        }
    }

    /**
     * Détermine automatiquement le statut de déploiement d'un processus basé sur :
     * - La validation BPMN
     * - L'existence du fichier dans les ressources (BPMN_PATH)
     * 
     * @param processId L'ID du processus
     * @param bpmnXml Le contenu BPMN (optionnel)
     * @return Le statut de déploiement approprié
     */
    private DeploymentStatus determineDeploymentStatus(String processId, String bpmnXml) {
        try {
            // Vérifier si le fichier existe dans les ressources (déployé)
            String fileName = processId + ".bpmn";
            Path filePath = Paths.get(BPMN_PATH, fileName);
            
            if (Files.exists(filePath)) {
                LOG.debug("Fichier trouvé dans les ressources: " + filePath);
                return DeploymentStatus.DEPLOYE;
            }

            // Si on a le contenu BPMN, vérifier la validation
            if (bpmnXml != null && !bpmnXml.trim().isEmpty()) {
                try {
                    LOG.debug("Validation du BPMN pour le processus: " + processId);
                    BpmnValidationResult validationResult = bpmnValidationService.validateBpmn(bpmnXml);
                    
                    if (validationResult.isValid()) {
                        LOG.debug("BPMN valide pour le processus: " + processId);
                        // Valide et pas encore déployé
                        return DeploymentStatus.VALIDE;
                    } else {
                        LOG.warn("BPMN invalide pour le processus: " + processId);
                        if (validationResult.getErrors() != null) {
                            for (String error : validationResult.getErrors()) {
                                LOG.warn("  - " + error);
                            }
                        }
                        // Contient des erreurs
                        return DeploymentStatus.INVALIDE;
                    }
                } catch (Exception e) {
                    LOG.error("Erreur lors de la validation BPMN pour " + processId + ": " + e.getMessage(), e);
                    return DeploymentStatus.INVALIDE;
                }
            }

            LOG.debug("Aucune information de validation BPMN pour le processus: " + processId);
            // Par défaut, retourner NON_DEPLOYE si pas d'information sur la validation
            return DeploymentStatus.NON_DEPLOYE;
            
        } catch (Exception e) {
            LOG.error("Erreur lors de la détermination du statut de déploiement pour " + processId, e);
            return DeploymentStatus.NON_DEPLOYE;
        }
    }

    @Override
    public ProcessDTO validateAndRepairProcess(String processId) {
        LOG.info("Validation et réparation du processus : " + processId);

        ProcessDTO process = getProcessById(processId);
        if (process == null) {
            throw new IllegalArgumentException("Processus '" + processId + "' non trouvé");
        }

        // Vérifier que le processus est bien en statut INVALIDE
        if (process.getDeploymentStatus() != DeploymentStatus.INVALIDE) {
            throw new IllegalStateException("Le processus '" + processId + "' n'est pas en statut INVALIDE. Statut actuel: " + process.getDeploymentStatus());
        }

        // Récupérer le XML du processus BPMN
        String bpmnXml = getProcessDefinitionXml(processId);
        if (bpmnXml == null || bpmnXml.trim().isEmpty()) {
            throw new IllegalArgumentException("Définition BPMN non trouvée pour le processus '" + processId + "'");
        }

        // Valider le BPMN
        BpmnValidationResult validationResult = bpmnValidationService.validateBpmn(bpmnXml);

        if (!validationResult.isValid()) {
            // Si la validation échoue, lancer une exception avec les détails
            String errorMessage = "Le processus contient " + validationResult.getErrors().size() + " erreur(s) de validation BPMN";
            LOG.warn("Validation échouée pour le processus : " + processId + " - " + errorMessage);
            throw new IllegalStateException(errorMessage + ": " + String.join(", ", validationResult.getErrors()));
        }

        // Si la validation réussit, mettre à jour le statut du processus à VALIDE
        ProcessDTO updatedProcess = new ProcessDTO();
        updatedProcess.setId(process.getId());
        updatedProcess.setName(process.getName());
        updatedProcess.setVersion(process.getVersion());
        updatedProcess.setType(process.getType());
        updatedProcess.setRoles(process.getRoles());
        updatedProcess.setMetadata(Map.of("message", "Le processus a passé la validation BPMN avec succès"));
        updatedProcess.setDeploymentStatus(DeploymentStatus.VALIDE);

        LOG.info("Processus validé et réparé avec succès : " + processId + ". Statut changé à VALIDE");

        return updatedProcess;
    }

    /**
     * Vérifie si le XML fourni est valide
     * @param xml Le contenu XML à vérifier
     * @return true si valide, false sinon
     */
    private boolean isValidXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return false;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(xml)));
            return true;
        } catch (Exception e) {
            LOG.debug("XML invalide détecté: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tente de normaliser le XML BPMN en utilisant le service de normalisation
     * @param xml Le XML BPMN à normaliser
     * @return Le XML normalisé ou l'original si la normalisation échoue
     */
    private String normalizeBpmnXml(String xml) {
        try {
            // Utiliser le service de normalisation BPMN existant
            if (bpmnNormalizerService.needsNormalization(xml)) {
                LOG.info("Application de la normalisation BPMN automatique");
                return bpmnNormalizerService.normalizeBpmn(xml);
            } else {
                LOG.debug("Aucune normalisation nécessaire");
                return xml;
            }
        } catch (Exception e) {
            LOG.warn("Échec de la normalisation BPMN, utilisation du XML original: " + e.getMessage());
            return xml; // Retourner l'original en cas d'échec
        }
    }

}
