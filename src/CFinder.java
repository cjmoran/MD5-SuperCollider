import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/*
	=====MD5 Collision Finder=====
		by Cullin Moran
		v1.0
		
	Attempts to find a collision for an unsalted MD5 hash by brute force (using random values).
	Does not check for duplicate values while creating random values.
	
	Will probably run for a very long period of time.
*/

public class CFinder {	
	static MessageDigest hasher;
	
	final static char[] characters = {
		'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
		'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
		'!','@','#','$','%','^','&','*','(',')','_','+',
		'1','2','3','4','5','6','7','8','9','0','-','=',
		'[',']','\\',';','\'',',','.','/','`',
		'{','}','|',':','"','<','>','?','~'};
	
	public static void main(String[] args) {
		final int LENGTH_OF_RANDOM_STRING;
		final boolean PRINT_STATUS;
		final float VERSION = 1.0f;
		
		Scanner scan = new Scanner(System.in);
		String userInput;
		byte[] inputHash = null;
		try {
			//Init hasher
			hasher = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		System.out.println("========MD5 SuperCollider========\nby Cullin Moran\nv" + VERSION + "\n");
		
		//Initial settings
			//Random String length
				System.out.println("What length random Strings should I generate to test with?");
				LENGTH_OF_RANDOM_STRING = scan.nextInt();
				scan.nextLine();
			//Print Status Messages
				PRINT_STATUS = yesNoQuestion("\nShould I print status messages for each hash I generate? (y/n)");
		
		//Get MD5 to find collision for
		System.out.println("\nEnter the MD5 you'd like to find a collision for, " +
				"or enter 't' if you'd like to enter some plain text and hash that:");
		userInput = scan.nextLine();
		
		if(userInput.equalsIgnoreCase("t")) {
			System.out.println("\nOkay, enter your text to hash:");
			userInput = scan.nextLine();
			
			//Hash it
			try {
				inputHash = getHash(userInput.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			System.out.println("\nYour input hashed to:\n" + getHexValue(inputHash));
		}
		else {
			//First, convert the hex String to an array of bytes
			inputHash = getByteArray(userInput);
			
			System.out.println("You entered hash: " + getHexValue(inputHash));
		}
		
		System.out.println("\nWorking...\n");
		
		//Now we can start to generate random values and test them
		boolean collision = false;
		byte[] randomBytes = null;
		while(!collision) {
			String randomString = new String(getRandomString(LENGTH_OF_RANDOM_STRING));
			try {
				randomBytes = getHash(randomString.getBytes("UTF-8"));	//hash it
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			if(PRINT_STATUS) {	//print every try to the console, if requested
				System.out.println("Random input: " + randomString + "\tHashed: " + getHexValue(randomBytes));
			}
			
			if(Arrays.equals(inputHash, randomBytes)) {
				collision = true;
				
				System.out.println("\nWow, I actually found a collision! The random input '" +
						randomString + "' hashed to the same value as your input: " + getHexValue(randomBytes));
				
				try {
					printResults(inputHash, randomBytes, userInput, randomString);
				} catch(FileNotFoundException f) {
					f.printStackTrace();
				}
				
				System.out.println("This information has been saved to 'collision.txt' in this program's directory.");
			}
		}
	}
	
	public static byte[] getHash(byte[] s) {
		return hasher.digest(s);
	}
	
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
			temp = Integer.parseInt(octet, 16);						//Tell Java that this is a hex number
			b[i] = (byte) Integer.parseInt(Integer.toBinaryString(temp), 2);	//Get the binary representation of the hex
			i++;
		}
		return b;
	}
	
	public static List<String> splitString(String hexString, int partSize) {
		//Splits a String into pieces of length partSize
		List<String> parts = new ArrayList<String>();
		int length = hexString.length();
		for(int i=0; i<length; i+=partSize) {
			parts.add(hexString.substring(i, Math.min(length, i + partSize)));
		}
		return parts;
	}
	
	public static String getRandomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		while(sb.length() < length) {
			sb.append(characters[getRandomInt(0, characters.length-1)]);
		}
		return sb.toString();
	}
	
	public static int getRandomInt(int min, int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}
	
	public static boolean yesNoQuestion(String question) {
		//Asks the user a yes/no question.
		boolean satisfactory = false;
		Scanner s = new Scanner(System.in);
		String input;
		do {
			System.out.println(question);
			input = s.nextLine();
			if(input.equalsIgnoreCase("y")) {
				satisfactory = true;
				return true;
			}
			else if(input.equalsIgnoreCase("n")) {
				satisfactory = true;
				return false;
			}
			else {
				System.out.println("Input not appropriate. Try again.");
			}
		}while(!satisfactory);
		return false;
	}
	
	public static synchronized void printResults(byte[] origHash, byte[] randomHash, String origInput, String randomInput) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter("collision.txt");
		String out = "Your original input: ";
		if(origInput.equalsIgnoreCase("t")) {	//If the user entered a hash directly for some reason
			out += "[you chose to enter an MD5 hash directly]";
		} else {
			out += origInput;
		}
		out += "\nOriginal Hash: " + getHexValue(origHash) +
				"\n\nRandomly-generated input: " + randomInput +
				"\nRandom Input Hash: " + getHexValue(randomHash);
		pw.print(out);
		pw.close();
	}
}