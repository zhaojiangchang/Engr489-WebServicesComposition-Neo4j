package Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.io.fs.FileUtils;

import component.ServiceNode;
import component.TaxonomyNode;
import generateDatabase.GenerateDatabase;
import modellingServices.LoadFiles;
import modellingServices.PopulateTaxonomyTree;
import task.FindCompositions;
import task.OuchException;
import task.ReduceGraphDb;
import task.RunTask;


//public class Main {
public class Main implements Runnable{
	private static String serviceFileName = null;
	private static String taxonomyFileName = null;
	private static String taskFileName = null;

	private final static String Neo4j_testServicesDBPath = "database/test_services";
	private final static String Neo4j_ServicesDBPath = "database/";
	private final static String newResultDBPath = "database/result/";

	private boolean running = true;
	private Map<String,Long>records = new HashMap<String,Long>();
	private Map<Integer, Map<String,String>> bestResults = new HashMap<Integer, Map<String, String>>();
	private Map<Integer, Double> bestResultsTimes = new HashMap<Integer,Double>();
	private static Map<String, Node> neo4jServNodes = new HashMap<String, Node>();
	private static Map<String, Node> subGraphNodesMap = new HashMap<String, Node>();;
	private static GraphDatabaseService graphDatabaseService = null;
	private static GraphDatabaseService subGraphDatabaseService = null;
	private static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	private static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private static IndexManager index = null;
	@SuppressWarnings("unused")
	private static Index<Node> services;
	@SuppressWarnings("unused")
	private static Index<Node> tempServices;
	private static Node endNode = null;
	private static Node startNode = null;
	private static Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();
	private static String databaseName = "";
	private static LoadFiles loadFiles = null;
	//For setup == file location, composition size, and run test file or not
	//******************************************************//
	private final boolean runTestFiles = false;
	private final static String year = "2008";
	private final static String dataSet = "01";
	private final static int individuleNodeSize = 30;
	private final static int candidateSize = 2;
	private final boolean runQosDataset = true;
	private final boolean runMultipileTime = false;
	private final int timesToRun = 30;

	private final static double m_a = 0.25;
	private final static double m_r = 0.25;
	private final static double m_c = 0.25;
	private final static double m_t = 0.25;

	//******************************************************//


