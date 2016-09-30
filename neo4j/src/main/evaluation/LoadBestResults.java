package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFileChooser;

public class LoadBestResults {

//	List<Individule> graphEvalResults = new ArrayList<Individule>();
//	List<Individule> neo4jResults = new ArrayList<Individule>();
	Map<String,List<Individule>> graphEvalResultsByDataset = new HashMap<String, List<Individule>>();
	Map<String,List<Individule>> neo4jResultsByDataset = new HashMap<String, List<Individule>>();

	String datasetDirName = "";
	public LoadBestResults(){
		try {
			run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main( String[] args ) {
		new LoadBestResults();
	}
	public void run() throws IOException{
		Map<String, BufferedReader>bufferedReadersGraphEval = loadFiles("evaluationGraphEvalResults");
		Map<String, BufferedReader>bufferedReadersNeo4j = loadFiles("evaluationNeo4jResults");

		graphEvalResultsByDataset = readFiles("graphEval",bufferedReadersGraphEval);
		
		neo4jResultsByDataset = readFiles("neo4j",bufferedReadersNeo4j);

	}
	public Map<String,List<Individule>> getEvalResults(){
		return graphEvalResultsByDataset;
	}
	public Map<String,List<Individule>> getNeo4jResults(){
		return neo4jResultsByDataset;
	}
	private Map<String,List<Individule>> readFiles(String id, Map<String, BufferedReader> bufferedReaders ) throws IOException {
		Map<String,List<Individule>> resultByDataset = new HashMap<String, List<Individule>>();

		//		for(int j = 0; j<bufferedReaders.size(); j++){
		for(Map.Entry<String, BufferedReader> bfWithFileName:bufferedReaders.entrySet()){
			long averagePreprocessTimeRunForEachDatabase = 0;
			long total = 0;
			if(id.equals("neo4j")){
				File file = new File("preprocessNeo4jTimeRunForEachDataset/"+bfWithFileName.getKey());
		        BufferedReader reader = null;
		        try {

		            reader = new BufferedReader(new FileReader(file));
		            String tempString = null;
		            int line = 1;

		            while ((tempString = reader.readLine()) != null) {
		                String[] timeAsArray = tempString.trim().split("\\s+");
		                total += Double.parseDouble(timeAsArray[1]);
		                line++;
		            }
		            averagePreprocessTimeRunForEachDatabase = total/line;
		            System.out.println("preprocess time for "+ bfWithFileName.getKey()+":  "+averagePreprocessTimeRunForEachDatabase);
		            reader.close();
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		            if (reader != null) {
		                try {
		                    reader.close();
		                } catch (IOException e1) {
		                }
		            }
		        }
			}
			String fileName = bfWithFileName.getKey();
			BufferedReader fb = bfWithFileName.getValue();
			List<Individule> results = new ArrayList<Individule>();
			while(true){
				double runningTime = 0.0;
				String time = fb.readLine();
				if(time == null){
					break;
				}   
				String[] timeAsArray = time.trim().split("\\s+");
				runningTime = Double.parseDouble(timeAsArray[0]);
				if(id.equals("neo4j")){
					runningTime = runningTime +averagePreprocessTimeRunForEachDatabase;
				}
				String qosString = fb.readLine();
				if(qosString == null){
					break;
				}        		
				String[] qosValues = qosString.trim().split("\\s+");
//				List<Double> qos = new ArrayList<Double>();
				Map<String, Double>qos = new HashMap<String,Double>();
				if(qosValues.length==4){
					qos.put("A", Double.parseDouble(qosValues[0]));
					qos.put("R", Double.parseDouble(qosValues[1]));
					qos.put("T", Double.parseDouble(qosValues[2]));
					qos.put("C", Double.parseDouble(qosValues[3]));
					
				}
				

				String serviceString = fb.readLine();
				if(serviceString == null){
					break;
				}
				List<String> services = Arrays.asList(serviceString.trim().split("\\s+"));
				results.add(new Individule(qos, services, runningTime));      
			}
			resultByDataset.put(fileName, results);
		}
		return resultByDataset;
	}
	//	private boolean containsKey(List<String> nodes) {
	//		for(List<String> nodeList: graphEvalResults.keySet()){
	//			List<String>temp = new ArrayList<String>(nodes);
	//			if(temp.retainAll(nodeList) && temp.size() == nodeList.size()){
	//				return true;
	//			}
	//		}
	//		return false;
	//	}

	public Map<String,BufferedReader> loadFiles(String folder) throws IOException{
		Map<String,BufferedReader> bufferedReaders = new HashMap<String,BufferedReader>();
		List<File> filesInFolder = Files.walk(Paths.get(folder))
				.filter(Files::isRegularFile)
				.map(Path::toFile)
				.collect(Collectors.toList());

		for(File file: filesInFolder){
			try{
				if(file.getName().substring(0, 3).equals("200")){
					bufferedReaders.put(file.getName(),new BufferedReader(new FileReader(file)));
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return bufferedReaders;	
	}

}
