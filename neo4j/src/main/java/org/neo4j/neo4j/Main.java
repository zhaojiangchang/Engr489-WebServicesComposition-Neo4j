
package org.neo4j.neo4j;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.cypher.internal.compiler.v2_2.planDescription.Children;
import org.neo4j.cypher.internal.frontend.v2_3.ast.functions.Relationships;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
//import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.Traversal;


public class Main {
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
	public ServiceNode startNode;
	public ServiceNode endNode;
	public Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();
	public static Set<TaxonomyNode> children;
	public static Set<TaxonomyNode> parents;
	private static String serviceFileName = "test_serv3.xml";
	private static String taxonomyFileName = "test_taxonomy3.xml";
	private static String taskFileName = "test_problem3.xml";

	public static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	public static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private static final String Neo4j_ServicesDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/services";
	private static final String Neo4j_TaxonomyDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/taxonomy";

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
	public Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;
	Node[] neo4jServiceNodes;
	//	Node[] neo4jTaxonomyNodes;
	Relationship relation;
	Iterable<Relationship> relations;

	GraphDatabaseService graphDatabaseService;
	public IndexManager index = null;
	public Index<Node> services = null;
	//	GraphDatabaseService graphDatabaseTaxonomy;
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	// Statistics tracking
	public static Map<String, Integer> nodeCount = new HashMap<String, Integer>();
	public static Map<String, Integer> edgeCount = new HashMap<String, Integer>();

