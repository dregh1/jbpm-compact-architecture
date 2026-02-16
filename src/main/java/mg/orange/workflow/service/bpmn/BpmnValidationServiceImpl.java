package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.bpmn.BpmnValidationResult;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import jakarta.enterprise.context.ApplicationScoped;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;


@ApplicationScoped
public class BpmnValidationServiceImpl implements BpmnValidationService {

    private static final Logger LOG = Logger.getLogger(BpmnValidationServiceImpl.class);

    private static final List<String> SUPPORTED_ELEMENTS = List.of(
        "startEvent", "endEvent", "userTask", "scriptTask", "serviceTask",
        "businessRuleTask", "sendTask", "receiveTask", "manualTask",
        "exclusiveGateway", "parallelGateway", "inclusiveGateway", "eventBasedGateway",
        "intermediateThrowEvent", "intermediateCatchEvent", "boundaryEvent",
        "sequenceFlow", "subProcess", "callActivity"
    );

    private static final List<String> PROBLEMATIC_ELEMENTS = List.of(
        "adHocSubProcess", "transaction", "complexGateway"
    );

    private static final List<String> SUPPORTED_EVENTS = List.of(
        "none", "message", "timer", "error", "escalation", "conditional", "signal"
    );

    public BpmnValidationResult validateBpmnFile(InputStream fileInputStream, String filename) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> infos = new ArrayList<>();
        
