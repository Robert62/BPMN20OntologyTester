package at.fh.BPMN20OntologyTester.view;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;
import org.w3c.dom.Element;

import at.fh.BPMN20OntologyTester.BPMN20OntologyTester;
import at.fh.BPMN20OntologyTester.controller.BPMNModelHandler;
import at.fh.BPMN20OntologyTester.controller.FxController;
import at.fh.BPMN20OntologyTester.controller.OntologyHandler;
import at.fh.BPMN20OntologyTester.model.BPMNElement;
import at.fh.BPMN20OntologyTester.model.BPMNModel;
import at.fh.BPMN20OntologyTester.model.FailedOWLClassRestriction;
import at.fh.BPMN20OntologyTester.model.OWLModel;
import at.fh.BPMN20OntologyTester.model.TestCase;
import at.fh.BPMN20OntologyTester.model.enums.TestCaseEnum;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * Handles user interactions on tab "BPMN2OWL"
 * 
 * @author Alexander Hoedl IMA16 - Information Management (BSc) University of
 *         applied Sciences FH JOANNEUM
 *
 */
public class CompareBpmn2OwlTabFxController implements FxController {

	// GUI Element from Tab "BPMN2OWL"
	@FXML
	private Label lbBPMN2OWLModelName, lbBPMN2OWLProcessAmount, lbBPMNConformanceClass;
	@FXML
	private TextArea taRestrictionDescription;
	@FXML
	private ListView<String> lstXmlNodesNotAsOWLClass, lstAttributesNotAsProperty, lstFailedRestrictions;
	@FXML
	private TreeView<String> treeBPMN20OWL;
	@FXML
	private TreeView<BPMNElement> treeBPMNfailedRestrictions;

	@FXML
	private Button btLoadBPMN, btGenerateReport, btConvertToOWL;

	// Initialized when user load BPMN Model
	private TestCase testcase = null;

	// Lists to show testresults
	ObservableList<String> xmlNodeNotFoundInOWL = FXCollections.observableArrayList();
	ObservableList<String> xmlAttributesNotFoundInOWL = FXCollections.observableArrayList();

	ObservableList<String> failedRestrictionsOfXmlNode = FXCollections.observableArrayList();

	public CompareBpmn2OwlTabFxController() {
	}

	/**
	 * Helper methods to activate or deactivate Buttons on GUI depending an ontology
	 * is set or not
	 */
	private void updateActivationSateofButtons() {
		boolean ontologyExists = OntologyHandler.getInstance().getLoadedOntology().isPresent();

		btLoadBPMN.setDisable(!ontologyExists);
		btConvertToOWL.setDisable(!ontologyExists);
		btGenerateReport.setDisable(!ontologyExists);
	}

	@FXML
	private void initialize() {

		// Initialize GUI-Elements
		lstXmlNodesNotAsOWLClass.setItems(xmlNodeNotFoundInOWL);
		lstAttributesNotAsProperty.setItems(xmlAttributesNotFoundInOWL);
		lstFailedRestrictions.setItems(failedRestrictionsOfXmlNode);

		// Set User-Input Elements according ontology found or not
		updateActivationSateofButtons();
	}

