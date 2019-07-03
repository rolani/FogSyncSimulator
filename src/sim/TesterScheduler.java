package sim;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

public class TesterScheduler extends Item {
	private static FileWriter fileWriter;
	static DecimalFormat df;
	static Machine JNode; // J-machine

	// counter for no of J-update points
	static int quorumCount = 0; // counter to check for quorum
	public static int counter = 0; // counter for no of loops
	public static int static_counter = 0; // static counter
	public static int failedSyncTaskCounter = 0; // counter for failed sync
													// tasks
	public static int total_quorum_attempts = 0; // number of times quorum has
													// been
	// tried
	public static int quorum_attempts = 0; // number of times quorum is
											// attempted
	public static int failed_machines_counter = 0;
	public static int joined_machines_counter = 0;

	public static double DECISION = 0.5; // decision variable to decide whether
											// to fail or join
	public static int JOIN_ID; // id for joining nodes
	
	public static double interim = 0.0;
	// measured execution times
	public static double static_predicted_exec_time = 0.0; // predicted
															// execution time
															// for static
															// scheduler
	public static int sync_task_counter = 0; // count number of sync tasks
	public static double sync_delay = 0.0; // synchronization delay
	// public static double sync_delayP = 0.0; // synchronization delay per run
	public static double totalStaticTime = 0.0; // total execution time
	// public static double totalStaticTimeP = 0.0; // total execution time per
	// run
	public static int MULTIPLIER;

	// shell parameters
	public static int SHELL_NO_OF_MACHINES, QUORUM_RETRIES; // number of
															// machines and
															// quorum retries
	public static String INPUTFILE; // input file
	public static String OUTFILE; // output files
	public static double VARIANCE, LAMBDA,  STATUS_PROCESSING_TIME; // variance and quorum delay and multiplier for task accuracy

	// create C-machines
	public static void createMachines() throws IOException {
		for (int i = 0; i < SHELL_NO_OF_MACHINES; i++) {
			String name = "C" + (i + 1);
			Machine machine = new Machine(name);
			machineMap.putMachine(machine); // holds all created machines
			availMachineMap.putMachine(machine); // holds machines currently in
													// the system
		}
		System.out.println("Total machines =  " + machineMap.getSize());
		System.out.println("Creating machines.... ");
		for (int j = 0; j < machineMap.getSize(); j++) {
			System.out.println("Machine " + machineMap.getName(j) + " has been created");
			traceWriter.write("Machine " + machineMap.getName(j) + " has been created" + "\n");
		}

		JOIN_ID = SHELL_NO_OF_MACHINES;

	}

