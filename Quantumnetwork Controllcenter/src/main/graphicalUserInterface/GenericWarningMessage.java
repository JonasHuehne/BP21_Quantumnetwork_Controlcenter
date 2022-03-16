package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;

/**This is a simple generic Warning or Error Dialog Window. It can be used to display important messages to the user.
 *
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class GenericWarningMessage extends JDialog {

	private static final long serialVersionUID = -1172300406464343271L;


	/**This is a simple generic Warning or Error Dialog Window. It can be used to display important messages to the user.
	 * 
	 * @param warningText the content of the warning windows text area.
	 */
	public GenericWarningMessage(String warningText) {
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		toFront();
		
		int width = 450;
		int height = 150;
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		int xPos = (int) Math.max(0, mousePos.getX() - (width / 2));
		int yPos = (int) Math.max(0, mousePos.getY() - (height / 2));
		setBounds(xPos, yPos, width, height);
		
		getContentPane().setLayout(new BorderLayout());
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					dispose();
				}
			});
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}

		JTextPane warningTextField = new JTextPane();
		warningTextField.setBackground(UIManager.getColor("CheckBox.background"));
		warningTextField.setEditable(false);
		warningTextField.setFont(new Font("Tahoma", Font.PLAIN, 16));
		warningTextField.setText(warningText);
		getContentPane().add(warningTextField, BorderLayout.CENTER);
	}
	
	/**This is a simple generic Warning or Error Dialog Window. It can be used to display important messages to the user.
	 * 
	 * @param warningText the content of the warning windows text area.
	 * @param xBounds the X Size
	 * @param yBounds the Y Size
	 */
	
	public GenericWarningMessage(String warningText, int xBounds, int yBounds) {
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		int xPos = (int) Math.max(0, mousePos.getX() - (xBounds / 2));
		int yPos = (int) Math.max(0, mousePos.getY() - (yBounds / 2));
		setBounds(xPos, yPos, xBounds, yBounds);
		
		getContentPane().setLayout(new BorderLayout());
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					dispose();
				}
			});
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}

		JTextPane warningTextField = new JTextPane();
		warningTextField.setBackground(UIManager.getColor("CheckBox.background"));
		warningTextField.setEditable(false);
		warningTextField.setFont(new Font("Tahoma", Font.PLAIN, 16));
		warningTextField.setText(warningText);
		getContentPane().add(warningTextField, BorderLayout.CENTER);
	}
	
	
	/**This is a simple generic Warning or Error Dialog Window. It can be used to display important messages to the user.
	 * 
	 * @param warningText the content of the warning windows text area.
	 * @param xPos the initial Window Position.x
	 * @param yPos the initial Window Position.y
	 * @param xBounds the X Size
	 * @param yBounds the Y Size
	 */
	public GenericWarningMessage(String warningText, int xPos, int yPos, int xBounds, int yBounds) {
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		setBounds(xPos, yPos, xBounds, yBounds);
		getContentPane().setLayout(new BorderLayout());
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					dispose();
				}
			});
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}

		JTextPane warningTextField = new JTextPane();
		warningTextField.setBackground(UIManager.getColor("CheckBox.background"));
		warningTextField.setEditable(false);
		warningTextField.setFont(new Font("Tahoma", Font.PLAIN, 16));
		warningTextField.setText(warningText);
		getContentPane().add(warningTextField, BorderLayout.CENTER);
	}


}
