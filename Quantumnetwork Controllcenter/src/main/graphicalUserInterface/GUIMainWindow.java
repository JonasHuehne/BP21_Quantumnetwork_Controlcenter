package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import communicationList.CommunicationList;
import communicationList.Contact;
import exceptions.EndpointIsNotConnectedException;
import exceptions.KeyGenRequestTimeoutException;
import exceptions.ManagerHasNoSuchEndpointException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import graphicalUserInterface.keyStoreEditor.DebugKeystoreEditor;
import keyStore.KeyStoreDbManager;
import messengerSystem.MessageSystem;
import messengerSystem.SigKeyQueryInteractionObject;
import messengerSystem.Utils;
import net.miginfocom.swing.MigLayout;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionState;
import networkConnection.ConnectionType;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**This is the Main GUI of this Application. The left half handles the ContactDB and the right half handles the Connections.
 * @implNote We encourage the use of a tool like WindowBuilder when making changes to this and other GUI classes.
 * @author Jonas Huehne, Sasha Petri
 *
 */
public final class GUIMainWindow implements Runnable{
	
	Log guiLogger;


	private Object[][] contactData = {};
	String[] contactColumnNames = {"Connection Name",
            "IP Address",
            "Target Port",
            "Public Signature Key"};
	
	private CustomClosingFrame frame;
	private JTable contactTable;
	private Box connectionEndpointVerticalBox;
	private HashMap<String, JPanel> representedConnectionEndpoints = new HashMap<String, JPanel>();
	private String activeConnection;
	private Thread ceUpdateThread;
	String prevActiveConnection;

	/** column of the contacts table in which the names are listed */
	private final int contactDBNameIndex = 0;
	/** column of the contacts table in which the IPs are listed */
	private final int contactDBIPIndex = 1;
	/** column of the contacts table in which the ports are listed */
	private final int contactDBPortIndex = 2;
	/** column of the contacts table in which the public keys are listed */
	private final int contactDBPubSigKeyIndex = 3;
	
