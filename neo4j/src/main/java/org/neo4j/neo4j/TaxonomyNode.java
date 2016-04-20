package org.neo4j.neo4j;

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
	public String value;
	public String parent = "";
	public String[] children = new String[0];
	public String[] subChildren = new String[0];
	public String[] parents = new String[0];
	public TaxonomyNode parentNode = null;

	public TaxonomyNode(String value) {
		this.value = value;
	}
	
	public void addChild(String t){
		children = increaseArray(children);
		children[children.length-1] = t;
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
	public String[] getChildren(){
		return children;
	}
	public TaxonomyNode getParentNode(){
		return parentNode;
	}
	public void setParentNode(TaxonomyNode t){
		this.parentNode = t;
	}
	public void addSubChildren(String s){
		subChildren = increaseArray(subChildren);
		subChildren[subChildren.length-1] = s;			
	}
	public String[] getSubChildren(){
		return subChildren;
	}
	public void addParents(String s){
		parents = increaseArray(parents);
		parents[parents.length-1] = s;			
	}
	public String[] getParents(){
		return parents;
	}
	/**
	 * Gets all concepts subsumed by this node (i.e. all
	 * concepts in its subtree).
	 *
	 * @return Set of concepts
	 */
//	public Set<String> getSubsumedConcepts() {
//		Set<String> concepts = new HashSet<String>();
//        _getSubsumedConcepts( concepts );
//		return concepts;
//	}

	/*
	 * This recursively subsume all the descendent concepts of the current node
	 */
//    private void _getSubsumedConcepts(Set<String> concepts) {
//        if (!concepts.contains( value )) {
//            concepts.add(value);
//            for (TaxonomyNode child : children) {
//                child._getSubsumedConcepts(concepts);
//            }
//        }
//    }

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
