package evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import gov.sandia.cognition.statistics.method.WilcoxonSignedRankConfidence;
import jsc.datastructures.PairedData;
import jsc.onesample.WilcoxonTest;
import jsc.tests.H1;
public class EvaluationMainTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadBestResults lb = new LoadBestResults();
		Map<String,List<Individule>> graphEvalResultsByDataset = lb.getEvalResults();
		Map<String,List<Individule>> neo4jResultsByDataset = lb.getNeo4jResults();
		NormalizeQos normalizeQos = new NormalizeQos(graphEvalResultsByDataset,neo4jResultsByDataset);

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
			PairedData pData = new PairedData(evalQos, neo4jQos);  
			double pValue = 0.0;
			if(evalQos.length>=500){  
//				if (less) {   
//					pValue = new WilcoxonTest(pData, H1.LESS_THAN).approxSP();  
//				}else {  
					pValue = new WilcoxonTest(pData).approxSP();  
//				}  
			}else {   
//				if (less) {
//					pValue = new WilcoxonTest(pData, H1.LESS_THAN).exactSP();  
//				}else {  
					pValue = new WilcoxonTest(pData).exactSP();  
//				}  
			}  
			System.out.println();
			System.out.println("File name: "+results.getKey()+"  P value: "+pValue);
			System.out.println();
			System.out.println();

		}
		
	}

}
