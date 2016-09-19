package evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EvaluationMainTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadBestResults lb = new LoadBestResults();
		Map<String,List<Individule>> graphEvalResultsByDataset = lb.getEvalResults();
		Map<String,List<Individule>> neo4jResultsByDataset = lb.getNeo4jResults();
		NormalizeQos normalizeQos = new NormalizeQos(graphEvalResultsByDataset,neo4jResultsByDataset);

	}

}