	public static void main( String[] args ) throws IOException, OuchException{
		Main neo4jwsc = new Main();
		//		Thread t = new Thread(neo4jwsc,"Neo4jThread");  
		//		t.start();
		databaseName = "wsc"+year+"dataset"+dataSet;
		String path;
		if(!neo4jwsc.runTestFiles){
			if(!neo4jwsc.runQosDataset){
				serviceFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/services.xml";
				taxonomyFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/taxonomy.xml";
				taskFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/problem.xml";
			}else{
				serviceFileName = "dataset/dataset/Set"+dataSet+"MetaData/services-output.xml";
				taxonomyFileName = "dataset/dataset/Set"+dataSet+"MetaData/taxonomy.xml";
				taskFileName = "dataset/dataset/Set"+dataSet+"MetaData/problem.xml";
			}
		}else{
			serviceFileName = "dataset/test/test_serv.xml";
			taxonomyFileName = "dataset/test/test_taxonomy.xml";
			taskFileName = "dataset/test/test_problem.xml";
		}
		//load files
		long startTime = System.currentTimeMillis();
		loadFiles();		
		long endTime = System.currentTimeMillis();
		neo4jwsc.records.put("Load files", endTime - startTime);
		System.out.println("Load files Total execution time: " + (endTime - startTime) );


		//populateTaxonomytree
		startTime = System.currentTimeMillis();
		populateTaxonomytree();		
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("Populate Taxonomy Tree", endTime - startTime);
		System.out.println("Populate Taxonomy Tree total execution time: " + (endTime - startTime) );


		//Identify run test file or not
		//if database exist read database
		//else if database not exist create database
		if(!neo4jwsc.runTestFiles){
			boolean dbExist;
			File f = new File(Neo4j_ServicesDBPath+""+databaseName +"/index");
			if (f.exists() && f.isDirectory()) {
				dbExist = true;
			}else{
				dbExist = false;   
			}
			if(dbExist){
				startTime = System.currentTimeMillis();
				loadExistingDB();
				endTime = System.currentTimeMillis();
				neo4jwsc.records.put("Load existing db", endTime - startTime);
				System.out.println("Load existing db Total execution time: " + (endTime - startTime) );

			}else{

				startTime = System.currentTimeMillis();
				neo4jwsc.generateDB(null,Neo4j_ServicesDBPath,"original",databaseName);			
				endTime = System.currentTimeMillis();
				neo4jwsc.records.put("Create new db", endTime - startTime);
				System.out.println("Create new db Total execution time: " + (endTime - startTime) );

			}
			path = Neo4j_ServicesDBPath+""+databaseName;
		}
		else{
			startTime = System.currentTimeMillis();
			try {
				FileUtils.deleteRecursively(new File(Neo4j_testServicesDBPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			neo4jwsc.generateDB(null,Neo4j_testServicesDBPath,"original test", null);						
			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("Create new test db", endTime - startTime);
			System.out.println("Create new test db Total execution time: " + (endTime - startTime) );

			path = Neo4j_testServicesDBPath;
		}



		//run task
		//1: copy database and call->tempServiceDatabase
		//2: connect to tempServiceDatabase
		//3: use task inputs outputs create start and end nodes and link to tempservicedatabase
		startTime = System.currentTimeMillis();
		runTask(path);
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("run task: copied db, create temp db, add start and end nodes", endTime - startTime);
		System.out.println("run task: copied db, create temp db, add start and end nodes Total execution time: " + (endTime - startTime) );



		//reduce database use copied database
		startTime = System.currentTimeMillis();
		reduceDB();
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("reduce graph db ", endTime - startTime);
		System.out.println("reduce graph db Total execution time: " + (endTime - startTime) );



		if(!neo4jwsc.runMultipileTime){
			//find compositions
			startTime = System.currentTimeMillis();
			Map<List<Node>, Map<String,Map<String, Double>>> resultWithQos =findCompositions();			
			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("generate candidates", endTime - startTime);
			System.out.println();
			System.out.println("generate candidates Total execution time: " + (endTime - startTime) );
			System.out.println();
			System.out.println();
			System.out.println();

			startTime = System.currentTimeMillis();
			printResult(resultWithQos);
			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("generate best result", endTime - startTime);
			System.out.println();
			System.out.println("generate best result Total execution time: " + (endTime - startTime) );
			System.out.println();
			System.out.println();
			System.out.println();



			startTime = System.currentTimeMillis();
			for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : resultWithQos.entrySet()){
				try {
					FileUtils.deleteRecursively(new File(newResultDBPath));
				} catch (IOException e) {
					e.printStackTrace();
				}
				//				generateDB(entry.getKey(),newResultDBPath,"result db", null);		
				GenerateDatabase generateDatabase2 = new GenerateDatabase(entry.getKey(), subGraphDatabaseService, newResultDBPath);
				generateDatabase2.createDbService();
				GraphDatabaseService newGraphDatabaseService = generateDatabase2.getGraphDatabaseService();
				registerShutdownHook(graphDatabaseService,"original test");
				generateDatabase2.setServiceMap(serviceMap);
				generateDatabase2.setTaxonomyMap(taxonomyMap);
				generateDatabase2.createServicesDatabase();
				//				System.out.println("findCompositions.getBestRels()"+bestRels);
				//				generateDatabase2.set(bestRels);
				generateDatabase2.addServiceNodeRelationShip();
				removeRedundantRel(newGraphDatabaseService);

				registerShutdownHook(subGraphDatabaseService,"Reduced");
				registerShutdownHook(newGraphDatabaseService, "Result");
			}

			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("create new result graph db ", endTime - startTime);
			System.out.println("create new result graph db Total execution time: " + (endTime - startTime) );


		}
		else {
			int count  = 0;
			while(count <neo4jwsc.timesToRun){
				//find compositions
				startTime = System.currentTimeMillis();
				FindCompositions findCompositions = new FindCompositions(candidateSize, individuleNodeSize, subGraphDatabaseService);
				findCompositions.setStartNode(startNode);
				findCompositions.setEndNode(endNode);
				findCompositions.setNeo4jServNodes(neo4jServNodes);
				findCompositions.setTaxonomyMap(taxonomyMap);
				findCompositions.setSubGraphNodesMap(subGraphNodesMap);
				findCompositions.setM_a(m_a);
				findCompositions.setM_r(m_r);
				findCompositions.setM_c(m_c);
				findCompositions.setM_t(m_t);
				Map<List<Node>, Map<String,Map<String, Double>>> candidates = findCompositions.run();
				Map<List<Node>, Map<String,Map<String, Double>>> resultWithQos = findCompositions.getResult(candidates);
				//		bestRels = findCompositions.getBestRels();

				System.out.println("Best result"+ count+": ");
				Transaction tx = subGraphDatabaseService.beginTx();


				try{
					for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry2 : resultWithQos.entrySet()){
						String services = "";
						for(Node n: entry2.getKey()){
							System.out.print(n.getProperty("name")+"--"+n.getId()+"   ");
							services += n.getProperty("name")+"  ";
						}
						String qos = "";
						System.out.println();
						System.out.print("QOS:  ");
						for (Map.Entry<String,Map<String, Double>> entry3 : entry2.getValue().entrySet()){
							if(entry3.getKey().equals("normalized")){
								System.out.println("normalized: ");
								for (Map.Entry<String, Double> entry4 : entry3.getValue().entrySet()){
									System.out.print(entry4.getKey()+": "+entry4.getValue()+"     ");

								}
								System.out.println();
								double fitnessOfBest = m_a*entry3.getValue().get("A") + m_r*entry3.getValue().get("R") + m_c*entry3.getValue().get("C") + m_t*entry3.getValue().get("T");
								System.out.println("fitnessOfBest:" +fitnessOfBest);
							}
							else if(entry3.getKey().equals("non_normalized")){
								System.out.println("non_normalized: ");
								for (Map.Entry<String, Double> entry4 : entry3.getValue().entrySet()){
									System.out.print(entry4.getKey()+": "+entry4.getValue()+"     ");
								}
								qos = entry3.getValue().get("A")+" "+entry3.getValue().get("R")+" "+entry3.getValue().get("T")+" "+entry3.getValue().get("C");
							}


						}
						Map<String, String> result = new HashMap<String,String>();
						result.put(qos, services);
						neo4jwsc.bestResults.put(count, result);
					}
					System.out.println();
				} catch (Exception e) {
					System.out.println(e);
					System.out.println("print populations error.."); 
				} finally {
					tx.close();
				}	
				endTime = System.currentTimeMillis();
				neo4jwsc.bestResultsTimes.put(count, (double) (endTime - startTime));
				neo4jwsc.records.put("generate best result", endTime - startTime);
				System.out.println();
				System.out.println("generate best result Total execution time: " + (endTime - startTime) );
				System.out.println();
				System.out.println();
				System.out.println();
				count++;
			}
			if(neo4jwsc.runTestFiles){
				FileWriter fw = new FileWriter("test-dataset-bestResults.stat");
				for(Entry<Integer,Map<String, String>> entry : neo4jwsc.bestResults.entrySet()){
					for(Entry<String,String> entry2: entry.getValue().entrySet()){
						fw.write(neo4jwsc.bestResultsTimes.get(entry.getKey())+"\n");
						fw.write(entry2.getKey()+ "\n");
						fw.write(entry2.getValue()+ "\n");
					}

				}
				fw.close();
			}
			else{
				FileWriter fw = new FileWriter("evaluationNeo4jResults/"+year+"-dataset"+dataSet+".stat");
				for(Entry<Integer,Map<String, String>> entry : neo4jwsc.bestResults.entrySet()){
					for(Entry<String,String> entry2: entry.getValue().entrySet()){
						fw.write(neo4jwsc.bestResultsTimes.get(entry.getKey())+"\n");
						fw.write(entry2.getKey()+ "\n");
						fw.write(entry2.getValue()+ "\n");
					}

				}
				fw.close();
			}
		}
		FileWriter fw = new FileWriter("timeRecord.txt");
		for(Entry<String, Long> entry : neo4jwsc.records.entrySet()){
			fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		}
		fw.close();
		neo4jwsc.setRunning(false);  
	}


	private static void printResult(Map<List<Node>, Map<String, Map<String, Double>>> resultWithQos) {
		System.out.println("Best result: ");
		Transaction tx = subGraphDatabaseService.beginTx();


		try{
			for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry2 : resultWithQos.entrySet()){
				for(Node n: entry2.getKey()){
					System.out.print(n.getProperty("name")+"--"+n.getId()+"   ");
				}
				System.out.println();
				System.out.print("QOS:  ");
				for (Map.Entry<String,Map<String, Double>> entry3 : entry2.getValue().entrySet()){
					if(entry3.getKey().equals("normalized")){
						System.out.println("normalized: ");
						for (Map.Entry<String, Double> entry4 : entry3.getValue().entrySet()){
							System.out.print(entry4.getKey()+": "+entry4.getValue()+"     ");

						}
						System.out.println();
						double fitnessOfBest = m_a*entry3.getValue().get("A") + m_r*entry3.getValue().get("R") + m_c*entry3.getValue().get("C") + m_t*entry3.getValue().get("T");
						System.out.println("fitnessOfBest:" +fitnessOfBest);
					}
					else if(entry3.getKey().equals("non_normalized")){
						System.out.println("non_normalized: ");
						for (Map.Entry<String, Double> entry4 : entry3.getValue().entrySet()){
							System.out.print(entry4.getKey()+": "+entry4.getValue()+"     ");

						}
					}


				}
			}
			System.out.println();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("print populations error.."); 
		} finally {
			tx.close();
		}			
	}


