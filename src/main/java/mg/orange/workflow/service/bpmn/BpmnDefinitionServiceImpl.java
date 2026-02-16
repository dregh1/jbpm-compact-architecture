package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.bpmn.*;
import mg.orange.workflow.repository.BpmnDefinitionHistoryRepository;
import mg.orange.workflow.repository.BpmnDefinitionRepository;
import mg.orange.workflow.util.SemverUtils;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des définitions BPMN
 * Support du versioning sémantique selon Semantic Versioning 2.0.0
 */
@ApplicationScoped
@Transactional
public class BpmnDefinitionServiceImpl implements BpmnDefinitionService {

    private static final Logger LOG = Logger.getLogger(BpmnDefinitionServiceImpl.class);

    @Inject
    BpmnDefinitionRepository definitionRepository;

    @Inject
    BpmnDefinitionHistoryRepository historyRepository;

    @Inject
    VersionManagementService versionManagementService;

    @Inject
    BpmnValidationService bpmnValidationService;

    @Override
    public BpmnDefinition saveBpmnDefinition(String bpmnXml, String createdBy) {
        // Méthode existante - marqueur pour compatibilité descendante
        LOG.warn("saveBpmnDefinition appelée - utiliser saveVersion pour nouvelle implémentation");
        return saveVersion(bpmnXml, createdBy, "Version initiale créée automatiquement");
    }

    @Override
    public String deployBpmnToFileSystem(String processId) {
        // Méthode existante - implémentation de base (à raffiner selon besoins)
        LOG.infof("Déploiement demandé pour processId: %s", processId);
        // Pour l'instant, simple log - implémentation complète dans phase ultérieure
        return "/tmp/deployed/" + processId + ".bpmn";
    }

    @Override
    public String getBpmnXml(String processId) {
        BpmnDefinition definition = getByProcessId(processId);
        return definition != null ? definition.getBpmnXml() : null;
    }

    @Override
    public BpmnDefinition getByProcessId(String processId) {
        return definitionRepository.findByProcessId(processId).orElse(null);
    }

    @Override
    public List<BpmnDefinitionDTO> getAllDefinitions() {
        return definitionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BpmnDefinition updateBpmnDefinition(String processId, String bpmnXml) {
        LOG.warn("updateBpmnDefinition appelée - utiliser createVersion pour nouvelle implémentation");
        return createVersion(processId, bpmnXml, "system", "Mise à jour automatique");
    }

    @Override
    public boolean deleteBpmnDefinition(String processId) {
        // Vérifier si le processus existe
        Optional<BpmnDefinition> definitionOpt = definitionRepository.findByProcessId(processId);
        if (!definitionOpt.isPresent()) {
            return false;
        }

        // Supprimer l'historique d'abord (contrainte de clé étrangère)
        historyRepository.deleteByBpmnDefinitionId(definitionOpt.get().getId());

        // Ensuite supprimer la définition
        return definitionRepository.deleteByProcessId(processId);
    }

    @Override
    public BpmnDefinition saveUploadedBpmn(InputStream inputStream, String fileName, String createdBy) {
        try {
            String bpmnXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String processId = extractProcessIdFromXml(bpmnXml);
            return createVersion(processId, bpmnXml, createdBy, "Upload du fichier " + fileName);
        } catch (IOException e) {
            LOG.error("Erreur lors de la lecture du fichier uploadé", e);
            throw new RuntimeException("Impossible de lire le fichier BPMN", e);
        }
    }

    // ======= NOUVELLES MÉTHODES VERSIONING SEMANTIQUE =======

    @Override
    public BpmnDefinition saveBpmnDraft(String bpmnXml, String createdBy, String changeComment) {
        LOG.info("Sauvegarde d'un brouillon BPMN sans validation");

        // Extraire l'ID du processus
        String processId = extractProcessIdFromXml(bpmnXml);
        if (processId == null) {
            throw new IllegalArgumentException("Impossible d'extraire l'ID du processus du BPMN");
        }

        return createVersion(processId, bpmnXml, createdBy, changeComment);
    }

    @Override
    public BpmnValidationResult validateBpmn(String bpmnXml) {
        LOG.debug("Validation BPMN sans sauvegarde");
        return bpmnValidationService.validateBpmn(bpmnXml);
    }

    @Override
    public BpmnDefinition saveVersionWithValidation(String bpmnXml, String createdBy, String changeComment) {
        // Valider le BPMN d'abord
        var validationResult = bpmnValidationService.validateBpmn(bpmnXml);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("BPMN invalide: " + String.join(", ", validationResult.getErrors()));
        }

        // Extraire l'ID du processus
        String processId = extractProcessIdFromXml(bpmnXml);
        if (processId == null) {
            throw new IllegalArgumentException("Impossible d'extraire l'ID du processus du BPMN");
        }

        return createVersion(processId, bpmnXml, createdBy, changeComment);
    }

