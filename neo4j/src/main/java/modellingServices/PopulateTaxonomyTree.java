package modellingServices;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import component.ServiceNode;
import component.TaxonomyNode;

public class PopulateTaxonomyTree {
	private Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();

	private Set<TaxonomyNode> children;
	private Set<TaxonomyNode> parents;
	//Key: taxonomyNode name Value: set of children
	private Map<String, Set<TaxonomyNode>> cs = new HashMap<String,Set<TaxonomyNode>>();
	//Key: taxonomyNode name Value: set of parents
	private Map<String, Set<TaxonomyNode>> ps = new HashMap<String,Set<TaxonomyNode>>();

	public PopulateTaxonomyTree(){
	}		
	public void setTaxonomyMap(Map<String, TaxonomyNode> taxonomyMap) {
		this.taxonomyMap = taxonomyMap;
	}

	public void setServiceMap(Map<String, ServiceNode> serviceMap) {
		this.serviceMap = serviceMap;
	}

	/**
	 * Populates the taxonomy tree by associating services to the
	 * nodes in the tree.
	 */
	public void populateTaxonomyTree() {
		//find all parents node and children node for each taxonomy node
		findAllParentsAndChildrenTNode();
		//remove inputs and outputs duplicates for each service node
		//if service node inputs A B C and in taxonomy file A is parent and BC are the children  then remove B C
		//if service outputs D E F and in taxonomy file D E are parents and F is the child  then remove D E 
		removeInputsAndOutputsDuplicates();
		System.out.println("removeInputsAndOutputsDuplicates");
		for (ServiceNode s: serviceMap.values()) {
			addServiceToTaxonomyTree(s);
		}
		System.out.println("addServiceToTaxonomyTree");

		//after add all services to taxonomyTree, for each service node, go through each inputs instance find all output services to this service node and add to outputsServicesToThisService set.
		// go through each outputs instance find all input services to this service node and add to inputsServicesToThisService set.
		addAllInputOutputServicesToEachServiceNode();
		System.out.println("addAllInputOutputServicesToEachServiceNode");
	}
	private void findAllParentsAndChildrenTNode(){
		Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
		Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
			TaxonomyNode n = entry.getValue();
			// Also add output to all parent nodes
			queue.clear();
			queue.add( n );
			while (!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsOutput.add( current );
					for (TaxonomyNode parent : current.parents) {
						if(!parent.value.equals("")){
							n.parentsString.add(parent.value);
							current.parents_notGrandparents.add(parent);
							if(!parent.value.equals("")){
								if (!seenConceptsOutput.contains( parent )) {
									queue.add(parent);
									seenConceptsOutput.add(parent);
								}
							}
						}
					}
				}
			}
			seenConceptsOutput.clear();
			queue.clear();
			queue.add(n);
			while (!queue.isEmpty()) {
				TaxonomyNode current2 = queue.poll();
				if(!current2.value.equals("")){
					seenConceptsInput.add( current2 );
					for (TaxonomyNode child : current2.children) {
						n.childrenString.add(child.value);
						current2.children_notGrandchildren.add(child);
						if(!child.value.equals("")){
							if (!seenConceptsInput.contains( child )) {
								queue.add(child);
								seenConceptsInput.add(child);
							}
						}
					}
				}
			}
			seenConceptsInput.clear();
		}
	}

	private void removeInputsAndOutputsDuplicates() {
		for(Entry<String, TaxonomyNode> entry : taxonomyMap.entrySet()){
			TaxonomyNode node = entry.getValue();
			children = new HashSet<TaxonomyNode>();
			parents = new HashSet<TaxonomyNode>();
			dfsGetChildren(node);
			dfsGetParents(node);
			cs.put(node.value, children);
			ps.put(node.value, parents);
		}
		removeChildrenInputs();
		removeParentsOutputs();
	}
	private void dfsGetChildren(TaxonomyNode root){
		if(root == null ||root.value.equals("")) return;
		//for every child
		for(TaxonomyNode n: root.children_notGrandchildren)
		{
			children.add(n);
			dfsGetChildren(n);
		}
	}

	private void dfsGetParents(TaxonomyNode root){
		if(root == null ||root.value.equals("")) return;
		//for every parents
		for(TaxonomyNode n: root.parents_notGrandparents)
		{
			parents.add(n);
			dfsGetParents(n);
		}
	}
	private void removeChildrenInputs(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			ServiceNode sNode = entry.getValue();
			Set<String> inputs = sNode.getInputs();
			Set<String> copy = new HashSet<String>(inputs);
			for(String input: inputs){
				if(!input.equals("")||input!=null){
					TaxonomyNode inputNode = taxonomyMap.get(input);
					input = inputNode.parentNode.value;
					if(cs.get(input).size()>0){
						Set<TaxonomyNode> children = cs.get(input);
						for(TaxonomyNode child:children){
							if(copy.contains(child.value) && !inputNode.value.equals(child.value)){
								copy.remove(child.value);
							}
						}
					}
				}
			}
			sNode.setInputs(copy);
		}
	}

	private void removeParentsOutputs(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			ServiceNode sNode = entry.getValue();
			Set<String> outputs = sNode.getOutputs();
			Set<String> copy = new HashSet<String>(outputs);
			for(String output: outputs){
				TaxonomyNode outputNode = taxonomyMap.get(output);
				output = outputNode.parentNode.value;
				if(cs.get(output).size()>0){
					Set<TaxonomyNode> parents = ps.get(output);
					for(TaxonomyNode parent:parents){
						String toRemve = getChildInst(parent);
						if(copy.contains(toRemve)){
							copy.remove(toRemve);
						}
					}
				}
			}
			sNode.setOutputs(copy);
		}
	}
	private void addServiceToTaxonomyTree(ServiceNode s) {
		// Populate outputs
		Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
		for (String outputVal : s.getOutputs()) {
			TaxonomyNode n = taxonomyMap.get(outputVal).parentNode;
			s.getTaxonomyOutputs().add(n);
			n.outputs.add(s.getName());
			queue.clear();
			// Also add output to all parent nodes
			queue.add( n );
			while (!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsOutput.add( current );
					current.servicesWithOutput.add(s);
					for (TaxonomyNode parent : current.parents) {
						current.parents_notGrandparents.add(parent);
						if(!parent.value.equals("")){
							parent.outputs.add(s.getName());
							if (!seenConceptsOutput.contains( parent )) {
								queue.add(parent);
								seenConceptsOutput.add(parent);
							}
						}
					}
				}
			}
		}
		seenConceptsOutput.clear();
		// Also add output to all parent nodes
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
		for (String inputVal : s.getInputs()) {
			TaxonomyNode n = taxonomyMap.get(inputVal).parentNode;
			s.getTaxonomyInputs().add(n);
			n.inputs.add(s.getName());
			// Also add output to all parent nodes
			queue.clear();
			queue.add( n );
			while (!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				if(!current.value.equals("")){
					seenConceptsInput.add( current );
					current.servicesWithInput.put(s, s.getInputs());
					for (TaxonomyNode child : current.children) {
						current.children_notGrandchildren.add(child);
						if(!child.value.equals("")){
							child.inputs.add(s.getName());
							if (!seenConceptsInput.contains( child )) {
								queue.add(child);
								seenConceptsOutput.add(child);
							}
						}
					}
				}
			}
		}		
		seenConceptsInput.clear();
		return;
	}
	private void addAllInputOutputServicesToEachServiceNode(){
		for(Entry<String, ServiceNode> entry : serviceMap.entrySet()) {
			Set<String> inputs = new HashSet<String>(entry.getValue().getInputs());
			Set<String> outputs = new HashSet<String>(entry.getValue().getOutputs());
			for(String serviceInput: inputs){
				TaxonomyNode tNode = taxonomyMap.get(serviceInput).parentNode;
				entry.getValue().inputsServicesToThisService.addAll(tNode.outputs);
				tNode = null;
			}
			for(String serviceOutput: outputs){
				TaxonomyNode tNode = taxonomyMap.get(serviceOutput).parentNode;
				entry.getValue().outputsServicesToThisService.addAll(tNode.inputs);
				tNode = null;
			}
		}
	}
	private String getChildInst(TaxonomyNode parent) {
		for(TaxonomyNode tNode: parent.children_notGrandchildren){
			if(tNode.childNode==null){
				return tNode.value;
			}
		}
		return null;
	}

}
