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
import javax.swing.event.ChangeEvent;

public class ConnectionAddDialog extends JDialog {
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
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
					useSelectedInputRadioButton.setToolTipText("If this is ticked, the textfields below will be disabled and the target information for the connection is taken from the \"Contact Table\" on the left side of the main app.");
					useSelectedInputRadioButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							toggleRadioButtons(useSelectedInputRadioButton);
						}
					});
					verticalBox.add(useSelectedInputRadioButton);
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel = new JLabel("Name:");
						panel.add(lblNewLabel);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel_1 = new JLabel("IP Address:");
						panel.add(lblNewLabel_1);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel_2 = new JLabel("Port:");
						panel.add(lblNewLabel_2);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel_3 = new JLabel("Signature:");
						panel.add(lblNewLabel_3);
					}
				}
			}
			{
				Box verticalBox = Box.createVerticalBox();
				horizontalBox.add(verticalBox);
				{
					useManualInputRadioButton = new JRadioButton("Use manual inputs");
					useManualInputRadioButton.setToolTipText("If this is ticked, the textfields below are used to set the intended connection target.");
					useManualInputRadioButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							toggleRadioButtons(useManualInputRadioButton);
						}
					});
					
					verticalBox.add(useManualInputRadioButton);
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField = new JTextField();
						textField.setToolTipText("This is the name of the connection. It should be named after the intended TARGET of the connection.");
						panel.add(textField);
						textField.setColumns(10);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField_1 = new JTextField();
						textField_1.setToolTipText("The IP Address that this connection should be connecting to.");
						panel.add(textField_1);
						textField_1.setColumns(10);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField_2 = new JTextField();
						textField_2.setToolTipText("The Port Number that this connection should be connecting to. This would be what the other end of the connection entered as \"Local Port\".");
						panel.add(textField_2);
						textField_2.setColumns(10);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField_3 = new JTextField();
						textField_3.setToolTipText("The signature used by this connection.");
						panel.add(textField_3);
						textField_3.setColumns(10);
					}
				}
			}
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, "cell 0 1,grow");
			FlowLayout fl_buttonPane = new FlowLayout(FlowLayout.CENTER, 5, 5);
			buttonPane.setLayout(fl_buttonPane);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String newID;
						int selectedTableRowIndex = -1;
						
						//Determine new ID
						if(useManualInputRadioButton.isSelected()) {
							newID = textField.getText();
						}else {
							selectedTableRowIndex = QuantumnetworkControllcenter.guiWindow.getContactTable().getSelectedRow();
							if(selectedTableRowIndex == -1) {
								System.out.println("Warning: No Row in Contact-Table is selected!");
								return;
							}
							newID = QuantumnetworkControllcenter.guiWindow.getContactTable().getValueAt(selectedTableRowIndex, QuantumnetworkControllcenter.guiWindow.getContactDBNameIndex()).toString();
						}
						
						//Check if ID is taken
						if(QuantumnetworkControllcenter.conMan.hasConnectionEndpoint(newID)) {
							System.out.println("Warning: ConnectionID is already in use!");
							return;
						}
						
						
						if(useManualInputRadioButton.isSelected()) {
							System.out.println("Created new CE: " + textField.getText() + " : "+ QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(textField.getText(), textField_1.getText(), Integer.valueOf(textField_2.getText())));
							QuantumnetworkControllcenter.guiWindow.createConnectionRepresentation(textField.getText(), textField_1.getText(), Integer.valueOf(textField_2.getText()));
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
							
							System.out.println("Created new CE: " + name + " : "+ QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(name, ip, port));
							QuantumnetworkControllcenter.guiWindow.createConnectionRepresentation(name, ip, port);
							
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
				textField.setEnabled(true);
				textField_1.setEnabled(true);
				textField_2.setEnabled(true);
				textField_3.setEnabled(true);
			}else {
				textField.setEnabled(false);
				textField_1.setEnabled(false);
				textField_2.setEnabled(false);
				textField_3.setEnabled(false);
			}

	}


}
