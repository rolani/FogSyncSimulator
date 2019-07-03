// this class implememts DSSA with time redundancy


package sim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import map.Utility;

public class ChinaDynamicScheduler extends Items {

	public static void executeDynaSchedule() throws IOException {
		getDetailsFromMap();
		// loop through task graph in custom map
		for (int i = 0; i < dynamicInputMap.size(); i++) {
			System.out.println("Revised time is: " + dynamic_revised_available_time);

			// check if task is local task
			if (dynamicInputMap.getKey(i) == TAG_C_LOCAL || dynamicInputMap.getKey(i) == TAG_C_ASYNC) {
				List<Device> noneDevices = new ArrayList<Device>();
				getMachines(getCurrentTimePoint());
				noneDevices.clear();
				if (dynamicInputMap.getKey(i) == TAG_C_LOCAL) {
					time_point += dynamicInputMap.getValue(i);
					dynamic_revised_available_time += dynamicInputMap.getValue(i);
					noneDevices = returnListDifference(initMachines, assignedMachines);

					for (int j = 0; j < noneDevices.size(); j++) {
						noneDevices.get(j).increaseTime(dynamicInputMap.getValue(i));
					}
				} else if (dynamicInputMap.getKey(i) == TAG_C_ASYNC) {
					// update devices that are not part of current system
					noneDevices = returnListDifference(initMachines, assignedMachines);
					dynamic_revised_available_time += dynamicInputMap.getValue(i) + COMMUNICATION_TIME;
					time_point += dynamicInputMap.getValue(i) + COMMUNICATION_TIME;
					for (int j = 0; j < noneDevices.size(); j++) {
						noneDevices.get(j).increaseTime(dynamicInputMap.getValue(i) + COMMUNICATION_TIME);
					}

					JNode.setTime(getMax());
				}

				// availMachineMap.sortByTime();
				schdAsyncOrLocalC(i);

				System.out.println("Running task: " + (i + 1));

				// should be changed
				// basically for controlling devices currently not running a task

			}

			// check if task is C sync task
			else if (dynamicInputMap.getKey(i) == TAG_C_SYNC) {
				// sync task counter

				// update fog time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				System.out.println("Running task: " + (i + 1));
				System.out.println("Time point is: " + time_point);
				quorum_attempts = 0;

				// get unique list of clusters with associated devices
				// syncClusterMap = getSyncMachinesSuccess(getCurrentTimePoint());
				int control = 0;

				while (control == 0) {
					getMachines(getCurrentTimePoint());
					control = availMachineMap.getSize();
					time_point += 100;
				}

				// getMachines(getCurrentTimePoint());
				// for debugging
				System.out.println("Size of devices from getSyncMachines is: " + availMachineMap.getSize());
				sync_counter++;
				// run sync scheme
				syncAvailUpdate(i);

				// update all devices to current point
				// update JNode time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				// dynamic_revised_available_time = Math.max(getMax(), JNode.getTime());

				// clear ready device list for next iteration
				readyMachines.clear();
				quorumCount = 0;
				dynamic_revised_available_time = Math.max(getMax(), JNode.getTime());
				time_point = (Math.max(getMax(), JNode.getTime()));

				for (int m = 0; m < initMachines.size(); m++) {
					initMachines.get(m).setTime(Math.max(getMax(), JNode.getTime()));
				}

			}

			// j local task
			if (dynamicInputMap.getKey(i) == TAG_J_LOCAL) {
				schdLocalJ(i);
			}

			// control no of continuous runs
			if (i == dynamicInputMap.size() - 1 && counter < (RUNS - 1)) {

				i = -1;
				// System.out.println("Current run is: " + counter);
				System.out.println("Ending run: " + (counter + 1));
				// traceWriter.write("Ending run: " + (counter + 1) + "\n");
				System.out.println("Starting run: " + (counter + 2));
				// traceWriter.write("Starting run: " + (counter + 2) + "\n");
				counter++;

			}
		}
	}

	// availMachineMap contains devices currently present in the system
	// should be pre-processed before calling this method

	// schedule a C-async or C-local task
	public static void schdAsyncOrLocalC(int task_num) throws IOException {

		// System.out.println("Running task: " + (task_num + 1));
		// loop through available machines in the system
		for (int k = 0; k < availMachineMap.getSize(); k++) {
			int runtime = 0;
			// runtime = getGaussian(dynamicInputMap.getValue(task_num), MULTIPLIER);
			runtime = (int) (dynamicInputMap.getValue(task_num) * availMachineMap.getMachine(k).multiplier);
			// System.out.println("Runtime is: "+ runtime);
			if (dynamicInputMap.getKey(task_num) == TAG_C_ASYNC) {
				availMachineMap.getMachine(k).increaseTime(COMMUNICATION_TIME);
				runSchedule(availMachineMap.getMachine(k), dynamicInputMap.getKey(task_num), runtime);
			} else if (dynamicInputMap.getKey(task_num) == TAG_C_LOCAL) {
				runSchedule(availMachineMap.getMachine(k), dynamicInputMap.getKey(task_num), runtime);
			}
			// generate heterogeneous runtime based on normal distribution

			// schedule task

		}

	}

