package graphicalUserInterface.keyStoreEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import exceptions.NoKeyWithThatIDException;
import graphicalUserInterface.GenericWarningMessage;
import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;

/**
 * Debug Window for viewing and editing the keystore.
 * @author Sasha Petri
 */
public class DebugKeystoreEditor extends JFrame {
	
	private static final long serialVersionUID = 145434116567160386L;
	
	private JTable table;
	public DebugKeystoreEditor() {
		setTitle("Key DB Editor");
		getContentPane().setLayout(null);
		
		this.setSize(1024, 720);
		
		JButton btnNewButton = new JButton("Query DB");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTable();
			}
		});
		
		btnNewButton.setBounds(159, 11, 89, 23);
		getContentPane().add(btnNewButton);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(20, 45, 978, 625);
		getContentPane().add(scrollPane);
		
		table = new JTable();
		table.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"ID", "Index", "Initiative", "Source", "Destination", "Key Bytes"
			}
		));
		TableColumnModel tcm = table.getColumnModel();
		tcm.getColumn(0).setMaxWidth(200);
		tcm.getColumn(1).setMaxWidth(50);
		tcm.getColumn(2).setMaxWidth(60);
		
		scrollPane.setViewportView(table);
		
		DebugKeystoreEditor kse = this;
		
		JButton insertButton = new JButton("Insert New Entry");
		insertButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DebugKeystoreEditorAddDialog dialog = new DebugKeystoreEditorAddDialog();
				dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
				dialog.caller = kse;
			}
		});
		insertButton.setBounds(258, 11, 140, 23);
		getContentPane().add(insertButton);
		
		JButton buttonRemoveSelected = new JButton("Remove Selected");
		buttonRemoveSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int rowIndex = table.getSelectedRow();
				String id = (String) table.getValueAt(rowIndex, 0); // id of the key in the selected row
				try {
					KeyStoreDbManager.deleteEntryIfExists(id);
				} catch (SQLException e1) {
					new GenericWarningMessage("An SQL Exception occurred: " + e1.getMessage());
				}
				updateTable();
			}
		});
		buttonRemoveSelected.setToolTipText("Removes the selected entry.");
		buttonRemoveSelected.setBounds(408, 11, 140, 23);
		getContentPane().add(buttonRemoveSelected);
		
		JButton buttonChecksum = new JButton("Display Checksum");
		buttonChecksum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int rowIndex = table.getSelectedRow();
				String id = (String) table.getValueAt(rowIndex, 0); // id of the key in the selected row
				byte[] key;
				try {
					key = KeyStoreDbManager.getEntryFromKeyStore(id).getCompleteKeyBuffer(); // complete key of selected row
					MessageDigest md5 = MessageDigest.getInstance("MD5");
					md5.update(key);
					String checksum = new String(md5.digest(), StandardCharsets.ISO_8859_1);
					new GenericWarningMessage(checksum);
				} catch (NoKeyWithThatIDException e1) {
					new GenericWarningMessage("Could not generate checksum: " + e1.getMessage());
				} catch (SQLException e1) {
					new GenericWarningMessage("An SQL Exception occurred: " + e1.getMessage());
				} catch (NoSuchAlgorithmException e1) {
					// Should not occur
				}
			}
		});
		buttonChecksum.setToolTipText("Displays the checksum of the selected key - useful for manually checking whether two keys are identical.");
		buttonChecksum.setBounds(558, 11, 117, 23);
		getContentPane().add(buttonChecksum);
		
		JButton buttonCreateDB = new JButton("Create DB");
		buttonCreateDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					KeyStoreDbManager.createNewKeyStoreAndTable();
				} catch (SQLException e1) {
					new GenericWarningMessage("An SQL Exception occurred: " + e1.getMessage());
				}
			}
		});
		buttonCreateDB.setToolTipText("Creates the Keystore if it does not exist yet.");
		buttonCreateDB.setBounds(20, 11, 129, 23);
		getContentPane().add(buttonCreateDB);
	}
	
	
	/**
	 * Clears the entries of the table.
	 */
	private void clearTable() {
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		int rc = model.getRowCount();
		for(int i = rc - 1; i >= 0; i--) {
			model.removeRow(i);
		}
	}
	
	/**
	 * Updates the entries in the table.
	 */
	protected void updateTable() {
		clearTable();
		ArrayList<KeyStoreObject> keys = new ArrayList<>();
		try {
			keys = KeyStoreDbManager.getKeyStoreAsList();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		for (KeyStoreObject key : keys) {
			String id = key.getID();
			String index = Integer.toString(key.getIndex());
			String source = key.getSource();
			String dest = key.getDestination();
			String initiative = Boolean.toString(key.getInitiative());
			String keyBytes = Arrays.toString(key.getCompleteKeyBuffer());
			
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			model.addRow(new Object[] {id, index, initiative, source, dest, keyBytes});
		}
	}
}


