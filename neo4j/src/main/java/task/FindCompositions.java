package task;

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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.Traversal;

import component.TaxonomyNode;

@SuppressWarnings("deprecation")
public class FindCompositions {
	private GraphDatabaseService subGraphDatabaseService;
	private Node endNode = null;
	private Node startNode = null;
	private  Map<String, Node> neo4jServNodes = new HashMap<String, Node>();
	private  Map<String, TaxonomyNode> taxonomyMap = null;
	private enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	Set<Node> population = null;
	private int compositionSize = 0;
	private Map<String,Node>subGraphNodesMap = null;
	private int totalCompositions = 0;
	@SuppressWarnings("unused")
	private boolean skipRecursive = false;
	public  double minAvailability = 0.0;
	public double maxAvailability = -1.0;
	public  double minReliability = 0.0;
	public double maxReliability = -1.0;
	public double minTime = Double.MAX_VALUE;
	public double maxTime = -1.0;
	public double minCost = Double.MAX_VALUE;
	public double maxCost = -1.0;
	private double m_a = 0;
	private double m_r = 0;
	private double m_c = 0;
	private double m_t = 0;
	
	public FindCompositions(int totalCompositions, int compositionSize, GraphDatabaseService subGraphDatabaseService ){
		this.subGraphDatabaseService = subGraphDatabaseService;
		this.compositionSize = compositionSize;
		this.totalCompositions = totalCompositions;
		population = new HashSet<Node>();
	}
	public Map<List<Node>, Map<String,Double>> run() throws OuchException{
		Map<List<Node>, Double> timeForEachCandidate = new HashMap<List<Node>, Double>();
		findCandidates(timeForEachCandidate);		
		Map<List<Node>, Map<String,Double>> candidatesWithQos = calculateQos(timeForEachCandidate);

		for (Map.Entry<List<Node>, Map<String,Double>> entry : candidatesWithQos.entrySet()){
			Map<String,Double> a = entry.getValue();
			System.out.println();
			for (Map.Entry<String,Double> entry2 : a.entrySet()){
				System.out.print(entry2.getKey()+": "+entry2.getValue()+";   ");
			}
			System.out.println();
		}
		
		
		//		List<Node> bestAvailability = getBest(candidatesWithQos, "A");
		//		List<Node> bestReliability = getBest(candidatesWithQos, "R");
		//		List<Node> bestCost = getBest(candidatesWithQos, "C");
		//		findBestTime(candidates);
		return candidatesWithQos;
	}
	public Map<List<Node>,Map<String,Double>> getResult(Map<List<Node>, Map<String,Double>> candidates) {
		double best = 0;
		@SuppressWarnings("unused")
		List<Node>bestList = new ArrayList<Node>();
		Map<List<Node>,Map<String,Double>>bestResultWithQos = new HashMap<List<Node>,Map<String,Double>>();
		for (Map.Entry<List<Node>,  Map<String,Double>> entry : candidates.entrySet()){
			Map<String, Double> qosValues = entry.getValue();
			double temp = m_a*qosValues.get("A") + m_r*qosValues.get("R") + m_c*qosValues.get("C") + m_t*qosValues.get("T");
			if(best<temp){
				best = temp;
				bestList = entry.getKey();
				bestResultWithQos.clear();
				bestResultWithQos.put(entry.getKey(), entry.getValue());
			}
		}
		return bestResultWithQos;
	}
	private Map<List<Node>, Map<String,Double>> calculateQos(Map<List<Node>, Double> timeForEachCandidate) {
		Map<List<Node>, Map<String,Double>> candidatesWithQos = new HashMap<List<Node>, Map<String,Double>>();
		List<Double> T = new ArrayList<Double>();
		for (Map.Entry<List<Node>, Double> entry1 : timeForEachCandidate.entrySet())
		{
			if(entry1.getValue()>maxTime)
				maxTime = entry1.getValue();
			if(entry1.getValue()<minTime)
				minTime = entry1.getValue();
			T.add(entry1.getValue());

		}
		for (Map.Entry<List<Node>, Double> entry : timeForEachCandidate.entrySet())
		{
			List<Node> candidate = entry.getKey();
			List<Double> A = new ArrayList<Double>();
			List<Double> R = new ArrayList<Double>();
			List<Double> C = new ArrayList<Double>();
			for(int j = 0; j<candidate.size(); j++){
				Node node = candidate.get(j);
				Transaction tx = subGraphDatabaseService.beginTx();
				A.add((double)node.getProperty("weightAvailibility"));
				R.add((double)node.getProperty("weightReliability"));
				C.add((double)node.getProperty("weightCost"));
				tx.close();
			}
			Map<String,Double> normalized = new HashMap<String,Double>();
			normalized.put("A", normalize(A, "A"));
			normalized.put("R", normalize(R, "R"));
			normalized.put("C", normalize(C, "C"));
			normalized.put("T", normalize(entry.getValue(),"T"));
			candidatesWithQos.put(candidate, normalized);
		}
		
		return candidatesWithQos;
	}
	private double normalize(List<Double> a, String id) {
		double mean = mean(a);
		
		double normalized = normalize(mean, id);

		return normalized;

	}
	private double normalize(double mean, String id) {
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
				return (maxCost- mean)/(maxCost-minCost);
			}
		}	
		else if(id.equals("T")){
			if(maxTime-minTime == 0)
				return 1;
			else{
				return (maxTime- mean)/(maxTime-minTime);
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

	private Set<Set<Node>> findCandidates(Map<List<Node>, Double> timeForEachCandidate) throws OuchException {
		Set<Set<Node>> candidates = new HashSet<Set<Node>>();

		while(candidates.size()<totalCompositions){
			skipRecursive  = false;			
			Set<Node>result = new HashSet<Node>();

			Transaction tt = subGraphDatabaseService.beginTx();
			for(Node node:subGraphDatabaseService.getAllNodes()){
				node.setProperty("totalTime", 0.0);
			}	
			tt.success();
			tt.close();
			result.add(endNode);
			composition(endNode, result);	
			result = checkDuplicateNodes(result);
			if(result.size()<=compositionSize && result.size()>0){
				for(Node n: result){
					Transaction transaction = subGraphDatabaseService.beginTx();
					if(n.getProperty("name").equals("start")){
						List<Node> list = new ArrayList<Node>(result);
						timeForEachCandidate.put(list,(double)n.getProperty("totalTime"));
					}
					transaction.close();
				}
			}
			if(result.size()<=compositionSize && result.size()>0){
				candidates.add(result);
			}
		}
		return candidates;

	}
	private void composition(Node subEndNode, Set<Node> result) {

		List<String>nodeInputs = Arrays.asList(getNodePropertyArray(subEndNode, "inputs"));
		List<Relationship>rels = new ArrayList<Relationship>();
		Transaction tx = subGraphDatabaseService.beginTx();

		try{
			for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
				rels.add(r);
			}
		}catch(Exception e){
		}finally{
			tx.close();
		}
		Set<Node> fulfillSubEndNodes = findNodesFulfillSubEndNode(nodeInputs,rels);

		if(fulfillSubEndNodes!=null){
			result.addAll(fulfillSubEndNodes);
			if(result.size()>compositionSize){
				return;
			}else{
				for (Node node: fulfillSubEndNodes){
					Transaction t = subGraphDatabaseService.beginTx();
					try{
						double preNodeTotalTime =(double) subEndNode.getProperty("totalTime");
						double currentNodeTime = (double)node.getProperty("weightTime");
						double currentNodeTotalTime = (double)node.getProperty("totalTime");
						if(currentNodeTotalTime<(preNodeTotalTime+currentNodeTime)){
							double weight = preNodeTotalTime+((double)node.getProperty("weightTime"));
							node.setProperty("totalTime", weight );

						}
						t.success();
					}
					catch(Exception e){

					}finally{
						t.close();
					}
					composition(node, result);
				}
			}


		}
		//			tx.success();

	}
	private Set<Node> findNodesFulfillSubEndNode(List<String> nodeInputs, List<Relationship> relationships) {
		int i = 100;
		while(i!=0){
			Collections.shuffle(relationships);
			Set<Node>toReturn = new HashSet<Node>();
			List<String>relOutputs = new ArrayList<String>();
			for(Relationship r: relationships){
				Transaction tx = subGraphDatabaseService.beginTx();
				Node node = null;
				List<String> commonValue  = new ArrayList<String>();
				try{
					node = subGraphNodesMap.get(r.getProperty("From"));
					commonValue = Arrays.asList(getNodeRelationshipPropertyArray(r, "outputs"));
				}
				catch(Exception e){
				}finally{
					tx.close();
				}
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
			List<String> sNodeInputs = Arrays.asList(getNodePropertyArray(sNode,"inputs"));
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

	private boolean fulfillSubGraphNodes(Set<Node> releatedNodes) {
		Transaction transaction = subGraphDatabaseService.beginTx();
		boolean toReturn = false;
		try{
			for(Node sNode: releatedNodes){
				if(sNode.getProperty("name").equals("start")){
					Set<Node>releatedToStartNodes = new HashSet<Node>();
					List<String> sNodeOutputs = Arrays.asList(getNodePropertyArray(sNode,"outputs"));
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
					List<String> sNodeInputs = Arrays.asList(getNodePropertyArray(sNode,"inputs"));
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
				relNodeInputs.addAll(Arrays.asList(getNodePropertyArray(sNode,"inputs")));
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
	private String[] getNodePropertyArray(Node sNode, String property){
		Transaction transaction = subGraphDatabaseService.beginTx();
		Object obj =sNode.getProperty(property);
		//    		//remove the "[" and "]" from string
		String[] array = new String[0];
		try{
			String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
			String[] tempInputs = ips.split("\\s*,\\s*");

			for(String s: tempInputs){
				if(s.length()>0){
					array =increaseArray(array);
					array[array.length-1] = s;
				}
			}
		}catch(Exception e){

		}
		finally{
			transaction.close();
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

	public void setMaxCost(double maxCost) {
		this.maxCost = maxCost;
	}
	public void setM_a(double m_a) {
		this.m_a = m_a;
	}
	public void setM_r(double m_r) {
		this.m_r = m_r;
	}
	public void setM_c(double m_c) {
		this.m_c = m_c;
	}
	public void setM_t(double m_t) {
		this.m_t = m_t;
	}
	
	
	
//	private String[] getInputOutputServicesForSubGraph(Node sNode, Set<Node> releatedNodes, String inputOrOutput) {
//	Transaction tx = subGraphDatabaseService.beginTx();
//	String [] toReturn = null;
//	try{
//		List<String>releatedNodesNames = new ArrayList<String>();
//		for(Node n: releatedNodes){
//			releatedNodesNames.add((String)n.getProperty("name"));
//		}
//
//		if(inputOrOutput.equals("inputServices")){
//			List<String>inputServicesList = Arrays.asList(getNodePropertyArray(sNode,"inputServices",subGraphDatabaseService));
//			if(inputServicesList.size()>0){
//				List<String>tempInputServices = new ArrayList<String>(inputServicesList);
//				tempInputServices.retainAll(releatedNodesNames);
//				String[] inputServices = new String[tempInputServices.size()];
//				for(int i = 0; i<tempInputServices.size(); i++){
//					inputServices[i] = tempInputServices.get(i);
//				}
//				toReturn = inputServices;
//			}
//		}
//		else if(inputOrOutput.equals("outputServices")){
//			List<String>outputServicesList = Arrays.asList(getNodePropertyArray(sNode,"outputServices",subGraphDatabaseService));
//			if(outputServicesList.size()>0){
//				List<String>tempOutputServices = new ArrayList<String>(outputServicesList);
//				tempOutputServices.retainAll(releatedNodesNames);
//				String[] outputServices = new String[tempOutputServices.size()];
//				for(int i = 0; i<tempOutputServices.size(); i++){
//					outputServices[i] = tempOutputServices.get(i);
//				}
//				toReturn = outputServices;
//			}
//		}
//		else if(inputOrOutput.equals("priousNodeNames")){
//			List<String>priousNodeNames = Arrays.asList(getNodePropertyArray(sNode,"priousNodeNames",subGraphDatabaseService));
//			if(priousNodeNames.size()>0){
//				List<String>tempPriousNodeNames = new ArrayList<String>(priousNodeNames);
//				tempPriousNodeNames.retainAll(releatedNodesNames);
//				String[] priousNodes = new String[tempPriousNodeNames.size()];
//				for(int i = 0; i<tempPriousNodeNames.size(); i++){
//					priousNodes[i] = tempPriousNodeNames.get(i);
//				}
//				toReturn = priousNodes;
//			}
//		}
//		tx.success();
//	}catch(Exception e){
//		System.out.println("getInputOutputServicesForSubGraph    "+e);
//	}finally{
//		tx.close();
//	}
//	return toReturn;
//}
//private String[] getOutputs(Node node, Node sNode,GraphDatabaseService candidateGraphDatabaseService) {
//	List<String>snodeOutputs = new ArrayList<String>();
//	List<String>nodeInputs =  new ArrayList<String>();
//
//	snodeOutputs = Arrays.asList(getNodePropertyArray(node,"outputs",candidateGraphDatabaseService));
//	nodeInputs = Arrays.asList(getNodePropertyArray(sNode, "inputs",candidateGraphDatabaseService));
//
//	List<String>snodeOutputsAllParents = new ArrayList<String>();
//	for(String output: snodeOutputs){
//		TaxonomyNode tNode = taxonomyMap.get(output);
//		snodeOutputsAllParents.addAll(getTNodeParentsString(tNode));
//	}
//	List<String>temp = new ArrayList<String>(snodeOutputsAllParents);
//	temp.retainAll(nodeInputs);
//	String[] tempToArray = new String[temp.size()];
//	for(int i = 0; i<temp.size(); i++){
//		tempToArray[i] = temp.get(i);
//	}
//
//	return tempToArray;
//}

//private void shutDownCandidateDbService() {
//		candidateGraphDatabaseService.shutdown();
//		
////		try {
////			FileUtils.deleteRecursively(new File(Neo4j_candidateDBPath));
////		} catch (IOException e) {
////			e.printStackTrace();
////		}
//		candidateGraphDatabaseService = null;
//	}



//	public GraphDatabaseService createCandidateDbService(int id) {
//		GraphDatabaseService candidateGraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4j_candidateDBPath+id);
//		Transaction t = candidateGraphDatabaseService.beginTx();
//		index = candidateGraphDatabaseService.index();
//		services = index.forNodes( "identifiers" );
//		t.close();
//		return candidateGraphDatabaseService;
//	}
//	private void findBestTime(List<List<Node>> candidates) {
//		List<GraphDatabaseService>gServices = new ArrayList<GraphDatabaseService>();
//		try {
//			FileUtils.deleteRecursively(new File("database/candidates/"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		for(int i = 0; i<1; i++){
//			GraphDatabaseService dbService = createCandidateDbService(i);
//			gServices.add(dbService);
//			System.out.println(i);
//			addNodesToCandidateDBService(candidates.get(i),subGraphDatabaseService, dbService );
//			Transaction t = dbService.beginTx();
//			
//				createCandidateRel(dbService);
//		 
//
////			dbService.shutdown();
//		}
//		//		shutDownCandidateDbService();
//
//	}

//	private void removeCandidateDBNodes() {
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



//public void createCandidateRel(GraphDatabaseService candidateGraphDatabaseService){
//	Transaction transaction = candidateGraphDatabaseService.beginTx();
//
//	for(Node sNode: candidateGraphDatabaseService.getAllNodes()){
//
//
//		String nodeName = (String) sNode.getProperty("name");
//		if(nodeName.equals("start")){
//			//				candidateStartNode = sNode;
//			addStartNodeRel(sNode, candidateGraphDatabaseService);
//		}
//		else {
//			addInputsServiceRelationship(sNode,candidateGraphDatabaseService);
//			//				Transaction t = candidateGraphDatabaseService.beginTx();
//			for(Relationship r: sNode.getRelationships()){
//			}
//			//				t.close();
//		}	
//
//
//	}
//	transaction.success();
//	transaction.close();
//}
//private Node getNodeByString(String name, GraphDatabaseService candidateGraphDatabaseService){
//	Transaction transaction = candidateGraphDatabaseService.beginTx();
//	Iterable<Node> nodeList = candidateGraphDatabaseService.getAllNodes();
//	for( Node n:nodeList){
//		if(n.getProperty("name").equals(name)){
//			return n;
//		}
//	}
//	transaction.close();
//	return null;
//}

//private void addInputsServiceRelationship(Node sNode, GraphDatabaseService candidateGraphDatabaseService) {
//
//	Transaction transaction = candidateGraphDatabaseService.beginTx();
//	//		double sNodeWeight = (double) sNode.getProperty("weight");
//	try{
//		String[] inputs = getNodePropertyArray(sNode, "inputServices",candidateGraphDatabaseService );
//		//		List<Node>inputsServicesNodes = new ArrayList<Node>();、
//		if(inputs.length>0){
//			for(String s: inputs){
//				ServiceNode serviceNode = serviceMap.get(s);
//				double[]qos = serviceNode.getQos();
//				Node inputsServicesNode = getNodeByString(s,candidateGraphDatabaseService);
//				if(inputsServicesNode!=null){
//					String[] tempToArray = getOutputs(inputsServicesNode, sNode, candidateGraphDatabaseService);
//					Relationship relation = inputsServicesNode.createRelationshipTo(sNode, RelTypes.IN);
//					relation.setProperty("From", s);
//					relation.setProperty("To", (String)sNode.getProperty("name"));
//					relation.setProperty("weightTime", qos[TIME]);
//					relation.setProperty("weightCost", qos[COST]);
//					relation.setProperty("weightAvailibility", qos[AVAILABILITY]);
//					relation.setProperty("weightReliability", qos[RELIABILITY]);
//					relation.setProperty("outputs", tempToArray);
//					relation.setProperty("Direction", "incoming");    
//				}
//
//
//			}
//
//		}
//		transaction.success();			
//	} catch (Exception e) {
//		System.out.println(e);
//		System.out.println("GenerateDatabase add candidate nodes InputsServiceRelationship error.."); 
//	} finally {
//		transaction.close();
//	}	
//
//}
//
//private void addStartNodeRel(Node sNode, GraphDatabaseService candidateGraphDatabaseService){
//	Transaction transaction = candidateGraphDatabaseService.beginTx();
//	try{
//		String[] outputs = getNodePropertyArray(sNode, "outputServices",candidateGraphDatabaseService);
//		if(outputs.length>0){
//			for(String s: outputs){
//				Node outputsServicesNode = getNodeByString(s, candidateGraphDatabaseService);
//				if(outputsServicesNode!=null){
//					String[] tempToArray = getOutputs(sNode, outputsServicesNode, candidateGraphDatabaseService);
//					Relationship relation = sNode.createRelationshipTo(outputsServicesNode, RelTypes.IN);
//					relation.setProperty("From", (String)sNode.getProperty("name"));
//					relation.setProperty("To", s);
//					relation.setProperty("outputs", tempToArray);
//					relation.setProperty("Direction", "incoming");  
//					relation.setProperty("weightTime", 0);
//					relation.setProperty("weightCost", 0);
//					relation.setProperty("weightAvailibility", 0);
//					relation.setProperty("weightReliability", 0);
//				}
//
//			}
//		}					
//
//		transaction.success();
//	}catch(Exception e){
//		e.printStackTrace();
//	}finally{
//		transaction.close();
//	}
//}

//private void addNodesToCandidateDBService(List<Node> list, GraphDatabaseService subGraphDatabaseService, GraphDatabaseService candidateGraphDatabaseService) {
//	Transaction transaction = subGraphDatabaseService.beginTx();
//	Set<Node>nodes = new HashSet<Node>(list);
//	for(Node sNode: nodes) {
//		//			try{
//		String[] inputServices = getInputOutputServicesForSubGraph(sNode, nodes, "inputServices");				
//		String[] outputServices = getInputOutputServicesForSubGraph(sNode, nodes,"outputServices");
//		String[] priousNodeNames = getInputOutputServicesForSubGraph(sNode, nodes,"priousNodeNames");
//		if(inputServices==null){
//			inputServices = new String[0];
//		}
//		if(outputServices==null){
//			outputServices = new String[0];
//		}
//		if(priousNodeNames==null){
//			priousNodeNames = new String[0];
//		}
//		//
//		Transaction tx = candidateGraphDatabaseService.beginTx();
//		Node service = candidateGraphDatabaseService.createNode();
//		try{
//			double[] qos = new double[4];
//			if(sNode.getProperty("name").equals("start")||sNode.getProperty("name").equals("end")){
//				qos = new double[4];
//			}else{
//				ServiceNode serviceNode = serviceMap.get((String) sNode.getProperty("name"));
//				qos = serviceNode.getQos();
//			}
//
//			Label nodeLable = DynamicLabel.label((String) sNode.getProperty("name"));
//			service.addLabel(nodeLable);
//			service.setProperty("name", (String) sNode.getProperty("name"));
//			service.setProperty("id", service.getId());
//			services.add(service, "name", service.getProperty("name"));
//			service.setProperty("qos", qos);
//			service.setProperty("weight", 0);
//			service.setProperty("inputs", getNodePropertyArray(sNode,"inputs",subGraphDatabaseService));
//			service.setProperty("outputs", getNodePropertyArray(sNode,"outputs",subGraphDatabaseService));
//			service.setProperty("inputServices", inputServices);
//			service.setProperty("outputServices", outputServices);
//			service.setProperty("priousNodeNames",priousNodeNames);
//			service.setProperty("weightTime", qos[TIME]);
//			service.setProperty("weightCost", qos[COST]);
//			service.setProperty("weightAvailibility", qos[AVAILABILITY]);
//			service.setProperty("weightReliability", qos[RELIABILITY]);
//			service.setProperty("visited", false);
//			service.setProperty("totalTime", sNode.getProperty("totalTime"));
//			//				candidateGraphNodesMap.put((String) sNode.getProperty("name"), service);
//			tx.success();
//		}catch(Exception e){
//			System.out.println("createCandidateGraphDatabase:  candidateGraphDatabaseService    "+ e);
//		}finally{
//			tx.close();
//		}
//
//	}	
//	transaction.close();
//}



//private List<Node> getBest(Map<List<Node>, Map<String, Double>> candidatesWithQos, String id) {
//	double best = -1.0;
//	List<Node>bestList = null;
//	for (Map.Entry<List<Node>, Map<String, Double>> entry : candidatesWithQos.entrySet()) {
//		List<Node> key = entry.getKey();
//		Map<String, Double> value = entry.getValue();
//		for (Map.Entry<String, Double> entry2 : value.entrySet()) {
//			if(entry2.getKey().equals(id)){
//				if(entry2.getValue()>best){
//					best = entry2.getValue();
//					bestList = entry.getKey();
//				}
//			}
//		}
//
//		// ...
//	}
//	System.out.println("best "+ id+":  "+best);
//	return bestList;
//}


//}
//	public void setServiceMap(Map<String, ServiceNode> serviceMap) {
//	this.serviceMap = serviceMap;
//}
}
