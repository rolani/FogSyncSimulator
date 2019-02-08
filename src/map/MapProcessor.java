package map;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


//'241;2014-02-07 00:00:00.078074+01;POINT(41.9130648885005 12.4768036922949)',
//'209;2014-02-07 00:00:00.155042+01;POINT(41.9004025843922 12.4732682952044)'
public class MapProcessor {
	static final Random RANDOM = new Random();
	static double minX = 41900;
	static double minY = 12440;
	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static Utility.Mapper map = new Utility.Mapper();
	static Utility.MyMap gridMap = new Utility.MyMap();

	private static String readData() {
		String filePath = "02-13.txt";
		StringBuilder contentBuilder = new StringBuilder();

		try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return contentBuilder.toString();
	}

	public static Date startOfDay() {
		return parseStringToDate("2014-02-13 08:00:00");
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

	public static long timeFromStartOfDay(Date d, TimeUnit timeUnit) {
		long diffInMillies = d.getTime() - startOfDay().getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	public static void processData() {

		String allData = readData();
		String[] eachInput;
		// int i = 0;
		eachInput = allData.split(",");

		for (String line : eachInput) {

			try {
				String[] splits = line.split(";");

				String idS = splits[0];
				idS = idS.replaceAll("'", "").trim();
				int id = Integer.parseInt(idS);

				String dateTime = splits[1];
				String date = dateTime.split("\\.")[0].trim();

				int time = (int) timeFromStartOfDay(parseStringToDate(date), TimeUnit.SECONDS);
				date = date.split(" ")[1].trim();

				String gps = splits[2];
				gps = gps.replaceAll("POINT", "").replaceAll("'", "").replaceAll("[\\[\\](){}]", "").trim();
				double lng = Double.parseDouble(gps.split(" ")[0].trim());
				double lat = Double.parseDouble(gps.split(" ")[1].trim());

				if (lng <= 41.94 && lng >= 41.90 && lat <= 12.50 && lat >= 12.44) {
					int x = (int) (((lng * 1000) - minX) / 8);
					int y = (int) (((lat * 1000) - minY) / 12);
					int grid = (y * 5) + (x + 1);

					map.addItem(id, time, grid, date);

					// if (i < 30){
					// System.out.println(id + " " + time + " " + grid);
					// i++;
					// }
				} else {
					// int grid = getRandomNumberInRange(0, 25);
					// map.addItem(id, time, grid, date);
				}

			} catch (Exception e) {
				System.out.println("Invalid input format: " + e);
			}
		}

		// System.out.println("Done");
		//assignGridsToIds();
	}

	// returns a random number between min and max
	public static int getRandomNumberInRange(int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		return RANDOM.nextInt((max - min) + 1) + min;
	}

	public static Utility.MyMap assignGridsToIds() {
		List<Integer> list = map.getUniqueIds();
		List<Integer> temp = new ArrayList<Integer>();
		
		for (int j = 0; j < list.size(); j++) {
			temp.clear();
			List<Integer> grids = new ArrayList<Integer>();
			temp = map.getAllIndexOfId(list.get(j));
			//System.out.println(temp.size());
			int index = map.ids.indexOf(list.get(j));
			int first = (int) ((map.diffs.get(index) / 15));
			if (first > 2000) {
				first = (int) (500 * Math.random());
			} else if (first > 1000 && first <= 2000) {
				first = (int) (300 * Math.random());
			} else if (first > 500 && first <= 1000) {
				first = (int) (200 * Math.random());
			} else if (first > 100 && first < 500) {
				first = (int) (100 * Math.random());
			}
			//System.out.println(list.get(j) + " first is: " + first);
			for (int i = 0; i < first; i++) {
				grids.add(0);
			}
			//System.out.println("temp size : " +temp.size());
			for (int k = 0; k < temp.size(); k++) {
				grids.add(map.box.get(temp.get(k)));
			}

			if (grids.size() < 3840) {
				int size = grids.size();
				for (int f = size; f <= 3840; f++) {
					grids.add(0);
				}
			}
			
			gridMap.addItem(list.get(j), grids);
		}
		return gridMap;
	}
	
	public static void checkGridContent() {
		System.out.println("here");
		System.out.println(gridMap.getSize());
		List<Integer> temp = new ArrayList<Integer>();
		
		for (int i = 0; i < gridMap.getSize(); i++) {
			int c = 0;
			temp = gridMap.getGridForId(gridMap.Ids.get(i));
			System.out.println(gridMap.Ids.get(i) + ":");
			for (Integer g : temp) {
				if (c < 10) {
					System.out.print(g+";");
					c++;
				}
				
			}
			System.out.println();
		}
		
	}

	public static void uniqueGrids() {
		for (int grid : map.getUniqueGrids()) {
			System.out.println(grid);
		}
	}

	public static void trackTime() {
		List<Integer> temp = new ArrayList<Integer>();
		List<Integer> list = map.getUniqueIds();

		for (int i = 0; i < list.size(); i++) {
			temp.clear();
			temp = map.getAllIndexOfId(list.get(i));

			// System.out.println(list.get(i) + ": " + map.dates.get(temp.get(0)) + " " +
			// map.dates.get(temp.get(1)) + " "
			// + map.dates.get(temp.get(temp.size()-2)) + " " +
			// map.dates.get(temp.get(temp.size()-1)));

			String start = map.dates.get(temp.get(0));
			start = start.split(":")[0].trim();
			String end = map.dates.get(temp.get(temp.size() - 1));
			end = end.split(":")[0].trim();
			System.out.println(start + " " + end);
		}

	}
	
	public static void trackGrids() {
		List<Integer> temp = new ArrayList<Integer>();
		List<Integer> list = map.getUniqueIds();

		for (int i = 0; i < list.size(); i++) {
			List<Integer> grids = new ArrayList<Integer>();
			temp.clear();
			//grids.clear()
			temp = map.getAllIndexOfId(list.get(i));

			System.out.println(list.get(i) + ": " + map.box.get(temp.get(0)) + " " +
			map.box.get(temp.get(1)) + " "
			 + map.box.get(temp.get(temp.size()-2)) + " " +
			 map.box.get(temp.get(temp.size()-1)));
			
			for (int k = 0; k < temp.size(); k++) {
				grids.add(map.box.get(temp.get(k)));
			}
			
			gridMap.addItem(list.get(i), grids);

			/*String start = map.dates.get(temp.get(0));
			start = start.split(":")[0].trim();
			String end = map.dates.get(temp.get(temp.size() - 1));
			end = end.split(":")[0].trim();
			System.out.println(start + " " + end);*/
		}

	}

	public static void main(String[] args) {
		processData();
		long l = timeFromStartOfDay(parseStringToDate("2014-02-13 08:10:19"), TimeUnit.SECONDS);
		System.out.println(l);
		//assignGridsToIds();		
		// uniqueGrids();
		//firstOccurenceOfIds();
		// trackTime();
		//trackGrids();
		//checkGridContent();
	}
	
}
