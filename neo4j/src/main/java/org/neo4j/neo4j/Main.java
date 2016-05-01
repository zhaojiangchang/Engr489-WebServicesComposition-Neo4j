
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

import org.neo4j.cypher.internal.compiler.v2_2.planDescription.Children;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.io.fs.FileUtils;


public class Main {
	// Constants with of order of QoS attributes
	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;

	public Set<ServiceNode> relevant;
	public static Map<String, Set<TaxonomyNode>> cs = new HashMap<String,Set<TaxonomyNode>>();
	public static Map<String, Set<TaxonomyNode>> ps = new HashMap<String,Set<TaxonomyNode>>();

	public Set<String> taskInput;
	public Set<String> taskOutput;
	public ServiceNode startNode;
	public ServiceNode endNode;
	public static Set<TaxonomyNode> children;
	public static Set<TaxonomyNode> parents;
	private static String serviceFileName = "services-output.xml";
	private static String taxonomyFileName = "taxonomy.xml";
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

	Node[] neo4jServiceNodes;
	Node[] neo4jTaxonomyNodes;
	Relationship relation;
	Iterable<Relationship> relations;

	GraphDatabaseService graphDatabaseService;
	GraphDatabaseService graphDatabaseTaxonomy;
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	// Statistics tracking
	public static Map<String, Integer> nodeCount = new HashMap<String, Integer>();
	public static Map<String, Integer> edgeCount = new HashMap<String, Integer>();

