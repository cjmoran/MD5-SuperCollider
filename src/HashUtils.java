import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HashUtils {

	public static String getHexValue(byte[] hash) {
		StringBuilder sb = new StringBuilder(2 * hash.length);
		for(byte b : hash) {
			sb.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4));
			sb.append("0123456789ABCDEF".charAt((b & 0x0F)));
		}
		return sb.toString();
	}

	public static byte[] getByteArray(String hexString) {
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

	public static List<String> splitString(String hexString, int partSize) {
		//Splits a String into pieces of length partSize
		List<String> parts = new ArrayList<>();
		int length = hexString.length();
		for(int i=0; i<length; i+=partSize) {
			parts.add(hexString.substring(i, Math.min(length, i + partSize)));
		}
		return parts;
	}

	public static class Worker implements Runnable {
		private boolean collisionFound = false;
		private OnCollisionFoundListener collisionFoundListener;
		private XORShiftRandom rng = new XORShiftRandom();
		private UserPreferencesManager userPrefs;

		public Worker(UserPreferencesManager prefs, OnCollisionFoundListener listener) {
			collisionFoundListener = listener;
			userPrefs = prefs;
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

				String randomString = getRandomString(userPrefs.getRandomStringLength());
				try {
					randomBytes = hasher.digest(randomString.getBytes("UTF-8"));	//hash it
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				if(userPrefs.isPrintingEveryHash()) {
					System.out.println("Random input: " + randomString + "\tHashed: " +
							HashUtils.getHexValue(randomBytes));
				} else {
					CFinder.attemptsPerSecond.incrementAndGet();
				}

				if(Arrays.equals(userPrefs.getHashToMatch(), randomBytes)) {
					collisionFound = true;

					System.out.println("\nWow, I actually found a collision! The random input '" +
							randomString + "' hashed to the same value as your input: " +
							HashUtils.getHexValue(randomBytes));

					try {
						CFinder.printResults(userPrefs.getHashToMatch(), randomBytes,
								userPrefs.getOriginalUserInput(), randomString);
					} catch(FileNotFoundException f) {
						f.printStackTrace();
					}

					System.out.println(
							"This information has been saved to 'collision.txt' in this program's directory.");

					collisionFoundListener.onCollisionFound();
				}
			}
		}

		public interface OnCollisionFoundListener {
			void onCollisionFound();
		}

		public String getRandomString(int length) {
			StringBuilder sb = new StringBuilder(length);
			while(sb.length() < length) {
				sb.append(
						UserPreferencesManager.CHARACTERS_TO_USE[
								rng.nextInt(UserPreferencesManager.CHARACTERS_TO_USE.length - 1)]
				);
			}
			return sb.toString();
		}
	}
}
