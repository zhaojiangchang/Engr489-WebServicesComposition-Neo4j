package org.neo4j.neo4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Edge {
	private ServiceNode fromNode;
	private ServiceNode toNode;
	private Set<String> intersect;
	private boolean consider = true;

	public Edge(Set<String> intersect) {
		this.intersect = intersect;
	}

	public ServiceNode getFromNode() {
		if(fromNode == null){
			throw new NullPointerException();
		}
		return fromNode;
	}

	public ServiceNode getToNode() {
		if(toNode == null) throw new NullPointerException();
		return toNode;
	}

	public void setFromNode(ServiceNode fromNode) {
		if(fromNode == null){
			throw new NullPointerException();
		}
		this.fromNode = fromNode;
	}

	public void setToNode(ServiceNode toNode) {
		if(toNode == null) throw new NullPointerException();
		this.toNode = toNode;
	}

	public Set<String> getIntersect() {
		return intersect;
	}

	/**
	 * This will add the intersects of another edge into this edge.
	 * @param inputs
	 */
	public void addIntersects(Set<String> inputs){
		intersect.addAll(inputs);
	}

	public boolean isConsidered() {
		return consider;
	}

	public void setConsidered(boolean consider) {
		this.consider = consider;
	}

	@Override
	public String toString() {
		if (consider)
			return String.format("%s->%s", fromNode, toNode);
		else
			return String.format("%s **> %s", fromNode, toNode);
	}

	@Override
	public int hashCode() {
		return (fromNode.getName() + toNode.getName()).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Edge) {
			Edge o = (Edge) other;
			return fromNode.getName().equals(o.fromNode.getName()) && toNode.getName().equals(o.toNode.getName());
		}
		else
			return false;
	}

	/**
	 * This clones the caller edge and returns an identical edge with completely different reference
	 * @return a new edge
	 */
	public Edge cloneEdge(Map<String,ServiceNode> nodeMap){
		Set<String> newIntersect = new HashSet<String>();
		for(String s: intersect){
			newIntersect.add(s);
		}
		Edge newEdge = new Edge (newIntersect);
		ServiceNode newFromNode = nodeMap.get(fromNode.getName());
		ServiceNode newToNode = nodeMap.get(toNode.getName());
		newEdge.setFromNode(newFromNode);
		newEdge.setToNode(newToNode);
		return newEdge;
	}
}
