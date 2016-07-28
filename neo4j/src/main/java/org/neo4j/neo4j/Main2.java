
package org.neo4j.neo4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.nio.file.Files;
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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.Traversal;


@SuppressWarnings("deprecation")
public class Main2 implements Runnable{
	//public class Main2{

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
	public Map<String,Node>subGraphNodesMap = new HashMap<String,Node>();
	public Set<Node>subGraphNodes = new HashSet<Node>();
	public Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();
	public static Set<TaxonomyNode> children;
	public static Set<TaxonomyNode> parents;
	private static String serviceFileName = null;
	private static String taxonomyFileName = null;
	private static String taskFileName = null;

	public static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	public static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private static final String Neo4j_testServicesDBPath = "database/test_services";
	private static final String Neo4j_ServicesDBPath = "database/";
	private static final String Neo4j_tempDBPath = "database/temp_graph";
	//	private static final String Neo4j_TaxonomyDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/taxonomy";
	private boolean running = true;  

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
	static GraphDatabaseService tempGraphDatabaseService;

	public static IndexManager index = null;
	public static Index<Node> services = null;
	public static IndexManager tempIndex = null;
	public static Index<Node> tempServices = null;
	//	GraphDatabaseService graphDatabaseTaxonomy;
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	// Statistics tracking
	public static Map<String, Integer> nodeCount = new HashMap<String, Integer>();
	public static Map<String, Integer> edgeCount = new HashMap<String, Integer>();

	private static boolean runTestFiles = false;
	private static String databasename = "wsc2008dataset01";
	private static String dataset = "dataset/wsc2008/Set01MetaData/";
	private static String testDataset = "dataset/test/";
	private final int compositionSize = 12;

