package task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import component.TaxonomyNode;

public class FindCompositions {
	private GraphDatabaseService tempGraphDatabaseService;
	private Node endNode = null;
	private  Map<String, TaxonomyNode> taxonomyMap = null;

	Set<Node> population = null;
	private int compositionSize = 0;
	private Map<String,Node>subGraphNodesMap = null;
	private int totalCompositions = 0;
	private boolean skipRecursive = false;
	public FindCompositions(int totalCompositions, int compositionSize,GraphDatabaseService tempGraphDatabaseService){
		this.compositionSize = compositionSize;
		this.totalCompositions = totalCompositions;
		this.tempGraphDatabaseService = tempGraphDatabaseService;
		population = new HashSet<Node>();
	}
	public void setGraphDb(GraphDatabaseService gb){
		this.tempGraphDatabaseService = gb;
	}
	public Set<Set<Node>> run() {
		Set<Set<Node>> candidates = new HashSet<Set<Node>>();
		while(candidates.size()<totalCompositions){
			skipRecursive = false;
			System.out.println("start while");
			Set<Node>result = new HashSet<Node>();
			List<Double> A = new ArrayList<Double>();
			List<Double> R = new ArrayList<Double>();
			List<Double> C = new ArrayList<Double>();
			List<Double> T = new ArrayList<Double>();
			int depth = 0;
			result.add(endNode);
			System.out.println("complllllll");
			try{
				composition(depth, endNode, result, A, R, C, T);	
			}catch(OuchException e){
				e.printStackTrace();
			}
			System.out.println("after returned");
			result = checkDuplicateNodes(result);
			if(result.size()<=compositionSize && result.size()>1){
				System.out.println("after returned 2222");
				candidates.add(result);
				for(Double a: A){
					System.out.print(a+"  ");
				}
				System.out.println();
				for(Double a: R){
					System.out.print(a+"  ");
				}
				System.out.println();
				for(Double a: C){
					System.out.print(a+"  ");
				}
				System.out.println();
				for(Double a: T){
					System.out.print(a+"  ");
				}
				System.out.println();
			}
			System.out.println("end while");
		}
		return candidates;
	}
	private void composition(int depth, Node subEndNode, Set<Node> result,List<Double>A,List<Double>R, List<Double>T,List<Double>C) throws OuchException{
		List<String>nodeInputs = Arrays.asList(getNodePropertyArray(subEndNode, "inputs"));
		List<Relationship>rels = new ArrayList<Relationship>();
		Transaction tx = tempGraphDatabaseService.beginTx();
		for(Relationship r: subEndNode.getRelationships(Direction.INCOMING)){
			rels.add(r);
		}
		tx.close();
		Set<Node> fulfillSubEndNodes = findNodesFulfillSubEndNode(nodeInputs,rels);
//		if(depth>10){
//			A.clear();
//			R.clear();
//			C.clear();
//			T.clear();
//			result.clear();
//			return;
//		}
		if(fulfillSubEndNodes!=null){
			result.addAll(fulfillSubEndNodes);
			if(result.size()>compositionSize){
				System.out.println("returned");
				A.clear();
				R.clear();
				C.clear();
				T.clear();
				result.clear();
				throw new OuchException("result>compositionSize");
			}else{
				for (Node node: fulfillSubEndNodes){
					A.add((Double) node.getProperty("weightAvailibility"));
					R.add((Double) node.getProperty("weightReliability"));
					C.add((Double) node.getProperty("weightCost"));
					double mix = Double.MAX_VALUE;
					for(Node n: fulfillSubEndNodes){
						if((Double)n.getProperty("weightTime")<mix){
							mix = (Double)n.getProperty("weightTime");
						}
					}
					T.add(mix);
					if(!node.getProperty("name").equals("sart")){
						depth++;
					}
					if(node.getProperty("name").equals("sart")){
						System.out.println("====================================="+ depth);;
					}
					composition(depth, node, result,A,R,T,C);
				}
			}
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
				Transaction tx = tempGraphDatabaseService.beginTx();
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
				tx.close();
			}
			i--;
		}
		return null;
	}
	private Set<Node> checkDuplicateNodes(Set<Node> result) {
		Transaction tx = tempGraphDatabaseService.beginTx();
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


	//	public void runComposition(){
	//		Set<Node>result = new HashSet<Node>();
	//		result.add(endNode);
	//		boolean isResult = composition(endNode, result);	
	//		if(isResult){
	//			result = checkDuplicateNodes(result);
	//			if(result.size()<=compositionSize){
	//				population = result;
	//				
	//			}
	//			else{
	//				population = null;
	//			}
	//		}
	//		
	//	}


	public Set<Node> getPopulation() {
		return population;
	}
	public void setEndNode(Node endNode) {
		this.endNode = endNode;
	}

	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap) {
		this.taxonomyMap = taxonomyMap;
	}

	public void setSubGraphNodesMap(Map<String, Node> subGraphNodesMap) {
		this.subGraphNodesMap = subGraphNodesMap;
	}

	private boolean fulfillSubGraphNodes(Set<Node> releatedNodes) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
		Transaction tx = tempGraphDatabaseService.beginTx();
		Object obj =sNode.getProperty(property);
		tx.close();
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
}