	private static Map<List<Node>, Map<String, Map<String, Double>>> findCompositions() {
		FindCompositions findCompositions = new FindCompositions(candidateSize, individuleNodeSize, subGraphDatabaseService);
		findCompositions.setStartNode(startNode);
		findCompositions.setEndNode(endNode);
		findCompositions.setNeo4jServNodes(neo4jServNodes);
		findCompositions.setTaxonomyMap(taxonomyMap);
		findCompositions.setSubGraphNodesMap(subGraphNodesMap);
		findCompositions.setM_a(m_a);
		findCompositions.setM_r(m_r);
		findCompositions.setM_c(m_c);
		findCompositions.setM_t(m_t);
		Map<List<Node>, Map<String, Map<String, Double>>> candidates = null;
		try {
			candidates = findCompositions.run();
		} catch (OuchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Transaction transaction = subGraphDatabaseService.beginTx();
		try{			
			System.out.println("candidates: ");
			int i = 0;
			for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : candidates.entrySet()){

				System.out.println();
				System.out.println();
				System.out.print("candidate "+ ++i+": ");

				for(Node n: entry.getKey()){
					System.out.print(n.getProperty("name")+"  ");
				}
				System.out.println();
				System.out.println("Total service nodes:"+entry.getKey().size());
				for (Map.Entry<String,Map<String, Double>> entry2 : entry.getValue().entrySet()){
					System.out.println(entry2.getKey()+": ");
					for (Map.Entry<String, Double> entry3 : entry2.getValue().entrySet()){
						System.out.print("    "+entry3.getKey()+": "+entry3.getValue()+";   ");
					}
					System.out.println();
				}
				System.out.println();
			}

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("print populations error.."); 
		} finally {
			transaction.close();
		}	
		return findCompositions.getResult(candidates);
	}


