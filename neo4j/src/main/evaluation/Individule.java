package evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Individule {
	Map<String, Double>qos = new HashMap<String, Double>();
	Map<String, Double>normalizedQos;
	double fitnessValue = 0.00;
	double runningTime = 0.0;
	List<String>services = new ArrayList<String>();
	private final double m_a = 0.25;
	private final double m_r = 0.25;
	private final double m_c = 0.25;
	private final double m_t = 0.25;
	public Individule(Map<String, Double> qos, List<String>services, double runningTime){
		this.qos = qos;
		this.services = services;
		this.normalizedQos = new HashMap<String, Double>();
		this.runningTime = runningTime;
	}
	public double fitness(){
		return m_a*normalizedQos.get("A") + m_r*normalizedQos.get("R") + m_c*normalizedQos.get("C") + m_t*normalizedQos.get("T");
	}

}
