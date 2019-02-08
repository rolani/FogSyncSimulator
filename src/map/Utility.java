package map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Utility {
	
public static class Converter{
		
		public static Set<Integer> convertListToSet(List<Integer> list){
			Set<Integer> set = new HashSet<>(list);
			return set;
		}
		
		public static List<Integer> convertSetToList(Set<Integer> set){
			List<Integer> list = new ArrayList<>(set);
			return list;
		}
	}
	
	public static class Mapper {
		public List<Integer> ids, diffs, box;
		public List<String> dates; //for debugging
		//private List<List<Integer>> clusters;
		
		public Mapper() {
			ids = new ArrayList<Integer>();
			diffs = new ArrayList<Integer>(); 
			box = new ArrayList<Integer>(); 
			dates = new ArrayList<String>(); 
		}
		
		public void addItem(int id, int diff, int grid, String time) {
			//if (devices.size() <= Items.TIMEPOINT) {
				ids.add(id);
				diffs.add(diff);
				box.add(grid);
				dates.add(time);
			//}
		}
		
		public List<Integer> getAllIndexOfId(int id) {
			List<Integer> indexList = new ArrayList<Integer>();
			for (int i = 0; i < ids.size(); i++){
				if (id == ids.get(i)){
					indexList.add(i);
				}
			}
			return indexList;
		}
		
		public int getSize() {
			return ids.size();
		}
		
		public List<Integer> getUniqueIds(){
			Set<Integer> set =  Utility.Converter.convertListToSet(ids);
			return Utility.Converter.convertSetToList(set);
		}
		
		public List<Integer> getUniqueGrids(){
			Set<Integer> set =  Utility.Converter.convertListToSet(box);
			return Utility.Converter.convertSetToList(set);
		}
		
	}

	
	public static class MyMap {
		public  List<Integer> Ids;
		public  List<List<Integer>> grids;
		
		public MyMap() {
			Ids = new ArrayList<Integer>();
			grids = new ArrayList<List<Integer>>(); 
		}
		
		public void addItem(Integer d, List<Integer> list) {
			//if (Integers.size() <= Items.TIMEPOINT) {
				Ids.add(d);
				grids.add(list);
			//}
		}
		
		public int getIdIndex(int id) {
			return Ids.indexOf(id);
		}
		
		public int getSize() {
			return Ids.size();
		}
		
		public Integer getId(int index) {
			return Ids.get(index);
		}
		
		public List<Integer> getGridForId(int id) {
			return grids.get(getIdIndex(id));
		}
		
		public Integer getGrid(int id, int time_point) {
			List<Integer> temp = new ArrayList<Integer>();
			temp = getGridForId(id);
			return temp.get(time_point);
		}
	}
	
	public static class NewMap {
		public Set<Integer> set;
		public List<Integer> idList;
		public List<Integer> grid;
		public List<Double> timeDiff;
		//public List<Double> longs;
		//public List<Double> lats;
		
		public NewMap() {
			idList = new ArrayList<Integer>();
			grid = new ArrayList<Integer>();
			timeDiff = new ArrayList<Double>();
			//set = new HashSet<Integer>();
			//longs = new ArrayList<Double>();
			//lats = new ArrayList<Double>();
		}
		
		// date- time diff, lng - x axis lat - y axis
		public void addTuple(int id, double date, int box) {
			idList.add(id);
			timeDiff.add(date);
			grid.add(box);
		}

		public int getId(int index) {
			return idList.get(index);
		}

		public double getDifference(int index) {
			return timeDiff.get(index);
		}
		
		public int getGrid(int index) {
			return grid.get(index);
		}
		
		public int getIdCount() {
			set = new HashSet<Integer>(idList);
			return set.size();
		}
		
		public Set<Integer> getUniqueIds(){
			set = new HashSet<Integer>(idList);
			return set;
		}
		
		public int getSize(){
			return idList.size();
		}
		
/*		public Integer returnBoundaries() {
			double minX = Double.parseDouble(longitude.get(0));
			double maxX = 0;
			double minY = Double.parseDouble(latitude.get(0));
			double maxY = 0;
			
			for (int i = 0; i < longitude.size(); i++) {
				double d = Double.parseDouble(longitude.get(i));
				if (d > maxX) {
					maxX  = d;
				}else if (d < minX) {
					minX = d;
				}
			}
			
			for (int j = 0; j < latitude.size(); j++) {
				double d = Double.parseDouble(latitude.get(j));
				if (d > maxY) {
					maxY  = d;
				}else if (d < minY) {
					minY = d;
				}
			}
			
			Integer boundaries = maxX + " " + maxY + ", " + minX + " " + minY;
			return boundaries;
		}*/
		
	}	
	
}
