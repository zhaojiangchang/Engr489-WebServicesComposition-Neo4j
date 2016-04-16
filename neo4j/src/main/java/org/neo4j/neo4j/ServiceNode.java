package org.neo4j.neo4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceNode implements Cloneable {
	private List<Edge> incomingEdgeList = new ArrayList<Edge>();
	private List<Edge> outgoingEdgeList = new ArrayList<Edge>();
	private List<TaxonomyNode> taxonomyOutputs = new ArrayList<TaxonomyNode>();
	private String name;
	private double[] qos;
	private String[] inputs;
	private String[] outputs;
	private boolean consider = true;

	public ServiceNode(String name, double[] qos, String[] inputs, String[] outputs) {
		this.name = name;
		this.qos = qos;
		this.inputs = inputs;
		this.outputs = outputs;
	}

	public List<Edge> getIncomingEdgeList() {
		return incomingEdgeList;
	}

	public List<Edge> getOutgoingEdgeList() {
		return outgoingEdgeList;
	}

	public double[] getQos() {
		return qos;
	}

	public String[] getInputs() {
		return inputs;
	}

	public String[] getOutputs() {
		return outputs;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * This clones the caller node and returns an identical node with completely different reference
	 */
	public ServiceNode clone() {
		String[] newInputs = {};
		String[] newOutputs = {};
		newInputs = inputs.clone();
		newOutputs = outputs.clone();
		return new ServiceNode(name, qos, newInputs, newOutputs);
	}

	public List<TaxonomyNode> getTaxonomyOutputs() {
		return taxonomyOutputs;
	}

	public boolean isConsidered() {
		return consider;
	}

	public void setConsidered(boolean consider) {
		this.consider = consider;
	}

	@Override
	public String toString(){
		if (consider)
			return name;
		else
			return name + "*";
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ServiceNode) {
			ServiceNode o = (ServiceNode) other;
			return name.equals(o.name);
		}
		else
			return false;
	}
}
