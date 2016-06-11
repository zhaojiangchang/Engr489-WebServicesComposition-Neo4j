
package org.neo4j.neo4j;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
//import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.Traversal;


@SuppressWarnings("deprecation")
public class Main2 {
	// Constants with of order of QoS attributes
	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;
	public int totalRel = 0;
	public Set<ServiceNode> relevant;
	//Key: taxonomyNode name Value: set of children
	public static Map<String, Set<TaxonomyNode>> cs = new HashMap<String,Set<TaxonomyNode>>();
	//Key: taxonomyNode name Value: set of parents
	public static Map<String, Set<TaxonomyNode>> ps = new HashMap<String,Set<TaxonomyNode>>();
	//service and list of service's outputs
	public Map<String,List<String>> servicesWithOutputs = new HashMap<String,List<String>>();
	public Map<String,List<String>> servicesWithInputs = new HashMap<String,List<String>>();
	public List<List<List<Node>>>allPaths = new ArrayList<List<List<Node>>>();
	public Set<String> taskInputs;
	public Set<String> taskOutputs;
	public static Node startNode;
	public static Node endNode;
	public static Node subStartNode;
	public static Node subEndNode;
	public Map<String,Node>subGraphNodesMap = new HashMap<String,Node>();
	public Set<Node>subGraphNodes = new HashSet<Node>();
	public Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();
	public static Set<TaxonomyNode> children;
	public static Set<TaxonomyNode> parents;
	private static String serviceFileName = "dataset/test/test_serv.xml";
	private static String taxonomyFileName = "dataset/test/test_taxonomy.xml";
	private static String taskFileName = "dataset/test/test_problem.xml";

	public static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	public static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private static final String Neo4j_testServicesDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/test_services";
	private static final String Neo4j_ServicesDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/services";
	private static final String Neo4j_subDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/sub_graph";

	//	private static final String Neo4j_TaxonomyDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/taxonomy";

	public final double minAvailability = 0.0;
	public double maxAvailability = -1.0;
	public final double minReliability = 0.0;
	public double maxReliability = -1.0;
	public double minTime = Double.MAX_VALUE;
	public double maxTime = -1.0;
	public double minCost = Double.MAX_VALUE;
	public double maxCost = -1.0;
	public double w1;
	public double w2;
	public double w3;
	public double w4;
	public boolean overlapEnabled;
	public boolean runningOwls;
	public boolean findConcepts;
	public double overlapPercentage;
	public int idealPathLength;
	public int idealNumAtomic;
	public int numNodesMutation;
	public static File histogramLogFile;
	public static Map<String,Long>records = new HashMap<String,Long>();
	public static Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;
	Node[] neo4jServiceNodes;
	//	Node[] neo4jTaxonomyNodes;
	Relationship relation;
	Iterable<Relationship> relations;

	static GraphDatabaseService graphDatabaseService;
	static GraphDatabaseService subGraphDatabaseService;

	public static IndexManager index = null;
	public static Index<Node> services = null;
	//	GraphDatabaseService graphDatabaseTaxonomy;
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	// Statistics tracking
	public static Map<String, Integer> nodeCount = new HashMap<String, Integer>();
	public static Map<String, Integer> edgeCount = new HashMap<String, Integer>();

	private static boolean runTestFiles = true;

