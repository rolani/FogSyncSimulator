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
import java.util.Set;

import map.MapProcessor;
import map.Utility;

public class CombinedSchedulerOvershoot extends Items {
	static List<Cluster> initClusterList;
	static Util.StreamMap streamMap;
	static Utility.MyMap deviceMap;
	static int sum;
	static FileWriter traceWriter;
	static String OUTFILE; 
	static int sync_count = 0;

	// extract devices and grid from map processor
	public static void getDetailsFromMap() throws FileNotFoundException, IOException {
		MapProcessor.processData();
		deviceMap = MapProcessor.assignGridsToIds();
		createClusters();
		createInitDevices();
	}

	public static void createClusters() {
		for (int i = 1; i < 26; i++) {
			String cName = "C" + i;
			Cluster c = new Cluster(cName, i);
			initClusterList.add(c);
		}
	}

	// create devices from ids and assign grids for all time points
	public static void createInitDevices() throws FileNotFoundException, IOException {
		for (int i = 0; i < deviceMap.getSize(); i++) {
			String name = "Dev" + (deviceMap.Ids.get(i));
			Device d = new Device(name, deviceMap.Ids.get(i));
			initMachines.add(d); // add all devices to list

			List<Integer> temp = deviceMap.getGridForId(deviceMap.Ids.get(i));

			for (int j = 0; j < temp.size(); j++) {
				if (temp.get(j) == 0) {
					d.addCluster(NONE);
				} else {
					d.addCluster(initClusterList.get(temp.get(j) - 1));
				}
			}
		}

		// unusedDevices.forEach(d -> d.addCluster(NONE));
		// createClusters();
		getTasks();
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
				if (tag == TAG_C_ASYNC || tag == TAG_C_SYNC || tag == TAG_C_LOCAL || tag == TAG_J_LOCAL) {
					dynamicInputMap.insert(tag, time);
				}
			}
			// close reader
			reader.close();
		}
		// createInitDevices();
	}

	public static void executeDynaSchedule() throws IOException {
		getDetailsFromMap();
		// loop through task graph in custom map
		for (int i = 0; i < dynamicInputMap.size(); i++) {
			// check if task is local task
			if (dynamicInputMap.getKey(i) == TAG_C_LOCAL) {
				List<Device> noneDevices = new ArrayList<Device>();
				noneDevices = getAsyncMachines(getCurrentTimePoint());
				// availMachineMap.sortByTime();
				schdAsyncOrLocalC(i);
				dynamic_revised_available_time += dynamicInputMap.getValue(i) + 1;
				average_time += dynamicInputMap.getValue(i);
				time_point += dynamicInputMap.getValue(i);
				for (int u = 0; u < noneDevices.size(); u++) {
					noneDevices.get(u).setTime((int)average_time);
				}
			}

			// check if task is asynchronous task
			else if (dynamicInputMap.getKey(i) == TAG_C_ASYNC) {
				List<Device> noneDevices = new ArrayList<Device>();
				// sort machine times
				noneDevices = getAsyncMachines(getCurrentTimePoint());
				// availMachineMap.sortByTime();
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				schdAsyncOrLocalC(i);
				dynamic_revised_available_time += dynamicInputMap.getValue(i) + COMMUNICATION_TIME + 1;
				average_time += dynamicInputMap.getValue(i) + COMMUNICATION_TIME;
				time_point += dynamicInputMap.getValue(i);
				for (int u = 0; u < noneDevices.size(); u++) {
					noneDevices.get(u).setTime((int)average_time);
				}
			}

			// check if task is C sync task
			else if (dynamicInputMap.getKey(i) == TAG_C_SYNC) {
				sync_counter++;
				List<Cluster> syncMap = new ArrayList<Cluster>();
				List<Device> noneDevices = new ArrayList<Device>();
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				System.out.println("Time point is: " + time_point);
				// get list of clusters
				syncMap = getSyncMachines(getCurrentTimePoint());
				System.out.println("Size of syncMap from getSyncMachines is: " + syncMap.size());
				// run sync scheme
				syncAvailUpdate(syncMap, noneDevices, time_point, i);
				// update all devices to current point
				// update JNode time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				dynamic_revised_available_time = Math.max(getMax(), JNode.getTime());
				
				// clear ready device list for next iteration
				readyMachines.clear();
				quorumCount = 0;

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

	// get machines available at time point to run async task
	public static List<Device> getAsyncMachines(int time_point) throws FileNotFoundException, IOException {
		// List<Cluster> yesList = new ArrayList<Cluster>();
		List<Device> noneList = new ArrayList<Device>();
		availMachineMap.clear();
		for (int j = 0; j < initMachines.size(); j++) {
			Cluster c = initMachines.get(j).getClusterAtTimePoint(time_point);
			if (c == NONE) {
				noneList.add(initMachines.get(j));
			} else {
				availMachineMap.putMachine(initMachines.get(j));
			}
		}
		return noneList;
	}

	// returns unique grids with devices in them
	public static List<Cluster> getSyncMachines(int time_point) throws FileNotFoundException, IOException {
		// Set<Cluster> clusterSet = new HashSet<Cluster>();
		List<Cluster> clusterSet = new ArrayList<Cluster>();
		for (int i = 0; i < initClusterList.size(); i++) {
			initClusterList.get(i).clearDeviceList();
		}

		for (int j = 0; j < initMachines.size(); j++) {

			Cluster c = initMachines.get(j).getClusterAtTimePoint(time_point);
			Device device = initMachines.get(j);
			if (clusterSet.contains(c) == false && c != NONE) {
				if (c.numOfDevices() >= CLUSTER_MIN_SIZE) {
					clusterSet.add(c);
				}
				// System.out.println(c.name + "added");
			}
			c.addDevice(device);
		}
		System.out.println("Set size is: " + clusterSet.size());
		for (Cluster cluster : clusterSet) {
			System.out.println("unique cluster " + cluster.name + " " + cluster.numOfDevices());
		}
		return clusterSet;
	}

	// returns unique grids with devices in them
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
		for (Cluster cluster : clusterSet) {
			System.out.println("unique cluster " + cluster.name + " " + cluster.numOfDevices());
		}
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

		System.out.println("Running task: " + (task_num + 1));
		// schedule task
		runSchedule(JNode, dynamicInputMap.getKey(task_num), dynamicInputMap.getValue(task_num));
	}

	// availMachineMap contains devices currently present in the system
	// should be pre-processed before calling this method

	// schedule a C-async or C-local task
	public static void schdAsyncOrLocalC(int task_num) throws IOException {

		System.out.println("Running task: " + (task_num + 1));
		// loop through available machines in the system
		for (int k = 0; k < availMachineMap.getSize(); k++) {
			int runtime = 0;
			if (dynamicInputMap.getKey(task_num) == TAG_C_ASYNC) {
				availMachineMap.getMachine(k).increaseTime(COMMUNICATION_TIME);
			}
			// generate heterogeneous runtime based on normal distribution
			runtime = getGaussian(dynamicInputMap.getValue(task_num));

			// schedule task
			runSchedule(availMachineMap.getMachine(k), dynamicInputMap.getKey(task_num), runtime);
		}

	}

	// dynamic_revised_available_time should be updated to the time first machine
	// shows up plus allowed delay
	public static void syncAvailUpdate(List<Cluster> clusterMachineMap, List<Device> noneDevices, double task_num, int i)
			throws IOException {
		quorumCount = 0;
		numClusters = clusterMachineMap.size();
		readyMachines.clear();
		System.out.println("Total number of clusters is: " + numClusters);
		// dynamic_revised_available_time = JNode.time;
		System.out.println("Running update task");
		for (int f = 0; f < clusterMachineMap.size(); f++) {
			List<Device> tempDevices = new ArrayList<Device>();

			tempDevices = clusterMachineMap.get(f).getDevices();

			System.out.println("The devices under: " + clusterMachineMap.get(f).name + " are: ");
			tempDevices.forEach(devi -> System.out.print(devi.name + ";"));
			// tempDevices.forEach(devi -> devi.setTime(getMinOrMax()));
			System.out.println("  ");

			boolean counted = true;
			// available machines send update to J-machine
			for (int j = 0; j < tempDevices.size(); j++) {
				Device dev = tempDevices.get(j);

				if (dev.getTime() <= dynamic_revised_available_time) {
					if (counted) {
						quorumCount++;
						counted = false;
					}
					// System.out.println(
					// dev.getTime() + " was compared to " + dynamic_revised_available_time + " for
					// " + dev.name);
					// update fog time -- thread

					JNode.setTime(Math.max(dev.getTime(), JNode.getTime()));
					// run update sending task
					runSchedule(dev, TAG_UPDATE, STATUS_SENDING_TIME); // ******************
					// quorumCount++;
					readyMachines.add(dev);
				} else {
					// System.out.println(
					// dev.getTime() + " is greater than " + dynamic_revised_available_time + " for
					// " + dev.name);
				}
			}
			tempDevices.clear();
		}

		for (Device ddd : noneDevices) {
			ddd.increaseTime(STATUS_SENDING_TIME);
		}
		// System.out.println("Fog time is: " + JNode.getTime());

		// update device time to latest finish
		for (int h = 0; h < readyMachines.size(); h++) {
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		//time_point += 1;
		checkQuorum(clusterMachineMap, readyMachines, noneDevices, i);

	}

	// check if at least a device per cluster sent update message
	public static void checkQuorum(List<Cluster> clusterMachineMap, List<Device> readyDevices, List<Device> noneDevices,
			int task_num) throws IOException {
		System.out.println("Checking for quorum");
		// run quorum for ready machines
		for (int g = 0; g < readyDevices.size(); g++) {
			// schedule quorum task
			runSchedule(readyDevices.get(g), TAG_QUORUM, QUORUM_TIME);
		}
		// time_point += 1;
		// update all machine time?????? WHY THIS????
		for (int j = 0; j < availMachineMap.getSize(); j++) {
			if (availMachineMap.getMachine(j).getTime() <= getReadyMax()) {
				availMachineMap.getMachine(j).setTime(getReadyMax());
			}
		}

		// update machine times after quorum
		for (int h = 0; h < readyDevices.size(); h++) {
			// schedule quorum task
			readyDevices.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
		}

		for (Device ddd : noneDevices) {
			ddd.setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		
		if (isQuorum()) {
			scheduleSync(clusterMachineMap, readyDevices, noneDevices, task_num);
		} else {
			//update this part for synchronization retries
			if (sync_count < MAX_RETRIES) {
				syncAvailUpdate(clusterMachineMap, noneDevices, task_num, task_num);
				sync_count++;
				syncAttempt++;
			}
			System.out.println("Sync task failed due to failed quorum, task number: " + (task_num + 1));
			syncFail++;
			readyMachines.clear();
		}
	}

	// if quorum successful go ahead and run sync task
	public static void scheduleSync(List<Cluster> clusterMachineMap, List<Device> runDevices, List<Device> noneDevices,
			int task_num) throws IOException {
		time_point += dynamicInputMap.getValue(task_num);

		System.out.println("Quorum successful!!!!!");

		for (int h = 0; h < runDevices.size(); h++) {
			// update device times
			runDevices.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		System.out.println("Running task: " + (task_num + 1));
		// schedule sync task
		for (int z = 0; z < runDevices.size(); z++) {
			runDevices.get(z).increaseTime(COMMUNICATION_TIME);
			// ready machines should be in sync at this point
			runSchedule(runDevices.get(z), TAG_C_SYNC, getGaussian(dynamicInputMap.getValue(task_num)));
		}

		for (Device ddd : noneDevices) {
			ddd.increaseTime(dynamicInputMap.getValue(task_num));
		}

		if (checkSyncSuccessNew(clusterMachineMap)) {
			System.out.println("Sync task successfully completed");
			syncSuccess++;
		} else {
			System.out.println("Sync task execution failed");
			syncFail++;
		}
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
		else {
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
		for (int i = 0; i < syncMap.size(); i ++) {
			tempDevices = syncMap.get(i).getDevices();;
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
		return (int)time_point;
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
					// traceWriter.write("Device failed: " + mach.name + "\n");
					// remove machine from available list
					availMachineMap.removeMachine(mach);
				}
			}
		}
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
		System.out.println("Total time: " + getMax());
		System.out.println("Failed sync tasks: " + syncFail);
		System.out.println(
				"============================================================================================");
		System.out.println("End of dynamic scheduling report");
		System.out.println(
				"============================================================================================");
		
		Double p = (double) syncFail/(double) sync_counter * 100;
		
		try {
			traceWriter.write(p +"\n");
		}catch(Exception e) {
			System.out.println("Could not write to output: " + e); 
		}

		// fileWriter.write(df.format(getMax()) + " " + df.format(sync_delay) + " "
		// + (total_quorum_attempts + sync_task_counter) + " " + failedSyncTaskCounter +
		// "\n");
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {

		try {
			CLUSTER_MIN_SIZE = Integer.parseInt(args[0]);
			OUTFILE = args[1];
		} catch (Exception e) {
			System.out.println("Invalid input(s)");
		}
		traceWriter = new FileWriter(OUTFILE, true);
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
		
		
		System.out.println("Starting simulation....");
		runDynamic();
		System.out.println("Simulation ended");
		traceWriter.close();
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

}
