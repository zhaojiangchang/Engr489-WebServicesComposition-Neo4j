package task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private int individuleNodeSize = 0;
	private Map<String,Node>subGraphNodesMap = null;
	private int candidateSize = 0;
	@SuppressWarnings("unused")
	private boolean skipRecursive = false;
	public  double minAvailability = Double.MAX_VALUE;
	public double maxAvailability = Double.MIN_VALUE;
	public  double minReliability =Double.MAX_VALUE;
	public double maxReliability = Double.MIN_VALUE;
	public double minTime = Double.MAX_VALUE;
	public double maxTime = Double.MIN_VALUE;
	public double minCost = Double.MAX_VALUE;
	public double maxCost = Double.MIN_VALUE;
	private double m_a = 0;
	private double m_r = 0;
	private double m_c = 0;
	private double m_t = 0;
	Set<Relationship> relationships = new HashSet<Relationship>();
	public Map<List<Node>,Set<Relationship>> candidatesWithRels = new HashMap<List<Node>,Set<Relationship>>();
	List<Node>bestList = new ArrayList<Node>();
	private Map<String,Node>bestNodesMap = new HashMap<String,Node>();



	public FindCompositions(int candidateSize, int individuleNodeSize, GraphDatabaseService subGraphDatabaseService ){
		this.subGraphDatabaseService = subGraphDatabaseService;
		this.individuleNodeSize = individuleNodeSize;
		this.candidateSize = candidateSize;
		population = new HashSet<Node>();
	}
	public Map<List<Node>, Map<String,Map<String, Double>>> run() throws OuchException{
		Map<List<Node>, Double> timeForEachCandidate = new HashMap<List<Node>, Double>();
		findCandidates(timeForEachCandidate);		
		Map<List<Node>, Map<String,Map<String, Double>>> candidatesWithQos = calculateQos(timeForEachCandidate);

		for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : candidatesWithQos.entrySet()){
			System.out.println();
			for (Map.Entry<String,Map<String, Double>> entry2 : entry.getValue().entrySet()){
				System.out.println();
				System.out.println(entry2.getKey());
				for (Map.Entry<String, Double> entry3 : entry2.getValue().entrySet()){
					System.out.print("    "+entry3.getKey()+": "+entry3.getValue()+";   ");
				}
			}
			System.out.println();
		}
		return candidatesWithQos;
	}
	public Map<List<Node>, Map<String,Map<String, Double>>> getResult(Map<List<Node>, Map<String,Map<String, Double>>> candidates) {
		double best = 0;
		Map<List<Node>, Map<String,Map<String, Double>>>bestResultWithQos = new HashMap<List<Node>, Map<String,Map<String, Double>>>();
		for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : candidates.entrySet()){
			for (Map.Entry<String,Map<String, Double>> entry2 : entry.getValue().entrySet()){
				if(entry2.getKey().equals("normalized")){
					Map<String, Double> qosValues = entry2.getValue();
					double temp = m_a*qosValues.get("A") + m_r*qosValues.get("R") + m_c*qosValues.get("C") + m_t*qosValues.get("T");
					if(best<temp){
						best = temp;
						bestList = entry.getKey();
						bestResultWithQos.clear();
						bestResultWithQos.put(entry.getKey(), entry.getValue());
					}
				}
			}


		}
		for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry3 : candidates.entrySet()){
			Set<Node>bestResult = new HashSet<Node>(entry3.getKey());
			for(Node n: bestResult){
				Transaction tx = subGraphDatabaseService.beginTx();
				bestNodesMap.put((String)n.getProperty("name"), n);
				tx.close();
			}
			composition(endNode,bestResult,true);
		}
		return bestResultWithQos;
	}
	private Map<List<Node>, Map<String,Map<String, Double>>> calculateQos(Map<List<Node>, Double> timeForEachCandidate) {
		Map<List<Node>, Map<String,Map<String, Double>>> candidatesWithQos = new HashMap<List<Node>, Map<String,Map<String, Double>>>();
		List<Double> T = new ArrayList<Double>();
		for (Map.Entry<List<Node>, Double> entry1 : timeForEachCandidate.entrySet())
		{
			if(entry1.getValue()>maxTime)
				maxTime = entry1.getValue();
			if(entry1.getValue()<minTime)
				minTime = entry1.getValue();
			T.add(entry1.getValue());
			double totalA = 1;
			double totalR = 1;
			double totalC = 0;
			List<Node> candidate = entry1.getKey();
			for(int j = 0; j<candidate.size(); j++){
				Node node = candidate.get(j);
				Transaction tx = subGraphDatabaseService.beginTx();
				totalA*=(double)node.getProperty("weightAvailibility");
				totalR*=(double)node.getProperty("weightReliability");
				totalC+=(double)node.getProperty("weightCost");
				
				tx.close();
			}
			if (totalA > maxAvailability)
				maxAvailability = totalA;
			if(totalA < minAvailability)
				minAvailability = totalA;

			// Reliability
			if (totalR > maxReliability)
				maxReliability = totalR;
			if(totalR < minReliability)
				minReliability = totalR;
			// Cost
			if (totalC > maxCost)
				maxCost = totalC;
			if (totalC < minCost)
				minCost = totalC;

		}
		for (Map.Entry<List<Node>, Double> entry : timeForEachCandidate.entrySet())
		{
			if(entry.getValue() != 0.00){
				List<Node> candidate = entry.getKey();
				List<Double> A = new ArrayList<Double>();
				List<Double> R = new ArrayList<Double>();
				List<Double> C = new ArrayList<Double>();
				double totalA = 1;
				double totalR = 1;
				double totalC = 0;
				for(int j = 0; j<candidate.size(); j++){
					Node node = candidate.get(j);
					Transaction tx = subGraphDatabaseService.beginTx();
					A.add((double)node.getProperty("weightAvailibility"));
					R.add((double)node.getProperty("weightReliability"));
					C.add((double)node.getProperty("weightCost"));
					totalA*=(double)node.getProperty("weightAvailibility");
					totalR*=(double)node.getProperty("weightReliability");
					totalC+=(double)node.getProperty("weightCost");
					tx.close();
				}
				System.out.println("A: "+totalA +"   R: "+ totalR  +"   T: "+ entry.getValue()+"   C: "+ totalC);
				Map<String,Double> non_normalized = new HashMap<String,Double>();
				non_normalized.put("A", totalA);
				non_normalized.put("R", totalR);
				non_normalized.put("C", totalC);
				non_normalized.put("T", entry.getValue());

				Map<String,Double> normalized = new HashMap<String,Double>();
				normalized.put("A", normalize(totalA, "A"));
				normalized.put("R", normalize(totalR, "R"));
				normalized.put("C", normalize(totalC, "C"));
				normalized.put("T", normalize(entry.getValue(),"T"));
				Map<String,Map<String, Double>> qosData = new HashMap<String,Map<String, Double>>();
				qosData.put("normalized", normalized);
				qosData.put("non_normalized", non_normalized);
				candidatesWithQos.put(candidate, qosData);
			}

			}
			
		return candidatesWithQos;
	}

	private double normalize(double total, String id) {
		if(id.equals("A")){
			if(maxAvailability-minAvailability == 0)
				return 1;
			else{
				return (total - minAvailability)/(maxAvailability-minAvailability);
			}
		}
		else if(id.equals("R")){
			if(maxReliability-minReliability == 0)
				return 1;
			else{
				return (total - minReliability)/(maxReliability-minReliability);
			}
		}
		else if(id.equals("C")){
			if(maxCost-minCost == 0)
				return 1;
			else{
				return (maxCost- total)/(maxCost-minCost);
			}
		}	
		else if(id.equals("T")){
			if(maxTime-minTime == 0)
				return 1;
			else{
				return (maxTime- total)/(maxTime-minTime);
			}
		}	
		else return -1;
	}

	private void findCandidates(Map<List<Node>, Double> timeForEachCandidate) throws OuchException {

		while(timeForEachCandidate.size()<candidateSize){

			skipRecursive  = false;			
			Set<Node>result = new HashSet<Node>();

			Transaction tt = subGraphDatabaseService.beginTx();
			for(Node node:subGraphDatabaseService.getAllNodes()){
				node.setProperty("totalTime", 0.0);
			}	
			startNode.setProperty("totalTime", 0.0);
			tt.success();
			tt.close();
			result.add(endNode);
			composition(endNode, result, false);	
			boolean findNonRelNode = false;
			for(Node sNode: result){
				if(!hasRel(startNode, sNode, result) || !hasRel(sNode, endNode, result)){
					findNonRelNode =true;
					relationships.clear();
					break;
				}
			}
			if(!findNonRelNode){
				result = checkDuplicateNodes(result);
				if(result.size()<=individuleNodeSize && result.size()>0){
					for(Node n: result){
						Transaction transaction = subGraphDatabaseService.beginTx();
						if(n.getProperty("name").equals("start")){
							List<Node> list = new ArrayList<Node>(result);
							timeForEachCandidate.put(list,(double)n.getProperty("totalTime"));
							List<Node> list2 = new ArrayList<Node>(result);
							candidatesWithRels.put(list2,relationships);
						}
						transaction.close();
					}

				}
				else{
					relationships.clear();
				}

			}
		}
	}
	private boolean hasRel(Node firstNode, Node secondNode, Set<Node> releatedNodes) {
		Transaction transaction = subGraphDatabaseService.beginTx();
		if(releatedNodes==null){
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

			if(finder.findSinglePath(firstNode, secondNode)!=null){
				transaction.close();
				return true;
			}
			transaction.close();
			return false;
		}
		else{
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

			if(finder.findSinglePath(firstNode, secondNode)!=null){
				transaction.close();
				return true;
			}
			transaction.close();
			return false;
		}
	}
	private void composition(Node subEndNode, Set<Node> result, boolean finalResult) {

		List<String>nodeInputs = Arrays.asList(getNodePropertyArray(subEndNode, "inputs"));
		//		List<Relationship>rels = new ArrayList<Relationship>();
		//		Transaction tx = subGraphDatabaseService.beginTx();
		//
		//		try{
		//			for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
		//				rels.add(r);
		//			}
		//		}catch(Exception e){
		//		}finally{
		//			tx.close();
		//		}
		//		

		List<Relationship> rels = new ArrayList<Relationship>();
		if(finalResult){
			Transaction tt = subGraphDatabaseService.beginTx();
			for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
				if(bestNodesMap.containsKey(r.getProperty("From"))){
					rels.add(r);
				}

			}
			tt.close();
		}else{
			Transaction tt = subGraphDatabaseService.beginTx();
			for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
				rels.add(r);
			}
			tt.close();

		}

		Set<Node> fulfillSubEndNodes = findNodesFulfillSubEndNode(nodeInputs,rels,finalResult);

		if(fulfillSubEndNodes!=null){
			result.addAll(fulfillSubEndNodes);
			if(result.size()>individuleNodeSize){
				if(finalResult){
					System.out.println(result.size()+" not fulfill");
					for(Relationship r: relationships){
						Transaction tt = subGraphDatabaseService.beginTx();
						try{
							r.setProperty("isRedundant", true);
							tt.success();
						}
						catch(Exception e){
						}finally{
							tt.close();
						}					
					}
				}
				relationships.clear();
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
					composition(node, result,finalResult);
				}
			}


		}
		//			tx.success();

	}
	private Set<Node> findNodesFulfillSubEndNode(List<String> nodeInputs, List<Relationship> rels, boolean finalResult) {
		int i = 100;
		while(i!=0){
			Collections.shuffle(rels);
			Set<Node>toReturn = new HashSet<Node>();

			List<String>relOutputs = new ArrayList<String>();
			List<Relationship> newRels = new ArrayList<Relationship>();
			if(finalResult){
				for(Relationship rel: rels){
					Transaction tx = subGraphDatabaseService.beginTx();
					if(bestNodesMap.containsKey(rel.getProperty("From"))){
						newRels.add(rel);
					}
					tx.close();

				}
			}else{
				newRels = rels;
			}
			for(Relationship r: newRels){
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
					if(finalResult){
						Transaction tt = subGraphDatabaseService.beginTx();
						try{
							r.setProperty("isRedundant", false);
							tt.success();
						}
						catch(Exception e){
						}finally{
							tt.close();
						}
					}
					relationships.add(r);
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
	public GraphDatabaseService getSubGraphDatabaseService() {
		return subGraphDatabaseService;
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
	//	public void setMinAvailability(double minAvailability) {
	//		this.minAvailability = minAvailability;
	//	}
	//
	//
	//	public void setMaxAvailability(double maxAvailability) {
	//		this.maxAvailability = maxAvailability;
	//	}
	//
	//
	//	public void setMinReliability(double minReliability) {
	//		this.minReliability = minReliability;
	//	}
	//
	//
	//	public void setMaxReliability(double maxReliability) {
	//		this.maxReliability = maxReliability;
	//	}
	//
	//
	//	public void setMinTime(double minTime) {
	//		this.minTime = minTime;
	//	}
	//
	//
	//	public void setMaxTime(double maxTime) {
	//		this.maxTime = maxTime;
	//	}
	//
	//
	//	public void setMinCost(double minCost) {
	//		this.minCost = minCost;
	//	}
	//
	//	public void setMaxCost(double maxCost) {
	//		this.maxCost = maxCost;
	//	}
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
}
