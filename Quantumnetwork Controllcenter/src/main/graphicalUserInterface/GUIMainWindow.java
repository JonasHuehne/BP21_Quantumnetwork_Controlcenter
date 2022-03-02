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

import communicationList.CommunicationList;
import communicationList.Contact;
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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

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
		
		
		frame.getContentPane().setLayout(new MigLayout("", "[1008px]", "[][528px][]"));
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		frame.getContentPane().add(toolBar, "cell 0 0,alignx left,aligny top");
		
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
	
	public void clearContacts() {
		DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
		int rc = model.getRowCount();
		for(int i = rc - 1; i >= 0; i--) {
			model.removeRow(i);
		}
	}
	
	public void addRowToContactTable(String name, String ip, int port, String sig) {
		
		DefaultTableModel model = (DefaultTableModel)contactTable.getModel();
		QuantumnetworkControllcenter.communicationList.insert(name, ip, port, sig);
		model.addRow(new Object[]{name, ip, port, sig});
	}
	
	
	public void refreshContactsTable() {
		contactTable.setModel(new DefaultTableModel(contactData, contactColumnNames));
	}
	
	public void createConnectionRepresentation(String connectionName, String targetIP, int targetPort) {
		//ConnectionEndpointTemplate
		
		int localPortNumber = QuantumnetworkControllcenter.conMan.getLocalPort();
		
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
		ceUpdateThread.start();
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
