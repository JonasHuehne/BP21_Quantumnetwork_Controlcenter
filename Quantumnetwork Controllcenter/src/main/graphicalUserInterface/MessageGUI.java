package graphicalUserInterface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import exceptions.CouldNotSendMessageException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;
import net.miginfocom.swing.MigLayout;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionState;
import networkConnection.ConnectionType;
import networkConnection.MessageArgs;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**This GUI contains a chatLog that visualizes the MessageLog of a connectionEndpoint.
 * It allows for sending plain-text Messages and for sending Files.
 * 
 * @author Jonas Huehne
 *
 */
public class MessageGUI extends JFrame {

	private static final long serialVersionUID = 8618837113863970672L;

	private String connectionID;
	
	private JPanel contentPane;
	
	private JTextPane chatLogTextPane;

	/** Used for chat refreshing */
	private int loggedMessagesAmount = 0;
	/** Used for chat refreshing, specifically, logging received files */
	private int loggedFilesAmount = 0;

	private static Log log = new Log(MessageGUI.class.getName(), LogSensitivity.WARNING);
	
	/**
	 * Create the frame.
	 */
	public MessageGUI(String connectionID) {
		this.connectionID = connectionID;
		setTitle("Message Log");
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
		String labelTitle = fromParty + " <-> " + toParty;
		JLabel commNamesLabel = new JLabel(labelTitle);
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
		messageTextArea.setLineWrap(true);
		controlSplitPane.setLeftComponent(messageTextArea);
		
		JButton sendMessageButton = new JButton("Send Message");
		sendMessageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Check for removed CE
				if(MessageSystem.conMan.getConnectionEndpoint(connectionID) == null) {
					new GenericWarningMessage("Warning: You are trying to send a Message on a Connection that no longer exists!");
					messageTextArea.setText("");
					return;
				}
				
				//Check for illegal State
				if((MessageSystem.conMan.getConnectionEndpoint(connectionID).reportState() != ConnectionState.CONNECTED) && (MessageSystem.conMan.getConnectionEndpoint(connectionID).reportState() != ConnectionState.GENERATING_KEY)) {
					new GenericWarningMessage("Warning: You can only send a Message if the Connection either has State CONNECTED or GENERATING_KEY! The current State is: " + MessageSystem.conMan.getConnectionEndpoint(connectionID).reportState());
					messageTextArea.setText("");
					return;
				}
				
				//Check for empty Text
				String msg = messageTextArea.getText();
				if(msg == null||msg.equals("")) {
					new GenericWarningMessage("Warning: you can not send an empty Message!");
				}

				try {
					switch(QuantumnetworkControllcenter.guiWindow.conType.get(connectionID)) {
					case AUTHENTICATED:
						if (!QuantumnetworkControllcenter.authentication.existsValidKeyPair()) {
							new GenericWarningMessage("Warning: an authenticated message cannot be sent without a valid signature key pair."
									+ " Message will not be sent.");
							break;
						}
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
					MessageSystem.conMan.getConnectionEndpoint(connectionID).appendMessageToChatLog(true, 0, msg);
				} catch (CouldNotSendMessageException e1) {
					new GenericWarningMessage("ERROR - Could not send message to connection: " + connectionID + ". " + e1.getMessage());
					log.logWarning("WARNING - Could not send message to connection: " + connectionID + ".", e1);
					// TODO (potentially): This Exception typically wraps a lower exception. Potentially use getCause() and display different error messages depending on the exact cause
				} 
				
				messageTextArea.setText("");
				
			}
		});
		buttonSplitPane.setLeftComponent(sendMessageButton);
		
		JButton sendFileButton = new JButton("Send File");
		sendFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Check for removed CE
				if(MessageSystem.conMan.getConnectionEndpoint(connectionID) == null) {
					new GenericWarningMessage("Warning: You are trying to send a File on a Connection that no longer exists!");
					messageTextArea.setText("");
					return;
				}
				
				//Check for illegal State
				if((MessageSystem.conMan.getConnectionEndpoint(connectionID).reportState() != ConnectionState.CONNECTED) && (MessageSystem.conMan.getConnectionEndpoint(connectionID).reportState() != ConnectionState.GENERATING_KEY)) {
					new GenericWarningMessage("Warning: You can only send a File if the Connection either has State CONNECTED or GENERATING_KEY! The current State is: " + MessageSystem.conMan.getConnectionEndpoint(connectionID).reportState());
					return;
				}
				
				final JFileChooser fc = new JFileChooser();
				int choice = fc.showOpenDialog(sendFileButton);
				File f = fc.getSelectedFile();
				if (choice == JFileChooser.APPROVE_OPTION) {
					// If user has approved of sending this file, send it with the currently selected security setting
					try {
						ConnectionType t = QuantumnetworkControllcenter.guiWindow.conType.get(connectionID);
					switch (t) {
						case AUTHENTICATED: MessageSystem.sendFile(connectionID, f, true, true);
							break;
						case ENCRYPTED: MessageSystem.sendEncryptedFile(connectionID, f, true);
							break;
						case UNSAFE: MessageSystem.sendFile(connectionID, f, false, false);
							break;
						default: new GenericWarningMessage("ERROR: Invalid Connection Security Setting selected!");
							return;
						}
						MessageSystem.conMan.getConnectionEndpoint(connectionID).appendMessageToChatLog(true, 0, "Sent the file " + f.toString() + " in " + t + " mode.");
					} catch (CouldNotSendMessageException e1) {
						new GenericWarningMessage("ERROR - Could not send File! An Exception occurred. Please see the log for details.");
					}
				}
				
				
			}
		});
		buttonSplitPane.setRightComponent(sendFileButton);
	}
	
	/**This method refreshes the ChatLog to reflect the latest MessageLog stored in the CE.
	 * 
	 */
	public void refreshMessageLog() {
		ConnectionEndpoint ce = MessageSystem.conMan.getConnectionEndpoint(connectionID);
		if (ce == null) return; // can not update message log for a CE that no longer exists
		if (ce.getChatLog() == null) return; // can not update a log that does not exist
		// log text messages
		ArrayList<SimpleEntry<String, String>> log = ce.getChatLog();
		int logSize = log.size(); // measure this once to prevent desync due to multiple threads
		if (logSize > loggedMessagesAmount) {
			// add each new message to the log
			for (int i = 0; i < logSize - loggedMessagesAmount; i++) {
				SimpleEntry<String, String> msgToLog = log.get(loggedMessagesAmount + i);
				chatLogTextPane.setText(chatLogTextPane.getText() + System.lineSeparator() + msgToLog.getKey() + " : " + msgToLog.getValue());
			}
			loggedMessagesAmount = logSize;
		}

		addReceivedFilesToMessageLog(ce);
	}
	
	/**
	 * Checks if new files have been received on the CE, and if they have, adds an appropriate message to the chat log.
	 * @param ce
	 * 		ConnectionEndpoint for which this is the message GUI
	 */
	private void addReceivedFilesToMessageLog(ConnectionEndpoint ce) {
		// for each received file, add an appropriate chat message
		ArrayList<NetworkPackage> filesLog = ce.getLoggedPackagesOfType(TransmissionTypeEnum.FILE_TRANSFER);
		if (filesLog == null)  {
			return;
		} else {
			if (filesLog.size() > loggedFilesAmount) {
				// add an entry for each new received file to the chat
				for (int i = 0; i < filesLog.size() - loggedFilesAmount; i++) {
					NetworkPackage nextFileToLog = filesLog.get(loggedFilesAmount + i);
					MessageArgs filePackageArgs = nextFileToLog.getMessageArgs();
					
					String securityLevel = "UNKNOWN"; // security level at which the file was sent/received
					if (filePackageArgs.keyIndex() >= 0) {
						securityLevel = ConnectionType.ENCRYPTED.toString();
					} else {
						if (nextFileToLog.getSignature() != null) {
							securityLevel =  ConnectionType.AUTHENTICATED.toString();
						} else {
							securityLevel = ConnectionType.UNSAFE.toString();
						}
					}
		
					String fileName = filePackageArgs.fileName();
					
					String appendToChat = ce.getID() + " : Sent the file " + fileName + " in security mode " + securityLevel + ".";
					
					chatLogTextPane.setText(chatLogTextPane.getText() + System.lineSeparator() + appendToChat);
				}
				loggedFilesAmount = filesLog.size();
			}
		}
	}
	
}
