package graphicalUserInterface.keyStoreEditor;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import graphicalUserInterface.GenericWarningMessage;
import keyStore.KeyStoreDbManager;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.awt.event.ActionEvent;

/**
 * Debug Window for inserting an entry into the keystore.
 * @author Sasha Petri
 */
public class DebugKeystoreEditorAddDialog extends JFrame {
	private JTextField textFieldID;
	private JTextField textFieldIndex;
	private JTextField textFieldInit;
	private JTextField textFieldSource;
	private JTextField textFieldDest;
	private JTextField textFieldKey;
	
	protected DebugKeystoreEditor caller;
	
	public DebugKeystoreEditorAddDialog() {
		getContentPane().setLayout(null);
		
		this.setSize(364, 267);
		
		JLabel labelID = new JLabel("ID");
		labelID.setBounds(27, 11, 115, 20);
		getContentPane().add(labelID);
		
		JLabel lblNewLabel = new JLabel("Index");
		lblNewLabel.setBounds(27, 42, 115, 20);
		getContentPane().add(lblNewLabel);
		
		JLabel lblInitiative = new JLabel("Initiative");
		lblInitiative.setBounds(27, 73, 115, 20);
		getContentPane().add(lblInitiative);
		
		JLabel lblSource = new JLabel("Source");
		lblSource.setBounds(27, 104, 115, 20);
		getContentPane().add(lblSource);
		
		JLabel lblDestination = new JLabel("Destination");
		lblDestination.setBounds(27, 135, 115, 20);
		getContentPane().add(lblDestination);
		
		JLabel lblKeyBytes = new JLabel("Key Bytes");
		lblKeyBytes.setToolTipText("UTF-8 String. Will be converted into a byte array.");
		lblKeyBytes.setBounds(27, 166, 115, 20);
		getContentPane().add(lblKeyBytes);
		
		textFieldID = new JTextField();
		textFieldID.setBounds(162, 11, 165, 20);
		getContentPane().add(textFieldID);
		textFieldID.setColumns(10);
		
		textFieldIndex = new JTextField();
		textFieldIndex.setBounds(162, 42, 165, 20);
		getContentPane().add(textFieldIndex);
		textFieldIndex.setColumns(10);
		
		textFieldInit = new JTextField();
		textFieldInit.setColumns(10);
		textFieldInit.setBounds(162, 73, 165, 20);
		getContentPane().add(textFieldInit);
		
		textFieldSource = new JTextField();
		textFieldSource.setColumns(10);
		textFieldSource.setBounds(162, 104, 165, 20);
		getContentPane().add(textFieldSource);
		
		textFieldDest = new JTextField();
		textFieldDest.setColumns(10);
		textFieldDest.setBounds(162, 135, 165, 20);
		getContentPane().add(textFieldDest);
		
		textFieldKey = new JTextField();
		textFieldKey.setColumns(10);
		textFieldKey.setBounds(162, 166, 165, 20);
		getContentPane().add(textFieldKey);
		
		JFrame frame = this;
		
		JButton addEntryButton = new JButton("Add Entry");
		addEntryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					// Parse Entries
					String ID = textFieldID.getText();
					// If ID is in use already, refuse
					if (KeyStoreDbManager.doesKeyStreamIdExist(ID)) {
						new GenericWarningMessage("Can not add entry - this ID is already in use.");
						return;
					}
					int index = Integer.parseInt(textFieldIndex.getText());
					boolean initiative = Boolean.parseBoolean(textFieldInit.getText());
					String source = textFieldSource.getText();
					String dest = textFieldDest.getText();
					
					byte[] key = textFieldKey.getText().getBytes(StandardCharsets.UTF_8);
					
					
					KeyStoreDbManager.insertToKeyStore(ID, key, source, dest, false, initiative);
					KeyStoreDbManager.changeIndex(ID, index);
					
					caller.updateTable();
					
					dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
					
				} catch (NumberFormatException e1) {
					new GenericWarningMessage("Index must be an Integer.");
					return;
				} catch (SQLException e1) {
					new GenericWarningMessage("There was an issue with the SQL Database. " + e1.getMessage());
					return;
				} catch (NoKeyWithThatIDException e) {
					// won't happen
				} catch (NotEnoughKeyLeftException e) {
					new GenericWarningMessage("Invalid Index - Index was instead set to 0.");
					return;
				}
				
				
			}
		});
		addEntryButton.setBounds(27, 197, 300, 23);
		getContentPane().add(addEntryButton);
	}
}
