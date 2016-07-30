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

public class FindComposition {
	private GraphDatabaseService tempGraphDatabaseService;
	private Node endNode = null;
	private Node startNode = null;
	public  Map<String, Node> neo4jServNodes = new HashMap<String, Node>();
	public  Map<String, TaxonomyNode> taxonomyMap = null;
	private enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
	Set<Node> population = null;
	private int compositionSize = 0;
	public Map<String,Node>subGraphNodesMap = null;

	public FindComposition(int compositionSize, GraphDatabaseService tempGraphDatabaseService ){
		this.tempGraphDatabaseService = tempGraphDatabaseService;
		this.compositionSize = compositionSize;
		population = new HashSet<Node>();
	}
	public void runComposition(){
		Set<Node>result = new HashSet<Node>();
		result.add(endNode);
		composition(endNode, result);	
		result = checkDuplicateNodes(result);
		if(result.size()<=compositionSize){
			population = result;
			System.out.println("==========="+result.size()+"=================");
		}
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
	
	private void composition(Node node, Set<Node> result) {
		Transaction tx = tempGraphDatabaseService.beginTx();
		try{
			List<String>nodeInputs = Arrays.asList(getNodePropertyArray(node, "inputs"));
			//		List<String>relOutputs = new ArrayList<String>();
			List<Relationship>rels = new ArrayList<Relationship>();
			for(Relationship r: node.getRelationships(Direction.INCOMING)){
				rels.add(r);
			}
			Set<Node> fulfillSubEndNodes = findNodesFulfillSubEndNode(nodeInputs,rels);

			if(fulfillSubEndNodes!=null){
				result.addAll(fulfillSubEndNodes);
				for (Node n: fulfillSubEndNodes){
					composition(n, result);
				}

			}
			tx.success();
		}catch(Exception e){
			System.out.println("composition:"+ e);
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
		Transaction transaction = tempGraphDatabaseService.beginTx();
		if(releatedNodes==null){
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

			if(finder.findSinglePath(firstNode, secondNode)!=null){
				transaction.finish();
				transaction.close();
				return true;
			}
			transaction.finish();
			transaction.close();
			return false;
		}
		else{
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  

			if(finder.findSinglePath(firstNode, secondNode)!=null){
				transaction.finish();
				transaction.close();
				return true;
			}
			transaction.finish();
			transaction.close();
			return false;
		}
	}

	private void removeNoneFulFillNodes(Set<Node> releatedNodes) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
		transaction.finish();
		transaction.close();
	}
	private boolean fulfill(Node sNode, Set<Node> releatedNodes) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
		boolean fulfill = false;
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
		transaction.finish();
		transaction.close();
		return fulfill;
	}
	private Set<Node> checkDuplicateNodes(Set<Node> result) {
		Transaction tx = tempGraphDatabaseService.beginTx();
		Set<Node>temp = new HashSet<Node>(result);
		for(Node n : result){
			if(!n.getProperty("name").equals("start") && !n.getProperty("name").equals("end") ){
				temp.remove(n);
				if(!fulfillSubGraphNodes(temp)){
					temp.add(n);
				}
			}
		}

		tx.close();
		return temp;
	}
	private boolean fulfillSubGraphNodes(Set<Node> releatedNodes) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
					return false;
				}
				if(inputs.size()==0){
					return false;
				}
			}

		}

		transaction.finish();
		transaction.close();
		return true;
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
		Object obj =sNode.getProperty(property);
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
	public String[] increaseArray(String[] theArray)
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
}