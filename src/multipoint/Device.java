package multipoint;

import java.util.ArrayList;
import java.util.List;

public class Device implements Runnable {
	String name;
	int d_id;
	double time;
	double multiplier;
	Boolean hasJoined = false;
	Boolean marked = false;
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
		time = 0;
	}
	
	Device(String tName, int id, double mult){
		clusterList = new ArrayList<Cluster>();
		name = tName;
		d_id = id;
		t = new Thread(this, name);
		multiplier = mult;
		t.start();
		time = 0;
	}
	@Override
	public void run() {
		//System.out.println("I am here" + t.getName());
	}
	
	public void setTime(double time){
		this.time = time;
	}
	
	public void increaseTime(double i){
		time = time + i;
	}
	
	//return current machine time
	public Double getTime(){
		return time;
	}
	
	public void mark(){
		marked = true;
	}
	
	public Boolean isMarked(){
		return marked;
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

	public Cluster getClusterAtTimePoint(Device this, int index) {
		return clusterList.get(index);
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
}
