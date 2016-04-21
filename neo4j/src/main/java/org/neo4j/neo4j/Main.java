package org.neo4j.neo4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.neo4j.cypher.internal.compiler.v2_2.planDescription.Children;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.io.fs.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;


/**
 * Hello world!
 *
 */
public class Main 
{
	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;
	private static String serviceFileName = "services-output.xml";
	private static String taxonomyFileName = "taxonomy.xml";
	public static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	public static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private static final String Neo4j_ServicesDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/services";
	private static final String Neo4j_TaxonomyDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/taxonomy";
	Node[] neo4jServiceNodes;
	Node[] neo4jTaxonomyNodes;
	Relationship relation;
	GraphDatabaseService graphDatabaseService;
	GraphDatabaseService graphDatabaseTaxonomy;
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT
	}
    public static void main( String[] args )
    {
    	Main neo4jwsc = new Main();
    	try {
			FileUtils.deleteRecursively(new File(Neo4j_TaxonomyDBPath));
			FileUtils.deleteRecursively(new File(Neo4j_ServicesDBPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      	neo4jwsc.parseWSCTaxonomyFile(taxonomyFileName);
      	neo4jwsc.addParentNodesForEachTaxonomyNode(taxonomyMap);
    	neo4jwsc.createTaxonomyDatabase(taxonomyMap);
    	neo4jwsc.parseWSCServiceFile(serviceFileName);
    	neo4jwsc.createServicesDatabase(serviceMap);
  
    	neo4jwsc.shutdown();
    	
    }
	private void createServicesDatabase(Map<String, ServiceNode> serviceMap){
    	graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath);
    	
    	Transaction transaction = graphDatabaseService.beginTx();
    	
    	neo4jServiceNodes = new Node[0];
    	try{
    		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
    		    String key = entry.getKey();
    		    ServiceNode value = entry.getValue();
    		    Node service = graphDatabaseService.createNode();
    		    Label nodeLable = DynamicLabel.label(key);
    		    service.addLabel(nodeLable);
    		    service.setProperty("name", key);
    		    service.setProperty("qos", value.getQos());
    		    service.setProperty("inputs", value.getInputs());
    		    service.setProperty("outputs", value.getOutputs());
    		    neo4jServiceNodes = increaseNodeArray(neo4jServiceNodes);
    		    neo4jServiceNodes[neo4jServiceNodes.length-1] =service;
    		}
    		System.out.println("web service nodes created");
    		//for each node use inputs and all inputs' subchildren find all other nodes
    		
    		for(int j= 0; j<neo4jServiceNodes.length; j++){
    			//use node's inputs find out all other nodes' outputs and create relationships
    			createRelationshipsInputs(neo4jServiceNodes[j]);
    			//use node's outputs find out all other nodes' inputs and create relationships
    			createRelationshipsOutputs(neo4jServiceNodes[j]);
    			System.out.println(j);
        		transaction.success();
    		}
    	}finally{
    		transaction.finish();
    	}
  	}
	private void createRelationshipsOutputs(Node serviceNode) {
		// TODO Auto-generated method stub
		//for each node use outputs and all outputs' parents nodes find all other nodes
  		Object outputsFromGdb =serviceNode.getProperty("outputs");
		//remove the "[" and "]" from string
		String ops = Arrays.toString((String[]) outputsFromGdb).substring(1, Arrays.toString((String[]) outputsFromGdb).length()-1);
		String[] outputs = ops.split("\\s*,\\s*");
		String[] tempOutputs = new String[0];
		Transaction tx = graphDatabaseTaxonomy.beginTx();
		for(int output = 0; output<outputs.length; output++){
			try ( 
    			    Result execResult = graphDatabaseTaxonomy.execute( "match (n{name: {o}}) return n", MapUtil.map("o",outputs[output])))
    			{
      				Iterator<Node> javaNodes = execResult.columnAs("n");
      				for (Node node : IteratorUtil.asIterable(javaNodes))
      				{	          					
      					String nodeParent = node.getProperty("parent").toString();
        			    Result resultParentNode = graphDatabaseTaxonomy.execute( "match (n{name: {o}}) return n", MapUtil.map("o",nodeParent));
        			    Iterator<Node> gNodes = resultParentNode.columnAs("n");
          				for (Node nd : IteratorUtil.asIterable(gNodes)){
          					Object subC = nd.getProperty("parents");
          					String subs = Arrays.toString((String[]) subC).substring(1, Arrays.toString((String[]) subC).length()-1);
      		    			String[] parentsFromTNode = subs.split("\\s*,\\s*");
          					for(int sc = 0; sc<parentsFromTNode.length; sc++){
          		    			tempOutputs = increaseArray(tempOutputs);
          		    			tempOutputs[tempOutputs.length-1] = parentsFromTNode[sc];
          					}
          				}
      				}  
    			}
		}
  		tx.finish();
		outputs = tempOutputs;
		//TODO: add all taxonomyGNode.children()
		for(int i = 0; i< outputs.length; i++){
			outputs[i] = outputs[i]+"_inst";
			try ( 
  			      Result execResult = graphDatabaseService.execute( "MATCH (n) WHERE ANY(input IN n.inputs  WHERE input =~ {o}) RETURN n", MapUtil.map("o",outputs[i]) ) )
  			{
				Iterator<Node> javaNodes = execResult.columnAs( "n");
				for (Node node : IteratorUtil.asIterable(javaNodes))
				{

					if(!node.getProperty("name").equals(serviceNode.getProperty("name"))){
//						relation = node.createRelationshipTo(neo4jServiceNodes[j], RelTypes.OUTPUT);
    	        		relation = serviceNode.createRelationshipTo(node, RelTypes.OUTPUT);
    	        		relation.setProperty("relationship", "output");
					}
					
   				}  
  			}
		}    
	
		
	}
	private void createRelationshipsInputs(Node serviceNode){
		Object inputsFromGdb =serviceNode.getProperty("inputs");
		//remove the "[" and "]" from string
		String ips = Arrays.toString((String[]) inputsFromGdb).substring(1, Arrays.toString((String[]) inputsFromGdb).length()-1);
		String[] inputs = ips.split("\\s*,\\s*");
		String[] tempInputs = new String[0];
		Transaction tx = graphDatabaseTaxonomy.beginTx();
		for(int input = 0; input<inputs.length; input++){
			try ( 
    			    Result execResult = graphDatabaseTaxonomy.execute( "match (n{name: {o}}) return n", MapUtil.map("o",inputs[input])))
    			{
      				Iterator<Node> javaNodes = execResult.columnAs("n");
      				for (Node node : IteratorUtil.asIterable(javaNodes))
      				{	          					
      					String nodeParent = node.getProperty("parent").toString();
        			    Result resultParentNode = graphDatabaseTaxonomy.execute( "match (n{name: {o}}) return n", MapUtil.map("o",nodeParent));
        			    Iterator<Node> gNodes = resultParentNode.columnAs("n");
          				for (Node nd : IteratorUtil.asIterable(gNodes)){
          					Object subC = nd.getProperty("subChildren");
          					String subs = Arrays.toString((String[]) subC).substring(1, Arrays.toString((String[]) subC).length()-1);
      		    			String[] subChildrenFromTNode = subs.split("\\s*,\\s*");
          					for(int sc = 0; sc<subChildrenFromTNode.length; sc++){
          		    			tempInputs = increaseArray(tempInputs);
          		    			tempInputs[tempInputs.length-1] = subChildrenFromTNode[sc];
          					}
          				}
      				}  
    			}
		}
  		tx.finish();
		inputs = tempInputs;
		//TODO: add all taxonomyGNode.children()
		for(int i = 0; i< inputs.length; i++){
			try ( 
  			      Result execResult = graphDatabaseService.execute( "MATCH (n) WHERE ANY(output IN n.outputs  WHERE output =~ {o}) RETURN n", MapUtil.map("o",inputs[i]) ) )
  			{
				Iterator<Node> javaNodes = execResult.columnAs( "n");
				for (Node node : IteratorUtil.asIterable(javaNodes))
				{
					if(!node.getProperty("name").equals(serviceNode.getProperty("name"))){
						relation = serviceNode.createRelationshipTo(node, RelTypes.INPUT);
    	        		relation.setProperty("relationship", "input");
					}        				
   				}  
  			}
		}    
	}
    void shutdown(){
    	graphDatabaseService.shutdown();
    	System.out.println("Neo4j database is shutdown");
    }
			
    
    private static boolean contains(String[] array, String v) {        	
            for (String e : array){
                if (e == v || v.equals(e)){
                    return true;
                }
            }
      return false;
    }
    
//	private void checkAndAddAllChildNodes(String name, String[] findOutputsNodes){
//		for(int t = 0; t<neo4jTaxonomyNodes.size(); t++){
//			String n = neo4jTaxonomyNodes.get(t).getProperty("name").toString();
//			if(name.equals(n)){
//				System.out.println(neo4jTaxonomyNodes.get(t).getRelationships(Direction.OUTGOING));
//				for (Relationship relationship : neo4jTaxonomyNodes.get(t).getRelationships(Direction.OUTGOING)) {
//			       
//			        for (Node node : relationship.getNodes()) {
//			            System.out.println("++++++++++++++++++++++++");
//
//			            System.out.println(node.getProperty("name"));
//			        }
//			       
//			    }
//
//			}
//			
//		}
//	}

	
    /**
	 * Parses the WSC Web service file with the given name, creating Web
	 * services based on this information and saving them to the service map.
	 *
	 * @param fileName
	 */
	private void parseWSCServiceFile(String fileName) {
		String[] inputs = new String[0];
		String[] outputs = new String[0];
		double[] qos = new double[4];

		try {
			File fXmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			NodeList nList = doc.getElementsByTagName("service");

			for (int i = 0; i < nList.getLength(); i++) {
				org.w3c.dom.Node nNode = nList.item(i);
				Element eElement = (Element) nNode;

				String name = eElement.getAttribute("name");
//				if (!runningOwls) {
					qos[TIME] = Double.valueOf(eElement.getAttribute("Res"));
					qos[COST] = Double.valueOf(eElement.getAttribute("Pri"));
					qos[AVAILABILITY] = Double.valueOf(eElement.getAttribute("Ava"));
					qos[RELIABILITY] = Double.valueOf(eElement.getAttribute("Rel"));
//				}
				// Get inputs
				org.w3c.dom.Node inputNode = eElement.getElementsByTagName("inputs").item(0);
				NodeList inputNodes = ((Element)inputNode).getElementsByTagName("instance");
				for (int j = 0; j < inputNodes.getLength(); j++) {
					org.w3c.dom.Node in = inputNodes.item(j);
					Element e = (Element) in;
					String[] newArray = increaseArray(inputs);
					inputs = newArray;
					inputs[j] = e.getAttribute("name");
					
				}

				// Get outputs
				org.w3c.dom.Node outputNode = eElement.getElementsByTagName("outputs").item(0);
				NodeList outputNodes = ((Element)outputNode).getElementsByTagName("instance");
				for (int j = 0; j < outputNodes.getLength(); j++) {
					org.w3c.dom.Node out = outputNodes.item(j);
					Element e = (Element) out;
					String[] newArray = increaseArray(outputs);
					outputs = newArray;
					outputs[j] = e.getAttribute("name");
				}
				ServiceNode ws = new ServiceNode(name, qos, inputs, outputs);
				serviceMap.put(name, ws);
				inputs = new String[0];
				outputs = new String[0];
				qos = new double[4];
			}
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
	
	
	
	private void createTaxonomyDatabase(Map<String, TaxonomyNode> taxonomyMap){
		graphDatabaseTaxonomy = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_TaxonomyDBPath);
//		setSubChilden(taxonomyMap);
    	Transaction transaction = graphDatabaseTaxonomy.beginTx();
    	neo4jTaxonomyNodes = new Node[0];
    	try{
    		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()) {
    		    String key = entry.getKey();
    		    TaxonomyNode value = entry.getValue();
    		    if(key!=""){
    		    	Node taxonomyGNode = graphDatabaseTaxonomy.createNode();
    				if(key!=""){
    				    Label nodeLable = DynamicLabel.label(key);
    				    taxonomyGNode.addLabel(nodeLable);
    				}
    			    taxonomyGNode.setProperty("name", key);
    			    taxonomyGNode.setProperty("parent", value.getParent());
    			    taxonomyGNode.setProperty("children", value.getChildren());
    			    taxonomyGNode.setProperty("subChildren", value.getSubChildren());
    			    taxonomyGNode.setProperty("parents", value.getParents());
    			    neo4jTaxonomyNodes = increaseNodeArray(neo4jTaxonomyNodes);
    			    neo4jTaxonomyNodes[neo4jTaxonomyNodes.length-1] = taxonomyGNode;
    		    }
    		}
    		System.out.println("taxonomy graph nodes created");
    		for(int tn = 0;tn<neo4jTaxonomyNodes.length;tn++){
    			String value = (String) neo4jTaxonomyNodes[tn].getProperty("parent");
    			String[] children = (String[]) neo4jTaxonomyNodes[tn].getProperty("children");
   		    
    			try (    
	    				Result execResult = graphDatabaseTaxonomy.execute( "Match (n{name: {o}}) RETURN n", MapUtil.map("o",value)))
	    			{
	  				Iterator<Node> javaNodes = execResult.columnAs( "n");
	  				for (Node node : IteratorUtil.asIterable(javaNodes))
	  				{
	  					relation = node.createRelationshipTo(neo4jTaxonomyNodes[tn], RelTypes.PARENT);
//	  	        		relation = neo4jTaxonomyNodes[tn].createRelationshipTo(node, RelTypes.PARENT);
	  	        		relation.setProperty("relationship", "relationship");
	  	       
	     				}  
	    			}
    		}
    		transaction.success();
    		
    	}finally{
    		transaction.finish();
    	}
		System.out.println("taxonomy graph nodes relationship created");

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
			
			System.out.println("add taxonomy objects added");
    		
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
					if (parent != null) {
						taxNode.setParentNode(parent);
						taxNode.setParent(parent.toString());
						parent.addChild(taxNode.toString());
						TaxonomyNode td = taxNode;

						while(td.getParentNode().toString()!=""){
							td.getParentNode().addSubChildren(taxNode.toString());
							td = td.getParentNode();
						}
					}
					NodeList children = currNode.getChildNodes();
					processTaxonomyChildren(taxNode, children);
				}
			}
		}
	}
	
	private void addParentNodesForEachTaxonomyNode(Map<String, TaxonomyNode> taxonomyMap){
		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()) {
			String key = entry.getKey();
		    TaxonomyNode value = entry.getValue();
		    TaxonomyNode td = value;
		    while(td.getParentNode()!=null && !td.toString().equals("")){
		    	if(!td.getParentNode().toString().equals("")){
			    	value.addParents(td.getParentNode().toString());
		    	}
		    	td = td.getParentNode();
			}	    
		}
	}
	
	private String[] increaseArray(String[] theArray)
	{
	    int i = theArray.length;
	    int n = ++i;
	    String[] newArray = new String[n];
	    for(int cnt=0;cnt<theArray.length;cnt++)
	    {
	        newArray[cnt] = theArray[cnt];
	    }
	    return newArray;
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

}
