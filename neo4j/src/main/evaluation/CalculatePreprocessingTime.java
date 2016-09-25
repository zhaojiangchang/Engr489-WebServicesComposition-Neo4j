package evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import task.OuchException;
import task.ReduceGraphDb;
import task.RunTask;


//public class Main {
public class CalculatePreprocessingTime implements Runnable{
	private static String serviceFileName = null;
	private static String taxonomyFileName = null;
	private static String taskFileName = null;

	private final static String Neo4j_testServicesDBPath = "database/test_services";
	private final static String Neo4j_ServicesDBPath = "database/";

	private boolean running = true;
	private static Map<Integer,Long>preprocessTimes = new HashMap<Integer,Long>();
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
	private static String year = "2008";
	private static String dataSet = "01";
	private static long costForCopyDBAndCreateTempDB;
	//	private final static int individuleNodeSize = 12;
	//	private final static int candidateSize = 50;
	private final boolean runQosDataset = true;


	//******************************************************//


	public static void main( String[] args ) throws IOException, OuchException{

		//run 8 dataset from year 2008 dataset from 01 to 08
		//will write to Folder preprocessTimeRunForEachDataset
		//file name format: 2008-dataset01-preprocessTime.txt
		//file format: index  timeCost
		for(int j = 7; j<=7; j++){
			CalculatePreprocessingTime neo4jwsc = new CalculatePreprocessingTime();
			preprocessTimes = new HashMap<Integer,Long>();
			neo4jServNodes = new HashMap<String, Node>();
			subGraphNodesMap = new HashMap<String, Node>();;
			serviceMap = new HashMap<String, ServiceNode>();
			taxonomyMap = new HashMap<String, TaxonomyNode>();
			serviceNodes = new HashSet<ServiceNode>();
			graphDatabaseService = null;
			subGraphDatabaseService = null;
			dataSet = "0"+j;
			year = "2008";
			databaseName = "wsc"+year+"dataset"+dataSet;
			String path;

			if(!neo4jwsc.runQosDataset){
				serviceFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/services.xml";
				taxonomyFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/taxonomy.xml";
				taskFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/problem.xml";
			}else{
				serviceFileName = "dataset/dataset/Set"+dataSet+"MetaData/services-output.xml";
				taxonomyFileName = "dataset/dataset/Set"+dataSet+"MetaData/taxonomy.xml";
				taskFileName = "dataset/dataset/Set"+dataSet+"MetaData/problem.xml";
			}
			
			System.out.println();
			System.out.println();
			System.out.println();

			System.out.println("dataset: "+ dataSet);
			int i = 1;
			while(i<31){
				neo4jServNodes = new HashMap<String, Node>();
				subGraphNodesMap = new HashMap<String, Node>();;
				serviceMap = new HashMap<String, ServiceNode>();
				taxonomyMap = new HashMap<String, TaxonomyNode>();
				serviceNodes = new HashSet<ServiceNode>();
				graphDatabaseService = null;
				subGraphDatabaseService = null;
				//				Thread t = new Thread(neo4jwsc,"Neo4jThread");  
				//				t.start();
				//load files
				System.out.println("run: "+ i);
				long startTime = System.currentTimeMillis();
				loadFiles();	
				populateTaxonomytree();	

				//if database exist read database
				//else if database not exist create database
				boolean dbExist;
				File f = new File(Neo4j_ServicesDBPath+""+databaseName +"/index");
				if (f.exists() && f.isDirectory()) {
					dbExist = true;
				}else{
					dbExist = false;   
				}
				if(dbExist){
					loadExistingDB();
				}else{
					generateDB(null,Neo4j_ServicesDBPath,"original",databaseName);
				}
				path = Neo4j_ServicesDBPath+""+databaseName;
				
				
				//run task
				//1: copy database and call->tempServiceDatabase
				//2: connect to tempServiceDatabase
				//3: use task inputs outputs create start and end nodes and link to tempservicedatabase
				runTask(path);	
				reduceDB();
				long endTime = System.currentTimeMillis();
				long cost = endTime- startTime - costForCopyDBAndCreateTempDB;
				preprocessTimes.put(i, cost);
				System.out.println("costForCopyDBAndCreateTempDB:  "+costForCopyDBAndCreateTempDB);
				System.out.println("preprocess time:  " + cost );
				i++;
				graphDatabaseService.shutdown();
				subGraphDatabaseService.shutdown();
//				neo4jwsc.setRunning(false);  
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} // do nothing for 1000 miliseconds (1 second)
			}
			FileWriter fw = new FileWriter("preprocessTimeRunForEachDataset/"+year+"-dataset"+dataSet+".stat");
			for(Entry<Integer, Long> entry : preprocessTimes.entrySet()){
				fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");

			}
			fw.close();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} // do nothing for 1000 miliseconds (10 second)
		}

		

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
		long startTime = System.currentTimeMillis();
		runtask.copyDb();
		runtask.createTempDb();
		long endTime = System.currentTimeMillis();
		costForCopyDBAndCreateTempDB = endTime - startTime;
		graphDatabaseService.shutdown();
		graphDatabaseService = runtask.getTempGraphDatabaseService();
		//		registerShutdownHook(graphDatabaseService, "Temp");
		neo4jServNodes.clear();
		neo4jServNodes = runtask.getNeo4jServNodes();
		tempServices = runtask.getTempServices();
		runtask.addStartEndNodes();
		startNode = runtask.getStartNode();
		endNode = runtask.getEndNode();
		runtask.createRel(startNode);
		runtask.createRel(endNode);		
	}


	private static void generateDB(List<Node> nodes, String dbpath, String string, String databaseName) {

		String path = "";
		if(databaseName == null){
			path = dbpath;
		}else{
			path = dbpath+"  "+databaseName;
		}
		GenerateDatabase generateDatabase = new GenerateDatabase(null, null,path);
		generateDatabase.createDbService();
		graphDatabaseService = generateDatabase.getGraphDatabaseService();
		//		registerShutdownHook(graphDatabaseService, string);
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


	@SuppressWarnings("deprecation")
	private static void loadExistingDB() {
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath+""+databaseName);
		//		registerShutdownHook(graphDatabaseService, "exist original");
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
}
