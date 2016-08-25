package task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
import org.neo4j.tooling.GlobalGraphOperations;

import component.ServiceNode;
import component.TaxonomyNode;

public class FindCompositions {
	private GraphDatabaseService subGraphDatabaseService;
	//	private GraphDatabaseService candidateGraphDatabaseService=null;
	private final String Neo4j_candidateDBPath = "database/candidates/candidate_graph";
	private  Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();

	private Node endNode = null;
	private Node startNode = null;
	private Node candidateEndNode = null;
	private Node candidateStartNode = null;
	private  Map<String, Node> neo4jServNodes = new HashMap<String, Node>();
	private  Map<String, TaxonomyNode> taxonomyMap = null;
	private enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
//	private Relationship relation;
	private static final int TIME = 0;
	private static final int COST = 1;
	private static final int AVAILABILITY = 2;
	private static final int RELIABILITY = 3;
	Set<Node> population = null;
	private int compositionSize = 0;
	private Map<String,Node>subGraphNodesMap = null;
	private int totalCompositions = 0;
	private boolean skipRecursive = false;
	private IndexManager index = null;
	private Index<Node> services = null;
	public  double minAvailability = 0.0;
	public double maxAvailability = -1.0;
	public  double minReliability = 0.0;
	public double maxReliability = -1.0;
	public double minTime = Double.MAX_VALUE;
	public double maxTime = -1.0;
	public double minCost = Double.MAX_VALUE;
	public double maxCost = -1.0;
	//	private Map<String,Node>candidateGraphNodesMap = new HashMap<String,Node>();
	//	private Set<Node>candidateGraphNodes = new HashSet<Node>();

	public FindCompositions(int totalCompositions, int compositionSize, GraphDatabaseService subGraphDatabaseService ){
		this.subGraphDatabaseService = subGraphDatabaseService;
		this.compositionSize = compositionSize;
		this.totalCompositions = totalCompositions;
		population = new HashSet<Node>();

	}



	public List<List<Node>> run(){
		System.out.println("find composition run");
		List<List<Node>> candidates = findCandidates();
		System.out.println("find composition run  -1");

		Map<List<Node>, Map<String,Double>> candidatesWithQos = calculateQos(candidates);
		System.out.println("find composition run  0");
		List<Node> bestAvailability = getBest(candidatesWithQos, "A");
		System.out.println("find composition run  1");

		List<Node> bestReliability = getBest(candidatesWithQos, "R");
		System.out.println("find composition run  2");
		List<Node> bestCost = getBest(candidatesWithQos, "C");
		System.out.println("find composition run - find best time");
		System.out.println("create candidate db");
		findBestTime(candidates);
		return candidates;
	}
	//	private void shutDownCandidateDbService() {
	//		// TODO Auto-generated method stub
	//		candidateGraphDatabaseService.shutdown();
	//		
	////		try {
	////			FileUtils.deleteRecursively(new File(Neo4j_candidateDBPath));
	////		} catch (IOException e) {
	////			e.printStackTrace();
	////		}
	//		candidateGraphDatabaseService = null;
	//	}



