package evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jsc.independentsamples.TwoSampleTtest;

import jsc.tests.H1;
public class EvaluationMain {
	private static final String[] filenames = { 
			"2008-dataset01.stat","2008-dataset02.stat","2008-dataset03.stat","2008-dataset04.stat","2008-dataset05.stat","2008-dataset06.stat","2008-dataset07.stat","2008-dataset08.stat",
	};
	public EvaluationMain(){
		LoadBestResults lb = new LoadBestResults();
		Map<String,List<Individule>> graphEvolResultsByDataset = lb.getEvalResults();
		Map<String,List<Individule>> neo4jResultsByDataset = lb.getNeo4jResults();
		NormalizeQos normalizeQos = new NormalizeQos(graphEvolResultsByDataset,neo4jResultsByDataset);
		calculateFitnessPValues(normalizeQos);
		calculateMeanAndSD("neo4jResults:  ",neo4jResultsByDataset, normalizeQos.getNeo4jFitnessValues());
		calculateMeanAndSD("graphEvalResults:  ",neo4jResultsByDataset, normalizeQos.getGraphEvalFitnessValues());
		printTimeForEachDataset("neo4jResults:  ",neo4jResultsByDataset);
		printTimeForEachDataset("graphEvalResults:  ",graphEvolResultsByDataset);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadBestResults lb = new LoadBestResults();
		Map<String,List<Individule>> graphEvolResultsByDataset = lb.getEvalResults();
		Map<String,List<Individule>> neo4jResultsByDataset = lb.getNeo4jResults();
		NormalizeQos normalizeQos = new NormalizeQos(graphEvolResultsByDataset,neo4jResultsByDataset);
		calculateFitnessPValues(normalizeQos);
		System.out.println("Calculate Mean and SD ");
		calculateMeanAndSD("neo4jResults:  ",neo4jResultsByDataset, normalizeQos.getNeo4jFitnessValues());
		calculateMeanAndSD("graphEvalResults:  ",neo4jResultsByDataset, normalizeQos.getGraphEvalFitnessValues());
		System.out.println();
		System.out.println();
		System.out.println("compare runing time: ");
		Map<String,List<Double>> neo4jRunTimes = printTimeForEachDataset("neo4jResults:  ",neo4jResultsByDataset);
		System.out.println();
		Map<String,List<Double>> EvolRunTimes = printTimeForEachDataset("graphEvalResults:  ",graphEvolResultsByDataset);
		calculatePValueTime(neo4jRunTimes, EvolRunTimes);

	}

	private static void calculatePValueTime(Map<String, List<Double>> neo4jRunTimes, Map<String, List<Double>> evolRunTimes) {
		for(String filename: filenames){
			double[] evalTimes = new double[ evolRunTimes.get(filename).size()];
			for(int i = 0; i< evolRunTimes.get(filename).size(); i++){
				evalTimes[i] = evolRunTimes.get(filename).get(i);
			}
			double[] neo4jTimes = new double[ neo4jRunTimes.get(filename).size()];
			for(int i = 0; i< neo4jRunTimes.get(filename).size(); i++){
				neo4jTimes[i] = neo4jRunTimes.get(filename).get(i);
			}
					
			
			TwoSampleTtest TwoSampleTtest = new TwoSampleTtest(evalTimes, neo4jTimes, H1.GREATER_THAN, false, 0.95);
			System.out.println("Calculate Time mean, sd and P value: "+filename);
			System.out.println( "\t P value (Not Equal): "+TwoSampleTtest.getSP());
			System.out.println( "\t T value (Not Equal): "+TwoSampleTtest.getTestStatistic());
			System.out.println( "\t Mean Evol: "+TwoSampleTtest.getMeanA());
			System.out.println( "\t Mean Neo4j: "+TwoSampleTtest.getMeanB());
			System.out.println( "\t SD Evol: "+TwoSampleTtest.getSdA());
			System.out.println( "\t SD Neo4j: "+TwoSampleTtest.getSdB());
			System.out.println( "\t MeanDiff: "+TwoSampleTtest.getMeanDiff());
		}
		
	}
	private static Map<String,List<Double>> printTimeForEachDataset(String string, Map<String, List<Individule>> map) {
		System.out.println("  Program: "+string);
		Map<String, List<Double>> runTimes = new HashMap<String, List<Double>>();
		for(Map.Entry<String, List<Individule>> fileNameWithFitnessValues: map.entrySet() ){
			
			System.out.println("  \t "+fileNameWithFitnessValues.getKey());
			List<Double> time= new ArrayList<Double>(); 
			List<Double> serviceSize = new ArrayList<Double>();
			for(Individule individule: fileNameWithFitnessValues.getValue()){
				time.add(individule.runningTime);
				serviceSize.add((double)individule.services.size());
			}
//			Statistics statisticsTime = new Statistics(time);
			Statistics statisticsServiceSize = new Statistics(serviceSize);

//			System.out.println("  \t File: "+fileNameWithFitnessValues.getKey()+"\t mean time: "+ statisticsTime.getMean()+"    SD: "+statisticsTime.getStdDev());
			System.out.println("  \t File: "+fileNameWithFitnessValues.getKey()+"\t mean serviceSize: "+ statisticsServiceSize.getMean() +"    SD: "+statisticsServiceSize.getStdDev());
			
			runTimes.put(fileNameWithFitnessValues.getKey(), time);			
		
		}
		return runTimes;
		
	}
	private static void calculateMeanAndSD(String string, Map<String, List<Individule>> resultsByDataset, Map<String, List<Double>> map) {
		System.out.println("\t Program: "+string);
		for(Map.Entry<String, List<Double>> fileNameWithFitnessValues: map.entrySet() ){
			Statistics statistics = new Statistics(fileNameWithFitnessValues.getValue());
			System.out.println("\t    File: "+fileNameWithFitnessValues.getKey()+"    Mean: "+statistics.getMean() +"     SD: "+statistics.getStdDev());			
		}
	}


	private static void calculateFitnessPValues(NormalizeQos normalizeQos) {
		System.out.println("calculate pValue: ");
		for(Map.Entry<String, List<Double>> results: normalizeQos.graphEvalFitnessValues.entrySet()){	
			System.out.println( "\t File name: "+results.getKey());

			double[]evalQos = new double[results.getValue().size()];
			System.out.println();
			System.out.println("\t graphEvolResultsByDataset:");
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
			TwoSampleTtest TwoSampleTtest = new TwoSampleTtest(evalQos, neo4jQos, H1.LESS_THAN, false, 0.95);
			System.out.println();
			System.out.println( "\t P value (Not Equal): "+TwoSampleTtest.getSP());
			System.out.println( "\t T value (Not Equal): "+TwoSampleTtest.getTestStatistic());
			System.out.println( "\t Mean Evol: "+TwoSampleTtest.getMeanA());
			System.out.println( "\t Mean Neo4j: "+TwoSampleTtest.getMeanB());
			System.out.println( "\t SD Evol: "+TwoSampleTtest.getSdA());
			System.out.println( "\t SD Neo4j: "+TwoSampleTtest.getSdB());
			System.out.println( "\t MeanDiff: "+TwoSampleTtest.getMeanDiff());
			

			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();		
		}

	}
}
