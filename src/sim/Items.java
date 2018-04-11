package sim;

import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
	public static int counter = 0; // counter for no of loops
	public static int dynamic_counter = 0; // dynamic counter
	public static int dynamicSyncCounter = 0; // dynamic sync counter
	public static int quorum_attempts = 0; // number of times quorum has been attempted
	public static int total_quorum_attempts = 0; // number of times quorum has
	public static int val = 0;
	public static int time_point = 0;
													// been
	public static double dynamic_revised_available_time;
	public static int failedSyncTaskCounter = 0;
	public static int sync_task_counter = 0;

	public static int failed_machines_counter = 0;
	public static int joined_machines_counter = 0;

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
	
	
	public static FileWriter traceWriter ;

	public static int sync_counter = 0; //count no of synchronizations
	static final int TIMEPOINT = 48;
	
	//number of devices initialized at startup
	public static final int INIT_DEVICES = 400; 
	public static final int MIN_CLUSTER_SIZE = 2;
	
	public static final double COMMUNICATION_TIME = 20.0;
	static final double RESIDENCE_TIME = 1000;
	public static final double QUORUM_TIME = 30.0;
	
	//custom map for reading input task graph for dynamic schedule
	public static Util.SimpleMap dynamicInputMap; 
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
	
	
	public static String INPUTFILE = "tg1.txt";
	public static String OUTFILE = ""; 
	public static final double VARIANCE = 10; // variance and quorum delay
	public static final double STATUS_PROCESSING_TIME = 2;
	public static final double QUORUM_PROCESSING_TIME = 2;
	public static final double STATUS_SENDING_TIME = 30.0; 
	public static final int RUNS = 1;
	
	
	//.............................................................//
}

