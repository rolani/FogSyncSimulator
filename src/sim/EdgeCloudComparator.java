package sim;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import map.Utility;

public class EdgeCloudComparator extends Items {

	public static void executeDynaSchedule() throws IOException {
		getDetailsFromMap();
		// loop through task graph in custom map
		for (int i = 0; i < dynamicInputMap.size(); i++) {
			// System.out.println("Revised time is: " + dynamic_revised_available_time);
			// sync task counter

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

			else if (dynamicInputMap.getKey(i) == TAG_C_SYNC) {
				// update fog time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				System.out.println("Running task: " + (i + 1));
				System.out.println("Time point is: " + time_point);
				quorum_attempts = 0;

				// get unique list of clusters with associated devices
				// syncClusterMap = getSyncMachinesSuccess(getCurrentTimePoint());
				int control = 0;
				getMachines(getCurrentTimePoint());
				control = availMachineMap.getSize();

				while (control == 0) {
					getMachines(getCurrentTimePoint());
					control = availMachineMap.getSize();
					time_point += 2;
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

	public static void schdAsyncOrLocalC(int task_num) throws IOException {

		// System.out.println("Running task: " + (task_num + 1));
		// loop through available machines in the system
		for (int k = 0; k < availMachineMap.getSize(); k++) {
			double runtime = 0;
			// runtime = getGaussian(dynamicInputMap.getValue(task_num), MULTIPLIER);
			runtime = dynamicInputMap.getValue(task_num);
			// System.out.println("Runtime is: "+ runtime);
			if (dynamicInputMap.getKey(task_num) == TAG_C_ASYNC) {
				availMachineMap.getMachine(k).increaseTime(COMMUNICATION_TIME);
				runSchedule(availMachineMap.getMachine(k), dynamicInputMap.getKey(task_num), getGauss(runtime));
			} else if (dynamicInputMap.getKey(task_num) == TAG_C_LOCAL) {
				runSchedule(availMachineMap.getMachine(k), dynamicInputMap.getKey(task_num), getGauss(runtime));
			}

		}

	}

	public static List<Cluster> getMachines(int time_point) throws FileNotFoundException, IOException {
		availMachineMap.clear();
		assignedMachines.clear();
		List<Cluster> clusterSet = new ArrayList<Cluster>();
		for (int j = 0; j < initMachines.size(); j++) {
			Device d = initMachines.get(j);
			d.multiplier = getMultiplier();
			// System.out.println(d.multiplier);
			Cluster c = d.getClusterAtTimePoint(time_point);
			if (c != NONE) {
				availMachineMap.putMachine(d);
				assignedMachines.add(initMachines.get(j));
				if (clusterSet.contains(c) == false)
					clusterSet.add(c);
			}
		}
		return clusterSet;
	}

	// extract devices (index of grids) and grid data extractor
	public static void getDetailsFromMap() throws FileNotFoundException, IOException {
		// get taxi positions in list
		grids = DataExtractor.processEdgeCloudTraceData();

		System.out.println("Done extracting map data");

		// create dummy clusters
		createClusters();
		// create initial devices from extracted data
		createInitDevices();
		// get tasks from task file
		getTasks();
	}

	// dynamic_revised_available_time should be updated to the time first machine
	// shows up plus allowed delay
	public static void syncAvailUpdate(int task_num) throws IOException {
		quorumCount = 0;

		readyMachines.clear();

		// message_count = cs.size();
		System.out.println("Running update task");
		quorum_quota = availMachineMap.getSize();
		System.out.println(quorum_quota);
		// System.out.println("Number of occupied clusters are: " + message_count);

		// cs.forEach(clus -> System.out.println(clus.name));

		availMachineMap.sortByTime();
		int border_num = (int) (quorum_quota * 0.7);
		System.out.println(border_num);

		// availMachineMap.getMachine(availMachineMap.getSize()-1)

		Device border = availMachineMap.getMachine(border_num);
		// Device border = availMachineMap.getMachine(quorum_quota - 1);

		// available machines send update to J-machine
		for (int j = 0; j < availMachineMap.getSize(); j++) {
			Device dev = availMachineMap.getMachine(j);
			// if device is early or on time go on
			System.out.println("Compared device time " + dev.getTime() + " to " + dynamic_revised_available_time);
			if (dev.getTime() <= border.getTime()) {
				quorumCount++;

				JNode.setTime(Math.max(dev.getTime(), JNode.getTime()));
				// run update sending task
				runSchedule(dev, TAG_UPDATE, COMM_TIME); // ******************
				// quorumCount++;
				readyMachines.add(dev);
			}
		}
		// JNode.increaseTime((message_count * STATUS_PROCESSING_TIME));
		System.out.println("Size of readyMachines: " + readyMachines.size());
		// dynamic_revised_available_time += STATUS_SENDING_TIME;
		time_point += COMM_TIME;

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
			runSchedule(readyDevices.get(g), TAG_QUORUM, COMM_TIME);
		}
		// dynamic_revised_available_time += QUORUM_TIME;
		time_point += COMM_TIME;

		for (int z = 0; z < readyDevices.size(); z++) {
			readyDevices.get(z).increaseTime(COMM_TIME);
		}

		scheduleSync(readyDevices, task_num);

		/*
		 * if (quorum_attempts < QUORUM_RETRIES) { if (isQuorumNew()) {
		 * scheduleSync(readyDevices, task_num); } else {
		 * System.out.println("Attempting quorum again"); dynamic_revised_available_time
		 * += LAMBDA; retryQuorum(readyDevices, task_num);
		 * 
		 * } } else {
		 * System.out.println("Sync task failed due to failed quorum, task number: " +
		 * (task_num + 1)); syncFail++; readyMachines.clear(); }
		 */
	}

	/*
	 * public static void retryQuorum(List<Device> readyDevices, int task_num)
	 * throws IOException { System.out.println("Retrying quorum no: " +
	 * (quorum_attempts + 1)); quorum_attempts++; // increment attempts
	 * total_quorum_attempts++; // total quorum attempts syncAvailUpdate(task_num);
	 * }
	 */

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
			runDevices.get(z).increaseTime(COMM_TIME);
			// ready machines should be in sync at this point
			runSchedule(runDevices.get(z), TAG_C_SYNC, getGauss(dynamicInputMap.getValue(task_num)));
		}

		for (int z = 0; z < runDevices.size(); z++) {
			runDevices.get(z).increaseTime(COMM_TIME);
		}
		// time_point = Math.max(getReadyMax(), JNode.getTime());
		System.out.println("Sync task successfully completed");
	}

	public static void runSchedule(Device m, int task_type, double runtime) throws IOException {

		String taskType = null;
		switch (task_type) {
		case 1:
			taskType = "C-async task";
			break;
		case 2:
			taskType = "C-sync task";
			break;
		case 3:
			taskType = "C-local task";
			break;
		case 4:
			taskType = "J-local";
			break;
		case 5:
			taskType = "Update task";
			break;
		case 6:
			taskType = "Quorum task";
			break;
		}

		// update C-machine time and save to file
		if (task_type == 1 || task_type == 2 || task_type == 3) {
			updateTimeAndOutput(taskType, m, runtime);
		}

		else if (task_type == 4) {
			System.out.println("JNode is running task :" + task_type + " at time: " + JNode.getTime() + " and ended "
					+ (JNode.time + runtime));

			JNode.increaseTime(runtime);
		}
		// update J-machine time and save to file

		// ****************** change here
		else if (task_type == 5) {
			m.increaseTime(runtime);
			// JNode.setTime(m.getTime());
			// if (message_count < INIT_DEVICES) {
			JNode.increaseTime(STATUS_PROCESSING_TIME);
			// message_count++;
			// }
			// else if (task_type == 6)
			// JNode.increaseTime(QUORUM_PROCESSING_TIME);
		} else {
			m.increaseTime(runtime);
			// JNode.setTime(m.getTime());
			// if (task_type == 5)
			// if (message_count < INIT_DEVICES) {
			JNode.increaseTime(QUORUM_PROCESSING_TIME);
			// message_count++;
			// }
			// else if (task_type == 6)
			// JNode.increaseTime(QUORUM_PROCESSING_TIME);
		}
	}

	public static void updateTimeAndOutput(String taskType, Device m, double k) throws IOException {
		// System.out.println("Task type is: " + taskType);
		// System.out.println("Device name is: " + m.name);
		// System.out.println("Device time is: " + m.time);
		// System.out.println("Device time ended: " + (m.time + k));
		System.out.println(taskType + " was scheduled on: " + m.name + " at: " + m.time + " for: " + k + " and ended: "
				+ (m.time + k));
		// System.out.println(taskType + " was successfully scheduled" );
		m.increaseTime(k);
	}

	public static int getCurrentTimePoint() {

		if (time_point >= 3000) {
			time_point = 0;
			return (int) time_point;
		} else
			return (int) (time_point);
	}

	public static double getGauss(double aMean) {
		double time = (aMean + RANDOM.nextGaussian() * VAR);
		if (time <= 0)
			getGaussian(aMean);
		return time;
	}

	public static double getInMax() {
		double max = 0;
		// System.out.println("Total machines = " + availMachineMap.getSize());
		for (int i = 0; i < initMachines.size(); i++) {
			// System.out.println("machine time = " +
			// availMachineMap.getMachine(i).getTime());
			max = Math.max(max, initMachines.get(i).getTime());
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
		System.out.println("Starting edge-cloud scheduling report...................... ");
		System.out
				.println("==========================================================================================");
		System.out.println("Totl no of sync tasks: " + sync_counter);
		System.out.println("No of runs: " + RUNS);
		System.out.println("Communication time is: " + COMM_TIME);
		System.out.println("No of tasks: " + (dynamicInputMap.size() * RUNS));
		System.out.println("Total time: " + getInMax());
		System.out.println("Failed sync tasks: " + syncFail);
		System.out.println("Extra quorum attempts: " + total_quorum_attempts);
		System.out.println(
				"============================================================================================");
		System.out.println("End of edge-cloud scheduling report");
		System.out.println(
				"============================================================================================");

		Double p = 10 * (double) (dynamicInputMap.size() * RUNS) / getInMax();

		try {
			// frequency
			fWriter.write(p + "\n");
		} catch (Exception e) {
			System.out.println("Could not write to output: " + e);
		}

		// traceWriter.write(sync_counter + " " + syncFail + " " + total_quorum_attempts
		// + "\n");

		// fileWriter.write(df.format(getMax()) + " " + df.format(sync_delay) + " "
		// + (total_quorum_attempts + sync_task_counter) + " " + failedSyncTaskCounter +
		// "\n");
	}

	// miscellaneous methods
	// ----------------------------------------------------------------------------------------------//

	public static void main(String[] args) throws FileNotFoundException, IOException {

		INIT_DEVICES = 50;
		// LAMBDA = 50;
		// DEGREE = 0.7;
		try {

			INPUTFILE = args[0];
			OUTFILE1 = args[1];
			// OUTFILE2 = args[2];
			COMM_TIME = Double.parseDouble(args[2]);

		} catch (Exception e) {
			System.out.println("Invalid input(s)");
		}

		// traceWriter = new FileWriter(OUTFILE2, true);
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

		// traceWriter.close();

		// processResult();

		fWriter.close();

		System.out.println("Simulation ended");
	}

}