	public HashMap<String,ConnectionType> conType = new HashMap<String,ConnectionType>();
	protected ArrayList<MessageGUI> openChatWindows = new ArrayList<MessageGUI>();

	
	/**
	 * Create the application.
	 */
	public GUIMainWindow() {
		guiLogger = new Log(GUIMainWindow.class.getSimpleName(), LogSensitivity.WARNING);
		initialize();
		startUpdateService();

		if(!QuantumnetworkControllcenter.authentication.existsValidKeyPair()) {
			GenericWarningMessage keyWarning = new GenericWarningMessage("No valid own signature keys set. Please generate a new pair in the Settings window.");
			// new GenericWarningMessage("No valid own signature keys set. Please generate a new pair in the Settings window, or refer to the user guide for setting a different key pair.");
			keyWarning.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			keyWarning.setVisible(true);
			keyWarning.toFront();
			keyWarning.setAlwaysOnTop(true);
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new CustomClosingFrame();
		getFrame().setBounds(100, 100, 1120, 567);
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getFrame().setTitle("QNCC");
		frame.getContentPane().setLayout(new MigLayout("", "[1088.00px]", "[][528px][]"));
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		frame.getContentPane().add(toolBar, "flowx,cell 0 0,alignx left,aligny top");
		
		JButton settingsButton = new JButton("Settings");
		settingsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					SettingsDialog settings = new SettingsDialog();
					settings.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					settings.setVisible(true);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		settingsButton.setToolTipText("Opens the Application Settings.");
		toolBar.add(settingsButton);
		
		JButton helpButton = new JButton("Help");
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String helpPath  = Configuration.getBaseDirPath() + File.separator + "Documentation" + File.separator;
				try {
					Desktop.getDesktop().open(new File(helpPath));
				} catch (IOException e1) {
					guiLogger.logWarning("Error while attempting to open the Folder containing the Documentation files. Folder Path: " + helpPath, e1);
					new GenericWarningMessage("Error while attempting to open the Folder containing the Documentation files. Folder Path: " + helpPath);
					e1.printStackTrace();
				}
			}
		});
		helpButton.setToolTipText("Opens the Help Screen.");
		toolBar.add(helpButton);
		
		JButton aboutButton = new JButton("About");
		aboutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new AboutPage();
			}
		});
		aboutButton.setToolTipText("About this Application");
		toolBar.add(aboutButton);

		JPanel contactsOuterPanel = new JPanel();
		contactsOuterPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Contacts", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
		frame.getContentPane().add(contactsOuterPanel, "flowx,cell 0 1,alignx left,growy");
		contactsOuterPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel contactsInnerPanel = new JPanel();
		contactsOuterPanel.add(contactsInnerPanel, BorderLayout.WEST);
		contactsInnerPanel.setLayout(new BoxLayout(contactsInnerPanel, BoxLayout.X_AXIS));
		
		Box contactsColumn = Box.createVerticalBox();
		contactsInnerPanel.add(contactsColumn);
		
		JPanel contactControlPanel = new JPanel();
		contactsColumn.add(contactControlPanel);
		
		JButton contactsAddButton = new JButton("Add new Contact");
		contactsAddButton.setToolTipText("Add a new contact to the \"Contacts\"-Table.");
		contactsAddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					ContactAddDialog dialog = new ContactAddDialog();
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		contactControlPanel.add(contactsAddButton);
		
		JButton removeContactButton = new JButton("Remove Contact");
		removeContactButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				int rowIndex = contactTable.getSelectedRow();
				String id = (String) contactTable.getValueAt(rowIndex, contactDBNameIndex);
				guiLogger.logInfo("Deleting Contact Entry! " + id);
				QuantumnetworkControllcenter.communicationList.delete(id);
				DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
				model.removeRow(rowIndex);
			}
		});
		removeContactButton.setToolTipText("Removes a row from the \"Contacts\"-Table.");
		contactControlPanel.add(removeContactButton);

		JButton contactRefreshButton = new JButton("Refresh Table");
		contactRefreshButton.setToolTipText("Forces the contacts table shown below to update by re-querying the database. This can be used after modifying the contacts database.");
		contactRefreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearContacts();
				gatherContacts();
			}
		});
		contactControlPanel.add(contactRefreshButton);

		JButton saveChangesButton = new JButton("Save changes to DB");
		saveChangesButton.setToolTipText("Saves the changes made in the table to the contacts database.");
		saveChangesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Delete all Entries in the DB first.
				CommunicationList cl = QuantumnetworkControllcenter.communicationList;
				ArrayList<Contact> dbContent = cl.queryAll();
				for (Contact c : dbContent) cl.delete(c.getName());
		
				//Create new DBEntries form JTable
				int rowCount = contactTable.getRowCount();
				for(int i = 0; i < rowCount; i++) {
					String name = (String) contactTable.getValueAt(i, contactDBNameIndex);
					String ip = (String) contactTable.getValueAt(i, contactDBIPIndex);
					String portString = String.valueOf(contactTable.getValueAt(i, contactDBPortIndex));
					int port = Integer.valueOf(portString);
					String sig = (String) contactTable.getValueAt(i, contactDBPubSigKeyIndex);
					cl.insert(name, ip, port, sig);
				}
				
				
				
			}
		});
		contactControlPanel.add(saveChangesButton);
		
		JScrollPane contactScrollPane = new JScrollPane();
		contactScrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
		contactsColumn.add(contactScrollPane);
		contactTable = new JTable(new DefaultTableModel(contactColumnNames,0));
		gatherContacts();
		contactScrollPane.setViewportView(contactTable);

		JPanel connectionsOuterPanel = new JPanel();
		connectionsOuterPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Connections", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
		frame.getContentPane().add(connectionsOuterPanel, "cell 0 1,grow");
		connectionsOuterPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel connectionsInnerPanel = new JPanel();
		connectionsOuterPanel.add(connectionsInnerPanel, BorderLayout.CENTER);
		connectionsInnerPanel.setLayout(new BoxLayout(connectionsInnerPanel, BoxLayout.X_AXIS));
		
		Box verticalBox = Box.createVerticalBox();
		verticalBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		connectionsInnerPanel.add(verticalBox);
		
		JPanel connectionsInnerPanel2 = new JPanel();
		verticalBox.add(connectionsInnerPanel2);
		connectionsInnerPanel2.setLayout(new BorderLayout(0, 0));
		
		JPanel connectionButtonsPanel = new JPanel();
		connectionsInnerPanel2.add(connectionButtonsPanel, BorderLayout.NORTH);
		
		JButton createConnectionButton = new JButton("Establish Connection");
		createConnectionButton.setToolTipText("Creates a new Connection-Endpoint, and immediately attempts to create a connection to the target.");
		connectionButtonsPanel.add(createConnectionButton);
		createConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					ConnectionAddDialog dialog = new ConnectionAddDialog();
					dialog.fillTextFieldsWithSelectedContactInfo();
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		JButton closeConnectionButton = new JButton("Close Connection");
		closeConnectionButton.setToolTipText("Closes the active Connection.");
		closeConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (activeConnection == null) { // Possibly display an error message here?
					return;
				}
				removeCEEntry(activeConnection);
				activeConnection = null;
				
			}
		});
		connectionButtonsPanel.add(closeConnectionButton);
		
		JButton generateKeyButton = new JButton("Generate Key");
		connectionButtonsPanel.add(generateKeyButton);
		generateKeyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(activeConnection == null) {
					guiLogger.logWarning("Warning: No Connection selected as active.");
					return;
				}

				try {
					if(QuantumnetworkControllcenter.conMan.getConnectionState(activeConnection) == ConnectionState.CONNECTED) {
						try {
							QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).getKeyGen().generateKey();
						} catch (NumberFormatException e1) {
							new GenericWarningMessage("ERROR - Could not generate key! The value stored as the port of the photon source is not an Integer!" + e1);
						} catch (KeyGenRequestTimeoutException e1) {
							new GenericWarningMessage("A timeout occurred while trying to generate a key with the specified connection." + e1);
						} catch (EndpointIsNotConnectedException e1) { // control flow wise, this should not occur, but I'd rather not have an empty catch here
							new GenericWarningMessage("ERROR - Could not generate key! The endpoint with id " + activeConnection + " is not connected!" + e1);
						}
					}else {
						guiLogger.logWarning("Warning: Active Connection is not connected to anything!");
						return;
					}
				} catch (ManagerHasNoSuchEndpointException e1) {
					new GenericWarningMessage("ERROR - Could not remove connection: " + activeConnection + ". No such connection exists." + e1);
				}
				
			}
		});
		generateKeyButton.setToolTipText("This will start the key generation with the selected connection.");
		
		JButton buttonDebugKeystoreEditor = new JButton("Key-DB Editor");
		connectionButtonsPanel.add(buttonDebugKeystoreEditor);
		buttonDebugKeystoreEditor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DebugKeystoreEditor dialog = new DebugKeystoreEditor();
				dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		
		JPanel panelForConnectionList = new JPanel();
		connectionsInnerPanel2.add(panelForConnectionList, BorderLayout.CENTER);
		panelForConnectionList.setLayout(new BoxLayout(panelForConnectionList, BoxLayout.X_AXIS));
		
		Box verticalBoxForConnections = Box.createVerticalBox();
		panelForConnectionList.add(verticalBoxForConnections);
		
		JScrollPane scrollPane = new JScrollPane();
		verticalBoxForConnections.add(scrollPane);
		
		connectionEndpointVerticalBox = Box.createVerticalBox();
		scrollPane.setViewportView(connectionEndpointVerticalBox);
		
		//Debug Buttons Commented out
		/*
		JButton connectionDebug = new JButton("Debug Button 1");
		connectionDebug.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ConnectionManager cm = QuantumnetworkControllcenter.conMan;
				System.out.println("Conman Size: " + cm.getConnectionsAmount());
				System.out.println("Entries: ");
				for (Map.Entry<String, ConnectionEndpoint> entry : cm.returnAllConnections().entrySet()) {
					System.out.println(" " + entry.getKey() + " with state " + entry.getValue().reportState());
				}
				System.out.println("namesOfConnections size: " + namesOfConnections.size());
				System.out.println("Entries: ");
				for (String n : namesOfConnections) System.out.println(" " + n);
			}
		});
		connectionDebug.setToolTipText("Used for debugging purposes by the developers. Displays some information about connections to the console.");
		frame.getContentPane().add(connectionDebug, "cell 0 0");
		
		JButton debugButton2 = new JButton("Debug Button 2 (CE Info)");
		debugButton2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (activeConnection != null) {
					ConnectionEndpoint active = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection);
					StringBuilder info = new StringBuilder();
					info.append("ID: " 				+ active.getID() + System.lineSeparator());
					info.append("Remote Name: " 	+ active.getRemoteName() + System.lineSeparator());
					info.append("PK: " 				+ active.getSigKey() + System.lineSeparator());
					info.append("KeyID: " 			+ active.getKeyStoreID() + System.lineSeparator());
					try {
						info.append("Keystore has an entry for this keyID == " 
									+ KeyStoreDbManager.doesKeyStreamIdExist(active.getKeyStoreID())
									+ System.lineSeparator());
					} catch (SQLException e1) {
						info.append("Can not reach Keystore. SQL Exception." + System.lineSeparator());
					}
					new GenericWarningMessage(info.toString(), 600, 300);
				}
			}
		});
		debugButton2.setToolTipText("Displays some information about the currently selected CE.");
		frame.getContentPane().add(debugButton2, "cell 0 0");
		*/
	}

	public JFrame getFrame() {
		return frame;
	}
	
	public JTable getContactTable() {
		return contactTable;
	}
	
	public int getContactDBNameIndex() {
		return contactDBNameIndex;
	}
	
	public int getContactDBIPIndex() {
		return contactDBIPIndex;
	}
	
	public int getContactDBPortIndex() {
		return contactDBPortIndex;
	}

	public int getContactDBPubSigKeyIndex() {
		return contactDBPubSigKeyIndex;
	}
	
	
	/**This method gathers all rows from the contact DB and translates them into JTable Rows.
	 * 
	 */
	private void gatherContacts() {
		ArrayList<Contact> dbEntries = QuantumnetworkControllcenter.communicationList.queryAll();
		
		Object[][]tmpContactData = new Object[dbEntries.size()][4];
		for(int i = 0; i < dbEntries.size(); i++){

			String name = dbEntries.get(i).getName();
			String ip = dbEntries.get(i).getIpAddress();
			int port = dbEntries.get(i).getPort();
			String sig = dbEntries.get(i).getSignatureKey();
			
			tmpContactData[i] = new Object[]{name, ip, port, sig};
		}
		contactData = tmpContactData;
		DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
		for(int i = 0; i < contactData.length; i++) {
			model.addRow(contactData[i]);		
		}
		
	}
	
	/**This Method clears all Lines from the JTable.
	 * 
	 */
	private void clearContacts() {
		DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
		int rc = model.getRowCount();
		for(int i = rc - 1; i >= 0; i--) {
			model.removeRow(i);
		}
	}
	
	/**This Method adds a single Row to the JTable.
	 * 
	 * @param name the connectionName
	 * @param ip	the targetIP
	 * @param port	the targetPort
	 * @param sig	the public signature of the connectionPartner
	 */
	void addRowToContactTable(String name, String ip, int port, String sig) {
		
		DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
		if(QuantumnetworkControllcenter.communicationList.insert(name, ip, port, sig)) {
			model.addRow(new Object[]{name, ip, port, sig});
		} else {
			new GenericWarningMessage("Contact could not be added. Please refer to the log for details");
		}
	}
	
	/**This Method adds a set of SwingItems that allow the user to interact with the ConnectionEndpoint.
	 * 
	 * @param connectionName	the name of the CE
	 * @param targetIP	the targetIP of the CE
	 * @param targetPort	the targetPort of the CE
	 */
	private void createConnectionRepresentation(String connectionName, String targetIP, int targetPort) {
	
		JPanel ceFrame = new JPanel();
		ceFrame.setBorder(new LineBorder(new Color(0, 0, 0)));
		ceFrame.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		connectionEndpointVerticalBox.add(ceFrame);

		
		JLabel connectionNameLabel = new JLabel(connectionName + " - " + targetIP + " - " + targetPort);
		ceFrame.add(connectionNameLabel);
		
		Component horizontalStrut = Box.createHorizontalStrut(20);
		ceFrame.add(horizontalStrut);
		
		JLabel lblNewLabel = new JLabel("New label");
		ceFrame.add(lblNewLabel);
		
		Component horizontalStrut1 = Box.createHorizontalStrut(20);
		ceFrame.add(horizontalStrut1);
		
		JButton selectConnectionButton = new JButton("Select");
		selectConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					prevActiveConnection = activeConnection;
					activeConnection = connectionName;
					guiLogger.logInfo("Changed Active Connection to: " + activeConnection);
				} catch (Exception e1) {
					guiLogger.logError("Error: Unable to change selected Connection", e1);
				}
			}
		});
		ceFrame.add(selectConnectionButton);
		
		Component horizontalStrut2 = Box.createHorizontalStrut(5);
		ceFrame.add(horizontalStrut2);
		
		JComboBox<ConnectionType> connectionTypeCB = new JComboBox<ConnectionType>();
		connectionTypeCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				conType.put(connectionName, (ConnectionType) connectionTypeCB.getSelectedItem());
				
				//Check if Key exists
				if(connectionTypeCB.getSelectedItem() == ConnectionType.ENCRYPTED) {
					try {
						if (!(KeyStoreDbManager.doesKeyStreamIdExist(connectionName))) {
							new GenericWarningMessage("Warning: no valid Key was found for " + connectionName + "! Please generate a Key before using encrypted communication.");
							connectionTypeCB.setSelectedItem(ConnectionType.AUTHENTICATED);
						}
					} catch (SQLException e1) {
						new GenericWarningMessage("SQL Exception while trying to get the key for " + connectionName + ". Encrypted communication is not possible.");
						guiLogger.logError("SQL Exception while trying to get the key for " + connectionName + ". Encrypted communication is not possible.", e1);
					}
				}
				if(connectionTypeCB.getSelectedItem() == ConnectionType.AUTHENTICATED && Utils.getPkIfPossible(connectionName) == null) {
					SigKeyQueryInteractionObject skq = new SigKeyQueryInteractionObject();
	                new CESignatureQueryDialog(connectionName, skq, true);
				}
			}
		});
		connectionTypeCB.setModel(new DefaultComboBoxModel<ConnectionType>(ConnectionType.values()));
		connectionTypeCB.setToolTipText("This determines what kind of message you can send. It has no influence on what you receive. UNSAFE: plain-text message; AUTH: Signed message, receiver needs to know the sig-key of the sender; ENC: The message is encrypted. This requires a Key to exist and still have at least 256 unused bits left.");
		ceFrame.add(connectionTypeCB);
		conType.put(connectionName, (ConnectionType) connectionTypeCB.getSelectedItem());
		
		Component horizontalStrut3 = Box.createHorizontalStrut(5);
		ceFrame.add(horizontalStrut3);
		
		JButton openTransferButton = new JButton("Message System");
		openTransferButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openChatWindows.add(new MessageGUI(connectionName)); 
				// add opened chat to list, so it can be refreshed when needed
			}
		});
		ceFrame.add(openTransferButton);
		
		representedConnectionEndpoints.put(connectionName, ceFrame);
		activeConnection = connectionName;
		
		connectionEndpointVerticalBox.revalidate();
		connectionEndpointVerticalBox.repaint();
	}
	
	/**
	 * Starts the thread used to update the representation of the connections in the right table.
	 */
	private void startUpdateService() {
		ceUpdateThread = new Thread(this, "_ceUpdateThread");
		ceUpdateThread.start();
	}
	
	/**
	 * Interrupts the thread used to update the representation of the connections in the right table.
	 */
	public void shutdownUpdateService() {
		ceUpdateThread.interrupt();
	}
	
	/**This methods removes the CE Representation from the GUI and then also destroys the Connection
	 * 
	 * @param activeConnection The name of the CE that should be removed.
	 */
	public void removeCEEntry(String activeConnection) {
		if(representedConnectionEndpoints.get(activeConnection)!= null) {
			representedConnectionEndpoints.get(activeConnection).setVisible(false);
			connectionEndpointVerticalBox.remove(representedConnectionEndpoints.get(activeConnection));
			representedConnectionEndpoints.remove(activeConnection);
		}
		conType.remove(activeConnection);
		try {
			QuantumnetworkControllcenter.conMan.destroyConnectionEndpoint(activeConnection);
		} catch (ManagerHasNoSuchEndpointException e1) {
			new GenericWarningMessage("ERROR - Could not remove connection: " + activeConnection + ". No such connection exists.");
		}
	}
	
	/**This has the same effect as removeCEEntry(), but applies to all CEs
	 * 
	 */
	public void removeAllCEEntries() {
		//System.out.println("Received call to remove all connections!");
		Map<String,ConnectionEndpoint> connections = MessageSystem.conMan.returnAllConnections();
		connections.forEach((k,v) -> removeCEEntry(k));
	}

	/**
	 * Runs a thread that updates the representation of the connections in the right table of the GUI.
	 */
	@Override
	public void run() {
		
		while(true) {

			// for each connection in the manager
			for (Entry<String, ConnectionEndpoint> currentConnection : QuantumnetworkControllcenter.conMan.returnAllConnections().entrySet()) {
				// if it is ready to remove, remove its representation (if present)
				if (currentConnection.getValue().reportState() == ConnectionState.READY_FOR_REMOVAL) {
					representedConnectionEndpoints.remove(currentConnection.getKey());
					// also remove it from the connection manager
					// (this is safe because returnAllConnections returns a different map than the one internally used by the CM)
					try {
						QuantumnetworkControllcenter.conMan.destroyConnectionEndpoint(currentConnection.getKey());
					} catch (ManagerHasNoSuchEndpointException e) {
						// Ignore this
					}
					continue;
				} else {
					// Otherwise, add a representation if not present
					if (!(representedConnectionEndpoints.keySet().contains(currentConnection.getKey()))) {
						createConnectionRepresentation(
								currentConnection.getKey(), 
								currentConnection.getValue().getRemoteAddress(), 
								currentConnection.getValue().getRemotePort());
					}
					
					// And update the representation of the CE 
					String k = currentConnection.getKey();
					JPanel v = representedConnectionEndpoints.get(k);
					
					JButton activeButton = ((JButton) representedConnectionEndpoints.get(k).getComponent(4));
					JLabel labelCeState = ((JLabel) representedConnectionEndpoints.get(k).getComponent(2));
					ConnectionEndpoint ce = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(k);
					ConnectionState state;
					//If the ce is invalid, switch to ERROR state.
					if(ce == null) {
						state = ConnectionState.ERROR;
					}else {
						state = ce.reportState();
					}
					
					labelCeState.setText(state.name());
					labelCeState.revalidate();
					labelCeState.repaint();
					
					//Color the Connections
					if(activeConnection != null && activeConnection.equals(k)) {
						
						//v.setBackground(new Color(0, 186, 0));
						activeButton.setBackground(new Color(0, 186, 0));
						v.setBorder(new LineBorder(new Color(0, 240, 0)));
					}else if(prevActiveConnection == null || !k.equals(prevActiveConnection)){
						
						activeButton.setBackground(null);
						//v.setBackground(new Color(240, 240, 240));
						v.setBorder(new LineBorder(new Color(64, 64, 64)));
					}
					v.revalidate();
					v.repaint();
				}
			}

			/*
			 * Sleep between the updates to save resources.
			 */
			
			// Update any open chat window
			for (MessageGUI c : openChatWindows) c.refreshMessageLog();
			
			/*
			 * Sleep between the updates to save resources.
			 */
			
			try {
				TimeUnit.MILLISECONDS.sleep(200);
			} catch (InterruptedException e) {
				guiLogger.logError("An Interrupt occured in the refresher thread for the GUI", e);
			}
			prevActiveConnection = activeConnection;
		}
	}
}