	@FXML
	private void onLoadBPMN() {
		try {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Load BPMN2.0 Process Modell");

			chooser.getExtensionFilters().add(new ExtensionFilter("BPMN 2.0", "*.bpmn"));
			chooser.getExtensionFilters().add(new ExtensionFilter("XML-BPMN", "*.xml"));

			// Handle selected file
			File selectedFile = chooser.showOpenDialog(null);

			if (selectedFile != null) {
				
				// Create Testcase with given BPMN
				loadAndTestProcessModel(selectedFile);
				appendLog("Read BPMN-File <" + selectedFile.getAbsolutePath() + ">");
			}
		} catch (Exception e) {
			appendLog("ERROR - Failed to load File <" + e.getMessage() + ">");
			e.printStackTrace();
		}
	}
	
	
	private void loadAndTestProcessModel(File f) {
		// Use a seperate Thread to create TestResultTabs
		Task<TestCase> ontologyTestsTask = new Task<TestCase>() {
			@Override
			public TestCase call() throws Exception {
				this.updateMessage("Test <" + f.getName() + ">");
				// Create Tab which triggers the execution of the test
				TestCase testcase = new TestCase(BPMNModelHandler.readModelFromFile(f));
				testcase.determineConformanceClassOfModel();
				return testcase;
			}
		};

		// Show loading screen while running
		Stage loadingScreen= BPMN20OntologyTester.getLoadingScreenWhileTask(ontologyTestsTask);
		loadingScreen.show();

		//Trigger action after test results and tabs are created
		ontologyTestsTask.setOnSucceeded(e -> {
			//TestCase tescase = ontologyTestsTask.getValue();
			this.testcase = ontologyTestsTask.getValue();
			// Show loaded BPMN Model Data and Testresults
			showModelOverview(testcase);
			showTcResults(testcase);
			
			// Hide Loadingscreen and set overview log
			loadingScreen.hide();
			
			//Fix hack: autogen report to repeat tests while OWL-Adjustments
			/*try {
				String fileName = testcase.getFileNameOfProcessMOdelCreatedOf();
				File file = new File(System.getProperty("user.home") + "/" + fileName.substring(0, fileName.lastIndexOf(".")) + ".txt");
				writeReportToFile(file);
			} catch(Exception x) {
				x.printStackTrace();
			}*/
		});
		
		ontologyTestsTask.setOnFailed( e -> {
			appendLog("Error occured: " + ontologyTestsTask.getException().getMessage());
			ontologyTestsTask.getException().printStackTrace();
			loadingScreen.hide();
		});
		
		//Start thread
		new Thread(ontologyTestsTask).start();
	}

	@FXML
	private void onReset() {
		testcase = null;
		showModelOverview(testcase);
		showTcResults(testcase);
	}

	@FXML
	private void onUpdateTestCaseResults() {
		// Just refresh GUI
		showTcResults(testcase);
	}

	@FXML
	private void onConvert2OWL() {
		if(testcase != null) {
			try {
				BPMNModel processModel = testcase.getProcessModel();
				
				FileChooser chooser = new FileChooser();
				chooser.setTitle("Export ProcessModel as ontology");
				chooser.getExtensionFilters().add(new ExtensionFilter("Ontology", "*.owl"));
				
				String fileName = processModel.getFileFromWhomModelWasCreated().getName();
				//Remove extension
				fileName = fileName.substring(0, fileName.lastIndexOf("."));
				chooser.setInitialFileName(fileName+".owl");
	
				// Handle selected file
				File selectedFile = chooser.showSaveDialog(null);
				if (selectedFile != null) {		
					
					OWLOntology newOntology = OntologyHandler.getInstance().convertToOntology(processModel);
					OntologyHandler.getInstance().saveOntology(selectedFile, newOntology);
					appendLog("Converted BPMN-Process model to ontology <" + selectedFile.getAbsolutePath() + ">");
				}
			} catch (Exception e) {
				e.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error!");
				alert.setHeaderText("Error while exporting process model as Ontology");
				alert.setContentText("ERROR - Failed to convert process model to ontology <" + e.getMessage() + ">");
				alert.showAndWait();
			} 
		}
	}

	@FXML
	private void onGenerateReport() {
		if (testcase != null ) {
			try {
				FileChooser chooser = new FileChooser();
				chooser.setTitle("Export Testcase report");
	
				chooser.getExtensionFilters().add(new ExtensionFilter("TestcaseReport", "*.txt"));
				String fileName = testcase.getFileNameOfProcessMOdelCreatedOf();
				fileName = fileName.substring(0, fileName.lastIndexOf("."));
				chooser.setInitialFileName(fileName);
				
				File dir = new File("C:/Users/Alexander/FH_Informationsmanagement/5. Semester/BAC2 - Bachelorarbeit2/BPMN20OntologyTester/resource/testresults/TestingConceptAdjustment/structuredTests_detailed");
				chooser.setInitialDirectory(dir);
	
				// Handle selected file
				File selectedFile = chooser.showSaveDialog(null);
	
				if (selectedFile != null) {	
					writeReportToFile(selectedFile);
					
				}
			} catch (Exception e) {
				appendLog("Error - Failed to create testcase report <" + e.getMessage() + ">");
				
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error!");
				alert.setHeaderText("Error creating testcase report");
				alert.setContentText("ERROR - Failed to write to File <" + e.getMessage() + ">");
				alert.showAndWait();
				
				e.printStackTrace();
			} 
		}
	}
	
