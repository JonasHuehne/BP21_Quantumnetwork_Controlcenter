package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import frame.Configuration;

/**
 * 
 * @author Jonas Huehne
 *
 */
public class SourceSignatureQueryDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField textField;
	JLabel titleNewLabel;


	/**
	 * Create the dialog.
	 */
	public SourceSignatureQueryDialog() {
		setBounds(100, 100, 450, 150);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			Box verticalBox = Box.createVerticalBox();
			contentPanel.add(verticalBox);
			{
				titleNewLabel = new JLabel("Please enter the public signature key of the Photon Source:");
				titleNewLabel.setToolTipText("Please enter the public signature key of the Photon Source.");
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
							Configuration.setProperty(textField.getText(), "SourceSignature");
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
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
	}

}
