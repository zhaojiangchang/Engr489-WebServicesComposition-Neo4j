package org.neo4j.neo4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
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

	private static final String Neo4j_ServicesDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/services";
	private static final String Neo4j_TaxonomyDBPath = "/Users/JackyChang/Engr489-WebServicesComposition-Neo4j/neo4j/database/taxonomy";
	List<Node> neo4jServiceNodes;
	List<Node> neo4jTaxonomyNodes;
	Relationship relation;
	GraphDatabaseService graphDatabaseService;
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD
	}
    public static void main( String[] args )
    {
    	Main neo4jwsc = new Main();
    	neo4jwsc.parseWSCServiceFile("test_serv.xml");
    	neo4jwsc.createServicesDatabase(serviceMap);
    	neo4jwsc.parseWSCTaxonomyFile("test_taxonomy.xml");
//    	hello.removeData();
    	neo4jwsc.setServicesRelationship(neo4jwsc.neo4jServiceNodes);
    	neo4jwsc.shutdown();
    	
    }
	void createServicesDatabase(Map<String, ServiceNode> serviceMap){
    	graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_ServicesDBPath);
    	Transaction transaction = graphDatabaseService.beginTx();
    	neo4jServiceNodes = new ArrayList<Node>();
    	try{
    		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
    		    String key = entry.getKey();
    		    ServiceNode value = entry.getValue();
    		    // do what you have to do here
    		    // In your case, an other loop.
    		    Node service = graphDatabaseService.createNode();
    		    Label nodeLable = DynamicLabel.label(key);
    		    service.addLabel(nodeLable);
    		    service.setProperty("name", key);
    		    service.setProperty("qos", value.getQos());
    		    service.setProperty("inputs", value.getInputs());
    		    service.setProperty("outputs", value.getOutputs());
    		    neo4jServiceNodes.add(service);
    		}
//    		relation = first.createRelationshipTo(second, RelTypes.KNOWS);
//    		relation.setProperty("relationship", "knows");
//    		System.out.println(first.getProperty("name").toString() +"  "+relation.getProperty("relationship").toString()+"  "+second.getProperty("name").toString());
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
    void setServicesRelationship(List<Node> neo4jServiceNodes){
    	
    }
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
			graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_TaxonomyDBPath);
			Transaction transaction = graphDatabaseService.beginTx();
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
						System.out.println(taxNode==null);
						System.out.println(value);
						taxNode = new TaxonomyNode(value);
						taxonomyMap.put( value, taxNode );
						
						taxonomyGNode = graphDatabaseService.createNode();
						if(value!=""){
						    Label nodeLable = DynamicLabel.label(value);
						    taxonomyGNode.addLabel(nodeLable);
						}
					    taxonomyGNode.setProperty("name", value);
					    neo4jTaxonomyNodes.add(taxonomyGNode);
					}
					if (parent != null) {
						taxNode.parents.add(parent);
						parent.children.add(taxNode);
						relation = taxonomyGNode.createRelationshipTo(parentGNode, RelTypes.CHILD);
						relation = parentGNode.createRelationshipTo(taxonomyGNode, RelTypes.PARENT);
//						relation.setProperty("relationship", "knows");
//						System.out.println(first.getProperty("name").toString() +"  "+relation.getProperty("relationship").toString()+"  "+second.getProperty("name").toString());

						
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

}
