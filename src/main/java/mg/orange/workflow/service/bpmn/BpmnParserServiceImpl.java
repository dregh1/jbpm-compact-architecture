package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.process.DiagramInfoDTO;
import mg.orange.workflow.model.process.ProcessNodeDTO;
import mg.orange.workflow.model.process.ProcessTaskDTO;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class BpmnParserServiceImpl implements BpmnParserService {

    private static final Logger LOG = Logger.getLogger(BpmnParserServiceImpl.class);

    @jakarta.inject.Inject
    BpmnNormalizerService bpmnNormalizerService;

    @Override
    public List<ProcessTaskDTO> parseProcessTasks(String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = parseBpmnXml(bpmnXml);
            Collection<Task> tasks = modelInstance.getModelElementsByType(Task.class);

            return tasks.stream().map(task -> {
                ProcessTaskDTO dto = new ProcessTaskDTO();
                dto.setId(task.getId());
                dto.setName(task.getName());
                dto.setType(getTaskType(task));

                // Extraire la documentation si présente
                Collection<Documentation> docs = task.getDocumentations();
                if (!docs.isEmpty()) {
                    dto.setDocumentation(docs.iterator().next().getTextContent());
                }

                // Extraire les groupes assignés (pour UserTask)
                if (task instanceof UserTask) {
                    UserTask userTask = (UserTask) task;
                    String candidateGroups = userTask.getCamundaCandidateGroups();
                    if (candidateGroups != null && !candidateGroups.isEmpty()) {
                        dto.setAssignedGroups(Arrays.asList(candidateGroups.split(",")));
                    }

                    String candidateUsers = userTask.getCamundaCandidateUsers();
                    if (candidateUsers != null && !candidateUsers.isEmpty()) {
                        dto.setAssignedUsers(Arrays.asList(candidateUsers.split(",")));
                    }
                }

                return dto;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            LOG.error("Erreur lors du parsing des tâches BPMN", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProcessNodeDTO> parseProcessNodes(String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = parseBpmnXml(bpmnXml);
            List<ProcessNodeDTO> nodes = new ArrayList<>();

            // Récupérer tous les FlowNode (activités, events, gateways)
            Collection<FlowNode> flowNodes = modelInstance.getModelElementsByType(FlowNode.class);

            for (FlowNode flowNode : flowNodes) {
                ProcessNodeDTO dto = new ProcessNodeDTO();
                dto.setId(flowNode.getId());
                dto.setName(flowNode.getName());
                dto.setType(getNodeType(flowNode));
                dto.setSubType(flowNode.getElementType().getTypeName());
                nodes.add(dto);
            }

            return nodes;

        } catch (Exception e) {
            LOG.error("Erreur lors du parsing des nœuds BPMN", e);
            return Collections.emptyList();
        }
    }

    @Override
    public DiagramInfoDTO parseDiagramInfo(String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = parseBpmnXml(bpmnXml);
            DiagramInfoDTO diagramInfo = new DiagramInfoDTO();

            Map<String, DiagramInfoDTO.ShapeInfo> shapes = new HashMap<>();
            Map<String, DiagramInfoDTO.EdgeInfo> edges = new HashMap<>();

            // Parser les shapes (positions des éléments)
            Collection<BpmnShape> bpmnShapes = modelInstance.getModelElementsByType(BpmnShape.class);
            for (BpmnShape shape : bpmnShapes) {
                Bounds bounds = shape.getBounds();
                if (bounds != null) {
                    String elementId = shape.getBpmnElement().getId();
                    DiagramInfoDTO.ShapeInfo shapeInfo = new DiagramInfoDTO.ShapeInfo(
                        elementId,
                        bounds.getX(),
                        bounds.getY(),
                        bounds.getWidth(),
                        bounds.getHeight()
                    );
                    shapes.put(elementId, shapeInfo);
                }
            }

            // Parser les edges (connexions)
            Collection<BpmnEdge> bpmnEdges = modelInstance.getModelElementsByType(BpmnEdge.class);
            for (BpmnEdge edge : bpmnEdges) {
                String elementId = edge.getBpmnElement().getId();
                DiagramInfoDTO.EdgeInfo edgeInfo = new DiagramInfoDTO.EdgeInfo();
                edgeInfo.setElementId(elementId);

                // Récupérer source et target
                if (edge.getBpmnElement() instanceof SequenceFlow) {
                    SequenceFlow flow = (SequenceFlow) edge.getBpmnElement();
                    edgeInfo.setSourceId(flow.getSource().getId());
                    edgeInfo.setTargetId(flow.getTarget().getId());
                }

                // Récupérer les waypoints
                List<DiagramInfoDTO.EdgeInfo.Waypoint> waypoints = new ArrayList<>();
                for (Waypoint wp : edge.getWaypoints()) {
                    waypoints.add(new DiagramInfoDTO.EdgeInfo.Waypoint(wp.getX(), wp.getY()));
                }
                edgeInfo.setWaypoints(waypoints);

                edges.put(elementId, edgeInfo);
            }

            diagramInfo.setShapes(shapes);
            diagramInfo.setEdges(edges);

            return diagramInfo;

        } catch (Exception e) {
            LOG.error("Erreur lors du parsing des informations du diagramme", e);
            return new DiagramInfoDTO();
        }
    }

    @Override
    public boolean validateBpmn(String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = parseBpmnXml(bpmnXml);

            // Vérifier qu'il y a au moins un processus
            Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
            if (processes.isEmpty()) {
                LOG.warn("Aucun processus trouvé dans le BPMN");
                return false;
            }

            // Validation basique réussie
            return true;

        } catch (Exception e) {
            LOG.error("Erreur lors de la validation du BPMN: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String extractProcessId(String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = parseBpmnXml(bpmnXml);
            Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);

            if (!processes.isEmpty()) {
                return processes.iterator().next().getId();
            }

        } catch (Exception e) {
            LOG.error("Erreur lors de l'extraction de l'ID du processus", e);
        }
        return null;
    }

    @Override
    public String extractProcessName(String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = parseBpmnXml(bpmnXml);
            Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);

            if (!processes.isEmpty()) {
                Process process = processes.iterator().next();
                return process.getName() != null ? process.getName() : process.getId();
            }

        } catch (Exception e) {
            LOG.error("Erreur lors de l'extraction du nom du processus", e);
        }
        return null;
    }

    /**
     * Parse le XML BPMN en instance de modèle Camunda
     * Applique automatiquement la normalisation BPMN si nécessaire.
     */
    private BpmnModelInstance parseBpmnXml(String bpmnXml) {
        try {
            // Appliquer la normalisation automatique si nécessaire
            if (bpmnNormalizerService.needsNormalization(bpmnXml)) {
                LOG.debug("Normalisation BPMN nécessaire, application des corrections automatiques");
                bpmnXml = bpmnNormalizerService.normalizeBpmn(bpmnXml);
            }

            InputStream stream = new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8));
            return Bpmn.readModelFromStream(stream);
        } catch (org.camunda.bpm.model.xml.ModelParseException e) {
            // Si le parsing échoue encore après normalisation, c'est une erreur grave
            LOG.error("Erreur de validation BPMN même après normalisation: " + e.getMessage());
            throw new RuntimeException("Le fichier BPMN ne respecte pas le schéma BPMN 2.0, " +
                                     "même après tentative de correction automatique. " +
                                     "Veuillez vérifier la structure du fichier BPMN.", e);
        } catch (Exception e) {
            LOG.error("Impossible de parser le BPMN", e);
            throw new RuntimeException("Erreur de parsing BPMN", e);
        }
    }

    /**
     * Détermine le type de tâche
     */
    private String getTaskType(Task task) {
        if (task instanceof UserTask) return "UserTask";
        if (task instanceof ServiceTask) return "ServiceTask";
        if (task instanceof ScriptTask) return "ScriptTask";
        if (task instanceof SendTask) return "SendTask";
        if (task instanceof ReceiveTask) return "ReceiveTask";
        if (task instanceof ManualTask) return "ManualTask";
        if (task instanceof BusinessRuleTask) return "BusinessRuleTask";
        return "Task";
    }

    /**
     * Détermine le type de nœud
     */
    private String getNodeType(FlowNode flowNode) {
        if (flowNode instanceof StartEvent) return "StartEvent";
        if (flowNode instanceof EndEvent) return "EndEvent";
        if (flowNode instanceof Task) return "Task";
        if (flowNode instanceof Gateway) return "Gateway";
        if (flowNode instanceof IntermediateCatchEvent) return "IntermediateCatchEvent";
        if (flowNode instanceof IntermediateThrowEvent) return "IntermediateThrowEvent";
        return "FlowNode";
    }
}
