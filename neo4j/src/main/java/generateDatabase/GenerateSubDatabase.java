package generateDatabase;

import java.util.ArrayList;
import java.util.Arrays;
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

import component.ServiceNode;
import component.TaxonomyNode;

public class GenerateSubDatabase {
	private static final int TIME = 0;
	private static final int COST = 1;
	private static final int AVAILABILITY = 2;
	private static final int RELIABILITY = 3;
	private GraphDatabaseService graphDatabaseService;
	private GraphDatabaseService newGraphDatabaseService;
	private  Map<String, TaxonomyNode> taxonomyMap = null;
	private Map<String, ServiceNode> serviceMap = null;;
	private List<Node> nodes;
	private List<Node> newNodes;
	private enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	private IndexManager index = null;
	private Index<Node> services = null;
	private String newDBPath = "database/result/";
	Relationship relation;
	public GenerateSubDatabase(List<Node> nodes, GraphDatabaseService graphDatabaseService, String path) {
		this.graphDatabaseService = graphDatabaseService;
		this.nodes = nodes;
		this.newDBPath = path;
		this.newNodes = new ArrayList<Node>();
	}

	public void run() {
		createNewDbService();
		newNodes = addNodesToNewDBService(nodes,newGraphDatabaseService, graphDatabaseService );
		createRel(newNodes);	
	
	}

	@SuppressWarnings("deprecation")
	public void createNewDbService() {
		newGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(newDBPath);
		Transaction Transaction = newGraphDatabaseService.beginTx();
		index = newGraphDatabaseService.index();
		services = index.forNodes( "identifiers" );
		Transaction.success();
		Transaction.close();
	}
	//	public void createDbForBestResult(List<Node> result) {
	//		try {
	//			FileUtils.deleteRecursively(new File(newDBPath));
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		addNodesToNewDBService(result,newGraphDatabaseService, graphDatabaseService );
	//		createResultRel(graphDatabaseService);	
	//		 
	//
	////			dbService.shutdown();
	//		
	//		//		shutDownCandidateDbService();
	//
	//	}

	//	private void removeCandidateDBNodes() {
	//		Transaction transaction = candidateGraphDatabaseService.beginTx();
	//		try{
	//			for( Node n:GlobalGraphOperations.at(candidateGraphDatabaseService).getAllNodes()){
	//				for(Relationship r: n.getRelationships()){
	//					r.delete();
	//				}
	//				n.delete();
	//			}			
	//			transaction.success();			
	//		} catch (Exception e) {
	//			System.out.println(e);
	//			System.out.println("remove candidatedb nodes error.."); 
	//		} finally {
	//			transaction.close();
	//		}	
	//
	//	}



	public void createRel(List<Node>newNodes){
		for(Node sNode: newNodes){
			Transaction tx = newGraphDatabaseService.beginTx();
			String nodeName = (String) sNode.getProperty("name");
			tx.close();

			if(nodeName.equals("start")){
				addStartNodeRel(sNode);				
			}
			else {
				addInputsServiceRelationship(sNode);		
			}

		}
	}
	@SuppressWarnings("deprecation")
	private Node getNodeByString(String name, GraphDatabaseService newGraphDatabaseService){
		Transaction transaction = newGraphDatabaseService.beginTx();
		Iterable<Node> nodeList = newGraphDatabaseService.getAllNodes();
		for( Node n:nodeList){
			if(n.getProperty("name").equals(name)){
				return n;
			}
		}
		transaction.close();
		return null;
	}

	private void addInputsServiceRelationship(Node sNode) {
		String[] inputs = getNodePropertyArray(sNode, "inputServices",newGraphDatabaseService );
		//		List<Node>inputsServicesNodes = new ArrayList<Node>();、
		if(inputs.length>0){
			for(String s: inputs){
				ServiceNode serviceNode = serviceMap.get(s);
				double[]qos = serviceNode.getQos();
				Node inputsServicesNode = getNodeByString(s,newGraphDatabaseService);
				if(inputsServicesNode!=null){
					Transaction transaction = newGraphDatabaseService.beginTx();
					//		double sNodeWeight = (double) sNode.getProperty("weight");
					try{
						String[] tempToArray = getOutputs(inputsServicesNode, sNode, newGraphDatabaseService);
						relation = inputsServicesNode.createRelationshipTo(sNode, RelTypes.IN);
						relation.setProperty("From", s);
						relation.setProperty("To", (String)sNode.getProperty("name"));
						relation.setProperty("weightTime", qos[TIME]);
						relation.setProperty("weightCost", qos[COST]);
						relation.setProperty("weightAvailibility", qos[AVAILABILITY]);
						relation.setProperty("weightReliability", qos[RELIABILITY]);
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");    
						transaction.success();			
					} catch (Exception e) {
						System.out.println(e);
						System.out.println("GenerateDatabase add result nodes InputsServiceRelationship error.."); 
					} finally {
						transaction.close();
					}	
				}
			}
		}
	}