        try {
            // Validation du fichier
            if (fileInputStream == null) {
                errors.add("Le fichier est requis");
                return new BpmnValidationResult(errors, warnings, infos, false);
            }
            
            if (filename == null || !(filename.endsWith(".bpmn") || filename.endsWith(".bpmn2"))) {
                errors.add("Le fichier doit avoir l'extension .bpmn ou .bpmn2");
                return new BpmnValidationResult(errors, warnings, infos, false);
            }
            
            // Lire le contenu du fichier
            String bpmnContent = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
            
            // Valider le contenu
            return validateBpmn(bpmnContent);
            
        } catch (Exception e) {
            LOG.error("Erreur lors de la lecture du fichier BPMN", e);
            errors.add("Erreur lors de la lecture du fichier: " + e.getMessage());
            return new BpmnValidationResult(errors, warnings, infos, false);
        }
    }

    @Override
    public BpmnValidationResult validateBpmn(String bpmnContent) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> infos = new ArrayList<>();

        try {
            LOG.debug("Début de la validation BPMN");
            
            // Afficher les premiers 500 caractères du contenu BPMN pour le débogage
            String debugContent = bpmnContent != null && bpmnContent.length() > 500 
                ? bpmnContent.substring(0, 500) + "..." 
                : bpmnContent;
            LOG.debug("Contenu BPMN à valider: " + debugContent);
            
            Document doc = parseXml(bpmnContent);
            if (doc == null) {
                String errorMsg = "Le contenu n'est pas un XML valide";
                LOG.error(errorMsg);
                errors.add(errorMsg);
                return new BpmnValidationResult(errors, warnings, infos, false);
            }

            LOG.debug("Validation de la structure BPMN");
            validateBpmnStructure(doc, errors, warnings, infos);
            
            if (!errors.isEmpty()) {
                LOG.warn("Erreurs de structure BPMN détectées: " + errors);
            }
            
            // Si la structure de base est valide, procéder aux autres validations
            if (errors.isEmpty()) {
                LOG.debug("Validation des processus BPMN");
                validateProcesses(doc, errors, warnings, infos);
                
                if (!errors.isEmpty()) {
                    LOG.warn("Erreurs de validation des processus: " + errors);
                } else {
                    LOG.debug("Vérification des éléments supportés");
                    validateSupportedElements(doc, warnings);
                    
                    LOG.debug("Vérification des éléments problématiques");
                    validateProblematicElements(doc, errors, warnings);
                    
                    LOG.debug("Validation des données et variables");
                    validateDataAndVariables(doc, warnings, infos);
                    
                    LOG.debug("Vérification de la connectivité");
                    validateConnectivity(doc, errors, warnings);
                    
                    LOG.debug("Vérification de la sémantique Kogito");
                    validateKogitoSemantics(doc, errors, warnings, infos);
                }
            }

        } catch (Exception e) {
            String errorMsg = "Erreur lors de la validation BPMN: " + e.getMessage();
            LOG.error(errorMsg, e);
            errors.add("Erreur technique lors de la validation: " + e.getMessage());
        }

        boolean isValid = errors.isEmpty();
        if (isValid) {
            infos.add("✓ Le BPMN est valide pour Kogito");
        }

        return new BpmnValidationResult(errors, warnings, infos, isValid);
    }

    public BpmnValidationResult validateBpmnFile(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return validateBpmnFile(is, file.getName());
        } catch (Exception e) {
            List<String> errors = new ArrayList<>();
            errors.add("Erreur lors de l'accès au fichier: " + e.getMessage());
            return new BpmnValidationResult(errors, new ArrayList<>(), new ArrayList<>(), false);
        }
    }

    private Document parseXml(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // IMPORTANT pour BPMN avec namespaces
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            // Désactiver les features de sécurité trop restrictives temporairement
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            } catch (Exception e) {
                LOG.warn("Secure processing feature non supporté");
            }
            
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            } catch (Exception e) {
                LOG.warn("Disallow doctype feature non supporté");
            }
    
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // Désactiver les erreurs pour les DTD externes
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    LOG.warn("Avertissement XML: " + e.getMessage());
                }
                
                @Override
                public void error(org.xml.sax.SAXParseException e) {
                    LOG.warn("Erreur XML: " + e.getMessage());
                }
                
                @Override
                public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
                    LOG.error("Erreur fatale XML: " + e.getMessage());
                    throw e;
                }
            });
            
            return builder.parse(new InputSource(new StringReader(content)));
            
        } catch (Exception e) {
            LOG.error("Erreur d'analyse XML: " + e.getMessage(), e);
            return null;
        }
    }

    private void validateBpmnStructure(Document doc, List<String> errors, List<String> warnings, List<String> infos) {
        Element root = doc.getDocumentElement();

        if (!"definitions".equals(root.getLocalName()) &&
            !"bpmn2:definitions".equals(root.getNodeName())) {
            errors.add("La racine doit être 'definitions' ou 'bpmn2:definitions'");
        }

        String xmlns = root.getAttribute("xmlns");
        if (!xmlns.contains("www.omg.org/spec/BPMN")) {
            warnings.add("Namespace BPMN non standard détecté: " + xmlns);
        }

        NodeList processes = doc.getElementsByTagNameNS("*", "process");
        if (processes.getLength() == 0) {
            errors.add("Aucun processus trouvé dans le BPMN");
        } else if (processes.getLength() > 1) {
            infos.add("Fichier BPMN contient " + processes.getLength() + " processus");
        }
    }

    private void validateProcesses(Document doc, List<String> errors, List<String> warnings, List<String> infos) {
        NodeList processes = doc.getElementsByTagNameNS("*", "process");

        for (int i = 0; i < processes.getLength(); i++) {
            Element process = (Element) processes.item(i);
            String processId = process.getAttribute("id");
            String isExecutable = process.getAttribute("isExecutable");

            if (processId == null || processId.trim().isEmpty()) {
                errors.add("Le processus doit avoir un ID");
            } else if (!isValidId(processId)) {
                warnings.add("L'ID du processus '" + processId + "' contient des caractères spéciaux");
            }

            if (!"true".equals(isExecutable)) {
                errors.add("Le processus '" + processId + "' doit avoir isExecutable='true'");
            }

            validateProcessElements(process, processId, errors, warnings, infos);
        }
    }

    private void validateProcessElements(Element process, String processId, List<String> errors, List<String> warnings, List<String> infos) {
        NodeList startEvents = process.getElementsByTagNameNS("*", "startEvent");
        if (startEvents.getLength() == 0) {
            errors.add("Le processus '" + processId + "' doit avoir au moins un startEvent");
        }

        NodeList endEvents = process.getElementsByTagNameNS("*", "endEvent");
        if (endEvents.getLength() == 0) {
            warnings.add("Le processus '" + processId + "' n'a pas de endEvent");
        }

        for (String problematic : PROBLEMATIC_ELEMENTS) {
            NodeList elements = process.getElementsByTagNameNS("*", problematic);
            if (elements.getLength() > 0) {
                warnings.add("Le processus '" + processId + "' contient '" + problematic + "' non supporté par Kogito");
            }
        }
    }

    private void validateSupportedElements(Document doc, List<String> warnings) {
        NodeList allElements = doc.getElementsByTagNameNS("*", "*");

        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            String elementName = element.getLocalName();

            // Ignorer les éléments qui ne sont pas des éléments BPMN de workflow
            if (elementName == null ||
                elementName.equals("definitions") ||
                elementName.equals("process") ||
                elementName.startsWith("bpmn2:") ||
                elementName.contains("extension") ||
                elementName.contains("property") ||
                elementName.contains("dataObject") ||
                elementName.contains("dataStore") ||
                elementName.contains("message") ||
                elementName.contains("signal") ||
                elementName.contains("error") ||
                elementName.contains("escalation") ||
                elementName.contains("condition") ||
                elementName.contains("script") ||
                elementName.contains("timer") ||
                elementName.contains("time") ||
                elementName.contains("eventDefinition")) {
                continue;
            }

            // Vérifier si l'élément est dans la liste des éléments supportés
            if (!SUPPORTED_ELEMENTS.contains(elementName)) {
                String elementId = element.getAttribute("id");
                String elementType = elementName != null ? elementName : "unknown";
                warnings.add("Élément non supporté détecté: '" + elementType + "' (id: " + elementId + ") - peut causer des problèmes avec Kogito");
            }
        }
    }

    private void validateProblematicElements(Document doc, List<String> errors, List<String> warnings) {
        String[] problematicElements = {"businessRuleTask", "sendTask", "receiveTask", "manualTask"};
        for (String element : problematicElements) {
            NodeList elements = doc.getElementsByTagNameNS("*", element);
            for (int i = 0; i < elements.getLength(); i++) {
                Element el = (Element) elements.item(i);
                String id = el.getAttribute("id");
                warnings.add("Élément '" + element + "' (" + id + ") peut nécessiter configuration supplémentaire");
            }
        }
    }

    private void validateDataAndVariables(Document doc, List<String> warnings, List<String> infos) {
        NodeList dataObjects = doc.getElementsByTagNameNS("*", "dataObject");
        NodeList properties = doc.getElementsByTagNameNS("*", "property");
        if (dataObjects.getLength() > 0 || properties.getLength() > 0) {
            infos.add("Contient " + dataObjects.getLength() + " dataObject(s) et " + properties.getLength() + " propriété(s)");
        }

        NodeList dataStores = doc.getElementsByTagNameNS("*", "dataStore");
        if (dataStores.getLength() > 0) {
            warnings.add("DataStores détectés - non supportés par Kogito");
        }
    }

    private void validateConnectivity(Document doc, List<String> errors, List<String> warnings) {
        NodeList sequenceFlows = doc.getElementsByTagNameNS("*", "sequenceFlow");
        NodeList allElements = doc.getElementsByTagNameNS("*", "*");

        Map<String, Element> elementsById = new HashMap<>();
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            String id = element.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                elementsById.put(id, element);
            }
        }

        for (int i = 0; i < sequenceFlows.getLength(); i++) {
            Element sequenceFlow = (Element) sequenceFlows.item(i);
            String sourceRef = sequenceFlow.getAttribute("sourceRef");
            String targetRef = sequenceFlow.getAttribute("targetRef");

            if (sourceRef.isEmpty() || targetRef.isEmpty()) {
                errors.add("SequenceFlow sans sourceRef ou targetRef");
            } else if (!elementsById.containsKey(sourceRef)) {
                warnings.add("SequenceFlow référence source inexistante: " + sourceRef);
            } else if (!elementsById.containsKey(targetRef)) {
                warnings.add("SequenceFlow référence cible inexistante: " + targetRef);
            }
        }
    }

    private boolean isValidId(String id) {
        return Pattern.matches("^[a-zA-Z0-9_]+$", id);
    }














    //validation sémantique : validation po détecter les problèmes Kogito

    private void validateKogitoSemantics(Document doc, List<String> errors, List<String> warnings, List<String> infos) {
        NodeList processes = doc.getElementsByTagNameNS("*", "process");
        
        for (int i = 0; i < processes.getLength(); i++) {
            Element process = (Element) processes.item(i);
            String processId = process.getAttribute("id");
            
            // Validation 1: Tâches sans type
            validateTaskTypes(process, processId, errors);
            
            // Validation 2: UserTasks sans assignation
            validateUserTaskAssignments(process, processId, errors);
            
            // Validation 3: Scripts invalides
            validateScriptTasks(process, processId, errors, warnings);
            
            // Validation 4: Variables non définies
            validateDataObjects(process, processId, warnings);
            
            // Validation 5: Événements non supportés
            validateEvents(process, processId, errors, warnings);
            
            // Validation 6: Gateways incomplets
            validateGateways(process, processId, errors, warnings);
        }
    }
    
    private void validateTaskTypes(Element process, String processId, List<String> errors) {
        NodeList tasks = process.getElementsByTagNameNS("*", "task");
        
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            String taskId = task.getAttribute("id");
            String taskName = task.getAttribute("name");
            
            // Vérifier si c'est une tâche générique sans type spécifique
            if (!isTypedTask(task)) {
                String errorMsg = String.format("Node '%s' [%s] Task has no task type", 
                    taskName.isEmpty() ? taskId : taskName, taskId);
                errors.add(errorMsg);
            }
        }
    }
    
    private boolean isTypedTask(Element task) {
        // Vérifier si la tâche a un type spécifique (userTask, scriptTask, etc.)
        String nodeName = task.getNodeName();
        return nodeName.contains("userTask") || 
               nodeName.contains("scriptTask") || 
               nodeName.contains("serviceTask") ||
               nodeName.contains("businessRuleTask") ||
               nodeName.contains("sendTask") || 
               nodeName.contains("receiveTask") ||
               nodeName.contains("manualTask");
    }
    
    private void validateUserTaskAssignments(Element process, String processId, List<String> errors) {
        NodeList userTasks = process.getElementsByTagNameNS("*", "userTask");
        
        for (int i = 0; i < userTasks.getLength(); i++) {
            Element userTask = (Element) userTasks.item(i);
            String taskId = userTask.getAttribute("id");
            String taskName = userTask.getAttribute("name");
            
            // Vérifier les assignations
            boolean hasPotentialOwner = userTask.getElementsByTagNameNS("*", "potentialOwner").getLength() > 0;
            boolean hasHumanPerformer = userTask.getElementsByTagNameNS("*", "humanPerformer").getLength() > 0;
            boolean hasResourceAssignment = userTask.getElementsByTagNameNS("*", "resourceAssignmentExpression").getLength() > 0;
            
            // Vérifier les extensions Kogito
            boolean hasKogitoExtension = hasKogitoTaskExtension(userTask);
            
            if (!hasPotentialOwner && !hasHumanPerformer && !hasResourceAssignment && !hasKogitoExtension) {
                errors.add(String.format("UserTask '%s' [%s] has no assignment - will need runtime assignment", 
                    taskName.isEmpty() ? taskId : taskName, taskId));
            }
        }
    }
    
    private boolean hasKogitoTaskExtension(Element userTask) {
        NodeList extensions = userTask.getElementsByTagNameNS("*", "extensionElements");
        for (int i = 0; i < extensions.getLength(); i++) {
            Element extension = (Element) extensions.item(i);
            if (extension.getTextContent().contains("kogito") || 
                extension.getTextContent().contains("groups") ||
                extension.getTextContent().contains("users")) {
                return true;
            }
        }
        return false;
    }
    
    private void validateScriptTasks(Element process, String processId, List<String> errors, List<String> warnings) {
        NodeList scriptTasks = process.getElementsByTagNameNS("*", "scriptTask");
        
        for (int i = 0; i < scriptTasks.getLength(); i++) {
            Element scriptTask = (Element) scriptTasks.item(i);
            String taskId = scriptTask.getAttribute("id");
            String scriptType = scriptTask.getAttribute("scriptFormat");
            
            // Vérifier le type de script
            if (scriptType.isEmpty()) {
                errors.add(String.format("ScriptTask '%s' has no scriptFormat attribute", taskId));
            } else if (!isSupportedScriptFormat(scriptType)) {
                warnings.add(String.format("ScriptTask '%s' uses unsupported script format: %s", taskId, scriptType));
            }
            
            // Vérifier le contenu du script
            NodeList scripts = scriptTask.getElementsByTagNameNS("*", "script");
            if (scripts.getLength() == 0) {
                errors.add(String.format("ScriptTask '%s' has no script content", taskId));
            } else {
                Element script = (Element) scripts.item(0);
                String scriptContent = script.getTextContent();
                if (scriptContent == null || scriptContent.trim().isEmpty()) {
                    errors.add(String.format("ScriptTask '%s' has empty script", taskId));
                }
            }
        }
    }
    
    private boolean isSupportedScriptFormat(String format) {
        return Arrays.asList("java", "javascript", "mvel", "drools").contains(format.toLowerCase());
    }
    
    private void validateDataObjects(Element process, String processId, List<String> warnings) {
        NodeList dataObjects = process.getElementsByTagNameNS("*", "dataObject");
        
        Set<String> definedDataObjects = new HashSet<>();
        
        // Collecter les dataObjects définis
        for (int i = 0; i < dataObjects.getLength(); i++) {
            Element dataObject = (Element) dataObjects.item(i);
            String dataObjectId = dataObject.getAttribute("id");
            definedDataObjects.add(dataObjectId);
        }
        
        // Vérifier les références dans les dataInputAssociations
        NodeList dataInputAssociations = process.getElementsByTagNameNS("*", "dataInputAssociation");
        for (int i = 0; i < dataInputAssociations.getLength(); i++) {
            Element association = (Element) dataInputAssociations.item(i);
            String targetRef = association.getAttribute("targetRef");
            if (!targetRef.isEmpty() && !definedDataObjects.contains(targetRef)) {
                warnings.add(String.format("DataInputAssociation references undefined data object: %s", targetRef));
            }
        }
    }
    
    private void validateEvents(Element process, String processId, List<String> errors, List<String> warnings) {
        NodeList events = process.getElementsByTagNameNS("*", "*");

        for (int i = 0; i < events.getLength(); i++) {
            Element event = (Element) events.item(i);
            String eventName = event.getNodeName();

            if (eventName.contains("Event")) {
                String eventId = event.getAttribute("id");

                // Valider le type d'événement supporté
                validateEventType(event, eventId, warnings);

                // Vérifier les événements de type message sans définition
                if (eventName.contains("Message")) {
                    validateMessageEvent(event, eventId, errors, warnings);
                }

                // Vérifier les événements de type timer
                if (eventName.contains("Timer")) {
                    validateTimerEvent(event, eventId, errors);
                }

                // Vérifier les événements de type signal
                if (eventName.contains("Signal")) {
                    validateSignalEvent(event, eventId, errors);
                }
            }
        }
    }
    
    private void validateMessageEvent(Element event, String eventId, List<String> errors, List<String> warnings) {
        NodeList messageRefs = event.getElementsByTagNameNS("*", "messageEventDefinition");
        if (messageRefs.getLength() > 0) {
            Element messageDef = (Element) messageRefs.item(0);
            String messageRef = messageDef.getAttribute("messageRef");
            if (messageRef.isEmpty()) {
                errors.add(String.format("MessageEvent '%s' has no messageRef", eventId));
            }
        }
    }
    
    private void validateTimerEvent(Element event, String eventId, List<String> errors) {
        NodeList timerRefs = event.getElementsByTagNameNS("*", "timerEventDefinition");
        if (timerRefs.getLength() > 0) {
            Element timerDef = (Element) timerRefs.item(0);
            NodeList timeDurations = timerDef.getElementsByTagNameNS("*", "timeDuration");
            NodeList timeCycles = timerDef.getElementsByTagNameNS("*", "timeCycle");
            NodeList timeDates = timerDef.getElementsByTagNameNS("*", "timeDate");
            
            if (timeDurations.getLength() == 0 && timeCycles.getLength() == 0 && timeDates.getLength() == 0) {
                errors.add(String.format("TimerEvent '%s' has no time definition", eventId));
            }
        }
    }
    
    private void validateSignalEvent(Element event, String eventId, List<String> errors) {
        NodeList signalRefs = event.getElementsByTagNameNS("*", "signalEventDefinition");
        if (signalRefs.getLength() > 0) {
            Element signalDef = (Element) signalRefs.item(0);
            String signalRef = signalDef.getAttribute("signalRef");
            if (signalRef.isEmpty()) {
                errors.add(String.format("SignalEvent '%s' has no signalRef", eventId));
            }
        }
    }

    private void validateEventType(Element event, String eventId, List<String> warnings) {
        // Chercher toutes les définitions d'événement dans cet élément
        NodeList eventDefinitions = event.getElementsByTagNameNS("*", "*");

        for (int i = 0; i < eventDefinitions.getLength(); i++) {
            Element eventDef = (Element) eventDefinitions.item(i);
            String defName = eventDef.getLocalName();

            // Vérifier si c'est une définition d'événement (se termine par EventDefinition)
            if (defName != null && defName.endsWith("EventDefinition")) {
                // Extraire le type d'événement (par exemple "message" de "messageEventDefinition")
                String eventType = defName.replace("EventDefinition", "").toLowerCase();

                // Vérifier si le type d'événement est supporté
                if (!SUPPORTED_EVENTS.contains(eventType)) {
                    warnings.add(String.format("Type d'événement non supporté détecté: '%s' dans l'événement '%s' - peut causer des problèmes avec Kogito",
                        eventType, eventId));
                }
            }
        }
    }
    





    //validation des gateways (izay essentiels ihany)
    private void validateGateways(Element process, String processId, List<String> errors, List<String> warnings) {
        NodeList allElements = process.getElementsByTagNameNS("*", "*");
        
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            String elementName = element.getLocalName();
            
            if (elementName != null && elementName.endsWith("Gateway")) {
                String gatewayId = element.getAttribute("id");
                String gatewayName = element.getAttribute("name");
                
                // Validation de base pour tous les gateways
                validateBasicGatewayStructure(element, gatewayId, gatewayName, errors);
                
                // Validation spécifique par type
                switch (elementName) {
                    case "exclusiveGateway":
                        validateExclusiveGateway(element, gatewayId, errors, warnings);
                        break;
                    case "parallelGateway":
                        validateParallelGateway(element, gatewayId, errors);
                        break;
                    case "inclusiveGateway":
                        validateInclusiveGateway(element, gatewayId, errors, warnings);
                        break;
                    case "eventBasedGateway":
                        validateEventBasedGateway(element, gatewayId, errors);
                        break;
                    case "complexGateway":
                        errors.add(String.format("ComplexGateway '%s' is not supported by Kogito", gatewayId));
                        break;
                    default:
                        warnings.add(String.format("Unknown gateway type '%s' for gateway '%s'", elementName, gatewayId));
                        break;
                }
                
                // Validation des flows entrants/sortants
                validateGatewayFlows(process, element, gatewayId, errors, warnings);
            }
        }
    }
    
    private void validateBasicGatewayStructure(Element gateway, String gatewayId, String gatewayName, List<String> errors) {
        // Vérifier que le gateway a un ID
        if (gatewayId == null || gatewayId.trim().isEmpty()) {
            errors.add("Gateway must have an id attribute");
            return;
        }
        
        // Vérifier la validité de l'ID
        if (!isValidId(gatewayId)) {
            errors.add(String.format("Gateway ID '%s' contains invalid characters", gatewayId));
        }
    }
    
    private void validateExclusiveGateway(Element gateway, String gatewayId, List<String> errors, List<String> warnings) {
        Document doc = gateway.getOwnerDocument();
        NodeList outgoingFlows = getOutgoingSequenceFlows(doc, gatewayId);
        
        if (outgoingFlows.getLength() == 0) {
            errors.add(String.format("ExclusiveGateway '%s' has no outgoing sequence flows", gatewayId));
            return;
        }
        
        // Compter les flows avec/sans conditions
        int conditionalFlows = 0;
        int defaultFlows = 0;
        
        for (int i = 0; i < outgoingFlows.getLength(); i++) {
            Element flow = (Element) outgoingFlows.item(i);
            
            // Vérifier si c'est un flow par défaut
            if ("true".equals(flow.getAttribute("isDefault"))) {
                defaultFlows++;
                continue;
            }
            
            // Vérifier les conditions
            NodeList conditions = flow.getElementsByTagNameNS("*", "conditionExpression");
            if (conditions.getLength() > 0) {
                conditionalFlows++;
                Element condition = (Element) conditions.item(0);
                String conditionText = condition.getTextContent();
                if (conditionText == null || conditionText.trim().isEmpty()) {
                    errors.add(String.format("SequenceFlow from ExclusiveGateway '%s' has empty condition", gatewayId));
                }
            }
        }
        
        // Règles pour les gateways exclusifs
        if (defaultFlows > 1) {
            errors.add(String.format("ExclusiveGateway '%s' has multiple default flows - only one allowed", gatewayId));
        }
        
        if (conditionalFlows == 0 && defaultFlows == 0) {
            errors.add(String.format("ExclusiveGateway '%s' has no conditional flows and no default flow", gatewayId));
        }
        
        if (conditionalFlows > 0 && defaultFlows == 0) {
            warnings.add(String.format("ExclusiveGateway '%s' has conditional flows but no default flow - risk of deadlock", gatewayId));
        }
    }
    
    private void validateParallelGateway(Element gateway, String gatewayId, List<String> errors) {
        Document doc = gateway.getOwnerDocument();
        NodeList incomingFlows = getIncomingSequenceFlows(doc, gatewayId);
        NodeList outgoingFlows = getOutgoingSequenceFlows(doc, gatewayId);
        
        // Gateway parallèle doit avoir au moins 1 flow entrant et 2+ flows sortants
        if (incomingFlows.getLength() == 0) {
            errors.add(String.format("ParallelGateway '%s' has no incoming sequence flows", gatewayId));
        }
        
        if (outgoingFlows.getLength() < 2) {
            errors.add(String.format("ParallelGateway '%s' has only %d outgoing flow(s) - parallel gateways should fork to multiple paths", 
                gatewayId, outgoingFlows.getLength()));
        }
        
        // Vérifier qu'aucun flow sortant n'a de condition
        for (int i = 0; i < outgoingFlows.getLength(); i++) {
            Element flow = (Element) outgoingFlows.item(i);
            NodeList conditions = flow.getElementsByTagNameNS("*", "conditionExpression");
            if (conditions.getLength() > 0) {
                errors.add(String.format("ParallelGateway '%s' has conditional outgoing flow - parallel gateways cannot have conditions", gatewayId));
            }
        }
    }
    
    private void validateInclusiveGateway(Element gateway, String gatewayId, List<String> errors, List<String> warnings) {
        Document doc = gateway.getOwnerDocument();
        NodeList outgoingFlows = getOutgoingSequenceFlows(doc, gatewayId);
        
        if (outgoingFlows.getLength() == 0) {
            errors.add(String.format("InclusiveGateway '%s' has no outgoing sequence flows", gatewayId));
            return;
        }
        
        // Gateway inclusif doit avoir des conditions sur certains flows
        int conditionalFlows = 0;
        int defaultFlows = 0;
        
        for (int i = 0; i < outgoingFlows.getLength(); i++) {
            Element flow = (Element) outgoingFlows.item(i);
            
            if ("true".equals(flow.getAttribute("isDefault"))) {
                defaultFlows++;
            }
            
            NodeList conditions = flow.getElementsByTagNameNS("*", "conditionExpression");
            if (conditions.getLength() > 0) {
                conditionalFlows++;
                Element condition = (Element) conditions.item(0);
                String conditionText = condition.getTextContent();
                if (conditionText == null || conditionText.trim().isEmpty()) {
                    errors.add(String.format("SequenceFlow from InclusiveGateway '%s' has empty condition", gatewayId));
                }
            }
        }
        
        if (conditionalFlows == 0) {
            errors.add(String.format("InclusiveGateway '%s' has no conditional flows - inclusive gateways require conditions", gatewayId));
        }
        
        if (defaultFlows > 1) {
            errors.add(String.format("InclusiveGateway '%s' has multiple default flows - only one allowed", gatewayId));
        }
    }
    
    private void validateEventBasedGateway(Element gateway, String gatewayId, List<String> errors) {
        Document doc = gateway.getOwnerDocument();
        NodeList outgoingFlows = getOutgoingSequenceFlows(doc, gatewayId);
        
        if (outgoingFlows.getLength() == 0) {
            errors.add(String.format("EventBasedGateway '%s' has no outgoing sequence flows", gatewayId));
            return;
        }
        
        // Vérifier que chaque flow sortant pointe vers un événement d'attente
        for (int i = 0; i < outgoingFlows.getLength(); i++) {
            Element flow = (Element) outgoingFlows.item(i);
            String targetRef = flow.getAttribute("targetRef");
            
            if (targetRef.isEmpty()) {
                errors.add(String.format("EventBasedGateway '%s' has sequence flow without target", gatewayId));
                continue;
            }
            
            Element targetElement = findElementById(doc, targetRef);
            if (targetElement == null) {
                errors.add(String.format("EventBasedGateway '%s' points to non-existent element: %s", gatewayId, targetRef));
                continue;
            }
            
            String targetType = targetElement.getLocalName();
            if (targetType == null || !targetType.contains("CatchEvent")) {
                errors.add(String.format("EventBasedGateway '%s' must connect to intermediate catch events, but connects to: %s", 
                    gatewayId, targetType));
            }
        }
        
        // Vérifier le type d'événement (instantiate attribute)
        String instantiate = gateway.getAttribute("instantiate");
        if (!instantiate.isEmpty() && !"true".equals(instantiate) && !"false".equals(instantiate)) {
            errors.add(String.format("EventBasedGateway '%s' has invalid instantiate value: %s", gatewayId, instantiate));
        }
    }
    
    private void validateGatewayFlows(Element process, Element gateway, String gatewayId, List<String> errors, List<String> warnings) {
        Document doc = gateway.getOwnerDocument();
        
        NodeList incomingFlows = getIncomingSequenceFlows(doc, gatewayId);
        NodeList outgoingFlows = getOutgoingSequenceFlows(doc, gatewayId);
        
        // Vérifier la direction du gateway
        if (incomingFlows.getLength() == 0 && outgoingFlows.getLength() == 0) {
            errors.add(String.format("Gateway '%s' has no incoming or outgoing flows - unknown gateway direction", gatewayId));
        } else if (incomingFlows.getLength() == 0) {
            // Gateway de début (diverging)
            validateDivergingGateway(gateway, gatewayId, outgoingFlows, errors);
        } else if (outgoingFlows.getLength() == 0) {
            // Gateway de fin (converging)
            validateConvergingGateway(gateway, gatewayId, incomingFlows, errors);
        } else {
            // Gateway mixte - généralement problématique
            warnings.add(String.format("Gateway '%s' has both incoming and outgoing flows - mixed gateway direction may cause issues", gatewayId));
        }
        
        // Vérifier les références des flows
        validateFlowReferences(doc, gatewayId, incomingFlows, outgoingFlows, errors);
    }
    
    private void validateDivergingGateway(Element gateway, String gatewayId, NodeList outgoingFlows, List<String> errors) {
        String gatewayType = gateway.getLocalName();
        
        if (outgoingFlows.getLength() < 2) {
            errors.add(String.format("Diverging %s '%s' has only %d outgoing flow(s) - should have multiple paths", 
                gatewayType, gatewayId, outgoingFlows.getLength()));
        }
    }
    
    private void validateConvergingGateway(Element gateway, String gatewayId, NodeList incomingFlows, List<String> errors) {
        String gatewayType = gateway.getLocalName();
        
        if (incomingFlows.getLength() < 2 && !"exclusiveGateway".equals(gatewayType)) {
            errors.add(String.format("Converging %s '%s' has only %d incoming flow(s) - converging gateways typically merge multiple paths", 
                gatewayType, gatewayId, incomingFlows.getLength()));
        }
    }
    
    private void validateFlowReferences(Document doc, String gatewayId, NodeList incomingFlows, NodeList outgoingFlows, List<String> errors) {
        // Vérifier que tous les flows entrants existent
        for (int i = 0; i < incomingFlows.getLength(); i++) {
            Element flow = (Element) incomingFlows.item(i);
            String sourceRef = flow.getAttribute("sourceRef");
            if (sourceRef.isEmpty() || findElementById(doc, sourceRef) == null) {
                errors.add(String.format("Gateway '%s' has incoming flow with invalid source: %s", gatewayId, sourceRef));
            }
        }
        
        // Vérifier que tous les flows sortants existent
        for (int i = 0; i < outgoingFlows.getLength(); i++) {
            Element flow = (Element) outgoingFlows.item(i);
            String targetRef = flow.getAttribute("targetRef");
            if (targetRef.isEmpty() || findElementById(doc, targetRef) == null) {
                errors.add(String.format("Gateway '%s' has outgoing flow with invalid target: %s", gatewayId, targetRef));
            }
        }
    }
    
    // Méthodes utilitaires
    private NodeList getIncomingSequenceFlows(Document doc, String elementId) {
        NodeList allFlows = doc.getElementsByTagNameNS("*", "sequenceFlow");
        List<Element> incomingFlows = new ArrayList<>();
        
        for (int i = 0; i < allFlows.getLength(); i++) {
            Element flow = (Element) allFlows.item(i);
            if (elementId.equals(flow.getAttribute("targetRef"))) {
                incomingFlows.add(flow);
            }
        }
        
        return createNodeList(incomingFlows);
    }
    
    private NodeList getOutgoingSequenceFlows(Document doc, String elementId) {
        NodeList allFlows = doc.getElementsByTagNameNS("*", "sequenceFlow");
        List<Element> outgoingFlows = new ArrayList<>();
        
        for (int i = 0; i < allFlows.getLength(); i++) {
            Element flow = (Element) allFlows.item(i);
            if (elementId.equals(flow.getAttribute("sourceRef"))) {
                outgoingFlows.add(flow);
            }
        }
        
        return createNodeList(outgoingFlows);
    }
    
    private Element findElementById(Document doc, String id) {
        NodeList allElements = doc.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            if (id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        return null;
    }
    
    private NodeList createNodeList(List<Element> elements) {
        return new NodeList() {
            public Node item(int index) { return elements.get(index); }
            public int getLength() { return elements.size(); }
        };
    }
    
}
