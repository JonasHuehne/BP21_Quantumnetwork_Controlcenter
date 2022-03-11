package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.w3c.dom.css.RGBColor;

import exceptions.CouldNotSendMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.ManagerHasNoSuchEndpointException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;

import javax.swing.JSplitPane;
import java.awt.FlowLayout;
import net.miginfocom.swing.MigLayout;
import networkConnection.ConnectionState;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;

import javax.swing.JTextPane;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.AbstractMap.SimpleEntry;
import java.awt.event.ActionEvent;

/**This GUI contains a chatLog that visualizes the MessageLog of a connectionEndpoint.
 * It allows for sending plain-text Messages and for sending Files.
 * 
 * @author Jonas Huehne
 *
 */
public class MessageGUI extends JFrame {

	private String connectionID;
	
	private JPanel contentPane;
	
	private JTextPane chatLogTextPane;

	private int loggedMessagesAmount = 0;
	
	/**
	 * Create the frame.
	 */
	public MessageGUI(String connectionID) {
		this.connectionID = connectionID;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
		setBounds(100, 100, 450, 550);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new MigLayout("", "[420.00px]", "[504.00px]"));
		
		JSplitPane mainSplitPane = new JSplitPane();
		mainSplitPane.setResizeWeight(0.9);
		mainSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		contentPane.add(mainSplitPane, "cell 0 0,grow");
		
		JSplitPane topSplitPane = new JSplitPane();
		topSplitPane.setResizeWeight(0.05);
		mainSplitPane.setLeftComponent(topSplitPane);
		topSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		String fromParty = Configuration.getProperty("UserName");
		String toParty = MessageSystem.conMan.getConnectionEndpoint(connectionID).getRemoteName();
		String labelTitel = fromParty + " <-> " + toParty;
		JLabel commNamesLabel = new JLabel(labelTitel);
		commNamesLabel.setHorizontalAlignment(SwingConstants.CENTER);
		topSplitPane.setLeftComponent(commNamesLabel);
		
		JScrollPane chatLogScrollPane = new JScrollPane();
		topSplitPane.setRightComponent(chatLogScrollPane);
		
		chatLogTextPane = new JTextPane();
		chatLogTextPane.setEditable(false);
		chatLogScrollPane.setViewportView(chatLogTextPane);
		
		JSplitPane controlSplitPane = new JSplitPane();
		controlSplitPane.setResizeWeight(1.0);
		mainSplitPane.setRightComponent(controlSplitPane);
		
		JSplitPane buttonSplitPane = new JSplitPane();
		buttonSplitPane.setResizeWeight(0.5);
		buttonSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		controlSplitPane.setRightComponent(buttonSplitPane);
		
		JTextArea messageTextArea = new JTextArea();
		controlSplitPane.setLeftComponent(messageTextArea);
		
		JButton sendMessageButton = new JButton("Send Message");
		sendMessageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String msg = messageTextArea.getText();
				if(msg == null||msg.equals("")) {
					new GenericWarningMessage("Warning: you can not send an empty Message!");
				}
				if(MessageSystem.conMan.getConnectionEndpoint(connectionID).reportState() != ConnectionState.CONNECTED) {
					new GenericWarningMessage("Warning: you can not send Message on a Connection that is not connected!");
				}

				try {
					switch(QuantumnetworkControllcenter.guiWindow.conType.get(connectionID)) {
					case AUTHENTICATED: 
						MessageSystem.sendTextMessage(connectionID, msg, true, true);
						break;
					case ENCRYPTED: 
						MessageSystem.sendEncryptedTextMessage(connectionID, msg, true);
						break;
					case UNSAFE: 
						MessageSystem.sendTextMessage(connectionID, msg, false, false);
						break;
					default: new GenericWarningMessage("ERROR: Invalid Connection Security Setting selected!");
						break;
					}
					MessageSystem.conMan.getConnectionEndpoint(connectionID).appendMessageToChatLog(true, msg);
				} catch (CouldNotSendMessageException e1) {
					new GenericWarningMessage("ERROR - Could not send message to connection: " + connectionID + ". " + e1.getMessage());
					// TODO log the error, for some specific errors maybe throw a specific warning message (getCause() and instanceof)
				} 
				
				messageTextArea.setText("");
				
			}
		});
		buttonSplitPane.setLeftComponent(sendMessageButton);
		
		JButton sendFileButton = new JButton("Send File");
		sendFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO: Add Sending of File here!
				
				new GenericWarningMessage("TODO: Implement SendFile-Functionality!");
				
			}
		});
		buttonSplitPane.setRightComponent(sendFileButton);
	}
	
	/**This method refreshes the ChatLog to reflect the latest MessageLog stored in the CE.
	 * 
	 */
	public void refreshMessageLog() {
		ArrayList<String> log = MessageSystem.conMan.getConnectionEndpoint(connectionID).getChatLog();
		int logSize = log.size(); // measure this once to prevent desync due to multiple threads
		if (logSize > loggedMessagesAmount) {
			// add each new message to the log
			for (int i = 0; i < logSize - loggedMessagesAmount; i++) {
				String messageToLog = log.get(loggedMessagesAmount + i);
				chatLogTextPane.setText(chatLogTextPane.getText() + System.lineSeparator() + messageToLog);
			}
			loggedMessagesAmount = logSize;
		}
	}
	
}