	// get tasks from file into custom map
	public static void inputReader() throws FileNotFoundException, IOException {
		staticInputMap = new Utility.SimpleMap(); // holds tasks for static
												// scheduling
		dynamicInputMap = new Utility.SimpleMap(); // holds tasks for dynamic
												// scheduling
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
				double time = Double.parseDouble(splits[1]); // mean execution
																// time
				// put execution times tag and task group in map for static
				staticInputMap.insert(tag, time);

			}
			// close reader
			reader.close();
		}
	}

	public static void runAsyncOrLocal(int i) throws IOException {

		// System.out.println("Predicted start time is: " +
		// static_predicted_exec_time);
		System.out.println("Running task: " + (i + 1));
		
		// loop through machine list
		for (int k = 0; k < availMachineMap.getSize(); k++) {

			double runtime = 0;
			// generate heterogeneous runtime based on normal distribution
			runtime = getGaussian(staticInputMap.getValue(i), MULTIPLIER);
			if (staticInputMap.getKey(i) == TAG_C_ASYNC) {
				availMachineMap.getMachine(k).increaseTime(COMMUNICATION_TIME);
			}
			// schedule local task
			finalSchedule(availMachineMap.getMachine(k), staticInputMap.getKey(i), runtime);

		}
		if (staticInputMap.getKey(i) == TAG_C_ASYNC) {
			static_predicted_exec_time += staticInputMap.getValue(i) + COMMUNICATION_TIME;
		}else {
			static_predicted_exec_time += staticInputMap.getValue(i);
		}
		// static_predicted_exec_time += staticInputMap.getValue(i) + VARIANCE;
		System.out.println("Predicted time is updated to: " + static_predicted_exec_time );
		traceWriter.write(
				"Predicted time is updated to: " + static_predicted_exec_time +  "\n");
		
		//static_predicted_exec_time = getMax();
		// VARIANCE);
		
	}

	public static void statusUpdate(int i) throws IOException {
		quorumCount = 0;

		// clear ready machine list for next iteration
		readyMachines.clear();

		// sort machines based on time
		availMachineMap.sortByTime();

		// System.out.println("Predicted update start time is: " +
		// static_predicted_exec_time);

		System.out.println("Running JAsync task: " + (i + 1));
		
		// available machines send update to J-machine
		for (int j = 0; j < availMachineMap.getSize(); j++) {

			if (availMachineMap.getMachine(j).getTime() <= static_predicted_exec_time) {
				System.out.println(
						availMachineMap.getMachine(j).getTime() + " was compared to " + static_predicted_exec_time);
				traceWriter.write(availMachineMap.getMachine(j).getTime() + " was compared to "
						+ static_predicted_exec_time + "\n");

				// update machine time -- thread
				JNode.setTime(Math.max(availMachineMap.getMachine(j).getTime(), JNode.getTime()));
				finalSchedule(availMachineMap.getMachine(j), TAG_J_ASYNC, STATUS_SENDING_TIME);
				quorumCount++;
				readyMachines.add(availMachineMap.getMachine(j));
			} else {
				System.out.println(
						availMachineMap.getMachine(j).getTime() + " is greater than " + static_predicted_exec_time);
				traceWriter.write(availMachineMap.getMachine(j).getTime() + " is greater than "
						+ static_predicted_exec_time + "\n");
			}

		}
		JNode.setTime(Math.max(availMachineMap.getMachine(availMachineMap.getSize()-1).getTime(), JNode.getTime()));
		interim =  static_predicted_exec_time + (readyMachines.size() * STATUS_PROCESSING_TIME);
		System.out.println("Interim is: " + interim);
		traceWriter.write("Interim is: " + interim +"\n");
		
		System.out.println("JNode time is: " + JNode.getTime());
		traceWriter.write("JNode time is: " + JNode.getTime()+"\n");
		// static_predicted_exec_time += (readyMachines.size() *
		// STATUS_PROCESSING_TIME);
		static_predicted_exec_time += STATUS_SENDING_TIME;
		traceWriter.write("Predicted time is updated to: " + static_predicted_exec_time + " + " + STATUS_SENDING_TIME
				+ " = " + (static_predicted_exec_time + STATUS_SENDING_TIME) + "\n");
		System.out.println("Predicted time is updated to: " + static_predicted_exec_time + " + " + STATUS_SENDING_TIME
				+ " = " + (static_predicted_exec_time + STATUS_SENDING_TIME));
		
	}

	// check if quorum successful or not
	public static void tryQuorum(int i) throws IOException {
		static_monitor = getEarliestReadyMachine(); // earliest available
													// machine for
		if (static_predicted_exec_time < interim) {
			System.out.println("I did this comparison with interim");
			static_predicted_exec_time = interim;
		}
		// System.out.println("Predicted quorumm start time is: " +
		// static_predicted_exec_time);
		// update machine times before quorum
		System.out.println("Updating ready machine time to :" + Math.max(getReadyMax(), JNode.getTime()));
		traceWriter.write("Updating ready machine time to max of:" + getReadyMax() + " or " + JNode.getTime() + "\n");
		for (int h = 0; h < readyMachines.size(); h++) {
			// schedule quorum task
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
			
		}
		
		for (int j = 0; j < availMachineMap.getSize(); j++) {
			if (availMachineMap.getMachine(j).getTime() <= getReadyMax()) {
				availMachineMap.getMachine(j).setTime(getReadyMax());
			}
		}
		
		
		// run quorum for ready machines
		for (int g = 0; g < readyMachines.size(); g++) {
			// schedule quorum task
			finalSchedule(readyMachines.get(g), TAG_QUORUM, QUORUM_TIME);
		}
		System.out.println("JNode time is: " + JNode.getTime());
		traceWriter.write("JNode time is: " + JNode.getTime()+"\n");
		System.out.println("Updating ready machine time to :" + Math.max(getReadyMax(), JNode.getTime()));
		traceWriter.write("Updating ready machine time to max of:" + getReadyMax() + " or " + JNode.getTime() + "\n");
		for (int h = 0; h < readyMachines.size(); h++) {
			// schedule quorum task
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
			
		}

		for (int j = 0; j < availMachineMap.getSize(); j++) {
			if (availMachineMap.getMachine(j).getTime() <= getReadyMax()) {
				availMachineMap.getMachine(j).setTime(getReadyMax());
			}
		}
		traceWriter.write("Predicted time is updated to: " + static_predicted_exec_time + " + " + QUORUM_TIME + " = "
				+ (static_predicted_exec_time + QUORUM_TIME) + "\n");
		System.out.println("Predicted time is updated to: " + static_predicted_exec_time + " + " + QUORUM_TIME + " = "
				+ (static_predicted_exec_time + QUORUM_TIME));
		
		System.out.println("quorum count: " + quorumCount);
		traceWriter.write("quorum count: " + quorumCount + "\n");
		static_predicted_exec_time += QUORUM_TIME;
		if (quorum_attempts < QUORUM_RETRIES) {
			if (checkQuorum(availMachineMap, quorumCount) == true) {
				runQuorum(i);
			} else if (checkQuorum(availMachineMap, quorumCount) == false) {
				static_predicted_exec_time += LAMBDA;
				traceWriter.write("Predicted time is updated to: " + static_predicted_exec_time + " + " + LAMBDA + " = "
						+ (static_predicted_exec_time + LAMBDA) + "\n");
				System.out.println("Predicted time is updated to: " + static_predicted_exec_time + " + " + LAMBDA
						+ " = " + (static_predicted_exec_time + LAMBDA));
				//static_predicted_exec_time = getMax() + LAMBDA;
				
				retryQuorum(i);
			}
		} else {
			traceWriter.write("Sync task failed!!! continue to next task" + "\n");
			System.out.println("Sync task failed!!! continue to next task");
			failedSyncTaskCounter++;
			traceWriter.write("No of failed sync tasks is: " + failedSyncTaskCounter + "\n");
			System.out.println("No of failed sync tasks is: " + failedSyncTaskCounter);
		}

	}

	// if quorum successful go ahead and run sync task
	public static void runQuorum(int i) throws IOException {
		double runtime;
		System.out.println("Quorum successful!!!!!");
		traceWriter.write("Quorum successful!!!!!" + "\n");

		// sync delay for task
		sync_delay += (readyMachines.get(readyMachines.size() - 1).getTime() - static_monitor);
		System.out.println("Monitored delay is " + readyMachines.get(readyMachines.size() - 1).getTime() + " - "
				+ static_monitor + " = " + df.format(sync_delay));
		traceWriter.write("Monitored delay is " + df.format(sync_delay) + "\n");
		// System.out.println("Predicted time is: " +
		// static_predicted_exec_time);
		System.out.println("Running Sync task: " + (i + 1));
		//update time
		System.out.println("Updating ready machine time to :" + Math.max(getReadyMax(), JNode.getTime()));
		traceWriter.write("Updating ready machine time to max of:" + getReadyMax() + " or " + JNode.getTime() + "\n");
		for (int h = 0; h < readyMachines.size(); h++) {
			// schedule quorum task
			readyMachines.get(h).setTime(Math.max(getReadyMax(), JNode.getTime()));
			
		}
		
		// schedule sync task
		for (int z = 0; z < readyMachines.size(); z++) {
			readyMachines.get(z).increaseTime(COMMUNICATION_TIME);
			runtime  =  getGaussian(staticInputMap.getValue(i),MULTIPLIER);
			// ready machines should be in sync at this point
			finalSchedule(readyMachines.get(z), TAG_C_SYNC, runtime);
		}
		static_predicted_exec_time += staticInputMap.getValue(i) + COMMUNICATION_TIME;
		traceWriter.write(
				"Predicted time is updated to: " + static_predicted_exec_time + "\n");

		System.out.println("Predicted time is updated to: " + static_predicted_exec_time );
		
		//static_predicted_exec_time = getMax();
		// static_predicted_exec_time += (staticInputMap.getValue(i) +
		// VARIANCE);

	}

	public static void retryQuorum(int i) throws IOException {
		System.out.println("Retrying quorum no: " + (quorum_attempts + 1));
		quorum_attempts++; // increment attempts
		total_quorum_attempts++; // total quorum attempts
		statusUpdate(i);
		tryQuorum(i);

	}

	// static schedule algorithm implementation
	public static void executeStaticSchedule() throws IOException {

		// loop through task graph in custom map
		for (int i = 0; i < staticInputMap.size(); i++) {
			randomJoin(); // add new machines randomly

			// check if task is local task
			if (staticInputMap.getKey(i) == TAG_C_LOCAL) {
				runAsyncOrLocal(i);
			}

			// check if task is asynchronous task
			else if (staticInputMap.getKey(i) == TAG_C_ASYNC) {
				// update machine times
				availMachineMap.sortByTime();
				//JNode.setTime(availMachineMap.getMachine(0).getTime());
				runAsyncOrLocal(i);
			}

			// check if task is status update task
			else if (staticInputMap.getKey(i) == TAG_J_ASYNC) {
				statusUpdate(i);
			}

			// check if task is C sync task
			else if (staticInputMap.getKey(i) == TAG_C_SYNC) {

				quorum_attempts = 0;
				sync_task_counter++; // sync task counter
				// check to see if quorum was successful based on updates sent
				// earlier
				tryQuorum(i);

				// update all machines to current point
				for (int k = 0; k < availMachineMap.getSize(); k++) {
					availMachineMap.getMachine(k).setTime(Math.max(getMax(), JNode.getTime()));
				}
				// update JNode time
				JNode.setTime(Math.max(getMax(), JNode.getTime()));

				// clear ready machine list for next iteration
				readyMachines.clear();

				randomFail(); // fail machines randomly

			}

			// just to keep track of quorum in prevTime Map
			else {

			}
			// control no of continuous runs
			if (i == staticInputMap.size() - 1 && counter < (RUNS - 1)) {
				i = -1;
				// System.out.println("Current run is: " + counter);
				System.out.println("Ending run: " + (counter + 1));
				traceWriter.write("Ending run: " + (counter + 1) + "\n");
				System.out.println("Starting run: " + (counter + 2));
				traceWriter.write("Starting run: " + (counter + 2) + "\n");
				counter++;

				for (int j = 0; j < unAvailMachineMap.getSize(); j++) {
					System.out.println("Machine joined: " + unAvailMachineMap.getMachine(j).name);
					traceWriter.write("Machine joined: " + unAvailMachineMap.getMachine(j).name + "\n");
					unAvailMachineMap.getMachine(j).setTime(availMachineMap.getMachine(0).getTime());
					availMachineMap.putMachine(unAvailMachineMap.getMachine(j));
					joined_machines_counter++;
				}
				unAvailMachineMap.removeAllMachines();
			}
		}
		totalStaticTime = getMax();
	}

	// generate the initial schedule
	public static void finalSchedule(Machine m, int i, double k) throws IOException {

		String taskType = null;
		switch (i) {
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
			taskType = "J-update";
			break;
		case 5:
			taskType = "Quorum task";
			break;
		}

		// update C-machine time and save to file
		if (i == 1 || i == 2 || i == 3 || i == 5) {
			updateTimeAndOutput(taskType, m, k);
		}

		// update J-machine time and save to file
		else {
			System.out.println(taskType + " was scheduled at: " + df.format(m.getTime()) + " by: " + m.name + " for: "
					+ k + " and ended: " + df.format(m.getTime() + k) + " JNode is at:" + (JNode.getTime() + k));
			traceWriter.write(taskType + " was scheduled at: " + df.format(JNode.getTime()) + " by: " + m.name
					+ " for: " + k + " and ended: " + df.format(JNode.getTime() + k)  + " JNode is at:" + (JNode.getTime() + k) +"\n");

			m.increaseTime(k);
			JNode.increaseTime(STATUS_PROCESSING_TIME);
		}
	}

	public static void updateTimeAndOutput(String taskType, Machine m, double k) throws IOException {
		System.out.println(taskType + " was scheduled on: " + m.name + " at: " + df.format(m.getTime()) + " for: " + k
				+ " and ended: " + df.format(m.getTime() + k));
		traceWriter.write(taskType + " was scheduled on: " + m.name + " at: " + df.format(m.getTime()) + " for: " + k
				+ " and ended: " + df.format(m.getTime() + k) + "\n");

		m.increaseTime(k);
		// static_predicted_exec_time += k;
	}

	// randomly disable some machines to simulate machine failure.
	public static void randomFail() throws IOException {

		// loop through
		for (int j = 0; j < availMachineMap.getSize(); j++) {
			if (Math.random() < DECISION) {
				// let failure occur for each machine at a probability of 0.1
				if (Math.random() < 0.05) {
					Machine mach = availMachineMap.getMachine(j);
					System.out.println("Machine failed: " + mach.name);
					traceWriter.write("Machine failed: " + mach.name + "\n");
					failed_machines_counter++;
					// remove machine from available list
					availMachineMap.removeMachine(mach);
				}
			}
		}
	}

	// randomly make machines join
	public static void randomJoin() throws IOException {
		// decision to wake machines or not
		if (Math.random() > DECISION) {
			// loop through
			// let new machine join at a probability of 0.1
			if (Math.random() < 0.5) {
				JOIN_ID++;
				String name = "C" + JOIN_ID;
				Machine machine = new Machine(name);

				// remove machine from available list
				unAvailMachineMap.putMachine(machine);
			}
		}
	}

	public static double getEarliestReadyMachine() {
		double min = 0;
		double m = 0;
		int t = 1;

		// get minimum time from ready machines
		for (int h = 0; h < readyMachines.size(); h++) {
			min = readyMachines.get(h).getTime();
			if (t <= 1) {
				m = min;
				t++;
			} else {
				if (min < m) {
					m = min;
				}
			}
		}
		return m;
	}

	// get the maximum of the
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

	// check for quorum by comparing size of machines with update count
	public static boolean checkQuorum(Utility.MachineMap<Machine> map1, int cnt) {
		if ((double) (map1.getSize() * DEGREE) <= (double) cnt)
			return true;
		else
			return false;
	}

	public static Random fRandom = new Random();

	public static double getGaussian(double aMean, int opt) {
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
		return time;
	}

	public static void runStatic() throws IOException {
		// create machines
		createMachines();
		// read input file into custom map
		inputReader();
		// run static scheduler
		executeStaticSchedule();
		// generate output
		generateStaticOutput();
	}

	// generate final output and trace
	public static void generateStaticOutput() throws IOException {

		System.out.println(
				"========================================================================================================");
		System.out.println("Starting static scheduling report...................... ");
		System.out.println(
				"========================================================================================================");
		System.out.println("Finish time: " + totalStaticTime);
		System.out.println("No of quorum attempts: " + (total_quorum_attempts + sync_task_counter));
		System.out.println("No of machines that started: " + SHELL_NO_OF_MACHINES);
		System.out.println("No of machines that ended: " + availMachineMap.getSize());
		System.out.println("No of failed machines: " + failed_machines_counter);
		System.out.println("No of machines that joined: " + joined_machines_counter);
		System.out.println("No of runs: " + RUNS);
		System.out.println("No of tasks: " + staticInputMap.size());
		System.out.println("Sync degree: " + DEGREE);
		System.out.println("Total delay: " + df.format(sync_delay));
		System.out.println("Total sync tasks: " + df.format(sync_task_counter));
		System.out.println("Failed sync tasks: " + failedSyncTaskCounter);
		System.out.println(
				"========================================================================================================");
		System.out.println("End of static scheduling report");
		System.out.println(
				"========================================================================================================");

		traceWriter.write("Finish time: " + totalStaticTime + "\n");
		traceWriter.write("No of quorum attempts: " + (total_quorum_attempts + sync_task_counter) + "\n");
		traceWriter.write("No of machines that started: " + SHELL_NO_OF_MACHINES + "\n");
		traceWriter.write("No of machines that ended: " + availMachineMap.getSize() + "\n");
		traceWriter.write("No of failed machines: " + failed_machines_counter + "\n");
		traceWriter.write("No of machines that joined: " + joined_machines_counter + "\n");
		traceWriter.write("No of runs: " + RUNS + "\n");
		traceWriter.write("No of tasks: " + staticInputMap.size() + "\n");
		traceWriter.write("Sync degree: " + DEGREE + "\n");
		traceWriter.write("Total delay: " + df.format(sync_delay) + "\n");
		traceWriter.write("Total sync tasks: " + df.format(sync_task_counter) + "\n");
		traceWriter.write("Failed sync tasks: " + failedSyncTaskCounter + "\n");

		fileWriter.write(totalStaticTime + " " + sync_delay + " " + (total_quorum_attempts + sync_task_counter) + " "
				+ failedSyncTaskCounter + "\n");

	}

	// main method for running simulation
	public static void main(String[] args) throws FileNotFoundException, IOException {

		try {

			SHELL_NO_OF_MACHINES = Integer.parseInt(args[0]);
			QUORUM_RETRIES = Integer.parseInt(args[1]);			
			LAMBDA = Double.parseDouble(args[2]);
			INPUTFILE = args[3];
			OUTFILE = args[4];	
			STATUS_PROCESSING_TIME = Double.parseDouble(args[5]);
			MULTIPLIER = Integer.parseInt(args[6]);

		} catch (Exception e) {
			System.out.println("Seven arguments needed -- no of machines, quorum retries, status processing time, quorum delay, "
					+ "input file, output file,  and accuracy multiplier");
		}
		
		SHELL_NO_OF_MACHINES = 20;
		QUORUM_RETRIES = 2;
		//VARIANCE = 30;
		LAMBDA = 10;
		INPUTFILE = "tGraph2.txt";
		OUTFILE = "chinaOut.txt";
		STATUS_PROCESSING_TIME = 10;
		MULTIPLIER = 2;
		
		System.out.println("Starting simulation....");

		// create J-machine
		JNode = new Machine("JNode");

		// machine maps
		machineMap = new Utility.MachineMap<Machine>();
		availMachineMap = new Utility.MachineMap<Machine>();
		unAvailMachineMap = new Utility.MachineMap<Machine>();
		readyMachines = new ArrayList<Machine>();

		// writers
		fileWriter = new FileWriter(OUTFILE, true);

		traceWriter = new FileWriter("staticAccTrace.txt", false);
		traceWriter.write("Starting simulation...." + "\n");

		df = new DecimalFormat("#.00");

		runStatic();

		System.out.println("Simulation ended");
		traceWriter.write("Simulation ended" + "\n");

		// close file writers
		traceWriter.close();
		fileWriter.close();
	}

}

