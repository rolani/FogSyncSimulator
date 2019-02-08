package sim;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Utility {

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
		// private Machine machine;
		private List<Machine> machineList;
		//private List<Machine> sortedList;

		public MachineMap() {
			machineList = new ArrayList<Machine>();
			Collections.sort(machineList, new Comparator<Machine>() {

				public int compare(Machine m1, Machine m2) {
					return m1.getTime().compareTo(m2.getTime());
				}
			});
		}

		public void putMachine(Machine m) {
			machineList.add(m);
		}

		public Machine getMachine(int index) {
			return machineList.get(index);
		}

		public void removeMachine(Machine m) {
			machineList.remove(m);
		}
		
		public void removeAllMachines() {
			machineList.clear();
		}

		public String getName(int index) {
			Machine m = getMachine(index);
			return m.name.toString();
		}

		public int getSize() {
			return machineList.size();
		}

		public void sortByTime() {
			Collections.sort(machineList, new Comparator<Machine>() {

				public int compare(Machine m1, Machine m2) {
					return m1.getTime().compareTo(m2.getTime());
				}
			});
		}
	}

	public static class NewMap {
		private List<Double> startTimes, endTimes;

		public NewMap() {
			startTimes = new ArrayList<Double>();
			endTimes = new ArrayList<Double>();
		}

		public void addDetails(Double a, Double b) {
			startTimes.add(a);
			endTimes.add(b);
		}

		public double getEndTime(int index) {
			return endTimes.get(index);
		}

		public double getStartTime(int index) {
			return startTimes.get(index);
		}
	}
}
