package mg.orange.workflow.service.bpmn;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jakarta.enterprise.context.ApplicationScoped;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Implémentation du service de normalisation BPMN.
 * Corrige automatiquement les erreurs courantes de conformité BPMN 2.0.
 */
@ApplicationScoped
public class BpmnNormalizerServiceImpl implements BpmnNormalizerService {

    private static final Logger LOG = Logger.getLogger(BpmnNormalizerServiceImpl.class);

    @Override
    public String normalizeBpmn(String bpmnXml) {
        try {
            Document doc = parseXml(bpmnXml);
            boolean modified = false;

            // Correction 1: Ajouter outputSet manquant aux ioSpecification
            modified |= fixIoSpecificationOutputSet(doc);

            // Correction 2: Autres corrections peuvent être ajoutées ici

            if (modified) {
                LOG.info("BPMN normalisé - corrections appliquées");
                return serializeXml(doc);
            }

            return bpmnXml;

        } catch (Exception e) {
            LOG.warn("Erreur lors de la normalisation BPMN, retour du XML original", e);
            return bpmnXml;
        }
    }

    @Override
    public boolean needsNormalization(String bpmnXml) {
        try {
            Document doc = parseXml(bpmnXml);

            // Vérifier si des corrections sont nécessaires
            return hasIncompleteIoSpecification(doc);

        } catch (Exception e) {
            LOG.debug("Erreur lors de l'analyse du BPMN pour normalisation", e);
            return false;
        }
    }

    /**
     * Parse le XML BPMN en document DOM.
     */
    private Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Sérialise un document DOM en chaîne XML.
     */
    private String serializeXml(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Vérifie si le document contient des ioSpecification incomplètes.
     * Une ioSpecification est considérée incomplète si elle a un inputSet mais pas d'outputSet,
     * car certains parsers BPMN stricts s'attendent à avoir les deux.
     */
    private boolean hasIncompleteIoSpecification(Document doc) {
        NodeList ioSpecs = doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "ioSpecification");
        for (int i = 0; i < ioSpecs.getLength(); i++) {
            Element ioSpec = (Element) ioSpecs.item(i);

            // Vérifier s'il y a un inputSet sans outputSet
            NodeList inputSets = ioSpec.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "inputSet");
            NodeList outputSets = ioSpec.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "outputSet");

            if (inputSets.getLength() > 0 && outputSets.getLength() == 0) {
                return true; // inputSet sans outputSet
            }
        }
        return false;
    }

    /**
     * Corrige les ioSpecification qui n'ont pas d'outputSet.
     * Selon BPMN 2.0, ioSpecification peut avoir inputSet et/ou outputSet,
     * mais certains parsers stricts s'attendent à avoir les deux.
     */
    private boolean fixIoSpecificationOutputSet(Document doc) {
        boolean modified = false;

        NodeList ioSpecs = doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "ioSpecification");
        for (int i = 0; i < ioSpecs.getLength(); i++) {
            Element ioSpec = (Element) ioSpecs.item(i);

            // Vérifier s'il y a un inputSet mais pas d'outputSet
            NodeList inputSets = ioSpec.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "inputSet");
            NodeList outputSets = ioSpec.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "outputSet");

            if (inputSets.getLength() > 0 && outputSets.getLength() == 0) {
                // Ajouter un outputSet vide avec le bon prefix
                Element outputSet = doc.createElementNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2:outputSet");
                ioSpec.appendChild(outputSet);

                LOG.debug("outputSet ajouté à ioSpecification");
                modified = true;
            }
        }

        return modified;
    }
}