	private static void reduceDB() {
		ReduceGraphDb reduceGraphDb = new ReduceGraphDb(graphDatabaseService);
		reduceGraphDb.setStartNode(startNode);
		reduceGraphDb.setEndNode(endNode);
		reduceGraphDb.setNeo4jServNodes(neo4jServNodes);
		reduceGraphDb.setTaxonomyMap(taxonomyMap);		
		reduceGraphDb.setServiceMap(serviceMap);

		Set<Node> relatedNodes = new HashSet<Node>();;
		reduceGraphDb.findAllReleatedNodes(relatedNodes, false);
		System.out.println(relatedNodes.size());
		reduceGraphDb.createNodes(relatedNodes);
		reduceGraphDb.createRel();
		relatedNodes = reduceGraphDb.getRelatedNodes();
		startNode = reduceGraphDb.getStartNode();
		endNode = reduceGraphDb.getEndNode();
		subGraphDatabaseService = reduceGraphDb.getSubGraphDatabaseService();		
		subGraphNodesMap = reduceGraphDb.getSubGraphNodesMap();
	}


	private static void runTask(String path) {

		RunTask runtask = new RunTask(path);
		runtask.setServiceNodes(serviceNodes);
		runtask.setTaxonomyMap(taxonomyMap);
		runtask.setServiceNodes(serviceNodes);
		runtask.setTaskInputs(loadFiles.getTaskInputs());
		runtask.setTaskOutputs(loadFiles.getTaskOutputs());
		runtask.copyDb();
		runtask.createTempDb();
		graphDatabaseService = runtask.getTempGraphDatabaseService();
		registerShutdownHook(graphDatabaseService, "Temp");
		neo4jServNodes.clear();
		neo4jServNodes = runtask.getNeo4jServNodes();
		tempServices = runtask.getTempServices();
		runtask.addStartEndNodes();
		startNode = runtask.getStartNode();
		endNode = runtask.getEndNode();
		runtask.createRel(startNode);
		runtask.createRel(endNode);		
	}


