import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class UserPreferencesManager {

	private int randomStringLength;
	private boolean printEveryHash;
	private String originalUserInput;

	private byte[] hashToMatch;

	public final static char[] CHARACTERS_TO_USE = {
			'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
			'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
			'!','@','#','$','%','^','&','*','(',')','_','+',
			'1','2','3','4','5','6','7','8','9','0','-','=',
			'[',']','\\',';','\'',',','.','/','`',
			'{','}','|',':','"','<','>','?','~'};

	public void initPrefsFromUser() {
		Scanner darkly = new Scanner(System.in);
		byte[] inputHash = null;
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch(NoSuchAlgorithmException e) {
			System.err.println("Congratulations; your Java installation is incapable of hashing to MD5!");
			System.exit(1);
		}

		//Random String length pref
		System.out.println("What length random Strings should I generate to test with?");
		randomStringLength = darkly.nextInt();
		darkly.nextLine();

		//Print Status Messages pref
		printEveryHash = yesNoQuestion("\n(SLOW) Should I print status messages for each hash I generate? (y/n)");

		//Get MD5 to find collision for
		System.out.println("\nEnter the MD5 you'd like to find a collision for, " +
				"or enter 't' if you'd like to enter some plain text and hash that:");
		originalUserInput = darkly.nextLine();

		if(originalUserInput.equalsIgnoreCase("t")) {
			System.out.println("\nOkay, enter your text to hash:");
			originalUserInput = darkly.nextLine();

			//Hash it
			try {
				inputHash = messageDigest.digest(originalUserInput.getBytes("UTF-8"));
				System.out.println("\nYour input hashed to:\n" + HashUtils.getHexValue(inputHash));
			} catch (UnsupportedEncodingException e) {
				System.err.println("Unsupported encoding. Exiting...");
				System.exit(1);
			}
		}
		else {
			//First, convert the hex String to an array of bytes
			inputHash = HashUtils.getByteArray(originalUserInput);

			System.out.println("You entered hash: " + HashUtils.getHexValue(inputHash));
		}

		hashToMatch = inputHash;
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

	public int getRandomStringLength() {
		return randomStringLength;
	}

	public boolean isPrintingEveryHash() {
		return printEveryHash;
	}

	public String getOriginalUserInput() {
		return originalUserInput;
	}

	public byte[] getHashToMatch() {
		return hashToMatch;
	}
}
