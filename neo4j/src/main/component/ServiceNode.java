package component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceNode implements Cloneable {
	private List<TaxonomyNode> taxonomyOutputs = new ArrayList<TaxonomyNode>();
	private List<TaxonomyNode> taxonomyInputs = new ArrayList<TaxonomyNode>();
	private String name;
	private double[] qos;
	public Set<String>inputs;
	public Set<String>outputs;
	private boolean consider = true;
	public Set<String>outputsServicesToThisService;
	public Set<String>inputsServicesToThisService;
	public long index;

	public ServiceNode(String name, double[] qos, Set<String> inputs, Set<String> outputs, long index) {
		this.index = index;
		this.name = name;
		this.qos = qos;
		this.inputs = inputs;
		this.outputs = outputs;
		inputsServicesToThisService = new HashSet<String>();
		outputsServicesToThisService = new HashSet<String>();

	}

	public double[] getQos() {
		return qos;
	}

	public String[] getInputsServiceArray() {
		String[] temp = new String[inputsServicesToThisService.size()];
		int index = -1;
		for(String s: inputsServicesToThisService){
			index++;
			temp[index] = s;
		}
		return temp;
	}
	public Set<String> getInputs(){
		return inputs;
	}
	
	public String[] getInputsArray(){
		String[] temp = new String[inputs.size()];
		int index = -1;
		for(String s: inputs){
			index++;
			temp[index] = s;
		}
		return temp;
	}
	public String[] getOutputsArray(){
		String[] temp = new String[outputs.size()];
		int index = -1;
		for(String s: outputs){
			index++;
			temp[index] = s;
		}
		return temp;
	}
	public String[] getOutputsServiceArray() {
		String[] temp = new String[outputsServicesToThisService.size()];
		int index = -1;
		for(String s: outputsServicesToThisService){
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
		return new ServiceNode(name, qos, newInputs, newOutputs, this.index);
	}

	public List<TaxonomyNode> getTaxonomyOutputs() {
		return taxonomyOutputs;
	}
	public List<TaxonomyNode> getTaxonomyInputs() {
		return taxonomyInputs;
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

	public void setInputs(Set<String> copy) {
		this.inputs = copy;
	}

	public void setOutputs(Set<String> copy) {
		this.outputs = copy;
	}
	public void addInputs(String service) {
		this.inputs.add(service);
	}

	public void addOutputs(String service) {
		this.outputs.add(service);
	}
}