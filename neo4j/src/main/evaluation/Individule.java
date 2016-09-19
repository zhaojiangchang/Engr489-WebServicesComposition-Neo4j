package evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Individule {
	Map<String, Double>qos = new HashMap<String, Double>();
	List<String>services = new ArrayList<String>();
	public Individule(Map<String, Double> qos, List<String>services){
		this.qos = qos;
		this.services = services;
	}

}
