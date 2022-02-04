package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CommandParser {

	/**
	 * For a given text command (String), this method finds the {@link Command} whose syntax is exactly matched by the text command. <br>
	 * Regarding case sensitivity: If the input String represents a valid command (e.g. "contacts add [...]") the case
	 * of the <i>command name</i> does not matter ("cOnTactS aDD" is treated the same as "contacts add"), however, for
	 * individual commands the arguments may be case sensitive.
	 * @param textCommand
	 * 		The input String to find a matching command for. <br>
	 * 		Any amount of whitespaces will be treated as one whitespace, leading and trailing whitespaces will be ignored.
	 * @return 
	 * 		If the input string fits the syntax ({@link Command#getCommandPattern()}) of a Command exactly, that Command is returned,
	 * 		e.g. for "help" this method returns {@link Command#HELP} and for "contacts add Annie 127.0.0.1 1234" it returns {@link Command#CONTACTS_ADD} <br>
	 * 		If the input string does not fit the syntax of a Command exactly, then null is returned,
	 * 		e.g. the input string is "xjcvnxvjn" or it is "contacts add Annie" (incomplete syntax for {@link Command#CONTACTS_ADD}) <br>
	 * 		If the input is null, null is returned.
	 */
	public static Command match(final String textCommand) { 
		String normedInput = normInput(textCommand);	
		if (normedInput == null) return null;
		for (Command command : Command.values()) {
			if(Pattern.matches(command.getCommandPattern(), normedInput)) {	
				return command;
			}
		}
		return null;
	}
	
	/**
	 * For a given text command (String) this method finds the {@link Command} whose name matches the text command. <br>
	 * @param commandName
	 * 		The string to check
	 * 		Regardless of <b>strict</b> this String is not case sensitive, any leading and trailing whitespace is ignored, and multiple whitespaces are treated as one
	 * @param strict
	 * 		If strict is true, commandName will have to be <i>only</i> the name of a valid Command, 
	 * 		i.e. "contacts add" would return {@link Command#CONTACTS_ADD}, but "contacts add Jimmy" would return null <br>
	 * 		If strict is false this method will check all Commands, and return the first Command whose name is the start of the input String commandName, 
	 * 		i.e. "contacts add Jimmy" would return "contacts add", "help ewemfw" would return "help", but "hlep" would still return null
	 * @return
	 * 		The Command with the name given by <b>commandName</b>, with strictness decided as above.
	 * 		null if there is no such command, or if commandName is null
	 */
	public static Command getCommandOfName(final String commandName, final boolean strict) {
		if (commandName == null) return null;
		String normedInput = normInput(commandName).toLowerCase();
		for(Command c : Command.values()) {
			if(strict) {
				if(Pattern.matches(c.getCommandName(), normedInput)) return c;
			} else {
				if(Pattern.matches(c.getCommandName() + "( .*)?", normedInput)) return c;
			}
		}
		return null;
	}
	
	/**
	 * Extracts the arguments from a given text command.
	 * @param input
	 * 		A String corresponding to a {@link Command}, including its arguments (e.g. "help contacts add") <br>
	 * 		Any amount of whitespaces will be treated as one whitespace, leading and trailing whitespaces will be ignored
	 * @return
	 * 		The arguments of the given text command, e.g. for "contacts search Bob" this method would return ["Bob"] <br>
	 * 		Returns an empty array iff the given command has no arguments following it (e.g. "contacts show") <br>
	 * 		Returns null iff {@link #match(String)} would return null for <b>input</b>
	 */
	public static String[] extractArguments(final String input) {
		
		Command command = match(input);
		if(command == null) return null;
		
		// remove the command itself from the command string
		String argumentString = normInput(input).substring(command.getCommandName().length());
		
		if(argumentString.isBlank()) { // If there are no arguments, return empty array
			return new String[]{};
		} else { 
			// Extracts all arguments from the argument string, e.g. "Alice name Bob" becomes ["Alice", "name", "Bob"]
			// This code is probably very inefficient, but it appears to work at least
			String[] argumentsUncleaned = argumentString.split(" "); // this array contains a few entries of form "", which we don't want
			Stream<String> stream = Arrays.stream(argumentsUncleaned);
			ArrayList<String> arrList = new ArrayList<String>(); // this list will contain only the non-empty strings, and then be converted to an array
			stream.forEach(str -> {if(!str.isBlank()) arrList.add(str);});
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
	public static String normInput(final String input) {
		if (input == null) return null;
		// Remove leading & trailing whitespace, and replace any double+ whitespaces with single spaces
		String out = input.replaceAll("\\s+", " ").strip();
		return out;
	}
}