	private void writeReportToFile(File file) throws Exception {
	
		FileWriter writer = new FileWriter(file);	
		writer.write(testcase.getTestResultReport());
		writer.flush();
		writer.close();
		appendLog("Created Testaces report <" + file.getAbsolutePath() + ">");
		
	}

	@SuppressWarnings("rawtypes")
	@FXML
	private void onHandleClickedOnElementFailedRestrictions(MouseEvent event) {
		Node node = event.getPickResult().getIntersectedNode();
		// Accept clicks only on node cells, and not on empty spaces of the TreeView
		if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {

			BPMNElement selectedNode = treeBPMNfailedRestrictions.getSelectionModel().getSelectedItem().getValue();

			// Show failed restrictions for selectedNode
			failedRestrictionsOfXmlNode.clear();
			for (FailedOWLClassRestriction r : selectedNode.getFailedRestrictions()) {
				failedRestrictionsOfXmlNode.add(r.getFormattedFailingReason());
			}
		}

	}

	@FXML
	private void onHandleClickedOnFailedRestriction(MouseEvent event) {

		TreeItem<BPMNElement> curSelElementFailedRestriction = treeBPMNfailedRestrictions.getSelectionModel()
				.getSelectedItem();

		// Get selected Error-Test of faild Restriction
		String selItem = lstFailedRestrictions.getSelectionModel().getSelectedItem();

		if (curSelElementFailedRestriction != null && selItem != null) {

			// Get selected BPMN-Element of tree with failed restrictions
			BPMNElement selectedBPMNElement = curSelElementFailedRestriction.getValue();
			// Get FailedRestriction Object with error text. Assume the error text is
			// unique!
			Optional<FailedOWLClassRestriction> rest = selectedBPMNElement.getFailedRestrictionWithErrorText(selItem);
			if (rest.isPresent()) {
				OWLProperty affectedProperty = rest.get().getRestriction().getOnProperty();

				// We know here that the ontology must present.
				OWLModel ontology = OntologyHandler.getInstance().getLoadedOntology().get();

				taRestrictionDescription.setText(ontology.getCommentOfEntity(affectedProperty));
			}
		}

	}

	private void showModelOverview(TestCase testcase) {

		// Reset GUI elements
		lbBPMN2OWLModelName.setText("");
		lbBPMN2OWLProcessAmount.setText("");
		lbBPMNConformanceClass.setText("");
		treeBPMN20OWL.setRoot(null);

		// Show Elements for created testcase
		if (testcase != null) {
			
			//Show model as Tree 
			BPMNModel model = testcase.getProcessModel();
			

			// Set Label-Texts
			lbBPMN2OWLModelName.setText(model.getModelDefinitions().getName());
			lbBPMN2OWLProcessAmount.setText("" + testcase.getProcessModel().getProcesses().size());
			lbBPMNConformanceClass.setText(""+model.getConformanceClass());

			// Show Model as tree
			TreeItem<String> rootItem = new TreeItem<String>(model.getModelDefinitions().getName());
			rootItem.setExpanded(true);
			for (org.camunda.bpm.model.bpmn.instance.Process p : model.getProcesses()) {
				TreeItem<String> procItem = new TreeItem<String>(p.getName());

				// Add Start and End Elements
				TreeItem<String> start = new TreeItem<String>("Start:");
				TreeItem<String> end = new TreeItem<String>("End:");
				model.getStartEventsForProcess(p)
						.forEach(e -> start.getChildren().add(new TreeItem<String>(e.getName())));
				model.getEndEventsForProcess(p).forEach(e -> end.getChildren().add(new TreeItem<String>(e.getName())));
				procItem.getChildren().add(start);
				procItem.getChildren().add(end);

				// Add Process item to root
				rootItem.getChildren().add(procItem);
			}

			treeBPMN20OWL.setRoot(rootItem);
		}
	}

