package sim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Mobility extends Items {
	static List<Cluster> initClusterList;
	static Util.StreamMap streamMap;
	static int sum;
	
	public Mobility() {
		initClusterList = new ArrayList<Cluster>();
		streamMap = new Util.StreamMap();
	}
	
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
		System.out.println("temp[4][9]: " + grid[4][9]);
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
				System.out.println(index + ": " + grid[j][z]);
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
							Device dd = convertSetToList(unusedDevices).get(0);
							dd.addCluster(c);
							tempAssigned.add(dd);
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
					c.setAssigned(true);
				} else {
					c.deviceList.clear();
				}
				index++;
			}
		}
		System.out.println("temp[1][8]: " + temp[8][1]);

		tempDevices = returnListDifference(initMachines, tempAssigned);
		unusedDevices.addAll(tempDevices);
		// unusedMachines.addAll(tempDevices);
		unusedMachines = convertSetToList(unusedDevices);
		sortByName(unusedMachines);
		for (int f = 0; f < unusedMachines.size(); f++) {
			unusedMachines.get(f).addCluster(NONE);
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

	public static Util.StreamMap getMobility() throws FileNotFoundException, IOException {
		createInitDevices();
		System.out.println("Size of initMachine is: " + initMachines.size());
		for (int i = 0; i < initMachines.size(); i++) {
			streamMap.addSeries(initMachines.get(i), initMachines.get(i).clusterList);
		}
		return streamMap;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		initMachines = new ArrayList<Device>();
		assignedMachines = new ArrayList<Device>();
		unusedMachines = new ArrayList<Device>();
		initClusterList = new ArrayList<Cluster>();
		neighbourList = new ArrayList<Device>();
		unusedDevices = new HashSet<Device>();
		streamMap = new Util.StreamMap();
		//createInitDevices();
		getMobility();
	}
}
