package generateDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

import component.ServiceNode;
import component.TaxonomyNode;

public class GenerateDatabase {
	public GraphDatabaseService graphDatabaseService;
	private String databasePath;
	Node[] neo4jServiceNodes;
	public IndexManager index = null;
	public Index<Node> services = null;
	public static Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;
	public long startTime = 0;
	public long endTime = 0;
	public Map<String,List<String>> servicesWithOutputs = new HashMap<String,List<String>>();
	public Map<String,List<String>> servicesWithInputs = new HashMap<String,List<String>>();
	public Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	public Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	Relationship relation;

	
	public GenerateDatabase(String databasePath ){
		this.databasePath = databasePath;
	}

	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap) {
		this.taxonomyMap = taxonomyMap;
	}

	public void setServiceMap(Map<String, ServiceNode> serviceMap) {
		this.serviceMap = serviceMap;
	}

	public void createDbService() {
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(databasePath);
		
	}
	public void addServiceNodeRelationShip() {
		Map<String, Object> maps = new HashMap<String, Object>();
		Map<String,List<String>> inputServices = new HashMap<String,List<String>>();
//		Map<String,List<String>> serviceOutputs = new HashMap<String,List<String>>();
		for(Node sNode: neo4jServiceNodes){
			addInputsServiceRelationship(sNode, maps, inputServices);
		}
		servicesWithInputs = inputServices;
//		servicesWithOutputs = serviceOutputs;
	}

	private void addInputsServiceRelationship(Node sNode, Map<String, Object>maps, Map<String, List<String>> inputServices) {
		Transaction transaction = graphDatabaseService.beginTx();
		//		double sNodeWeight = (double) sNode.getProperty("weight");
		String[] inputs = getNodePropertyArray(sNode, "inputServices");
		//		List<Node>inputsServicesNodes = new ArrayList<Node>();
		if(inputs.length>0){
			for(String s: inputs){
				Node inputsServicesNode = neo4jServNodes.get(s);
				String[] tempToArray = getOutputs(inputsServicesNode, sNode, graphDatabaseService);
				relation = inputsServicesNode.createRelationshipTo(sNode, RelTypes.IN);
				relation.setProperty("From", s);
				relation.setProperty("To", (String)sNode.getProperty("name"));
				relation.setProperty("outputs", tempToArray);
				relation.setProperty("Direction", "incoming");    
			}
		}
		inputServices.put((String) sNode.getProperty("name"), Arrays.asList(inputs));
		transaction.success();
		transaction.finish();
		transaction.close();
	}
	private String[] getOutputs(Node node, Node sNode,GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		List<String>snodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs"));
		List<String>nodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs"));
		transaction.success();
		transaction.finish();
		transaction.close();
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
	public void createServicesDatabase(String path){
		Transaction transaction = graphDatabaseService.beginTx();
		neo4jServiceNodes = new Node[0];
		try{
			index = graphDatabaseService.index();
			services = index.forNodes( "identifiers" );
			for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
				String key = entry.getKey();
				ServiceNode value = entry.getValue();
				//double weight = calculateWeight(value.getQos());
				double weight = 0;
				Node service = graphDatabaseService.createNode();
				String [] priousNodeNames = new String[0];
				Label nodeLable = DynamicLabel.label(key);
				service.addLabel(nodeLable);
				service.setProperty("name", key);
				service.setProperty("id", service.getId());
				services.add(service, "name", service.getProperty("name"));
				service.setProperty("qos", value.getQos());
				service.setProperty("weight", weight);
				service.setProperty("inputs", value.getInputsArray());
				service.setProperty("outputs", value.getOutputsArray());
				service.setProperty("inputServices", value.getInputsServiceArray());
				service.setProperty("outputServices", value.getOutputsServiceArray());
				service.setProperty("priousNodeNames", priousNodeNames);
				service.setProperty("visited", false);
				neo4jServiceNodes = increaseNodeArray(neo4jServiceNodes);
				neo4jServiceNodes[neo4jServiceNodes.length-1] =service;
				neo4jServNodes.put(entry.getKey(), service);
			}
			System.out.println("web service nodes created");			
			transaction.success();
		}finally{
			transaction.finish();
		}
	}
	private Node[] increaseNodeArray(Node[] theArray)
	{
		int i = theArray.length;
		int n = ++i;
		Node[] newArray = new Node[n];
		for(int cnt=0;cnt<theArray.length;cnt++) 
		{
			newArray[cnt] = theArray[cnt];
		}
		return newArray;
	}
	public Map<String, Node> getNeo4jServNodes(){
		return neo4jServNodes;
	}
	public GraphDatabaseService getGraphDatabaseService(){
		return graphDatabaseService;
	}
	public IndexManager getIndex() {
		return index;
	}

	public Index<Node> getServices() {
		return services;
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
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
}