	public static void main( String[] args ) throws IOException{
		Main2 neo4jwsc = new Main2();
		Thread t = new Thread(neo4jwsc,"Neo4jThread");  
		t.start();
		//		neo4jwsc.index = null;
		neo4jwsc.servicesWithInputs= null;
		//		neo4jwsc.graphDatabaseService = null;
		long startTime = 0;
		long endTime = 0;
		if(!runTestFiles){
			serviceFileName = dataset+"services.xml";
			taxonomyFileName = dataset+"taxonomy.xml";
			taskFileName = dataset+"problem.xml";
		}else{
			serviceFileName = testDataset+"test_serv.xml";
			taxonomyFileName = testDataset+"test_taxonomy.xml";
			taskFileName = testDataset+"test_problem.xml";
		}
		startTime = System.currentTimeMillis();
		neo4jwsc.parseWSCTaxonomyFile(taxonomyFileName);
		endTime = System.currentTimeMillis();
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
		if(!runTestFiles){
			boolean dbExist = false;
			File f = new File(Neo4j_ServicesDBPath+""+databasename+"/index");
			if (f.exists() && f.isDirectory()) {
				dbExist = true;
			}else{
				dbExist = false;   
			}
			if(dbExist){
				graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath+""+databasename);
				registerShutdownHook(graphDatabaseService);  
				Transaction transaction = graphDatabaseService.beginTx();
				index = graphDatabaseService.index();
				services = index.forNodes( "identifiers" );
				transaction.success();
				transaction.close();
				signNodesToField(graphDatabaseService);
			}else{
				graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath+""+databasename);
				registerShutdownHook(graphDatabaseService);  
				startTime = 0;
				endTime = 0;
				startTime = System.currentTimeMillis();
				neo4jwsc.createServicesDatabase(serviceMap, Neo4j_ServicesDBPath+""+databasename);
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
			neo4jwsc.shutdown(graphDatabaseService);
			neo4jwsc.runTask(Neo4j_ServicesDBPath+""+databasename);
		}
		else{
			try {
				FileUtils.deleteRecursively(new File(Neo4j_testServicesDBPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_testServicesDBPath);
			registerShutdownHook(graphDatabaseService);  

			startTime = 0;
			endTime = 0;
			startTime = System.currentTimeMillis();
			neo4jwsc.createServicesDatabase(serviceMap,Neo4j_testServicesDBPath);
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
			neo4jwsc.shutdown(graphDatabaseService);
			neo4jwsc.runTask(Neo4j_testServicesDBPath);			
		}
		FileWriter fw = new FileWriter("timeRecord.txt");
		for(Entry<String, Long> entry : records.entrySet()){
			fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		}
		fw.close();
		//		neo4jwsc.shutdown(graphDatabaseService);
		neo4jwsc.setRunning(false);  

	}
	private static void signNodesToField(GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		Iterable<Node> nodes = graphDatabaseService.getAllNodes();
		neo4jServNodes.clear();
		for(Node n: nodes){
			neo4jServNodes.put((String)n.getProperty("name"), n);
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
			addInputsServiceRelationship(sNode, maps, inputServices);
		}
		servicesWithInputs = inputServices;
		servicesWithOutputs = serviceOutputs;
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
	//run task
	public boolean findNonFulfillNode = false;

	private void runTask(String path) throws IOException {
		System.out.println("run task");
		long startTime = 0;
		long endTime = 0;
		startNode = null;
		endNode = null;
		startTime = System.currentTimeMillis();
		File srcDir = new File(path);
		File destDir = new File(Neo4j_tempDBPath);
		FileUtils.deleteRecursively(new File(Neo4j_tempDBPath));
		copyFolder(srcDir, destDir);
		System.out.println("copying of file from Java program is completed");
		tempGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_tempDBPath);
		registerShutdownHook(tempGraphDatabaseService);  

		Transaction transaction = tempGraphDatabaseService.beginTx();				
		signNodesToField(tempGraphDatabaseService);

		tempIndex = tempGraphDatabaseService.index();
		tempServices = tempIndex.forNodes( "identifiers" );

		startNode = createStartEndNode("start");
		endNode = createStartEndNode("end");
		neo4jServNodes.put("start", startNode);
		neo4jServNodes.put("end", endNode);
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

		Set<Node> relatedNodes = new HashSet<Node>();
		findAllReleatedNodes(relatedNodes, false);
		System.out.println("related Nodes size: "+relatedNodes.size());
		reduceGraphDatabase(relatedNodes);
		endTime = System.currentTimeMillis();
		System.out.println("reduce graph Database Total execution time: " + (endTime - startTime) );
		startTime = 0;
		endTime = 0;
		startTime = System.currentTimeMillis();
		findCompositionNodes();
		endTime = System.currentTimeMillis();
		records.put("Find composition Nodes", endTime - startTime);
		System.out.println("Find composition Nodes Total execution time: " + (endTime - startTime) );
	}
	private Node createStartEndNode(String nodeName) {
		String[]temp = new String[0];
		Node service = tempGraphDatabaseService.createNode();
		Label nodeLable = DynamicLabel.label(nodeName);
		service.addLabel(nodeLable);
		service.setProperty("name", nodeName);
		tempServices.add(service, "name", service.getProperty("name"));
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
	private void reduceGraphDatabase(Set<Node> relatedNodes) {


		Transaction t = tempGraphDatabaseService.beginTx();

		for(Node n: getAllNodes()) {
			if(!relatedNodes.contains(n) && !n.getProperty("name").equals("end")&& !n.getProperty("name").equals("start")){
				for (Relationship r : n.getRelationships()) {
					r.delete();
				}
				n.delete();
				t.success();
				t.close();
				t = tempGraphDatabaseService.beginTx();
			}
		}
		t = tempGraphDatabaseService.beginTx();

		for(Node n: getAllNodes()) {
			subGraphNodesMap.put((String)n.getProperty("name"), n);			
		}
		t.close();
	}
	//	private void executeCypher(String cypher, String name){
	//		Transaction tx = tempGraphDatabaseService.beginTx();
	//		Map<String, Object> params = new HashMap<String, Object>();
	//		params.put( "node", name);
	//		Result result = null;
	//		try {
	//			ExecutionEngine engine = new ExecutionEngine( tempGraphDatabaseService );
	//			result = engine.execute( cypher, params );
	//			tx.success();
	//		} catch(Exception e){
	//			throw new RuntimeException("Error executing cypher: ", e);
	//		} finally {
	//			tx.finish();
	//		}
	//	}
	private Iterable<Node>  getAllNodes( ) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
		Iterable<Node> nodes = tempGraphDatabaseService.getAllNodes();
		transaction.success();
		transaction.close();
		return nodes;

	}

	private void findCompositionNodes() {
		Set<Set<Node>> populations = new HashSet<Set<Node>>();
		while(populations.size()<10){
			Set<Node>result = new HashSet<Node>();
			result.add(endNode);
			composition(endNode, result);	
			result = checkDuplicateNodes(result);
			if(result.size()<=compositionSize){
				populations.add(result);
				System.out.println("==========="+result.size()+"=================");
			}
		}
	}

	//	private void printAsPath(Set<Set<Node>> populations) {
	//		Transaction tx = subGraphDatabaseService.beginTx();
	//
	//		for(Set<Node> composition: populations){
	//			System.out.println();
	//			print(subEndNode, composition);
	//			System.out.println();
	//		}
	//		tx.close();
	//	}
	//
	//	private void print(Node subEndNode, Set<Node> composition) {
	//		List<String> inputServices = Arrays.asList(getNodePropertyArray(subEndNode, "inputServices"));
	//		if(inputServices.size()>0){
	//			for(String s: inputServices){
	//				Node node = subGraphNodesMap.get(s);
	//				if(composition.contains(node)){
	//					print(node, composition);
	//				}
	//			}
	//		}	
	//	}

	private Set<Node> checkDuplicateNodes(Set<Node> result) {
		Transaction tx = tempGraphDatabaseService.beginTx();
		Set<Node>temp = new HashSet<Node>(result);
		for(Node n : result){
			if(!n.getProperty("name").equals("start") && !n.getProperty("name").equals("end") ){
				temp.remove(n);
				if(!fulfillSubGraphNodes(temp)){
					temp.add(n);
				}
			}
		}

		tx.close();
		return temp;
	}

	private void composition(Node node, Set<Node> result) {
		Transaction tx = tempGraphDatabaseService.beginTx();
		try{
			List<String>nodeInputs = Arrays.asList(getNodePropertyArray(node, "inputs"));
			//		List<String>relOutputs = new ArrayList<String>();
			List<Relationship>rels = new ArrayList<Relationship>();
			for(Relationship r: node.getRelationships(Direction.INCOMING)){
				rels.add(r);
			}
			Set<Node> fulfillSubEndNodes = findNodesFulfillSubEndNode(nodeInputs,rels);

			if(fulfillSubEndNodes!=null){
				result.addAll(fulfillSubEndNodes);
				for (Node n: fulfillSubEndNodes){
					composition(n, result);
				}

			}
			tx.success();
		}catch(Exception e){
			System.out.println("composition:"+ e);
		}finally{
			tx.close();
		}

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
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
	
	private void createRel(Node node) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
		//		double sNodeWeight = (double) sNode.getProperty("weight");
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
		transaction.finish();
		transaction.close();
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

	void shutdown(GraphDatabaseService graphDatabaseService){
		graphDatabaseService.shutdown();
		System.out.println("Neo4j database is shutdown");
	}

	private void createServicesDatabase(Map<String, ServiceNode> serviceMap, String path){
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
			sNode.setOutputs(copy);
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
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (running) {  
			System.out.println(new Date() + " ### Neo4jService working.....！");  
			try {  
				Thread.sleep(20000);  
			} catch (InterruptedException e) {  
				System.out.println(e);  
			}  
		}  

	}
	public void setRunning(boolean running) {  
		this.running = running;  
	}  
	private static void registerShutdownHook(GraphDatabaseService graphDatabaseService) {  
		// Registers a shutdown hook for the Neo4j instance so that it  
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the  
		// running example before it's completed)  
		Runtime.getRuntime()  
		.addShutdownHook( new Thread()  
		{  
			@Override  
			public void run()  
			{  
				System.out.println("neo4j shutdown hook ... ");  
				graphDatabaseService.shutdown();  
			}  
		} );  
	}  
}
