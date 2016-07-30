
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.neo4j.GenerateDatabase;
import org.neo4j.neo4j.LoadFiles;
import org.neo4j.neo4j.PopulateTaxonomyTree;

import component.ServiceNode;
import component.TaxonomyNode;
import task.FindComposition;
import task.ReduceGraphDb;
import task.RunTask;


//public class Main {
	public class Main implements Runnable{
	private String serviceFileName = null;
	private String taxonomyFileName = null;
	private String taskFileName = null;

	private final String Neo4j_testServicesDBPath = "database/test_services";
	private final String Neo4j_ServicesDBPath = "database/";
	private final String Neo4j_tempDBPath = "database/temp_graph";
	private boolean running = true;  
	public Map<String,Long>records = new HashMap<String,Long>();
	public Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;
	Node[] neo4jServiceNodes;

	public GraphDatabaseService graphDatabaseService;
	public GraphDatabaseService tempGraphDatabaseService;

	public Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	public Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	public IndexManager index = null;
	public Index<Node> services = null;
	public IndexManager tempIndex = null;
	public Index<Node> tempServices = null;
	private Node endNode = null;
	private Node startNode = null;
	public Set<String> taskInputs;
	public Set<String> taskOutputs;
	public Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();

	//For setup == file location, composition size, and run test file or not
	//******************************************************//
	private boolean runTestFiles = false;
	private String databasename = "wsc2008dataset08";
	private String dataset = "dataset/wsc2008/Set08MetaData/";
	private String testDataset = "dataset/test/";
	private final int compositionSize = 32;
	//******************************************************//


	public static void main( String[] args ) throws IOException{
		Main neo4jwsc = new Main();
		Thread t = new Thread(neo4jwsc,"Neo4jThread");  
		t.start();
		String path = null;
		if(!neo4jwsc.runTestFiles){
			neo4jwsc.serviceFileName = neo4jwsc.dataset+"services.xml";
			neo4jwsc.taxonomyFileName = neo4jwsc.dataset+"taxonomy.xml";
			neo4jwsc.taskFileName = neo4jwsc.dataset+"problem.xml";
		}else{
			neo4jwsc.serviceFileName = neo4jwsc.testDataset+"test_serv.xml";
			neo4jwsc.taxonomyFileName = neo4jwsc.testDataset+"test_taxonomy.xml";
			neo4jwsc.taskFileName = neo4jwsc.testDataset+"test_problem.xml";
		}
		//load files
		LoadFiles loadFiles = new LoadFiles(neo4jwsc.serviceFileName,neo4jwsc.taxonomyFileName, neo4jwsc.taskFileName);
		loadFiles.runLoadFiles();
		neo4jwsc.taxonomyMap = loadFiles.getTaxonomyMap();
		neo4jwsc.serviceMap = loadFiles.getServiceMap();
		neo4jwsc.taskInputs = loadFiles.getTaskInputs();
		neo4jwsc.taskOutputs = loadFiles.getTaskOutputs();
		neo4jwsc.serviceNodes = loadFiles.getServiceNodes();
		PopulateTaxonomyTree populateTaxonomyTree = new PopulateTaxonomyTree();
		populateTaxonomyTree.setTaxonomyMap(neo4jwsc.taxonomyMap);
		populateTaxonomyTree.setServiceMap(neo4jwsc.serviceMap);
		populateTaxonomyTree.populateTaxonomyTree();
		//Identify run test file or not
		//if database exist read database
		//else if database not exist create database
		if(!neo4jwsc.runTestFiles){
			boolean dbExist = false;
			File f = new File(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databasename+"/index");
			if (f.exists() && f.isDirectory()) {
				dbExist = true;
			}else{
				dbExist = false;   
			}
			if(dbExist){
				neo4jwsc.graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databasename);
				registerShutdownHook(neo4jwsc.graphDatabaseService);  
				Transaction transaction = neo4jwsc.graphDatabaseService.beginTx();
				neo4jwsc.index = neo4jwsc.graphDatabaseService.index();
				neo4jwsc.services = neo4jwsc.index.forNodes( "identifiers" );
				transaction.success();
				transaction.close();
				signNodesToField(neo4jwsc.neo4jServNodes, neo4jwsc.graphDatabaseService);
			}else{
				
				GenerateDatabase generateDatabase = new GenerateDatabase(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databasename);
				generateDatabase.createDbService();
				neo4jwsc.graphDatabaseService = generateDatabase.getGraphDatabaseService();
				registerShutdownHook(neo4jwsc.graphDatabaseService);  
				generateDatabase.setServiceMap(neo4jwsc.serviceMap);
				generateDatabase.setTaxonomyMap(neo4jwsc.taxonomyMap);
				generateDatabase.createServicesDatabase(neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databasename);
				generateDatabase.addServiceNodeRelationShip();
				neo4jwsc.neo4jServNodes.clear();
				neo4jwsc.neo4jServNodes = generateDatabase.getNeo4jServNodes();
				Transaction transaction = neo4jwsc.graphDatabaseService.beginTx();
				neo4jwsc.index = neo4jwsc.graphDatabaseService.index();
				neo4jwsc.services = neo4jwsc.index.forNodes( "identifiers" );
				transaction.success();
				transaction.close();
			}
			path = neo4jwsc.Neo4j_ServicesDBPath+""+neo4jwsc.databasename;
		}
		else{
			try {
				FileUtils.deleteRecursively(new File(neo4jwsc.Neo4j_testServicesDBPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			GenerateDatabase generateDatabase = new GenerateDatabase(neo4jwsc.Neo4j_testServicesDBPath);
			generateDatabase.createDbService();
			neo4jwsc.graphDatabaseService = generateDatabase.getGraphDatabaseService();
			registerShutdownHook(neo4jwsc.graphDatabaseService);  
			generateDatabase.setServiceMap(neo4jwsc.serviceMap);
			generateDatabase.setTaxonomyMap(neo4jwsc.taxonomyMap);
			generateDatabase.createServicesDatabase(neo4jwsc.Neo4j_testServicesDBPath);
			generateDatabase.addServiceNodeRelationShip();
			neo4jwsc.neo4jServNodes.clear();
			neo4jwsc.neo4jServNodes = generateDatabase.getNeo4jServNodes();

			Transaction transaction = neo4jwsc.graphDatabaseService.beginTx();
			neo4jwsc.index = neo4jwsc.graphDatabaseService.index();
			neo4jwsc.services = neo4jwsc.index.forNodes( "identifiers" );
			transaction.success();
			transaction.close();
			path = neo4jwsc.Neo4j_testServicesDBPath;
		}
		//		FileWriter fw = new FileWriter("timeRecord.txt");
		//		for(Entry<String, Long> entry : records.entrySet()){
		//			fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		//		}
		//		fw.close();
		//		neo4jwsc.shutdown(graphDatabaseService);

		//run task
		//1: copy database and call->tempServiceDatabase
		//2: connect to tempServiceDatabase
		//3: use task inputs outputs create start and end nodes and link to tempservicedatabase
		RunTask runtask = new RunTask(path);
//		runtask.setNeo4jServNodes(neo4jwsc.neo4jServNodes);
		runtask.setServiceNodes(neo4jwsc.serviceNodes);
		runtask.setTaxonomyMap(neo4jwsc.taxonomyMap);
		runtask.setServiceNodes(neo4jwsc.serviceNodes);
		runtask.setTaskInputs(loadFiles.getTaskInputs());
		runtask.setTaskOutputs(loadFiles.getTaskOutputs());
		runtask.copyDb();
		runtask.createTempDb();
		neo4jwsc.tempGraphDatabaseService = runtask.getTempGraphDatabaseService();
		registerShutdownHook(neo4jwsc.tempGraphDatabaseService);  
		neo4jwsc.neo4jServNodes.clear();
		neo4jwsc.neo4jServNodes = runtask.getNeo4jServNodes();
		neo4jwsc.tempServices = runtask.getTempServices();
		runtask.addStartEndNodes();
		neo4jwsc.startNode = runtask.getStartNode();
		neo4jwsc.endNode = runtask.getEndNode();
		runtask.createRel(neo4jwsc.startNode);
		runtask.createRel(neo4jwsc.endNode);


		//reduce database use copied database
		ReduceGraphDb reduceGraphDb = new ReduceGraphDb(neo4jwsc.neo4jServNodes,neo4jwsc.tempGraphDatabaseService);
		reduceGraphDb.setStartNode(neo4jwsc.startNode);
		reduceGraphDb.setEndNode(neo4jwsc.endNode);
		reduceGraphDb.runReduceGraph();


		//find compositions
		Set<Set<Node>> populations = new HashSet<Set<Node>>();
		while(populations.size()<10){
			FindComposition findComposition = new FindComposition(neo4jwsc.compositionSize, neo4jwsc.tempGraphDatabaseService);

			findComposition.setStartNode(neo4jwsc.startNode);
			findComposition.setEndNode(neo4jwsc.endNode);
			findComposition.setNeo4jServNodes(neo4jwsc.neo4jServNodes);
			findComposition.setTaxonomyMap(neo4jwsc.taxonomyMap);
			System.out.println(reduceGraphDb.getSubGraphNodesMap().size());
			findComposition.setSubGraphNodesMap(reduceGraphDb.getSubGraphNodesMap());
			findComposition.runComposition();
			populations.add(findComposition.getPopulation());

		}
		neo4jwsc.setRunning(false);  
	}


	private static void signNodesToField(Map<String, Node> neo4jServNodes, GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		Iterable<Node> nodes = graphDatabaseService.getAllNodes();
		neo4jServNodes.clear();
		for(Node n: nodes){
			neo4jServNodes.put((String)n.getProperty("name"), n);
		}
		transaction.success();
		transaction.close();
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
