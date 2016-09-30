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
	private GraphDatabaseService newGraphDatabaseService;
	private GraphDatabaseService oldGraphDatabaseService;	
	private List<Node> graphNodes = new ArrayList<Node>();

	private String databasePath;
	private Node[] neo4jServiceNodes;
	private IndexManager index = null;
	public Index<Node> services = null;
	private static Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;
	@SuppressWarnings("unused")
	private long startTime = 0;
	@SuppressWarnings("unused")
	private long endTime = 0;
	@SuppressWarnings("unused")
	private Map<String,List<String>> servicesWithOutputs = new HashMap<String,List<String>>();
	@SuppressWarnings("unused")
	private Map<String,List<String>> servicesWithInputs = new HashMap<String,List<String>>();
	private Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	Relationship relation;
	private List<Map<String, String>> bestRels = new ArrayList<Map<String,String>>();
	private static final int TIME = 0;
	private static final int COST = 1;
	private static final int AVAILABILITY = 2;
	private static final int RELIABILITY = 3;

	public GenerateDatabase(List<Node> graphNodes, GraphDatabaseService oldGraphDatabaseService, String databasePath){
		this.databasePath = databasePath;
		this.graphNodes = graphNodes;
		this.oldGraphDatabaseService = oldGraphDatabaseService;	}

	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap) {
		this.taxonomyMap = taxonomyMap;
	}

	public void setServiceMap(Map<String, ServiceNode> serviceMap) {
		this.serviceMap = serviceMap;
	}

	@SuppressWarnings("deprecation")
	public void createDbService() {
		newGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(databasePath);

	}
	public void addServiceNodeRelationShip() {
//		System.out.println(bestRels.size());
//		if(bestRels.size()==0){
			Map<String, Object> maps = new HashMap<String, Object>();
			Map<String,List<String>> inputServices = new HashMap<String,List<String>>();
			//		Map<String,List<String>> serviceOutputs = new HashMap<String,List<String>>();
			for(Node sNode: neo4jServiceNodes){
				Transaction transaction = newGraphDatabaseService.beginTx();
				transaction.close();
				addInputsServiceRelationship(sNode, maps, inputServices);
			}
			servicesWithInputs = inputServices;

			//		servicesWithOutputs = serviceOutputs;
//		}
//		else{
//			System.out.println(neo4jServNodes.size());
//			for(String node: neo4jServNodes.keySet()){
//				System.out.println(node);
//			}
//			System.out.println();
//			for(Map<String,String> rels: bestRels){
//				for(Map.Entry<String, String> rel: rels.entrySet()){
//					String from = rel.getKey();
//					String to = rel.getValue();
//					System.out.println(from+"     "+to);
//				}
//				for(Map.Entry<String, String> rel: rels.entrySet()){
//					Transaction transaction = newGraphDatabaseService.beginTx();
//					try{
//						String from = rel.getKey();
//						String to = rel.getValue();
//						System.out.println(from+"     "+to);
//						Node fromNode = neo4jServNodes.get(from);
//						Node toNode = neo4jServNodes.get(to);
//						System.out.println(fromNode.getId()+"     "+toNode.getId());
//						ServiceNode serviceNode = serviceMap.get(from);
//						String[] tempToArray = getOutputs(fromNode, toNode, newGraphDatabaseService);
//						relation = fromNode.createRelationshipTo(toNode, RelTypes.IN);
//						relation.setProperty("From", from);
//						relation.setProperty("To", to);
//						relation.setProperty("outputs", tempToArray);
//						relation.setProperty("Direction", "incoming");    
//						if(from.equals("start")){
//							relation.setProperty("weightTime", 0);
//							relation.setProperty("weightCost", 0);
//							relation.setProperty("weightAvailibility", 0);
//							relation.setProperty("weightReliability", 0);
//						}
//						else{
//							relation.setProperty("weightTime", serviceNode.getQos()[TIME]);
//							relation.setProperty("weightCost", serviceNode.getQos()[COST]);
//							relation.setProperty("weightAvailibility", serviceNode.getQos()[AVAILABILITY]);
//							relation.setProperty("weightReliability", serviceNode.getQos()[RELIABILITY]);
//						}
//						transaction.success();
//					}catch(Exception e){
//						System.out.println(e);
//						System.out.println("GenerateDatabase addServiceNodeRelationShip error.."); 
//					}
//					finally{
//						transaction.close();
//					}
//				}
//
//			}
//		}
	}


	private void addInputsServiceRelationship(Node sNode, Map<String, Object>maps, Map<String, List<String>> inputServices) {
		Transaction transaction = newGraphDatabaseService.beginTx();
		//		double sNodeWeight = (double) sNode.getProperty("weight");
		try{
			String[] inputs;
			if(sNode.getProperty("name").equals("start")){
				inputs = getNodePropertyArray(sNode, "outputServices");
			}
			else
				inputs = getNodePropertyArray(sNode, "inputServices");
			//		List<Node>inputsServicesNodes = new ArrayList<Node>();
			if(inputs.length>0){
				for(String s: inputs){
					ServiceNode serviceNode = serviceMap.get(s);
					Node inputsServicesNode = neo4jServNodes.get(s);
					if(sNode.getProperty("name").equals("start")){
						String[] tempToArray = getOutputs(sNode, inputsServicesNode, newGraphDatabaseService);
						relation = sNode.createRelationshipTo(inputsServicesNode, RelTypes.IN);
						relation.setProperty("From", (String)sNode.getProperty("name"));
						relation.setProperty("To", s);
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");  
						relation.setProperty("weightTime", 0);
						relation.setProperty("weightCost", 0);
						relation.setProperty("weightAvailibility", 0);
						relation.setProperty("weightReliability", 0);
						relation.setProperty("removeable", false);
					}
					else{
						if(!inputsServicesNode.getProperty("name").equals(sNode.getProperty("name"))){
							String[] tempToArray = getOutputs(inputsServicesNode, sNode, newGraphDatabaseService);
							relation = inputsServicesNode.createRelationshipTo(sNode, RelTypes.IN);
							relation.setProperty("From", s);
							relation.setProperty("To", (String)sNode.getProperty("name"));
							relation.setProperty("outputs", tempToArray);
							relation.setProperty("Direction", "incoming");    
							relation.setProperty("weightTime", serviceNode.getQos()[TIME]);
							relation.setProperty("weightCost", serviceNode.getQos()[COST]);
							relation.setProperty("weightAvailibility", serviceNode.getQos()[AVAILABILITY]);
							relation.setProperty("weightReliability", serviceNode.getQos()[RELIABILITY]);
							relation.setProperty("removeable", false);

						}
					}
				}
			}
			inputServices.put((String) sNode.getProperty("name"), Arrays.asList(inputs));
			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("GenerateDatabase addInputsServiceRelationship error.."); 
		} finally {
			transaction.close();
		}		
	}
	private String[] getOutputs(Node node, Node sNode,GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		List<String>snodeOutputs = new ArrayList<String>();
		List<String>nodeInputs =  new ArrayList<String>();
		try{
			snodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs"));
			nodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs"));
			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("GenerateDatabase getOutputs error.."); 
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
	public void createServicesDatabase(){
		if(graphNodes==null){

			Transaction transaction = newGraphDatabaseService.beginTx();
			neo4jServiceNodes = new Node[0];
			try{
				index = newGraphDatabaseService.index();
				services = index.forNodes( "identifiers" );
				int i = 0;
				for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
					i++;
					String key = entry.getKey();
					ServiceNode value = entry.getValue();
					//double weight = calculateWeight(value.getQos());
					double weight = 0;
					Node service = newGraphDatabaseService.createNode();
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
					neo4jServiceNodes = increaseNodeArray(neo4jServiceNodes);
					neo4jServiceNodes[neo4jServiceNodes.length-1] =service;
					neo4jServNodes.put(entry.getKey(), service);
				}
				System.out.println("web service nodes created:  "+ i+" nodes;");			
				transaction.success();
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("GenerateDatabase createServicesDatabase error.."); 
			} finally {
				transaction.close();
			}		
		}
		//create nodes from neo4j nodes
		else{
			List<Node>nNodes = new ArrayList<Node>();
			neo4jServiceNodes = new Node[0];
			Transaction transaction = oldGraphDatabaseService.beginTx();
			for(Node sNode: graphNodes) {

				String[] inputServices = getInputOutputServicesForSubGraph(sNode, graphNodes, "inputServices",oldGraphDatabaseService);				
				String[] outputServices = getInputOutputServicesForSubGraph(sNode, graphNodes,"outputServices",oldGraphDatabaseService);
				String[] priousNodeNames = getInputOutputServicesForSubGraph(sNode, graphNodes,"priousNodeNames",oldGraphDatabaseService);
				if(inputServices==null){
					inputServices = new String[0];
				}
				if(outputServices==null){
					outputServices = new String[0];
				}
				if(priousNodeNames==null){
					priousNodeNames = new String[0];
				}
				//
				Transaction tx = newGraphDatabaseService.beginTx();
				Node service = newGraphDatabaseService.createNode();
				index = newGraphDatabaseService.index();
				services = index.forNodes( "identifiers" );
				try{
					double[] qos = new double[4];
					if(sNode.getProperty("name").equals("start")||sNode.getProperty("name").equals("end")){
						qos = new double[4];
					}else{
						ServiceNode serviceNode = serviceMap.get((String) sNode.getProperty("name"));
						qos = serviceNode.getQos();
					}

					Label nodeLable = DynamicLabel.label((String) sNode.getProperty("name"));
					service.addLabel(nodeLable);
					service.setProperty("name", (String) sNode.getProperty("name"));
					service.setProperty("id", service.getId());
					services.add(service, "name", service.getProperty("name"));
					service.setProperty("qos", qos);
					service.setProperty("weight", 0);
					service.setProperty("inputs", getNodePropertyArray(sNode,"inputs",oldGraphDatabaseService));
					service.setProperty("outputs", getNodePropertyArray(sNode,"outputs",oldGraphDatabaseService));
					service.setProperty("inputServices", inputServices);
					service.setProperty("outputServices", outputServices);
					service.setProperty("priousNodeNames",priousNodeNames);
					service.setProperty("weightTime", qos[TIME]);
					service.setProperty("weightCost", qos[COST]);
					service.setProperty("weightAvailibility", qos[AVAILABILITY]);
					service.setProperty("weightReliability", qos[RELIABILITY]);
					service.setProperty("visited", false);
					service.setProperty("totalTime", sNode.getProperty("totalTime"));
					//				candidateGraphNodesMap.put((String) sNode.getProperty("name"), service);
					neo4jServiceNodes = increaseNodeArray(neo4jServiceNodes);
					neo4jServiceNodes[neo4jServiceNodes.length-1] =service;
					neo4jServNodes.put((String) sNode.getProperty("name"), service);
					nNodes.add(service);
					tx.success();
				}catch(Exception e){
					System.out.println("createNewGraphDatabase:  graphDatabaseService    "+ e);
				}finally{
					tx.close();
				}

			}	
			transaction.close();
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
		return newGraphDatabaseService;
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
	private String[] getNodePropertyArray(Node sNode, String property,GraphDatabaseService oldGraphDatabaseService){
		Transaction transaction = oldGraphDatabaseService.beginTx();
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
		transaction.close();
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
	private String[] getInputOutputServicesForSubGraph(Node sNode, List<Node> releatedNodes, String inputOrOutput, GraphDatabaseService graphDatabaseService) {
		Transaction tx = graphDatabaseService.beginTx();
		String [] toReturn = null;
		try{
			List<String>releatedNodesNames = new ArrayList<String>();
			for(Node n: releatedNodes){
				releatedNodesNames.add((String)n.getProperty("name"));
			}

			if(inputOrOutput.equals("inputServices")){
				List<String>inputServicesList = Arrays.asList(getNodePropertyArray(sNode,"inputServices",graphDatabaseService));
				if(inputServicesList.size()>0){
					List<String>tempInputServices = new ArrayList<String>(inputServicesList);
					tempInputServices.retainAll(releatedNodesNames);
					String[] inputServices = new String[tempInputServices.size()];
					for(int i = 0; i<tempInputServices.size(); i++){
						inputServices[i] = tempInputServices.get(i);
					}
					toReturn = inputServices;
				}
			}
			else if(inputOrOutput.equals("outputServices")){
				List<String>outputServicesList = Arrays.asList(getNodePropertyArray(sNode,"outputServices",graphDatabaseService));
				if(outputServicesList.size()>0){
					List<String>tempOutputServices = new ArrayList<String>(outputServicesList);
					tempOutputServices.retainAll(releatedNodesNames);
					String[] outputServices = new String[tempOutputServices.size()];
					for(int i = 0; i<tempOutputServices.size(); i++){
						outputServices[i] = tempOutputServices.get(i);
					}
					toReturn = outputServices;
				}
			}
			else if(inputOrOutput.equals("priousNodeNames")){
				List<String>priousNodeNames = Arrays.asList(getNodePropertyArray(sNode,"priousNodeNames",graphDatabaseService));
				if(priousNodeNames.size()>0){
					List<String>tempPriousNodeNames = new ArrayList<String>(priousNodeNames);
					tempPriousNodeNames.retainAll(releatedNodesNames);
					String[] priousNodes = new String[tempPriousNodeNames.size()];
					for(int i = 0; i<tempPriousNodeNames.size(); i++){
						priousNodes[i] = tempPriousNodeNames.get(i);
					}
					toReturn = priousNodes;
				}
			}
			tx.success();
		}catch(Exception e){
			System.out.println("getInputOutputServicesForSubGraph    "+e);
		}finally{
			tx.close();
		}
		return toReturn;
	}

	public void set(List<Map<String, String>> bestRels) {
		this.bestRels  = bestRels;
	}
}
