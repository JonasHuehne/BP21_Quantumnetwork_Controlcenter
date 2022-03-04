package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import communicationList.CommunicationList;
import communicationList.Contact;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import net.miginfocom.swing.MigLayout;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionState;
import networkConnection.ConnectionType;

/**This is the Main GUI of this Application. The left half handles the ContactDB and the right half handles the Connections.
 * 
 * @author Jonas Huehne
 *
 */
public class GUIMainWindow implements Runnable{

	private Object[][] contactData = {};
	String[] contactColumnNames = {"Connection Name",
            "IP Address",
            "Target Port",
            "Signature"};
	
	private JFrame frame;
	private JTable contactTable;
	private Box connectionEndpointVerticalBox;
	private HashMap<String, JPanel> representedConnectionEndpoints = new HashMap<String, JPanel>();
	private String activeConnection;
	private Thread ceUpdateThread;
	String prevActiveConnection;

	private int contactDBNameIndex = 0;
	private int contactDBIPIndex = 1;
	private int contactDBPortIndex = 2;
	private int contactDBSigIndex = 3;
	
	/** contains the last measured size of our local ConnectionManager, used in updating the list of active connections */
	private int ceAmountOld = 0;
	/** used in updating the list of active connections */
	private ArrayList<String> namesOfConnections = new ArrayList<String>();
	
	public HashMap<String,ConnectionType> conType = new HashMap<String,ConnectionType>();

