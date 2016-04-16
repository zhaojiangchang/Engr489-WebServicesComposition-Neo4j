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

	
	private static final String Neo4j_DBPath = "/Users/JackyChang/Documents/workspace/neo4j/database";
	List<Node> neo4jServiceNodes;
	Relationship relation;
	GraphDatabaseService graphDatabaseService;
	private static enum RelTypes implements RelationshipType{
		KNOWS
	}
    public static void main( String[] args )
    {
//        System.out.println( "Hello World!" );
    	Main neo4jwsc = new Main();
    	neo4jwsc.parseWSCServiceFile("test_serv.xml");
    	neo4jwsc.createDatabase(serviceMap);
//    	hello.removeData();
    	neo4jwsc.shutdown();
    	
    }
    @SuppressWarnings("deprecation")
	void createDatabase(Map<String, ServiceNode> serviceMap){
    	graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_DBPath);
    	Transaction transaction = graphDatabaseService.beginTx();
    	neo4jServiceNodes = new ArrayList<Node>();
    	try{
    		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
    		    String key = entry.getKey();
    		    ServiceNode value = entry.getValue();
    		    // do what you have to do here
    		    // In your case, an other loop.
    		    Node n = graphDatabaseService.createNode();
    		    Label nodeLable = DynamicLabel.label(key);
    		    n.addLabel(nodeLable);
    		    n.setProperty("name", key);
    		    n.setProperty("qos", value.getQos());
    		    n.setProperty("inputs", value.getInputs());
    		    n.setProperty("outputs", value.getOutputs());
    		    neo4jServiceNodes.add(n);
    		}
    		System.out.println(neo4jServiceNodes.size());

   		
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
				System.out.println(name);

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
