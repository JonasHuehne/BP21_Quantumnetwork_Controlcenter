import java.util.ArrayList;
import java.util.Random;

public final class HelperMethods {
	
	/**
	 * Used to randomize the whitespace in a string.
	 * @param input
	 * 		any string
	 * @return
	 * 		the same string, but each whitespace will be replaced by 1 to 5 whitespaces 
	 * 		(this means that through randomness, there may be no change sometimes)
	 */
	public static String randomizeWhiteSpace(String input) {
		char[] inputArray = input.toCharArray();
		String output = "";
		Random r = new Random();
		for (char c : inputArray) {
			if(Character.isWhitespace(c)) {
				int randomWhiteSpaceAmount = r.nextInt(5) + 1; // random number between 1 and 5
				output += " ".repeat(randomWhiteSpaceAmount);
			} else {
				output += c;
			}
		}
		return output;
	}
	
	public static String randomizeCapitalization(String input) {
		char[] inputArray = input.toCharArray();
		String output = "";
		Random r = new Random();
		for (char c : inputArray) {
			if(Character.isAlphabetic(c)) {
				boolean uppercase = r.nextBoolean();
				if(uppercase) {
					output += Character.toUpperCase(c);
				} else {
					output += Character.toLowerCase(c);
				}
			} else {
				output += c;
			}
		}
		return output;
	}
	
	public final static String SEMI_RANDOM_STRING_BASE = "xJqwKAsslaA";
	
	/**
	 * Generates a somewhat random String for use as an invalid input.
	 * The output String will always start with {@link #SEMI_RANDOM_STRING_BASE}
	 * but will be extended by bonusLength additional, randomly chosen, characters.
	 * @param bonusLength
	 * 		how many additional characters are to be appended to {@link #SEMI_RANDOM_STRING_BASE} <br>
	 * 		must not be negative <br>
	 * 		if this value is 0, {@link #SEMI_RANDOM_STRING_BASE} is returned regardless of the boolean parameters
	 * @param alphabeticals
	 * 		whether the additional characters may include alphabetical characters
	 * @param numerics
	 * 		whether the additional characters may include numbers
	 * @param separators
	 * 		whether the additional characters may include the symbols "-" and "_"
	 * @return
	 * 		a String as described above
	 */
	public static String somewhatRandomString(int bonusLength, boolean alphabeticals, boolean numerics, boolean separators) {
		
		if(bonusLength == 0) return SEMI_RANDOM_STRING_BASE;
		
		if (bonusLength < 0)
			throw new IllegalArgumentException("Invalid value for the # of additional characters to append. Must not be below 0 but was " + bonusLength);
		if (!(alphabeticals || numerics || separators)) 
			throw new IllegalArgumentException("Can not extend the base string if no characters are allowed to be used.");
		
		char[] alphabet 	= "ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
		char[] numbers  	= "0123456789".toCharArray();
		char[] sepChars 	= "-_".toCharArray();
		
		ArrayList<Character> availableChars = new ArrayList<>();
		
		if (alphabeticals) {
			for (char c : alphabet) {
				availableChars.add(c);
			}
		}
		
		if (numerics) {
			for (char c : numbers) {
				availableChars.add(c);
			}
		}
		
		if (separators) {
			for (char c : sepChars) {
				availableChars.add(c);
			}
		}
		
		StringBuilder out = new StringBuilder(SEMI_RANDOM_STRING_BASE);
		Random r = new Random();
		int availableCharsLength = availableChars.size();
		for (int i = 0; i < bonusLength; i++) {
			char nextRandomChar = availableChars.get(r.nextInt(availableCharsLength));
			out.append(nextRandomChar);
		}
		
		return out.toString();
		
	}

}
