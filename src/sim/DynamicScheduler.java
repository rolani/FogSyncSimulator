package sim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DynamicScheduler extends Items {

	// shell parameters
	//public static String INPUTFILE, OUTFILE; // input file
	//public static double VARIANCE, STATUS_PROCESSING_TIME, QUORUM_PROCESSING_TIME; // variance and quorum delay


	public static void executeDynaSchedule() throws IOException {
		Initiator.getTasks();
		// loop through task graph in custom map
		for (int i = 0; i < dynamicInputMap.size(); i++) {
			val = (int) (JNode.getTime() / RESIDENCE_TIME);
			if (val > time_point) {
				time_point = val;
			}	
			// check if task is local task
			if (dynamicInputMap.getKey(i) == TAG_C_LOCAL) {
				Initiator.getAsyncMachines(time_point);
				availMachineMap.sortByTime();
				Scheduler.schdAsyncOrLocalC(i);
			}

			// check if task is asynchronous task
			else if (dynamicInputMap.getKey(i) == TAG_C_ASYNC) {
				// sort machine times
				Initiator.getAsyncMachines(time_point);
				availMachineMap.sortByTime();
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				Scheduler.schdAsyncOrLocalC(i);
				
			}

			// check if task is C sync task
			else if (dynamicInputMap.getKey(i) == TAG_C_SYNC) {
				sync_counter++;
				List<Cluster> syncMap = new ArrayList<Cluster>();
				// get list of clusters
				syncMap = Initiator.getSyncMachines(time_point);
				// run sync scheme
				Scheduler.syncAvailUpdate(syncMap, time_point);
				// update all devices to current point
				for (int k = 0; k < availMachineMap.getSize(); k++) {
					availMachineMap.getMachine(k).setTime(Math.max(getMax(), JNode.getTime()));
				}
				// update JNode time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));

				// clear ready device list for next iteration
				readyMachines.clear();
				quorumCount = 0;

			}

			// j local task
			if (dynamicInputMap.getKey(i) == TAG_J_LOCAL) {
				Scheduler.schdLocalJ(i);
			}

			// control no of continuous runs
			if (i == dynamicInputMap.size() - 1 && counter < (RUNS - 1)) {
				i = -1;
				// System.out.println("Current run is: " + counter);
				System.out.println("Ending run: " + (counter + 1));
				traceWriter.write("Ending run: " + (counter + 1) + "\n");
				System.out.println("Starting run: " + (counter + 2));
				traceWriter.write("Starting run: " + (counter + 2) + "\n");
				counter++;
			}
		}

	}

	// randomly disable some machines to simulate machine failure.
	public static void randomFail() throws IOException {

		// loop through
		for (int j = 0; j < availMachineMap.getSize(); j++) {
			if (Math.random() < DECISION) {
				// let failure occur for each machine at a probability of 0.1
				if (Math.random() < 0.05) {
					failed_machines_counter++;
					Device mach = availMachineMap.getMachine(j);
					System.out.println("Device failed: " + mach.name);
					traceWriter.write("Device failed: " + mach.name + "\n");
					// remove machine from available list
					availMachineMap.removeMachine(mach);
				}
			}
		}
	}

	// get the maximum of the
	public static double getMax() {
		double max = 0;
		// System.out.println("Total machines = " + availMachineMap.getSize());
		for (int i = 0; i < availMachineMap.getSize(); i++) {
			// System.out.println("machine time = " +
			// availMachineMap.getMachine(i).getTime());
			max = Math.max(max, availMachineMap.getMachine(i).getTime());
		}
		return max;
	}


	public static void runDynamic() throws IOException {
		executeDynaSchedule();
		generateDynamicOutput();
	}

	// generate final output and trace
	public static void generateDynamicOutput() throws IOException {

		System.out
				.println("==========================================================================================");
		System.out.println("Starting dynamic scheduling report...................... ");
		System.out
				.println("==========================================================================================");
		System.out.println("Totl no of sync tasks: " + sync_counter);
		System.out.println("No of runs: " + RUNS);
		System.out.println("No of tasks: " + dynamicInputMap.size());
		System.out.println("Failed sync tasks: " + syncFail);
		System.out.println(
				"============================================================================================");
		System.out.println("End of dynamic scheduling report");
		System.out.println(
				"============================================================================================");

		//fileWriter.write(df.format(getMax()) + " " + df.format(sync_delay) + " "
				//+ (total_quorum_attempts + sync_task_counter) + " " + failedSyncTaskCounter + "\n");
	}

	// main method for running simulation
	public static void main(String[] args) throws FileNotFoundException, IOException {

/*		try {
			LAMBDA = Double.parseDouble(args[2]);
			INPUTFILE = args[3];
			OUTFILE = args[4];
			STATUS_PROCESSING_TIME = Double.parseDouble(args[5]);
			VARIANCE = Double.parseDouble(args[6]);

		} catch (Exception e) {
			System.out.println("Seven arguments needed -- no of machines, quorum retries," + " variance, quorum delay, "
					+ "input file, output file and status processing time");
		}*/
		
		System.out.println("Starting simulation....");
		// create J-machine
		JNode = new Device("JNode", 1);
		unsortedMachineMap = new Util.MachineMap<Device>();
		availMachineMap = new Util.MachineMap<Device>();
		readyMachines = new ArrayList<Device>();
		initMachines = new ArrayList<Device>();
		assignedMachines = new ArrayList<Device>();
		unusedMachines = new ArrayList<Device>();
		neighbourList = new ArrayList<Device>();
		unusedDevices = new HashSet<Device>();

		// writers
		//fileWriter = new FileWriter(OUTFILE, true);

		runDynamic();

		System.out.println("Simulation ended");
		//traceWriter.write("Simulation ended" + "\n");

		// close file writers
		//fileWriter.close();
		//traceWriter.close();
	}

}
