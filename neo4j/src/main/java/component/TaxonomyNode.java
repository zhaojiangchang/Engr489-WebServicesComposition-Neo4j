package component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a node in the input/output taxonomy
 * used by the WSC dataset.
 *
 * @author sawczualex
 */
public class TaxonomyNode {
	public Set<String> endNodeInputs = new HashSet<String>();
	public Set<String> inputs;
	public Set<String> outputs;
	public Set<ServiceNode> servicesWithOutput = new HashSet<ServiceNode>();
	public Map<ServiceNode,Set<String>> servicesWithInput = new HashMap<ServiceNode,Set<String>>();
	public String value = null;
	public String parent = "";
	public String child = "";
	public Set<String> childrenString;
	public Set<String> parentsString;
	public Set<TaxonomyNode> childrenInstance = new HashSet<TaxonomyNode>();
	public TaxonomyNode parentNode = null;
	public TaxonomyNode childNode = null;
	public Set<TaxonomyNode> children_notGrandchildren = new HashSet<TaxonomyNode>();
	public Set<TaxonomyNode> parents_notGrandparents = new HashSet<TaxonomyNode>();
	public boolean isInstance = false;
	public List<TaxonomyNode> parents = new ArrayList<TaxonomyNode>();
	public List<TaxonomyNode> children = new ArrayList<TaxonomyNode>();
	public String state = "Unvisited";
	
	public TaxonomyNode(String value, String tagName) {
		if(tagName.equals("instance")){
			this.isInstance = true;
		}
		this.value = value;
		inputs = new HashSet<String>();
		outputs = new HashSet<String>();
		childrenString = new HashSet<String>();
		parentsString= new HashSet<String>();
	}
	public String[] getInputsArray() {
//		System.out.println("inputs: "+inputs.size());
		String[] temp = new String[inputs.size()];
		int index = -1;
		for(String s: inputs){
			index++;
			temp[index] = s;
		}
		return temp;
	}
	public String[] getOutputsArray() {
//		System.out.println("outputs: "+outputs.size());
		String[] temp = new String[outputs.size()];
		int index = -1;
		for(String s: outputs){
			index++;
			temp[index] = s;
		}
		return temp;
	}
	public void setParent(String t){
		parent = t;
		}
	public String getParent(){
		return parent;
	}
	public boolean hasParent(){
		if(parent.equals("")){
			return false;
		}
		else{
			return true; 
		}
	}
	public boolean hasChild(){
		if(child.equals("")){
			return false;
		}
		else{
			return true; 
		}
	}
	public void addParent (String s){
		this.parentsString.add(s);
	}
	public void addChild (String s){
		this.childrenString.add(s);
	}
	public void setChildNode(TaxonomyNode c){
		this.child = c.value;
		this.childNode = c;
	}
	public TaxonomyNode getParentNode(){
		return parentNode;
	}
	public void setParentNode(TaxonomyNode t){
		this.parentNode = t;
	}
	public boolean hasParentNode(){
		if(parentNode!=null){
			return true;
		}
		else return false;
	}
	public boolean hasChildNode(){
		if(childNode!=null){
			return true;
		}
		else return false;
	}
	

	/**
	 * Gets all concepts subsumed by this node (i.e. all
	 * concepts in its subtree).
	 *
	 * @return Set of concepts
	 */
	public Set<String> getSubsumedConcepts() {
		Set<String> concepts = new HashSet<String>();
        _getSubsumedConcepts( concepts );
		return concepts;
	}

	/*
	 * This recursively subsume all the descendent concepts of the current node
	 */
    private void _getSubsumedConcepts(Set<String> concepts) {
        if (!concepts.contains( value )) {
            concepts.add(value);
            for (TaxonomyNode child : children) {
                child._getSubsumedConcepts(concepts);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TaxonomyNode) {
            return ((TaxonomyNode)other).value.equals( value );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
    public String[] increaseArray(String[] theArray)
	{
	    int i = theArray.length;
	    int n = ++i;
	    String[] newArray = new String[n];
	    for(int cnt=0;cnt<theArray.length;cnt++)
	    {
	        newArray[cnt] = theArray[cnt];
	    }
	    return newArray;
	}
}
