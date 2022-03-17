package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import frame.QuantumnetworkControllcenter;
import messengerSystem.SigKeyQueryInteractionObject;

/**
 * Dialog to ask for the public signature key for a connection endpoint
 * @author Jonas Huehne, Sarah Schumann
 *
 */
public class CESignatureQueryDialog extends JFrame {

	private static final long serialVersionUID = 7944987425759629195L;
	
	// fields for the dialog
	private final JPanel contentPanel = new JPanel();
	private JTextField textField;
	JLabel titleNewLabel;

	/**
	 * Create the dialog.
	 */
	public CESignatureQueryDialog(String connectionID, SigKeyQueryInteractionObject sigKeyQuery) {
		setBounds(100, 100, 550, 150);
		getContentPane().setLayout(new BorderLayout());
		setVisible(true);
		toFront();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
							sigKeyQuery.setContinueVerify(true);
						} else {
							setVisible(false);
							dispose();
							new CESignatureQueryDialog(connectionID, sigKeyQuery);
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
						new DiscardMessageDialog(connectionID, sigKeyQuery);
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
				new DiscardMessageDialog(connectionID, sigKeyQuery);
			}
		});
		
		titleNewLabel.setText("Please enter the public signature key of " + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemoteName() + "_" + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemoteAddress() + "_" + QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getRemotePort());
		
	}

}