	// dynamic_revised_available_time should be updated to the time first machine
	// shows up plus allowed delay
	public static void syncAvailUpdate(int task_num) throws IOException {
		quorumCount = 0;

		readyMachines.clear();

		// message_count = cs.size();
		System.out.println("Running update task");
		quorum_quota = availMachineMap.getSize();
		// System.out.println("Number of occupied clusters are: " + message_count);

		// cs.forEach(clus -> System.out.println(clus.name));

		// available machines send update to J-machine
		for (int j = 0; j < availMachineMap.getSize(); j++) {
			Device dev = availMachineMap.getMachine(j);
			// if device is early or on time go on
			System.out.println("Compared device time " + dev.getTime() + " to " + dynamic_revised_available_time);
			if (dev.getTime() <= dynamic_revised_available_time) {
				quorumCount++;

				JNode.setTime(Math.max(dev.getTime(), JNode.getTime()));
				// run update sending task
				runSchedule(dev, TAG_UPDATE, STATUS_SENDING_TIME); // ******************
				// quorumCount++;
				readyMachines.add(dev);
			}
		}
		// JNode.increaseTime((message_count * STATUS_PROCESSING_TIME));
		System.out.println("Size of readyMachines: " + readyMachines.size());
		dynamic_revised_available_time += STATUS_SENDING_TIME;
		time_point += STATUS_SENDING_TIME;

		// update device time to latest finish
		for (int h = 0; h < readyMachines.size(); h++) {
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
			JNode.setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		// time_point += 1;
		checkQuorum(readyMachines, task_num);

	}

	// check if at least a device per cluster sent update message
	public static void checkQuorum(List<Device> readyDevices, int task_num) throws IOException {
		System.out.println("Checking for quorum");
		// run quorum for ready machines
		for (int g = 0; g < readyDevices.size(); g++) {
			// schedule quorum task
			runSchedule(readyDevices.get(g), TAG_QUORUM, QUORUM_TIME);
		}
		dynamic_revised_available_time += QUORUM_TIME;
		time_point += QUORUM_TIME;

		if (quorum_attempts < QUORUM_RETRIES) {
			if (isQuorumNew()) {
				scheduleSync(readyDevices, task_num);
			} else {
				System.out.println("Attempting quorum again");
				dynamic_revised_available_time += LAMBDA;
				retryQuorum(readyDevices, task_num);

			}
		} else {
			System.out.println("Sync task failed due to failed quorum, task number: " + (task_num + 1));
			syncFail++;
			readyMachines.clear();
		}
	}

	public static void retryQuorum(List<Device> readyDevices, int task_num) throws IOException {
		System.out.println("Retrying quorum no: " + (quorum_attempts + 1));
		quorum_attempts++; // increment attempts
		total_quorum_attempts++; // total quorum attempts
		syncAvailUpdate(task_num);
	}

	// if quorum successful go ahead and run sync task
	public static void scheduleSync(List<Device> runDevices, int task_num) throws IOException {

		System.out.println("Quorum successful!!!!!");

		for (int h = 0; h < runDevices.size(); h++) {
			// update device times
			runDevices.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		System.out.println("Running sync task with id: " + (task_num + 1));
		// schedule sync task
		for (int z = 0; z < runDevices.size(); z++) {
			runDevices.get(z).increaseTime(COMMUNICATION_TIME);
			// ready machines should be in sync at this point
			runSchedule(runDevices.get(z), TAG_C_SYNC,
					(int) (dynamicInputMap.getValue(task_num) * runDevices.get(z).multiplier));
		}
		// time_point = Math.max(getReadyMax(), JNode.getTime());
		System.out.println("Sync task successfully completed");
	}


	// check if sync task passed
	public static boolean checkSyncSuccess(List<Cluster> clusterMachineMap) throws FileNotFoundException, IOException {
		List<Cluster> syncMap = new ArrayList<Cluster>();
		// get list of clusters
		syncMap = getSyncMachinesSuccess(getCurrentTimePoint());
		List<Device> tempDevices = new ArrayList<Device>();
		for (int f = 0; f < syncMap.size(); f++) {
			tempDevices = syncMap.get(f).getDevices();
		}

		Set<Cluster> successCount = new HashSet<Cluster>();
		for (int i = 0; i < readyMachines.size(); i++) {
			for (int j = 0; j < tempDevices.size(); j++) {
				if (readyMachines.get(i) == tempDevices.get(j)) {
					// successCount.add(readyMachines.get(i).getCluster());
				}
			}
		}
		System.out.println("Successful count is: " + successCount.size() + " while expected count is: " + numClusters);
		if (successCount.size() == numClusters) {
			return true;
		} else {
			return false;
		}
	}

	// check if sync task passed
	public static boolean checkSyncSuccessNew(List<Cluster> clusterMachineMap)
			throws FileNotFoundException, IOException {
		List<Cluster> syncMap = new ArrayList<Cluster>();
		List<Device> tempDevices = new ArrayList<Device>();
		// get list of clusters at current time point
		System.out.println("Time point is: " + time_point);
		syncMap = getSyncMachines(getCurrentTimePoint());
		for (int i = 0; i < syncMap.size(); i++) {
			tempDevices = syncMap.get(i).getDevices();
			;
			System.out.println("The devices under: " + syncMap.get(i).name + " are: ");
			tempDevices.forEach(devi -> System.out.print(devi.name + ";"));
			// tempDevices.forEach(devi -> devi.setTime(getMinOrMax()));
			System.out.println("  ");
		}
		// List<Device> tempDevices = new ArrayList<Device>();
		if (syncMap.containsAll(clusterMachineMap)) {
			return true;
		} else {
			return false;
		}
	}

	public static void runDynamic() throws IOException {
		executeDynaSchedule();
		generateDynamicOutput();
	}

	public static void processResult() throws FileNotFoundException, IOException {
		// fWriter = new FileWriter("chinaDyna.txt", true);
		List<Integer> values = new ArrayList<>();
		// values.add(0);
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
		// int prev = 0;
		System.out.println(values.size());
		for (int q = 0; q < values.size(); q++) {

			if ((q + 1) < values.size()) {

				int prev = values.get(q);
				int curr = values.get(q + 1);
				int upload = (curr - prev) / 7;
				System.out.println(upload);

				try {
					fWriter.write(String.valueOf(upload) + "\n");
				} catch (Exception e) {
					System.out.println("Could not write to output: " + e);
				}
			}

		}

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
		System.out.println("No of tasks: " + (dynamicInputMap.size() * RUNS));
		System.out.println("Total time: " + getInitMax());
		System.out.println("Failed sync tasks: " + syncFail);
		System.out.println("Extra quorum attempts: " + total_quorum_attempts);
		System.out.println(
				"============================================================================================");
		System.out.println("End of dynamic scheduling report");
		System.out.println(
				"============================================================================================");

		// Double p = (double) syncFail / (double) sync_counter * 100;

		try {
			fWriter.write((getInitMax() / sync_counter) + "\n");
		} catch (Exception e) {
			System.out.println("Could not write to output: " + e);
		}

		traceWriter.write(sync_counter + " " + syncFail + " " + total_quorum_attempts + "\n");

		// fileWriter.write(df.format(getMax()) + " " + df.format(sync_delay) + " "
		// + (total_quorum_attempts + sync_task_counter) + " " + failedSyncTaskCounter +
		// "\n");
	}

	// miscellaneous methods
	// ----------------------------------------------------------------------------------------------//

	public static void main(String[] args) throws FileNotFoundException, IOException {

		try {
			INIT_DEVICES = Integer.parseInt(args[0]);
			INPUTFILE = args[1];
			OUTFILE1 = args[2];
			OUTFILE2 = args[3];
			LAMBDA = Integer.parseInt(args[4]);
			DEGREE = Double.parseDouble(args[5]);
		} catch (Exception e) {
			System.out.println("Invalid input(s)");
		}

		traceWriter = new FileWriter(OUTFILE2, true);
		fWriter = new FileWriter(OUTFILE1, true);
		initClusterList = new ArrayList<Cluster>();
		streamMap = new Util.StreamMap();
		deviceMap = new Utility.MyMap();
		dynamicInputMap = new Util.SimpleMap(); // holds tasks
		mobilityPattern = new Util.StreamMap();
		JNode = new Device("JNode", 1);
		unsortedMachineMap = new Util.MachineMap<Device>();
		availMachineMap = new Util.MachineMap<Device>();
		readyMachines = new ArrayList<Device>();
		initMachines = new ArrayList<Device>();
		assignedMachines = new ArrayList<Device>();
		unusedMachines = new ArrayList<Device>();
		neighbourList = new ArrayList<Device>();
		unusedDevices = new HashSet<Device>();

		grids = new ArrayList<ArrayList<Integer>>();

		System.out.println("Starting simulation....");
		runDynamic();
		// processResult();

		traceWriter.close();

		// processResult();

		fWriter.close();

		System.out.println("Simulation ended");
	}

}