	public static void main( String[] args ) throws IOException{
		Main neo4jwsc = new Main();
		try {
			FileUtils.deleteRecursively(new File(Neo4j_TaxonomyDBPath));
			FileUtils.deleteRecursively(new File(Neo4j_ServicesDBPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		long startTime = System.currentTimeMillis();
		neo4jwsc.parseWSCTaxonomyFile(taxonomyFileName);
		long endTime = System.currentTimeMillis();
		records.put("parseTaxonomyfile", endTime - startTime);
		System.out.println("parseTaxonomyfile Total execution time: " + (endTime - startTime) );
		
		System.out.println("done taxonomy nodes");
		startTime = System.currentTimeMillis();
		neo4jwsc.parseWSCServiceFile(serviceFileName);
		endTime = System.currentTimeMillis();
		records.put("parseservicefile", endTime - startTime);
		System.out.println("parseservicefile Total execution time: " + (endTime - startTime) );

		
		System.out.println("done service nodes");
		startTime = System.currentTimeMillis();
		neo4jwsc.populateTaxonomyTree();
		endTime = System.currentTimeMillis();
		records.put("populateTaxonomyTree", endTime - startTime);
		System.out.println("populateTaxonomyTree Total execution time: " + (endTime - startTime) );

		

//
//				for(Entry<String, Set<TaxonomyNode>> entry : cs.entrySet()){
//					String key = entry.getKey();
//					System.out.println("node: " + key);
//
//					Set<TaxonomyNode> tNodes = entry.getValue();
//					for(TaxonomyNode tNode: tNodes){
//						System.out.println("children TNode: " + tNode.value);
//					}
//				}
//				
//				for(Entry<String, Set<TaxonomyNode>> entry : ps.entrySet()){
//					String key = entry.getKey();
//					System.out.println("node: " + key);
//
//					Set<TaxonomyNode> tNodes = entry.getValue();
//					for(TaxonomyNode tNode: tNodes){
//						System.out.println("parents TNode: " + tNode.value);
//					}
//				}
		
		startTime = System.currentTimeMillis();
		neo4jwsc.createServicesDatabase(serviceMap);
		endTime = System.currentTimeMillis();
		records.put("createServicesDatabase", endTime - startTime);
		System.out.println("createServicesDatabase Total execution time: " + (endTime - startTime) );
		
		startTime = System.currentTimeMillis();
		neo4jwsc.addServiceNodeRelationShip();
		System.out.println();
		endTime = System.currentTimeMillis();
		records.put("addServiceNodeRelationShip", endTime - startTime);
		System.out.println("addServiceNodeRelationShip Total execution time: " + (endTime - startTime) );

		
		
		 FileWriter fw = new FileWriter("timeRecord.txt");
			for(Entry<String, Long> entry : records.entrySet()){
					fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		    }
		  fw.close();
		neo4jwsc.shutdown();

	}
	private void addServiceNodeRelationShip() {
		// TODO Auto-generated method stub
		Transaction transaction = graphDatabaseService.beginTx();
		int index = -1;
		for(Node sNode: neo4jServiceNodes){
			index++;
			System.out.print("create relationship: "+index);
			Object inputsFromGdb =sNode.getProperty("inputs");
			//    		//remove the "[" and "]" from string
			String ips = Arrays.toString((String[]) inputsFromGdb).substring(1, Arrays.toString((String[]) inputsFromGdb).length()-1);
			String[] inputs = ips.split("\\s*,\\s*");
			for(String input:inputs){
				TaxonomyNode tNode = taxonomyMap.get(input).parentNode;
				Set<String>outputServices = tNode.outputs;
				for(String output: outputServices){
					try ( 
							Result execResult = graphDatabaseService.execute( "match (n{name: {o}}) return n", MapUtil.map("o",output)))
					{
						Iterator<Node> javaNodes = execResult.columnAs("n");
						for (Node node : IteratorUtil.asIterable(javaNodes))
						{	          	
							if(node!=null && node!=sNode){
								relations = sNode.getRelationships();
								boolean find = false;
								for(Relationship r: relations){
									if(r.getOtherNode(sNode).equals(node) && r.getStartNode().getProperty("name").equals(node.getProperty("name"))){
										find = true;
									}
								}
								if(!find){
									relation = node.createRelationshipTo(sNode, RelTypes.IN);
									relation.setProperty("From", sNode.getProperty("name"));
									relation.setProperty("To", node.getProperty("name"));
									relation.setProperty("Direction", "incoming");    	   

								}
							}
						}  
					}
				}
			}
			Object outputsFromGdb =sNode.getProperty("outputs");
			//    		//remove the "[" and "]" from string
			String ops = Arrays.toString((String[]) outputsFromGdb).substring(1, Arrays.toString((String[]) outputsFromGdb).length()-1);
			String[] outputs = ops.split("\\s*,\\s*");
			for(String output: outputs){
				TaxonomyNode tNode = taxonomyMap.get(output).parentNode;
				Set<String>inputServices = tNode.inputs;
				for(String input: inputServices){
					try ( 
							Result execResult = graphDatabaseService.execute( "match (n{name: {o}}) return n", MapUtil.map("o",input)))
					{
						Iterator<Node> javaNodes = execResult.columnAs("n");

						for (Node node : IteratorUtil.asIterable(javaNodes))
						{	          					
							if(node!=null && node!=sNode){
								relations = sNode.getRelationships();
								boolean find = false;
								for(Relationship r: relations){
									if(r.getOtherNode(sNode).equals(node) && r.getStartNode().getProperty("name").equals(sNode.getProperty("name"))){
										find = true;
									}
								}
								if(!find){
									relation = sNode.createRelationshipTo(node, RelTypes.OUT);
									relation.setProperty("From", node.getProperty("name"));
									relation.setProperty("To", sNode.getProperty("name"));    	
									relation.setProperty("Direction", "outgoing");    	      						
								}
							}

						}
					}  
				}
			}


		}
		transaction.success();
		transaction.finish();

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
			int index = -1;
			for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {

				index++;
				System.out.println("create noce: "+index);
				String key = entry.getKey();
				ServiceNode value = entry.getValue();
				Node service = graphDatabaseService.createNode();
				Label nodeLable = DynamicLabel.label(key);
				service.addLabel(nodeLable);
				service.setProperty("name", key);
				service.setProperty("qos", value.getQos());
				service.setProperty("inputs", value.getInputsArray());
				service.setProperty("outputs", value.getOutputsArray());
				neo4jServiceNodes = increaseNodeArray(neo4jServiceNodes);
				neo4jServiceNodes[neo4jServiceNodes.length-1] =service;
			}
			System.out.println("web service nodes created");			
			transaction.success();
		}finally{
			transaction.finish();
		}
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
		int index = -1;
		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
			index++;
			System.out.print(index+" ");

			TaxonomyNode node = entry.getValue();
//			findAllChildrenTNode();
//			findAllParentsTNode();
			//TODO: too slow may work without this function
			findAllParentsAndChildrenTNode();
			children = new HashSet<TaxonomyNode>();
			parents = new HashSet<TaxonomyNode>();

			dfsGetChildren(node);
			dfsGetParents(node);

			cs.put(node.value, children);
			ps.put(node.value, parents);
		}
		System.out.println("found child and parent");
		removeChildrenInputs();
		removeParentsOutputs();

		System.out.println("remove child and parent");
		for (ServiceNode s: serviceMap.values()) {
			addServiceToTaxonomyTree(s);
		}
		System.out.println("addServiceToTaxonomyTree done");

		
		System.out.println("add services to taxonomyTree");
	}

	private void addServiceToTaxonomyTree(ServiceNode s) {
		// Populate outputs
		Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		for (String outputVal : s.getOutputs()) {

			TaxonomyNode n = taxonomyMap.get(outputVal).parentNode;
			s.getTaxonomyOutputs().add(n);
			n.outputs.add(s.getName());

			// Also add output to all parent nodes
			Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
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
		// Also add output to all parent nodes
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
		for (String inputVal : s.getInputs()) {

			TaxonomyNode n = taxonomyMap.get(inputVal).parentNode;
			s.getTaxonomyOutputs().add(n);
			n.inputs.add(s.getName());

			// Also add output to all parent nodes
			Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
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
		return;
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

		for (String s : taskInput)
			temp.add(taxonomyMap.get(s).parents.get(0).value);
		taskInput.clear();
		taskInput.addAll(temp);

		temp.clear();
		for (String s : taskOutput)
			temp.add(taxonomyMap.get(s).parents.get(0).value);
		taskOutput.clear();
		taskOutput.addAll(temp);

		for (ServiceNode s : serviceMap.values()) {
			temp.clear();
			Set<String> inputs = s.getInputs();
			for (String i : inputs)
				temp.add(taxonomyMap.get(i).parents.get(0).value);
			inputs.clear();
			inputs.addAll(temp);

			temp.clear();
			Set<String> outputs = s.getOutputs();
			for (String o : outputs)
				temp.add(taxonomyMap.get(o).parents.get(0).value);
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
		Set<String> inputs = new HashSet<String>();
		Set<String> outputs = new HashSet<String>();
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
			
				ServiceNode ws = new ServiceNode(name, qos, inputs, outputs);
				serviceMap.put(name, ws);
				inputs = new HashSet<String>();
				outputs = new HashSet<String>();
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
	private void removeChildrenInputs(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			ServiceNode sNode = entry.getValue();
			Set<String> inputs = sNode.getInputs();
			Set<String> copy = new HashSet<String>(inputs);
			for(String input: inputs){
				TaxonomyNode inputNode = taxonomyMap.get(input);
				input = inputNode.parentNode.value;
				if(cs.get(input).size()>0){
					Set<TaxonomyNode> children = cs.get(input);
					for(TaxonomyNode child:children){
						if(inputs.contains(child.value) && !inputNode.value.equals(child.value)){
							copy.remove(child.value);
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
						if(outputs.contains(toRemve)){
							copy.remove(toRemve);
						}
					}
				}
				
			}
			sNode.setOutputs(copy);
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
			taskInput = new HashSet<String>();
			for (int i = 0; i < providedList.getLength(); i++) {
				org.w3c.dom.Node item = providedList.item(i);
				Element e = (Element) item;
				taskInput.add(e.getAttribute("name"));
			}

			org.w3c.dom.Node wanted = doc.getElementsByTagName("wanted").item(0);
			NodeList wantedList = ((Element) wanted).getElementsByTagName("instance");
			taskOutput = new HashSet<String>();
			for (int i = 0; i < wantedList.getLength(); i++) {
				org.w3c.dom.Node item = wantedList.item(i);
				Element e = (Element) item;
				taskOutput.add(e.getAttribute("name"));
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
	private void findAllParentsAndChildrenTNode(){
		Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();

		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
			TaxonomyNode n = entry.getValue();
			// Also add output to all parent nodes
			Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
			Queue<TaxonomyNode> queue2 = new LinkedList<TaxonomyNode>();

			queue.add( n );
			queue2.add( n );

			while (!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsOutput.add( current );
//					System.out.println("current: "+current.value);
					for (TaxonomyNode parent : current.parents) {
						current.parents_notGrandparents.add(parent);
//						System.out.println("================parent: "+parent.value);
						if(!parent.value.equals("")){
							if (!seenConceptsOutput.contains( parent )) {
								queue.add(parent);
								seenConceptsOutput.add(parent);
							}
						}

					}
				}
			}
			while (!queue2.isEmpty()) {
				TaxonomyNode current2 = queue2.poll();
				if(!current2.value.equals("")){
					seenConceptsInput.add( current2 );
//					System.out.println("current: "+current.value);
					for (TaxonomyNode child : current2.children) {
						current2.children_notGrandchildren.add(child);
						if(!child.value.equals("")){
							if (!seenConceptsInput.contains( child )) {
								queue2.add(child);
								seenConceptsInput.add(child);
							}
						}


					}
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
