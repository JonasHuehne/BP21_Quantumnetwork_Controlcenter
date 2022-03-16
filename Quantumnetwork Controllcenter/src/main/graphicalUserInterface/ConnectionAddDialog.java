package graphicalUserInterface;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import exceptions.ConnectionAlreadyExistsException;
import exceptions.IpAndPortAlreadyInUseException;
import frame.QuantumnetworkControllcenter;
import net.miginfocom.swing.MigLayout;

/**This Dialog is used to create a new Connection.
 * 
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class ConnectionAddDialog extends JDialog {
	private JTextField textFieldContactName;
	private JTextField textFieldContactIpAddr;
	private JTextField textFieldContactPort;
	private JTextField textFieldContactPK;
	private JRadioButton useManualInputRadioButton;
	private JRadioButton useSelectedInputRadioButton;
	

	/**
	 * Create the dialog.
	 */
	public ConnectionAddDialog() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 229);
		getContentPane().setLayout(new MigLayout("", "[434px]", "[80px][80px]"));
		{
			Box horizontalBox = Box.createHorizontalBox();
			getContentPane().add(horizontalBox, "cell 0 0,grow");
			{
				Box verticalBox = Box.createVerticalBox();
				horizontalBox.add(verticalBox);
				{
					useSelectedInputRadioButton = new JRadioButton("Use selected contact");
					useSelectedInputRadioButton.setToolTipText("If this is ticked, the text fields below will be disabled and the target information for the connection is taken from the \"Contact Table\" on the left side of the main app.");
					useSelectedInputRadioButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							toggleRadioButtons(useSelectedInputRadioButton);
							fillTextFieldsWithSelectedContactInfo();
						}
					});
					verticalBox.add(useSelectedInputRadioButton);
				}
				{
					JPanel panelForNameLabel = new JPanel();
					verticalBox.add(panelForNameLabel);
					{
						JLabel labelName = new JLabel("Name:");
						panelForNameLabel.add(labelName);
					}
				}
				{
					JPanel panelForIpAddrLabel = new JPanel();
					verticalBox.add(panelForIpAddrLabel);
					{
						JLabel labelIpAddr = new JLabel("IP Address:");
						panelForIpAddrLabel.add(labelIpAddr);
					}
				}
				{
					JPanel panelForPortLabel = new JPanel();
					verticalBox.add(panelForPortLabel);
					{
						JLabel labelPort = new JLabel("Port:");
						panelForPortLabel.add(labelPort);
					}
				}
				{
					JPanel panelForSignatureLabel = new JPanel();
					verticalBox.add(panelForSignatureLabel);
					{
						JLabel labelSignature = new JLabel("Signature:");
						panelForSignatureLabel.add(labelSignature);
					}
				}
			}
			{
				Box verticalBox = Box.createVerticalBox();
				horizontalBox.add(verticalBox);
				{
					useManualInputRadioButton = new JRadioButton("Use manual inputs");
					useManualInputRadioButton.setToolTipText("If this is ticked, the text fields below are used to set the intended connection target.");
					useManualInputRadioButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							toggleRadioButtons(useManualInputRadioButton);
						}
					});
					
					verticalBox.add(useManualInputRadioButton);
				}
				{
					JPanel panelForNameEntry = new JPanel();
					verticalBox.add(panelForNameEntry);
					{
						textFieldContactName = new JTextField();
						textFieldContactName.setToolTipText("This is the name of the connection. It should be named after the intended TARGET of the connection.");
						panelForNameEntry.add(textFieldContactName);
						textFieldContactName.setColumns(10);
					}
				}
				{
					JPanel panelForIpEntry = new JPanel();
					verticalBox.add(panelForIpEntry);
					{
						textFieldContactIpAddr = new JTextField();
						textFieldContactIpAddr.setToolTipText("The IP Address that this connection should be connecting to.");
						panelForIpEntry.add(textFieldContactIpAddr);
						textFieldContactIpAddr.setColumns(10);
					}
				}
				{
					JPanel panelForPortEntry = new JPanel();
					verticalBox.add(panelForPortEntry);
					{
						textFieldContactPort = new JTextField();
						textFieldContactPort.setToolTipText("The Port Number that this connection should be connecting to. This would be what the other end of the connection entered as \"Local Port\".");
						panelForPortEntry.add(textFieldContactPort);
						textFieldContactPort.setColumns(10);
					}
				}
				{
					JPanel panelForSignatureEntry = new JPanel();
					verticalBox.add(panelForSignatureEntry);
					{
						textFieldContactPK = new JTextField();
						textFieldContactPK.setToolTipText("The signature used by this connection.");
						panelForSignatureEntry.add(textFieldContactPK);
						textFieldContactPK.setColumns(10);
					}
				}
			}
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, "cell 0 1,grow");
			FlowLayout flButtonPane = new FlowLayout(FlowLayout.CENTER, 5, 5);
			buttonPane.setLayout(flButtonPane);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String newID;
						int selectedTableRowIndex = -1;
						
						//Determine new ID
						if(useManualInputRadioButton.isSelected()) {
							newID = textFieldContactName.getText();
						}else {
							selectedTableRowIndex = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
							if(selectedTableRowIndex == -1) {
								System.out.println("Warning: No Row in Contact-Table is selected!");
								return;
							}
							newID = QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedTableRowIndex, QuantumnetworkControllcenter.guiWindow.getContactDBNameIndex()).toString();
						}
						
						if(useManualInputRadioButton.isSelected()) {
							// Create new CE
							String ceName = textFieldContactName.getText();
							String ceIP = textFieldContactIpAddr.getText();
							int cePort =  Integer.valueOf(textFieldContactPort.getText());
							String sig = textFieldContactPK.getText();
							try {
								QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(ceName, ceIP, cePort, sig);
								System.out.println("Created new CE: " + textFieldContactName.getText());
							} catch (ConnectionAlreadyExistsException e1) {
								new GenericWarningMessage("Could not create connection with that name. Such a connection already exists.");
							} catch (IpAndPortAlreadyInUseException e1) {
								new GenericWarningMessage("Could not create connection with that IP / Port pairing. Such a connection already exists.");
							}
						}else {
							selectedTableRowIndex = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
							if(selectedTableRowIndex == -1) {
								System.out.println("Warning: No Row in Contact-Table is selected!");
								return;
							}
							System.out.println("Selected RowIndex is " + String.valueOf(selectedTableRowIndex));
							String name = getNameOfSelectedContact();
							String ip = getIpOfSelectedContact();
							int port = Integer.valueOf(getPortOfSelectedContact());
							String sig = getPkOfSelectedContact();
							
							try {
								QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(name, ip, port, sig);
								System.out.println("Created new CE: " + name);
							} catch (ConnectionAlreadyExistsException e1) {
								new GenericWarningMessage("Could not create connection with that name. Such a connection already exists.");
							} catch (IpAndPortAlreadyInUseException e1) {
								new GenericWarningMessage("Could not create connection with that IP / Port pairing. Such a connection already exists.");
							}
						}
						
						
						setVisible(false);
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						setVisible(false);
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		setLocationRelativeTo(null);
		
		if(QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow() == -1) {
			useManualInputRadioButton.setSelected(true);
			toggleRadioButtons(useManualInputRadioButton);
		}else {
			useSelectedInputRadioButton.setSelected(true);
			toggleRadioButtons(useSelectedInputRadioButton);
		}
		
	}
	
	/**
	 * Used to automatically fill the text fields with the selected contacts values.
	 */
	protected void fillTextFieldsWithSelectedContactInfo() {
		textFieldContactName.setText(getNameOfSelectedContact());
		textFieldContactIpAddr.setText(getIpOfSelectedContact());
		textFieldContactPort.setText(getPortOfSelectedContact().toString());
		textFieldContactPK.setText(getPkOfSelectedContact());
	}
	
	/**
	 * @return name of the currently selected contact in the contacts table, "" if no row is selected
	 */
	private String getNameOfSelectedContact() {
		int selectedRow = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
		if (selectedRow == -1) return "";
		return (String) QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedRow, QuantumnetworkControllcenter.guiWindow.getContactDBNameIndex());
	}
	
	/**
	 * @return ip of the currently selected contact in the contacts table, "" if no row is selected
	 */
	private String getIpOfSelectedContact() {
		int selectedRow = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
		if (selectedRow == -1) return "";
		return (String) QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedRow, QuantumnetworkControllcenter.guiWindow.getContactDBIPIndex());
	}
	
	/**
	 * @return port of the currently selected contact in the contacts table, 0 if no row is selected
	 */
	private Integer getPortOfSelectedContact() {
		int selectedRow = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
		if (selectedRow == -1) return 0;
		return (Integer) Integer.valueOf((String) QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedRow, QuantumnetworkControllcenter.guiWindow.getContactDBPortIndex()).toString());
	}
	
	/**
	 * @return pk of the currently selected contact in the contacts table, "" if no row is selected
	 */
	private String getPkOfSelectedContact() {
		int selectedRow = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
		if (selectedRow == -1) return "";
		return (String) QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedRow, QuantumnetworkControllcenter.guiWindow.getContactDBPubSigKeyIndex());
	}

	/**This is used to ensure that only one of the 2 RadioButtons is selected at once.
	 * 
	 * @param editedRadioButton The newly changed RadioButton.
	 */
	private void toggleRadioButtons(JRadioButton editedRadioButton) {
		boolean newState;
		JRadioButton notEditedRB;
		if (editedRadioButton == useManualInputRadioButton) {
			notEditedRB = useSelectedInputRadioButton;
		}else {
			notEditedRB = useManualInputRadioButton;
		}
		
			newState = editedRadioButton.isSelected();
			notEditedRB.setSelected(!newState);
			
			if(useManualInputRadioButton.getSelectedObjects() != null) {
				textFieldContactName.setEnabled(true);
				textFieldContactIpAddr.setEnabled(true);
				textFieldContactPort.setEnabled(true);
				textFieldContactPK.setEnabled(true);
			}else {
				textFieldContactName.setEnabled(false);
				textFieldContactIpAddr.setEnabled(false);
				textFieldContactPort.setEnabled(false);
				textFieldContactPK.setEnabled(false);
			}

	}


}
