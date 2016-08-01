package task;

import java.util.ArrayList;
import java.util.Arrays;
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

public class ReduceGraphDb {
	private Node startNode;
	private Node endNode;
	private GraphDatabaseService tempGraphDatabaseService = null;
	private Map<String, Node> neo4jServNodes = new HashMap<String, Node>();;
	private Map<String,Node>subGraphNodesMap = new HashMap<String,Node>();
	private Set<Node> relatedNodes;

	public ReduceGraphDb( Map<String, Node> neo4jServNodes, GraphDatabaseService tempGraphDatabaseService){
		this.neo4jServNodes = neo4jServNodes;
		this.tempGraphDatabaseService = tempGraphDatabaseService;
		relatedNodes = new HashSet<Node>();
	}
	public void runReduceGraph(){
		for(Entry<String, Node>entry: neo4jServNodes.entrySet()){
			relatedNodes.add(entry.getValue());
		}
		findAllReleatedNodes(relatedNodes);
		reduceGraphDatabase(relatedNodes);

	}

	public void setStartNode(Node startNode) {
		this.startNode = startNode;
	}
	public void setEndNode(Node endNode) {
		this.endNode = endNode;
	}

	public Map<String, Node> getSubGraphNodesMap() {
		return subGraphNodesMap;
	}
	private void findAllReleatedNodes(Set<Node> relatedNodes) {
		Transaction t = tempGraphDatabaseService.beginTx();
		try{
			int i = 0;
//			if(!b){
//				for(Entry<String, Node>entry: neo4jServNodes.entrySet()){
//					i++;
//					if(i%2000 == 0){
//						t = tempGraphDatabaseService.beginTx();
//					}
//					Node sNode = entry.getValue();
//
//					if(hasRel(startNode, sNode, relatedNodes) && hasRel(sNode, endNode, relatedNodes)){
//						relatedNodes.add(sNode);
//					}
//				}
//				removeNoneFulFillNodes(relatedNodes);	
//				
//			}else{
				i = 0;
				Set<Node>temp = new HashSet<Node>(relatedNodes);
				for(Node sNode: temp){	
					i++;
					System.out.println(i);
					if(i%2000 == 0){
						t = tempGraphDatabaseService.beginTx();
					}
					if(!hasRel(startNode, sNode, temp) || !hasRel(sNode, endNode, temp)){
						relatedNodes.remove(sNode);
					}
				}
				removeNoneFulFillNodes(relatedNodes);		
//			}	
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("findAllReleatedNodes error.."); 
		} finally {
			t.close();
		}
	}
	private boolean hasRel(Node firstNode, Node secondNode, Set<Node> releatedNodes) {
//		Transaction t = tempGraphDatabaseService.beginTx();
		boolean hasR = false;
//		try{
			if(releatedNodes==null){
				PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  
				if(finder.findSinglePath(firstNode, secondNode)!=null){
					return true;
				}
			}
			else{
				PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.IN, Direction.OUTGOING), neo4jServNodes.size());                  
				if(finder.findSinglePath(firstNode, secondNode)!=null){
					return true;
				}
			}
//		} catch (Exception e) {
//			System.out.println(e);
//			System.out.println("hasRel error.."); 
//		} finally {
//			t.close();
//		}
		return false;	
	}

	private void removeNoneFulFillNodes(Set<Node> releatedNodes) {
		Transaction t = tempGraphDatabaseService.beginTx();
		Set<Node>copied = new HashSet<Node>(releatedNodes);

		boolean removed = false;
		try{
			for(Node sNode: copied){
				if(!fulfill(sNode, copied) && !sNode.getProperty("name").equals("end")&& !sNode.getProperty("name").equals("start")){
					removed = true;
					releatedNodes.remove(sNode);
				}
			}
			if(removed){
				findAllReleatedNodes(releatedNodes);
			}
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("removeNoneFulFillNodes error.."); 
		} finally {
			t.close();
		}		
	}
	private boolean fulfill(Node sNode, Set<Node> releatedNodes) {
		Transaction t = tempGraphDatabaseService.beginTx();
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
			System.out.println("fulfill error.."); 
		} finally {
			t.close();
		}		
		return fulfill;
	}
	private String[] getNodePropertyArray(Node node, String property){
		Transaction transaction = tempGraphDatabaseService.beginTx();
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
	private String[] getNodeRelationshipPropertyArray(Relationship relationship, String property){
		Transaction t = tempGraphDatabaseService.beginTx();
		String[] array = new String[0];
		try{
			Object obj =relationship.getProperty(property);
			//    		//remove the "[" and "]" from string
			String string = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
			String[] tempOutputs = string.split("\\s*,\\s*");

			for(String s: tempOutputs){
				if(s.length()>0){
					array =increaseArray(array);
					array[array.length-1] = s;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("getNodeRelationshipPropertyArray error.."); 
		} finally {
			t.close();
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

	private void reduceGraphDatabase(Set<Node> relatedNodes) {
		// TODO Auto-generated method stub
		Transaction t = tempGraphDatabaseService.beginTx();
		try{
			int i = 0;
			for(Node n: getAllNodes()) {
				i++;
				if(i%2000 == 0){
					t = tempGraphDatabaseService.beginTx();
				}
				if(!relatedNodes.contains(n) && !n.getProperty("name").equals("end")&& !n.getProperty("name").equals("start")){
					for (Relationship r : n.getRelationships()) {
						r.delete();
					}
					n.delete();
				}
			}
			t.success();

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("reduceGraphDatabase error.."); 
		} finally {
			t.close();
		}
		t = tempGraphDatabaseService.beginTx();
		for(Node n: getAllNodes()) {
			subGraphNodesMap.put((String)n.getProperty("name"), n);			
		}
		System.out.println("db reduced to: "+subGraphNodesMap.size()+" nodes;");

		t.close();
	}
	private Iterable<Node>  getAllNodes( ) {
		Transaction transaction = tempGraphDatabaseService.beginTx();
		Iterable<Node> nodes = tempGraphDatabaseService.getAllNodes();
		transaction.success();
		transaction.close();
		return nodes;

	}
	private static enum RelTypes implements RelationshipType{
		PARENT, CHILD, OUTPUT, INPUT, TO, IN, OUT
	}
}
