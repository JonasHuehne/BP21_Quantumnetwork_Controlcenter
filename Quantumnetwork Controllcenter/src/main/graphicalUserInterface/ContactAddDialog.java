package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import frame.QuantumnetworkControllcenter;

import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Window.Type;
import net.miginfocom.swing.MigLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

public class ContactAddDialog extends JDialog {
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
	private JButton okButton;
	private JButton cancelButton;


	/**
	 * Create the dialog.
	 */
	public ContactAddDialog() {
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
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel = new JLabel("Name:");
						panel.add(lblNewLabel);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel_1 = new JLabel("IP Address:");
						panel.add(lblNewLabel_1);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel_2 = new JLabel("Port:");
						panel.add(lblNewLabel_2);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						JLabel lblNewLabel_3 = new JLabel("Signature:");
						panel.add(lblNewLabel_3);
					}
				}
			}
			{
				Box verticalBox = Box.createVerticalBox();
				horizontalBox.add(verticalBox);
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField = new JTextField();
						panel.add(textField);
						textField.setColumns(10);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField_1 = new JTextField();
						panel.add(textField_1);
						textField_1.setColumns(10);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField_2 = new JTextField();
						panel.add(textField_2);
						textField_2.setColumns(10);
					}
				}
				{
					JPanel panel = new JPanel();
					verticalBox.add(panel);
					{
						textField_3 = new JTextField();
						panel.add(textField_3);
						textField_3.setColumns(10);
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
						
						QuantumnetworkControllcenter.communicationList.insert( textField.getText(), textField_1.getText(), Integer.valueOf(textField_2.getText()), textField_3.getText());
						QuantumnetworkControllcenter.guiWindow.gatherContacts(false);
						setVisible(false);
						dispose();
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
			GroupLayout gl_buttonPane = new GroupLayout(buttonPane);
			gl_buttonPane.setHorizontalGroup(
				gl_buttonPane.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_buttonPane.createSequentialGroup()
						.addGap(29)
						.addComponent(okButton)
						.addGap(5)
						.addComponent(cancelButton))
			);
			gl_buttonPane.setVerticalGroup(
				gl_buttonPane.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_buttonPane.createSequentialGroup()
						.addGap(5)
						.addGroup(gl_buttonPane.createParallelGroup(Alignment.LEADING)
							.addComponent(okButton)
							.addComponent(cancelButton)))
			);
			buttonPane.setLayout(gl_buttonPane);
		}
		setLocationRelativeTo(null);
	}

}