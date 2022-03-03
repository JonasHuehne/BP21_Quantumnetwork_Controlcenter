package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import frame.Configuration;
import messengerSystem.MessageSystem;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JSeparator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**This Dialog contains settings such as the own ServerIP/Port
 * 
 * @author Jonas Huehne
 *
 */
public class SettingsDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField ownNameTextfield;
	private JTextField ownIPTextField;
	private JTextField ownPortTextField;
	private JTextField sourceIPTextField;
	private JTextField sourcePortTextField;
	private JTextField encodingTextField;
	
	private static String name = null;
	private static String ip = null;
	private static String port = null;
	private static String sourceIP = null;
	private static String sourcePort = null;
	private static String enc = null;
	



	/**
	 * Create the dialog.
	 */
	public SettingsDialog() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		setBounds(100, 100, 230, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			Box verticalBox = Box.createVerticalBox();
			contentPanel.add(verticalBox);
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
			{
				Box horizontalBox = Box.createHorizontalBox();
				verticalBox.add(horizontalBox);
				{
					JLabel ownNameLabel = new JLabel("Own Name:     ");
					ownNameLabel.setToolTipText("This should be a name that represents you. It will be sent to connecting parties and be used on their end to name the connection.");
					horizontalBox.add(ownNameLabel);
				}
				{
					ownNameTextfield = new JTextField();
					ownNameTextfield.setText("Default Name");
					ownNameTextfield.setToolTipText("This should be a name that represents you. It will be sent to connecting parties and be used on their end to name the connection.");
					horizontalBox.add(ownNameTextfield);
					ownNameTextfield.setColumns(10);
				}
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
			{
				Box horizontalBox = Box.createHorizontalBox();
				verticalBox.add(horizontalBox);
				{
					JLabel ownIPLabel = new JLabel("Own IP:     ");
					ownIPLabel.setToolTipText("This should be your public IP. You can choose localhost, your LAN IP or your WAN IP.");
					horizontalBox.add(ownIPLabel);
				}
				{
					ownIPTextField = new JTextField();
					ownIPTextField.setToolTipText("This should be your public IP. You can choose localhost, your LAN IP or your WAN IP.");
					ownIPTextField.setEnabled(true);
					ownIPTextField.setEditable(true);
					ownIPTextField.setText("127.0.0.1");
					horizontalBox.add(ownIPTextField);
					ownIPTextField.setColumns(10);
				}
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
			{
				Box horizontalBox = Box.createHorizontalBox();
				verticalBox.add(horizontalBox);
				{
					JLabel ownPortLabel = new JLabel("Server Port:     ");
					ownPortLabel.setToolTipText("When hosting across networks via the internet, this port needs to be accessible to other parties. It needs to be forwarded in your router settings.");
					horizontalBox.add(ownPortLabel);
				}
				{
					ownPortTextField = new JTextField();
					ownPortTextField.setToolTipText("When hosting across networks via the internet, this port needs to be accessible to other parties. It needs to be forwarded in your router settings.");
					ownPortTextField.setText("5000");
					horizontalBox.add(ownPortTextField);
					ownPortTextField.setColumns(10);
				}
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
			{
				Box horizontalBox = Box.createHorizontalBox();
				verticalBox.add(horizontalBox);
				{
					JLabel sourceIPLabel = new JLabel("Source IP:    ");
					sourceIPLabel.setToolTipText("This needs to be set to the public IP of the Photon Source Server.");
					horizontalBox.add(sourceIPLabel);
				}
				{
					sourceIPTextField = new JTextField();
					sourceIPTextField.setToolTipText("This needs to be set to the public IP of the Photon Source Server.");
					sourceIPTextField.setText("127.0.0.1");
					horizontalBox.add(sourceIPTextField);
					sourceIPTextField.setColumns(10);
				}
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
			{
				Box horizontalBox = Box.createHorizontalBox();
				verticalBox.add(horizontalBox);
				{
					JLabel sourcePortNewLabel = new JLabel("Source Port:    ");
					sourcePortNewLabel.setToolTipText("This needs to be set to the Portnumber of the Photon Source Server.");
					horizontalBox.add(sourcePortNewLabel);
				}
				{
					sourcePortTextField = new JTextField();
					sourcePortTextField.setToolTipText("This needs to be set to the Portnumber of the Photon Source Server.");
					sourcePortTextField.setText("2300");
					horizontalBox.add(sourcePortTextField);
					sourcePortTextField.setColumns(10);
				}
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
			{
				Box horizontalBox = Box.createHorizontalBox();
				verticalBox.add(horizontalBox);
				{
					JLabel encodingLabel = new JLabel("Preferred Encoding:");
					encodingLabel.setToolTipText("The Encoding used when transferring Strings to bytes. If some characters are not correctly transmitted, you can change the encoding to one that supports the characters in question.");
					horizontalBox.add(encodingLabel);
				}
				{
					encodingTextField = new JTextField();
					encodingTextField.setToolTipText("The Encoding used when transferring Strings to bytes. If some characters are not correctly transmitted, you can change the encoding to one that supports the characters in question.");
					encodingTextField.setText("ISO-8859-1");
					horizontalBox.add(encodingTextField);
					encodingTextField.setColumns(10);
				}
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
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
						writeSettings();
						setVisible(false);
						dispose();
					}
				});
				okButton.setToolTipText("This will apply the settings.");
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
				cancelButton.setToolTipText("This will ignore any changes to the settings.");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		initSettings();
		
		readSettings();
		
		
		
	}
	
	/**This method checks if the config file contains each of the setting parameters and if not, it creates the parameter with a default value.
	 * 
	 */
	public static void initSettings() {
		
		name = Configuration.getProperty("UserName");	
		if(name == null) {
			Configuration.setProperty("UserName", "Default Name");
			name = Configuration.getProperty("UserName");
		}
		
		ip = Configuration.getProperty("UserIP");	
		if(ip == null) {
			Configuration.setProperty("UserIP", "localhost");
			ip = Configuration.getProperty("UserIP");
		}
		
		port = Configuration.getProperty("UserPort");	
		if(port == null) {
			Configuration.setProperty("UserPort", "5000");
			port = Configuration.getProperty("UserPort");
		}
		
		sourceIP = Configuration.getProperty("SourceIP");	
		if(sourceIP == null) {
			Configuration.setProperty("SourceIP", "127.0.0.1");
			sourceIP = Configuration.getProperty("SourceIP");
		}
		
		sourcePort = Configuration.getProperty("SourcePort");	
		if(sourcePort == null) {
			Configuration.setProperty("SourcePort", "2400");
			sourcePort = Configuration.getProperty("SourcePort");
		}
		
		enc = Configuration.getProperty("Encoding");
		if(enc == null) {
			Configuration.setProperty("Encoding", "ISO-8859-1");
			enc = Configuration.getProperty("Encoding");
		}
	}
	
	/**This method reads the config file values and adds them into the textFields.
	 * 
	 */
	private void readSettings(){
		ownNameTextfield.setText(name);
		
		ownIPTextField.setText(ip);
		
		ownPortTextField.setText(port);
		
		sourceIPTextField.setText(sourceIP);
		
		sourcePortTextField.setText(sourcePort);
		
		encodingTextField.setText(enc);
	}
	
	/**This method reads the text from the textFields and writes them into the config file.
	 * 
	 */
	private void writeSettings() {
		
		Configuration.setProperty("UserName", ownNameTextfield.getText());
		if(!Configuration.getProperty("UserIP").equals(ownIPTextField.getText())) {
			MessageSystem.conMan.destroyAllConnectionEndpoints();
			MessageSystem.conMan.setLocalAddress(ownNameTextfield.getText());
		}
		Configuration.setProperty("UserIP", ownIPTextField.getText());
		if(!Configuration.getProperty("UserPort").equals(ownPortTextField.getText())) {
			MessageSystem.conMan.destroyAllConnectionEndpoints();
			MessageSystem.conMan.setLocalPort(Integer.valueOf(ownPortTextField.getText()));
		}
		Configuration.setProperty("UserPort", ownPortTextField.getText());
		Configuration.setProperty("SourceIP", sourceIPTextField.getText());
		Configuration.setProperty("SourcePort", sourcePortTextField.getText());
		Configuration.setProperty("Encoding", encodingTextField.getText());
	}

}
