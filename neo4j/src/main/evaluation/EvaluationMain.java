package evaluation;

import java.util.List;
import java.util.Map;
import jsc.independentsamples.TwoSampleTtest;

import jsc.tests.H1;
public class EvaluationMain {

	public EvaluationMain(){
		LoadBestResults lb = new LoadBestResults();
		Map<String,List<Individule>> graphEvalResultsByDataset = lb.getEvalResults();
		Map<String,List<Individule>> neo4jResultsByDataset = lb.getNeo4jResults();
		NormalizeQos normalizeQos = new NormalizeQos(graphEvalResultsByDataset,neo4jResultsByDataset);
		calculatePValues(normalizeQos);
		calculateMeanAndSD("neo4jResults:  ",neo4jResultsByDataset, normalizeQos.getNeo4jFitnessValues());
		calculateMeanAndSD("graphEvalResults:  ",neo4jResultsByDataset, normalizeQos.getGraphEvalFitnessValues());
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadBestResults lb = new LoadBestResults();
		Map<String,List<Individule>> graphEvalResultsByDataset = lb.getEvalResults();
		Map<String,List<Individule>> neo4jResultsByDataset = lb.getNeo4jResults();
		NormalizeQos normalizeQos = new NormalizeQos(graphEvalResultsByDataset,neo4jResultsByDataset);
		calculatePValues(normalizeQos);
		calculateMeanAndSD("neo4jResults:  ",neo4jResultsByDataset, normalizeQos.getNeo4jFitnessValues());
		calculateMeanAndSD("graphEvalResults:  ",neo4jResultsByDataset, normalizeQos.getGraphEvalFitnessValues());

	}

	private static void calculateMeanAndSD(String string, Map<String, List<Individule>> resultsByDataset, Map<String, List<Double>> map) {
		System.out.println("Program: "+string);
		for(Map.Entry<String, List<Double>> fileNameWithFitnessValues: map.entrySet() ){
			Statistics statistics = new Statistics(fileNameWithFitnessValues.getValue());
			System.out.println("File: "+fileNameWithFitnessValues.getKey()+"    Mean: "+statistics.getMean() +"     SD: "+statistics.getStdDev());			
		}
	}


	private static void calculatePValues(NormalizeQos normalizeQos) {
		for(Map.Entry<String, List<Double>> results: normalizeQos.graphEvalFitnessValues.entrySet()){	

			double[]evalQos = new double[results.getValue().size()];
			System.out.println();
			System.out.println("graphEvalResultsByDataset:");
			for(int i = 0; i<results.getValue().size(); i++){
				evalQos[i] = results.getValue().get(i);
				System.out.print( results.getValue().get(i)+"   ");
			}
			System.out.println();
			System.out.println();
			double[]neo4jQos = new double[normalizeQos.neo4jFitnessValues.get(results.getKey()).size()];

			System.out.println("neo4jResultsByDataset:");
			for(int i = 0; i<normalizeQos.neo4jFitnessValues.get(results.getKey()).size(); i++){
				neo4jQos[i] = normalizeQos.neo4jFitnessValues.get(results.getKey()).get(i);
				System.out.print( normalizeQos.neo4jFitnessValues.get(results.getKey()).get(i)+"   ");

			}
			System.out.println();
			TwoSampleTtest TwoSampleTtest = new TwoSampleTtest(evalQos, neo4jQos, H1.GREATER_THAN, false, 0.95);
			double pValue = TwoSampleTtest.getSP();
			System.out.println();
			System.out.println("File name: "+results.getKey()+"  P value: "+pValue);
			System.out.println();
			System.out.println();		
		}

	}
}
