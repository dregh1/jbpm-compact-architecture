package mg.orange.workflow.service.bpmn;

import mg.orange.workflow.model.bpmn.VersionChangeType;
import mg.orange.workflow.util.SemverUtils;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.enterprise.context.ApplicationScoped;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashSet;
import java.util.Set;

/**
 * Implémentation du service de gestion du versioning BPMN
 * Analyse les différences XML pour classifier automatiquement les types de changements
 */
@ApplicationScoped
public class VersionManagementServiceImpl implements VersionManagementService {

    private static final Logger LOG = Logger.getLogger(VersionManagementServiceImpl.class);

    @Override
    public VersionChangeType calculateChangeType(String oldBpmnXml, String newBpmnXml) {
        if (newBpmnXml == null) {
            throw new IllegalArgumentException("Le contenu BPMN nouveau ne peut pas être nul");
        }

        // Création initiale : toujours MAJOR (passage à 1.0.0)
        if (oldBpmnXml == null) {
            return VersionChangeType.MAJOR;
        }

        BpmnComparisonResult comparison = compareBpmnVersions(oldBpmnXml, newBpmnXml);
        return comparison.classifyChangeType();
    }

    @Override
    public BpmnComparisonResult compareBpmnVersions(String oldBpmnXml, String newBpmnXml) {
        BpmnComparisonResult result = new BpmnComparisonResult();

        try {
            Document oldDoc = parseXml(oldBpmnXml);
            Document newDoc = parseXml(newBpmnXml);

            if (oldDoc != null && newDoc != null) {
                compareBpmnElements(oldDoc, newDoc, result);
                compareBpmnFlows(oldDoc, newDoc, result);
            } else {
                LOG.warn("Impossible de parser un des documents BPMN pour comparaison");
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de la comparaison des BPMN", e);
            // En cas d'erreur, considérer comme changement majeur par défaut
        }

        return result;
    }

    @Override
    public VersionChangeType classifyChangeType(BpmnComparisonResult comparison) {
        return comparison.classifyChangeType();
    }

    @Override
    public boolean hasSignificantChanges(String oldBpmnXml, String newBpmnXml) {
        if (oldBpmnXml == null || newBpmnXml == null) {
            return oldBpmnXml != newBpmnXml; // Changement si l'un des deux est null
        }

        try {
            Document oldDoc = parseXml(oldBpmnXml);
            Document newDoc = parseXml(newBpmnXml);

            if (oldDoc != null && newDoc != null) {
                return hasStructuralDifferences(oldDoc, newDoc);
            }
            return !oldBpmnXml.trim().equals(newBpmnXml.trim());
        } catch (Exception e) {
            LOG.warn("Erreur lors de la vérification des changements", e);
            return !oldBpmnXml.trim().equals(newBpmnXml.trim());
        }
    }

    @Override
    public String generateChangeDescription(BpmnComparisonResult comparison) {
        return comparison.generateSummary();
    }

    @Override
    public boolean validateVersioningLogic(String currentVersion, VersionChangeType newChangeType) {
        if (currentVersion == null || newChangeType == null) {
            throw new IllegalArgumentException("Version courante et type de changement requis");
        }

        SemverUtils.validateSemver(currentVersion);

        // Pour cet implémentation de base, on accepte tous les types de changements
        // Validation plus stricte pourrait être ajoutée selon les règles métier
        return true;
    }

    /**
     * Parse un document XML BPMN selon les bonnes pratiques
     */
    private Document parseXml(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            } catch (Exception e) {
                LOG.warn("Secure processing feature non supporté");
            }

            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        } catch (Exception e) {
            LOG.error("Erreur d'analyse XML BPMN", e);
            return null;
        }
    }

    /**
     * Compare les éléments BPMN entre deux versions
     */
    private void compareBpmnElements(Document oldDoc, Document newDoc, BpmnComparisonResult result) {
        // Éléments à comparer: process, tasks, events, gateways
        String[] elementTypes = {
            "userTask", "scriptTask", "serviceTask", "businessRuleTask", "manualTask",
            "sendTask", "receiveTask", "startEvent", "endEvent", "intermediateThrowEvent",
            "intermediateCatchEvent", "boundaryEvent", "exclusiveGateway", "parallelGateway",
            "inclusiveGateway", "eventBasedGateway"
        };

        for (String elementType : elementTypes) {
            compareElementType(oldDoc, newDoc, elementType, result);
        }
    }

    /**
     * Compare un type spécifique d'éléments
     */
    private void compareElementType(Document oldDoc, Document newDoc, String elementType, BpmnComparisonResult result) {
        Set<String> oldElementIds = extractElementIds(oldDoc, elementType);
        Set<String> newElementIds = extractElementIds(newDoc, elementType);

        // Éléments ajoutés
        for (String newId : newElementIds) {
            if (!oldElementIds.contains(newId)) {
                result.addElement(newId, elementType);
            }
        }

        // Éléments supprimés
        for (String oldId : oldElementIds) {
            if (!newElementIds.contains(oldId)) {
                result.removeElement(oldId, elementType);
            }
        }

        // Éléments modifiés (même ID mais propriétés différentes)
        compareCommonElements(oldDoc, newDoc, elementType, oldElementIds, newElementIds, result);
    }

    /**
     * Compare les propriétés des éléments communs
     */
    private void compareCommonElements(Document oldDoc, Document newDoc, String elementType,
            Set<String> oldIds, Set<String> newIds, BpmnComparisonResult result) {

        for (String elementId : oldIds) {
            if (newIds.contains(elementId)) {
                Element oldElement = findElementById(oldDoc, elementId);
                Element newElement = findElementById(newDoc, elementId);

                if (oldElement != null && newElement != null) {
                    compareElementProperties(oldElement, newElement, result);
                }
            }
        }
    }

    /**
     * Compare les propriétés d'un élément BPMN
     */
    private void compareElementProperties(Element oldElement, Element newElement, BpmnComparisonResult result) {
        String elementId = oldElement.getAttribute("id");

        // Comparaison des noms
        String oldName = oldElement.getAttribute("name");
        String newName = newElement.getAttribute("name");

        if (!equalsOrBothEmpty(oldName, newName)) {
            result.modifyElement(elementId, oldElement.getLocalName(),
                oldName != null ? oldName : "", newName != null ? newName : "");
        }
    }

    /**
     * Compare les flux BPMN (sequenceFlows)
     */
    private void compareBpmnFlows(Document oldDoc, Document newDoc, BpmnComparisonResult result) {
        Set<String> oldFlowIds = extractElementIds(oldDoc, "sequenceFlow");
        Set<String> newFlowIds = extractElementIds(newDoc, "sequenceFlow");

        // Flux ajoutés
        for (String newId : newFlowIds) {
            if (!oldFlowIds.contains(newId)) {
                result.addFlow(newId);
            }
        }

        // Flux supprimés
        for (String oldId : oldFlowIds) {
            if (!newFlowIds.contains(oldId)) {
                result.removeFlow(oldId);
            }
        }
    }

    /**
     * Vérifie rapidement s'il y a des différences structurelles significatives
     */
    private boolean hasStructuralDifferences(Document oldDoc, Document newDoc) {
        String[] criticalElements = {"startEvent", "endEvent", "task", "gateway"};

        for (String elementType : criticalElements) {
            Set<String> oldIds = extractElementIds(oldDoc, elementType);
            Set<String> newIds = extractElementIds(newDoc, elementType);

            if (!oldIds.equals(newIds)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extrait les IDs d'un type d'élément donné
     */
    private Set<String> extractElementIds(Document doc, String elementType) {
        Set<String> ids = new HashSet<>();
        NodeList elements = doc.getElementsByTagNameNS("*", elementType);

        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String id = element.getAttribute("id");
            if (id != null && !id.trim().isEmpty()) {
                ids.add(id);
            }
        }

        return ids;
    }

    /**
     * Trouve un élément par son ID dans un document
     */
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

    /**
     * Utilitaire pour comparer des valeurs (gère null et empty)
     */
    private boolean equalsOrBothEmpty(String s1, String s2) {
        String v1 = s1 != null ? s1.trim() : "";
        String v2 = s2 != null ? s2.trim() : "";
        return v1.equals(v2);
    }
}
