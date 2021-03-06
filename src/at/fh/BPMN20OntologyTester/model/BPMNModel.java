package at.fh.BPMN20OntologyTester.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import at.fh.BPMN20OntologyTester.model.enums.OWLConformanceClassEnum;

/**
 * Representation of an .bpmn File(XML) (BPMN-Model)
 * 
 * @author Alexander Hödl
 * IMA16 - Information Management
 * University of applied Sciences FH JOANNEUM
 *
 */
public class BPMNModel {

	private final BpmnModelInstance model;
	
	// Represents the File from which the Process model was created out	
	private final File fileCreatedFrom;
	private final Document rawDOMDocument;
	private OWLConformanceClassEnum conformanceClass;
	
	private final List<String> allowedElementNamespacesForBPMN20;

	public BPMNModel(BpmnModelInstance model, File file) throws Exception {
		try {
			this.model = model;
			this.fileCreatedFrom = file;
			this.rawDOMDocument = praseXMlFile(file);
			this.allowedElementNamespacesForBPMN20 = Arrays.asList(
											 "http://www.omg.org/spec/BPMN/20100524/MODEL", 
			                                 "http://www.omg.org/spec/BPMN/20100524/DI"
			                                 //"http://www.omg.org/spec/DD/20100524/DC"
											 );
		}catch (Exception e) {
			throw new Exception ("unable to parse raw XML Doucment for model!");
		}
	}
	
	public List<String> getAllowedNameSpaceURisForBPMN20() {
		return this.allowedElementNamespacesForBPMN20;
	}
	
	/**
	 * Parses given XML-File and creates an DOM-Document out of it
	 * 
	 * @param file
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private Document praseXMlFile(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		doc.getDocumentElement().normalize();	
		return doc;
	}
	
	
	public Document getRawDOMDocument() {
		return rawDOMDocument;
	}
	
	public File getFileFromWhomModelWasCreated() {
		return fileCreatedFrom;
	}
	
	public OWLConformanceClassEnum getConformanceClass() {
		return conformanceClass;
	}

	public void setConformanceClass(OWLConformanceClassEnum conformanceClass) {
		this.conformanceClass = conformanceClass;
	}

	/**
	 * Return the Collaboration of the model which represents the head information
	 * in XLM this is represented by TAG 'collaboration' and occur once
	 * 
	 * @return
	 */
	public Collaboration getCollaboration() {
		List<Collaboration> collList = (List<Collaboration>) model.getModelElementsByType(Collaboration.class);
		return collList.get(0);
	}

	/**
	 * Returns all Processes of the Model. in XML this is represented by TAG
	 * 'process' and can occur more than once
	 * 
	 * @return
	 */
	public List<Process> getProcesses() {
		return (List<Process>) model.getModelElementsByType(Process.class);
	}
		
	/**
	 * Return process object for given name
	 * @param name
	 * @return
	 */
	/*public Process getProcessByName(String name) {
		for (Process p : getProcesses()) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}*/
	

	/**
	 * Returns all childs of given Process as DOMElement which contains the raw information
	 * @param p
	 * @return
	 */
	/*public List<DomElement> getProcessElementsAsDomElements(Process p) {
		List<DomElement> domElements = new ArrayList<DomElement>();
		domElements.addAll(p.getDomElement().getChildElements());
		
		return domElements;
	}*/
	

	/**
	 * Returns a list of StartEvents forgiven Process
	 * @return
	 */
	public List<StartEvent> getStartEventsForProcess(Process p) {
		return (List<StartEvent>) p.getChildElementsByType(StartEvent.class);
	}
	
	/**
	 * Returns a list of EndEvents for given Process
	 * @param p
	 * @return
	 */
	public List<EndEvent> getEndEventsForProcess(Process p) {
		return (List<EndEvent>) p.getChildElementsByType(EndEvent.class);
	}

	/**
	 * Return the BPMNDiagram of the model which represents graphical representation
	 * in XLM this is represented by TAG 'bpmndi:BPMNDiagram' and occur once. If the
	 * element can not found, method will return null.
	 * 
	 * @return
	 */
	public BpmnDiagram getBPMNDiagram() {
		List<BpmnDiagram> dia = (List<BpmnDiagram>) model.getModelElementsByType(BpmnDiagram.class);

		if (dia == null || dia.isEmpty())
			return null;
		return dia.get(0);
	}

	/**
	 * Returns the process description of the BPMN Model
	 * 
	 * @return
	 */
	public Optional<String> getProcessDescription() {
		BpmnDiagram dia = this.getBPMNDiagram();
		if (dia != null) {
			return Optional.of(dia.getDocumentation());
		}

		return Optional.empty();
	}

	/**
	 * Returns the ModelInstance itself
	 * 
	 * @return
	 */
	public BpmnModelInstance getModel() {

		return model;
	}

	/**
	 * Returns the main Definitions Tag from an .bpmn file which contains -
	 * Namespace - Modelname - ...
	 * 
	 * @return
	 */
	public Definitions getModelDefinitions() {
		Definitions def = model.getDefinitions();
		return def;
	}

	/**
	 * Returns a Set of Elements which occure in the model. Required to check if the
	 * ontology has an entry for all of this elements
	 * 
	 * @param ignoreFromExtensionElementsField - Flag to ignore content of 'exteionsElements'
	 * 
	 * @return
	 */
	public Set<Element> getAllElementsOfModel() {
		Set<Element> elements = new HashSet<Element>();

		Document doc = getRawDOMDocument();
		
		for(String nsURI: getAllowedNameSpaceURisForBPMN20()) {
			
			NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(nsURI, "*");
			for(int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					elements.add((Element)node);
				}
			}
		}		
		return elements;
	}
	
	public Set<String> getAllAttributesOfModel() {
		Set<String> xmlAttributes = new HashSet<String>();
		
		
		//Collect all occurred attributes in this model
		getAllAttributesOfNode(rawDOMDocument.getDocumentElement(), xmlAttributes);
		
		return xmlAttributes;
	}
	
	public DomElement getModelDefinitionAsDomElement() {
		return model.getDocument().getRootElement();
	}
	
	/**
	 * Helper method which returns a set of attributes of given element including
	 * all attributes of all child nodes
	 * 
	 * @param element
	 * @param attributeSet
	 */
	private void getAllAttributesOfNode(Element element, Set<String> attributeSet) {

		//Add all attributes of curent given element if namespace of element is allowed
		if(getAllowedNameSpaceURisForBPMN20().contains(element.getNamespaceURI())) {			
			NamedNodeMap attributes = element.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				Node attribute = attributes.item(i);
				String namespaceURI = attribute.getNamespaceURI();
				if(namespaceURI != null) {
					if(getAllowedNameSpaceURisForBPMN20().contains(namespaceURI)) {
						attributeSet.add(attribute.getNodeName());
					}
				} else {
					attributeSet.add(attribute.getNodeName());
				}
				
				//Ignore namespace attributes...
				//if( ! (attribute.getNodeName().startsWith("xmlns") || attribute.getNodeName().startsWith("xsi") ))				
				//	attributeSet.add(attribute.getNodeName());
			}
		}

		// Now add all childs in a recursive way
		NodeList childs = element.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			if (childs.item(i) instanceof Element) {
				getAllAttributesOfNode((Element) childs.item(i), attributeSet);
			}
		}
	}
	
	
}
