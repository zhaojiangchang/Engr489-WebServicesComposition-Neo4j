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
	public static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	public Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private List<ArrayList<Node>> aa;
	private static final String Neo4j_ServicesDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/services";
	private static final String Neo4j_TaxonomyDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/taxonomy";
	Node[] neo4jServiceNodes;
	List<Node> neo4jTaxonomyNodes;
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
    	neo4jwsc.aa = new ArrayList<ArrayList<Node>>();
    	neo4jwsc.parseWSCServiceFile("test_serv.xml");
    	neo4jwsc.createServicesDatabase(serviceMap);
    	neo4jwsc.parseWSCTaxonomyFile("test_taxonomy.xml");
    	neo4jwsc.shutdown();
    	
    }
	void createServicesDatabase(Map<String, ServiceNode> serviceMap){
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

    		for(int j= 0; j<neo4jServiceNodes.length; j++){
          		Object inputsFromGdb =neo4jServiceNodes[j].getProperty("inputs");
        		//remove the "[" and "]" from string
        		String inputs = Arrays.toString((String[]) inputsFromGdb).substring(1, Arrays.toString((String[]) inputsFromGdb).length()-1);
        		String[] items = inputs.split("\\s*,\\s*");
        		//TODO: add all taxonomyGNode.children()
        		for(int i = 0; i< items.length; i++){
        			try ( 
//        					Transaction t = graphDatabaseService.beginTx();
          			      Result execResult = graphDatabaseService.execute( "MATCH (n) WHERE ANY(output IN n.outputs  WHERE output =~ {o}) RETURN n", MapUtil.map("o",items[i]) ) )
          			{
        				Iterator<Node> javaNodes = execResult.columnAs( "n");
        				for (Node node : IteratorUtil.asIterable(javaNodes))
        				{
        				    //do something with the node
        					if (node instanceof Node) {
        						  System.out.println("node......."+ node.getProperty("name") +"      "+neo4jServiceNodes[j].getProperty("name"));
        					}  
        					ArrayList<Node> bb = new ArrayList<Node>();
        					bb.add(node);
        					bb.add(neo4jServiceNodes[j]);
        					aa.add(bb);
           				}  
          			}
        		}    		
        	}
    		for(int i = 0; i< aa.size(); i++){
        		Node a = aa.get(i).get(0);
        		Node b = aa.get(i).get(1);
        		relation = a.createRelationshipTo(b, RelTypes.OUTPUT);
        		relation = b.createRelationshipTo(a, RelTypes.INPUT);
        		relation.setProperty("relationship", "output");
        		relation.setProperty("input", b.getProperty("name"));
        		relation.setProperty("output", a.getProperty("name"));
        	}    		
    		transaction.success();
    		
    	}finally{
    		transaction.finish();
    	}
    }
//    void removeData(){
//    	Transaction transaction = graphDatabaseService.beginTx();
//    	try{
//    		first.getSingleRelationship(RelTypes.KNOWS, Direction.OUTGOING).delete();
//    		System.out.println("Nodes are removed");
//    		first.delete();
//    		second.delete();
//    		transaction.success();
//    	}finally{
//    		transaction.finish();
//    	}
//    	
//    }
    void shutdown(){
    	graphDatabaseService.shutdown();
    	System.out.println("Neo4j database is shutdown");
    }
    void findRelatedServices(){
    	for(int j= 0; j<neo4jServiceNodes.length; j++){
      		Object inputsFromGdb =neo4jServiceNodes[j].getProperty("inputs");
    		//remove the "[" and "]" from string
    		String inputs = Arrays.toString((String[]) inputsFromGdb).substring(1, Arrays.toString((String[]) inputsFromGdb).length()-1);
    		String[] items = inputs.split("\\s*,\\s*");
    		//TODO: add all taxonomyGNode.children()
    		for(int i = 0; i< items.length; i++){
    			try ( 
//    					Transaction t = graphDatabaseService.beginTx();
      			      Result execResult = graphDatabaseService.execute( "MATCH (n) WHERE ANY(output IN n.outputs  WHERE output =~ {o}) RETURN n", MapUtil.map("o",items[i]) ) )
      			{
    				Iterator<Node> javaNodes = execResult.columnAs( "n");
    				for (Node node : IteratorUtil.asIterable(javaNodes))
    				{
    				    //do something with the node
    					if (node instanceof Node) {
    						  System.out.println("node......."+ node.getProperty("name") +"      "+neo4jServiceNodes[j].getProperty("name"));
    					}  
    					ArrayList<Node> bb = new ArrayList<Node>();
    					bb.add(node);
    					bb.add(neo4jServiceNodes[j]);
    					aa.add(bb);
       				}  
      			}
    		}    
    		
    	}
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
		
	/**
	 * Parses the WSC taxonomy file with the given name, building a
	 * tree-like structure.
	 *
	 * @param fileName
	 */
	@SuppressWarnings("deprecation")
	private void parseWSCTaxonomyFile(String fileName) {
		try {
			File fXmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			NodeList taxonomyRoots = doc.getChildNodes();
			graphDatabaseTaxonomy = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_TaxonomyDBPath);
			
			Transaction transaction = graphDatabaseTaxonomy.beginTx();
			neo4jTaxonomyNodes = new ArrayList<Node>();
			try{
				processTaxonomyChildren(null, taxonomyRoots, null);
				transaction.success();
			}finally{
				transaction.finish();
			}
			
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
	private void processTaxonomyChildren(TaxonomyNode parent, NodeList nodes, Node parentGNode) {
		Node taxonomyGNode = null;
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
						
						taxonomyGNode = graphDatabaseTaxonomy.createNode();
						if(value!=""){
						    Label nodeLable = DynamicLabel.label(value);
						    taxonomyGNode.addLabel(nodeLable);
						}
					    taxonomyGNode.setProperty("name", value);
					    neo4jTaxonomyNodes.add(taxonomyGNode);
					}
					if (parent != null) {
						taxNode.setParentNode(parent);
						taxNode.addParent(parent);
						parent.addChild(taxNode);
						relation = taxonomyGNode.createRelationshipTo(parentGNode, RelTypes.CHILD);
						relation = parentGNode.createRelationshipTo(taxonomyGNode, RelTypes.PARENT);
					}
					NodeList children = currNode.getChildNodes();
					processTaxonomyChildren(taxNode, children, taxonomyGNode);
				}
			}
		}
	}
	public String[] increaseArray(String[] theArray)
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
	public Node[] increaseNodeArray(Node[] theArray)
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
