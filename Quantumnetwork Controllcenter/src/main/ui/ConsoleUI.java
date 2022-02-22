package ui;


import javax.swing.JFrame;
import javax.swing.JTextArea;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Font;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

/**
 * The ConsoleUI class provides a GUI which appears and acts similar to a console / terminal application.
 * The UI displayed to the user consists of two text boxes: a large box and a small box. 
 * The larger text box is for output only, displaying information to the user, 
 * such as notifiyng them that an operation was executed successfully, or displaying error messages.
 * The smaller text box allows the user to input text as they would input commands in a terminal, confirming an input with the ENTER key.
 * Entered text is interpreted as a command and processed via the {@link CommandHandler}. The returned String is then presented in the larger text box.
 * @author Sasha Petri
 *
 */
public class ConsoleUI {
	
	private JFrame frmQuantumNetworkControl;
	
	/** Title displayed in the applications border */
	private static final String APPLICATION_TITLE = "Quantum Network Control Center Console UI";
	/** Text displayed on startup in the {@link #consoleOutArea} */
	private static final String INITIAL_TEXT = "Welcome to the Quantum Network Control Center. What would you like to do?" + System.lineSeparator() + "Enter \"help\" for a list of commands.";
	
	/** The text area for the user input (commands) */
	private JTextField consoleInArea;
	
	/** For convenience purposes we save the last entered commands in a list, which the user can cycle through by pressing UP and DOWN */
	private LinkedList<String> enteredCommands = new LinkedList<>();
	/** This index is used to cycle through the list of entered commands {@link #enteredCommands} */
	private int commandIndex = 0;
	private JScrollPane scrollPane;
	private JTextArea consoleOutArea;
	
	/**
	 * Create the application.
	 */
	public ConsoleUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmQuantumNetworkControl = new JFrame();
		frmQuantumNetworkControl.setTitle(APPLICATION_TITLE);
		frmQuantumNetworkControl.setBounds(100, 100, 1440, 720);
		frmQuantumNetworkControl.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// The area in which the user enters the commands
		consoleInArea = new JTextField();
		consoleInArea.setCaretColor(Color.GREEN);
		consoleInArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
		consoleInArea.setForeground(Color.GREEN);
		consoleInArea.setBackground(Color.BLACK);
		consoleInArea.setBorder(javax.swing.BorderFactory.createEmptyBorder());
		frmQuantumNetworkControl.getContentPane().add(consoleInArea, BorderLayout.SOUTH);
		
		// To allow scrolling the text area, for overly long outputs (e.g. output of some help commands)
		scrollPane = new JScrollPane();
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane.getHorizontalScrollBar().setBackground(Color.BLACK);
		scrollPane.getVerticalScrollBar().setBackground(Color.BLACK);
		scrollPane.setBackground(Color.BLACK);
		frmQuantumNetworkControl.getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		consoleOutArea = new JTextArea();
		consoleOutArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) { 
				// If the user has clicked on the output (e.g. to select some text)
				// and then types again, put their focus back on the consoleInArea
				consoleInArea.requestFocusInWindow();
				// transfer the key press into the console in area (hacky, not sure if this is proper)
				try {
					Robot r = new Robot();
					r.keyPress(e.getKeyCode());
				} catch (AWTException e1) {
					// If the key press can't be faked, do nothing for now.
					// TODO if an error logger is implemented, log this
				}
			}
		});
		consoleOutArea.setAutoscrolls(false);
		consoleOutArea.setBorder(new EmptyBorder(0, 0, 0, 0));
		consoleOutArea.setWrapStyleWord(true);
		consoleOutArea.setText("Welcome to the Quantum Network Control Center. What would you like to do?\r\nEnter \"help\" for a list of commands.");
		consoleOutArea.setLineWrap(true);
		consoleOutArea.setForeground(Color.GREEN);
		consoleOutArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
		consoleOutArea.setEditable(false);
		consoleOutArea.setBackground(Color.BLACK);
		scrollPane.setViewportView(consoleOutArea);

		
		consoleInArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) { // Attempt to parse entered command if ENTER key is pressed
					String enteredCommand = consoleInArea.getText(); 
					enteredCommands.addFirst(enteredCommand);
					consoleOutArea.setText(CommandHandler.processCommand(enteredCommand));			
					consoleInArea.setText("");
					commandIndex = 0;
				} else if(e.getKeyCode() == KeyEvent.VK_UP) { // If the user presses UP, replace the input area text with the previously entered command
					if(enteredCommands.size() > commandIndex) {
						consoleInArea.setText(enteredCommands.get(commandIndex));
						if(commandIndex + 1 != enteredCommands.size()) commandIndex++; // If list has n elements, index may at most be (n-1)
					} 
				} else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
					if (commandIndex > 0) {
						commandIndex--;
						consoleInArea.setText(enteredCommands.get(commandIndex));
					}
				}
			}
		});
		
		
		frmQuantumNetworkControl.setVisible(true);
		consoleInArea.requestFocusInWindow(); // This makes it so that the user automatically types in the input field on startup of the console

	}
	
}
