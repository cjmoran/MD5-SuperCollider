import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/*
	=====MD5 Collision Finder=====
		by Cullin Moran
		
	Attempts to find a collision for an unsalted MD5 hash by brute force (using random values).
	Does not check for duplicate values while creating random values.
	
	Will probably run for a very long period of time.
*/

/*** Disclaimer: This program is a joke, and completely unrealistic for the purpose it purports to serve. ***
 *** Please excuse the abysmal coding practices contained herein. ***/

public class CFinder {
	private static final String VERSION = "2.2.1";
	private static final int NUM_CPU_CORES = Runtime.getRuntime().availableProcessors();

	private static int randomStringLength;
	private static boolean printEveryHash;
	private static String userInput;

	private static Thread[] threads;
	public static AtomicInteger attemptsPerSecond;

	private final static char[] characters = {
		'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
		'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
		'!','@','#','$','%','^','&','*','(',')','_','+',
		'1','2','3','4','5','6','7','8','9','0','-','=',
		'[',']','\\',';','\'',',','.','/','`',
		'{','}','|',':','"','<','>','?','~'};
	
	public static void main(String[] args) {
		attemptsPerSecond = new AtomicInteger(0);

		Scanner darkly = new Scanner(System.in);
		byte[] inputHash;
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch(NoSuchAlgorithmException e) {
			System.err.println("Congratulations; your Java installation is incapable of hashing to MD5!");
			System.exit(0);
		}

		System.out.println("========MD5 SuperCollider========\nby Cullin Moran\nv" + VERSION + "\n");
		
		//Initial settings
			//Random String length
				System.out.println("What length random Strings should I generate to test with?");
				randomStringLength = darkly.nextInt();
				darkly.nextLine();
			//Print Status Messages
				printEveryHash = yesNoQuestion("\n(SLOW) Should I print status messages for each hash I generate? (y/n)");
		
		//Get MD5 to find collision for
		System.out.println("\nEnter the MD5 you'd like to find a collision for, " +
				"or enter 't' if you'd like to enter some plain text and hash that:");
		userInput = darkly.nextLine();
		
		if(userInput.equalsIgnoreCase("t")) {
			System.out.println("\nOkay, enter your text to hash:");
			userInput = darkly.nextLine();

			//Hash it
			try {
				inputHash = messageDigest.digest(userInput.getBytes("UTF-8"));
				System.out.println("\nYour input hashed to:\n" + getHexValue(inputHash));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				inputHash = null;
			}
		}
		else {
			//First, convert the hex String to an array of bytes
			inputHash = getByteArray(userInput);

			System.out.println("You entered hash: " + getHexValue(inputHash));
		}

		final byte[] hashToMatch = inputHash;
		
		System.out.println("\nWorking on " + NUM_CPU_CORES + " threads...\n");

		final Worker w = new Worker(hashToMatch, () -> stopAllThreads(threads));
		threads = createThreads(NUM_CPU_CORES, w);
		startThreads(threads);

		if(!printEveryHash) {
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

	/**
	 * @return Yes = true, No = false
	 */
	private static boolean yesNoQuestion(String question) {
		//Asks the user a yes/no question.
		Scanner s = new Scanner(System.in);
		String input;
		do {
			System.out.println(question);
			input = s.nextLine();
			if(input.equalsIgnoreCase("y")) {
				return true;
			}
			else if(input.equalsIgnoreCase("n")) {
				return false;
			}
			else {
				System.out.println("Input not appropriate. Try again.");
			}
		} while(true);
	}
	
	private static synchronized void printResults(byte[] origHash, byte[] randomHash,
												  String origInput, String randomInput) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new FileOutputStream("collision.txt", true));
		String out = "Your original input: ";
		if(origInput.equalsIgnoreCase("t")) {	//If the user entered a hash directly for some reason
			out += "[you chose to enter an MD5 hash directly]";
		} else {
			out += origInput;
		}
		out += "\nOriginal Hash: " + getHexValue(origHash) +
				"\n\nRandomly-generated input: " + randomInput +
				"\nRandom Input Hash: " + getHexValue(randomHash) +
				"\n\n=====\n\n";
		pw.append(out);
		pw.close();
	}

	private static void stopAllThreads(final Thread[] threadsToStop) {
		for(Thread thread : threadsToStop) {
			thread.interrupt();
		}
	}

	private static class Worker implements Runnable {
		private boolean collisionFound = false;
		private byte[] inputHash;
		private OnCollisionFoundListener collisionFoundListener;
		XORShiftRandom rng = new XORShiftRandom();

		public Worker(byte[] hashToMatch, OnCollisionFoundListener listener) {
			inputHash = hashToMatch;
			collisionFoundListener = listener;
		}

		@Override
		public void run() {
			byte[] randomBytes = null;

			MessageDigest hasher = null;
			try {
				hasher = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				System.err.println("Congratulations; your Java installation is incapable of hashing to MD5!");
				System.exit(0);
			}

			while(!collisionFound) {

				if(Thread.interrupted()) {
					return;
				}

				String randomString = getRandomString(CFinder.randomStringLength);
				try {
					randomBytes = hasher.digest(randomString.getBytes("UTF-8"));	//hash it
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				if(printEveryHash) {
					System.out.println("Random input: " + randomString + "\tHashed: " + getHexValue(randomBytes));
				}

				if(Arrays.equals(inputHash, randomBytes)) {
					collisionFound = true;

					System.out.println("\nWow, I actually found a collision! The random input '" +
							randomString + "' hashed to the same value as your input: " + getHexValue(randomBytes));

					try {
						printResults(inputHash, randomBytes, userInput, randomString);
					} catch(FileNotFoundException f) {
						f.printStackTrace();
					}

					System.out.println(
							"This information has been saved to 'collision.txt' in this program's directory.");

					collisionFoundListener.onCollisionFound();
				}

				if(!printEveryHash) {
					CFinder.attemptsPerSecond.incrementAndGet();
				}
			}
		}

		public interface OnCollisionFoundListener {
			void onCollisionFound();
		}

		private String getRandomString(int length) {
			StringBuilder sb = new StringBuilder(length);
			while(sb.length() < length) {
				sb.append(characters[rng.nextInt(characters.length-1)]);
			}
			return sb.toString();
		}
	}

	private static String getHexValue(byte[] hash) {
		StringBuilder sb = new StringBuilder(2 * hash.length);
		for(byte b : hash) {
			sb.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4));
			sb.append("0123456789ABCDEF".charAt((b & 0x0F)));
		}
		return sb.toString();
	}

	private static byte[] getByteArray(String hexString) {
		//Two hex characters per byte, so we only need half the space
		byte[] b = new byte[hexString.length() / 2];

		int i = 0;
		int temp;
		//Get two characters at a time and convert them to binary, then store them
		for(String octet : splitString(hexString, 2)) {
			temp = Integer.parseInt(octet, 16); //Tell Java that this is a hex number
			b[i] = (byte) Integer.parseInt(Integer.toBinaryString(temp), 2); //Get the binary representation of the hex
			i++;
		}
		return b;
	}

	private static List<String> splitString(String hexString, int partSize) {
		//Splits a String into pieces of length partSize
		List<String> parts = new ArrayList<>();
		int length = hexString.length();
		for(int i=0; i<length; i+=partSize) {
			parts.add(hexString.substring(i, Math.min(length, i + partSize)));
		}
		return parts;
	}
}
