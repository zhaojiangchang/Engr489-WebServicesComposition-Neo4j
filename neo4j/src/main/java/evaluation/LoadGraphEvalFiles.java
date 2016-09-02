package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;

import task.OuchException;

public class LoadGraphEvalFiles {
	
	private File[] files = null;
	public static void main( String[] args ) {
		LoadGraphEvalFiles LoadGraphEvalFiles = new LoadGraphEvalFiles();
		LoadGraphEvalFiles.run();
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
	private void readFiles() throws IOException {
        List<BufferedReader>bufferedReaders = getBufferedReaders();
        PrintWriter datasetWriter = null;
        File dataset = new File("evaluationGraphEvalResults/resultDataset.stat");
        datasetWriter = new PrintWriter(dataset);
		 for(int j = 0; j<bufferedReaders.size(); j++){
			 int i = 0;
			 String text = "";
			 while(i<50){
	             text = bufferedReaders.get(j).readLine();
	             i++;
			 }
             text = bufferedReaders.get(j).readLine();
             String [] arr = text.split(" ");
             String[] outputArray = new String[4];
             int index = 0;
             for (int l = arr.length-4; l < arr.length; l++) {
            	    if(l!=arr.length-1)
            	    	datasetWriter.append(arr[l]+",");
            	    else
            	    	datasetWriter.append(arr[l]);


            	    index++;
            	}

             datasetWriter.append("\n");             

             text = bufferedReaders.get(j).readLine();
             text = text.substring(11, text.length()-1);
             text = text.replace("->", " ");
             text = text.replace(";", "");
             Set<String>string = new HashSet<String>(Arrays.asList(text.split("\\s+")));
             System.out.println(string.size());
             index = 0;
             for (String s: string) {
         	    if(index!=string.size()-1)
         	    	datasetWriter.append(s +",");
         	    else
         	    	datasetWriter.append(s);


         	    index++;
         	}
             System.out.println(text);
             datasetWriter.append("\n");      
          
         }
		   for(int rd = 0; rd<bufferedReaders.size();rd++){
               bufferedReaders.get(rd).close();
           }
		   datasetWriter.close();
		
	}
	public void loadFiles(){
		JFileChooser chooser2 = new JFileChooser("Files Chooser");
		chooser2.setMultiSelectionEnabled(true);
		int f2 = chooser2.showOpenDialog(null);
		if (JFileChooser.APPROVE_OPTION == f2) {
			files = chooser2.getSelectedFiles();
			System.out.println(files.length);

		} else {
			System.exit(0);
		}
	}
	private List<BufferedReader> getBufferedReaders(){
		List<BufferedReader> bufferedReaders = new ArrayList<BufferedReader>();
		for(File file: files){
			try{
				System.out.println(file.getName());
				bufferedReaders.add(new BufferedReader(new FileReader(file)));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return bufferedReaders;
	}
}
