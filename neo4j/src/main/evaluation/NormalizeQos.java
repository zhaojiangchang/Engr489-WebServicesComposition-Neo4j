package evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalizeQos {
	Map<String,List<Individule>> graphEvalResultsByDataset = new HashMap<String, List<Individule>>();
	Map<String,List<Individule>> neo4jResultsByDataset = new HashMap<String, List<Individule>>();
	Map<String,List<Individule>> normalizedGraphEvalResultsByDataset = new HashMap<String, List<Individule>>();
	Map<String,List<Individule>> normalizedNeo4jResultsByDataset = new HashMap<String, List<Individule>>();
	Map<String,Map<String,Double>> minMaxQosWithFileName = new HashMap<String,Map<String,Double>>();
	List<Individule> graphEvalResults = new ArrayList<Individule>();
	List<Individule> neo4jResults = new ArrayList<Individule>();
	List<Individule> normalizedGraphEvalResults = new ArrayList<Individule>();
	List<Individule> normalizedNeo4jResults = new ArrayList<Individule>();
	
	public NormalizeQos(Map<String,List<Individule>> graphEvalResultsByDataset,Map<String,List<Individule>> neo4jResultsByDataset){
		this.graphEvalResultsByDataset = graphEvalResultsByDataset;
		this.neo4jResultsByDataset = neo4jResultsByDataset;
		init();
		
	}
	private void init(){
		getMinMaxValues(graphEvalResultsByDataset);
		getMinMaxValues(neo4jResultsByDataset);
		


	}
	
	private void getMinMaxValues(Map<String, List<Individule>> resultsByDataset){
		for(Map.Entry<String, List<Individule>>bestResultWithFileName: graphEvalResultsByDataset.entrySet()){
			Map<String,Double> minMax = new HashMap<String,Double>();
			double minA = Double.MAX_VALUE;
			double maxA = Double.MIN_VALUE;
			double minR = Double.MAX_VALUE;
			double maxR = Double.MIN_VALUE;
			double minC = Double.MAX_VALUE;
			double maxC = Double.MIN_VALUE;
			double minT = Double.MAX_VALUE;
			double maxT = Double.MIN_VALUE;
			System.out.println(bestResultWithFileName.getKey());
			for(Individule i: bestResultWithFileName.getValue()){
				Map<String, Double>qos = i.qos;
				for(Map.Entry<String, Double> qosValue: qos.entrySet()){
					if(qosValue.getKey().equals("A")){
						if(qosValue.getValue()>maxA){
							maxA = qosValue.getValue();
						}
						if(qosValue.getValue()<minA){
							minA = qosValue.getValue();
						}
					}
					else if(qosValue.getKey().equals("R")){
						if(qosValue.getValue()>maxR){
							maxR = qosValue.getValue();
						}
						if(qosValue.getValue()<minR){
							minR = qosValue.getValue();
						}
					}
					else if(qosValue.getKey().equals("C")){
						if(qosValue.getValue()>maxC){
							maxC = qosValue.getValue();
						}
						if(qosValue.getValue()<minC){
							minC = qosValue.getValue();
						}
					}
					else if(qosValue.getKey().equals("T")){
						if(qosValue.getValue()>maxT){
							maxT = qosValue.getValue();
						}
						if(qosValue.getValue()<minT){
							minT = qosValue.getValue();
						}
					}
				}

			}
			minMax.put("minA", minA);
			minMax.put("maxA", maxA);
			minMax.put("minR", minR);
			minMax.put("maxR", maxR);
			minMax.put("minC", minC);
			minMax.put("maxC", maxC);
			minMax.put("minT", minT);
			minMax.put("maxT", maxT);
			
			minMaxQosWithFileName.put(bestResultWithFileName.getKey(),minMax);
			System.out.println(minA+"   "+maxA+"   "+minR+"   "+maxR+"   "+minC+"   "+maxC+"   "+minT+"   "+maxT);
		}
	}
}
