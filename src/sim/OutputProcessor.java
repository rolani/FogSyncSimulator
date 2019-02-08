package sim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OutputProcessor {
	public static FileWriter fWriter1, fWriter2;
	static final Random RANDOM = new Random();
	
	public static void processResult() throws FileNotFoundException, IOException {
		//fWriter = new FileWriter("chinaDyna.txt", true);
		List<Integer> values = new ArrayList<>();
		values.add(0);
		try (BufferedReader br = new BufferedReader(new FileReader("chinaDynaOut.txt"))) {

			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				// remove trailing spaces
				sCurrentLine = sCurrentLine.trim();

				// get values from each line
				int prev = Integer.parseInt(sCurrentLine); // task id
				values.add(prev);
			}
			// close reader
			br.close();
			System.out.println("Done");
		}
		//int prev = 0;
		System.out.println(values.size());
		for (int q = 0; q < values.size(); q++) {
			
			if ((q + 1) < values.size()) {

				int prev = values.get(q);
				int curr = values.get(q + 1);
				int upload = curr - prev;
				System.out.println(upload);
				

				try {
					//fWriter.write(String.valueOf(upload) + "\n");
				} catch (Exception e) {
					System.out.println("Could not write to output: " + e);
				}
			}

		}

	}
	
	public static void randomizeResult() throws FileNotFoundException, IOException {
		fWriter1 = new FileWriter("chinaDynaN.txt", true);
		fWriter2 = new FileWriter("chinaStaticN.txt", true);
		
		try (BufferedReader br1 = new BufferedReader(new FileReader("chinaDyna.txt"))) {

			String sCurrentLine;
			while ((sCurrentLine = br1.readLine()) != null) {
				// remove trailing spaces
				sCurrentLine = sCurrentLine.trim();

				// get values from each line
				int prev = getGaussian(Integer.parseInt(sCurrentLine)); // task id
				//prev = getGaussian(prev);
				
				fWriter1.write(prev + "\n");
				
			}
			// close reader
			br1.close();
			System.out.println("Done1");
		}
		
		try (BufferedReader br2 = new BufferedReader(new FileReader("chinaStatic.txt"))) {

			String sCurrentLine;
			while ((sCurrentLine = br2.readLine()) != null) {
				// remove trailing spaces
				sCurrentLine = sCurrentLine.trim();

				// get values from each line
				int prev = getGaussian(Integer.parseInt(sCurrentLine)); // task id
				
				fWriter2.write(prev + "\n");
			}
			// close reader
			br2.close();
			System.out.println("Done2");
		}


	}
	
	public static int getGaussian(double aMean) {
		int time = (int) (aMean + RANDOM.nextGaussian() * 100);
		if (time <= 0)
			getGaussian(aMean);
		return time;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		
		//processResult();
		randomizeResult();
		
		fWriter1.close();
		fWriter2.close();
	}

		

}
