package sim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CombinedScheduler extends Items {
	static List<Cluster> initClusterList;
	static Util.StreamMap streamMap;
	static int sum;

	public static void createInitDevices() throws FileNotFoundException, IOException {
		for (int i = 0; i < INIT_DEVICES; i++) {
			String name = "Dev" + (i + 1);
			Device d = new Device(name, i + 1);
			initMachines.add(d);
			// unusedMachines.add(d);
			unusedDevices.add(d);
		}
		createGrids();
		initialGrid();
		initGrids();
	}

	public static void initialGrid() throws FileNotFoundException, IOException {
		Util.NewMap details = new Util.NewMap();
		int[][] grid = new int[10][10];// store mean value for each grid 10 x 10
		Reader.readInput();
		details = Reader.getTimePoint(0);// get all the data for this time point
		int index = 0; // grid counter
		int dev_count = 0; // device counter
		for (int j = 0; j < 10; j++) {
			for (int z = 0; z < 10; z++) {
				// save each mean number of devices per grid
				grid[j][z] = (int) Math.round(details.getMean(index));
				sum += grid[j][z];

				if (grid[j][z] > 0) {
					Cluster c = initClusterList.get(index);
					for (int k = 0; k < grid[j][z]; k++) {
						Device d = initMachines.get(dev_count);
						assignedMachines.add(d);
						// unusedMachines.remove(d);
						unusedDevices.remove(d);
						d.addCluster(c);
						c.addDevice(d);
						c.tempoList.add(d);
						dev_count++;
						// System.out.println("Timepoint 0; Device: " + d.name + " joined cluster: " +
						// c.name);
					}
				}
				index++;
			}
		}

		unusedDevices.forEach(d -> d.addCluster(NONE));
		// for (int f = 0; f < unusedMachines.size(); f++) {
		// unusedMachines.get(f).addCluster(NONE);

		// System.out.println("Timepoint 0; Device: " + unusedMachines.get(f).name + "
		// joined cluster: " + NONE.name);
		// }
	}

	public static void initGrids() throws FileNotFoundException, IOException {
		for (int j = 1; j < TIMEPOINT; j++) {
			assignMachines(j);
		}
	}

	public static void createGrids() {
		// int c = 0;
		for (int j = 0; j < 10; j++) {
			for (int z = 0; z < 10; z++) {
				// save each mean number of devices per grid
				String name = "Con-" + Integer.toString(j) + "-" + Integer.toString(z);
				Cluster gc = new Cluster(name);
				initClusterList.add(gc);
				// c++;
			}
		}
		assignNeighbours();
	}

	// get the current cluster for each device
	// necessary for getting total number of machines per cluster
	public static void assignMachines(int time_point) throws FileNotFoundException, IOException {
		Util.NewMap details = new Util.NewMap();
		int[][] grid = new int[10][10];// store mean value for each grid 10 x 10
		Reader.readInput();
		details = Reader.getTimePoint(time_point);// get all the data for this time point
		int index = 0;
		for (int j = 0; j < 10; j++) {
			for (int z = 0; z < 10; z++) {
				// save each mean number of devices per grid
				grid[j][z] = (int) Math.round(details.getMean(index));
				sum += grid[j][z];
				index++;
			}
		}
		preProcess(grid, time_point);
	}

	// processing grids and device joining
	public static void preProcess(int[][] grid, int time_point) throws IOException {
		List<Device> tempAssigned = new ArrayList<Device>();
		List<Device> tempDevices = new ArrayList<Device>();
		int[][] temp = grid;
		int index = 0;
		for (int j = 0; j < 10; j++) {
			for (int z = 0; z < 10; z++) {
				Cluster c = initClusterList.get(index);
				c.setAssigned(false);
				// create controller(s) for grid
				if (temp[j][z] > 0) {
					// for debugging
					/*
					 * for (Device dev : c.getDevices()) { System.out.println(dev.name); }
					 */

					if (c.numOfDevices() == temp[j][z]) {
						for (Device dev : c.getDevices()) {
							c.tempoList.clear();
							dev.addCluster(c);
							dev.hasJoined = true;
							tempAssigned.add(dev);
							if (!c.tempoList.contains(dev))
								c.tempoList.add(dev);
							// unusedMachines.remove(dev);
							unusedDevices.remove(dev);
							// c.addDevice(dev);
							// System.out.println("Same as previous time point");
							// System.out.println(
							// "Timepoint" + time_point + "; Device: " + dev.name + " joined cluster: " +
							// c.name);
						}
					} else if (c.numOfDevices() > temp[j][z]) {
						// System.out.println("values" + c.numOfDevices() + " > " + temp[j][z]);
						for (Device dev : c.getSelectedDevices(temp[j][z])) {
							c.tempoList.clear();
							dev.addCluster(c);
							dev.hasJoined = true;
							tempAssigned.add(dev);
							if (!c.tempoList.contains(dev))
								c.tempoList.add(dev);
							// unusedMachines.remove(dev);
							unusedDevices.remove(dev);
							// c.addDevice(dev);
							// System.out.println("Greater than previous time point");
							// System.out.println(
							/// "Timepoint" + time_point + "; Device: " + dev.name + " joined cluster: " +
							// c.name);
						}
					} else {
						for (Cluster clus : c.neighbourList) {
							for (Device dev : clus.unSelectedDeviceList) {
								if (clus.isAssigned()) {
									if (c.numOfDevices() <= temp[j][z]) {
										if (!dev.hasJoined) {
											c.tempoList.clear();
											dev.addCluster(c);
											tempAssigned.add(dev);
											if (!c.tempoList.contains(dev))
												c.tempoList.add(dev);
											// System.out.println("Less than previous time point");
											// System.out.println("Timepoint" + time_point + "; Device: " + dev.name
											// + " joined cluster: " + c.name);
											dev.hasJoined = true;
											c.addDevice(dev);
											// unusedMachines.remove(dev);
											unusedDevices.remove(dev);
										}
									}
								}
							}
						}

						while (c.numOfDevices() <= temp[j][z]) {
							if (unusedDevices.size() > 0) {
								// c.tempoList.clear();
								Device dd = convertSetToList(unusedDevices).get(0);
								dd.addCluster(c);
								tempAssigned.add(dd);
								if (!c.tempoList.contains(dd))
									c.tempoList.add(dd);
								// System.out.println("Less than previous time point");
								// System.out.println(
								// "Timepoint" + time_point + "; Device: " + dd.name + " joined cluster: " +
								// c.name);
								c.addDevice(dd);
								// unusedMachines.remove(dd);
								unusedDevices.remove(dd);
							}
						}
					}
					c.setAssigned(true);
				} else {
					c.deviceList.clear();
				}
				index++;
			}
		}
		tempDevices = returnListDifference(initMachines, tempAssigned);
		unusedDevices.addAll(tempDevices);
		// unusedMachines.addAll(tempDevices);
		unusedMachines = convertSetToList(unusedDevices);
		sortByName(unusedMachines);
		for (int f = 0; f < unusedMachines.size(); f++) {

			unusedMachines.get(f).addCluster(NONE);
			unusedMachines.get(f).setTime(getReadyMax());
			// System.out.println("Timepoint" + time_point + "; Device: " +
			// tempDevices.get(f).name + " joined cluster: " + NONE.name);
		}
		sum = 0;
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

	// return the list number of devices to cover current time point
	public static List<Device> returnValidDevices() {
		List<Device> validDevices = new ArrayList<Device>();
		for (int i = 0; i < sum; i++) {
			validDevices.add(initMachines.get(i));
		}
		return validDevices;
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

	public static void assignNeighbours() {
		int totalCount = 0;
		for (int j = 0; j < 10; j++) {
			for (int z = 0; z < 10; z++) {
				int NORTH = (j - 1) * 10 + z;
				int SOUTH = (j + 1) * 10 + z;
				int EAST = (j * 10) + (z + 1);
				int WEST = (j * 10) + (z - 1);
				int NORTHEAST = (j - 1) * 10 + (z + 1);
				int NORTHWEST = (j - 1) * 10 + (z - 1);
				int SOUTHEAST = (j + 1) * 10 + (z + 1);
				int SOUTHWEST = (j + 1) * 10 + (z - 1);
				if (j == 0 && z == 0) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(EAST), initClusterList.get(SOUTH),
							initClusterList.get(SOUTHEAST));
				} else if (j == 0 && z == 9) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(WEST), initClusterList.get(SOUTH),
							initClusterList.get(SOUTHWEST));
				} else if (j == 9 && z == 0) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(NORTH), initClusterList.get(EAST),
							initClusterList.get(NORTHEAST));
				} else if (j == 9 && z == 9) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(NORTH), initClusterList.get(WEST),
							initClusterList.get(NORTHWEST));
				} else if (z > 0 && z < 9 && j == 0) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(SOUTH), initClusterList.get(EAST),
							initClusterList.get(WEST), initClusterList.get(SOUTHWEST), initClusterList.get(SOUTHEAST));
				} else if (z > 0 && z < 9 && j == 9) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(NORTH), initClusterList.get(EAST),
							initClusterList.get(WEST), initClusterList.get(NORTHWEST), initClusterList.get(NORTHEAST));
				} else if (j > 0 && j < 9 && z == 0) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(SOUTH), initClusterList.get(EAST),
							initClusterList.get(NORTH), initClusterList.get(NORTHEAST), initClusterList.get(SOUTHEAST));
				} else if (j > 0 && j < 9 && z == 9) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(SOUTH), initClusterList.get(WEST),
							initClusterList.get(NORTH), initClusterList.get(NORTHWEST), initClusterList.get(SOUTHWEST));
				} else if (j > 0 && j < 9 && z > 0 && z < 9) {
					initClusterList.get(totalCount).addNeighbours(initClusterList.get(SOUTH), initClusterList.get(EAST),
							initClusterList.get(NORTH), initClusterList.get(NORTHEAST), initClusterList.get(SOUTHEAST),
							initClusterList.get(WEST), initClusterList.get(NORTHWEST), initClusterList.get(SOUTHWEST));
				}
				totalCount++;
			}
		}
	}

	public static void getMobility() throws FileNotFoundException, IOException {

		System.out.println("Size of initMachine is: " + initMachines.size());
		for (int i = 0; i < initMachines.size(); i++) {
			streamMap.addSeries(initMachines.get(i), initMachines.get(i).clusterList);
		}
		// streamMap.print();
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

				// put execution times tag and task group in map for dynamic
				if (tag == TAG_C_ASYNC || tag == TAG_C_SYNC || tag == TAG_C_LOCAL || tag == TAG_J_LOCAL) {
					dynamicInputMap.insert(tag, time);
				}
			}
			// close reader
			reader.close();
		}
		createInitDevices();
		getMobility();
	}

	// get machines available at time point to run async task
	public static List<Device> getAsyncMachines(int time_point) throws FileNotFoundException, IOException {
		// List<Cluster> yesList = new ArrayList<Cluster>();
		List<Device> noneList = new ArrayList<Device>();
		availMachineMap.clear();
		for (int j = 0; j < streamMap.getSize(); j++) {
			Cluster c = streamMap.clusterList.getCluster(j).get(time_point);
			if (c == NONE) {
				noneList.add(streamMap.clusterList.getDevice(j));
			} else {
				availMachineMap.putMachine(streamMap.clusterList.getDevice(j));
			}
		}
		return noneList;
	}

	// perform some operation on each time point specified by the id
	public static List<Cluster> getSyncMachines(int time_point) throws FileNotFoundException, IOException {
		Set<Cluster> clusterSet;
		Device device;
		List<Cluster> clusterMap = new ArrayList<Cluster>();
		List<Cluster> cMap = new ArrayList<Cluster>();
		List<Cluster> retMap = new ArrayList<Cluster>();


		for (int j = 0; j < streamMap.getSize(); j++) {
			Cluster c = streamMap.clusterList.getCluster(j).get(time_point);
			device = streamMap.clusterList.getDevice(j);
			if (!clusterMap.contains(c) && c != NONE) {
				clusterMap.add(c);
				c.currentList.add(device);
			} else if (clusterMap.contains(c) && c != NONE) {
				c.currentList.add(device);
			}
		}
		clusterSet = new HashSet<Cluster>(clusterMap);
		System.out.println("Set size is: " + clusterSet.size());
		cMap = convertSetToListCluster(clusterSet);
		for (Cluster clus : cMap) {
			if (clus.currentList.size() < MIN_CLUSTER_SIZE) {
				if (getMinNeighbour(clus.neighbourList) != null) {
					getMinNeighbour(clus.neighbourList).currentList.addAll(clus.currentList);
				}
			}else {
				retMap.add(clus);
			}
		}
		return retMap;
	}
	
	public static Cluster getMinNeighbour(List<Cluster> list) {
		Cluster c = null;
		int min = list.get(0).currentList.size();
		for (Cluster clus: list) {
			if (clus.currentList.size() < min ) {
				min = clus.currentList.size();
				c = clus;
			}
		}	
		//System.out.println("Device to be returned is: " + c.name);
		return c;
	}

	public static List<Device> getNoneList(int time_point) throws FileNotFoundException, IOException {
		List<Device> noneList = new ArrayList<Device>();

		for (int j = 0; j < streamMap.getSize(); j++) {
			Cluster c = streamMap.clusterList.getCluster(j).get(time_point);
			Device device = streamMap.clusterList.getDevice(j);
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
			double runtime = 0;
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
	public static void syncAvailUpdate(List<Cluster> clusterMachineMap, List<Device> noneDevices, int task_num, int i)
			throws IOException {
		quorumCount = 0;
		numClusters = clusterMachineMap.size();
		readyMachines.clear();
		System.out.println("Total number of clusters is: " + numClusters);
		// dynamic_revised_available_time = JNode.time;
		System.out.println("Running update task");
		for (int f = 0; f < clusterMachineMap.size(); f++) {
			List<Device> tempDevices = new ArrayList<Device>();
			clusterMachineMap.get(f).sortByTime();
			tempDevices = clusterMachineMap.get(f).currentList;
			System.out.println("The devices under: " + clusterMachineMap.get(f).name + " are: ");
			tempDevices.forEach(devi -> System.out.print(devi.name + ";"));
			// tempDevices.forEach(devi -> devi.setTime(getMinOrMax()));
			System.out.println("  ");
			boolean counted = true;
			// available machines send update to J-machine
			for (int j = 0; j < tempDevices.size(); j++) {
				Device dev = tempDevices.get(j);
				dev.setCurrentCluster(clusterMachineMap.get(f));
				if (dev.getTime() <= dynamic_revised_available_time) {
					if (counted) {
						quorumCount++;
						counted = false;
					}
					System.out.println(
							dev.getTime() + " was compared to " + dynamic_revised_available_time + " for " + dev.name);
					// update fog time -- thread
					JNode.setTime(Math.max(dev.getTime(), JNode.getTime()));
					// run update sending task
					runSchedule(dev, TAG_UPDATE, STATUS_SENDING_TIME); // ******************
					// quorumCount++;
					readyMachines.add(dev);
				} else
					System.out.println(
							dev.getTime() + " is greater than " + dynamic_revised_available_time + " for " + dev.name);

			}
			tempDevices.clear();
		}

		for (Device ddd : noneDevices) {
			ddd.increaseTime(STATUS_SENDING_TIME);
		}
		System.out.println("Fog time is: " + JNode.getTime());

		// update device time to latest finish
		for (int h = 0; h < readyMachines.size(); h++) {
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		checkQuorum(readyMachines, noneDevices, i);

	}

	// check if at least a device per cluster sent update message
	public static void checkQuorum(List<Device> readyDevices, List<Device> noneDevices, int task_num)
			throws IOException {
		System.out.println("Checking for quorum");
		// run quorum for ready machines
		for (int g = 0; g < readyDevices.size(); g++) {
			// schedule quorum task
			runSchedule(readyDevices.get(g), TAG_QUORUM, QUORUM_TIME);
		}
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
			scheduleSync(readyDevices, noneDevices, task_num);
		} else {
			System.out.println("Sync task failed due to failed quorum, task number: " + (task_num + 1));
			syncFail++;
			readyMachines.clear();
		}
	}

	// if quorum successful go ahead and run sync task
	public static void scheduleSync(List<Device> runDevices, List<Device> noneDevices, int task_num)
			throws IOException {

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

		if (checkSyncSuccess()) {
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
		else {
			m.increaseTime(runtime);
			// JNode.setTime(m.getTime());
			if (task_type == 5)
				JNode.increaseTime(STATUS_PROCESSING_TIME);
			else if (task_type == 6)
				JNode.increaseTime(QUORUM_PROCESSING_TIME);
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

	// check if sync task passed
	public static boolean checkSyncSuccess() throws FileNotFoundException, IOException {
		List<Cluster> syncMap = new ArrayList<Cluster>();
		//int success = 0;
		// get list of clusters
		syncMap = getSyncMachines(getCurrentTimePoint());
		List<Device> tempDevices = new ArrayList<Device>();
		for (int f = 0; f < syncMap.size(); f++) {
			syncMap.get(f).sortByTime();
			tempDevices.addAll(syncMap.get(f).currentList);
		}

		List<Cluster> successCount = new ArrayList<Cluster>();
		for (int i = 0; i < readyMachines.size(); i++) {
			for (int j = 0; j < tempDevices.size(); j++) {
				if (readyMachines.get(i) == tempDevices.get(j)) {
					successCount.add(readyMachines.get(i).getCluster());
					//success++;
				}
			}
		}
		System.out.println("Success count is: " + successCount.size());
		Set<Cluster> clusterSet = new HashSet<Cluster>(successCount);
		System.out.println("Successful count is: " + clusterSet.size() + " while expected count is: " + numClusters);
		if (clusterSet.size() == numClusters) {
			return true;
		} else {
			return false;
		}
	}

	public static int getCurrentTimePoint() {
		val = (int) (Math.max(getMax(), JNode.time) / RESIDENCE_TIME);
		if (val > time_point) {
			time_point = val;
		}
		return time_point;

	}

	public static double getGaussian(double aMean) {
		return aMean + RANDOM.nextGaussian() * VARIANCE;
	}

	// get the maximum of the
	public static double getReadyMax() {
		double max = 0;
		// System.out.println("Total machines = " + availMachineMap.getSize());
		for (int i = 0; i < readyMachines.size(); i++) {
			// System.out.println("machine time = " +
			// availMachineMap.getMachine(i).getTime());
			max = Math.max(max, readyMachines.get(i).getTime());
		}
		return max;
	}

	public static void executeDynaSchedule() throws IOException {
		getTasks();
		// loop through task graph in custom map
		for (int i = 0; i < dynamicInputMap.size(); i++) {
			// check if task is local task
			if (dynamicInputMap.getKey(i) == TAG_C_LOCAL) {
				List<Device> noneDevices = new ArrayList<Device>();
				noneDevices = getAsyncMachines(getCurrentTimePoint());
				availMachineMap.sortByTime();
				schdAsyncOrLocalC(i);
				dynamic_revised_available_time += dynamicInputMap.getValue(i) + 10;

				for (int u = 0; u < noneDevices.size(); u++) {
					noneDevices.get(u).increaseTime(dynamicInputMap.getValue(i));
				}
			}

			// check if task is asynchronous task
			else if (dynamicInputMap.getKey(i) == TAG_C_ASYNC) {
				List<Device> noneDevices = new ArrayList<Device>();
				// sort machine times
				noneDevices = getAsyncMachines(getCurrentTimePoint());
				availMachineMap.sortByTime();
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
				schdAsyncOrLocalC(i);
				dynamic_revised_available_time += dynamicInputMap.getValue(i) + COMMUNICATION_TIME + 10;

				for (int u = 0; u < noneDevices.size(); u++) {
					noneDevices.get(u).increaseTime(dynamicInputMap.getValue(i) + COMMUNICATION_TIME);
				}
			}

			// check if task is C sync task
			else if (dynamicInputMap.getKey(i) == TAG_C_SYNC) {
				sync_counter++;
				List<Cluster> syncMap = new ArrayList<Cluster>();
				List<Device> noneDevices = new ArrayList<Device>();
				JNode.setTime(Math.max(getMax(), JNode.getTime()));
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
		System.out.println("Total time: " + getMax());
		System.out.println("Failed sync tasks: " + syncFail);
		System.out.println(
				"============================================================================================");
		System.out.println("End of dynamic scheduling report");
		System.out.println(
				"============================================================================================");

		// fileWriter.write(df.format(getMax()) + " " + df.format(sync_delay) + " "
		// + (total_quorum_attempts + sync_task_counter) + " " + failedSyncTaskCounter +
		// "\n");
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		initClusterList = new ArrayList<Cluster>();
		streamMap = new Util.StreamMap();
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
	}

}