	public static void main( String[] args ) throws IOException{
		Main neo4jwsc = new Main();
		try {
			//			FileUtils.deleteRecursively(new File(Neo4j_TaxonomyDBPath));
			FileUtils.deleteRecursively(new File(Neo4j_ServicesDBPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		System.out.println("parse servicefile Total execution time: " + (endTime - startTime) );

		startTime = 0;
		endTime = 0;
		startTime = System.currentTimeMillis();
		neo4jwsc.populateTaxonomyTree();
		endTime = System.currentTimeMillis();
		records.put("populate Taxonomy Tree", endTime - startTime);
		System.out.println("populate Taxonomy Tree Total execution time: " + (endTime - startTime) );



		//
		//		for(Entry<String, Set<TaxonomyNode>> entry : cs.entrySet()){
		//			String key = entry.getKey();
		//			System.out.println("node: " + key);
		//
		//			Set<TaxonomyNode> tNodes = entry.getValue();
		//			for(TaxonomyNode tNode: tNodes){
		//				System.out.println("children TNode: " + tNode.value);
		//			}
		//		}
		//
		//		for(Entry<String, Set<TaxonomyNode>> entry : ps.entrySet()){
		//			String key = entry.getKey();
		//			System.out.println("node: " + key);
		//
		//			Set<TaxonomyNode> tNodes = entry.getValue();
		//			for(TaxonomyNode tNode: tNodes){
		//				System.out.println("parents TNode: " + tNode.value);
		//			}
		//		}
		startTime = 0;
		endTime = 0;
		startTime = System.currentTimeMillis();
		neo4jwsc.createServicesDatabase(serviceMap);
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

		neo4jwsc.runTask();

		FileWriter fw = new FileWriter("timeRecord.txt");
		for(Entry<String, Long> entry : records.entrySet()){
			fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		}
		fw.close();


		neo4jwsc.shutdown();

	}

	private void addServiceNodeRelationShip() {
		// TODO Auto-generated method stub

		Map<String, Object> maps = new HashMap<String, Object>();
		Map<String,List<String>> serviceInputs = new HashMap<String,List<String>>();
		Map<String,List<String>> serviceOutputs = new HashMap<String,List<String>>();
		int[] ids = null;
		for(Node sNode: neo4jServiceNodes){

			addInputsServiceRelationship(sNode, ids, maps, serviceInputs);
			addOutputsServiceRelationship(sNode, ids, maps, serviceOutputs);

		}
		servicesWithInputs = serviceInputs;
		servicesWithOutputs = serviceOutputs;


	}
	private void addOutputsServiceRelationship(Node sNode, int[] ids, Map<String, Object> maps, Map<String,List<String>> serviceOutputs) {
		// TODO Auto-generated method stub
		Transaction transaction = graphDatabaseService.beginTx();

		double sNodeWeight = (double) sNode.getProperty("weight");
		String[] outputs = getNodePropertyArray(sNode, "outputServices");

		serviceOutputs.put((String) sNode.getProperty("name"), Arrays.asList(outputs));
		//		ids = null;
		//		ids = getServicesId(outputs);
		//		maps.clear();
		//		maps.put("output", ids);
		//		Iterator<Node> javaOutputNodes = null;
		//		try ( 
		//				//						Result execResult = graphDatabaseService.execute( "match (n{name: {o}}) return n", MapUtil.map("o",input)))
		//
		//				Result execResult = graphDatabaseService.execute( "MATCH (n) WHERE ID(n) IN {output} RETURN n",maps))
		//
		//		{
		//			relation = null;
		//			javaOutputNodes = null;
		//			javaOutputNodes = execResult.columnAs("n");
		//			for (Node node : IteratorUtil.asIterable(javaOutputNodes))
		//			{	   
		//				double nodeWeight = (double) node.getProperty("weight");
		//				relation = node.createRelationshipTo(sNode, RelTypes.OUT);
		//				relation.setProperty("From", (String)sNode.getProperty("name"));
		//				relation.setProperty("To", (String)node.getProperty("name"));
		//				relation.setProperty("Direction", "outgoing");    
		//				relation.setProperty("weight", (sNodeWeight+nodeWeight)/2);
		//			}
		//			javaOutputNodes = null;
		//		}
		//		outputsFromGdb =null;
		//		//    		//remove the "[" and "]" from string
		//		ops = null;
		//		outputs = null;
		//		ids = null;
		//		relation = null;
		//		//transaction close for each query - otherwise run out memory
		transaction.success();
		transaction.finish();
		transaction.close();
	}

	private void addInputsServiceRelationship(Node sNode, int[]ids, Map<String, Object>maps, Map<String, List<String>> serviceInputs) {
		// TODO Auto-generated method stub
		Transaction transaction = graphDatabaseService.beginTx();

		double sNodeWeight = (double) sNode.getProperty("weight");
		String[] inputs = getNodePropertyArray(sNode, "inputServices");
		serviceInputs.put((String) sNode.getProperty("name"), Arrays.asList(inputs));
		System.out.println(sNode.getProperty("name")+"==================");
		System.out.println(inputs.length+"  inputs length");

		for(String s: inputs){

			System.out.print(s.length()+" ==  ");
		}
		System.out.println();

		ids = null;
		ids = getServicesId(inputs);
		maps.clear();
		maps.put("input", ids);

		Iterator<Node> javaInputNodes = null;
		try ( 
				//						Result execResult = graphDatabaseService.execute( "match (n{name: {o}}) return n", MapUtil.map("o",input)))

				Result execResult = graphDatabaseService.execute( "MATCH (n) WHERE ID(n) IN {input} RETURN n",maps))

		{
			relation = null;
			javaInputNodes = null;
			javaInputNodes = execResult.columnAs("n");
			for (Node node : IteratorUtil.asIterable(javaInputNodes))
			{	   
				System.out.println(inputs.length);
				List<String>nodeInputs = Arrays.asList(getNodePropertyArray(node,"outputs"));
				List<String>sNodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs"));
				List<String>temp = new ArrayList<String>(nodeInputs);
				temp.retainAll(sNodeInputs);
				String[] tempToArray = new String[temp.size()];
				for(int i = 0; i<temp.size(); i++){
					tempToArray[i] = temp.get(i);
				}


				if(inputs.length>0){
					System.out.println("========="+node.getProperty("name")+"    node pro");
					double nodeWeight = (double) node.getProperty("weight");
					relation = node.createRelationshipTo(sNode, RelTypes.IN);
					relation.setProperty("From", (String)node.getProperty("name"));
					relation.setProperty("To", (String)sNode.getProperty("name"));
					relation.setProperty("outputs", tempToArray);
					relation.setProperty("Direction", "incoming");    
					relation.setProperty("weight", (sNodeWeight+nodeWeight)/2);
				}

			}
			javaInputNodes = null;
		}
		inputs = null;
		ids = null;
		relation = null;
		sNode = null;
		transaction.success();
		transaction.finish();
		transaction.close();
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
	private int[] getServicesId(String[] nodes) {
		// TODO Auto-generated method stub
		int[] ids = new int[nodes.length];
		int i = -1;
		for(String input: nodes){
			if(input.length()>0){
				System.out.println(input);
				ServiceNode sNode = serviceMap.get(input);
				if(sNode!=null){
					i++;
					ids[i] = sNode.index;
				}
			}
		}
		return ids;
	}
	//run task
	private void runTask() {
		System.out.println("run task");
		Set<Set<Node>>allPossibleNeo4jInputServiceNodes = new HashSet<Set<Node>>();
		findAllTaskInputsNodes(allPossibleNeo4jInputServiceNodes);

		Set<Set<Node>>allPossibleNeo4jOutputServiceNodes = new HashSet<Set<Node>>();
		findAllTaskOutputsNodes(allPossibleNeo4jOutputServiceNodes);

		Transaction transaction = graphDatabaseService.beginTx();
		Node startNode = createNode("start",taskInputs);
		Node endNode = createNode("end",taskOutputs);

		for(Set<Node> inputNodes: allPossibleNeo4jInputServiceNodes){
			removeStartNodeAndEndNodeRel(startNode, endNode);
			for(Set<Node> outputNodes: allPossibleNeo4jOutputServiceNodes){
				createRel(startNode, inputNodes);
				createRel(endNode, outputNodes);
				List<List<Node>> pathsBetweenNodes = findAllPath(startNode, endNode);
				boolean removed = removeUnrelatedPaths(pathsBetweenNodes);
				if(!removed){
					allPaths.add(pathsBetweenNodes);
				}
			}	
		}


		//		for(Node input: neo4jInputServiceNodes){
		//			for(Node output: neo4jOutputServiceNodes){
		//				findAllPath(input, output);
		//			}
		//		}

		//		Node problemOutputNode = null;
		//		Node problemInputNode = null;
		//		for(int i = 0; i<paths.size(); i++){//paths
		//			problemInputNode = paths.get(i).get(0).get(0);
		//			if(paths.get(i).get(0).size()==1){
		//				problemOutputNode = paths.get(i).get(0).get(0);
		//			}
		//			else {
		//				problemOutputNode = paths.get(i).get(0).get(1);
		//			}
		//			//			System.out.print(problemOutputNode.getProperty("name")+"  ");
		//			for(int j = 0; j<paths.get(i).size(); j++){//each start node and end node path list
		//				//				for(int m = 0; m<paths.get(i).get(j).size(); m++){//each path		
		//				checkPathMatchAllInputs(startNode, endNode,problemInputNode, problemOutputNode, paths.get(i).get(j));
		//				//				}
		//			}
		//
		//		}
		transaction.success();
		transaction.finish();
		transaction.close();

		//		double minWeight = Double.MAX_VALUE;
		//		String path = "";
		//		String startNode = "";
		//		String endNode="";
		//		for(List<String> weightInfo: compareWeights){
		//			double weight = Double.parseDouble(weightInfo.get(3));
		//			if (weight<minWeight){
		//				minWeight = weight;
		//				path = weightInfo.get(2);
		//				startNode = weightInfo.get(0);
		//				endNode = weightInfo.get(1);
		//			}
		//		}
		//		System.out.println("================================");
		//		System.out.println("Start Node: "+ startNode+ "  End Node："+ endNode);
		//
		//		System.out.println("Path: "+ path);
		//		System.out.println("min weight: "+ minWeight);




	}


	private boolean removeUnrelatedPaths(List<List<Node>> pathsBetweenNodes) {
		// TODO Auto-generated method stub
		boolean toReturn = false;
		Set<Node>relatedNodes = getsNodes(pathsBetweenNodes);
		toReturn = checkValidComposition(relatedNodes);

		return toReturn;
	}

	private boolean checkValidComposition(Set<Node> relatedNodes) {
		// TODO Auto-generated method stub
		//		IndexHits<Node> hits = services.get( "name", "start" );
		//		Node startNode = hits.getSingle();
		//		hits = services.get( "name", "end" );
		//		Node endNode = hits.getSingle();
		//		checkNodeInputNodes(relatedNodes);
		for(Node node: relatedNodes){

			Iterable<Relationship> relationships = node.getRelationships(Direction.INCOMING);
			List<Map<String,List<String>>> nodeRelOutputs = new ArrayList<Map<String,List<String>>>();
			List<String> priousNodeNames = Arrays.asList(getNodePropertyArray(node,"priousNodeNames"));
			Set<String> relOutputs = new HashSet<String>();

			for(Relationship r: relationships){
				String from = (String) r.getProperty("From");
				if(priousNodeNames.contains(from)){
					relOutputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r,"outputs")));

				}
				List<String> outputs = Arrays.asList(getNodeRelationshipPropertyArray(r,"outputs"));
				Map<String, List<String>> mapOutputs = new HashMap<String,List<String>>();
				mapOutputs.put((String) r.getProperty("From"), outputs);
				nodeRelOutputs.add(mapOutputs);
			}
			if(relOutputs.size()<getNodePropertyArray(node,"inputs").length){
				return false;
			}
		}
		return true;
	}

	//	private boolean checkNodeInputNodes(Set<Node> relatedNodes) {
	// TODO Auto-generated method stub
	//		for(Node node: relatedNodes){
	//			
	//			Iterable<Relationship> relationships = node.getRelationships(Direction.INCOMING);
	//			List<Map<String,List<String>>> nodeRelOutputs = new ArrayList<Map<String,List<String>>>();
	//			List<String> priousNodeNames = Arrays.asList(getNodePropertyArray(node,"priousNodeNames"));
	//			Set<String> relOutputs = new HashSet<String>();
	//			
	//			for(Relationship r: relationships){
	//				String from = (String) r.getProperty("From");
	//				if(priousNodeNames.contains(from)){
	//					relOutputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r,"outputs")));
	//					
	//				}
	//				List<String> outputs = Arrays.asList(getNodeRelationshipPropertyArray(r,"outputs"));
	//				Map<String, List<String>> mapOutputs = new HashMap<String,List<String>>();
	//				mapOutputs.put((String) r.getProperty("From"), outputs);
	//				nodeRelOutputs.add(mapOutputs);
	//			}
	//			if(relOutputs.size()<getNodePropertyArray(node,"inputs").length){
	//				return false;
	//			}
	//		}
	//		return true;

	//	}

	private Set<Node> getsNodes(List<List<Node>> paths) {
		// TODO Auto-generated method stub
		Set<Node> allNodes = new HashSet<Node>();
		for(List<Node> path: paths){
			allNodes.addAll(path);
		}
		return allNodes;
	}

	private void removeStartNodeAndEndNodeRel(Node startNode, Node endNode) {
		// TODO Auto-generated method stub
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

	private void createRel(Node node, Set<Node> nodes) {
		System.out.println("nodesize: "+nodes.size());
		// TODO Auto-generated method stub
		Transaction transaction = graphDatabaseService.beginTx();


		if(node.getProperty("name").equals("start")){
			List<String>nodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs"));
			for(Node n: nodes){
				List<String>temp = new ArrayList<String>(nodeOutputs);
				List<String>inputs = Arrays.asList(getNodePropertyArray(n, "inputs"));
				temp.retainAll(inputs);
				String[] tempToArray = new String[temp.size()];
				for(int i = 0; i<temp.size(); i++){
					tempToArray[i] = temp.get(i);
				}
				relation = node.createRelationshipTo(n, RelTypes.IN);
				relation.setProperty("From", (String)node.getProperty("name"));
				relation.setProperty("To", (String)n.getProperty("name"));
				relation.setProperty("outputs", tempToArray);
				relation.setProperty("Direction", "incoming");   
			}
		}
		else if(node.getProperty("name").equals("end")){
			List<String>nodeInputs = Arrays.asList(getNodePropertyArray(node,"inputs"));
			for(Node n: nodes){
				List<String>temp = new ArrayList<String>(nodeInputs);
				List<String>outputs = Arrays.asList(getNodePropertyArray(n, "outputs"));
				temp.retainAll(outputs);
				String[] tempToArray = new String[temp.size()];
				for(int i = 0; i<temp.size(); i++){
					System.out.println("end: "+temp.get(i));
					tempToArray[i] = temp.get(i);
				}
				relation = n.createRelationshipTo(node, RelTypes.IN);
				relation.setProperty("From", (String)n.getProperty("name"));
				relation.setProperty("To", (String)node.getProperty("name"));
				relation.setProperty("outputs", tempToArray);
				relation.setProperty("Direction", "incoming");   
			}
		}
		transaction.success();
		transaction.finish();
		transaction.close();


	}

	private boolean checkPathMatchAllInputs(Node startNode, Node endNode, Node problemInputNode, Node problemOutputNode, List<Node> list) {
		if(!checkStringArrays(endNode.getProperty("inputs"),problemOutputNode.getProperty("outputs"))){
			return false;
		}
		if(!checkStringArrays(startNode.getProperty("outputs"),problemInputNode.getProperty("inputs"))){
			return false;
		}
		findAllInputsForPath(problemOutputNode, list);
		return false;
	}

	private void findAllInputsForPath(Node problemOutputNode, List<Node> list) {
		for(Node node: list){
			if(checkStringArrays(problemOutputNode.getProperty("inputs"), node.getProperty("outputs"))){
				findAllInputsForPath(node,list);
			}
		}

	}

	private boolean checkStringArrays(Object obj1, Object obj2) {
		String o1 = Arrays.toString((String[]) obj1).substring(1, Arrays.toString((String[]) obj1).length()-1);
		String[] array1 = o1.split("\\s*,\\s*");
		String o2 = Arrays.toString((String[]) obj2).substring(1, Arrays.toString((String[]) obj2).length()-1);
		String[] array2 = o2.split("\\s*,\\s*");
		return Arrays.equals(array1	, array2);
	}

	private Node createNode(String nodeName, Set<String> inputOutputServices) {
		String[]temp = new String[0];
		String[]array = inputOutputServices.toArray(new String[inputOutputServices.size()]);
		Node service = graphDatabaseService.createNode();
		Label nodeLable = DynamicLabel.label(nodeName);
		service.addLabel(nodeLable);
		service.setProperty("name", nodeName);
		services.add(service, "name", service.getProperty("name"));
		if(nodeName.equals("start")){
			service.setProperty("inputs", temp);
			service.setProperty("outputs", array);
			service.setProperty("priousNodeNames", temp);


			service.setProperty("id", 0);
		}
		else if(nodeName.equals("end")){
			service.setProperty("id", serviceNodes.size()+1);
			service.setProperty("inputs", array);
			service.setProperty("outputs", temp);
			service.setProperty("priousNodeNames", temp);
		}
		return service;
	}

	private void findAllTaskInputsNodes(Set<Set<Node>> allPossibleNeo4jInputServiceNodes) {
		//		System.out.println("find task input nodes: ");
		//Set<Set<String>> set of set of possible services connected to start node
		Set<Set<String>>taskInputsServiceNodes = new HashSet<Set<String>>();

		for(ServiceNode sNode: serviceNodes){
			Set<String>inputServicesNodes = new HashSet<String>();

			Set<String> temp = new HashSet<String>(sNode.getInputs());
			temp.retainAll(taskInputs);
			if(temp.size()==taskInputs.size() && taskInputs.size()==sNode.getInputs().size()){
				inputServicesNodes.add(sNode.getName());
				taskInputsServiceNodes.add(inputServicesNodes);
			}
			else if(temp.size()>0  && temp.size()==sNode.getInputs().size()){
				inputServicesNodes.add(sNode.getName());
				findAllOtherInputServices(temp, taskInputs, inputServicesNodes);
				taskInputsServiceNodes.add(inputServicesNodes);
			}
		}

		for(Set<String> inputServices: taskInputsServiceNodes){
			Set<Node> neo4jInputServiceNodes = new HashSet<Node>();
			for(String inputService: inputServices){
				System.out.println("inputservice: "+inputService);
				neo4jInputServiceNodes.add(neo4jServNodes.get(inputService));
			}
			allPossibleNeo4jInputServiceNodes.add(neo4jInputServiceNodes);
		}
	}

	private void findAllOtherInputServices(Set<String> temp, Set<String> taskInputs, Set<String> inputServicesNodes) {
		// TODO Auto-generated method stub
		Set<String> s = new HashSet<String>();
		for(String input: taskInputs){
			if(!temp.contains(input)){
				s.add(input);
			}
		}
		for(ServiceNode sNode: serviceNodes){
			if(sNode.getInputs().size()>0){
				Set<String> temp2 = new HashSet<String>(sNode.getInputs());
				temp2.retainAll(s);

				if(temp2.size()==s.size() && s.size()==sNode.getInputs().size()){
					inputServicesNodes.add(sNode.getName());
				}
				else if(temp2.size()>0 && temp2.size()==sNode.getInputs().size()){
					inputServicesNodes.add(sNode.getName());

					findAllOtherInputServices(temp2, s,inputServicesNodes);
				}
			}

		}


	}
	private void findAllTaskOutputsNodes(Set<Set<Node>> allPossibleNeo4jOutputServiceNodes) {
		//		System.out.println("find task input nodes: ");
		//Set<Set<String>> set of set of possible services connected to start node
		Set<Set<String>>taskOutputsServiceNodes = new HashSet<Set<String>>();

		for(ServiceNode sNode: serviceNodes){
			Set<String>outputServicesNodes = new HashSet<String>();

			Set<String> temp = new HashSet<String>(sNode.getOutputs());
			temp.retainAll(taskOutputs);
			if(temp.size()==taskOutputs.size() && taskOutputs.size()==sNode.getOutputs().size()){
				outputServicesNodes.add(sNode.getName());
				taskOutputsServiceNodes.add(outputServicesNodes);
			}
			else if(temp.size()>0  && temp.size()==sNode.getInputs().size()){
				outputServicesNodes.add(sNode.getName());
				findAllOtherOutputServices(temp, taskOutputs, outputServicesNodes);
				taskOutputsServiceNodes.add(outputServicesNodes);
			}
		}
		for(Set<String> outputServices: taskOutputsServiceNodes){
			Set<Node> neo4jOutputServiceNodes = new HashSet<Node>();
			for(String outputService: outputServices){
				System.out.println("outputservice: "+outputService);
				neo4jOutputServiceNodes.add(neo4jServNodes.get(outputService));
			}
			allPossibleNeo4jOutputServiceNodes.add(neo4jOutputServiceNodes);
		}
	}

	private void findAllOtherOutputServices(Set<String> temp, Set<String> taskOutputs, Set<String> outputServicesNodes) {
		// TODO Auto-generated method stub
		Set<String> s = new HashSet<String>();
		for(String input: taskOutputs){
			if(!temp.contains(input)){
				s.add(input);
			}
		}
		for(ServiceNode sNode: serviceNodes){
			Set<String> temp2 = new HashSet<String>(sNode.getOutputs());
			temp2.retainAll(s);

			if(temp2.size()==s.size() && s.size()==sNode.getOutputs().size()){
				outputServicesNodes.add(sNode.getName());
			}
			else if(temp2.size()>0 && temp2.size()==sNode.getOutputs().size()){
				outputServicesNodes.add(sNode.getName());

				findAllOtherOutputServices(temp2, s,outputServicesNodes);
			}
		}


	}
	void shutdown(){
		graphDatabaseService.shutdown();
		System.out.println("Neo4j database is shutdown");
	}

	private void createServicesDatabase(Map<String, ServiceNode> serviceMap){
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath);
		Transaction transaction = graphDatabaseService.beginTx();
		neo4jServiceNodes = new Node[0];
		try{

			//			Index<Node> idIndex = graphDatabaseService.index().forNodes("identifiers");
			index = graphDatabaseService.index();
			services = index.forNodes( "services" );

			for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {

				//				System.out.println("create noce: "+index);
				String key = entry.getKey();
				ServiceNode value = entry.getValue();
				//				System.out.println(value.getName());
				//				System.out.println(value.getInputsArray().length+"     "+value.getOutputsArray().length);
				//				double weight = calculateWeight(value.getQos());
				double weight = value.getQos()[TIME];
				Node service = graphDatabaseService.createNode();
				String [] priousNodeNames = new String[0];
				Label nodeLable = DynamicLabel.label(key);
				service.addLabel(nodeLable);
				service.setProperty("name", key);
				service.setProperty("id", value.index+1);
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
			System.out.println("web service nodes created");			
			transaction.success();
		}finally{
			transaction.finish();
		}
	}

	private double calculateWeight(double[] qos) {
		// TODO Auto-generated method stub
		double weight = 0;
		for(double d: qos){
			weight += d;
		}
		return weight;
	}

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

		for (ServiceNode s: serviceMap.values()) {
			addServiceToTaxonomyTree(s);
		}
		//after add all services to taxonomyTree, for each service node, go through each inputs instance find all output services to this service node and add to outputsServicesToThisService set.
		// go through each outputs instance find all input services to this service node and add to inputsServicesToThisService set.
		addAllInputOutputServicesToEachServiceNode();

	}
	private void findAllParentsAndChildrenTNode(){
		Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
		Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();

		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
			//			System.out.println();
			//			System.out.println();

			//			System.out.println("taxonomyNode: "+entry.getKey());

			TaxonomyNode n = entry.getValue();
			// Also add output to all parent nodes
			queue.clear();

			queue.add( n );

			while (!queue.isEmpty()) {
				//				System.out.println();
				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsOutput.add( current );
					//					System.out.println("current: "+current.value);
					for (TaxonomyNode parent : current.parents) {
						if(!parent.value.equals("")){

							current.parents_notGrandparents.add(parent);
							//							System.out.println("================parent: "+parent.value);
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
					//					System.out.println("current: "+current.value);
					for (TaxonomyNode child : current2.children) {
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
		// TODO Auto-generated method stub

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
			//			System.out.println("Service node: "+sNode.getName() +"  inputs size:  "+inputs.size());
			for(String input: inputs){
				//				System.out.println("input: "+input);
				if(!input.equals("")||input!=null){
					TaxonomyNode inputNode = taxonomyMap.get(input);
					input = inputNode.parentNode.value;
					if(cs.get(input).size()>0){
						Set<TaxonomyNode> children = cs.get(input);
						for(TaxonomyNode child:children){
							//							System.out.println("child: "+child.value);
							if(copy.contains(child.value) && !inputNode.value.equals(child.value)){
								copy.remove(child.value);
								//								System.out.println("remove input: "+child.value);
							}
						}
					}
				}
			}
			//		inputs.clear();
			sNode.setInputs(copy);
			//			System.out.println("  inputs size AFTER REMOVE:  "+sNode.getInputs().size());
			//			copy.clear();
			//		sNode = null;
		}
	}

	private void removeParentsOutputs(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			ServiceNode sNode = entry.getValue();
			//			System.out.println("Service node: "+sNode.getName());

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
					//					System.out.println("current: "+current.value);

					for (TaxonomyNode parent : current.parents) {
						current.parents_notGrandparents.add(parent);
						//						System.out.println("================parent: "+parent.value);
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
			s.getTaxonomyOutputs().add(n);
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
			//		inputs.clear();
			//		outputs.clear();

		}

	}

	private void addEndNodeToTaxonomyTree() {
		for (String inputVal : endNode.getInputs()) {
			TaxonomyNode n = taxonomyMap.get(inputVal);
			n.endNodeInputs.add(inputVal);

			// Also add input to all children nodes
			Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
			queue.addAll(n.children);

			while(!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				current.endNodeInputs.add(inputVal);
				queue.addAll(current.children);
			}
		}

	}

	/**
	 * Converts input, output, and service instance values to their corresponding
	 * ontological parent.
	 */
	private void findConceptsForInstances() {
		Set<String> temp = new HashSet<String>();

		for (String s : taskInputs)
			temp.add(taxonomyMap.get(s).parents.get(0).value);
		taskInputs.clear();
		taskInputs.addAll(temp);

		temp.clear();
		for (String s : taskOutputs)
			temp.add(taxonomyMap.get(s).parents.get(0).value);
		taskOutputs.clear();
		taskOutputs.addAll(temp);

		for (ServiceNode s : serviceMap.values()) {
			temp.clear();
			Set<String> inputs = s.getInputs();
			for (String i : inputs)
				temp.add(taxonomyMap.get(i).parents.get(0).value);
			inputs.clear();
			inputs.addAll(temp);

			temp.clear();
			Set<String> outputs = s.getOutputs();
			for (String o : outputs){
				temp.add(taxonomyMap.get(o).parents.get(0).value);
			}

			outputs.clear();
			outputs.addAll(temp);
		}
	}



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

	private void calculateNormalisationBounds(Set<ServiceNode> services) {
		for(ServiceNode service: services) {
			double[] qos = service.getQos();

			// Availability
			double availability = qos[AVAILABILITY];
			if (availability > maxAvailability)
				maxAvailability = availability;

			// Reliability
			double reliability = qos[RELIABILITY];
			if (reliability > maxReliability)
				maxReliability = reliability;

			// Time
			double time = qos[TIME];
			if (time > maxTime)
				maxTime = time;
			if (time < minTime)
				minTime = time;

			// Cost
			double cost = qos[COST];
			if (cost > maxCost)
				maxCost = cost;
			if (cost < minCost)
				minCost = cost;
		}
		// Adjust max. cost and max. time based on the number of services in shrunk repository
		maxCost *= services.size();
		maxTime *= services.size();
	}

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
	private Set<ServiceNode> discoverService(Collection<ServiceNode> services, Set<String> searchSet) {
		Set<ServiceNode> found = new HashSet<ServiceNode>();
		for (ServiceNode s: services) {
			if (isSubsumed(s.getInputs(), searchSet))
				found.add(s);
		}
		return found;
	}

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
				if (!runningOwls) {
					qos[TIME] = Double.valueOf(eElement.getAttribute("Res"));
					qos[COST] = Double.valueOf(eElement.getAttribute("Pri"));
					qos[AVAILABILITY] = Double.valueOf(eElement.getAttribute("Ava"));
					qos[RELIABILITY] = Double.valueOf(eElement.getAttribute("Rel"));
				}

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
				ServiceNode ws = new ServiceNode(name, qos, inputs, outputs, i);
				serviceNodes.add(ws);
				serviceMap.put(name, ws);
				//			inputs = new HashSet<String>();
				//			outputs = new HashSet<String>();
				//				inputs.clear();
				//				outputs.clear();
				qos = new double[4];
				ws = null;
			}
			calculateNormalisationBounds(serviceNodes);
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
		// TODO Auto-generated method stub
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
						taxNode = new TaxonomyNode(value);
						taxonomyMap.put( value, taxNode );
					}
					value =null;
					if (parent != null) {
						taxNode.setParentNode(parent);
						parent.setChildNode(taxNode);
						taxNode.parents.add(parent);
						taxNode.addParent(parent.value);
						parent.children.add(taxNode);
						parent.addChild(taxNode.value);
					}
					NodeList children = currNode.getChildNodes();
					processTaxonomyChildren(taxNode, children);
				}
			}
		}
	}



	//	private void findAllChildrenTNode(){
	//		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
	//
	//		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
	//			TaxonomyNode n = entry.getValue();
	//			// Also add output to all parent nodes
	//			Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
	//			queue.add( n );
	//
	//			while (!queue.isEmpty()) {
	//				TaxonomyNode current = queue.poll();
	//				if(!current.value.equals("")){
	//					seenConceptsInput.add( current );
	////					System.out.println("current: "+current.value);
	//					for (TaxonomyNode child : current.children) {
	//						current.children_notGrandchildren.add(child);
	//						if(!child.value.equals("")){
	//							if (!seenConceptsInput.contains( child )) {
	//								queue.add(child);
	//								seenConceptsInput.add(child);
	//							}
	//						}
	//
	//
	//					}
	//				}
	//			}
	//		}
	//	}

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
	@SuppressWarnings("deprecation")
	//http://neo4j.com/docs/stable/tutorials-java-embedded-graph-algo.html example
	private List<String> findPath(Node startNode, Node endNode){
		Transaction transaction = graphDatabaseService.beginTx();
		//		System.out.println(startNode.getProperty("name")+"  -   "+endNode.getProperty("name"));
		if(startNode==null| endNode == null)return null;
		List<String> toReturn = new ArrayList<String>();
		toReturn.add( (String) startNode.getProperty("name"));
		toReturn.set(0, (String) startNode.getProperty("name"));
		toReturn.add( (String) endNode.getProperty("name"));
		toReturn.set(1, (String) endNode.getProperty("name"));

		PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), 5);                  
		//将经过的节点数相同的路径全部查询回来         
		Iterable<Path> paths = finder.findAllPaths(startNode, endNode);          
		Iterator<Path> iterator = paths.iterator();          
		Path p;          
		while(iterator.hasNext()) {              
			p = iterator.next();        

			System.out.println("shortestPath" + p.toString());              
			System.out.println("From Start Node: "+p.startNode().getProperty("name") + " to End Node:  " + p.endNode().getProperty("name"));          
		}                    

		//取所有路径中的第一条         
		p = finder.findSinglePath(startNode, endNode);          
		System.out.println("shorestPath singlePath :" + p.toString());    
		//		
		//		PathFinder<WeightedPath> finder1 = GraphAlgoFactory.dijkstra(                  
		//				Traversal.expanderForTypes( RelTypes.IN, Direction.BOTH ), "weight" );                   
		//		WeightedPath path = finder1.findSinglePath(startNode, endNode);  
		//		if(path==null){
		//			return null;
		//		}
		//	
		//		// Get the weight for the found path          
		//		System.out.println("Dijkstra Weight: "+path.weight());          
		//		System.out.println("Dijkstra Path: "+path.toString());
		transaction.success();
		transaction.finish();
		transaction.close();
		return toReturn; 
	}
	private List<List<Node>> findAllPath(Node startNode, Node endNode){
		Map<String, Object>maps = new HashMap<String, Object>();

		Transaction transaction = graphDatabaseService.beginTx();

		System.out.println(startNode.getProperty("id")+"  -   "+endNode.getProperty("id"));
		int startNodeId = (int) startNode.getProperty("id");
		int endNodeId = (int) endNode.getProperty("id");
		String output = "";
		TraversalDescription friendsTraversal = graphDatabaseService.traversalDescription()
				.depthFirst()
				.relationships( RelTypes.IN ,Direction.OUTGOING)
				//Traversals have uniqueness rules. Read that link, it talks about how the traverser works, and how you can configure it. By default, the uniqueness rules are set to NODE_GLOBAL meaning that a certain node cannot be traversed more than once.
				//should use RELATIONSHIP_PATH instead
				.uniqueness( Uniqueness.RELATIONSHIP_PATH );
		List<List<Node>> p = new ArrayList<List<Node>>();
		for ( Path path : friendsTraversal
				.evaluator( Evaluators.toDepth( 100 ) )
				.evaluator(Evaluators.returnWhereEndNodeIs(graphDatabaseService.getNodeById(endNodeId)))
				.traverse( startNode ) )
		{
			Set<Node> sNodes = new HashSet<Node>();
			List<Node>nodes = new ArrayList<Node>();
			int i = -1;
			for(Node node: path.nodes()){
				i++;
				nodes.add(node);
				nodes.set(i, node);
			}
			for(int j = nodes.size()-1; j>0; j--){
				String priousNodeName = (String) nodes.get(j-1).getProperty("name");
				String[] priousNodeNames = getNodePropertyArray(nodes.get(j),"priousNodeNames");
				//TODO: both input as same value serv3 node 4 and 7 have same output
				if(!containElement(priousNodeName,priousNodeNames)){
					priousNodeNames = increaseArray(priousNodeNames);
					priousNodeNames[priousNodeNames.length-1] = priousNodeName;
				}
				nodes.get(j).setProperty("priousNodeNames", priousNodeNames);
			}

			for(Node n: path.nodes()){
				sNodes.add(n);
			}
			List<Node> sNodesToList = new ArrayList<Node>();
			sNodesToList.addAll(sNodes);
			output += path + "\n";
			p.add(sNodesToList);
		}
		System.out.println("p size: "+p.size());
		System.out.println(output);
		transaction.success();
		transaction.finish();
		transaction.close();
		return p;
	}
	private boolean containElement(String value, String[]values){
		for(String s: values){
			if(s.equals(value)){
				return true;
			}
		}
		return false;
	}
	//	private void findAllPath(Node startNode, Node endNode){
	//		Map<String, Object>maps = new HashMap<String, Object>();
	//
	//		Transaction transaction = graphDatabaseService.beginTx();
	//		System.out.println(startNode.getProperty("name")+"  -   "+endNode.getProperty("name"));
	//		int startNodeId = (int) startNode.getProperty("id");
	//		int endNodeId = (int) endNode.getProperty("id");
	//		String output = "";
	//		TraversalDescription friendsTraversal = graphDatabaseService.traversalDescription()
	//				.depthFirst()
	//				.relationships( RelTypes.IN ,Direction.OUTGOING)
	//				//Traversals have uniqueness rules. Read that link, it talks about how the traverser works, and how you can configure it. By default, the uniqueness rules are set to NODE_GLOBAL meaning that a certain node cannot be traversed more than once.
	//				//should use RELATIONSHIP_PATH instead
	//				.uniqueness( Uniqueness.RELATIONSHIP_PATH );
	//		List<List<Node>> p = new ArrayList<List<Node>>();
	//		for ( Path path : friendsTraversal
	//				.evaluator( Evaluators.toDepth( 100 ) )
	//				.evaluator(Evaluators.returnWhereEndNodeIs(graphDatabaseService.getNodeById(endNodeId)))
	//				.traverse( startNode ) )
	//		{
	//			Set<Node> sNodes = new HashSet<Node>();
	//			sNodes.add(startNode);
	//			sNodes.add(endNode);
	//			for(Node n: path.nodes()){
	//				sNodes.add(n);
	//			}
	//			List<Node> sNodesToList = new ArrayList<Node>();
	//			sNodesToList.addAll(sNodes);
	//			sNodesToList.set(0, startNode);
	//			if(sNodes.size()>1)
	//				sNodesToList.set(1, endNode);
	//
	//			output += path + "\n";
	//			p.add(sNodesToList);
	//		}
	//		paths.add(p);
	//		System.out.println("p size: "+p.size());
	//		System.out.println("paths size: "+paths.size());
	//		System.out.println(output);
	//		//		maps.put("startNodeId",startNodeId+"");
	//		//		maps.put("endNodeId", endNodeId+"");
	//		//		TraversalDescription desc = Traversal.description()
	//		////		        .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
	//		//		        .evaluator(Evaluators.returnWhereEndNodeIs(graphDatabaseService.getNodeById(endNodeId)))
	//		////		        .evaluator(Evaluators.includingDepths(3, 3))
	//		//		        .relationships(RelTypes.IN);
	//		//
	//		//		Traverser traverse = desc.traverse(graphDatabaseService.getNodeById(startNodeId), graphDatabaseService.getNodeById(endNodeId));
	//		//		Iterator<Path> paths = traverse.iterator();	
	//		//		System.out.println(paths.hasNext());
	//		//
	//		//		while(paths.hasNext()){
	//		//			System.out.println("paht has next");
	//		//
	//		//			Path path = paths.next();
	//		//			System.out.println(path.toString());
	//		//		}
	//		//		Iterator<Node> pathNodes = null;
	//		//		try ( 
	//		//				//						Result execResult = graphDatabaseService.execute( "match (n{name: {o}}) return n", MapUtil.map("o",input)))
	//		//
	//		//				Result execResult = graphDatabaseService.execute( "START a=node({startNodeId}), d=node({endNodeId}) MATCH p=a-[r:IN*..]-d",maps))
	//		//
	//		//		{
	//		//			relation = null;
	//		//			pathNodes = null;
	//		//			pathNodes = execResult.columnAs("p");
	//		//			for (Node node : IteratorUtil.asIterable(pathNodes))
	//		//			{	   
	//		//				System.out.println(node.getProperty("name"));
	//		//				
	//		//			}
	//		//			pathNodes = null;
	//		//		}
	//		//		System.out.println(startNode.getProperty("name")+"  -   "+endNode.getProperty("name"));
	//		//		if(startNode==null| endNode == null)return null;
	//		//		List<String> toReturn = new ArrayList<String>();
	//		//		toReturn.add( (String) startNode.getProperty("name"));
	//		//		toReturn.set(0, (String) startNode.getProperty("name"));
	//		//		toReturn.add( (String) endNode.getProperty("name"));
	//		//		toReturn.set(1, (String) endNode.getProperty("name"));
	//		//
	//		//		PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), 5);                  
	//		//		//将经过的节点数相同的路径全部查询回来         
	//		//		Iterable<Path> paths = finder.findAllPaths(startNode, endNode);          
	//		//		Iterator<Path> iterator = paths.iterator();          
	//		//		Path p;          
	//		//		while(iterator.hasNext()) {              
	//		//			p = iterator.next();        
	//		//
	//		//			System.out.println("shortestPath" + p.toString());              
	//		//			System.out.println("From Start Node: "+p.startNode().getProperty("name") + " to End Node:  " + p.endNode().getProperty("name"));          
	//		//		}                    
	//		//
	//		//		//取所有路径中的第一条         
	//		//		p = finder.findSinglePath(startNode, endNode);          
	//		//		System.out.println("shorestPath singlePath :" + p.toString());    
	//		//		
	//		//		PathFinder<WeightedPath> finder1 = GraphAlgoFactory.dijkstra(                  
	//		//				Traversal.expanderForTypes( RelTypes.IN, Direction.BOTH ), "weight" );                   
	//		//		WeightedPath path = finder1.findSinglePath(startNode, endNode);  
	//		//		if(path==null){
	//		//			return null;
	//		//		}
	//		//	
	//		//		// Get the weight for the found path          
	//		//		System.out.println("Dijkstra Weight: "+path.weight());          
	//		//		System.out.println("Dijkstra Path: "+path.toString());
	//		transaction.success();
	//		transaction.finish();
	//		transaction.close();
	//	}

	private String[] getNodePropertyArray(Node sNode, String property){
		Object obj =sNode.getProperty(property);
		//    		//remove the "[" and "]" from string
		String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
		String[] tempInputs = ips.split("\\s*,\\s*");
		String[] array = new String[0];
		int i = -1;
		for(String s: tempInputs){

			if(s.length()>0){
				i++;
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
		int i = -1;
		for(String s: tempOutputs){

			if(s.length()>0){
				i++;
				array =increaseArray(array);
				array[array.length-1] = s;
			}
		}
		return array;
	}
}
