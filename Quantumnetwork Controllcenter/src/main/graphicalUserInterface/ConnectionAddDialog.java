package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import frame.QuantumnetworkControllcenter;

import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Window.Type;
import net.miginfocom.swing.MigLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeListener;

import exceptions.ConnectionWithThatNameAlreadyExistsException;

import javax.swing.event.ChangeEvent;

/**This Dialog is used to create a new Connection.
 * 
 * @author Jonas Huehne
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
							try {
								QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(ceName, ceIP, cePort);
								System.out.println("Created new CE: " + textFieldContactName.getText());
							} catch (ConnectionWithThatNameAlreadyExistsException e1) {
								System.err.println("Could not created connection with that name, such a connection already exists."); // TODO Make this into a GUI warning
							}
						}else {
							selectedTableRowIndex = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
							if(selectedTableRowIndex == -1) {
								System.out.println("Warning: No Row in Contact-Table is selected!");
								return;
							}
							System.out.println("Selected RowIndex is " + String.valueOf(selectedTableRowIndex));
							String name = QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedTableRowIndex, QuantumnetworkControllcenter.guiWindow.getContactDBNameIndex()).toString();
							String ip = QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedTableRowIndex, QuantumnetworkControllcenter.guiWindow.getContactDBIPIndex()).toString();
							int port = Integer.valueOf((QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedTableRowIndex, QuantumnetworkControllcenter.guiWindow.getContactDBPortIndex()).toString()));
							String sig = QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedTableRowIndex, QuantumnetworkControllcenter.guiWindow.getContactDBSigIndex()).toString();
							
							try {
								QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(name, ip, port);
								System.out.println("Created new CE: " + name);
							} catch (ConnectionWithThatNameAlreadyExistsException e1) {
								System.err.println("Could not created connection with that name, such a connection already exists."); // TODO Make this into a GUI warning
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
