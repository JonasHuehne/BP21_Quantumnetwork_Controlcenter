package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.NetworkPackage;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Font;

/**
 * 
 * @author Jonas Huehne
 *
 */
public class CESignatureQueryDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private String connectionID;
	private JTextField textField;
	JLabel titleNewLabel;

	/**
	 * Create the dialog.
	 */
	public CESignatureQueryDialog(String connectionID, boolean triedBefore) {
		setBounds(100, 100, 450, 150);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			Box verticalBox = Box.createVerticalBox();
			contentPanel.add(verticalBox);
			{
				titleNewLabel = new JLabel("Please enter the public signature key of the connected Party:");
				titleNewLabel.setToolTipText("Please enter the public signature key of the connected Party.");
				titleNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
				verticalBox.add(titleNewLabel);
			}
			{
				textField = new JTextField();
				verticalBox.add(textField);
				textField.setColumns(10);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if(textField.getText() != null && !textField.getText().equals("")) {
							QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).setSig(textField.getText());
						}
						SHA256withRSAAuthentication.continueVerify = true;
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
						if (!triedBefore) {
							GenericWarningMessage noKeyWarning = new GenericWarningMessage("No public key added.");
							noKeyWarning.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							noKeyWarning.setAlwaysOnTop(true);
							new CESignatureQueryDialog(connectionID, true);
						} else {
							GenericWarningMessage noKeyWarning = new GenericWarningMessage("No public key added. Message will be discarded.");
							noKeyWarning.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							noKeyWarning.setAlwaysOnTop(true);
							SHA256withRSAAuthentication.abortVerify = true;
						}
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		
		this.connectionID = connectionID;
		titleNewLabel.setText("Please enter the public signature key of " + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemoteName() + "_" + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemoteAddress() + "_" + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemotePort());
		
	}

}
