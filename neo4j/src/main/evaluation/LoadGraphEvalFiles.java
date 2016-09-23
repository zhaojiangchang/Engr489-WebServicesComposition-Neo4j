package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JFileChooser;

public class LoadGraphEvalFiles {
	
	private File[] files = null;
	Map<Integer,Map<List<String>, List<Double>>> graphEvalResults = new HashMap<Integer,Map<List<String>, List<Double>>>();
	String datasetDirName = "";
	public LoadGraphEvalFiles(){
		run();
	}
	@SuppressWarnings("unused")
	public static void main( String[] args ) {
		LoadGraphEvalFiles LoadGraphEvalFiles = new LoadGraphEvalFiles();
	}

	public void run(){
		loadFiles();
		try {
			readFiles();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Map<Integer,Map<List<String>, List<Double>>> getEvalResults(){
		return graphEvalResults;
	}
	private void readFiles() throws IOException {
        List<BufferedReader>bufferedReaders = getBufferedReaders();
        PrintWriter datasetWriter = null;
        File file = new File("evaluationGraphEvalResults/");
        if(!file.exists()){
        	file.mkdirs();
        }

        File dataset = new File("evaluationGraphEvalResults/"+datasetDirName+".stat");
        datasetWriter = new PrintWriter(dataset);
		 for(int j = 0; j<bufferedReaders.size(); j++){
			 double totalTimeCostForEachBestResult = 0.0;
			 int i = 0;
			 String textQos = "";
			 while(i<50){
				 String candidateInfo = bufferedReaders.get(j).readLine();
				 String[] candidateInfoAsArray = candidateInfo.split(" ");
				 totalTimeCostForEachBestResult += Double.parseDouble(candidateInfoAsArray[1]);
				 totalTimeCostForEachBestResult += Double.parseDouble(candidateInfoAsArray[2]);

	             i++;
			 }
			 textQos = bufferedReaders.get(j).readLine();
			 
             System.out.println(j+"  "+textQos);
			 List<Double> qos = new ArrayList<Double>();
             String[] arrQos = textQos.split(" ");
             if(i==50){
				 totalTimeCostForEachBestResult += Double.parseDouble(arrQos[1]);
				 totalTimeCostForEachBestResult += Double.parseDouble(arrQos[2]);
			 }
             int index = 0;
             datasetWriter.append(totalTimeCostForEachBestResult+"\n");
             for (int l = arrQos.length-4; l < arrQos.length; l++) {
            	    if(l!=arrQos.length-1){
            	    	datasetWriter.append(arrQos[l]+"   ");
            	    	qos.add(Double.parseDouble(arrQos[l]));

            	    }
            	    	
            	    else{
            	    	datasetWriter.append(arrQos[l]);
            	    	qos.add(Double.parseDouble(arrQos[l]));
            	    }


            	    index++;
            	}

             datasetWriter.append("\n");             

             String textNodes = bufferedReaders.get(j).readLine();
             textNodes = textNodes.substring(11, textNodes.length()-1);
             textNodes = textNodes.replace("->", " ");
             textNodes = textNodes.replace(";", "");
             System.out.println(textNodes);
             Set<String>stringNodes = new HashSet<String>(Arrays.asList(textNodes.split("\\s+")));
             index = 0;
             for (String s: stringNodes) {
         	    if(index!=stringNodes.size()-1)
         	    	datasetWriter.append(s +"   ");
         	    else
         	    	datasetWriter.append(s);


         	    index++;
         	}
             datasetWriter.append("\n");      
             List<String>nodes = new ArrayList<String>(stringNodes);
//             if(!containsKey(nodes)){
             Map<List<String>, List<Double>> result = new HashMap<List<String>, List<Double>>();
             graphEvalResults.put(j, result);
//             }
         }
		   for(int rd = 0; rd<bufferedReaders.size();rd++){
               bufferedReaders.get(rd).close();
           }
		   datasetWriter.close();
		
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

	public void loadFiles(){
		JFileChooser chooser2 = new JFileChooser("Files Chooser");
		chooser2.setMultiSelectionEnabled(true);
		int f2 = chooser2.showOpenDialog(null);
		if (JFileChooser.APPROVE_OPTION == f2) {
			files = chooser2.getSelectedFiles();
			String[]path = files[0].getParent().split("/");
			datasetDirName = path[path.length-1];
		} else {
			System.exit(0);
		}
	}
	private List<BufferedReader> getBufferedReaders(){
		List<BufferedReader> bufferedReaders = new ArrayList<BufferedReader>();
		for(File file: files){
			try{
				bufferedReaders.add(new BufferedReader(new FileReader(file)));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return bufferedReaders;
	}
}
