package sim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scheduler extends Items{
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
	public static void syncAvailUpdate(List<Cluster> clusterMachineMap, int task_num) throws IOException {
		quorumCount = 0;
		numClusters = clusterMachineMap.size();
		System.out.println("Total number of clusters is: " + numClusters);
		dynamic_revised_available_time = JNode.time;
		for (int f = 0; f < clusterMachineMap.size(); f++) {
			List<Device> tempDevices = new ArrayList<Device>();
			clusterMachineMap.get(f).sortByTime();
			tempDevices = clusterMachineMap.get(f).tempoList;

			System.out.println("Running update task");

			boolean counted = true;
			// available machines send update to J-machine
			for (int j = 0; j < tempDevices.size(); j++) {
				Device dev = tempDevices.get(j);
				
				if (dev.getTime() <= dynamic_revised_available_time) {
					if (counted) {
						quorumCount++;
						counted = false;
					}
					System.out.println(dev.getTime() + " was compared to " + dynamic_revised_available_time);
					// update fog time -- thread
					JNode.setTime(Math.max(dev.getTime(), JNode.getTime()));
					// run update sending task
					runSchedule(dev, TAG_UPDATE, STATUS_SENDING_TIME); // ******************
					// quorumCount++;
					readyMachines.add(dev);
				} else {					
					dynamic_revised_available_time += 30;
					if (dev.getTime() <= dynamic_revised_available_time) {
						if (counted) {
							quorumCount++;
							counted = false;
						}
						System.out.println(dev.getTime() + " was compared to " + dynamic_revised_available_time);
						// update fog time -- thread
						JNode.setTime(Math.max(dev.getTime(), JNode.getTime()));
						// run update sending task
						runSchedule(dev, TAG_UPDATE, STATUS_SENDING_TIME); // ******************
						// quorumCount++;
						readyMachines.add(dev);
					}else
						System.out.println(dev.getTime() + " is greater than " + dynamic_revised_available_time);
				}
				
			}
			tempDevices.clear();
		}
		System.out.println("Fog time is: " + JNode.getTime());

		// update device time to latest finish
		for (int h = 0; h < readyMachines.size(); h++) {
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
		}
		checkQuorum(readyMachines, task_num);

	}

	// check if at least a device per cluster sent update message
	public static void checkQuorum(List<Device> readyDevices, int task_num) throws IOException {

		// run quorum for ready machines
		for (int g = 0; g < readyDevices.size(); g++) {
			// schedule quorum task
			runSchedule(readyDevices.get(g), TAG_QUORUM, QUORUM_TIME);
		}
		//update all machine time?????? WHY THIS????
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

		if (isQuorum()) {
			scheduleSync(readyDevices, task_num);
		} else {
			System.out.println("Sync task failed due to failed quorum, task number: " + task_num);
			readyMachines.clear();
		}
	}

	// if quorum successful go ahead and run sync task
	public static void scheduleSync(List<Device> runDevices, int task_num) throws IOException {

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
		else
			return false;
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
			m.increaseTime(runtime);
			System.out.println("JNode is at time: " + JNode.getTime());
		}
		// update J-machine time and save to file
		else {
			m.increaseTime(runtime);
			//JNode.setTime(m.getTime());
			if (task_type == 5)
				JNode.increaseTime(STATUS_PROCESSING_TIME);
			else if (task_type == 6)
				JNode.increaseTime(QUORUM_PROCESSING_TIME);
		}
	}

	public static void updateTimeAndOutput(String taskType, Device m, double k) throws IOException {
		System.out.println(taskType + " was successfully scheduled" );
		m.increaseTime(k);
	}

	// check if sync task passed
	public static boolean checkSyncSuccess() throws FileNotFoundException, IOException {
		List<Cluster> syncMap = new ArrayList<Cluster>();
		// get list of clusters
		syncMap = Initiator.getSyncMachines(time_point);
		List<Device> tempDevices = new ArrayList<Device>();
		for (int f = 0; f < syncMap.size(); f++) {		
			syncMap.get(f).sortByTime();
			tempDevices = syncMap.get(f).tempoList;
		}
		
		Set<Cluster> successCount = new HashSet<Cluster>();
		for (int i = 0; i < readyMachines.size(); i++) {
			for (int j = 0; j < tempDevices.size(); j++) {
				if (readyMachines.get(i) == tempDevices.get(j)) {
					successCount.add(readyMachines.get(i).getCluster());
				}
			}
		}
		if (successCount.size() == numClusters) {
			return true;
		} else {
			return false;
		}
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

}
