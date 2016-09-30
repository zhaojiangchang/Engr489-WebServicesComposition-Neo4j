package evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalizeQos {
	private Map<String,List<Individule>> graphEvalResultsByDataset = new HashMap<String, List<Individule>>();
	private Map<String,List<Individule>> neo4jResultsByDataset = new HashMap<String, List<Individule>>();

	//fitness values for each dataset
	public Map<String, List<Double>>graphEvalFitnessValues = new HashMap<String, List<Double>>();
	public Map<String, List<Double>>neo4jFitnessValues = new HashMap<String, List<Double>>();


	//Map<dataset1, Map<minA, A value>
	private Map<String,Map<String,Double>> minMaxQosWithFileNames = new HashMap<String,Map<String,Double>>();


	public NormalizeQos(Map<String,List<Individule>> graphEvalResultsByDataset,Map<String,List<Individule>> neo4jResultsByDataset){
		this.graphEvalResultsByDataset = graphEvalResultsByDataset;
		this.neo4jResultsByDataset = neo4jResultsByDataset;
		init();

	}
	private void init(){
		getMinMaxValues();
		normalizeAllDataset(graphEvalResultsByDataset);
		normalizeAllDataset(neo4jResultsByDataset);
		graphEvalFitnessValues = fitnessByFile(graphEvalResultsByDataset);
		neo4jFitnessValues = fitnessByFile(neo4jResultsByDataset);

	}


	private Map<String, List<Double>> fitnessByFile(Map<String, List<Individule>> resultsByDataset) {
		Map<String, List<Double>>fitnessValues = new HashMap<String, List<Double>>();
		for(Map.Entry<String, List<Individule>> results: resultsByDataset.entrySet()){	
			List<Double> fitness = new ArrayList<Double>();
			for(Individule individule: results.getValue()){
				fitness.add(individule.fitnessValue);
			}
			fitnessValues.put(results.getKey(), fitness);
		}	
		return fitnessValues;
	}


	private void normalizeAllDataset(Map<String, List<Individule>> resultsByDataset) {
		for(Map.Entry<String, List<Individule>> results: resultsByDataset.entrySet()){	
			Map<String, Double> minMax = minMaxQosWithFileNames.get(results.getKey());
			for(Individule individule: results.getValue()){
				Map<String,Double>qos = individule.qos;
				for(Map.Entry<String, Double> values: qos.entrySet()){
					normalize(values.getKey(), values.getValue(), minMax, individule);
				}
				individule.fitnessValue = individule.fitness();
			}

		}
	}
	private void normalize(String qosAttr, double value, Map<String, Double> minMax, Individule individule) {
		if(qosAttr.equals("A")){
			if(minMax.get("maxA")-minMax.get("minA") == 0)
				individule.normalizedQos.put("A", 1.0);
			else{
				individule.normalizedQos.put("A", (value - minMax.get("minA"))/(minMax.get("maxA")-minMax.get("minA")));
			}
		}
		else if(qosAttr.equals("R")){
			if(minMax.get("maxR")-minMax.get("minR") == 0)
				individule.normalizedQos.put("R", 1.0);
			else{
				individule.normalizedQos.put("R",  (value - minMax.get("minR"))/(minMax.get("maxR")-minMax.get("minR")));
			}
		}
		else if(qosAttr.equals("C")){
			if(minMax.get("maxC")-minMax.get("minC") == 0)
				individule.normalizedQos.put("C", 1.0);
			else{
				individule.normalizedQos.put("C", (minMax.get("maxC")- value)/(minMax.get("maxC")-minMax.get("minC")));
			}
		}	
		else if(qosAttr.equals("T")){
			if(minMax.get("maxT")-minMax.get("minT") == 0)
				individule.normalizedQos.put("T", 1.0);
			else{
				individule.normalizedQos.put("T",  (minMax.get("maxT")- value)/(minMax.get("maxT")-minMax.get("minT")));
			}
		}	
	}

	private void getMinMaxValues(){
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

			
			for(Individule i: neo4jResultsByDataset.get(bestResultWithFileName.getKey())){
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
			
			System.out.println("Dataset: "+bestResultWithFileName.getKey());
			System.out.println("minA: "+minA+" maxA: "+maxA+" minR: "+minR+" maxR: "+maxR+" minC:  "+minC+" maxC:  "+maxC+" minT:  "+minT+" maxT:  "+maxT);
			minMaxQosWithFileNames.put(bestResultWithFileName.getKey(),minMax);
			System.out.println();
		}
	}
	public Map<String, List<Double>> getGraphEvalFitnessValues() {
		return graphEvalFitnessValues;
	}
	public Map<String, List<Double>> getNeo4jFitnessValues() {
		return neo4jFitnessValues;
	}
	
}
