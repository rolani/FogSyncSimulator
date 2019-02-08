package sim;

import java.util.ArrayList;
import java.util.List;

public class MyMaps {

	public static class TaxiMap {
		public  List<Integer> ids;
		public  List<List<Integer>> grids;
		//public  Map<Integer, List<Integer>> data;

		public TaxiMap() {
			ids = new ArrayList<Integer>();
			grids = new ArrayList<List<Integer>>();
			//data = new HashMap<Integer, List<Integer>>(); 
		}

		public void addItem(int id, List<Integer> grid) {
			//ids.add(id);
			//grids.add(grid);
			//data.put(id, grid);
		}

		public List<Integer> getGridsForId(int id) {
			System.out.println("The index is " + ids.indexOf(id));
			return grids.get(ids.indexOf(id));
		}
		
		public int getGridSizeForId(int id) {
			//System.out.println("The index is " + ids.indexOf(id));
			return grids.get(ids.indexOf(id)).size();
		}
		
		
		public int getSize() {
			return ids.size();		
		}

	}

}
