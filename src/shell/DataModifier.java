package shell;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DataModifier {
	
	static FileWriter traceWriter;
	public static void getTasks(String IN, String OUT) throws FileNotFoundException, IOException {
		FileWriter traceWriter = new FileWriter(OUT);
		try (BufferedReader reader = new BufferedReader(new FileReader(IN))) {

			String sCurrentLine;
			String [] splits;
			while ((sCurrentLine = reader.readLine()) != null) {
				// remove trailing spaces
				sCurrentLine = sCurrentLine.trim();
				
				splits = sCurrentLine.split("\\s");
				// get values from each line
				double one = Double.parseDouble(splits[0]); // task id
				//int two = Integer.parseInt(splits[1]); // execution time

				// put execution times tag and task group in map for dynamic
				
				double newTag1 =  one / 700 * 100;
				//double newTag2 =  two / 2;
				System.out.println( newTag1);
				traceWriter.write( newTag1 +  "\n");
			}
			// close reader
			reader.close();
		}
		traceWriter.close();
		// createInitDevices();
	}
	
	public static void getTasks1(String IN) throws FileNotFoundException, IOException {
		double sum = 0;
		double min = 100000;
		double max = 0;
		double num = 0;
		double ave = 0;
		int count = 0;
		
		//List<Integer> lists = new ArrayList<Integer>();
		try (BufferedReader reader = new BufferedReader(new FileReader(IN))) {

			String sCurrentLine;
			//String [] splits;
			while ((sCurrentLine = reader.readLine()) != null) {
				// remove trailing spaces
				sCurrentLine = sCurrentLine.trim();
				num = Double.parseDouble(sCurrentLine);
				sum += num;
				if (num > max) {
					max = num;
				}
				if (num < min) {
					min = num;
				}
				count++;
								
			}
			ave = sum/count;
			// close reader
			reader.close();
		}
		System.out.println(ave + " " + max + " " + min);
		traceWriter.write(ave + "\n");
		
		
		// createInitDevices();
	}
	
	public static void main (String [] args) throws FileNotFoundException, IOException {
		traceWriter = new FileWriter("histoplot.txt");
		/*getTasks("dRedF1n.txt","dRedF1.txt");
		getTasks("dRedF2n.txt","dRedF2.txt");
		getTasks("dRedF3n.txt","dRedF3.txt");
		getTasks("dRedF4n.txt","dRedF4.txt");
		getTasks("dRedF5n.txt","dRedF5.txt");
		getTasks("dRedF6n.txt","dRedF6.txt");*/
		
		getTasks1("dRedF1.txt");
		getTasks1("dRedF2.txt");
		getTasks1("dRedF3.txt");
		getTasks1("dRedF4.txt");
		getTasks1("dRedF5.txt");
		getTasks1("dRedF6.txt");
		
		traceWriter.close();
	}
	

}
