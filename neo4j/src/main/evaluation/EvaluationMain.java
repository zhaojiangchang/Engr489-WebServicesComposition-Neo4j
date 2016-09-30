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
		printTimeForEachDataset("neo4jResults:  ",neo4jResultsByDataset);
		printTimeForEachDataset("graphEvalResults:  ",graphEvalResultsByDataset);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadBestResults lb = new LoadBestResults();
		Map<String,List<Individule>> graphEvalResultsByDataset = lb.getEvalResults();
		Map<String,List<Individule>> neo4jResultsByDataset = lb.getNeo4jResults();
		NormalizeQos normalizeQos = new NormalizeQos(graphEvalResultsByDataset,neo4jResultsByDataset);
		calculatePValues(normalizeQos);
		System.out.println("Calculate Mean and SD ");
		calculateMeanAndSD("neo4jResults:  ",neo4jResultsByDataset, normalizeQos.getNeo4jFitnessValues());
		calculateMeanAndSD("graphEvalResults:  ",neo4jResultsByDataset, normalizeQos.getGraphEvalFitnessValues());
		System.out.println();
		System.out.println();
		System.out.println("compare runing time: ");
		printTimeForEachDataset("neo4jResults:  ",neo4jResultsByDataset);
		System.out.println();
		printTimeForEachDataset("graphEvalResults:  ",graphEvalResultsByDataset);

	}

	private static void printTimeForEachDataset(String string, Map<String, List<Individule>> map) {
		System.out.println("  Program: "+string);
		for(Map.Entry<String, List<Individule>> fileNameWithFitnessValues: map.entrySet() ){
			System.out.println("  \t "+fileNameWithFitnessValues.getKey());
			double total = 0.0;
			for(Individule individule: fileNameWithFitnessValues.getValue()){
				total += individule.runningTime;
			}
			System.out.println("  \t File: "+fileNameWithFitnessValues.getKey()+"\t average time: "+ total/fileNameWithFitnessValues.getValue().size());
		}
		
	}
	private static void calculateMeanAndSD(String string, Map<String, List<Individule>> resultsByDataset, Map<String, List<Double>> map) {
		System.out.println("\t Program: "+string);
		for(Map.Entry<String, List<Double>> fileNameWithFitnessValues: map.entrySet() ){
			Statistics statistics = new Statistics(fileNameWithFitnessValues.getValue());
			System.out.println("\t    File: "+fileNameWithFitnessValues.getKey()+"    Mean: "+statistics.getMean() +"     SD: "+statistics.getStdDev());			
		}
	}


	private static void calculatePValues(NormalizeQos normalizeQos) {
		System.out.println("calculate pValue: ");
		for(Map.Entry<String, List<Double>> results: normalizeQos.graphEvalFitnessValues.entrySet()){	
			System.out.println( "\t File name: "+results.getKey());

			double[]evalQos = new double[results.getValue().size()];
			System.out.println();
			System.out.println("\t graphEvalResultsByDataset:");
			for(int i = 0; i<results.getValue().size(); i++){
				evalQos[i] = results.getValue().get(i);
				System.out.print( "\t "+results.getValue().get(i)+"   ");
			}
			System.out.println();
			System.out.println();
			double[]neo4jQos = new double[normalizeQos.neo4jFitnessValues.get(results.getKey()).size()];

			System.out.println("\t neo4jResultsByDataset:");
			for(int i = 0; i<normalizeQos.neo4jFitnessValues.get(results.getKey()).size(); i++){
				neo4jQos[i] = normalizeQos.neo4jFitnessValues.get(results.getKey()).get(i);
				System.out.print(  "\t "+normalizeQos.neo4jFitnessValues.get(results.getKey()).get(i)+"   ");

			}
			System.out.println();
			TwoSampleTtest TwoSampleTtest = new TwoSampleTtest(evalQos, neo4jQos, H1.NOT_EQUAL, false, 0.95);
			System.out.println();
			System.out.println( "\t P value (Not Equal): "+TwoSampleTtest.getSP());
			System.out.println( "\t T value (Not Equal): "+TwoSampleTtest.getTestStatistic());
			System.out.println( "\t Mean Eval: "+TwoSampleTtest.getMeanA());
			System.out.println( "\t Mean Neo4j: "+TwoSampleTtest.getMeanB());
			System.out.println( "\t SD Eval: "+TwoSampleTtest.getSdA());
			System.out.println( "\t SD Neo4j: "+TwoSampleTtest.getSdB());
			System.out.println( "\t MeanDiff: "+TwoSampleTtest.getMeanDiff());
			

			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();		
		}

	}
}
