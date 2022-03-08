package graphicalUserInterface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import exceptions.EndpointIsNotConnectedException;
import exceptions.ManagerHasNoSuchEndpointException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;
import net.miginfocom.swing.MigLayout;
import networkConnection.ConnectionState;
import networkConnection.TransmissionTypeEnum;

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
		String labelTitle = fromParty + " <-> " + toParty;
		JLabel commNamesLabel = new JLabel(labelTitle);
		commNamesLabel.setHorizontalAlignment(SwingConstants.CENTER);
		topSplitPane.setLeftComponent(commNamesLabel);
		
		JScrollPane chatLogScrollPane = new JScrollPane();
		topSplitPane.setRightComponent(chatLogScrollPane);
		
		chatLogTextPane = new JTextPane();
		chatLogTextPane.setEditable(false);
		chatLogTextPane.setText(MessageSystem.conMan.getConnectionEndpoint(connectionID).getMessageLog());
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
					case AUTHENTICATED: MessageSystem.sendAuthenticatedMessage(connectionID, msg);
						break;
					case ENCRYPTED: MessageSystem.sendEncryptedMessage(connectionID, msg);
						break;
					case UNSAFE: MessageSystem.sendMessage(connectionID, TransmissionTypeEnum.TRANSMISSION, "", msg, null);
						break;
					default: new GenericWarningMessage("ERROR: Invalid Connection Security Setting selected!");
						break;
					}
				} catch (ManagerHasNoSuchEndpointException e1) {
					new GenericWarningMessage("ERROR - Could not send message to connection: " + connectionID + ". No such connection exists.");
				} catch (EndpointIsNotConnectedException e1) {
					new GenericWarningMessage("ERROR - Could not send message to connection: " + connectionID + ". You are not connected.");
				}
				
				logSentMessage(msg);
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
		MessageSystem.conMan.getConnectionEndpoint(connectionID).setLogGUI(this);
	}

	/**This logs any messages that have been sent from the local CE.
	 * 
	 * @param msg the message content that will be added to the log.
	 */
	private void logSentMessage(String msg) {
		MessageSystem.conMan.getConnectionEndpoint(connectionID).logMessage(MessageSystem.conMan.getConnectionEndpoint(connectionID).getMessageLog() + "\n" + "You wrote: \n" + msg);
		refreshMessageLog();
	}
	
	/**This method refreshes the ChatLog to reflect the latest MessageLog stored in the CE.
	 * 
	 */
	public void refreshMessageLog() {
		chatLogTextPane.setText(MessageSystem.conMan.getConnectionEndpoint(connectionID).getMessageLog());
	}
}