	public static void main( String[] args ) throws IOException{
		Main2 neo4jwsc = new Main2();
		//		neo4jwsc.index = null;
		neo4jwsc.servicesWithInputs= null;
		//		neo4jwsc.graphDatabaseService = null;

		if(!runTestFiles){
			serviceFileName = "dataset/wsc2008/Set01MetaData/services.xml";
			taxonomyFileName = "dataset/wsc2008/Set01MetaData/taxonomy.xml";
			taskFileName = "dataset/wsc2008/Set01MetaData/problem.xml";
		}
		long startTime = System.currentTimeMillis();
		neo4jwsc.parseWSCTaxonomyFile(taxonomyFileName);
		long endTime = System.currentTimeMillis();
		records.put("parse Taxonomy file", endTime - startTime);
		System.out.println("parse Taxonomy file Total execution time: " + (endTime - startTime) );


		startTime = 0;
		endTime = 0;
		startTime = System.currentTimeMillis();
		neo4jwsc.parseWSCServiceFile(serviceFileName);
		endTime = System.currentTimeMillis();
		records.put("parse service file", endTime - startTime);
		System.out.println("parse servicefile Total execution time: " + (endTime - startTime) );


		startTime = 0;
		endTime = 0;
		startTime = System.currentTimeMillis();
		neo4jwsc.parseWSCTaskFile(taskFileName);
		endTime = System.currentTimeMillis();
		records.put("parse task file", endTime - startTime);
		System.out.println("parse taskfile Total execution time: " + (endTime - startTime) );

		startTime = 0;
		endTime = 0;
		startTime = System.currentTimeMillis();
		neo4jwsc.populateTaxonomyTree();
		endTime = System.currentTimeMillis();
		records.put("populate Taxonomy Tree", endTime - startTime);
		System.out.println("populate Taxonomy Tree Total execution time: " + (endTime - startTime) );

		if(runTestFiles ){
			try {
				FileUtils.deleteRecursively(new File(Neo4j_testServicesDBPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			startTime = 0;
			endTime = 0;
			startTime = System.currentTimeMillis();
			neo4jwsc.createServicesDatabase(serviceMap);
			System.out.println(serviceMap.size());
			endTime = System.currentTimeMillis();
			records.put("createServicesDatabase", endTime - startTime);
			System.out.println("createServicesDatabase Total execution time: " + (endTime - startTime) );


			startTime = 0;
			endTime = 0;
			startTime = System.currentTimeMillis();
			neo4jwsc.addServiceNodeRelationShip();
			endTime = System.currentTimeMillis();
			records.put("addServiceNodeRelationShip", endTime - startTime);
			System.out.println("addServiceNodeRelationShip Total execution time: " + (endTime - startTime) );
			startTime = 0;
			endTime = 0;

		}
		else{

			graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath);
			Transaction transaction = graphDatabaseService.beginTx();
			index = graphDatabaseService.index();
			services = index.forNodes( "identifiers" );
			transaction.success();
			transaction.close();
			signNodesToField();
		}


		neo4jwsc.runTask();

		FileWriter fw = new FileWriter("timeRecord.txt");
		for(Entry<String, Long> entry : records.entrySet()){
			fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		}
		fw.close();


		neo4jwsc.shutdown();

	}

	private static void signNodesToField() {
		Transaction transaction = graphDatabaseService.beginTx();
		Iterable<Node> nodes = graphDatabaseService.getAllNodes();

		for(Node n: nodes){
			neo4jServNodes.put((String)n.getProperty("name"), n);
			if(n.getProperty("name").equals("start")){
				startNode = n;
			}
			if(n.getProperty("name").equals("end")){
				endNode = n;
			}
			if(n.getProperty("name").equals("start")||n.getProperty("name").equals("end")){
				Iterable<Relationship> relationships = n.getRelationships();
				for(Relationship r: relationships){
					r.delete();
				}
				//					n.delete();
			}
		}
		transaction.success();
		transaction.close();
	}

	private void addServiceNodeRelationShip() {
		Map<String, Object> maps = new HashMap<String, Object>();
		Map<String,List<String>> inputServices = new HashMap<String,List<String>>();
		Map<String,List<String>> serviceOutputs = new HashMap<String,List<String>>();
		long[] ids = null;
		for(Node sNode: neo4jServiceNodes){

			addInputsServiceRelationship(sNode, ids, maps, inputServices);

		}
		servicesWithInputs = inputServices;
		servicesWithOutputs = serviceOutputs;


	}

	private void addInputsServiceRelationship(Node sNode, long[]ids, Map<String, Object>maps, Map<String, List<String>> inputServices) {
		Transaction transaction = graphDatabaseService.beginTx();
		//		double sNodeWeight = (double) sNode.getProperty("weight");
		String[] inputs = getNodePropertyArray(sNode, "inputServices");
		//		List<Node>inputsServicesNodes = new ArrayList<Node>();
		if(inputs.length>0){
			for(String s: inputs){
				Node inputsServicesNode = neo4jServNodes.get(s);
				String[] tempToArray = getOutputs(inputsServicesNode, sNode);
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
	private String[] getOutputs(Node node, Node sNode) {
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

	//run task
	public boolean findNonFulfillNode = false;

	private void runTask() {
		System.out.println("run task");
		removeStartNodeAndEndNodeRel(startNode, endNode);
		Transaction transaction = graphDatabaseService.beginTx();
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

		createRel(startNode);
		createRel(endNode);
		Set<Node> releatedNodes = new HashSet<Node>();
		findAllReleatedNodes(releatedNodes, false);
		createSubGraphDatabase(releatedNodes);
	}
	private void createSubGraphDatabase(Set<Node> releatedNodes) {
		try {
			FileUtils.deleteRecursively(new File(Neo4j_subDBPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		subGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_subDBPath);
		Transaction transaction = subGraphDatabaseService.beginTx();
		try{
			for(Node sNode: releatedNodes) {
				String[] inputServices = getInputOutputServicesForSubGraph(sNode, releatedNodes, "inputServices");
				String[] outputServices = getInputOutputServicesForSubGraph(sNode, releatedNodes,"outputServices");
				String[] priousNodeNames = getInputOutputServicesForSubGraph(sNode, releatedNodes,"priousNodeNames");
				if(inputServices==null){
					inputServices = new String[0];
				}
				if(outputServices==null){
					outputServices = new String[0];
				}
				if(priousNodeNames==null){
					priousNodeNames = new String[0];
				}

				Node service = subGraphDatabaseService.createNode();
				Transaction tx = graphDatabaseService.beginTx();
				Label nodeLable = DynamicLabel.label((String) sNode.getProperty("name"));
				service.addLabel(nodeLable);
				service.setProperty("name", (String) sNode.getProperty("name"));
				service.setProperty("id", service.getId());
				service.setProperty("qos", 0);
				service.setProperty("weight", 0);
				service.setProperty("inputs", getNodePropertyArray(sNode,"inputs"));
				service.setProperty("outputs", getNodePropertyArray(sNode,"outputs"));
				service.setProperty("inputServices", inputServices);
				service.setProperty("outputServices", outputServices);
				service.setProperty("priousNodeNames",priousNodeNames);
				service.setProperty("visited", false);
				subGraphNodes.add(service);
				subGraphNodesMap.put((String) sNode.getProperty("name"), service);
				tx.finish();
				tx.close();
			}	
			for(Node sNode: subGraphNodes){
				//					double sNodeWeight = (double) sNode.getProperty("weight");
				String[] inputs = getNodePropertyArray(sNode, "inputServices");
				//					List<Node>inputsServicesNodes = new ArrayList<Node>();
				if(inputs.length>0){
					for(String s: inputs){
						Node inputsServicesNode = subGraphNodesMap.get(s);
						String[] tempToArray = getOutputs(inputsServicesNode, sNode);
						relation = inputsServicesNode.createRelationshipTo(sNode, RelTypes.IN);
						relation.setProperty("From", s);
						relation.setProperty("To", (String)sNode.getProperty("name"));
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");    
					}
				}	
				if(sNode.getProperty("name").equals("end")){
					subEndNode = sNode;
				}
				if(sNode.getProperty("name").equals("start")){
					subStartNode = sNode;
					String[] outputs = getNodePropertyArray(sNode, "outputServices");
					if(outputs.length>0){
						for(String s: outputs){
							Node outputsServicesNode = subGraphNodesMap.get(s);
							String[] tempToArray = getOutputs(sNode, outputsServicesNode);
							relation = sNode.createRelationshipTo(outputsServicesNode, RelTypes.IN);
							relation.setProperty("From", (String)sNode.getProperty("name"));
							relation.setProperty("To", s);
							relation.setProperty("outputs", tempToArray);
							relation.setProperty("Direction", "incoming");    
						}
					}					
				}
			}
			transaction.success();
		}finally{
			transaction.finish();
			transaction.close();
		}
		findCompositionNodes();

	}

	private void findCompositionNodes() {
		Set<Set<Node>> populations = new HashSet<Set<Node>>();
		while(populations.size()<40){
			Set<Node>result = new HashSet<Node>();
			result.add(subEndNode);
			composition(subEndNode, result);	
			System.out.println("before remove duplicate: "+result.size());
			result = checkDuplicateNodes(result);
			System.out.println("after remove duplicate: "+result.size());
			populations.add(result);
			System.out.println();
		}
		System.out.println(populations.size());
		
		


	}

	private Set<Node> checkDuplicateNodes(Set<Node> result) {
		Transaction tx = subGraphDatabaseService.beginTx();
		Set<Node>temp = new HashSet<Node>(result);
		for(Node n : result){
			if(!n.getProperty("name").equals("start") && !n.getProperty("name").equals("end") ){
				temp.remove(n);
				boolean notFulfill = false;
				if(!fulfillSubGraphNodes(temp)){
					temp.add(n);
				}
			}
		}
		System.out.println();
		for(Node n: temp){
			System.out.print(n.getProperty("name")+"  ");
		}
		System.out.println();
		tx.close();

		return temp;


	}

	private void composition(Node subEndNode, Set<Node> result) {
		Transaction tx = subGraphDatabaseService.beginTx();
		List<String>nodeInputs = Arrays.asList(getNodePropertyArray(subEndNode, "inputs"));
		//		List<String>relOutputs = new ArrayList<String>();
		List<Relationship>rels = new ArrayList<Relationship>();
		for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
			rels.add(r);
		}
		Set<Node> fulfillSubEndNodes = findNodesFulfillSubEndNode(nodeInputs,rels);
		if(fulfillSubEndNodes!=null){
			result.addAll(fulfillSubEndNodes);
			for (Node node: fulfillSubEndNodes){
				composition(node, result);
			}		
		}
	





		//		for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
		//			if(subEndNode.getProperty("name").equals("serv5592677")){
		//				System.out.println(r.getId());
		//			}
		//			List<String> temp = Arrays.asList(getNodeRelationshipPropertyArray(r, "outputs")); 
		//			List<String> temp2 = new ArrayList<String>(temp);
		//			List<String> temp3 = new ArrayList<String>(temp);
		//			temp2.retainAll(nodeInputs);
		//			temp3.retainAll(relOutputs);
		//			System.out.println(temp2.size()+"        "+relOutputs.size() +"     "+nodeInputs.size());
		//			if(temp2.size()>=relOutputs.size() && temp2.size()!=0){
		//				//TODO: need remove duplicate nodes
		//				relOutputs = temp2;
		//				
		//				Node node = subGraphNodesMap.get(r.getProperty("From"));
		//				System.out.println("From: "+node.getProperty("name"));
		////				subEndNode.setProperty("visited", true);
		//				result.add(node);
		////				boolean visited = (boolean) node.getProperty("visited");
		////				System.out.println(visited);
		//				if(node!=subEndNode && !node.getProperty("name").equals("start")){
		//					composition(node, result);
		//				}	
		//				if(nodeInputs.size()==relOutputs.size()){
		//					System.out.println("node: "+subEndNode.getProperty("name")+"fulfilled");
		//				}
		//			}
		//			else if(nodeInputs.size()>=relOutputs.size()+temp.size()  && temp2.size()==0){
		//				if(subEndNode.getProperty("name").equals("serv5592677")){
		//					System.out.println(r.getId()+"  =====   ");
		//				}
		//				relOutputs.addAll(temp);
		//				Node node = subGraphNodesMap.get(r.getProperty("From"));
		//				System.out.println("From------: "+node.getProperty("name"));
		////				subEndNode.setProperty("visited", true);
		//				result.add(node);
		////				boolean visited = (boolean) node.getProperty("visited");
		////				System.out.println(visited);
		//				System.out.println(node==subEndNode);
		//				if(node!=subEndNode && !node.getProperty("name").equals("start")){
		//					composition(node, result);
		//				}
		//				if(nodeInputs.size()==relOutputs.size()){
		//					System.out.println("node: "+subEndNode.getProperty("name")+"fulfilled");
		//				}
		//			}		
		//		}
		tx.success();
		tx.close();
	}

	private Set<Node> findNodesFulfillSubEndNode(List<String> nodeInputs, List<Relationship> relationships) {
		// TODO Auto-generated method stub

		int i = 100;
		while(i!=0){
			Collections.shuffle(relationships);
			Set<Node>toReturn = new HashSet<Node>();
			List<String>relOutputs = new ArrayList<String>();
			for(Relationship r: relationships){
				Node node = subGraphNodesMap.get(r.getProperty("From"));
				List<String> commonValue = Arrays.asList(getNodeRelationshipPropertyArray(r, "outputs"));
				List<String> temp = new ArrayList<String>(commonValue);
				temp.retainAll(relOutputs);
				if(temp.size()==0){
					relOutputs.addAll(commonValue);
					toReturn.add(node);
				}
				if(relOutputs.size()==nodeInputs.size()){
					return toReturn;
				}
			}
			i--;
		}





		return null;
	}

	private String[] getInputOutputServicesForSubGraph(Node sNode, Set<Node> releatedNodes, String inputOrOutput) {
		Transaction tx = graphDatabaseService.beginTx();
		List<String>releatedNodesNames = new ArrayList<String>();
		for(Node n: releatedNodes){
			releatedNodesNames.add((String)n.getProperty("name"));
		}

		String [] toReturn = null;
		if(inputOrOutput.equals("inputServices")){
			List<String>inputServicesList = Arrays.asList(getNodePropertyArray(sNode,"inputServices"));
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
			List<String>outputServicesList = Arrays.asList(getNodePropertyArray(sNode,"outputServices"));
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
			List<String>priousNodeNames = Arrays.asList(getNodePropertyArray(sNode,"priousNodeNames"));
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
		tx.close();
		return toReturn;
	}

	private void findAllReleatedNodes(Set<Node> releatedNodes, boolean b) {
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
				transaction.finish();
				transaction.close();
				return true;
			}
			transaction.finish();
			transaction.close();
			return false;
		}
		else{
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

			if(finder.findSinglePath(firstNode, secondNode)!=null){
				transaction.finish();
				transaction.close();
				return true;
			}
			transaction.finish();
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
		List<String> sNodeInputs = Arrays.asList(getNodePropertyArray(sNode,"inputs"));
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

	private void removeStartNodeAndEndNodeRel(Node startNode, Node endNode) {
		try (Transaction tx = graphDatabaseService.beginTx()) {
			for (Relationship r : startNode.getRelationships()) {
				r.delete();
			}
			for (Relationship r : endNode.getRelationships()) {
				r.delete();
			}
			tx.success();
			tx.finish();
			tx.close();
		}
	}

	private void createRel(Node node) {
		Transaction transaction = graphDatabaseService.beginTx();
		//		double sNodeWeight = (double) sNode.getProperty("weight");
		String nodeString = (String) node.getProperty("name");
		if(nodeString.equals("start")){
			String[] outputs = getNodePropertyArray(node, "outputServices");
			if(outputs.length>0){
				for(String s: outputs){
					Node outputsServicesNode = neo4jServNodes.get(s);
					String[] tempToArray = getOutputs(node, outputsServicesNode);
					relation = node.createRelationshipTo(outputsServicesNode, RelTypes.IN);
					relation.setProperty("From", nodeString);
					relation.setProperty("To", s);
					relation.setProperty("outputs", tempToArray);
					relation.setProperty("Direction", "incoming");    
				}
			}
		}
		else if(nodeString.equals("end")){
			String[] inputs = getNodePropertyArray(node, "inputServices");
			if(inputs.length>0){
				for(String s: inputs){
					Node inputsServicesNode = neo4jServNodes.get(s);
					String[] tempToArray = getOutputs(inputsServicesNode,node);
					relation = inputsServicesNode.createRelationshipTo(node, RelTypes.IN);
					relation.setProperty("From", s);
					relation.setProperty("To", nodeString);
					relation.setProperty("outputs", tempToArray);
					relation.setProperty("Direction", "incoming");    
				}
			}
		}
		transaction.success();
		transaction.finish();
		transaction.close();
	}

	private Node createNode(String nodeName) {
		String[]temp = new String[0];
		Node service = graphDatabaseService.createNode();
		Label nodeLable = DynamicLabel.label(nodeName);
		service.addLabel(nodeLable);
		service.setProperty("name", nodeName);
		services.add(service, "name", service.getProperty("name"));
		if(nodeName.equals("start")){
			service.setProperty("inputs", temp);
			service.setProperty("priousNodeNames", temp);
			service.setProperty("id", (long)0);
		}
		else if(nodeName.equals("end")){
			service.setProperty("id", (long)serviceNodes.size()+1);
			service.setProperty("outputs", temp);
			service.setProperty("priousNodeNames", temp);

		}
		service.setProperty("inputServices", temp);
		service.setProperty("outputServices", temp);

		service.setProperty("visited", false);

		return service;
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

	void shutdown(){
		graphDatabaseService.shutdown();
		System.out.println("Neo4j database is shutdown");
	}

	private void createServicesDatabase(Map<String, ServiceNode> serviceMap){
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_testServicesDBPath);
		Transaction transaction = graphDatabaseService.beginTx();
		neo4jServiceNodes = new Node[0];
		try{
			index = graphDatabaseService.index();
			services = index.forNodes( "identifiers" );

			for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {

				String key = entry.getKey();
				ServiceNode value = entry.getValue();
				//double weight = calculateWeight(value.getQos());
				double weight = value.getQos()[TIME];
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
			startNode = createNode("start");
			endNode = createNode("end");
			neo4jServNodes.put("start", startNode);
			neo4jServNodes.put("end", endNode);
			System.out.println("web service nodes created");			
			transaction.success();
		}finally{
			transaction.finish();
		}
	}

	//	private double calculateWeight(double[] qos) {
	//		double weight = 0;
	//		for(double d: qos){
	//			weight += d;
	//		}
	//		return weight;
	//	}

	/**
	 * Checks whether set of inputs can be completely satisfied by the search
	 * set, making sure to check descendants of input concepts for the subsumption.
	 *
	 * @param inputs
	 * @param searchSet
	 * @return true if search set subsumed by input set, false otherwise.
	 */
	public boolean isSubsumed(Set<String> inputs, Set<String> searchSet) {
		boolean satisfied = true;
		for (String input : inputs) {
			Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
			if (!isIntersection( searchSet, subsumed )) {
				satisfied = false;
				break;
			}
		}
		return satisfied;
	}

	/**
	 * Checks whether two sets of strings have overlapping elements
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean isIntersection( Set<String> a, Set<String> b ) {
		for ( String v1 : a ) {
			if ( b.contains( v1 ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Populates the taxonomy tree by associating services to the
	 * nodes in the tree.
	 */
	private void populateTaxonomyTree() {
		//find all parents node and children node for each taxonomy node
		findAllParentsAndChildrenTNode();
		//remove inputs and outputs duplicates for each service node
		//if service node inputs A B C and in taxonomy file A is parent and BC are the children  then remove B C
		//if service outputs D E F and in taxonomy file D E are parents and F is the child  then remove D E 
		removeInputsAndOutputsDuplicates();
		System.out.println("removeInputsAndOutputsDuplicates");

		for (ServiceNode s: serviceMap.values()) {
			addServiceToTaxonomyTree(s);
		}
		System.out.println("addServiceToTaxonomyTree");

		//after add all services to taxonomyTree, for each service node, go through each inputs instance find all output services to this service node and add to outputsServicesToThisService set.
		// go through each outputs instance find all input services to this service node and add to inputsServicesToThisService set.
		addAllInputOutputServicesToEachServiceNode();
		System.out.println("addAllInputOutputServicesToEachServiceNode");

	}
	private void findAllParentsAndChildrenTNode(){
		Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
		Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
			TaxonomyNode n = entry.getValue();
			// Also add output to all parent nodes
			queue.clear();
			queue.add( n );
			while (!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsOutput.add( current );
					for (TaxonomyNode parent : current.parents) {
						if(!parent.value.equals("")){
							n.parentsString.add(parent.value);
							current.parents_notGrandparents.add(parent);
							if(!parent.value.equals("")){
								if (!seenConceptsOutput.contains( parent )) {
									queue.add(parent);
									seenConceptsOutput.add(parent);
								}
							}
						}
					}
				}
			}

			seenConceptsOutput.clear();
			queue.clear();
			queue.add(n);
			while (!queue.isEmpty()) {
				TaxonomyNode current2 = queue.poll();
				if(!current2.value.equals("")){
					seenConceptsInput.add( current2 );
					for (TaxonomyNode child : current2.children) {
						n.childrenString.add(child.value);
						current2.children_notGrandchildren.add(child);
						if(!child.value.equals("")){
							if (!seenConceptsInput.contains( child )) {
								queue.add(child);
								seenConceptsInput.add(child);
							}
						}
					}
				}
			}
			seenConceptsInput.clear();
		}
	}
	private void removeInputsAndOutputsDuplicates() {
		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
			TaxonomyNode node = entry.getValue();
			children = new HashSet<TaxonomyNode>();
			parents = new HashSet<TaxonomyNode>();
			dfsGetChildren(node);
			dfsGetParents(node);
			cs.put(node.value, children);
			ps.put(node.value, parents);
		}
		removeChildrenInputs();
		removeParentsOutputs();

	}
	private void dfsGetChildren(TaxonomyNode root){
		if(root == null ||root.value.equals("")) return;
		//for every child
		for(TaxonomyNode n: root.children_notGrandchildren)
		{
			children.add(n);
			dfsGetChildren(n);
		}
	}

	private void dfsGetParents(TaxonomyNode root){
		if(root == null ||root.value.equals("")) return;
		//for every parents
		for(TaxonomyNode n: root.parents_notGrandparents)
		{
			parents.add(n);
			dfsGetParents(n);
		}

	}
	private void removeChildrenInputs(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			ServiceNode sNode = entry.getValue();
			Set<String> inputs = sNode.getInputs();
			Set<String> copy = new HashSet<String>(inputs);
			for(String input: inputs){
				if(!input.equals("")||input!=null){
					TaxonomyNode inputNode = taxonomyMap.get(input);
					input = inputNode.parentNode.value;
					if(cs.get(input).size()>0){
						Set<TaxonomyNode> children = cs.get(input);
						for(TaxonomyNode child:children){
							if(copy.contains(child.value) && !inputNode.value.equals(child.value)){
								copy.remove(child.value);
							}
						}
					}
				}
			}
			sNode.setInputs(copy);
		}
	}

	private void removeParentsOutputs(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			ServiceNode sNode = entry.getValue();
			Set<String> outputs = sNode.getOutputs();
			Set<String> copy = new HashSet<String>(outputs);
			for(String output: outputs){
				TaxonomyNode outputNode = taxonomyMap.get(output);
				output = outputNode.parentNode.value;
				if(cs.get(output).size()>0){
					Set<TaxonomyNode> parents = ps.get(output);
					for(TaxonomyNode parent:parents){
						String toRemve = getChildInst(parent);
						if(copy.contains(toRemve)){
							copy.remove(toRemve);
						}
					}
				}

			}
			//		outputs.clear();
			sNode.setOutputs(copy);
			//			copy.clear();
			//		sNode = null;
		}

	}
	private void addServiceToTaxonomyTree(ServiceNode s) {
		// Populate outputs
		Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();

		for (String outputVal : s.getOutputs()) {
			TaxonomyNode n = taxonomyMap.get(outputVal).parentNode;
			s.getTaxonomyOutputs().add(n);
			n.outputs.add(s.getName());
			queue.clear();
			// Also add output to all parent nodes
			queue.add( n );

			while (!queue.isEmpty()) {

				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsOutput.add( current );
					current.servicesWithOutput.add(s);
					for (TaxonomyNode parent : current.parents) {
						current.parents_notGrandparents.add(parent);
						if(!parent.value.equals("")){
							parent.outputs.add(s.getName());
							if (!seenConceptsOutput.contains( parent )) {
								queue.add(parent);
								seenConceptsOutput.add(parent);
							}
						}

					}
				}
			}
		}
		seenConceptsOutput.clear();
		// Also add output to all parent nodes
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
		for (String inputVal : s.getInputs()) {
			TaxonomyNode n = taxonomyMap.get(inputVal).parentNode;
			s.getTaxonomyInputs().add(n);
			n.inputs.add(s.getName());

			// Also add output to all parent nodes
			queue.clear();
			queue.add( n );
			while (!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsInput.add( current );
					current.servicesWithInput.put(s, s.getInputs());
					for (TaxonomyNode child : current.children) {
						current.children_notGrandchildren.add(child);
						if(!child.value.equals("")){
							child.inputs.add(s.getName());
							if (!seenConceptsInput.contains( child )) {
								queue.add(child);
								seenConceptsOutput.add(child);
							}
						}

					}
				}
			}
		}		

		seenConceptsInput.clear();
		return;
	}
	private void addAllInputOutputServicesToEachServiceNode(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			Set<String> inputs = new HashSet<String>(entry.getValue().getInputs());
			Set<String> outputs = new HashSet<String>(entry.getValue().getOutputs());
			for(String serviceInput: inputs){
				TaxonomyNode tNode = taxonomyMap.get(serviceInput).parentNode;
				entry.getValue().inputsServicesToThisService.addAll(tNode.outputs);
				tNode = null;
			}
			for(String serviceOutput: outputs){
				TaxonomyNode tNode = taxonomyMap.get(serviceOutput).parentNode;
				entry.getValue().outputsServicesToThisService.addAll(tNode.inputs);
				tNode = null;
			}
		}
	}

	/**
	 * Converts input, output, and service instance values to their corresponding
	 * ontological parent.
	 */
	//	private void findConceptsForInstances() {
	//		Set<String> temp = new HashSet<String>();
	//
	//		for (String s : taskInputs)
	//			temp.add(taxonomyMap.get(s).parents.get(0).value);
	//		taskInputs.clear();
	//		taskInputs.addAll(temp);
	//
	//		temp.clear();
	//		for (String s : taskOutputs)
	//			temp.add(taxonomyMap.get(s).parents.get(0).value);
	//		taskOutputs.clear();
	//		taskOutputs.addAll(temp);
	//
	//		for (ServiceNode s : serviceMap.values()) {
	//			temp.clear();
	//			Set<String> inputs = s.getInputs();
	//			for (String i : inputs)
	//				temp.add(taxonomyMap.get(i).parents.get(0).value);
	//			inputs.clear();
	//			inputs.addAll(temp);
	//
	//			temp.clear();
	//			Set<String> outputs = s.getOutputs();
	//			for (String o : outputs){
	//				temp.add(taxonomyMap.get(o).parents.get(0).value);
	//			}
	//
	//			outputs.clear();
	//			outputs.addAll(temp);
	//		}
	//	}



	/**
	 * This helps invoke the getRelevantSevices(Map<String,Node> serviceMap, Set<String> inputs, Set<String> outputs)
	 * method outside the GraphInitializer class, without the argument of serviceMap.
	 * @param inputs
	 * @param outputs
	 * @return a set of relevant service nodes
	 */
	/*public Set<Node> getRelevantServices(Set<String> inputs, Set<String> outputs){
		return getRelevantServices(serviceMap, inputs, outputs);
	}*/

	//	private void calculateNormalisationBounds(Set<ServiceNode> services) {
	//		for(ServiceNode service: services) {
	//			double[] qos = service.getQos();
	//
	//			// Availability
	//			double availability = qos[AVAILABILITY];
	//			if (availability > maxAvailability)
	//				maxAvailability = availability;
	//
	//			// Reliability
	//			double reliability = qos[RELIABILITY];
	//			if (reliability > maxReliability)
	//				maxReliability = reliability;
	//
	//			// Time
	//			double time = qos[TIME];
	//			if (time > maxTime)
	//				maxTime = time;
	//			if (time < minTime)
	//				minTime = time;
	//
	//			// Cost
	//			double cost = qos[COST];
	//			if (cost > maxCost)
	//				maxCost = cost;
	//			if (cost < minCost)
	//				minCost = cost;
	//		}
	//		// Adjust max. cost and max. time based on the number of services in shrunk repository
	//		maxCost *= services.size();
	//		maxTime *= services.size();
	//	}

	/*
	 * This calculates the availability and reliability of each path of the graph.
	 */

	/*public void calculateMultiplicativeNormalisationBounds(Node node, double availability, double reliability){

		for(Edge e: node.getOutgoingEdgeList()){
			Node service = e.getToNode();
			//calculate overall availability and reliability before end node
			if(!service.getName().equals("end")){
				double[] qos = service.getQos();
				double a = qos[AVAILABILITY];
				double r = qos[RELIABILITY];
				availability *= a;
				reliability *= r;
				//recursively calculate the graph overall availability and reliability
				calculateMultiplicativeNormalisationBounds(service, availability, reliability);
			}
			//update maxAvailability and maxReliability only at the end node
			else{
				if (availability > maxAvailability)
					maxAvailability = availability;
				if (reliability > maxReliability)
					maxReliability = reliability;
			}
		}
	}*/

	/**
	 * This calculates the normalisation bounds for reliability and availability of the whole graph
	 * @param services
	 */
	/*public void calculateMultiplicativeNormalisationBounds(Collection<Node> services){
		double availability = 1;
		double reliability = 1;
		for(Node service: services) {
			double[] qos = service.getQos();

			//Availability
			availability *= qos[AVAILABILITY];

			// Reliability
			reliability *= qos[RELIABILITY];
		}
		//System.out.println("maxA: "+maxAvailability+", maxR: "+maxReliability);//debug
		if (availability > maxAvailability)
			maxAvailability = availability;
		if (reliability > maxReliability)
			maxReliability = reliability;
	}
	 */
	/**
	 * Discovers all services from the provided collection whose
	 * input can be satisfied either (a) by the input provided in
	 * searchSet or (b) by the output of services whose input is
	 * satisfied by searchSet (or a combination of (a) and (b)).
	 *
	 * @param services
	 * @param searchSet
	 * @return set of discovered services
	 */
	//	private Set<ServiceNode> discoverService(Collection<ServiceNode> services, Set<String> searchSet) {
	//		Set<ServiceNode> found = new HashSet<ServiceNode>();
	//		for (ServiceNode s: services) {
	//			if (isSubsumed(s.getInputs(), searchSet))
	//				found.add(s);
	//		}
	//		return found;
	//	}

	/**
	 * Parses the WSC Web service file with the given name, creating Web
	 * services based on this information and saving them to the service map.
	 *
	 * @param fileName
	 */
	private void parseWSCServiceFile(String fileName) {

		double[] qos = new double[4];

		try {
			File fXmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			NodeList nList = doc.getElementsByTagName("service");
			for (int i = 0; i < nList.getLength(); i++) {
				Set<String> inputs = new HashSet<String>();
				Set<String> outputs = new HashSet<String>();
				org.w3c.dom.Node nNode = nList.item(i);
				Element eElement = (Element) nNode;

				String name = eElement.getAttribute("name");
				//				if (!runningOwls) {
				//					qos[TIME] = Double.valueOf(eElement.getAttribute("Res"));
				//					qos[COST] = Double.valueOf(eElement.getAttribute("Pri"));
				//					qos[AVAILABILITY] = Double.valueOf(eElement.getAttribute("Ava"));
				//					qos[RELIABILITY] = Double.valueOf(eElement.getAttribute("Rel"));
				//				}

				// Get inputs
				org.w3c.dom.Node inputNode = eElement.getElementsByTagName("inputs").item(0);
				NodeList inputNodes = ((Element)inputNode).getElementsByTagName("instance");
				for (int j = 0; j < inputNodes.getLength(); j++) {
					org.w3c.dom.Node in = inputNodes.item(j);
					Element e = (Element) in;
					String input = e.getAttribute("name");

					inputs.add(input);
				}


				// Get outputs
				org.w3c.dom.Node outputNode = eElement.getElementsByTagName("outputs").item(0);
				NodeList outputNodes = ((Element)outputNode).getElementsByTagName("instance");
				for (int j = 0; j < outputNodes.getLength(); j++) {
					org.w3c.dom.Node out = outputNodes.item(j);
					Element e = (Element) out;
					outputs.add(e.getAttribute("name"));
				}
				ServiceNode ws = new ServiceNode(name, qos, inputs, outputs, (long)i);
				serviceNodes.add(ws);
				serviceMap.put(name, ws);
				qos = new double[4];
				ws = null;
			}

			//			calculateNormalisationBounds(serviceNodes);
			nList = null;
		}
		catch(IOException ioe) {
			System.out.println("Service file parsing failed...");
		}
		catch (ParserConfigurationException e) {
			System.out.println("Service file parsing failed...");
		}
		catch (SAXException e) {
			System.out.println("Service file parsing failed...");
		}
	}

	private String getChildInst(TaxonomyNode parent) {
		for(TaxonomyNode tNode: parent.children_notGrandchildren){
			if(tNode.childNode==null){
				return tNode.value;
			}
		}
		return null;
	}
	/**
	 * Parses the WSC task file with the given name, extracting input and
	 * output values to be used as the composition task.
	 *
	 * @param fileName
	 */
	private void parseWSCTaskFile(String fileName) {
		try {
			File fXmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			org.w3c.dom.Node provided = doc.getElementsByTagName("provided").item(0);
			NodeList providedList = ((Element) provided).getElementsByTagName("instance");
			taskInputs = new HashSet<String>();
			for (int i = 0; i < providedList.getLength(); i++) {
				org.w3c.dom.Node item = providedList.item(i);
				Element e = (Element) item;
				taskInputs.add(e.getAttribute("name"));
			}

			org.w3c.dom.Node wanted = doc.getElementsByTagName("wanted").item(0);
			NodeList wantedList = ((Element) wanted).getElementsByTagName("instance");
			taskOutputs = new HashSet<String>();
			for (int i = 0; i < wantedList.getLength(); i++) {
				org.w3c.dom.Node item = wantedList.item(i);
				Element e = (Element) item;
				taskOutputs.add(e.getAttribute("name"));
			}
		}
		catch (ParserConfigurationException e) {
			System.out.println("Task file parsing failed...");
			e.printStackTrace();
		}
		catch (SAXException e) {
			System.out.println("Task file parsing failed...");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("Task file parsing failed...");
			e.printStackTrace();
		}
	}

	/**
	 * Parses the WSC taxonomy file with the given name, building a
	 * tree-like structure.
	 *
	 * @param fileName
	 */
	private void parseWSCTaxonomyFile(String fileName) {
		try {
			File fXmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			NodeList taxonomyRoots = doc.getChildNodes();

			processTaxonomyChildren(null, taxonomyRoots);
			dbFactory = null;
			dBuilder = null;
			doc = null;
			taxonomyRoots = null;

		}

		catch (ParserConfigurationException e) {
			System.err.println("Taxonomy file parsing failed...");
		}
		catch (SAXException e) {
			System.err.println("Taxonomy file parsing failed...");
		}
		catch (IOException e) {
			System.err.println("Taxonomy file parsing failed...");
		}
	}

	/**
	 * Recursive function for recreating taxonomy structure from file.
	 *
	 * @param parent - Nodes' parent
	 * @param nodes
	 */
	private void processTaxonomyChildren(TaxonomyNode parent, NodeList nodes) {
		if (nodes != null && nodes.getLength() != 0) {
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Node ch = nodes.item(i);

				if (!(ch instanceof Text)) {
					Element currNode = (Element) nodes.item(i);
					String value = currNode.getAttribute("name");
					TaxonomyNode taxNode = taxonomyMap.get( value );
					if (taxNode == null) {
						taxNode = new TaxonomyNode(value, currNode.getTagName());
						taxonomyMap.put( value, taxNode );
					}
					value =null;
					if (parent != null) {
						taxNode.setParentNode(parent);
						if(taxNode.isInstance){
							parent.childrenInstance.add(taxNode);
						}
						parent.setChildNode(taxNode);
						taxNode.parents.add(parent);
						//						taxNode.addParent(parent.value);
						parent.children.add(taxNode);
						//						parent.addChild(taxNode.value);
					}
					NodeList children = currNode.getChildNodes();
					processTaxonomyChildren(taxNode, children);
				}
			}
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
	private String[] getNodeRelationshipPropertyArray(Relationship relationship, String property){
		Object obj =relationship.getProperty(property);
		//    		//remove the "[" and "]" from string
		String string = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
		String[] tempOutputs = string.split("\\s*,\\s*");
		String[] array = new String[0];
		for(String s: tempOutputs){
			if(s.length()>0){
				array =increaseArray(array);
				array[array.length-1] = s;
			}
		}
		return array;
	}

	private boolean fulfillSubGraphNodes(Set<Node> releatedNodes) {
		Transaction transaction = subGraphDatabaseService.beginTx();
		boolean fulfill = false;
		for(Node sNode: releatedNodes){
			if(sNode.getProperty("name").equals("start")){
				Set<Node>releatedToStartNodes = new HashSet<Node>();
				List<String> sNodeOutputs = Arrays.asList(getNodePropertyArray(sNode,"outputs"));
				for(Relationship r: sNode.getRelationships(Direction.OUTGOING)){
					String to = (String) r.getProperty("To");
					Node toNode = subGraphNodesMap.get(to);

					if(releatedNodes.contains(toNode)){
						releatedToStartNodes.add(toNode);
					}
				}
				if(!fulfillStartNode(sNodeOutputs, releatedToStartNodes)){
					return false;			
				}
			}
			else{
				List<String> sNodeInputs = Arrays.asList(getNodePropertyArray(sNode,"inputs"));
				Set<String> inputs = new HashSet<String>();
				for(Relationship r: sNode.getRelationships(Direction.INCOMING)){
					String from = (String) r.getProperty("From");
					
					Node fromNode = subGraphNodesMap.get(from);
					if(releatedNodes.contains(fromNode)){
						inputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r,"outputs")));
					}

					
				}
				if(inputs.size()!=sNodeInputs.size()){
					return false;
				}
				if(inputs.size()==0){
					return false;
				}
			}

		}

		transaction.finish();
		transaction.close();
		return true;
	}
	private boolean fulfillStartNode(List<String> sNodeOutputs, Set<Node> releatedToStartNodes) {
		Map<String, Set<String>>startNodeOutputAndParents = new HashMap<String, Set<String>>();
		Set<String> matched = new HashSet<String>();
		for(String output: sNodeOutputs){
			TaxonomyNode tNode = taxonomyMap.get(output);
			Set<String>tNodeParents = new HashSet<String>();
			tNodeParents = getTNodeParentsString(tNode);
			startNodeOutputAndParents.put(tNode.value ,tNodeParents);
			Set<String> relNodeInputs = new HashSet<String>();
			for(Node sNode: releatedToStartNodes){
				Set<String> temp = new HashSet<String>(tNodeParents);
				relNodeInputs.addAll(Arrays.asList(getNodePropertyArray(sNode,"inputs")));
				temp.retainAll(relNodeInputs);
				if(temp.size()>0){
					matched.addAll(temp);
					if(matched.size()==sNodeOutputs.size()){
						return true;
					}
				}
			}

		}
		return false;
	}
}
