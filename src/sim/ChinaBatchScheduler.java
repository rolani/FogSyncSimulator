package sim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import china.DataExtractor;
import map.Utility;

public class ChinaBatchScheduler extends Items {
	static List<Cluster> initClusterList;
	static Util.StreamMap streamMap;
	static Utility.MyMap deviceMap;
	static List<ArrayList<Integer>> grids;
	static int sum;
	static FileWriter traceWriter, fWriter;
	static String OUTFILE;

	public static int failedSyncTaskCounter = 0; // counter for failed sync
	// tasks
	public static int total_execution_time = 0;
	public static int quorum_quota = 0;
	public static int prevMax = 0;
	public static int newMax = 0;

	// extract devices (index of grids) and grid data extractor
	public static void getDetailsFromMap() throws FileNotFoundException, IOException {
		// get taxi positions in list
		grids = DataExtractor.processTraceData();

		System.out.println("Done extracting map data");

		// create dummy clusters
		createClusters();
		// create initial devices from extracted data
		createInitDevices();
		// get tasks from task file
		getTasks();
	}

	public static void createClusters() {
		System.out.println("Initializing clusters");
		for (int i = 1; i < 500; i++) {
			String cName = "C" + i;
			Cluster c = new Cluster(cName, i);
			// contains list of all 400 grids
			initClusterList.add(c);
		}
	}

	// create devices from ids and assign grids for all time points
	public static void createInitDevices() throws FileNotFoundException, IOException {
		System.out.println("Initializing machines");
		for (int i = 0; i < grids.size(); i++) {
			String name = "Dev" + (i + 1);
			Device d = new Device(name, i + 1); // device name starting from 1
			initMachines.add(d); // add all devices to list

			List<Integer> temp = grids.get(i);

			for (int j = 0; j < temp.size(); j++) {
				if (temp.get(j) == 0) {
					d.addCluster(NONE);
				} else {
					d.addCluster(initClusterList.get(temp.get(j)));
				}
			}
		}

		// unusedDevices.forEach(d -> d.addCluster(NONE));
		// createClusters();
	}

