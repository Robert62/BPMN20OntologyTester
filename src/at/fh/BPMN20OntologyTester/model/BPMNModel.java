package at.fh.BPMN20OntologyTester.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.xml.instance.DomDocument;
import org.camunda.bpm.model.xml.instance.DomElement;

/**
 * A BPMNModel is the objectional representation of a .bpmn File(XML)
 * 
 * @author Alexander Hoedl
 *
 */
public class BPMNModel {

	private final BpmnModelInstance model;

	public BPMNModel(BpmnModelInstance model) {
		this.model = model;
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
	public Process getProcessByName(String name) {
		for (Process p : getProcesses()) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	public List<StartEvent> getAllStartEvents() {
		List<StartEvent> starts = new ArrayList<StartEvent>();

		for (Process p : getProcesses()) {
			starts.addAll((List<StartEvent>) p.getChildElementsByType(StartEvent.class));
		}

		return starts;
	}

	public List<StartEvent> getStartEventsForProcess(Process p) {
		return (List<StartEvent>) p.getChildElementsByType(StartEvent.class);
	}

	public List<EndEvent> getAllEndEvents() {
		List<EndEvent> ends = new ArrayList<EndEvent>();

		for (Process p : getProcesses()) {
			ends.addAll((List<EndEvent>) p.getChildElementsByType(EndEvent.class));
		}
		return ends;
	}
	
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
	 * ontolgy has an entry for all of this elements
	 * 
	 * @return
	 */
	public Set<String> getAllElementsOfModel() {
		Set<String> elements = new HashSet<String>();

		DomDocument doc = model.getDocument();
		addElementToSet(doc.getRootElement(), elements);

		return elements;
	}

	/**
	 * Helper Method to add an Element and his childs to the element Set in a
	 * recursive way
	 * 
	 * @param element
	 * @param elementSet
	 */
	private void addElementToSet(DomElement element, Set<String> elementSet) {
		elementSet.add(element.getLocalName());

		// Iterate over all Childs and add them now...
		for (DomElement e : element.getChildElements()) {
			// calls this method for all the children which is Element
			addElementToSet(e, elementSet);
		}
	}
}
