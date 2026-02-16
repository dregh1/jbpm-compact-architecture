package mg.orange.workflow.resource.process;

import mg.orange.workflow.model.process.BpmnUploadForm;
import mg.orange.workflow.model.process.CreateVersionRequestDTO;
import mg.orange.workflow.model.process.ProcessDTO;
import mg.orange.workflow.model.process.ProcessStatistics;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/processes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Process Management", description = "Gestion des définitions de processus Kogito")
public interface ProcessResource {

    @GET
    @Operation(summary = "Liste des processus", 
               description = "Récupère la liste de tous les processus disponibles avec possibilité de filtrage")
    @APIResponses({
        @APIResponse(responseCode = "200",
                     description = "Liste des processus récupérée avec succès",
                     content = @Content(schema = @Schema(implementation = ProcessListResponse.class))),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response listProcesses(
            @Parameter(description = "Filtre par nom de processus (recherche partielle)")
            @QueryParam("name") String name,

            @Parameter(description = "Numéro de page (défaut: 0)")
            @QueryParam("page") @DefaultValue("0") int page,

            @Parameter(description = "Taille de la page (défaut: 20)")
            @QueryParam("size") @DefaultValue("20") int size,

            @Parameter(description = "Champ de tri (id, name, version, type, status)")
            @QueryParam("sort") String sortField,

            @Parameter(description = "Direction du tri (asc ou desc, défault: asc)")
            @QueryParam("direction") @DefaultValue("asc") String sortDirection
    );


    @GET
    @Path("/count")
    @Operation(summary = "Nombre de processus",
               description = "Retourne le nombre total de processus disponibles")
    @APIResponse(responseCode = "200", description = "Nombre de processus")
    Response countProcesses();

    @GET
    @Path("/statistics")
    @Operation(summary = "Statistiques globales des processus",
               description = "Retourne les statistiques globales des processus (déployés, valides, invalides, etc.)")
    @APIResponses({
        @APIResponse(responseCode = "200",
                     description = "Statistiques récupérées avec succès",
                     content = @Content(schema = @Schema(implementation = ProcessStatistics.class))),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getProcessStatistics();

    @GET
    @Path("/statistics/trends")
    @Operation(summary = "Tendances des processus",
               description = "Retourne les statistiques de tendance sur les 7 derniers jours")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Tendances récupérées avec succès"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getProcessTrends();

    @GET
    @Path("/statistics/performance")
    @Operation(summary = "Métriques de performance",
               description = "Retourne les KPIs et métriques de performance du système")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Métriques de performance récupérées"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getPerformanceMetrics();

    @GET
    @Path("/statistics/health")
    @Operation(summary = "Score de santé du système",
               description = "Retourne le score de santé global et les alertes actives")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Score de santé récupéré"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getSystemHealth();

    @GET
    @Path("/{processId}")
    @Operation(summary = "Détail complet d'un processus",
               description = "Récupère les détails complets d'un processus (métadonnées, XML, tâches, nœuds, diagramme)")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Détails du processus récupérés avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getProcessDetail(@Parameter(description = "ID du processus") @PathParam("processId") String processId);

    @GET
    @Path("/{processId}/definition")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(summary = "Définition XML du processus",
               description = "Récupère le contenu XML brut du fichier BPMN")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "XML BPMN récupéré avec succès"),
        @APIResponse(responseCode = "404", description = "Processus ou fichier BPMN non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getProcessDefinition(@Parameter(description = "ID du processus") @PathParam("processId") String processId);

    @GET
    @Path("/{processId}/diagram")
    @Operation(summary = "Informations du diagramme",
               description = "Récupère les informations de layout du diagramme BPMN (coordonnées, shapes, edges)")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Informations du diagramme récupérées avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getProcessDiagram(@Parameter(description = "ID du processus") @PathParam("processId") String processId);

    @GET
    @Path("/{processId}/tasks")
    @Operation(summary = "Liste des tâches du processus",
               description = "Récupère la liste de toutes les tâches du processus")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Liste des tâches récupérée avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getProcessTasks(@Parameter(description = "ID du processus") @PathParam("processId") String processId);

    @GET
    @Path("/{processId}/nodes")
    @Operation(summary = "Liste des nœuds du processus",
               description = "Récupère la liste de tous les nœuds (activités, events, gateways) du processus")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Liste des nœuds récupérée avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getProcessNodes(@Parameter(description = "ID du processus") @PathParam("processId") String processId);

    @GET
    @Path("/{processId}/tasks/{taskId}/assigned-groups")
    @Operation(summary = "Groupes assignés à une tâche",
               description = "Récupère la liste des groupes assignés à une tâche spécifique d'un processus")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Liste des groupes récupérée avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getTaskAssignedGroups(@Parameter(description = "ID du processus") @PathParam("processId") String processId,
                                   @Parameter(description = "ID de la tâche") @PathParam("taskId") String taskId);

    // Classe interne pour la doc OpenAPI
    @Schema(description = "Réponse paginée contenant une liste de processus")
    class ProcessListResponse {
        @Schema(description = "Liste des processus de la page courante")
        public List<ProcessDTO> content;

        @Schema(description = "Numéro de la page courante")
        public int page;

        @Schema(description = "Taille de la page")
        public int size;

        @Schema(description = "Nombre total d'éléments")
        public int totalElements;

        @Schema(description = "Nombre total de pages")
        public int totalPages;

        @Schema(description = "Indique si c'est la première page")
        public boolean first;

        @Schema(description = "Indique si c'est la dernière page")
        public boolean last;
    }

    @POST
    @Path("/validate-file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Response validateBpmnFile(@MultipartForm BpmnUploadForm uploadForm);

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Uploader un fichier BPMN")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Fichier BPMN uploadé avec succès"),
        @APIResponse(responseCode = "400", description = "Fichier invalide"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response uploadBpmn(@MultipartForm BpmnUploadForm form);

    
    
    @DELETE
    @Path("/{filename}")
    @Operation(summary = "Supprimer un BPMN")
    Response deleteBpmn(@PathParam("filename") String filename);

    @GET
    @Path("/files")
    @Operation(summary = "Liste des fichiers BPMN")
    Response listBpmnFiles();

    // ======= ENDPOINTS VERSIONING SEMANTIQUE BPMN =======


    @POST
    @Path("/{processId}/versions")
    @Operation(summary = "Créer une nouvelle version BPMN sémantique",
               description = "Crée une nouvelle version BPMN avec analyse automatique des changements et génération du numéro de version sémantique")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Nouvelle version créée avec succès"),
        @APIResponse(responseCode = "400", description = "Requête invalide ou erreurs dans le BPMN"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response createNewVersion(
            @Parameter(description = "ID du processus à versionner", required = true)
            @PathParam("processId") String processId,

            CreateVersionRequestDTO request);

    @GET
    @Path("/{processId}/versions")
    @Operation(summary = "Historique complet des versions d'un processus",
               description = "Récupère l'historique complet des versions d'un processus BPMN classé par date décroissante")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Historique des versions récupéré avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getVersionHistory(
            @Parameter(description = "ID du processus", required = true)
            @PathParam("processId") String processId,

            @Parameter(description = "Numéro de page (défaut: 0)")
            @QueryParam("page") @DefaultValue("0") int page,

            @Parameter(description = "Taille de la page (défaut: 20)")
            @QueryParam("size") @DefaultValue("20") int size);

    @GET
    @Path("/{processId}/versions/{versionSemver}")
    @Operation(summary = "Détails d'une version spécifique",
               description = "Récupère les détails complets d'une version spécifique d'un processus BPMN")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Détails de la version récupérés avec succès"),
        @APIResponse(responseCode = "404", description = "Processus ou version non trouvée"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response getVersionDetails(
            @Parameter(description = "ID du processus", required = true)
            @PathParam("processId") String processId,

            @Parameter(description = "Version sémantique (format X.Y.Z)", required = true)
            @PathParam("versionSemver") String versionSemver);

    @POST
    @Path("/{processId}/versions/{versionSemver}/activate")
    @Operation(summary = "Activer une version comme version courante",
               description = "Active une version spécifique comme version courante du processus BPMN")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Version activée avec succès"),
        @APIResponse(responseCode = "404", description = "Processus ou version non trouvée"),
        @APIResponse(responseCode = "409", description = "Conflit (version déjà active ou inéligible)"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response activateVersion(
            @Parameter(description = "ID du processus", required = true)
            @PathParam("processId") String processId,

            @Parameter(description = "Version sémantique à activer (format X.Y.Z)", required = true)
            @PathParam("versionSemver") String versionSemver);

    @POST
    @Path("/{processId}/versions/{versionSemver}/deactivate")
    @Operation(summary = "Désactiver une version",
               description = "Désactive une version spécifique du processus BPMN (passe le statut à INACTIVE)")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Version désactivée avec succès"),
        @APIResponse(responseCode = "404", description = "Processus ou version non trouvée"),
        @APIResponse(responseCode = "409", description = "Conflit (impossible de désactiver la dernière version active)"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response deactivateVersion(
            @Parameter(description = "ID du processus", required = true)
            @PathParam("processId") String processId,

            @Parameter(description = "Version sémantique à désactiver (format X.Y.Z)", required = true)
            @PathParam("versionSemver") String versionSemver);

    @GET
    @Path("/{processId}/versions/compare")
    @Operation(summary = "Comparer deux versions",
               description = "Génère un résumé des changements entre deux versions d'un processus BPMN")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Comparaison générée avec succès"),
        @APIResponse(responseCode = "400", description = "Versions invalides"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response compareVersions(
            @Parameter(description = "ID du processus", required = true)
            @PathParam("processId") String processId,

            @Parameter(description = "Version précédente (optionnel)")
            @QueryParam("fromVersion") String fromVersion,

            @Parameter(description = "Version cible", required = true)
            @QueryParam("toVersion") String toVersion);


    // ======= ENDPOINTS DEPLOIEMENT/GESTION DU STATUT =======

    @POST
    @Path("/{processId}/deploy")
    @Operation(summary = "Déployer un processus BPMN",
               description = "Change le statut de déploiement d'un processus BPMN à DEPLOYE et déclenche le déploiement")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Processus déployé avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "409", description = "Processus déjà déployé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response deployProcess(@Parameter(description = "ID du processus à déployer", required = true) @PathParam("processId") String processId);

    @POST
    @Path("/{processId}/undeploy")
    @Operation(summary = "Retirer le déploiement d'un processus BPMN",
               description = "Change le statut de déploiement d'un processus BPMN à NON_DEPLOYE et retire le déploiement")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Déploiement retiré avec succès"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "409", description = "Processus déjà non déployé"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response undeployProcess(@Parameter(description = "ID du processus à retirer du déploiement", required = true) @PathParam("processId") String processId);

    @POST
    @Path("/{processId}/validate")
    @Operation(summary = "Valider et réparer un processus BPMN invalide",
               description = "Valide le fichier BPMN d'un processus invalide et change son statut à VALIDE s'il passe la validation, sans le déployer")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Processus validé avec succès"),
        @APIResponse(responseCode = "400", description = "Processus contient des erreurs de validation"),
        @APIResponse(responseCode = "404", description = "Processus non trouvé"),
        @APIResponse(responseCode = "409", description = "Processus n'est pas en statut INVALIDE"),
        @APIResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    Response validateAndRepairProcess(@Parameter(description = "ID du processus à valider", required = true) @PathParam("processId") String processId);


}
