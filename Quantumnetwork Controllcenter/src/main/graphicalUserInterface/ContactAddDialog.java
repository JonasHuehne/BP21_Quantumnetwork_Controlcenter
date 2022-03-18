package graphicalUserInterface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import frame.QuantumnetworkControllcenter;
import net.miginfocom.swing.MigLayout;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**This Dialog is used to add a new Contact to the ContactDB.
 * 
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class ContactAddDialog extends JFrame {
	
	private static final long serialVersionUID = 482654877597814412L;
	
	private JTextField nameTextField;
	private JTextField ipTextField;
	private JTextField portTextField;
	private JTextField sigTextField;
	private JButton okButton;
	private JButton cancelButton;

	private static Log log = new Log(ConnectionAddDialog.class.getName(), LogSensitivity.WARNING);


	/**
	 * Create the dialog.
	 */
	public ContactAddDialog() {
		setTitle("Add Contact");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 352, 210);
		getContentPane().setLayout(new MigLayout("", "[368px]", "[123.00px][]"));
		{
			Box horizontalBox = Box.createHorizontalBox();
			getContentPane().add(horizontalBox, "cell 0 0,alignx center,aligny top");
			{
				Box verticalBox = Box.createVerticalBox();
				horizontalBox.add(verticalBox);
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
					JPanel panelForNameTextField = new JPanel();
					verticalBox.add(panelForNameTextField);
					{
						nameTextField = new JTextField();
						panelForNameTextField.add(nameTextField);
						nameTextField.setColumns(10);
					}
				}
				{
					JPanel panelForIpTextField = new JPanel();
					verticalBox.add(panelForIpTextField);
					{
						ipTextField = new JTextField();
						panelForIpTextField.add(ipTextField);
						ipTextField.setColumns(10);
					}
				}
				{
					JPanel panelForPortTextField = new JPanel();
					verticalBox.add(panelForPortTextField);
					{
						portTextField = new JTextField();
						panelForPortTextField.add(portTextField);
						portTextField.setColumns(10);
					}
				}
				{
					JPanel panelForSignatureTextField = new JPanel();
					verticalBox.add(panelForSignatureTextField);
					{
						sigTextField = new JTextField();
						panelForSignatureTextField.add(sigTextField);
						sigTextField.setColumns(10);
					}
				}
			}
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, "cell 0 1,alignx center,aligny center");
			{
				okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
						dispose();
						try {
							int port = Integer.valueOf(portTextField.getText());
							QuantumnetworkControllcenter.guiWindow.addRowToContactTable(nameTextField.getText(), ipTextField.getText(), port, sigTextField.getText());
						} catch (NumberFormatException nfe) {
							log.logWarning("Contact could not be added, because the port is not a number.", nfe);
							new GenericWarningMessage("Contact could not be added, because the entered port is not a number."
									+ " Please refer to the log for details.");
						}

					}
				});
				okButton.setActionCommand("OK");
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						setVisible(false);
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
			}
			GroupLayout glButtonPane = new GroupLayout(buttonPane);
			glButtonPane.setHorizontalGroup(
				glButtonPane.createParallelGroup(Alignment.LEADING)
					.addGroup(glButtonPane.createSequentialGroup()
						.addGap(29)
						.addComponent(okButton)
						.addGap(5)
						.addComponent(cancelButton))
			);
			glButtonPane.setVerticalGroup(
				glButtonPane.createParallelGroup(Alignment.LEADING)
					.addGroup(glButtonPane.createSequentialGroup()
						.addGap(5)
						.addGroup(glButtonPane.createParallelGroup(Alignment.LEADING)
							.addComponent(okButton)
							.addComponent(cancelButton)))
			);
			buttonPane.setLayout(glButtonPane);
		}
		setLocationRelativeTo(null);
	}

}
