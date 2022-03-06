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
import java.awt.Font;

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
		setTitle("Settings");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		setBounds(100, 100, 350, 304);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel ownNameLabel = new JLabel("Local Name:");
			ownNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
			ownNameLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
			ownNameLabel.setBounds(10, 26, 161, 20);
			contentPanel.add(ownNameLabel);
			ownNameLabel.setToolTipText("This should be a name that represents you. It will be sent to connecting parties and be used on their end to name the connection.");
		}
		{
			ownNameTextfield = new JTextField();
			ownNameTextfield.setBounds(181, 26, 140, 20);
			contentPanel.add(ownNameTextfield);
			ownNameTextfield.setText("Default Name");
			ownNameTextfield.setToolTipText("This should be a name that represents you. It will be sent to connecting parties and be used on their end to name the connection.");
			ownNameTextfield.setColumns(10);
		}
		{
			JLabel ownIPLabel = new JLabel("Local IP:");
			ownIPLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
			ownIPLabel.setBounds(10, 52, 161, 20);
			contentPanel.add(ownIPLabel);
			ownIPLabel.setToolTipText("This should be your public IP. You can choose localhost, your LAN IP or your WAN IP.");
		}
		{
			ownIPTextField = new JTextField();
			ownIPTextField.setBounds(181, 52, 140, 20);
			contentPanel.add(ownIPTextField);
			ownIPTextField.setToolTipText("This should be your public IP. You can choose localhost, your LAN IP or your WAN IP.");
			ownIPTextField.setEnabled(true);
			ownIPTextField.setEditable(true);
			ownIPTextField.setText("127.0.0.1");
			ownIPTextField.setColumns(10);
		}
		{
			JLabel ownPortLabel = new JLabel("Server Port:     ");
			ownPortLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
			ownPortLabel.setBounds(10, 79, 161, 20);
			contentPanel.add(ownPortLabel);
			ownPortLabel.setToolTipText("When hosting across networks via the Internet, this port needs to be accessible to other parties. It needs to be forwarded in your router settings.");
		}
		{
			ownPortTextField = new JTextField();
			ownPortTextField.setBounds(181, 79, 140, 20);
			contentPanel.add(ownPortTextField);
			ownPortTextField.setToolTipText("When hosting across networks via the Internet, this port needs to be accessible to other parties. It needs to be forwarded in your router settings.");
			ownPortTextField.setText("5000");
			ownPortTextField.setColumns(10);
		}
		{
			JLabel sourceIPLabel = new JLabel("Photon Source IP:    ");
			sourceIPLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
			sourceIPLabel.setBounds(10, 110, 161, 20);
			contentPanel.add(sourceIPLabel);
			sourceIPLabel.setToolTipText("This needs to be set to the public IP of the Photon Source Server.");
		}
		{
			sourceIPTextField = new JTextField();
			sourceIPTextField.setBounds(181, 110, 140, 20);
			contentPanel.add(sourceIPTextField);
			sourceIPTextField.setToolTipText("This needs to be set to the public IP of the Photon Source Server.");
			sourceIPTextField.setText("127.0.0.1");
			sourceIPTextField.setColumns(10);
		}
		{
			JLabel sourcePortNewLabel = new JLabel("Photon Source Port: ");
			sourcePortNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
			sourcePortNewLabel.setBounds(10, 141, 161, 20);
			contentPanel.add(sourcePortNewLabel);
			sourcePortNewLabel.setToolTipText("This needs to be set to the Portnumber of the Photon Source Server.");
		}
		{
			sourcePortTextField = new JTextField();
			sourcePortTextField.setBounds(181, 141, 140, 20);
			contentPanel.add(sourcePortTextField);
			sourcePortTextField.setToolTipText("This needs to be set to the Portnumber of the Photon Source Server.");
			sourcePortTextField.setText("2300");
			sourcePortTextField.setColumns(10);
		}
		{
			JLabel encodingLabel = new JLabel("Preferred Encoding:");
			encodingLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
			encodingLabel.setBounds(10, 172, 161, 20);
			contentPanel.add(encodingLabel);
			encodingLabel.setToolTipText("The Encoding used when transferring Strings to bytes. If some characters are not correctly transmitted, you can change the encoding to one that supports the characters in question.");
		}
		{
			encodingTextField = new JTextField();
			encodingTextField.setBounds(181, 172, 140, 20);
			contentPanel.add(encodingTextField);
			encodingTextField.setToolTipText("The Encoding used when transferring Strings to bytes. If some characters are not correctly transmitted, you can change the encoding to one that supports the characters in question.");
			encodingTextField.setText("ISO-8859-1");
			encodingTextField.setColumns(10);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("Apply");
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
