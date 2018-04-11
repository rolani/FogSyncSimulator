package sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Util {
	

	public static class SimpleMap {

		private List<Integer> keys;
		private List<Double> values;

		double a, b;

		public SimpleMap() {
			keys = new ArrayList<Integer>();
			values = new ArrayList<Double>();
		}

		
		public void insert(int key, double v1) {
			keys.add(key);
			values.add(v1);
		}

		public int getKey(int index) {
			if (index >= 0)
				return (int) keys.get(index);
			else
				return 0;
		}


		public double getValue(int index) {
			return (double) values.get(index);
		}

		public int size() {
			return keys.size();
		}
		
		public void clear() {
			keys.clear();
			values.clear();
		}
		
		public void removeFirst() {
			keys.remove(0);
			values.remove(0);
		}
	}

	public static class MachineMap<M> {
		// private Device machine;
		private List<Device> machineList;
		//private List<Device> sortedList;

		public MachineMap() {
			machineList = new ArrayList<Device>();
			Collections.sort(machineList, new Comparator<Device>() {

				public int compare(Device m1, Device m2) {
					return m1.getTime().compareTo(m2.getTime());
				}
			});
		}

		public void putMachine(Device m) {
			machineList.add(m);
		}
		
		public void clear() {
			machineList.clear();
		}

		public Device getMachine(int index) {
			return machineList.get(index);
		}

		public void removeMachine(Device m) {
			machineList.remove(m);
		}
		
		public void removeAllMachines() {
			machineList.clear();
		}

		public String getName(int index) {
			Device m = getMachine(index);
			return m.name.toString();
		}

		public int getSize() {
			return machineList.size();
		}

		public void sortByTime() {
			Collections.sort(machineList, new Comparator<Device>() {

				public int compare(Device m1, Device m2) {
					return m1.getTime().compareTo(m2.getTime());
				}
			});
		}
	}
	
	public static class NewMap {
		private List<Integer> time_id, xAxis, yAxis;
		private List<Double>mean, std;
		
		public NewMap() {
			xAxis = new ArrayList<Integer>();
			yAxis = new ArrayList<Integer>(); 
			time_id = new ArrayList<Integer>();
			mean = new ArrayList<Double>();
			std = new ArrayList<Double>();
		}

		public void addValue(int id, int a, int b, double ave, double d) {
			xAxis.add(a);
			yAxis.add(b);
			time_id.add(id);
			mean.add(ave);
			std.add(d);
		}

		public int getyAxis(int index) {
			return yAxis.get(index);
		}

		public int getxAxis(int index) {
			return xAxis.get(index);
		}
		
		public int getId(int index) {
			return time_id.get(index);
		}
		
		public double getMean(int index) {
			return mean.get(index);
		}
		
		public double getStd(int index) {
			return std.get(index);
		}
		
		public int getSize(){
			return time_id.size();
		}
	}
	
	public static class Mapper {
		private List<Device> devices;
		private List<List<Cluster>> clusters;
		
		public Mapper() {
			devices = new ArrayList<Device>();
			clusters = new ArrayList<List<Cluster>>(); 
		}
		
		public void addItem(Device d, List<Cluster> list) {
			//if (devices.size() <= Items.TIMEPOINT) {
				devices.add(d);
				clusters.add(list);
			//}
		}
		
		public int getDeviceIndex(Device d) {
			return devices.indexOf(d);
		}
		
		public int getSize() {
			return devices.size();
		}
		
		public int clusterSize(int index) {
			return clusters.get(index).size();
		}
		
		public Device getDevice(int index) {
			return devices.get(index);
		}
		
		public List<Cluster> getCluster(int index) {
			return clusters.get(index);
		}
		
		public Cluster getCluster(Device d, int time_point) {
			List<Cluster> temp = clusters.get(getDeviceIndex(d));
			return temp.get(time_point);
		}
	}
	
	public static class StreamMap {
		public Mapper clusterList; 
		
		public StreamMap() {
			clusterList = new Mapper();
		}
		//add a device and cluster evolution over 48 time points
		public void addSeries(Device d, List<Cluster> list) {
			clusterList.addItem(d, list);
		}
		
		public Cluster getClusterAtTimePoint(Device d, int time_point) {
			return clusterList.getCluster(d, time_point);
		}
		
		public int getSize() {
			return clusterList.getSize();
		}
		
		public void print() {
			for (int i = 0; i < clusterList.getSize(); i++) {
				System.out.print(clusterList.getDevice(i).name + ": {");
				for (Cluster c: clusterList.getCluster(i)) {
					System.out.print(c.name + ";");					
				}				
				System.out.print("}... Total is: " + clusterList.clusterSize(i) + "\n ");
			}
		}		
	}
}
