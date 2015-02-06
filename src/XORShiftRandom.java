/**
 * XorShift Random Number Generator.
 *
 * Based on: http://www.javamex.com/tutorials/random_numbers/xorshift.shtml#.VNUw4tlVh7Q
 */
public class XORShiftRandom {

	private long currentValue;

	public XORShiftRandom() {
		//Seed the RNG with the current time upon creation
		currentValue = System.currentTimeMillis();
	}

	/**
	 * Returns a random int between 0 and the given max value.
	 *
	 * @param max Max value.
	 * @return A random int.
	 */
	public int nextInt(final int max) {
		currentValue ^= (currentValue << 21);
		currentValue ^= (currentValue >>> 35);
		currentValue ^= (currentValue << 4);
		int out = (int) currentValue % max;
		return (out < 0) ? -out : out;
	}
}