	private void generateDB(List<Node> nodes, String dbpath, String string, String databaseName) {

		String path = "";
		if(databaseName == null){
			path = dbpath;
		}else{
			path = dbpath+"  "+databaseName;
		}
		GenerateDatabase generateDatabase = new GenerateDatabase(null, null,path);
		generateDatabase.createDbService();
		graphDatabaseService = generateDatabase.getGraphDatabaseService();
		registerShutdownHook(graphDatabaseService, string);
		generateDatabase.setServiceMap(serviceMap);
		generateDatabase.setTaxonomyMap(taxonomyMap);
		generateDatabase.createServicesDatabase();
		generateDatabase.addServiceNodeRelationShip();
		neo4jServNodes.clear();
		neo4jServNodes = generateDatabase.getNeo4jServNodes();
		Transaction transaction = graphDatabaseService.beginTx();
		try{
			index = graphDatabaseService.index();
			services = index.forNodes( "identifiers" );
			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Main set index and services error.."); 
		} finally {
			transaction.close();
		}			
	}


	private static void removeRedundantRel(GraphDatabaseService graphDatabaseService) {
		List<Relationship>toRemove = new ArrayList<Relationship>();
		Transaction transaction = graphDatabaseService.beginTx();
		try{
			Iterable<Node> nodes = graphDatabaseService.getAllNodes();
			for(Node node: nodes){
				if(!node.getProperty("name").equals("start")){
					Iterable<Relationship> rels = node.getRelationships(Direction.INCOMING);
					for(Relationship r: rels){
						Transaction tt = graphDatabaseService.beginTx();
						r.setProperty("removeable", true);
						System.out.println("set: "+node.getProperty("name")+"  removeable to true");

						tt.success();
						tt.close();
						if(isAllNodesFulfilled(node, nodes,graphDatabaseService)){
//							Transaction t = graphDatabaseService.beginTx();
//							r.delete();
//							System.out.println("eeee: "+r.getId());
//							t.success();
//							t.close();
							toRemove.add(r);

						}else{
							Transaction ttt = graphDatabaseService.beginTx();
							System.out.println("set: "+node.getProperty("name")+"  removeable to false");
							r.setProperty("removeable", false);
							ttt.success();
							ttt.close();
						}
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Main removeRedundantRel"); 
		} finally {
			transaction.close();
		}			
		for(Relationship r: toRemove){
			Transaction t = graphDatabaseService.beginTx();
			System.out.println("remove: "+r.getId());
			r.delete();
			t.success();
			t.close();
		}
	}


	private static boolean isAllNodesFulfilled(Node node, Iterable<Node> nodes, GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		System.out.println(node.getProperty("name"));
		Set<String> inputs = new HashSet<String>();
		Set<String> nodeInputs = new HashSet<String>();
		nodeInputs.addAll(Arrays.asList(getNodePropertyArray(node,"inputs")));

		Iterable<Relationship> rels = node.getRelationships(Direction.INCOMING);
		for(Relationship r: rels){
			System.out.println(r.getId());
			System.out.println(r.getProperty("removeable"));
			if(!(boolean) r.getProperty("removeable")){
				inputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r, "outputs")));
			}
		}
		for(Node n: nodes){
			if(!n.getProperty("name").equals("start") &&!n.getProperty("name").equals("end") ){
				Iterable<Relationship> relationships = n.getRelationships(Direction.OUTGOING);
				int i = 0;
				for(Relationship r: relationships){
					if(!(boolean) r.getProperty("removeable"))
						i++;
				}
				if(i==0){
					transaction.close();
					//node not start node
					//node has no output relationship
					return false;
				}
			}

		}
		if(equalLists(inputs, nodeInputs)){
			transaction.close();
			return true;
		}
		return false;

	}
	public static boolean equalLists(Set<String> one, Set<String> two){     

		List<String>one1 = new ArrayList<String>(one); 
		List<String>two2 = new ArrayList<String>(two);  
		List<String>one11 = new ArrayList<String>(one); 
		List<String>two22 = new ArrayList<String>(two);
		one1.retainAll(two2);
		two22.retainAll(one11);

		if (one1.size()>0 && two22.size()>0 && two22.size()==two.size() && one1.size()==one.size() && one11.size()>0 && two2.size()>0){
			return true;
		}
		else return false;
	}