	// get tasks from file into custom map
	public static void getTasks() throws FileNotFoundException, IOException {
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
				int time = Integer.parseInt(splits[1]); // execution time

				// put execution times tag and task group in map for dynamic
				staticInputMap.insert(tag, time);
			}
			// close reader
			reader.close();
		}
		// createInitDevices();
	}

	public static void executeStaticSchedule() throws IOException {
		getDetailsFromMap();
		// loop through task graph in custom map
		for (int i = 0; i < staticInputMap.size(); i++) {
			// check if task is local task
			if (staticInputMap.getKey(i) == TAG_C_LOCAL || staticInputMap.getKey(i) == TAG_C_ASYNC) {
				getMachinesN(getCurrentTimePoint());
				// availMachineMap.sortByTime();
				schdAsyncOrLocalC(i);
				static_available_time += staticInputMap.getValue(i) + 20;
				System.out.println("Running task: " + (i + 1));
				// should be changed
				// basically for controlling devices currently not running a task
				// * for (int u = 0; u < noneDevices.size(); u++) {
				if (staticInputMap.getKey(i) == TAG_C_LOCAL) {
					time_point += staticInputMap.getValue(i);
				} else if (staticInputMap.getKey(i) == TAG_C_ASYNC) {
					// update devices that are not part of current system
					time_point += staticInputMap.getValue(i) + COMMUNICATION_TIME;
				}
			}

			// check if task is C sync task
			else if (staticInputMap.getKey(i) == TAG_C_SYNC) {
				// sync task counter
				sync_counter++;
				// update fog time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				System.out.println("Running task: " + (i + 1));
				System.out.println("Time point is: " + time_point);

				// get unique list of clusters with associated devices
				// syncClusterMap = getSyncMachinesSuccess(getCurrentTimePoint());

				getMachinesN(getCurrentTimePoint());
				// for debugging
				System.out.println("Size of devices from getSyncMachines is: " + availMachineMap.getSize());

				// run sync scheme
				checkQuorum(readyMachines, i);

				// update all devices to current point
				// update JNode time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				// static_available_time = Math.max(getMax(), JNode.getTime());

				// clear ready device list for next iteration
				readyMachines.clear();
				quorumCount = 0;
				//static_available_time = Math.max(getMax(), JNode.getTime());
				static_available_time += staticInputMap.getValue(i) + 20;
				time_point = (Math.max(getMax(), JNode.getTime()));

				for (int m = 0; m < initMachines.size(); m++) {
					initMachines.get(m).setTime(Math.max(getMax(), JNode.getTime()));
				}
			}

			// j local task
			else if (staticInputMap.getKey(i) == TAG_UPDATE) {
				syncAvailUpdate(i);
			}

			// j local task
			if (staticInputMap.getKey(i) == TAG_J_LOCAL) {
				schdLocalJ(i);
			}

			// control no of continuous runs
			if (i == staticInputMap.size() - 1 && counter < (RUNS - 1)) {

				i = -1;
				// System.out.println("Current run is: " + counter);
				System.out.println("Ending run: " + (counter + 1));
				// traceWriter.write("Ending run: " + (counter + 1) + "\n");
				System.out.println("Starting run: " + (counter + 2));
				// traceWriter.write("Starting run: " + (counter + 2) + "\n");
				counter++;

				try {
					traceWriter.write(getInitMax() + "\n");
				} catch (Exception e) {
					System.out.println("Could not write to output: " + e);
				}

				for (int j = 0; j < initMachines.size(); j++) {
					// initMachines.get(j).setTime(0);
				}

			}
		}
	}

	// get machines available at time point to run async task
	public static void getMachinesN(int time_point) throws FileNotFoundException, IOException {
		availMachineMap.clear();
		for (int j = 0; j < initMachines.size(); j++) {
			Cluster c = initMachines.get(j).getClusterAtTimePoint(time_point);
			if (c != NONE) {
				availMachineMap.putMachine(initMachines.get(j));
			}
		}
	}

	// returns unique grids with devices in them
	public static List<Cluster> getSyncMachines(int time_point) throws FileNotFoundException, IOException {

		// Set<Cluster> clusterSet = new HashSet<Cluster>();
		List<Cluster> clusterSet = new ArrayList<Cluster>();
		// loop through list of clusters
		for (int i = 0; i < initClusterList.size(); i++) {
			// empty devices in a cluster if not already empty
			initClusterList.get(i).clearDeviceList();
		}

		for (int j = 0; j < initMachines.size(); j++) {

			Device device = initMachines.get(j);
			// get cluster for device at the current time point
			Cluster c = initMachines.get(j).getClusterAtTimePoint(time_point);

			if (clusterSet.contains(c) == false && c != NONE) {
				// add only clusters with at least the minimum number of devices
				if (c.numOfDevices() >= CLUSTER_MIN_SIZE) {
					// add unique cluster to list
					clusterSet.add(c);
				}
				// System.out.println(c.name + "added");
			}
			// add device to cluster device list
			c.addDevice(device);
		}

		// for debugging
		System.out.println("Set size is: " + clusterSet.size());

		return clusterSet;
	}

	// returns grids with devices in them
	public static List<Cluster> getSyncMachinesSuccess(int time_point) throws FileNotFoundException, IOException {
		// Set<Cluster> clusterSet = new HashSet<Cluster>();
		List<Cluster> clusterSet = new ArrayList<Cluster>();
		for (int i = 0; i < initClusterList.size(); i++) {
			initClusterList.get(i).clearDeviceList();
		}

		for (int j = 0; j < initMachines.size(); j++) {

			Cluster c = initMachines.get(j).getClusterAtTimePoint(time_point);
			Device device = initMachines.get(j);
			// if (clusterSet.contains(c) == false && c != NONE) {
			// if (c.numOfDevices() >= CLUSTER_MIN_SIZE) {
			clusterSet.add(c);
			// }
			// System.out.println(c.name + "added");
			// }
			c.addDevice(device);
		}
		System.out.println("Set size is: " + clusterSet.size());
		return clusterSet;
	}

	public static List<Device> getNoneList(int time_point) throws FileNotFoundException, IOException {
		List<Device> noneList = new ArrayList<Device>();

		for (int j = 0; j < initMachines.size(); j++) {
			Cluster c = initMachines.get(j).getClusterAtTimePoint(time_point);
			Device device = initMachines.get(j);
			if (c == NONE) {
				noneList.add(device);
			}
		}
		return noneList;
	}

	public static void schdLocalJ(int task_num) throws IOException {

		// System.out.println("Running task: " + (task_num + 1));
		// schedule task
		runSchedule(JNode, staticInputMap.getKey(task_num), staticInputMap.getValue(task_num));
	}

	// availMachineMap contains devices currently present in the system
	// should be pre-processed before calling this method

	// schedule a C-async or C-local task
	public static void schdAsyncOrLocalC(int task_num) throws IOException {

		// System.out.println("Running task: " + (task_num + 1));
		// loop through available machines in the system
		for (int k = 0; k < availMachineMap.getSize(); k++) {
			int runtime = 0;
			if (staticInputMap.getKey(task_num) == TAG_C_ASYNC) {
				availMachineMap.getMachine(k).increaseTime(COMMUNICATION_TIME);
			}
			// generate heterogeneous runtime based on normal distribution
			runtime = getGaussian(staticInputMap.getValue(task_num), (int)MULTIPLIER);

			// schedule task
			runSchedule(availMachineMap.getMachine(k), staticInputMap.getKey(task_num), runtime);
		}

	}

	// static_available_time should be updated to the time first machine
	// shows up plus allowed delay
	public static void syncAvailUpdate(int task_num) throws IOException {
		quorumCount = 0;

		readyMachines.clear();

		System.out.println("Running update task");
		quorum_quota = availMachineMap.getSize();
		// available machines send update to J-machine
		for (int j = 0; j < availMachineMap.getSize(); j++) {
			Device dev = availMachineMap.getMachine(j);
			// if device is early or on time go on
			if (dev.getTime() <= static_available_time) {
				quorumCount++;

				JNode.setTime(Math.max(dev.getTime(), JNode.getTime()));
				// run update sending task
				runSchedule(dev, TAG_UPDATE, STATUS_SENDING_TIME); // ******************
				// quorumCount++;
				readyMachines.add(dev);
				
				
			}
		}
		System.out.println("Size of readyMachines: " + readyMachines.size());
		//static_available_time += STATUS_SENDING_TIME;
		static_available_time += (readyMachines.size() * STATUS_PROCESSING_TIME);

		// update device time to latest finish
		for (int h = 0; h < readyMachines.size(); h++) {
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
			JNode.setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		// time_point += 1;

	}

	// check if at least a device per cluster sent update message
	public static void checkQuorum(List<Device> readyDevices, int task_num) throws IOException {
		System.out.println("Checking for quorum");
		// run quorum for ready machines
		for (int g = 0; g < readyDevices.size(); g++) {
			// schedule quorum task
			runSchedule(readyDevices.get(g), TAG_QUORUM, QUORUM_TIME);
		}
		static_available_time += QUORUM_TIME;

		if (quorum_attempts < QUORUM_RETRIES) {
			if (isQuorumNew()) {
				scheduleSync(readyDevices, task_num);
			} else {
				System.out.println("Attempting quorum again");
				static_available_time += LAMBDA;
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
			runSchedule(runDevices.get(z), TAG_C_SYNC, getGaussian(staticInputMap.getValue(task_num), (int)MULTIPLIER));
		}
		time_point = Math.max(getReadyMax(), JNode.getTime());
		System.out.println("Sync task successfully completed");
	}

	// check for quorum by comparing size of machines with update count
	public static boolean isQuorum() {
		if (quorumCount == numClusters)
			return true;
		else {
			System.out.println("Expected: " + numClusters + " got: " + quorumCount);
			return false;
		}
	}

	// check for quorum by comparing size of machines with update count
	public static boolean isQuorumNew() {
		if (quorumCount >= (int) (quorum_quota * 0.7)) {
			System.out.println("Compared: " + (int) (quorum_quota * 0.7) + " to " + quorumCount);
			return true;
		} else {
			System.out.println("Expected: " + (int) (quorum_quota * 0.7) + " got: " + quorumCount);
			return false;
		}
	}

	// run tasks and update time
	public static void runSchedule(Device m, int task_type, int runtime) throws IOException {

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
			// if (task_type == 5)
			JNode.increaseTime(STATUS_PROCESSING_TIME);
			// else if (task_type == 6)
			// JNode.increaseTime(QUORUM_PROCESSING_TIME);
		} else {
			m.increaseTime(runtime);
			// JNode.setTime(m.getTime());
			// if (task_type == 5)
			// JNode.increaseTime(STATUS_PROCESSING_TIME);
			// else if (task_type == 6)
			// JNode.increaseTime(QUORUM_PROCESSING_TIME);
		}
	}

	public static void updateTimeAndOutput(String taskType, Device m, int k) throws IOException {
		// System.out.println("Task type is: " + taskType);
		// System.out.println("Device name is: " + m.name);
		// System.out.println("Device time is: " + m.time);
		// System.out.println("Device time ended: " + (m.time + k));
		// System.out.println(taskType + " was scheduled on: " + m.name + " at: " +
		// m.time + " for: " + k + " and ended: "
		// + (m.time + k));
		// System.out.println(taskType + " was successfully scheduled" );
		m.increaseTime(k);
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

	public static int getCurrentTimePoint() {

		if (time_point >= 5000) {
			time_point = 0;
			return (int) time_point;
		}
		else
			return (int) time_point;
	}

	public static Random fRandom = new Random();

	public static int getGaussian(double aMean, int opt) {
		double time = 0;
		switch (opt) {
		case 0:
			time = aMean;
			break;
		case 1:
			time = aMean + fRandom.nextGaussian() * 5;
			break;
		case 2:
			time = aMean + fRandom.nextGaussian() * 10;
			break;
		case 3:
			time = aMean + fRandom.nextGaussian() * 15;
			break;
		case 4:
			time = aMean + fRandom.nextGaussian() * 20;
			break;
		}
		return (int) time;
	}

	public static int getGaussian(double aMean) {
		int time = (int) (aMean + RANDOM.nextGaussian() * VARIANCE);
		if (time <= 0)
			getGaussian(aMean);
		return time;
	}

	// get the maximum of the
	public static int getReadyMax() {
		int max = 0;
		// System.out.println("Total machines = " + availMachineMap.getSize());
		for (int i = 0; i < readyMachines.size(); i++) {
			// System.out.println("machine time = " +
			// availMachineMap.getMachine(i).getTime());
			max = (int) Math.max(max, readyMachines.get(i).getTime());
		}
		return max;
	}

	public static int getMax() {
		int max = 0;
		// System.out.println("Total machines = " + availMachineMap.getSize());
		for (int i = 0; i < availMachineMap.getSize(); i++) {
			// System.out.println("machine time = " +
			// availMachineMap.getMachine(i).getTime());
			max = (int) Math.max(max, availMachineMap.getMachine(i).getTime());
		}
		return max;
	}

	public static int getInitMax() {
		int max = 0;
		// System.out.println("Total machines = " + availMachineMap.getSize());
		for (int i = 0; i < initMachines.size(); i++) {
			// System.out.println("machine time = " +
			// availMachineMap.getMachine(i).getTime());
			max = (int) Math.max(max, initMachines.get(i).getTime());
		}
		return max;
	}

	public static void runStatic() throws IOException {
		executeStaticSchedule();
		generateStaticOutput();
	}

	public static void processResult() throws FileNotFoundException, IOException {
		fWriter = new FileWriter("chinaDyna.txt", true);
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
	public static void generateStaticOutput() throws IOException {

		System.out
				.println("==========================================================================================");
		System.out.println("Starting static scheduling report...................... ");
		System.out
				.println("==========================================================================================");
		System.out.println("Totl no of sync tasks: " + sync_counter);
		System.out.println("No of runs: " + RUNS);
		System.out.println("No of tasks: " + (staticInputMap.size() * RUNS));
		System.out.println("Total time: " + getMax());
		System.out.println("Failed sync tasks: " + syncFail);
		System.out.println("Extra quorum attempts: " + total_quorum_attempts);
		System.out.println(
				"============================================================================================");
		System.out.println("End of static scheduling report");
		System.out.println(
				"============================================================================================");

		// Double p = (double) syncFail / (double) sync_counter * 100;

		try {
			traceWriter.write(getInitMax() + "\n");
		} catch (Exception e) {
			System.out.println("Could not write to output: " + e);
		}

		// fileWriter.write(df.format(getMax()) + " " + df.format(sync_delay) + " "
		// + (total_quorum_attempts + sync_task_counter) + " " + failedSyncTaskCounter +
		// "\n");
	}

	// miscellaneous methods
	// ----------------------------------------------------------------------------------------------//

	public static void sortByName(List<Device> list) {
		Collections.sort(list, new Comparator<Device>() {

			public int compare(Device m1, Device m2) {
				return m1.name.compareTo(m2.name);
			}
		});
	}

	public static List<Device> returnListDifference(List<Device> one, List<Device> two) {
		List<Device> tempDevices = new ArrayList<Device>();
		Boolean bool = true;
		for (int i = 0; i < one.size(); i++) {
			for (int j = 0; j < two.size(); j++) {
				if (one.get(i).name == two.get(j).name)
					bool = false;
			}
			if (bool)
				tempDevices.add(one.get(i));
			bool = true;
		}
		return tempDevices;
	}

	public static List<Device> convertSetToList(Set<Device> hash) {
		List<Device> convertedList = new ArrayList<Device>();
		hash.forEach(d -> convertedList.add(d));
		return convertedList;
	}

	public static List<Cluster> convertSetToListCluster(Set<Cluster> hash) {
		List<Cluster> convertedList = new ArrayList<Cluster>();
		hash.forEach(d -> convertedList.add(d));
		return convertedList;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {

		/*
		 * try { CLUSTER_MIN_SIZE = Integer.parseInt(args[0]); OUTFILE = args[1]; }
		 * catch (Exception e) { System.out.println("Invalid input(s)"); }
		 */
		traceWriter = new FileWriter("chinaDynaOut.txt", true);
		fWriter = new FileWriter("chinaDyna.txt", true);
		initClusterList = new ArrayList<Cluster>();
		streamMap = new Util.StreamMap();
		deviceMap = new Utility.MyMap();
		staticInputMap = new Util.SimpleMap(); // holds tasks
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

		runStatic();
		// processResult();
		System.out.println("Simulation ended");
		traceWriter.close();
		processResult();
		fWriter.close();
		System.out.println("Starting simulation....");

	}

}
