package ui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Font;

/**
 * A simple console-based UI that allows text commands to be entered and processed, and text to be displayed to the user.
 * @author Sasha Petri
 *
 */
public class ConsoleUI {

	private JFrame frmQuantumNetworkControl;
	
	private final String ENTER_COMMAND_TEXT = "Enter Command: ";

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ConsoleUI window = new ConsoleUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

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
		frmQuantumNetworkControl.setTitle("Quantum Network Control Center Console UI");
		frmQuantumNetworkControl.setBounds(100, 100, 677, 403);
		frmQuantumNetworkControl.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// The area in which the user enters the commands
		JTextField consoleInArea = new JTextField();
		consoleInArea.setFont(new Font("Arial", Font.PLAIN, 14));
		consoleInArea.setText(ENTER_COMMAND_TEXT);
		consoleInArea.setForeground(Color.GREEN);
		consoleInArea.setBackground(Color.BLACK);
		consoleInArea.setBorder(javax.swing.BorderFactory.createEmptyBorder());
		frmQuantumNetworkControl.getContentPane().add(consoleInArea, BorderLayout.SOUTH);
		
		// Output area containing result of computing the command
		JTextArea consoleOutArea = new JTextArea();
		consoleOutArea.setFont(new Font("Arial", Font.PLAIN, 14));
		consoleOutArea.setEditable(false);
		consoleOutArea.setText("Welcome to the Quantum Network Control Center. What would you like to do?" + System.lineSeparator() + "Enter \"help\" for a list of commands.");
		consoleOutArea.setForeground(Color.GREEN);
		consoleOutArea.setBackground(Color.BLACK);
		frmQuantumNetworkControl.getContentPane().add(consoleOutArea, BorderLayout.CENTER);

		
		consoleInArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) { // Attempt to parse entered command if ENTER key is pressed
					String enteredCommand = consoleInArea.getText().substring(ENTER_COMMAND_TEXT.length()); 
					consoleOutArea.setText(parseCommand(enteredCommand));			
					consoleInArea.setText(ENTER_COMMAND_TEXT);
				}
			}
		});
		
		
		frmQuantumNetworkControl.setVisible(true);
		consoleInArea.requestFocusInWindow(); // This makes it so that the user automatically types in the input field on startup of the console

	}
	
	// TODO: Should probably rename this method, not sure whether "parse" is the correct terminology to use here
	// TODO: JavaDoc
	/* TODO: Actually implement any commands. For reasons of modularity & cohesion, do only as much as "neccessary" in this class, 
	 * 		 i.e. when a command is entered, this class should at most handle the parameters of the command and then call the appropriate method
	 * 		 e.g. KeyManager.deleteKeys(...) or whatever other method is appropriate to call for the entered command
	*/
	private String parseCommand(String command) {
		
		switch (command) {
			case "help": return "Sorry, at the moment there is no help available.";
			default:
				return "Unrecognized Command.";
		}
	}
	
}
