import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/*
	=====MD5 Collision Finder=====
		by Cullin Moran
		
	Attempts to find a collision for an unsalted MD5 hash by brute force (using random values).
	Does not check for duplicate values while creating random values.
	
	Will probably run for a very long period of time.
*/

public class CFinder {
	private static final String VERSION = "2.2.2";
	private static final int NUM_CPU_CORES = Runtime.getRuntime().availableProcessors();

	private static Thread[] threads;
	public static AtomicInteger attemptsPerSecond;
	
	public static void main(String[] args) {
		attemptsPerSecond = new AtomicInteger(0);

		printAppTitle();

		UserPreferencesManager prefs = new UserPreferencesManager();
		prefs.initPrefsFromUser();

		System.out.println("\nWorking on " + NUM_CPU_CORES + " threads...\n");

		final HashUtils.Worker w = new HashUtils.Worker(prefs, () -> stopAllThreads(threads));
		threads = createThreads(NUM_CPU_CORES, w);
		startThreads(threads);

		if(!prefs.isPrintingEveryHash()) {
			startPrintingAttemptsStats();
		}
	}

	public static String getBackspaceCharacters(int num) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<num; i++) {
			sb.append("\b");
		}
		return sb.toString();
	}

	private static Thread[] createThreads(final int numCpuCores, final Runnable runnable) {
		Thread[] workerThreads = new Thread[numCpuCores];
		for(int i=0; i<NUM_CPU_CORES; i++) {
			workerThreads[i] = new Thread(runnable);
		}
		return workerThreads;
	}

	private static void startThreads(final Thread[] threadsToRun) {
		for(Thread thread : threadsToRun) {
			thread.start();
		}
	}
	
	public static synchronized void printResults(byte[] origHash, byte[] randomHash,
												  String origInput, String randomInput) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new FileOutputStream("collision.txt", true));
		String out = "Your original input: ";
		if(origInput.equalsIgnoreCase("t")) {	//If the user entered a hash directly for some reason
			out += "[you chose to enter an MD5 hash directly]";
		} else {
			out += origInput;
		}
		out += "\nOriginal Hash: " + HashUtils.getHexValue(origHash) +
				"\n\nRandomly-generated input: " + randomInput +
				"\nRandom Input Hash: " + HashUtils.getHexValue(randomHash) +
				"\n\n=====\n\n";
		pw.append(out);
		pw.close();
	}

	private static void stopAllThreads(final Thread[] threadsToStop) {
		for(Thread thread : threadsToStop) {
			thread.interrupt();
		}
	}

	private static void printAppTitle() {
		System.out.println("========MD5 SuperCollider========\nby Cullin Moran\nv" + CFinder.VERSION + "\n");
	}

	private static void startPrintingAttemptsStats() {
		System.out.print("Attempts per second: ");
		new Thread(new Runnable() {
			final long UPDATE_INTERVAL = 1000l;
			String previousOutput = null;

			@Override
			public void run() {
				while(!Thread.interrupted()) {
					if(previousOutput != null) {
						System.out.print(getBackspaceCharacters(previousOutput.length()));
					}
					final int attempts = attemptsPerSecond.intValue();
					attemptsPerSecond.set(0);
					System.out.print(attempts);
					previousOutput = Integer.toString(attempts);

					try {
						Thread.sleep(UPDATE_INTERVAL);
					} catch(InterruptedException e) {
						System.err.println("Display update thread interrupted!");
					}
				}
			}
		}).start();
	}
}