    @Override
    public BpmnDefinition saveVersion(String bpmnXml, String createdBy, String changeComment) {
        // Alias pour compatibilité descendante
        return saveVersionWithValidation(bpmnXml, createdBy, changeComment);
    }

    @Override
    public BpmnDefinition createVersion(String processId, String bpmnXml, String createdBy, String changeComment) {
        if (changeComment == null || changeComment.trim().isEmpty()) {
            changeComment = "Nouvelle version créée";
        }

        LOG.infof("Création d'une nouvelle version pour processId: %s", processId);

        // Récupérer la définition courante
        BpmnDefinition currentDefinition = getByProcessId(processId);
        String oldXml = (currentDefinition != null) ? currentDefinition.getBpmnXml() : null;

        // Calculer le type de changement et la nouvelle version
        VersionChangeType changeType;
        if (oldXml == null) {
            // Première version de ce processus
            changeType = VersionChangeType.MAJOR;
            LOG.infof("Première version du processus %s - Changement: MAJOR", processId);
        } else {
            // Version existante - analyser les changements
            changeType = versionManagementService.calculateChangeType(oldXml, bpmnXml);
            LOG.infof("Version existante - Changement calculé: %s", changeType);
        }

        String newVersion = calculateNewVersion(currentDefinition != null ? currentDefinition.getVersionSemver() : null, changeType);
        SemverUtils.validateSemver(newVersion);

        LOG.infof("Nouvelle version calculée: %s", newVersion);

        if (currentDefinition != null) {
            // Mettre à jour la définition existante
            currentDefinition.setBpmnXml(bpmnXml);
            currentDefinition.setVersionSemver(newVersion);
            currentDefinition.setUpdatedAt(LocalDateTime.now());

            // Sauvegarder
            definitionRepository.persist(currentDefinition);

            // Créer l'entrée d'historique
            BpmnDefinitionHistory history = BpmnDefinitionHistory.createFromDefinition(
                currentDefinition,
                newVersion,
                changeType,
                changeComment
            );
            historyRepository.persist(history);

            LOG.infof("Version mise à jour: %s (type: %s)", newVersion, changeType);
            return currentDefinition;

        } else {
            // Créer une nouvelle définition
            BpmnDefinition newDefinition = new BpmnDefinition();
            newDefinition.setProcessId(processId);
            newDefinition.setName(extractProcessNameFromXml(bpmnXml));
            newDefinition.setVersion("legacy"); // Conserver numéro version legacy
            newDefinition.setVersionSemver(newVersion);
            newDefinition.setBpmnXml(bpmnXml);
            newDefinition.setCreatedBy(createdBy);
            newDefinition.setStatus(BpmnDefinition.Status.ACTIVE);

            definitionRepository.persist(newDefinition);

            // Créer l'entrée d'historique initiale
            BpmnDefinitionHistory history = BpmnDefinitionHistory.createFromDefinition(
                newDefinition,
                newVersion,
                VersionChangeType.MAJOR,
                "Création initiale du processus"
            );
            historyRepository.persist(history);

            LOG.infof("Nouvelle définition créée avec version: %s", newVersion);
            return newDefinition;
        }
    }

