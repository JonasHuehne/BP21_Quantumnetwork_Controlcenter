package graphicalUserInterface;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JScrollPane;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.BoxLayout;
import javax.swing.JTable;
import java.awt.Color;
import javax.swing.border.BevelBorder;

import communicationList.DbObject;
import frame.QuantumnetworkControllcenter;

import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import java.awt.GridLayout;
import javax.swing.JTextArea;
import javax.swing.JInternalFrame;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import networkConnection.NetworkPackage;

import javax.swing.border.EtchedBorder;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import java.awt.CardLayout;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.JComboBox;

public class GUIMainWindow implements Runnable{

	private Object[][] contactData = {};
	String[] contactColumnNames = {"Connection Name",
            "IP Address",
            "Target Port",
            "Signature"};
	private Boolean contactListChanged = false;
	
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
		
		gatherContacts(true);
		frame.getContentPane().setLayout(new MigLayout("", "[1008px]", "[][528px][]"));
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		frame.getContentPane().add(toolBar, "cell 0 0,alignx left,aligny top");
		
		JButton settingsButton = new JButton("Settings");
		settingsButton.setToolTipText("Opens the Application Settings.");
		toolBar.add(settingsButton);
		
		JButton btnNewButton_7 = new JButton("?");
		btnNewButton_7.setToolTipText("Opens the Help Screen.");
		toolBar.add(btnNewButton_7);
		
		JPanel panel_5 = new JPanel();
		panel_5.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Contacts", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
		frame.getContentPane().add(panel_5, "flowx,cell 0 1,grow");
		panel_5.setLayout(new BorderLayout(0, 0));
		
