package sim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Reader {

	static Util.NewMap values;
	static Util.NewMap points;
	static int sum;
	static int count;

	public static void readInput() throws FileNotFoundException, IOException {

		values = new Util.NewMap();
		try (BufferedReader reader = new BufferedReader(new FileReader("target.txt"))) {

			String sCurrentLine;
			String[] splits;
			// int i = 1;
			while ((sCurrentLine = reader.readLine()) != null) {
				// remove trailing spaces
				sCurrentLine = sCurrentLine.trim();
				splits = sCurrentLine.split(",");
				int timeid = Integer.parseInt(splits[0]);
				int xAxis = Integer.parseInt(splits[1]);
				int yAxis = Integer.parseInt(splits[2]);
				double mean = Double.parseDouble(splits[3]);
				double std = Double.parseDouble(splits[4]);

				values.addValue(timeid, xAxis, yAxis, mean, std);
			}
			reader.close();
		}

	}

	public static Util.NewMap getTimePoint(int id) {

		points = new Util.NewMap();
		for (int i = 0; i < values.getSize(); i++) {
			if (values.getId(i) == id) {
				if (values.getMean(i) >= 15) {
					points.addValue(values.getId(i), values.getxAxis(i), values.getyAxis(i), 15 * Math.random(),
							values.getStd(i));
				} else {
					points.addValue(values.getId(i), values.getxAxis(i), values.getyAxis(i), values.getMean(i),
							values.getStd(i));
				}
			}
		}
		return points;
	}
	
	public static void run() throws FileNotFoundException, IOException {
		for (int j = 0; j < 48; j++) {
			appender(28);
		}
	}
	
	// perform some operation on each time point specified by the id
	public static void appender(int time_point)
			throws FileNotFoundException, IOException {
		Util.NewMap details = new Util.NewMap();
		int[][] grid = new int[10][10];// store mean value for each grid 10 x 10
		Reader.readInput();
		details = Reader.getTimePoint(time_point);// get all the data for this time point
		int index = 0;
		for (int j = 0; j < 10; j++) {
			for (int z = 0; z < 10; z++) {
				// save each mean number of devices per grid
				grid[j][z] = (int) Math.round(details.getMean(index));
				index++;
			}
		}
		preProcessSync(grid, time_point);
	}

	// processing grids and device joining
	public static void preProcessSync(int[][] grid, int time_point) throws IOException {
		int[][] temp = grid;
		// COVERAGE = 0;
		//int mid = 0;
		for (int j = 0; j < 10; j++) {
			for (int z = 0; z < 10; z++) {
				//System.out.println("Grid-" + j + "-" + z);
				// create controller for grid
				if (temp[j][z] > 1) {
					sum += temp[j][z];
					count++;
/*					mid++;
					if (mid < 10)
						System.out.print(j + "," + z + " ");
					else {
						System.out.println(j + "," + z + " ");
						mid = 0;
					}*/
					
				}

			}
		}
		System.out.println("\n");
		System.out.println("Timepoint " + time_point + "'s total is: " + sum + " and count is: " + count);
		sum = 0;
		count = 0;
	}
	
/*	public static void main (String [] args) throws FileNotFoundException, IOException {
		run();
	}*/
}
