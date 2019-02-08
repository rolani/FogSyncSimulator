package sim;


import java.io.FileWriter;
import java.util.List;

//class to hold simulation parameters
public class Item {
	//tags for identifying tasks
	public static final int TAG_C_ASYNC = 1;
	public static final int TAG_C_SYNC = 2;
	public static final int TAG_C_LOCAL = 3;
	public static final int TAG_J_ASYNC = 4;
	public static final int TAG_QUORUM = 5;
	
	public static final double PROB_FAIL = 0.1; //no of continuous runs
	public static final double QUORUM_TIME = 30.0;
	
	public static FileWriter traceWriter ;

	public static int early_quorum = 0; //counter for early quorum
	public static int sync_counter = 0; //count no of synchronizations
	public static final int RUNS = 1; //

	//public static final double STATUS_PROCESSING_TIME = 20.0;//update processing at JNode
	public static final double STATUS_SENDING_TIME = 30.0; //time to send update from CNode
	public static final double COMMUNICATION_TIME = 20.0;
	public static final double DEGREE = 0.7; //probability of under shoot
	public static double static_monitor = 0.0; //maintains earliest start time of machines before quorum
	public static double dynamic_monitor = 0.0; //maintains earliest start time of machines before quorum
	
	public static Utility.SimpleMap taskMap;//for batch tasks
	public static Utility.SimpleMap staticInputMap; //custom map for reading input task graph for static schedule
	public static Utility.SimpleMap dynamicInputMap; //custom map for reading input task graph for dynamic schedule
	public static Utility.SimpleMap updatedDynamicInputMap; //
	public static Utility.SimpleMap poppedDynamicInputMap;
	public static Utility.MachineMap<Machine> machineMap; //custom map for all machines created
	public static Utility.MachineMap<Machine> markedMap;//holds marked machines
	public static Utility.MachineMap<Machine> availMachineMap; //custom map for available machines excluding failed machines
	public static Utility.MachineMap<Machine> unAvailMachineMap; // custom map for failed machines
	
	
	public static List<Machine> readyMachines; //custom map for quorum machines
	public static List<Machine> readySampleMachines; //custom map for quorum machines
	public static List<Machine> runningDynamicMachines; //custom map for machines that can run task before sync task
	public static List<Machine> pausedDynamicMachines; //custom map for machines that must pause for sync task
	public static List<Double> staticSyncTimes; // list for sync start times
	public static List<Double> staticUpdateTimes; // list for static J-update start times
}






