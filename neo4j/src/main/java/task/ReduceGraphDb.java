package task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.Traversal;

import component.TaxonomyNode;

public class ReduceGraphDb {
	private Node startNode;
	private Node endNode;
	private GraphDatabaseService graphDatabaseService = null;
	private GraphDatabaseService subGraphDatabaseService = null;

	private Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;
	private Map<String,Node>subGraphNodesMap = new HashMap<String,Node>();
	private Set<Node> relatedNodes;
	private final String Neo4j_testServicesDBPath = "database/test_services";
	private final String Neo4j_subDBPath = "database/sub_graph";

	private Set<Node>subGraphNodes = new HashSet<Node>();
	Relationship relation;
	private static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();

	public ReduceGraphDb(GraphDatabaseService graphDatabaseService){
		relatedNodes = new HashSet<Node>();
		this.graphDatabaseService = graphDatabaseService;
	}

	public void setStartNode(Node startNode) {
		this.startNode = startNode;
	}
	public void setEndNode(Node endNode) {
		this.endNode = endNode;
	}
	public Node getStartNode(){
		return this.startNode;
	}
	public Node getEndNode(){
		return this.endNode;
	}
	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap){
		this.taxonomyMap = taxonomyMap;
	}

	public Map<String, Node> getSubGraphNodesMap() {
		return subGraphNodesMap;
	}
	public Set<Node> getRelatedNodes(){
		return relatedNodes;
	}
	public GraphDatabaseService getSubGraphDatabaseService(){
		return this.subGraphDatabaseService;
	}
	public void setNeo4jServNodes(Map<String, Node> neo4jServNodes){
		this.neo4jServNodes = neo4jServNodes;
	}
	public void findAllReleatedNodes(Set<Node> releatedNodes, boolean b) {
		if(!b){
			for(Entry<String, Node>entry: neo4jServNodes.entrySet()){
				Node sNode = entry.getValue();

				if(hasRel(startNode, sNode, releatedNodes) && hasRel(sNode, endNode, releatedNodes)){
					releatedNodes.add(sNode);
				}
			}
			removeNoneFulFillNodes(releatedNodes);		
		}else{
			Set<Node>temp = new HashSet<Node>(releatedNodes);
			for(Node sNode: temp){				
				if(!hasRel(startNode, sNode, temp) || !hasRel(sNode, endNode, temp)){
					releatedNodes.remove(sNode);
				}
			}
			removeNoneFulFillNodes(releatedNodes);		
		}
	}
	private boolean hasRel(Node firstNode, Node secondNode, Set<Node> releatedNodes) {
		Transaction transaction = graphDatabaseService.beginTx();
		if(releatedNodes==null){
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

			if(finder.findSinglePath(firstNode, secondNode)!=null){
				transaction.close();
				return true;
			}
			transaction.close();
			return false;
		}
		else{
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

			if(finder.findSinglePath(firstNode, secondNode)!=null){
				transaction.close();
				return true;
			}
			transaction.close();
			return false;
		}
	}

	private void removeNoneFulFillNodes(Set<Node> releatedNodes) {
		Transaction transaction = graphDatabaseService.beginTx();
		Set<Node>copied = new HashSet<Node>(releatedNodes);
		boolean removed = false;
		for(Node sNode: copied){
			if(!fulfill(sNode, copied) && !sNode.getProperty("name").equals("end")&& !sNode.getProperty("name").equals("start")){
				removed = true;
				releatedNodes.remove(sNode);
			}
		}
		if(removed){
			findAllReleatedNodes(releatedNodes, true);
		}
		transaction.finish();
		transaction.close();
	}
	private boolean fulfill(Node sNode, Set<Node> releatedNodes) {
		Transaction transaction = graphDatabaseService.beginTx();
		boolean fulfill = false;
		Set<String> inputs = new HashSet<String>();
		List<String> sNodeInputs = Arrays.asList(getNodePropertyArray(sNode,"inputs",graphDatabaseService));
		for(Relationship r: sNode.getRelationships(Direction.INCOMING)){
			String from = (String) r.getProperty("From");
			Node fromNode = neo4jServNodes.get(from);
			if(releatedNodes.contains(fromNode)){
				inputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r,"outputs")));
			}
			List<String> temp = new ArrayList<String>(sNodeInputs); 
			temp.retainAll(inputs);
			if(temp.size()==sNodeInputs.size()){
				fulfill = true;
			}
		}
		transaction.finish();
		transaction.close();
		return fulfill;
	}

	private String[] getNodePropertyArray(Node node, String property, GraphDatabaseService graphDatabaseService){
		Transaction transaction = graphDatabaseService.beginTx();
		Object obj =node.getProperty(property);
		transaction.close();
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
	private String[] getNodeRelationshipPropertyArray(Relationship relationship, String property){
		Transaction t = graphDatabaseService.beginTx();
		String[] array = new String[0];
		try{
			Object obj =relationship.getProperty(property);
			//    		//remove the "[" and "]" from string
			String string = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
			String[] tempOutputs = string.split("\\s*,\\s*");

			for(String s: tempOutputs){
				if(s.length()>0){
					array =increaseArray(array);
					array[array.length-1] = s;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("getNodeRelationshipPropertyArray error.."); 
		} finally {
			t.close();
		}		
		return array;
	}
	private String[] increaseArray(String[] theArray)
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
	public void createGraphDatabase(Set<Node>relatedNodes){
		try {
			FileUtils.deleteRecursively(new File(Neo4j_subDBPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		subGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_subDBPath);
		Transaction transaction = graphDatabaseService.beginTx();
		for(Node sNode: relatedNodes) {
			//			try{
			String[] inputServices = getInputOutputServicesForSubGraph(sNode, relatedNodes, "inputServices",graphDatabaseService);				
			String[] outputServices = getInputOutputServicesForSubGraph(sNode, relatedNodes,"outputServices",graphDatabaseService);
			String[] priousNodeNames = getInputOutputServicesForSubGraph(sNode, relatedNodes,"priousNodeNames",graphDatabaseService);
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
			Transaction tx = subGraphDatabaseService.beginTx();
			Node service = subGraphDatabaseService.createNode();
			try{
				Label nodeLable = DynamicLabel.label((String) sNode.getProperty("name"));
				service.addLabel(nodeLable);
				service.setProperty("name", (String) sNode.getProperty("name"));
				service.setProperty("id", service.getId());
				service.setProperty("qos", 0);
				service.setProperty("weight", 0);
				service.setProperty("inputs", getNodePropertyArray(sNode,"inputs",graphDatabaseService));
				service.setProperty("outputs", getNodePropertyArray(sNode,"outputs",graphDatabaseService));
				service.setProperty("inputServices", inputServices);
				service.setProperty("outputServices", outputServices);
				service.setProperty("priousNodeNames",priousNodeNames);
				service.setProperty("visited", false);
				subGraphNodes.add(service);
				subGraphNodesMap.put((String) sNode.getProperty("name"), service);
				tx.success();
			}catch(Exception e){
				System.out.println("createGraphDatabase:  subGraphDatabaseService    "+ e);
			}finally{
				tx.close();
			}

		}	
		transaction.close();
		for(Node sNode: subGraphNodes){
			//					double sNodeWeight = (double) sNode.getProperty("weight");
			String[] inputs = getNodePropertyArray(sNode, "inputServices",subGraphDatabaseService);
			//					List<Node>inputsServicesNodes = new ArrayList<Node>();
			if(inputs.length>0){
				for(String s: inputs){
					Node inputsServicesNode = subGraphNodesMap.get(s);
					String[] tempToArray = getOutputs(inputsServicesNode, sNode);
					transaction = subGraphDatabaseService.beginTx();
					try{
						relation = inputsServicesNode.createRelationshipTo(sNode, RelTypes.IN);
						relation.setProperty("From", s);
						relation.setProperty("To", (String)sNode.getProperty("name"));
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");    
						transaction.success();
					}
					catch(Exception e){
						System.out.println(e);
					}finally{
						transaction.close();
					}
				}
				//				transaction.success();
				//				transaction.close();
			}	
			transaction = subGraphDatabaseService.beginTx();
			if(sNode.getProperty("name").equals("end")){
				endNode = sNode;
			}
			transaction.close();
			transaction = subGraphDatabaseService.beginTx();
			if(sNode.getProperty("name").equals("start")){
				startNode = sNode;
				transaction.close();
				String[] outputs = getNodePropertyArray(sNode, "outputServices",subGraphDatabaseService);
				if(outputs.length>0){
					for(String s: outputs){
						transaction = subGraphDatabaseService.beginTx();
						try{
							Node outputsServicesNode = subGraphNodesMap.get(s);

							String[] tempToArray = getOutputs(sNode, outputsServicesNode);
							relation = sNode.createRelationshipTo(outputsServicesNode, RelTypes.IN);
							relation.setProperty("From", (String)sNode.getProperty("name"));
							relation.setProperty("To", s);
							relation.setProperty("outputs", tempToArray);
							relation.setProperty("Direction", "incoming");  
							transaction.success();
						}
						catch(Exception e){
							System.out.println(e);
						}finally{
							transaction.close();
						}
					}
				}					
			}
		}
	}
	private String[] getInputOutputServicesForSubGraph(Node sNode, Set<Node> releatedNodes, String inputOrOutput, GraphDatabaseService graphDatabaseService) {
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

	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	private String[] getOutputs(Node node, Node sNode) {
		//		Transaction transaction = subGraphDatabaseService.beginTx();
		//		System.out.println(node.getProperty("name")+"    "+sNode.getProperty("name"));
		List<String>snodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs",subGraphDatabaseService));
		List<String>nodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs",subGraphDatabaseService));
		//		transaction.success();
		//		transaction.close();
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
}