		JPanel contactPanel = new JPanel();
		panel_5.add(contactPanel, BorderLayout.NORTH);
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
				gatherContacts(false);
			}
		});
		removeContactButton.setToolTipText("Removes a row from the \"Contacts\"-Table.");
		contactControlPanel.add(removeContactButton);
		
		JButton contactRefreshButton = new JButton("Re-Query DB");
		contactRefreshButton.setToolTipText("Forces the \"Contacts\"-Table to update. This can be used after modifying the ContactsDatabase.");
		contactRefreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshContactsTable();
			}
		});
		contactControlPanel.add(contactRefreshButton);
		
		JScrollPane ContactScrollPane = new JScrollPane();
		ContactScrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
		contactsColumn.add(ContactScrollPane);
		contactTable = new JTable(contactData, contactColumnNames);
		ContactScrollPane.setViewportView(contactTable);
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Connections", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
		frame.getContentPane().add(panel, "cell 0 1,alignx right,growy");
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		
		Box verticalBox = Box.createVerticalBox();
		verticalBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel_1.add(verticalBox);
		
		JPanel panel_2 = new JPanel();
		verticalBox.add(panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_6 = new JPanel();
		panel_2.add(panel_6, BorderLayout.NORTH);
		
		JButton createConnectionButton = new JButton("Prepare Connection");
		createConnectionButton.setToolTipText("Creates a new Connection-Endpoint, but does not yet establish an actual Connection to another Endpoint.");
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
				QuantumnetworkControllcenter.conMan.destroyConnectionEndpoint(activeConnection);
				activeConnection = null;
				
			}
		});
		panel_6.add(closeConnectionButton);
		
		JButton establishConnectionButton = new JButton("Establish Connection");
		establishConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Abort if nothing is selected.
				if(activeConnection == null) {
					System.out.println("Warning: No Connection is selected as Active!");
					return;
				}
				
				String targetIP = ((JLabel)representedConnectionEndpoints.get(activeConnection).getComponent(4)).getText();
				int targetPort = Integer.valueOf(((JLabel)representedConnectionEndpoints.get(activeConnection).getComponent(5)).getText());
				
				try {
					QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).establishConnection(targetIP, targetPort);
				} catch (IOException e1) {
					System.err.println("Error: Could not establish a connection for " + activeConnection + " to " + targetIP + ":" + targetPort);
					e1.printStackTrace();
				}
				
				
			}
		});
		establishConnectionButton.setToolTipText("Establishes a Connection between the local Endpoint of a Connection and a remote Endpoint of another Connection.");
		panel_6.add(establishConnectionButton);
		
		JButton waitForConButton = new JButton("Wait for Con-Attempt");
		waitForConButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(activeConnection == null) {
					System.out.println("Warning: No Connection selected as active.");
					return;
				}
				QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).waitForConnection();
			}
		});
		waitForConButton.setToolTipText("This opens the active Connection for Connection-Requests from the outside.");
		panel_6.add(waitForConButton);
		
		JPanel panel_3 = new JPanel();
		panel_2.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));
		
		Box verticalBox_1 = Box.createVerticalBox();
		panel_3.add(verticalBox_1);
		
		JScrollPane scrollPane = new JScrollPane();
		verticalBox_1.add(scrollPane);
		
		connectionEndpointVerticalBox = Box.createVerticalBox();
		scrollPane.setViewportView(connectionEndpointVerticalBox);
		
		JPanel panel_7 = new JPanel();
		verticalBox_1.add(panel_7);
		
		JButton GenerateKeyButton = new JButton("Encrypt Connection");
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
		panel_7.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		panel_7.add(GenerateKeyButton);
		
		JPanel panel_4 = new JPanel();
		panel_2.add(panel_4, BorderLayout.SOUTH);
		GroupLayout gl_panel_4 = new GroupLayout(panel_4);
		gl_panel_4.setHorizontalGroup(
			gl_panel_4.createParallelGroup(Alignment.LEADING)
				.addGap(0, 427, Short.MAX_VALUE)
		);
		gl_panel_4.setVerticalGroup(
			gl_panel_4.createParallelGroup(Alignment.LEADING)
				.addGap(0, 34, Short.MAX_VALUE)
		);
		panel_4.setLayout(gl_panel_4);
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
	
	
	public void gatherContacts(Boolean initialGather) {
		ArrayList<DbObject> dbEntries = QuantumnetworkControllcenter.communicationList.queryAll();
		
		Object[][]tmpContactData = new Object[dbEntries.size()][4];
		for(int i = 0; i < dbEntries.size(); i++){

			String name = dbEntries.get(i).getName();
			String ip = dbEntries.get(i).getIpAddress();
			int port = dbEntries.get(i).getPort();
			String sig = dbEntries.get(i).getSignatureKey();
			
			tmpContactData[i] = new Object[]{name, ip, port, sig};
			contactData = tmpContactData;
			if(!initialGather) {
			refreshContactsTable();
			}
			};
	}
	
	public void refreshContactsTable() {
		contactTable.setModel(new DefaultTableModel(contactData, contactColumnNames));
		contactListChanged = true;
	}
	
	
	public void createConnectionRepresentation(String connectionName, int localPortNumber, String targetIP, int targetPort) {
		//ConnectionEndpointTemplate
				JPanel ceInteractionPanel = new JPanel();
				ceInteractionPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
				connectionEndpointVerticalBox.add(ceInteractionPanel);
				System.out.println("Adding new rep to the verical box!");
				ceInteractionPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
				
				JLabel cEName_Label = new JLabel(connectionName);
				ceInteractionPanel.add(cEName_Label);
				cEName_Label.setText(cEName_Label.getText() + " @ LocalPort: ");
				
				JLabel cEPort_Label = new JLabel(String.valueOf(localPortNumber));
				ceInteractionPanel.add(cEPort_Label);
				
				JButton CESelectorButton = new JButton("Select Connection");
				CESelectorButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							prevActiveConnection = activeConnection;
							activeConnection = connectionName;
							System.out.println("Changed Active Connection to: " + activeConnection);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						//QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(null, 0)
					}
				});
				ceInteractionPanel.add(CESelectorButton);
				
				JLabel connectionStateLabel = new JLabel("CE State");
				ceInteractionPanel.add(connectionStateLabel);
				
				JLabel ceTargetIPLabel = new JLabel(targetIP);
				ceInteractionPanel.add(ceTargetIPLabel);
				
				JLabel ceTargetPortLabel = new JLabel(String.valueOf(targetPort));
				ceInteractionPanel.add(ceTargetPortLabel);
				
				representedConnectionEndpoints.put(connectionName, ceInteractionPanel);
				
				activeConnection = connectionName;
				
				connectionEndpointVerticalBox.revalidate();
				connectionEndpointVerticalBox.repaint();
				
		
	}
	
	public void startUpdateService() {
		ceUpdateThread = new Thread(this, "_ceUpdateThread");
		System.out.println("avccc");
		ceUpdateThread.start();
		System.out.println("bvccc");
	}
	
	@Override
	public void run() {
		
		while(true) {
			
			representedConnectionEndpoints.forEach((k,v)->{
				
				JButton activeButton = ((JButton) representedConnectionEndpoints.get(k).getComponent(2));
				JLabel label_2 = ((JLabel) representedConnectionEndpoints.get(k).getComponent(3));
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
