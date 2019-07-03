package multipoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import china.MyMaps;

public class DataExtractor {
	
	static String path = "taxi";
	//static String path = "/home/laricdbuddy/Desktop/ChinaScheduler/taxi/Taxi_070220/";
	static String javaPath = path.replace("\\", "/"); // Create a new variable
	static File folder = new File(javaPath);
	static File[] listOfFiles = folder.listFiles();
	static int num = listOfFiles.length;
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// static double maxLong = 0.0;
	// static double maxLat = 0.0;
	static double minLong = 12100.0;
	static double minLat = 30900.0;
	static MyMaps.TaxiMap taxiMap;
	static List<Integer> uniqueGrids = new ArrayList<>();

	public static List<Integer> ids;
	public static List<ArrayList<Integer>> grids;

	public DataExtractor() {

	}

	public static Date startOfDay() {
		return parseStringToDate("2007-02-20 00:00:00");
	}

	public static Date parseStringToDate(String s) {
		Date date = null;
		try {
			date = dateFormat.parse(s);
			return date;
		} catch (ParseException e) {
			System.out.println("Error parsing date: " + e);
		}
		return date;
	}

/*	public static void getFileName() {
		for (int i = 0; i < 40; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("File " + listOfFiles[i].getName());
			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
			}
		}
	}*/

	public static List<ArrayList<Integer>> processTraceData() throws FileNotFoundException, IOException {
		
		grids = new ArrayList<ArrayList<Integer>>();
		ids = new ArrayList<Integer>();
		taxiMap = new MyMaps.TaxiMap();

		for (int i = 0; i < num; i++) {

			if (listOfFiles[i].isFile()) {
				try (BufferedReader reader = new BufferedReader(new FileReader(listOfFiles[i]))) {
					ArrayList<Integer> box = new ArrayList<Integer>();

					String sCurrentLine;
					String[] splits;
					// int j = 1;

					String taxiId = reader.readLine();
					String myId = taxiId.split(",")[0];
					taxiMap.ids.add(Integer.parseInt(myId));

					while ((sCurrentLine = reader.readLine()) != null) {
						// remove trailing spaces
						sCurrentLine = sCurrentLine.trim();
						// split on comma
						splits = sCurrentLine.split(",");
						double lng = Double.parseDouble(splits[2]);
						double lat = Double.parseDouble(splits[3]);

						if (lng <= 121.999 && lng >= 121.000 && lat <= 31.5 && lat >= 30.900) {
							int x = (int) (((lat * 1000) - minLat) / 25);
							int y = (int) (((lng * 100) - minLong) / 5);
							int grid = (y * 20) + (x + 1);							
							box.add(grid);
							uniqueGrids.add(grid);

							// if (j < 5) {
							// System.out.println(myId + " " + grid);
							// j++;
							// }
						}
					}
									
					
					/*
					 * int c = 0;
					 * 
					 * try { System.out.println(myId + ": " + box.size()); for (int k = 100; k <
					 * 105; k++) { System.out.print(box.get(k) + ";"); } for (Integer g : box) { if
					 * (c < 10) { System.out.print(g + ";"); c++; } } System.out.println();
					 * 
					 * //System.out.println(box.size() + " " + box.get(0) + " " + box.get(10) + " "
					 * + box.get(100)); } catch (Exception e) {
					 * System.out.println("Error processing"); }
					 */

					// if (grids.size() < 1000) {
					grids.add(box);
					taxiMap.grids.add(box);
					taxiMap.addItem(Integer.parseInt(myId), box);
					// }
					
					Collections.sort(grids, new Comparator<ArrayList<Integer>>(){
					    public int compare(ArrayList<Integer> a1, ArrayList<Integer> a2) {
					        return a2.size() - a1.size(); // assumes you want biggest to smallest
					    }
					});

					reader.close();
					
					
					for (int v = 0; v < grids.size(); v++) {
						if (grids.get(v).size() < 7003) {
							int size = box.size();
							for (int f = size; f < 7003; f++) {
								box.add(0);
							}
						}
					}
				}
			}
		}
		return grids;
	}
	
