package sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

//represents the fog class.
public class Cluster {
	String name;
	Double time;
	Boolean assigned;
	List<Device> deviceList; // contains devices in a cluster
	List<Device> tempoList; 
	List<Device> currentList; 
	// contains devices selected for video capture (at most two)
	List<Device> selectedDeviceList;
	List<Device> unSelectedDeviceList;
	List<Cluster> neighbourList;
	static final Random RANDOM = new Random();
	private static Set<Integer> set;

	// constructor with name
	Cluster(String tName) {
		deviceList = new ArrayList<Device>();
		tempoList = new ArrayList<Device>();
		currentList = new ArrayList<Device>();
		set = new HashSet<Integer>();
		selectedDeviceList = new ArrayList<Device>();
		unSelectedDeviceList = new ArrayList<Device>();
		neighbourList = new ArrayList<Cluster>();
		name = tName;
		// t = new Thread(this, name);
		// t.start();
	}

	// add the fog the cluster is attached to
	public void addController(Device controller) {
		deviceList.add(0, controller);
	}

	// register a device to a cluster
	public void addDevice(Device d) {
		deviceList.add(d);
	}

	// remove a device from a cluster
	public void removeDevice(Device d) {
		deviceList.remove(d);
	}

	// check if a device is in a cluster
	public boolean findDevice(Device d) {
		if (deviceList.contains(d)) {
			return true;
		} else
			return false;
	}

	public List<Device> getDevices() {
		return deviceList;
	}

	// get the num of devices in cluster at time point
	public int numOfDevices() {
		return deviceList.size();
	}

	public void addNeighbours(Cluster... clusters) {
		for (Cluster cl : clusters) {
			neighbourList.add(cl);
		}
	}

	public void setAssigned(boolean bool) {
		assigned = bool;
	}

	public boolean isAssigned() {
		return assigned;
	}

	// select random devices from the list of devices to remain in or leave grid
	public List<Device> getSelectedDevices(int max) {
		selectedDeviceList.clear();
		unSelectedDeviceList.clear();
		// System.out.println("Min is 0; Max is: " + max);
		while (set.size() < max) {
			set.add(getRandomNumberInRange(0, max - 1));
		}
		//System.out.println("Size of set is: " + set.size());
		// add selected devices to selected list
		//System.out.println(this.name + " Size of deviceList is: " + numOfDevices());
		if (numOfDevices() > 0) {
			for (Integer s : set) {
				selectedDeviceList.add(deviceList.get(s));
			}
		}
		
		unSelectedDeviceList = returnListDifference(deviceList, selectedDeviceList);
		deviceList = selectedDeviceList;
		set.clear();
		return selectedDeviceList;
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

	// returns a random number between min and max
	public static int getRandomNumberInRange(int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		return RANDOM.nextInt((max - min) + 1) + min;
	}

	public void sortByTime() {
		Collections.sort(deviceList, new Comparator<Device>() {

			public int compare(Device m1, Device m2) {
				return m1.getTime().compareTo(m2.getTime());
			}
		});
	}
}
