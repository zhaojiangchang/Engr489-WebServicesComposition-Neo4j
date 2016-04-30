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
	private Set<String>inputs;
	private Set<String>outputs;
	private boolean consider = true;

	public ServiceNode(String name, double[] qos, Set<String> inputs, Set<String> outputs) {
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

	public String[] getInputsArray() {
		String[] temp = new String[inputs.size()];
		int index = -1;
		for(String s: inputs){
			index++;
			temp[index] = s;
		}
		return temp;
	}
	public Set<String> getInputs(){
		return inputs;
	}

	public String[] getOutputsArray() {
		String[] temp = new String[outputs.size()];
		int index = -1;
		for(String s: outputs){
			index++;
			temp[index] = s;
		}
		return temp;
		}
	public Set<String> getOutputs(){
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
		Set<String> newInputs = new HashSet<String>();
		Set<String> newOutputs = new HashSet<String>();
		for(String s: inputs){
			newInputs.add(s);
		}
		for(String s: outputs){
			newOutputs.add(s);
		}
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