    @Override
    public List<BpmnDefinitionHistoryDTO> getVersionHistory(String processId) {
        BpmnDefinition definition = getByProcessId(processId);
        if (definition == null) {
            LOG.warnf("Processus %s non trouvé pour récupération historique", processId);
            return List.of();
        }

        return historyRepository.findByBpmnDefinitionId(definition.getId())
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Décroissant par date
                .map(BpmnDefinitionHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public BpmnDefinition activateVersion(String processId, String versionSemver) {
        BpmnDefinition definition = getByProcessId(processId);
        if (definition == null) {
            throw new IllegalArgumentException("Processus " + processId + " non trouvé");
        }

        if (versionSemver.equals(definition.getVersionSemver())) {
            throw new IllegalArgumentException("La version " + versionSemver + " est déjà active pour le processus " + processId);
        }

        BpmnDefinitionHistory targetVersion = historyRepository.findByProcessId(processId)
                .stream()
                .filter(h -> versionSemver.equals(h.getVersionSemver()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Version " + versionSemver + " non trouvée pour le processus " + processId));

        LOG.infof("Activation de la version %s pour processus %s", versionSemver, processId);

        // 1. DÉSACTIVER LA VERSION ACTIVE ACTUELLE dans l'historique
        String currentActiveVersion = definition.getVersionSemver();
        if (currentActiveVersion != null) {
            // Trouver l'entrée de la version active courante
            Optional<BpmnDefinitionHistory> currentActiveHistory = historyRepository.findByProcessId(processId)
                .stream()
                .filter(h -> currentActiveVersion.equals(h.getVersionSemver()) && BpmnDefinition.Status.ACTIVE.equals(h.getStatus()))
                .findFirst();

            if (currentActiveHistory.isPresent()) {
                // Créer une nouvelle entrée d'historique pour marquer l'ancienne version comme INACTIVE
                BpmnDefinitionHistory deactivatedEntry = BpmnDefinitionHistory.builder()
                    .bpmnDefinitionId(definition.getId())
                    .processId(definition.getProcessId())
                    .versionSemver(currentActiveVersion)
                    .changeType(currentActiveHistory.get().getChangeType())
                    .name(currentActiveHistory.get().getName())
                    .bpmnXml(currentActiveHistory.get().getBpmnXml())
                    .filePath(currentActiveHistory.get().getFilePath())
                    .status(BpmnDefinition.Status.INACTIVE)  // Désactivation
                    .createdBy("system")
                    .changeComment("Version désactivée automatiquement lors de l'activation d'une nouvelle version")
                    .build();

                historyRepository.persist(deactivatedEntry);
                LOG.infof("Version active précédente %s marquée comme INACTIVE", currentActiveVersion);
            }
        }

        // 2. ACTIVER LA NOUVELLE VERSION dans l'historique
        // Créer une nouvelle entrée d'historique pour marquer la nouvelle version comme ACTIVE
        BpmnDefinitionHistory activatedEntry = BpmnDefinitionHistory.builder()
            .bpmnDefinitionId(definition.getId())
            .processId(definition.getProcessId())
            .versionSemver(versionSemver)
            .changeType(targetVersion.getChangeType())
            .name(targetVersion.getName())
            .bpmnXml(targetVersion.getBpmnXml())
            .filePath(targetVersion.getFilePath())
            .status(BpmnDefinition.Status.ACTIVE)  // Activation
            .createdBy("system")
            .changeComment("Version activée comme version courante")
            .build();

        historyRepository.persist(activatedEntry);

        // 3. Mettre à jour la définition courante avec le contenu de la version activée
        definition.setBpmnXml(targetVersion.getBpmnXml());
        definition.setVersionSemver(versionSemver);
        definition.setFilePath(targetVersion.getFilePath());
        definition.setUpdatedAt(LocalDateTime.now());

        definitionRepository.persist(definition);

        LOG.infof("Version %s activée avec succès pour processus %s", versionSemver, processId);

        return definition;
    }

    @Override
    public BpmnDefinitionHistory deactivateVersion(String processId, String versionSemver) {
        // Récupérer la définition courante pour vérifier si c'est la version actuelle
        BpmnDefinition currentDefinition = getByProcessId(processId);
        if (currentDefinition == null) {
            throw new IllegalArgumentException("Processus " + processId + " non trouvé");
        }

        // Vérifier que la version demandée n'est pas la version courante
        if (versionSemver.equals(currentDefinition.getVersionSemver())) {
            throw new IllegalStateException("Impossible de désactiver la version courante du processus. Activez d'abord une autre version.");
        }

        // Trouver l'entrée d'historique
        BpmnDefinitionHistory historyEntry = historyRepository.findByProcessId(processId)
                .stream()
                .filter(h -> versionSemver.equals(h.getVersionSemver()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version " + versionSemver + " non trouvée pour le processus " + processId));

        // Vérifier qu'elle n'est pas déjà inactive
        if (BpmnDefinition.Status.INACTIVE.equals(historyEntry.getStatus())) {
            throw new IllegalStateException("La version " + versionSemver + " est déjà désactivée");
        }

        LOG.infof("Désactivation de la version historique %s pour processus %s", versionSemver, processId);

        // Créer une nouvelle entrée d'historique avec statut INACTIVE
        // Note : BpmnDefinitionHistory est immutable, donc on crée une nouvelle entrée
        BpmnDefinitionHistory deactivatedEntry = BpmnDefinitionHistory.builder()
                .bpmnDefinitionId(historyEntry.getBpmnDefinitionId())
                .processId(historyEntry.getProcessId())
                .versionSemver(historyEntry.getVersionSemver())
                .changeType(historyEntry.getChangeType())
                .name(historyEntry.getName())
                .bpmnXml(historyEntry.getBpmnXml())
                .filePath(historyEntry.getFilePath())
                .status(BpmnDefinition.Status.INACTIVE)  // Changement de statut
                .createdBy("system")  // Déactivation automatique
                .changeComment("Version désactivée")
                .build();

        historyRepository.persist(deactivatedEntry);

        LOG.infof("Version %s désactivée avec succès pour processus %s", versionSemver, processId);

        return deactivatedEntry;
    }

    @Override
    public BpmnDefinitionHistoryDTO getVersionDetails(String processId, String versionSemver) {
        return historyRepository.findByProcessId(processId)
                .stream()
                .filter(h -> versionSemver.equals(h.getVersionSemver()))
                .findFirst()
                .map(BpmnDefinitionHistoryDTO::fromEntity)
                .orElse(null);
    }

    @Override
    public String generateChangeSummary(String processId, String fromVersion, String toVersion) {
        BpmnDefinition definition = getByProcessId(processId);
        if (definition == null) {
            return "Processus " + processId + " non trouvé";
        }

        String oldXml = null;
        if (fromVersion != null) {
            oldXml = historyRepository.findByProcessId(processId)
                    .stream()
                    .filter(h -> fromVersion.equals(h.getVersionSemver()))
                    .findFirst()
                    .map(BpmnDefinitionHistory::getBpmnXml)
                    .orElse(null);
        }

        String newXml = historyRepository.findByProcessId(processId)
                .stream()
                .filter(h -> toVersion.equals(h.getVersionSemver()))
                .findFirst()
                .map(BpmnDefinitionHistory::getBpmnXml)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Version " + toVersion + " non trouvée pour le processus " + processId));

        if (oldXml == null) {
            return "Création initiale (version " + toVersion + ")";
        }

        BpmnComparisonResult comparison = versionManagementService.compareBpmnVersions(oldXml, newXml);
        return versionManagementService.generateChangeDescription(comparison);
    }

    /**
     * Convertit une entité en DTO avec l'historique
     */
    private BpmnDefinitionDTO convertToDTO(BpmnDefinition entity) {
        BpmnDefinitionDTO dto = new BpmnDefinitionDTO();
        dto.setId(entity.getId());
        dto.setProcessId(entity.getProcessId());
        dto.setName(entity.getName());
        dto.setVersion(entity.getVersion());
        dto.setVersionSemver(entity.getVersionSemver());
        dto.setStatus(entity.getStatus());
        dto.setFilePath(entity.getFilePath());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());

        // Ajouter l'historique récent
        dto.setHistory(getVersionHistory(entity.getProcessId()));

        return dto;
    }

    /**
     * Calcule la nouvelle version selon le type de changement
     */
    private String calculateNewVersion(String currentVersion, VersionChangeType changeType) {
        if (currentVersion == null) {
            // Première version
            return SemverUtils.setVersion(1, 0, 0);
        }

        SemverUtils.validateSemver(currentVersion);

        switch (changeType) {
            case MAJOR:
                return SemverUtils.bumpMajor(currentVersion);
            case MINOR:
                return SemverUtils.bumpMinor(currentVersion);
            case PATCH:
                return SemverUtils.bumpPatch(currentVersion);
            default:
                throw new IllegalArgumentException("Type de changement non supporté: " + changeType);
        }
    }

    /**
     * Extrait l'ID du processus depuis le XML BPMN
     */
    private String extractProcessIdFromXml(String bpmnXml) {
        try {
            var doc = parseXml(bpmnXml);
            var elements = doc.getElementsByTagNameNS("*", "process");
            if (elements.getLength() > 0) {
                var processElement = (org.w3c.dom.Element) elements.item(0);
                return processElement.getAttribute("id");
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de l'extraction de l'ID du processus", e);
        }
        return null;
    }

    /**
     * Extrait le nom du processus depuis le XML BPMN
     */
    private String extractProcessNameFromXml(String bpmnXml) {
        try {
            var doc = parseXml(bpmnXml);
            var elements = doc.getElementsByTagNameNS("*", "process");
            if (elements.getLength() > 0) {
                var processElement = (org.w3c.dom.Element) elements.item(0);
                String name = processElement.getAttribute("name");
                return name != null && !name.isEmpty() ? name : "Processus sans nom";
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de l'extraction du nom du processus", e);
        }
        return "Processus BPMN";
    }

    /**
     * Parse XML (méthode utilitaire simple)
     */
    private org.w3c.dom.Document parseXml(String xml) throws Exception {
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
