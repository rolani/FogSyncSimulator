package sim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Initiator extends Items{
	
	// get tasks from file into custom map
	public static void getTasks() throws FileNotFoundException, IOException {
		dynamicInputMap = new Util.SimpleMap(); // holds tasks
		mobilityPattern = new Util.StreamMap();
		try (BufferedReader reader = new BufferedReader(new FileReader(INPUTFILE))) {

			String sCurrentLine;
			String[] splits;
			while ((sCurrentLine = reader.readLine()) != null) {
				// remove trailing spaces
				sCurrentLine = sCurrentLine.trim();
				// split input on space
				splits = sCurrentLine.split("\\s");
				// get values from each line
				int tag = Integer.parseInt(splits[0]); // task id
				double time = Double.parseDouble(splits[1]); // execution time

				// put execution times tag and task group in map for dynamic
				if (tag == TAG_C_ASYNC || tag == TAG_C_SYNC || tag == TAG_C_LOCAL || tag == TAG_J_LOCAL) {
					dynamicInputMap.insert(tag, time);
				}
			}
			// close reader
			reader.close();
		}
		//createInitDevices();
	}

	// get machines available at time point to run async task
	public static void getAsyncMachines(int time_point) throws FileNotFoundException, IOException {
		availMachineMap.clear();
		for (int j = 0; j < mobilityPattern.getSize(); j++) {
			if (mobilityPattern.clusterList.getCluster(j).get(time_point).name != "NONE" ) {
				availMachineMap.putMachine(mobilityPattern.clusterList.getDevice(j));
			}
		}
	}

	// perform some operation on each time point specified by the id
	public static List<Cluster> getSyncMachines(int time_point)
			throws FileNotFoundException, IOException {
		List<Cluster> clusterMap = new ArrayList<Cluster>();
		
		for (int j = 0; j < mobilityPattern.getSize(); j++) {
			if (mobilityPattern.getClusterAtTimePoint(mobilityPattern.clusterList.getDevice(time_point), time_point).name != "NONE" ) {
				clusterMap.add(mobilityPattern.getClusterAtTimePoint(mobilityPattern.clusterList.getDevice(time_point), time_point));
			}
		}		
		return clusterMap;
	}
}
