package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import frame.QuantumnetworkControllcenter;
import messengerSystem.SHA256withRSAAuthenticationGUI;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog to ask for the public signature key for a connection endpoint
 * @author Jonas Huehne, Sarah Schumann
 *
 */
public class CESignatureQueryDialog extends JFrame {

	// fields for the dialog
	private final JPanel contentPanel = new JPanel();
	private String connectionID;
	private JTextField textField;
	JLabel titleNewLabel;

	/**
	 * Create the dialog.
	 */
	public CESignatureQueryDialog(String connectionID) {
		setBounds(100, 100, 550, 150);
		getContentPane().setLayout(new BorderLayout());
		setVisible(true);
		toFront();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
							setVisible(false);
							dispose();
							QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).setSigKey(textField.getText());
							SHA256withRSAAuthenticationGUI.continueVerify = true;
						} else {
							setVisible(false);
							dispose();
							new CESignatureQueryDialog(connectionID);
						}
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
						new DiscardMessageDialog(connectionID);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				dispose();
				new DiscardMessageDialog(connectionID);
			}
		});

		this.connectionID = connectionID;
		titleNewLabel.setText("Please enter the public signature key of " + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemoteName() + "_" + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemoteAddress() + "_" + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemotePort());

	}

}
