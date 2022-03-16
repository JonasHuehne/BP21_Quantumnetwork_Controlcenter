package graphicalUserInterface.keyStoreEditor;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.event.ActionEvent;

public class DebugKeystoreEditor extends JFrame {
	private JTable table;
	public DebugKeystoreEditor() {
		getContentPane().setLayout(null);
		
		this.setSize(1024, 720);
		
		JButton btnNewButton = new JButton("Query DB");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTable();

				
			}
		});
		
		btnNewButton.setBounds(10, 11, 89, 23);
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
		insertButton.setBounds(109, 11, 140, 23);
		getContentPane().add(insertButton);
	}
	
	
	private void clearTable() {
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		int rc = model.getRowCount();
		for(int i = rc - 1; i >= 0; i--) {
			model.removeRow(i);
		}
	}
	
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