	public static List<ArrayList<Integer>> processEdgeCloudTraceData() throws FileNotFoundException, IOException {
		
		grids = new ArrayList<ArrayList<Integer>>();
		ids = new ArrayList<Integer>();
		taxiMap = new MyMaps.TaxiMap();
		
		for (int i = 0; i < 400; i++) {

			if (listOfFiles[i].isFile() && listOfFiles[i].getName().startsWith("Taxi")) {
				//System.out.println("Processing file " + listOfFiles[i].getName());
				try (BufferedReader reader = new BufferedReader(new FileReader(listOfFiles[i]))) {
					ArrayList<Integer> box = new ArrayList<Integer>();

					String sCurrentLine;
					String[] splits;
					// int j = 1;

					String taxiId = reader.readLine();
					String myId = taxiId.split(",")[0];
					taxiMap.ids.add(Integer.parseInt(myId));

					while ((sCurrentLine = reader.readLine()) != null) {
						// remove trailing spaces
						sCurrentLine = sCurrentLine.trim();
						// split on comma
						splits = sCurrentLine.split(",");
						double lng = Double.parseDouble(splits[2]);
						double lat = Double.parseDouble(splits[3]);

						if (lng <= 121.999 && lng >= 121.000 && lat <= 31.5 && lat >= 30.900) {
							int x = (int) (((lat * 1000) - minLat) / 25);
							int y = (int) (((lng * 100) - minLong) / 5);
							int grid = (y * 20) + (x + 1);							
							box.add(grid);
							uniqueGrids.add(grid);

							// if (j < 5) {
							// System.out.println(myId + " " + grid);
							// j++;
							// }
						}
					}
									

					// if (grids.size() < 1000) {
					grids.add(box);
					taxiMap.grids.add(box);
					taxiMap.addItem(Integer.parseInt(myId), box);
					// }
					
					Collections.sort(grids, new Comparator<ArrayList<Integer>>(){
					    public int compare(ArrayList<Integer> a1, ArrayList<Integer> a2) {
					        return a2.size() - a1.size(); // assumes you want biggest to smallest
					    }
					});

					reader.close();
					
					
					for (int v = 0; v < grids.size(); v++) {
						if (grids.get(v).size() < 7003) {
							int size = box.size();
							for (int f = size; f < 7003; f++) {
								box.add(0);
							}
						}
					}
				}
			}
		}
		return grids;
	}

	public static void checkGridContent() {
		// System.out.println("here");
		System.out.println("Total taxis: " + taxiMap.getSize());

		for (int i = 950; i < 955; i++) {
			List<Integer> temp = new ArrayList<Integer>();
			int c = 0;
			temp = taxiMap.getGridsForId(taxiMap.ids.get(i));
			/// temp = ;
			System.out.println(taxiMap.ids.get(i) + ":" + taxiMap.getGridSizeForId(taxiMap.ids.get(i)));
			System.out.println(temp.size());
			for (int j = 1000; j < 1005; j++) {
				System.out.print(temp.get(j) + ";");
			}

			for (Integer g : temp) {
				if (c < 10) {
					System.out.print(g + ";");
					c++;
				}
			}
			System.out.print(temp.get(1300) + " " + temp.get(1350));

			System.out.println();

		}
		System.out.println("Grids");
		Set<Integer> sets = new HashSet<Integer>(uniqueGrids);

		for (Integer h : sets) {
			if (Collections.frequency(uniqueGrids, h) > 10000) {
				System.out.println(h + " " + Collections.frequency(uniqueGrids, h));
			}

		}
	}

	public static void peepGridContent() {
		int max = 0;
		int sum = 0;
		int c = 1;
		for (List<Integer> g : grids) {
			System.out.println("Taxi: " + c + " " + g.size());
			sum += g.size();
			if (max < g.size()) {

				max = g.size();
			}
			try {
				for (int j = 10; j < 25; j++) {
					System.out.print(g.get(j) + ";");

				}
			} catch (Exception e) {
				System.out.println("Error processing");
			}
			System.out.println();
			c++;
		}
		System.out.println("Max is: " + max + " and average is: " + (sum / grids.size()));

		
		Set<Integer> sets = new HashSet<Integer>(uniqueGrids);
		System.out.println("Grids" + sets.size());
		for (Integer h : sets) {
			if (Collections.frequency(uniqueGrids, h) > 10000) {
				System.out.println(h + " " + Collections.frequency(uniqueGrids, h));
			}

		}

	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		ids = new ArrayList<Integer>();
		grids = new ArrayList<ArrayList<Integer>>();
		// getFileName();
		processTraceData();
		// checkGridContent();
		peepGridContent();

	}
}