	@SuppressWarnings("deprecation")
	private static void loadExistingDB() {
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath+""+databaseName);
		registerShutdownHook(graphDatabaseService, "exist original");
		Transaction transaction = graphDatabaseService.beginTx();
		index = graphDatabaseService.index();
		services = index.forNodes( "identifiers" );
		transaction.success();
		transaction.close();
		signNodesToField(neo4jServNodes, graphDatabaseService);		
	}


	private static void populateTaxonomytree() {
		PopulateTaxonomyTree populateTaxonomyTree = new PopulateTaxonomyTree();
		populateTaxonomyTree.setTaxonomyMap(taxonomyMap);
		populateTaxonomyTree.setServiceMap(serviceMap);
		populateTaxonomyTree.populateTaxonomyTree();		
	}


	private static void loadFiles() {
		loadFiles = new LoadFiles(serviceFileName,taxonomyFileName, taskFileName);
		loadFiles.runLoadFiles();
		taxonomyMap = loadFiles.getTaxonomyMap();
		serviceMap = loadFiles.getServiceMap();
		//		neo4jwsc.taskInputs = loadFiles.getTaskInputs();
		loadFiles.getTaskOutputs();
		serviceNodes = loadFiles.getServiceNodes();		
	}


	private static void signNodesToField(Map<String, Node> neo4jServNodes, GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		@SuppressWarnings("deprecation")
		Iterable<Node> nodes = graphDatabaseService.getAllNodes();
		neo4jServNodes.clear();
		int i = 0;
		for(Node n: nodes){
			i++;
			neo4jServNodes.put((String)n.getProperty("name"), n);
		}
		System.out.println("total service nodes: "+i);
		transaction.success();
		transaction.close();
	}
	public void run() {
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

	private static void registerShutdownHook(GraphDatabaseService graphDatabaseService,String database ) {
		// Registers a shutdown hook for the Neo4j instance so that it  
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the  
		// running example before it's completed)
		Runtime.getRuntime()
		.addShutdownHook( new Thread()  
		{  
			@Override  
			public void run()  
			{  
				System.out.println("neo4j graph database shutdown hook ("+database+")... ");  
				graphDatabaseService.shutdown();
			}  
		} );  
	}  
	private static String[] getNodeRelationshipPropertyArray(Relationship relationship, String property){
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
	private static String[] getNodePropertyArray(Node sNode, String property){
		Object obj =sNode.getProperty(property);
		//    		//remove the "[" and "]" from string
		String[] array = new String[0];

		String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
		String[] tempInputs = ips.split("\\s*,\\s*");

		for(String s: tempInputs){
			if(s.length()>0){
				array =increaseArray(array);
				array[array.length-1] = s;
			}
		}
		return array;
	}
	private static String[] increaseArray(String[] theArray)
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
}
