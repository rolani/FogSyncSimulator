package sim;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TaskGenerator {
	private static FileWriter fileWriter;
	private static List<Integer> tagList;
	private static List<Integer> noSyncList;
	private static List<Integer> fullList;
	private static final int SYNC_NUM = 10; // number of sync tasks
	// private static final int UPDATE = 5; //update message tag
	private static final int TASKS = 30; // total number of tasks
	private static final double VARIANCE = 30; // execution time variance
	public static final int SYNC_TAG = 2; // sync task tag
	static String filename;

	public TaskGenerator(String filename) throws IOException {
		TaskGenerator.filename = filename;
		fileWriter = new FileWriter(filename);
		noSyncList = new ArrayList<Integer>();
		fullList = new ArrayList<Integer>();
		tagList = new ArrayList<Integer>();

	}

	// generate regular tasks
	public void getTags() throws IOException {
		int syncCount = 1;
		int previous = 0;
		int other = 0;
		int i = 0;
		// no of tasks to create
		while (tagList.size() < TASKS) {
			int num = (int) ((Math.random() * 4) + 1); // generate random numbers between 1 and 4 inclusive
			if (num == 2 && previous != 2 && other != 2 && syncCount <= SYNC_NUM) {
				if (tagList.size() > 1) {
					tagList.add(num);
					syncCount++;
				}
				
			} else if (num == 1 || num == 3 || num == 4) {
				tagList.add(num);
			}
			if (i % 2 == 0)
				previous = num;
			else
				other = num;
			i++;
		}
		System.out.println(tagList.size());
		for (int j = 0; j < tagList.size(); j++) {
			/*
			 * if (tagList.get(j) == 2) { if (j - 1 > 0 && j + 1 < tagList.size()) {
			 * fullList.add(fullList.size() - 1, UPDATE); fullList.add(tagList.get(j)); } }
			 * else { fullList.add(tagList.get(j)); }
			 */
			fullList.add(tagList.get(j));
		}
		generateNeutral();
	}

	// generate tasks for loading pattern
	public void getFreqTags() throws IOException {

		// no of tasks to create
		while (noSyncList.size() < TASKS) {
			int num = (int) ((Math.random() * 3) + 1);
			if (noSyncList.size() % 4 == 0) {
				noSyncList.add(SYNC_TAG);
				// syncCount++;

			} else {
				if (num == 1 || num == 3 || num == 4) {
					noSyncList.add(num);
				}
			}
		}

		System.out.println(noSyncList.size());

		for (int j = 0; j < noSyncList.size(); j++) {
			/*
			 * if (tagList.get(j) == 2) { if (j - 1 > 0 && j + 1 < tagList.size()) {
			 * fullList.add(fullList.size() - 1, UPDATE); fullList.add(tagList.get(j)); } }
			 * else { fullList.add(tagList.get(j)); }
			 */
			fullList.add(tagList.get(j));
		}
		generateFrequency();
	}

	@SuppressWarnings("unused")
	private void generate() throws IOException {

		int taskGroup = 1;
		for (int i = 0; i < fullList.size(); i++) {
			int tag = fullList.get(i);
			double execTime = 0.0;
			if (tag == 1 || tag == 3) {
				execTime = (int) (Math.random() * 10 + 10);
			} else if (tag == 2) {
				execTime = (int) (Math.random() * 10 + 10);
				taskGroup++;
			} else if (tag == 5) {
				execTime = 0.05;
			} else if (tag == 6) {
				execTime = 1;
			} else {
				execTime = (int) (Math.random() * 10 + 10);
			}
			System.out.println(tag + " " + taskGroup + " " + execTime);

			fileWriter.write(tag + " " + taskGroup + " " + execTime + "\n");

		}
	}

	public void generateNeutral() throws IOException {

		for (int i = 0; i < fullList.size(); i++) {
			int tag = fullList.get(i);
			double execTime = 0.0;
			execTime = (int) getGaussian(100); // distribution for task execution time
			System.out.println(i + " " + tag + " " + execTime);

			fileWriter.write(tag + " " + execTime + "\n");

		}
	}

	// generate task with execution time
	public void generateFrequency() throws IOException {
		for (int i = 0; i < fullList.size(); i++) {
			int tag = fullList.get(i);
			double execTime = 0.0;
			if (tag == 5)
				execTime = 10;
			else
				execTime = (int) getGaussian(100);

			System.out.println(i + " " + tag + " " + execTime);

			// fileWriter.write(tag + " " + execTime + "\n");

		}
	}

	public static Random fRandom = new Random();

	public static double getGaussian(double aMean) {
		return aMean + fRandom.nextGaussian() * VARIANCE;

	}

	public static void main(String args[]) throws IOException {
		TaskGenerator tg1 = new TaskGenerator("tg1.txt");
		tg1.getTags();
		fileWriter.close();

	}

}
