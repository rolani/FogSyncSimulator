package sim;

import java.util.ArrayList;
import java.util.List;

public class Device implements Runnable {
	String name;
	int d_id;
	Double time;
	Boolean hasJoined = false;
	Cluster c;
	List<Cluster> clusterList;
	static Thread t;
	//constructor with name
	Device(String tName, int id){
		clusterList = new ArrayList<Cluster>();
		name = tName;
		d_id = id;
		t = new Thread(this, name);
		t.start();
		time = 0.0;
	}
	@Override
	public void run() {
		//System.out.println("I am here" + t.getName());
	}
	
	public void setTime(Double time){
		this.time = time;
	}
	
	public void increaseTime(Double incr){
		time = time + incr;
	}
	
	//return current machine time
	public Double getTime(){
		return time;
	}
	
	public void setCurrentCluster(Cluster c) {
		this.c = c;
	}
	
	public void addCluster(Device this, Cluster c) {
		clusterList.add(c);
	}
	
	public void joinCluster(Device this, Cluster c) {
		c.addDevice(this);
		this.c = c;
	}

	public Cluster getCluster(Device this) {
		return c;
	}
	
	public boolean hasCluster(Device this) {
		if (clusterList.size() == 0)
			return false;
		else 
			return true;
	}
	
	public void setJoined(boolean bool) {
		hasJoined = bool;
	}

	public boolean isJoined() {
		return hasJoined;
	}

	public void leaveCluster(Device this) {
		this.getCluster().removeDevice(this);
	}

}
