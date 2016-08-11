package task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.io.fs.FileUtils;
import component.ServiceNode;
import component.TaxonomyNode;

public class RunTask {
	private String path;
	private Set<String> taskInputs;
	private Set<String> taskOutputs;
	private Node startNode;
	private Node endNode;
	private static GraphDatabaseService tempGraphDatabaseService;
	private Map<String, Node> neo4jServNodes = new HashMap<String, Node>();
	private Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private final String Neo4j_tempDBPath = "database/temp_graph";
	private IndexManager tempIndex = null;
	private Index<Node> tempServices = null;
	private Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();
	Relationship relation;

	public RunTask(String path){
		this.path = path;
		taskInputs = new HashSet<String>();
		taskOutputs = new HashSet<String>();
	}

	public void setNeo4jServNodes(Map<String, Node> neo4jServNodes) {
		this.neo4jServNodes = neo4jServNodes;
	}

	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap) {
		this.taxonomyMap = taxonomyMap;
	}
	public void setServiceNodes(Set<ServiceNode> serviceNodes) {
		this.serviceNodes = serviceNodes;
	}

	public void setTaskInputs(Set<String> taskInputs) {
		this.taskInputs = taskInputs;
	}
	public void setTaskOutputs(Set<String> taskOutputs) {
		this.taskOutputs = taskOutputs;
	}

	public Node getStartNode() {
		return startNode;
	}
	public Node getEndNode() {
		return endNode;
	}
	public void addStartEndNodes() {
		startNode = createStartEndNode("start");
		endNode = createStartEndNode("end");
		neo4jServNodes.put("start", startNode);
		neo4jServNodes.put("end", endNode);
		Transaction transaction = tempGraphDatabaseService.beginTx();


		startNode.setProperty("outputs", taskInputs.toArray(new String[taskInputs.size()]));
		endNode.setProperty("inputs", taskOutputs.toArray(new String[taskOutputs.size()]));

		Set<String> inputServices = new HashSet<String>();
		for(String input: taskInputs){
			TaxonomyNode tNode = taxonomyMap.get(input).parentNode;
			inputServices.addAll(tNode.inputs);
		}
		String[]inputs = new String[inputServices.size()];
		int i = -1;
		for(String s: inputServices){
			i++;
			inputs[i] = s;
		}

		startNode.setProperty("outputServices", inputs);
		Set<String> outputServices = new HashSet<String>();
		for(String output: taskOutputs){
			TaxonomyNode tNode = taxonomyMap.get(output).parentNode;
			outputServices.addAll(tNode.outputs);
		}
		String[]outputs = new String[outputServices.size()];
		i = -1;
		for(String s: outputServices){
			i++;
			outputs[i] = s;
		}
		endNode.setProperty("inputServices", outputs);
		transaction.success();
		transaction.close();

	}
	public void createTempDb() {
		tempGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_tempDBPath);
		Transaction transaction = tempGraphDatabaseService.beginTx();
		try{
			tempIndex = tempGraphDatabaseService.index();
			tempServices = tempIndex.forNodes( "identifiers" );
			Iterable<Node> nodes = tempGraphDatabaseService.getAllNodes();
			neo4jServNodes.clear();
			for(Node n: nodes){
				neo4jServNodes.put(n.getProperty("name").toString(), n);
			}
			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Runtask createTempDb error.."); 
		} finally {
			transaction.close();
		}		
	}
	public void copyDb() {
		File srcDir = new File(path);
		File destDir = new File(Neo4j_tempDBPath);
		try {
			FileUtils.deleteRecursively(new File(Neo4j_tempDBPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			copyFolder(srcDir, destDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("copying of file from Java program is completed");	
	}
	private static void copyFolder(File sourceFolder, File destinationFolder) throws IOException
	{
		//Check if sourceFolder is a directory or file
		//If sourceFolder is file; then copy the file directly to new location
		if (sourceFolder.isDirectory()) 
		{
			//Verify if destinationFolder is already present; If not then create it
			if (!destinationFolder.exists()) 
			{
				destinationFolder.mkdir();
				//				System.out.println("Directory created :: " + destinationFolder);
			}

			//Get all files from source directory
			String files[] = sourceFolder.list();

			//Iterate over all files and copy them to destinationFolder one by one
			for (String file : files) 
			{
				File srcFile = new File(sourceFolder, file);
				File destFile = new File(destinationFolder, file);

				//Recursive function call
				copyFolder(srcFile, destFile);
			}
		}
		else
		{
			//Copy the file content from one place to another 
			Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
			//			System.out.println("File copied :: " + destinationFolder);
		}
	}
	public Node createStartEndNode(String nodeName) {
		String[]temp = new String[0];
		Transaction transaction = tempGraphDatabaseService.beginTx();
		Node service = tempGraphDatabaseService.createNode();
		try{
			Label nodeLable = DynamicLabel.label(nodeName);
			service.addLabel(nodeLable);
			service.setProperty("name", nodeName);
			tempServices.add(service, "name", service.getProperty("name"));
			if(nodeName.equals("start")){
				service.setProperty("inputs", temp);
				service.setProperty("outputs", temp);
				service.setProperty("priousNodeNames", temp);
				service.setProperty("id", (long)0);
			}
			else if(nodeName.equals("end")){
				service.setProperty("id", (long)serviceNodes.size()+1);
				service.setProperty("inputs", temp);
				service.setProperty("outputs", temp);
				service.setProperty("priousNodeNames", temp);
			}
			service.setProperty("weightTime", 0);
			service.setProperty("weightCost", 0);
			service.setProperty("weightAvailibility", 0);
			service.setProperty("weightReliability", 0);
			service.setProperty("inputServices", temp);
			service.setProperty("outputServices", temp);

			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Runtask createStartEndNode error.."); 
		} finally {
			transaction.close();
		}				
		return service;
	}
	public void createRel(Node node) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
		//		double sNodeWeight = (double) sNode.getProperty("weight");
		try{
			String nodeString = (String) node.getProperty("name");
			if(nodeString.equals("start")){
				String[] outputs = getNodePropertyArray(node, "outputServices");
				if(outputs.length>0){
					for(String s: outputs){
						Node outputsServicesNode = neo4jServNodes.get(s);
						
						String[] tempToArray = getOutputs(node, outputsServicesNode, tempGraphDatabaseService);
						relation = node.createRelationshipTo(outputsServicesNode, RelTypes.IN);
						relation.setProperty("From", nodeString);
						relation.setProperty("To", s);
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");    
						relation.setProperty("weightTime", 0);
						relation.setProperty("weightCost", 0);
						relation.setProperty("weightAvailibility", 0);
						relation.setProperty("weightReliability", 0);
					}
				}
			}
			else if(nodeString.equals("end")){
				String[] inputs = getNodePropertyArray(node, "inputServices");
				if(inputs.length>0){
					for(String s: inputs){
						Node inputsServicesNode = neo4jServNodes.get(s);
						
						String[] tempToArray = getOutputs(inputsServicesNode,node, tempGraphDatabaseService);
						relation = inputsServicesNode.createRelationshipTo(node, RelTypes.IN);
						relation.setProperty("From", s);
						relation.setProperty("To", nodeString);
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");    
					}
				}
			}
		
			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Runtask createTempDb error.."); 
		} finally {
			transaction.close();
		}	
	}
	private String[] getNodePropertyArray(Node sNode, String property){
		Object obj =sNode.getProperty(property);
		//    		//remove the "[" and "]" from string
		String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
		String[] tempInputs = ips.split("\\s*,\\s*");
		String[] array = new String[0];
		for(String s: tempInputs){
			if(s.length()>0){
				array =increaseArray(array);
				array[array.length-1] = s;
			}
		}
		return array;
	}
	private String[] getOutputs(Node node, Node sNode,GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		List<String>snodeOutputs = new ArrayList<String>();
		List<String>nodeInputs = new ArrayList<String>();
		try{
			snodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs"));
			nodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs"));
			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Runtask getOutputs error.."); 
		} finally {
			transaction.close();
		}	
		List<String>snodeOutputsAllParents = new ArrayList<String>();
		for(String output: snodeOutputs){
			TaxonomyNode tNode = taxonomyMap.get(output);
			snodeOutputsAllParents.addAll(getTNodeParentsString(tNode));
		}
		List<String>temp = new ArrayList<String>(snodeOutputsAllParents);
		temp.retainAll(nodeInputs);
		String[] tempToArray = new String[temp.size()];
		for(int i = 0; i<temp.size(); i++){
			tempToArray[i] = temp.get(i);
		}

		return tempToArray;
	}
	private Set<String> getTNodeParentsString(TaxonomyNode tNode) {
		Set<String>tNodeParents = new HashSet<String>();
		for(String t: tNode.parentsString){
			TaxonomyNode taxNode = taxonomyMap.get(t);
			if(!taxNode.isInstance){
				Set<TaxonomyNode> taxNodeInstanceChildren = taxNode.childrenInstance;
				for(TaxonomyNode childInstance: taxNodeInstanceChildren){
					tNodeParents.add(childInstance.value);
				}
			}
			else{
				tNodeParents.add(t);
			}
		}
		return tNodeParents;
	}
	public String[] increaseArray(String[] theArray)
	{
		int i = theArray.length;
		int n = ++i;
		String[] newArray = new String[n];
		if(theArray.length==0){
			return new String[1];
		}
		else{
			for(int cnt=0;cnt<theArray.length;cnt++)
			{
				newArray[cnt] = theArray[cnt];
			}
		}

		return newArray;
	}
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	public GraphDatabaseService getTempGraphDatabaseService() {
		return tempGraphDatabaseService;
	}
	public Index<Node> getTempServices() {
		return tempServices;
	}
	public Map<String, Node> getNeo4jServNodes() {
		return neo4jServNodes;
	}
}