	/**
	 * Create the application.
	 */
	public GUIMainWindow() {
		initialize();
		startUpdateService();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		getFrame().setBounds(100, 100, 1120, 567);
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
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
		
		JButton btnNewButton_7 = new JButton("?");
		btnNewButton_7.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new HelpMenu();
			}
		});
		btnNewButton_7.setToolTipText("Opens the Help Screen.");
		toolBar.add(btnNewButton_7);
		
		JPanel panel_5 = new JPanel();
		panel_5.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Contacts", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
		frame.getContentPane().add(panel_5, "flowx,cell 0 1,alignx left,growy");
		panel_5.setLayout(new BorderLayout(0, 0));
		
		JPanel contactPanel = new JPanel();
		panel_5.add(contactPanel, BorderLayout.WEST);
		contactPanel.setLayout(new BoxLayout(contactPanel, BoxLayout.X_AXIS));
		
		Box contactsColumn = Box.createVerticalBox();
		contactPanel.add(contactsColumn);
		
		JPanel contactControlPanel = new JPanel();
		contactsColumn.add(contactControlPanel);
		
		JButton contactsAddButton = new JButton("Add new contact");
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
		
		JButton removeContactButton = new JButton("Remove contact");
		removeContactButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				int rowIndex = contactTable.getSelectedRow();
				String id = (String) contactTable.getValueAt(rowIndex, contactDBNameIndex);
				System.out.println("Deleting Contact Entry! " + id);
				QuantumnetworkControllcenter.communicationList.delete(id);
				DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
				model.removeRow(rowIndex);
			}
		});
		removeContactButton.setToolTipText("Removes a row from the \"Contacts\"-Table.");
		contactControlPanel.add(removeContactButton);
		
		JButton contactRefreshButton = new JButton("Re-Query DB");
		contactRefreshButton.setToolTipText("Forces the \"Contacts\"-Table to update. This can be used after modifying the ContactsDatabase.");
		contactRefreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearContacts();
				gatherContacts();
			}
		});
		contactControlPanel.add(contactRefreshButton);
		
		JButton SaveChangesButton = new JButton("Save Changes to DB");
		SaveChangesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Delete all Entries in the DB first.
				CommunicationList cl = QuantumnetworkControllcenter.communicationList;
				ArrayList<Contact> dbContent = cl.queryAll();
				int entryNumber = dbContent.size();
				
				for(int i = 0; i < entryNumber; i++) {
					cl.delete(dbContent.get(i).getName());			
				}
				
				
				//Create new DBEntries form JTable
				int rowCount = contactTable.getRowCount();
				for(int i = 0; i < rowCount; i++) {
					String name = (String) contactTable.getValueAt(i, contactDBNameIndex);
					String ip = (String) contactTable.getValueAt(i, contactDBIPIndex);
					String portString = String.valueOf(contactTable.getValueAt(i, contactDBPortIndex));
					int port = Integer.valueOf(portString);
					String sig = (String) contactTable.getValueAt(i, contactDBSigIndex);
					cl.insert(name, ip, port, sig);
				}
				
				
				
			}
		});
		contactControlPanel.add(SaveChangesButton);
		
		JScrollPane ContactScrollPane = new JScrollPane();
		ContactScrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
		contactsColumn.add(ContactScrollPane);
		//contactTable = new JTable(contactData, contactColumnNames);
		contactTable = new JTable(new DefaultTableModel(contactColumnNames,0));
		gatherContacts();
		ContactScrollPane.setViewportView(contactTable);
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Connections", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
		frame.getContentPane().add(panel, "cell 0 1,grow");
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		
		Box verticalBox = Box.createVerticalBox();
		verticalBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel_1.add(verticalBox);
		
		JPanel panel_2 = new JPanel();
		verticalBox.add(panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_6 = new JPanel();
		panel_2.add(panel_6, BorderLayout.NORTH);
		
		JButton createConnectionButton = new JButton("Establish Connection");
		createConnectionButton.setToolTipText("Creates a new Connection-Endpoint, and immediately attempts to create a connection to the target.");
		panel_6.add(createConnectionButton);
		createConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					ConnectionAddDialog dialog = new ConnectionAddDialog();
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
				representedConnectionEndpoints.get(activeConnection).setVisible(false);
				connectionEndpointVerticalBox.remove(representedConnectionEndpoints.get(activeConnection));
				representedConnectionEndpoints.remove(activeConnection);
				conType.remove(activeConnection);
				QuantumnetworkControllcenter.conMan.destroyConnectionEndpoint(activeConnection);
				activeConnection = null;
				
			}
		});
		panel_6.add(closeConnectionButton);
		
		JButton GenerateKeyButton = new JButton("Encrypt Connection");
		panel_6.add(GenerateKeyButton);
		GenerateKeyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if(activeConnection == null) {
					System.out.println("Warning: No Connection selected as active.");
					return;
				}
				
				if(QuantumnetworkControllcenter.conMan.getConnectionState(activeConnection) == ConnectionState.CONNECTED) {

					QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).getKeyGen().generateKey();
					
				}else {
					System.out.println("Warning: Active Connection is not connected to anything!");
					return;
				}
				
			}
		});
		GenerateKeyButton.setToolTipText("This will start using encryption on the active connection or generate a key if one does not yet exist.");
		
		JPanel panel_3 = new JPanel();
		panel_2.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));
		
		Box verticalBox_1 = Box.createVerticalBox();
		panel_3.add(verticalBox_1);
		
		JScrollPane scrollPane = new JScrollPane();
		verticalBox_1.add(scrollPane);
		
		connectionEndpointVerticalBox = Box.createVerticalBox();
		scrollPane.setViewportView(connectionEndpointVerticalBox);
		
		JLabel dummyLabel = new JLabel("0");
		frame.getContentPane().add(dummyLabel, "cell 0 0");
		
		// Once per Second, update the connections
		Timer connectionsUpdater = new Timer(100, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int ceAmountNew = QuantumnetworkControllcenter.conMan.getConnectionsAmount();
				// If the amount of connections in the CM changed
				if (ceAmountNew != ceAmountOld) {
					if (ceAmountNew > ceAmountOld) { // if new connections were added
						Map<String, ConnectionEndpoint> currentConnections = QuantumnetworkControllcenter.conMan.returnAllConnections();		
						// Add a graphical entry for each connection that doesn't have one yet
						for (Entry<String, ConnectionEndpoint> entry : currentConnections.entrySet()) {
							if (!(namesOfConnections.contains(entry.getKey()))) {
									createConnectionRepresentation(
											entry.getKey(), 
											entry.getValue().getRemoteAddress(), 
											entry.getValue().getRemotePort());
							}
						}
					}
					// Update the List of names currently in the CM and the size of the CM accordingly
					namesOfConnections = new ArrayList<>(QuantumnetworkControllcenter.conMan.returnAllConnections().keySet());
					ceAmountNew = ceAmountOld;
				}
			}
		});
		connectionsUpdater.start();
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
	
	public int getContactDBSigIndex() {
		return contactDBSigIndex;
	}
	
	
	/**This method gathers all rows from the contact DB and translates them into JTable Rows.
	 * 
	 */
	public void gatherContacts() {
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
	public void clearContacts() {
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
	public void addRowToContactTable(String name, String ip, int port, String sig) {
		
		DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
		QuantumnetworkControllcenter.communicationList.insert(name, ip, port, sig);
		model.addRow(new Object[]{name, ip, port, sig});
	}
	

	/**This Method adds a set of SwingItems that allow the user to interact with the ConnectionEndpoint.
	 * 
	 * @param connectionName	the name of the CE
	 * @param targetIP	the targetIP of the CE
	 * @param targetPort	the targetPort of the CE
	 */
	public void createConnectionRepresentation(String connectionName, String targetIP, int targetPort) {

		
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
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(20);
		ceFrame.add(horizontalStrut_1);
		
		JButton selectConnectionButton = new JButton("Select");
		selectConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					prevActiveConnection = activeConnection;
					activeConnection = connectionName;
					System.out.println("Changed Active Connection to: " + activeConnection);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		ceFrame.add(selectConnectionButton);
		
		Component horizontalStrut_2 = Box.createHorizontalStrut(5);
		ceFrame.add(horizontalStrut_2);
		
		JComboBox<ConnectionType> connectionTypeCB = new JComboBox<ConnectionType>();
		connectionTypeCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				conType.put(connectionName, (ConnectionType) connectionTypeCB.getSelectedItem());
			}
		});
		connectionTypeCB.setModel(new DefaultComboBoxModel<ConnectionType>(ConnectionType.values()));
		ceFrame.add(connectionTypeCB);
		conType.put(connectionName, (ConnectionType) connectionTypeCB.getSelectedItem());
		
		Component horizontalStrut_3 = Box.createHorizontalStrut(5);
		ceFrame.add(horizontalStrut_3);
		
		JButton openTransferButton = new JButton("Message System");
		openTransferButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new MessageGUI(connectionName);
			}
		});
		ceFrame.add(openTransferButton);
		
		representedConnectionEndpoints.put(connectionName, ceFrame);
		activeConnection = connectionName;
		
		connectionEndpointVerticalBox.revalidate();
		connectionEndpointVerticalBox.repaint();
	}
	
	public void startUpdateService() {
		ceUpdateThread = new Thread(this, "_ceUpdateThread");
		ceUpdateThread.start();
	}
	
	@Override
	public void run() {
		
		while(true) {
			
			representedConnectionEndpoints.forEach((k,v)->{
				
				JButton activeButton = ((JButton) representedConnectionEndpoints.get(k).getComponent(4));
				JLabel label_2 = ((JLabel) representedConnectionEndpoints.get(k).getComponent(2));
				ConnectionEndpoint ce = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(k);
				ConnectionState state = ce.reportState();
				label_2.setText(state.name());
				label_2.revalidate();
				label_2.repaint();
				
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
				
			});
			
			try {
				TimeUnit.MILLISECONDS.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			prevActiveConnection = activeConnection;
		}
	}
}
