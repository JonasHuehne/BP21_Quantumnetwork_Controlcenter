package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CommandParser {

	/**
	 * For a given text command (String), this method finds the {@link Command} matching it. <p>
	 * Regarding case sensitivity: If the input String represents a valid command (e.g. "contacts add [...]") the case
	 * of the <i>command name</i> does not matter ("cOnTactS aDD" is treated the same as "contacts add"), however, for
	 * individual commands the arguments may be case sensitive.
	 * @param input
	 * 		The input String to find a matching command for. <p>
	 * 		Any amount of whitespaces will be treated as one whitespace, leading and trailing whitespaces will be ignored
	 * @return 
	 * 		Returns the Command matching the input String, or null if no such Command exists. <p>
	 * 		e.g. for "help" this method returns {@link #HELP}, and for "kdjnvusdn" it returns null. <p>
	 * 		If the input is null, null is returned.
	 */
	public static Command match(String input) {
		String normedInput = normInput(input);	
		if (normedInput == null) return null;
		for (Command command : Command.values()) {
			if(Pattern.matches(command.getCommandPattern(), normedInput)) {	
				return command;
			}
		}
		return null;
	}
	
	/**
	 * Extracts the arguments from a given text command.
	 * @param input
	 * 		A String corresponding to a {@link Command} <p>
	 * 		Any amount of whitespaces will be treated as one whitespace, leading and trailing whitespaces will be ignored
	 * @return
	 * 		The arguments of the given text command, e.g. for "contacts search Bob" this method would return ["Bob"] <p>
	 * 		Returns an empty array iff the given command has no arguments following it (e.g. "contacts show") <p>
	 * 		Returns null iff {@link #match(String)} would return null for <b>input</b> <p>
	 */
	public static String[] extractArguments(String input) {
		
		Command command = match(input);
		if(command == null) return null;
		
		String argumentString = normInput(input).substring(command.getCommandName().length());
		
		if(argumentString == "") { // If there are no arguments, return empty array
			return new String[]{};
		} else { 
			// Extracts all arguments from the argument string, e.g. "Alice name Bob" becomes ["Alice", "name", "Bob"]
			// This code is probably very inefficient, but it appears to work at least
			String[] argumentsUncleaned = argumentString.split(" ");
			Stream<String> stream = Arrays.stream(argumentsUncleaned);
			ArrayList<String> arrList = new ArrayList<String>();
			stream.forEach(str -> {if(str != "") arrList.add(str);});
			return arrList.toArray(new String[0]);
		}
	}
	
	/**
	 * Norms a string for parsing by removing any leading and trailing whitespaces, 
	 * and replacing all instances of multiple whitespaces with one " " each.
	 * @param input
	 * 		the String to norm
	 * @return 
	 * 		the resulting String
	 */
	public static String normInput(String input) {
		if (input == null) return null;
		// Remove leading & trailing whitespace, and replace any double+ whitespaces with single spaces
		String out = input.trim().replaceAll("\\s+", " ");
		return out;
	}
}