	public GraphDatabaseService createCandidateDbService(int id) {
		GraphDatabaseService candidateGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_candidateDBPath+id);
		Transaction t = candidateGraphDatabaseService.beginTx();
		index = candidateGraphDatabaseService.index();
		services = index.forNodes( "identifiers" );
		t.close();
		return candidateGraphDatabaseService;
	}
	private void findBestTime(List<List<Node>> candidates) {
		// TODO Auto-generated method stub
		List<GraphDatabaseService>gServices = new ArrayList<GraphDatabaseService>();
		try {
			FileUtils.deleteRecursively(new File("database/candidates/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int i = 0; i<1; i++){
			GraphDatabaseService dbService = createCandidateDbService(i);
			gServices.add(dbService);
			System.out.println(i);
			addNodesToCandidateDBService(candidates.get(i),subGraphDatabaseService, dbService );
			Transaction t = dbService.beginTx();
			
				createCandidateRel(dbService);
		 

//			dbService.shutdown();
		}
		//		shutDownCandidateDbService();

	}

	//	private void removeCandidateDBNodes() {
	//		// TODO Auto-generated method stub
	//		Transaction transaction = candidateGraphDatabaseService.beginTx();
	//		try{
	//			for( Node n:GlobalGraphOperations.at(candidateGraphDatabaseService).getAllNodes()){
	//				for(Relationship r: n.getRelationships()){
	//					r.delete();
	//				}
	//				n.delete();
	//			}			
	//			transaction.success();			
	//		} catch (Exception e) {
	//			System.out.println(e);
	//			System.out.println("remove candidatedb nodes error.."); 
	//		} finally {
	//			transaction.close();
	//		}	
	//
	//	}



	public void createCandidateRel(GraphDatabaseService candidateGraphDatabaseService){
		Transaction transaction = candidateGraphDatabaseService.beginTx();

		for(Node sNode: candidateGraphDatabaseService.getAllNodes()){


			String nodeName = (String) sNode.getProperty("name");
			if(nodeName.equals("start")){
				//				candidateStartNode = sNode;
				addStartNodeRel(sNode, candidateGraphDatabaseService.getAllNodes(), candidateGraphDatabaseService);
			}
			else {
				addInputsServiceRelationship(sNode,candidateGraphDatabaseService.getAllNodes(),candidateGraphDatabaseService);
//				Transaction t = candidateGraphDatabaseService.beginTx();
				for(Relationship r: sNode.getRelationships()){
					System.out.print("  3 "+r.getProperty("From")+"   3  ");
				}
				System.out.println();
//				t.close();
			}	


		}
		transaction.success();
		transaction.close();
	}
	private Node getNodeByString(String name, Iterable<Node> nodeList, GraphDatabaseService candidateGraphDatabaseService){
		Transaction transaction = candidateGraphDatabaseService.beginTx();
		for( Node n:nodeList){
			if(n.getProperty("name").equals(name)){
				return n;
			}
		}
		transaction.close();
		return null;
	}

	private void addInputsServiceRelationship(Node sNode,Iterable<Node> nodeList, GraphDatabaseService candidateGraphDatabaseService) {

		Transaction transaction = candidateGraphDatabaseService.beginTx();
		//		double sNodeWeight = (double) sNode.getProperty("weight");
		try{
			String[] inputs = getNodePropertyArray(sNode, "inputServices",candidateGraphDatabaseService );
			//		List<Node>inputsServicesNodes = new ArrayList<Node>();、
			System.out.println(sNode.getProperty("name"));
			if(inputs.length>0){
				for(String s: inputs){
					ServiceNode serviceNode = serviceMap.get(s);
					double[]qos = serviceNode.getQos();
					Node inputsServicesNode = getNodeByString(s,nodeList,candidateGraphDatabaseService);
					if(inputsServicesNode!=null){
						System.out.print("  "+inputsServicesNode.getProperty("name")+"  ");
						String[] tempToArray = getOutputs(inputsServicesNode, sNode, candidateGraphDatabaseService);
						Relationship relation = inputsServicesNode.createRelationshipTo(sNode, RelTypes.IN);
						relation.setProperty("From", s);
						relation.setProperty("To", (String)sNode.getProperty("name"));
						relation.setProperty("weightTime", qos[TIME]);
						relation.setProperty("weightCost", qos[COST]);
						relation.setProperty("weightAvailibility", qos[AVAILABILITY]);
						relation.setProperty("weightReliability", qos[RELIABILITY]);
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");    
					}
					
					
				}
				System.out.println();
				
			}
			transaction.success();			
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("GenerateDatabase add candidate nodes InputsServiceRelationship error.."); 
		} finally {
			transaction.close();
		}	
	
	}

	private void addStartNodeRel(Node sNode,Iterable<Node> nodeList, GraphDatabaseService candidateGraphDatabaseService){
		Transaction transaction = candidateGraphDatabaseService.beginTx();
		try{
			String[] outputs = getNodePropertyArray(sNode, "outputServices",candidateGraphDatabaseService);
			if(outputs.length>0){
				for(String s: outputs){
					Node outputsServicesNode = getNodeByString(s,nodeList, candidateGraphDatabaseService);
					if(outputsServicesNode!=null){
						String[] tempToArray = getOutputs(sNode, outputsServicesNode, candidateGraphDatabaseService);
						Relationship relation = sNode.createRelationshipTo(outputsServicesNode, RelTypes.IN);
						relation.setProperty("From", (String)sNode.getProperty("name"));
						relation.setProperty("To", s);
						relation.setProperty("outputs", tempToArray);
						relation.setProperty("Direction", "incoming");  
						relation.setProperty("weightTime", 0);
						relation.setProperty("weightCost", 0);
						relation.setProperty("weightAvailibility", 0);
						relation.setProperty("weightReliability", 0);
					}
					
				}
			}					

			transaction.success();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			transaction.close();
		}
	}

	private void addNodesToCandidateDBService(List<Node> list, GraphDatabaseService subGraphDatabaseService, GraphDatabaseService candidateGraphDatabaseService) {
		// TODO Auto-generated method stub
		Transaction transaction = subGraphDatabaseService.beginTx();
		Set<Node>nodes = new HashSet<Node>(list);
		for(Node sNode: nodes) {
			//			try{
			String[] inputServices = getInputOutputServicesForSubGraph(sNode, nodes, "inputServices");				
			String[] outputServices = getInputOutputServicesForSubGraph(sNode, nodes,"outputServices");
			String[] priousNodeNames = getInputOutputServicesForSubGraph(sNode, nodes,"priousNodeNames");
			if(inputServices==null){
				inputServices = new String[0];
			}
			if(outputServices==null){
				outputServices = new String[0];
			}
			if(priousNodeNames==null){
				priousNodeNames = new String[0];
			}
			//
			Transaction tx = candidateGraphDatabaseService.beginTx();
			Node service = candidateGraphDatabaseService.createNode();
			try{
				double[] qos = new double[4];
				if(sNode.getProperty("name").equals("start")||sNode.getProperty("name").equals("end")){
					qos = new double[4];
				}else{
					ServiceNode serviceNode = serviceMap.get((String) sNode.getProperty("name"));
					qos = serviceNode.getQos();
				}

				Label nodeLable = DynamicLabel.label((String) sNode.getProperty("name"));
				service.addLabel(nodeLable);
				service.setProperty("name", (String) sNode.getProperty("name"));
				service.setProperty("id", service.getId());
				services.add(service, "name", service.getProperty("name"));
				service.setProperty("qos", qos);
				service.setProperty("weight", 0);
				service.setProperty("inputs", getNodePropertyArray(sNode,"inputs",subGraphDatabaseService));
				service.setProperty("outputs", getNodePropertyArray(sNode,"outputs",subGraphDatabaseService));
				service.setProperty("inputServices", inputServices);
				service.setProperty("outputServices", outputServices);
				service.setProperty("priousNodeNames",priousNodeNames);
				service.setProperty("weightTime", qos[TIME]);
				service.setProperty("weightCost", qos[COST]);
				service.setProperty("weightAvailibility", qos[AVAILABILITY]);
				service.setProperty("weightReliability", qos[RELIABILITY]);
				service.setProperty("visited", false);
				//				candidateGraphNodesMap.put((String) sNode.getProperty("name"), service);
				tx.success();
			}catch(Exception e){
				System.out.println("createCandidateGraphDatabase:  candidateGraphDatabaseService    "+ e);
			}finally{
				tx.close();
			}

		}	
		transaction.close();
	}



	private List<Node> getBest(Map<List<Node>, Map<String, Double>> candidatesWithQos, String id) {
		// TODO Auto-generated method stub
		double best = -1.0;
		List<Node>bestList = null;
		for (Map.Entry<List<Node>, Map<String, Double>> entry : candidatesWithQos.entrySet()) {
			List<Node> key = entry.getKey();
			Map<String, Double> value = entry.getValue();
			for (Map.Entry<String, Double> entry2 : value.entrySet()) {
				if(entry2.getKey().equals(id)){
					if(entry2.getValue()>best){
						best = entry2.getValue();
						bestList = entry.getKey();
					}
				}
			}

			// ...
		}
		System.out.println("best "+ id+":  "+best);
		return bestList;
	}

	private List<List<Node>> findCandidates() {
		// TODO Auto-generated method stub
		Set<List<Node>> candidates = new HashSet<List<Node>>();
		List<List<Node>> candidates2 = new ArrayList<List<Node>>();

		while(candidates.size()<totalCompositions){
			System.out.println(candidates.size());
			skipRecursive = false;
			Set<Node>result = new HashSet<Node>();
			result.add(endNode);
			try {
				composition(endNode, result);
			} catch (OuchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			result = checkDuplicateNodes(result);
			System.out.println("result size:  "+result.size());
			if(result.size()<=compositionSize && result.size()>0){
				List<Node> result2 = new ArrayList<Node>(result);
				candidates.add(result2);
			}
		}
		candidates2 = new ArrayList<List<Node>>(candidates);
		return candidates2;
	}

	private Map<List<Node>, Map<String,Double>> calculateQos(List<List<Node>> candidates) {
		// TODO Auto-generated method stub
		Map<List<Node>, Map<String,Double>> candidatesWithQos = new HashMap<List<Node>, Map<String,Double>>();

		for(int i = 0; i < candidates.size(); i++){
			List<Double> A = new ArrayList<Double>();
			List<Double> R = new ArrayList<Double>();
			List<Double> C = new ArrayList<Double>();
			List<Double> T = new ArrayList<Double>();
			for(int j = 0; j<candidates.get(i).size(); j++){
				Node candidate = candidates.get(i).get(j);
				Transaction tx = subGraphDatabaseService.beginTx();
				A.add(Double.parseDouble(candidate.getProperty("weightAvailibility")+""));
				R.add(Double.parseDouble(candidate.getProperty("weightReliability")+""));
				C.add(Double.parseDouble(candidate.getProperty("weightCost")+""));
				System.out.println(Double.parseDouble(candidate.getProperty("weightCost")+""));
				tx.close();
			}
			Map<String,Double> normalized = normalize(A,R,C);
			candidatesWithQos.put(candidates.get(i), normalized);
		}
		return candidatesWithQos;
	}

	private Map<String,Double> normalize(List<Double> a, List<Double> r, List<Double> c) {
		// TODO Auto-generated method stub
		Map<String,Double> toReturn = new HashMap<String,Double>();
		double meanA = mean(a);
		double meanR = mean(r);
		double meanC = mean(c);
		double normalizedA = normalize(meanA, "A");
		double normalizedR = normalize(meanR, "R");
		double normalizedC = normalize(meanC, "C");

		toReturn.put("A",normalizedA);
		toReturn.put("R",normalizedR);
		toReturn.put("C",normalizedC);

		return toReturn;

	}
	private double normalize(double mean, String id) {
		// TODO Auto-generated method stub
		if(id.equals("A")){
			if(maxAvailability-minAvailability == 0)
				return 1;
			else{
				return (mean - minAvailability)/(maxAvailability-minAvailability);
			}
		}
		else if(id.equals("R")){
			if(maxReliability-minReliability == 0)
				return 1;
			else{
				return (mean - minReliability)/(maxReliability-minReliability);
			}
		}
		else if(id.equals("C")){
			if(maxCost-minCost == 0)
				return 1;
			else{
				System.out.println(maxCost+"    "+minCost);
				return (maxCost- mean)/(maxCost-minCost);
			}
		}	
		else return -1;
	}

	private double mean(List<Double>list){
		double total = 0;
		for(Double d: list){
			total += d;
		}
		double toReturn = total/(double)list.size();
		return toReturn;
	}
	private void composition(Node subEndNode, Set<Node> result)  throws OuchException{
		Transaction tx = subGraphDatabaseService.beginTx();

		try{
			List<String>nodeInputs = Arrays.asList(getNodePropertyArray(subEndNode, "inputs",subGraphDatabaseService));
			//		List<String>relOutputs = new ArrayList<String>();
			List<Relationship>rels = new ArrayList<Relationship>();
			for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
				rels.add(r);
			}
			Set<Node> fulfillSubEndNodes = findNodesFulfillSubEndNode(nodeInputs,rels);

			if(fulfillSubEndNodes!=null){
				//			System.out.println(fulfillSubEndNodes.size() +"    " +subEndNode.getProperty("name"));
				//			for(Node n: fulfillSubEndNodes){
				//				System.out.println(n.getProperty("name"));
				//			}

				result.addAll(fulfillSubEndNodes);
				if(result.size()>compositionSize){
					throw new OuchException("result>compositionSize");
				}else{
					for (Node node: fulfillSubEndNodes){
						composition(node, result);
					}
				}


			}
			//			tx.success();
		}catch(Exception e){
			//			System.out.println("find composition - composition: "+ e);
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
	private Set<Node> checkDuplicateNodes(Set<Node> result) {
		Transaction tx = subGraphDatabaseService.beginTx();
		Set<Node>temp = new HashSet<Node>(result);
		try{
			for(Node n : result){
				if(!n.getProperty("name").equals("start") && !n.getProperty("name").equals("end") ){
					temp.remove(n);
					if(!fulfillSubGraphNodes(temp)){
						temp.add(n);
					}
				}
			}

		}catch(Exception e){
			System.out.println("composition:"+ e);
		}finally{
			tx.close();
		}
		return temp;
	}


	public Set<Node> getPopulation() {
		return population;
	}
	public void setEndNode(Node endNode) {
		this.endNode = endNode;
	}
	public void setStartNode(Node startNode) {
		this.startNode = startNode;
	}
	public void setNeo4jServNodes(Map<String, Node> neo4jServNodes) {
		this.neo4jServNodes = neo4jServNodes;
	}
	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap) {
		this.taxonomyMap = taxonomyMap;
	}

	public void setSubGraphNodesMap(Map<String, Node> subGraphNodesMap) {
		this.subGraphNodesMap = subGraphNodesMap;
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
		Transaction transaction = subGraphDatabaseService.beginTx();
		boolean toReturn = false;
		try{
			if(releatedNodes==null){
				PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

				if(finder.findSinglePath(firstNode, secondNode)!=null){
					toReturn = true;
				}
			}
			else{
				PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

				if(finder.findSinglePath(firstNode, secondNode)!=null){
					toReturn = true;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("find composition hasRel error.."); 
		} finally {
			transaction.close();
		}
		return toReturn;		
	}

	private void removeNoneFulFillNodes(Set<Node> releatedNodes) {
		Transaction transaction = subGraphDatabaseService.beginTx();
		try{
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
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("find composition removeNoneFulFillNodes error.."); 
		} finally {
			transaction.close();
		}
	}
	private boolean fulfill(Node sNode, Set<Node> releatedNodes) {
		Transaction transaction = subGraphDatabaseService.beginTx();
		boolean fulfill = false;
		try{
			Set<String> inputs = new HashSet<String>();
			List<String> sNodeInputs = Arrays.asList(getNodePropertyArray(sNode,"inputs",subGraphDatabaseService));
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
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("find composition fulfill error.."); 
		} finally {
			transaction.close();
		}
		return fulfill;
	}
	//	private Set<Node> checkDuplicateNodes(Set<Node> result) {
	//		Transaction transaction = subGraphDatabaseService.beginTx();
	//		Set<Node>temp = new HashSet<Node>(result);
	//		try{
	//			for(Node n : result){
	//				if(!n.getProperty("name").equals("start") && !n.getProperty("name").equals("end") ){
	//					temp.remove(n);
	//					if(!fulfillSubGraphNodes(temp)){
	//						temp.add(n);
	//					}
	//				}
	//			}
	//
	//		} catch (Exception e) {
	//			System.out.println(e);
	//			System.out.println("find composition checkDuplicateNodes error.."); 
	//		} finally {
	//			transaction.close();
	//		}		
	//		return temp;
	//	}
	private boolean fulfillSubGraphNodes(Set<Node> releatedNodes) {
		Transaction transaction = subGraphDatabaseService.beginTx();
		boolean toReturn = false;
		try{
			for(Node sNode: releatedNodes){
				if(sNode.getProperty("name").equals("start")){
					Set<Node>releatedToStartNodes = new HashSet<Node>();
					List<String> sNodeOutputs = Arrays.asList(getNodePropertyArray(sNode,"outputs",subGraphDatabaseService));
					for(Relationship r: sNode.getRelationships(Direction.OUTGOING)){
						String to = (String) r.getProperty("To");
						Node toNode = subGraphNodesMap.get(to);

						if(releatedNodes.contains(toNode)){
							releatedToStartNodes.add(toNode);
						}
					}
					if(!fulfillStartNode(sNodeOutputs, releatedToStartNodes)){
						toReturn = false;
						return false;			
					}
				}
				else{
					List<String> sNodeInputs = Arrays.asList(getNodePropertyArray(sNode,"inputs",subGraphDatabaseService));
					Set<String> inputs = new HashSet<String>();
					for(Relationship r: sNode.getRelationships(Direction.INCOMING)){
						String from = (String) r.getProperty("From");

						Node fromNode = subGraphNodesMap.get(from);
						if(releatedNodes.contains(fromNode)){
							inputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r,"outputs")));
						}
					}
					if(inputs.size()!=sNodeInputs.size()){
						toReturn = false;
						return false;
					}
					if(inputs.size()==0){
						toReturn = false;
						return false;
					}
				}

			}
			if(!toReturn){
				toReturn = true;
				return true;
			}		
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("find composition checkDuplicateNodes error.."); 
		} finally {
			transaction.close();
		}
		return toReturn;	
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
				relNodeInputs.addAll(Arrays.asList(getNodePropertyArray(sNode,"inputs",subGraphDatabaseService)));
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
	//	private String[] getNodePropertyArray(Node sNode, String property, GraphDatabaseService graphDatabaseService){
	//		Transaction transaction = graphDatabaseService.beginTx();
	//
	//		Object obj =sNode.getProperty(property);
	//		//    		//remove the "[" and "]" from string
	//		String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
	//		String[] tempInputs = ips.split("\\s*,\\s*");
	//		String[] array = new String[0];
	//		for(String s: tempInputs){
	//			if(s.length()>0){
	//				array =increaseArray(array);
	//				array[array.length-1] = s;
	//			}
	//		}
	//		transaction.close();
	//		return array;
	//	}
	private String[] getNodePropertyArray(Node node, String property, GraphDatabaseService graphDatabaseService){
		Transaction transaction = graphDatabaseService.beginTx();
		Object obj =node.getProperty(property);
		transaction.close();
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
	private String[] increaseArray(String[] theArray)
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

	public void setMinAvailability(double minAvailability) {
		this.minAvailability = minAvailability;
	}


	public void setMaxAvailability(double maxAvailability) {
		this.maxAvailability = maxAvailability;
	}


	public void setMinReliability(double minReliability) {
		this.minReliability = minReliability;
	}


	public void setMaxReliability(double maxReliability) {
		this.maxReliability = maxReliability;
	}


	public void setMinTime(double minTime) {
		this.minTime = minTime;
	}


	public void setMaxTime(double maxTime) {
		this.maxTime = maxTime;
	}


	public void setMinCost(double minCost) {
		this.minCost = minCost;
	}


	public void setServiceMap(Map<String, ServiceNode> serviceMap) {
		this.serviceMap = serviceMap;
	}



	public void setMaxCost(double maxCost) {
		this.maxCost = maxCost;
	}
	private String[] getInputOutputServicesForSubGraph(Node sNode, Set<Node> releatedNodes, String inputOrOutput) {
		Transaction tx = subGraphDatabaseService.beginTx();
		String [] toReturn = null;
		try{
			List<String>releatedNodesNames = new ArrayList<String>();
			for(Node n: releatedNodes){
				releatedNodesNames.add((String)n.getProperty("name"));
			}

			if(inputOrOutput.equals("inputServices")){
				List<String>inputServicesList = Arrays.asList(getNodePropertyArray(sNode,"inputServices",subGraphDatabaseService));
				if(inputServicesList.size()>0){
					List<String>tempInputServices = new ArrayList<String>(inputServicesList);
					tempInputServices.retainAll(releatedNodesNames);
					String[] inputServices = new String[tempInputServices.size()];
					for(int i = 0; i<tempInputServices.size(); i++){
						inputServices[i] = tempInputServices.get(i);
					}
					toReturn = inputServices;
				}
			}
			else if(inputOrOutput.equals("outputServices")){
				List<String>outputServicesList = Arrays.asList(getNodePropertyArray(sNode,"outputServices",subGraphDatabaseService));
				if(outputServicesList.size()>0){
					List<String>tempOutputServices = new ArrayList<String>(outputServicesList);
					tempOutputServices.retainAll(releatedNodesNames);
					String[] outputServices = new String[tempOutputServices.size()];
					for(int i = 0; i<tempOutputServices.size(); i++){
						outputServices[i] = tempOutputServices.get(i);
					}
					toReturn = outputServices;
				}
			}
			else if(inputOrOutput.equals("priousNodeNames")){
				List<String>priousNodeNames = Arrays.asList(getNodePropertyArray(sNode,"priousNodeNames",subGraphDatabaseService));
				if(priousNodeNames.size()>0){
					List<String>tempPriousNodeNames = new ArrayList<String>(priousNodeNames);
					tempPriousNodeNames.retainAll(releatedNodesNames);
					String[] priousNodes = new String[tempPriousNodeNames.size()];
					for(int i = 0; i<tempPriousNodeNames.size(); i++){
						priousNodes[i] = tempPriousNodeNames.get(i);
					}
					toReturn = priousNodes;
				}
			}
			tx.success();
		}catch(Exception e){
			System.out.println("getInputOutputServicesForSubGraph    "+e);
		}finally{
			tx.close();
		}
		return toReturn;
	}
	private String[] getOutputs(Node node, Node sNode,GraphDatabaseService candidateGraphDatabaseService) {
		List<String>snodeOutputs = new ArrayList<String>();
		List<String>nodeInputs =  new ArrayList<String>();

		snodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs",candidateGraphDatabaseService));
		nodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs",candidateGraphDatabaseService));

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
}