	private void addStartNodeRel(Node sNode){

		String[] outputs = getNodePropertyArray(sNode, "outputServices",newGraphDatabaseService);
		if(outputs.length>0){
			for(String s: outputs){
				Node outputsServicesNode = getNodeByString(s, newGraphDatabaseService);
				if(outputsServicesNode!=null){
					Transaction transaction = newGraphDatabaseService.beginTx();
					try{
						String[] tempToArray = getOutputs(sNode, outputsServicesNode, newGraphDatabaseService);
						relation = sNode.createRelationshipTo(outputsServicesNode, RelTypes.IN);
						relation.setProperty("From", (String)sNode.getProperty("name"));
						relation.setProperty("To", s);
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");  
						relation.setProperty("weightTime", 0);
						relation.setProperty("weightCost", 0);
						relation.setProperty("weightAvailibility", 0);
						relation.setProperty("weightReliability", 0);

						transaction.success();
					}catch(Exception e){
						e.printStackTrace();
					}finally{
						transaction.close();
					}
				}
			}
		}
	}

	private List<Node> addNodesToNewDBService(List<Node> list, GraphDatabaseService newGraphDatabaseService, GraphDatabaseService graphDatabaseService) {
		Set<Node>nodes = new HashSet<Node>(list);
		List<Node>nNodes = new ArrayList<Node>();

		Transaction transaction = graphDatabaseService.beginTx();
		for(Node sNode: nodes) {
			String[] inputServices = getInputOutputServicesForSubGraph(sNode, nodes, "inputServices",graphDatabaseService);				
			String[] outputServices = getInputOutputServicesForSubGraph(sNode, nodes,"outputServices",graphDatabaseService);
			String[] priousNodeNames = getInputOutputServicesForSubGraph(sNode, nodes,"priousNodeNames",graphDatabaseService);
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
				service.setProperty("inputs", getNodePropertyArray(sNode,"inputs",graphDatabaseService));
				service.setProperty("outputs", getNodePropertyArray(sNode,"outputs",graphDatabaseService));
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
				nNodes.add(service);
				tx.success();
			}catch(Exception e){
				System.out.println("createNewGraphDatabase:  graphDatabaseService    "+ e);
			}finally{
				tx.close();
			}

		}	
		transaction.close();
		return nNodes;
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
	private String[] getOutputs(Node node, Node sNode,GraphDatabaseService newGraphDatabaseService) {
		List<String>snodeOutputs = new ArrayList<String>();
		List<String>nodeInputs =  new ArrayList<String>();

		snodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs",newGraphDatabaseService));
		nodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs",newGraphDatabaseService));

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

	private String[] getNodePropertyArray(Node sNode, String property, GraphDatabaseService graphDatabaseService){
		Transaction transaction = graphDatabaseService.beginTx();
		Object obj =sNode.getProperty(property);
		//    		//remove the "[" and "]" from string
		String[] array = new String[0];
		try{
			String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
			String[] tempInputs = ips.split("\\s*,\\s*");

			for(String s: tempInputs){
				if(s.length()>0){
					array =increaseArray(array);
					array[array.length-1] = s;
				}
			}
		}catch(Exception e){

		}
		finally{
			transaction.close();
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
	public void setServiceMap(Map<String, ServiceNode> serviceMap) {
		this.serviceMap = serviceMap;
	}

	public GraphDatabaseService getNewGraphDatabaseService() {
		return newGraphDatabaseService;
	}


	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap) {
		this.taxonomyMap = taxonomyMap;

	}

}
