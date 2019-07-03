package multipoint;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import map.Utility;

//class to hold simulation parameters
public class Items {
	
	//from dynamic executor
	//private static FileWriter fileWriter;
	static Device JNode; // J-machine

	// counter for no of J-update points
	static int quorumCount = 0; // counter to check for quorum
	static int numClusters = 0; // counter to check for quorum
	static int syncSuccess = 0; // counter to check for quorum
	static int syncFail = 0; // counter to check for quorum
	static int syncFailT = 0; // counter to check for quorum
	static int syncFailC = 0; // counter to check for quorum
	static int syncAttempt = 0; // counter to check for quorum
	public static int counter = 0; // counter for no of loops
	public static int dynamic_counter = 0; // dynamic counter
	public static int dynamicSyncCounter = 0; // dynamic sync counter
	public static int quorum_attempts = 0; // number of times quorum has been attempted
	public static int total_quorum_retries = 0; // number of times quorum has
	public static int total_quorum_attempts = 0; // number of times quorum has
	public static int val = 0;
	public static double time_point = 0;
													// been
	public static double dynamic_revised_available_time;
	public static double barrier_time;
	public static double static_available_time;
	public static double average_time;
	
	public static int failedSyncTaskCounter = 0;
	public static int sync_task_counter = 0;
	
	public static int failed_machines_counter;


	public static double sync_delay = 0.0; // synchronization delay
	public static double DECISION = 0.5; // decision variable to decide whether
	// to fail or join
	public static double JOIN_ID; // id for joining nodes
	
	//tags for identifying tasks
	public static final int TAG_C_ASYNC = 1;
	public static final int TAG_C_SYNC = 2;
	public static final int TAG_C_LOCAL = 3;
	public static final int TAG_J_LOCAL = 4; 
	public static final int TAG_UPDATE = 5;
	public static final int TAG_QUORUM = 6;
	
	
	//public static FileWriter traceWriter ;

	public static int sync_counter = 0; //count no of synchronizations
	static int CLUSTER_MIN_SIZE;
	public static int INIT_DEVICES;
	//number of devices initialized at startup
	//public static final int INIT_DEVICES = 10; 
	public static final int MAX_RETRIES = 2;
	
	public static double COMMUNICATION_TIME = 5;
	public static double COMM_TIME;
	//static final int RESIDENCE_TIME = 200;
	public static final double QUORUM_TIME = 10;
	public static double DEGREE;
	
	//custom map for reading input task graph for dynamic schedule
	public static Util.SimpleMap dynamicInputMap; 
	public static Util.SimpleMap staticInputMap; //custom map for reading input task graph for static schedule

	//custom map with mobility pattern of devices
	public static Util.StreamMap mobilityPattern;
	
	 //custom map for all devices created
	public static Util.MachineMap<Device> unsortedMachineMap;
	 //custom map for available devices excluding failed machines
	public static Util.MachineMap<Device> availMachineMap;
	
	
	public static List<Device> initMachines; //initialized devices at startup
	public static List<Device> assignedMachines; //initialized devices at startup
	public static List<Device> unusedMachines; //initialized devices at startup
	public static List<Device> neighbourList; //possible neighbor devices
	public static List<Device> readyMachines; //custom map for quorum devices
	
	public static Set<Device> unusedDevices;
	
	static final Random RANDOM = new Random();
	static DecimalFormat df;
	
	static final Cluster NONE = new Cluster("NONE");
	
	
	//.............................................................//
	
	
	public static String INPUTFILE;
	
	public static final int VARIANCE = 10; // variance and quorum delay
	public static final double VAR = 20;
	public static final double STATUS_PROCESSING_TIME = 1;
	public static final double QUORUM_PROCESSING_TIME = 1;
	public static final double STATUS_SENDING_TIME = 5; 
	public static final int RUNS = 1;
	public static final int QUORUM_RETRIES = 2;
	public static final int NO_RETRIES = 1;
	public static final double PUB_SUB_RATIO = 0.4; //ratio of marked devices for pubsub
	public static double LAMBDA;
	public static double MULTIPLIER;
	
	
	//.............................................................//
	// methods and common variables
	
	static List<Cluster> initClusterList;
	static Util.StreamMap streamMap;
	static Utility.MyMap deviceMap;
	static List<ArrayList<Integer>> grids;
	static int sum;
	static int total_sync_counter = 0;
	static FileWriter traceWriter, fWriter;
	static String OUTFILE, OUTFILE1, OUTFILE2;

	// tasks
	public static int total_execution_time = 0;
	public static int quorum_quota = 0;
	public static int prevMax = 0;
	public static int newMax = 0;
	public static int message_count = 0;
	static int keepCount = 0; // for multiplier
	
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
		// int m_count = 0;
		System.out.println("Initializing machines");
		for (int i = 0; i < INIT_DEVICES; i++) {
			// if (m_count < INIT_DEVICES) {
			String name = "Dev" + (i + 1);
			Device d = new Device(name, i + 1); // device name starting from 1
			initMachines.add(d); // add all devices to list
			
			List<Integer> temp = grids.get(i); // grids occupied by device i

			for (int j = 0; j < temp.size(); j++) {
				if (temp.get(j) == 0) {
					d.addCluster(NONE);
				} else {
					d.addCluster(initClusterList.get(temp.get(j)));
				}
			}
		}
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
				double time = Double.parseDouble(splits[1]); // execution time

				// put execution times tag and task group in map for dynamic scheduling
				if (tag == TAG_C_ASYNC || tag == TAG_C_SYNC || tag == TAG_C_LOCAL || tag == TAG_J_LOCAL) {
					dynamicInputMap.insert(tag, time);
				}
			}
			// close reader
			reader.close();
		}		
	}
	
	public static List<Cluster> getMachines(int time_point) throws FileNotFoundException, IOException {
		availMachineMap.clear();
		assignedMachines.clear();
		List<Cluster> clusterSet = new ArrayList<Cluster>();
		for (int j = 0; j < initMachines.size(); j++) {
			Device d = initMachines.get(j);
			d.multiplier = getMultiplier();
			//System.out.println(d.multiplier);
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
	

	public static double getMultiplier() {
		// int opt = 0;
		double value = 0.0;
		switch (keepCount) {
		case 0:
			value = 1.0 * MULTIPLIER;
			break;
		case 1:
			value = 0.97 * MULTIPLIER;
			break;
		case 2:
			value = 0.93 * MULTIPLIER;
			break;
		case 3:
			value = 0.9 * MULTIPLIER;
			break;
		case 4:
			value = 0.85 * MULTIPLIER;
			break;
		}
		if (keepCount < 4) {
			keepCount++;
		} else {
			keepCount = 0;
		}
		return value;
	}
	
	public static void schdLocalJ(int task_num) throws IOException {

		// System.out.println("Running task: " + (task_num + 1));
		// schedule task
		runSchedule(JNode, dynamicInputMap.getKey(task_num), dynamicInputMap.getValue(task_num));
	}
	

	// run tasks and update time
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
		if (quorumCount >= (int) (quorum_quota * DEGREE)) {
			System.out.println("Compared: " + (int) (quorum_quota * DEGREE) + " to " + quorumCount);
			return true;
		} else {
			System.out.println("Expected: " + (int) (quorum_quota * DEGREE) + " got: " + quorumCount);
			return false;
		}
	}
	
	public static int getCurrentTimePoint() {

		if (time_point >= 300000) {
			time_point = 0;
			return (int) time_point;
		} else
			return (int) (time_point / 100);
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