	private void showTcResults(TestCase testcase) {
		// Clear TC-specific GUI elements and execute tests of testcase
		xmlNodeNotFoundInOWL.clear();
		xmlAttributesNotFoundInOWL.clear();
		failedRestrictionsOfXmlNode.clear();
		treeBPMNfailedRestrictions.setRoot(null);
		taRestrictionDescription.setText("");

		try {
			if (testcase != null) {
				testXmlNodesNotExistInOWL(testcase);
				testXmlAttriubtesNotExistinOWL(testcase);
				testXmlNodesFailRestrictions(testcase);
			}
		} catch (Exception e) {
			appendLog("Error - Error occured while tests: " + e.getMessage());
			e.printStackTrace();
		}
	};

	private void testXmlNodesNotExistInOWL(TestCase testcase) throws Exception {
		//Execute tests and show result
		testcase.executeTest(TestCaseEnum.XMLElementsAsOWLClasses);
		
		if(testcase.getResultsXmlNodesWithoutOWLClass().isEmpty()) {
			xmlNodeNotFoundInOWL.add("No issues found!");
		} else {
			testcase.getResultsXmlNodesWithoutOWLClass().forEach(e -> {
				//if (!xmlNodeNotFoundInOWL.contains(e.getDomLocalName())) {
					xmlNodeNotFoundInOWL.add(e.getDomLocalName());
				//}
			});
			Collections.sort(xmlNodeNotFoundInOWL);
			appendLog("Found <" + xmlNodeNotFoundInOWL.size()
					+ "> XML-Nodes in BPMN which have no OWL-Class in the ontology!");
		}
	}

	private void testXmlAttriubtesNotExistinOWL(TestCase testcase) throws Exception {
		//Execute tests and show result
		testcase.executeTest(TestCaseEnum.XMlAttributesAsOWLProperties);
		
		if(testcase.getResultsXmlAttributesWithoutOWLProperty().isEmpty()) {
			xmlAttributesNotFoundInOWL.add("No issues found!");
		} else {
		
			testcase.getResultsXmlAttributesWithoutOWLProperty().forEach(attr -> {
				xmlAttributesNotFoundInOWL.add((String) attr);
			});
	
			Collections.sort(xmlAttributesNotFoundInOWL);
			appendLog("Found <" + xmlAttributesNotFoundInOWL.size()
					+ "> XML-Attributes in BPMN which have no OWL-Property in the ontology!");
		}
	}

	private void testXmlNodesFailRestrictions(TestCase testcase) throws Exception {
		
		//Execute tests and show result
		testcase.executeTest(TestCaseEnum.XMLElementFailOWLClassRestrictions);	
		Map<Element,Set<FailedOWLClassRestriction>> elementsFailedRestrictions = testcase.getResultsXmlNodesFailOWLRestrictions();
		int totalFailedElements = 0;


		// Build Tree to show results
		TreeItem<BPMNElement> rootItem;
		if (!elementsFailedRestrictions.isEmpty()) {
			rootItem = new TreeItem<BPMNElement>(new BPMNElement("Failures"));
			rootItem.setExpanded(true);

			for (Element elem: elementsFailedRestrictions.keySet()) {
				String guiDisplayName = elem.getTagName().substring(elem.getTagName().indexOf(":")+1) + "(Err: "
							+ elementsFailedRestrictions.get(elem).size() + ")";

				BPMNElement bpmnElement = new BPMNElement(elem,elementsFailedRestrictions.get(elem),guiDisplayName);
				
				TreeItem<BPMNElement> treeItem = new TreeItem<BPMNElement>(bpmnElement);
				rootItem.getChildren().add(treeItem);
				totalFailedElements++;

			}
			
		} else {
			rootItem = new TreeItem<BPMNElement>(new BPMNElement("No issues found!"));
		}
		
		treeBPMNfailedRestrictions.setRoot(rootItem);
		treeBPMNfailedRestrictions.refresh();

		// Give some Feedback in log
		appendLog("Warning - Found <" + totalFailedElements
				+ "> elements in BPMN which does not meet the defined restrictions in the ontology!");
	}

	private void appendLog(String text) {
		MainSceneFxController.getInstance().appendLog(text);
	}
}
