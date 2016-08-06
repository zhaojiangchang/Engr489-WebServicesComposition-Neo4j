
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
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
import task.ReduceGraphDb;
import task.RunTask;


//public class Main {
public class Main implements Runnable{
	private String serviceFileName = null;
	private String taxonomyFileName = null;
	private String taskFileName = null;

	private final String Neo4j_testServicesDBPath = "database/test_services";
	private final String Neo4j_ServicesDBPath = "database/";
	private boolean running = true;
	private Map<String,Long>records = new HashMap<String,Long>();
	private Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;

	private static GraphDatabaseService graphDatabaseService = null;
	private static GraphDatabaseService subGraphDatabaseService = null;

	private Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	private Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private IndexManager index = null;
	private Index<Node> services = null;
	private Index<Node> tempServices = null;
	private Node endNode = null;
	private Node startNode = null;
	private Set<String> taskInputs;
	private Set<String> taskOutputs;
	private Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();
	private String databaseName = "";

	//For setup == file location, composition size, and run test file or not
	//******************************************************//
	private final boolean runTestFiles = true;
	private final String year = "2008";
	private final String dataSet = "01";
	private final int compositionSize = 32;
	private final int totalCompositions = 50;
	private final boolean runQosDataset = true;

	//******************************************************//


	public static void main( String[] args ) throws IOException{
		Main neo4jwsc = new Main();
		Thread t = new Thread(neo4jwsc,"Neo4jThread");  
		t.start();
		neo4jwsc.databaseName = "wsc"+neo4jwsc.year+"dataset"+neo4jwsc.dataSet;
		String path;
		if(!neo4jwsc.runTestFiles){
			if(!neo4jwsc.runQosDataset){
				neo4jwsc.serviceFileName = "dataset/wsc"+neo4jwsc.year+"/Set"+neo4jwsc.dataSet+"MetaData/services.xml";
				neo4jwsc.taxonomyFileName = "dataset/wsc"+neo4jwsc.year+"/Set"+neo4jwsc.dataSet+"MetaData/taxonomy.xml";
				neo4jwsc.taskFileName = "dataset/wsc"+neo4jwsc.year+"/Set"+neo4jwsc.dataSet+"MetaData/problem.xml";
			}else{
				neo4jwsc.serviceFileName = "dataset/dataset/Set"+neo4jwsc.dataSet+"MetaData/services-output.xml";
				neo4jwsc.taxonomyFileName = "dataset/dataset/Set"+neo4jwsc.dataSet+"MetaData/taxonomy.xml";
				neo4jwsc.taskFileName = "dataset/dataset/Set"+neo4jwsc.dataSet+"MetaData/problem.xml";
			}
			

		}else{
			neo4jwsc.serviceFileName = "dataset/test/test_serv.xml";
			neo4jwsc.taxonomyFileName = "dataset/test/test_taxonomy.xml";
			neo4jwsc.taskFileName = "dataset/test/test_problem.xml";
		}
		//load files
		neo4jwsc.records.put("TIME: ", System.currentTimeMillis());
		long startTime = System.currentTimeMillis();
		LoadFiles loadFiles = new LoadFiles(neo4jwsc.serviceFileName,neo4jwsc.taxonomyFileName, neo4jwsc.taskFileName);
		loadFiles.runLoadFiles();
		neo4jwsc.taxonomyMap = loadFiles.getTaxonomyMap();
		neo4jwsc.serviceMap = loadFiles.getServiceMap();
		neo4jwsc.taskInputs = loadFiles.getTaskInputs();
		neo4jwsc.taskOutputs = loadFiles.getTaskOutputs();
		neo4jwsc.serviceNodes = loadFiles.getServiceNodes();
		long endTime = System.currentTimeMillis();
		neo4jwsc.records.put("Load files", endTime - startTime);
		System.out.println("Load files Total execution time: " + (endTime - startTime) );


		startTime = System.currentTimeMillis();
		PopulateTaxonomyTree populateTaxonomyTree = new PopulateTaxonomyTree();
		populateTaxonomyTree.setTaxonomyMap(neo4jwsc.taxonomyMap);
		populateTaxonomyTree.setServiceMap(neo4jwsc.serviceMap);
		populateTaxonomyTree.populateTaxonomyTree();
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("Populate Taxonomy Tree", endTime - startTime);
		System.out.println("Populate Taxonomy Tree total execution time: " + (endTime - startTime) );


		//Identify run test file or not
		//if database exist read database
		//else if database not exist create database
		if(!neo4jwsc.runTestFiles){
			boolean dbExist;
			File f = new File(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databaseName +"/index");
			if (f.exists() && f.isDirectory()) {
				dbExist = true;
			}else{
				dbExist = false;   
			}
			if(dbExist){

				startTime = System.currentTimeMillis();
				graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databaseName);
				registerShutdownHook();
				Transaction transaction = graphDatabaseService.beginTx();
				neo4jwsc.index = graphDatabaseService.index();
				neo4jwsc.services = neo4jwsc.index.forNodes( "identifiers" );
				transaction.success();
				transaction.close();
				signNodesToField(neo4jwsc.neo4jServNodes, graphDatabaseService);
				endTime = System.currentTimeMillis();
				neo4jwsc.records.put("Load existing db", endTime - startTime);
				System.out.println("Load existing db Total execution time: " + (endTime - startTime) );

			}else{

				startTime = System.currentTimeMillis();
				GenerateDatabase generateDatabase = new GenerateDatabase(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databaseName);
				generateDatabase.createDbService();
				graphDatabaseService = generateDatabase.getGraphDatabaseService();
				registerShutdownHook();
				generateDatabase.setServiceMap(neo4jwsc.serviceMap);
				generateDatabase.setTaxonomyMap(neo4jwsc.taxonomyMap);
				generateDatabase.createServicesDatabase(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databaseName);
				generateDatabase.addServiceNodeRelationShip();
				neo4jwsc.neo4jServNodes.clear();
				neo4jwsc.neo4jServNodes = generateDatabase.getNeo4jServNodes();
				Transaction transaction = graphDatabaseService.beginTx();
				try{
					neo4jwsc.index = graphDatabaseService.index();
					neo4jwsc.services = neo4jwsc.index.forNodes( "identifiers" );
					transaction.success();
				} catch (Exception e) {
					System.out.println(e);
					System.out.println("Main set index and services error.."); 
				} finally {
					transaction.close();
				}		
				endTime = System.currentTimeMillis();
				neo4jwsc.records.put("Create new db", endTime - startTime);
				System.out.println("Create new db Total execution time: " + (endTime - startTime) );

			}
			path = neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databaseName;
		}
		else{

			startTime = System.currentTimeMillis();
			try {
				FileUtils.deleteRecursively(new File(neo4jwsc.Neo4j_testServicesDBPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			GenerateDatabase generateDatabase = new GenerateDatabase(neo4jwsc.Neo4j_testServicesDBPath);
			generateDatabase.createDbService();
			graphDatabaseService = generateDatabase.getGraphDatabaseService();
			registerShutdownHook();
			generateDatabase.setServiceMap(neo4jwsc.serviceMap);
			generateDatabase.setTaxonomyMap(neo4jwsc.taxonomyMap);
			generateDatabase.createServicesDatabase(neo4jwsc.Neo4j_testServicesDBPath);
			generateDatabase.addServiceNodeRelationShip();
			neo4jwsc.neo4jServNodes.clear();
			neo4jwsc.neo4jServNodes = generateDatabase.getNeo4jServNodes();

			Transaction transaction = graphDatabaseService.beginTx();
			try{
				neo4jwsc.index = graphDatabaseService.index();
				neo4jwsc.services = neo4jwsc.index.forNodes( "identifiers" );
				transaction.success();
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Main test set index and services error.."); 
			} finally {
				transaction.close();
			}				
			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("Create new test db", endTime - startTime);
			System.out.println("Create new test db Total execution time: " + (endTime - startTime) );

			path = neo4jwsc.Neo4j_testServicesDBPath;
		}

		//run task
		//1: copy database and call->tempServiceDatabase
		//2: connect to tempServiceDatabase
		//3: use task inputs outputs create start and end nodes and link to tempservicedatabase

		startTime = System.currentTimeMillis();

		RunTask runtask = new RunTask(path);
		runtask.setServiceNodes(neo4jwsc.serviceNodes);
		runtask.setTaxonomyMap(neo4jwsc.taxonomyMap);
		runtask.setServiceNodes(neo4jwsc.serviceNodes);
		runtask.setTaskInputs(loadFiles.getTaskInputs());
		runtask.setTaskOutputs(loadFiles.getTaskOutputs());

		runtask.copyDb();
		runtask.createTempDb();
		graphDatabaseService = runtask.getTempGraphDatabaseService();
		registerShutdownHook();
		neo4jwsc.neo4jServNodes.clear();
		neo4jwsc.neo4jServNodes = runtask.getNeo4jServNodes();
		neo4jwsc.tempServices = runtask.getTempServices();
		runtask.addStartEndNodes();
		neo4jwsc.startNode = runtask.getStartNode();
		neo4jwsc.endNode = runtask.getEndNode();
		runtask.createRel(neo4jwsc.startNode);
		runtask.createRel(neo4jwsc.endNode);

		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("run task: copied db, create temp db, add start and end nodes", endTime - startTime);
		System.out.println("run task: copied db, create temp db, add start and end nodes Total execution time: " + (endTime - startTime) );

		//reduce database use copied database

		startTime = System.currentTimeMillis();
		ReduceGraphDb reduceGraphDb = new ReduceGraphDb(graphDatabaseService);
		reduceGraphDb.setStartNode(neo4jwsc.startNode);
		reduceGraphDb.setEndNode(neo4jwsc.endNode);
		reduceGraphDb.setNeo4jServNodes(neo4jwsc.neo4jServNodes);
		reduceGraphDb.setTaxonomyMap(neo4jwsc.taxonomyMap);		
		reduceGraphDb.setServiceMap(neo4jwsc.serviceMap);
		
		Set<Node> relatedNodes = new HashSet<Node>();;
		reduceGraphDb.findAllReleatedNodes(relatedNodes, false);
		System.out.println(relatedNodes.size());
		reduceGraphDb.createNodes(relatedNodes);
		reduceGraphDb.createRel();
		relatedNodes = reduceGraphDb.getRelatedNodes();
		neo4jwsc.startNode = reduceGraphDb.getStartNode();
		neo4jwsc.endNode = reduceGraphDb.getEndNode();
		subGraphDatabaseService = reduceGraphDb.getSubGraphDatabaseService();

		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("reduce graph db ", endTime - startTime);
		System.out.println("reduce graph db Total execution time: " + (endTime - startTime) );

		//find compositions
		startTime = System.currentTimeMillis();
		FindCompositions findCompositions = new FindCompositions(neo4jwsc.totalCompositions, neo4jwsc.compositionSize,subGraphDatabaseService);
		findCompositions.setEndNode(neo4jwsc.endNode);
		findCompositions.setTaxonomyMap(neo4jwsc.taxonomyMap);
		findCompositions.setSubGraphNodesMap(reduceGraphDb.getSubGraphNodesMap());
		Set<Set<Node>> candidates = findCompositions.run();
		Transaction transaction = null;
		transaction = subGraphDatabaseService.beginTx();
		try{
			for(Set<Node> pop: candidates){
				System.out.println();
				for(Node n: pop){
					System.out.print(n.getProperty("name")+"  ");
				}
				System.out.println();
				System.out.println("composition size: "+pop.size());
			}
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("print populations error.."); 
		} finally {
			transaction.close();
		}		
		
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("generate compositions", endTime - startTime);
		System.out.println("generate compositions Total execution time: " + (endTime - startTime) );
		FileWriter fw = new FileWriter("timeRecord.txt");
		for(Entry<String, Long> entry : neo4jwsc.records.entrySet()){
			fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		}
		fw.close();

		neo4jwsc.setRunning(false);  
	}


	private static void signNodesToField(Map<String, Node> neo4jServNodes, GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
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

	private static void registerShutdownHook() {
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
