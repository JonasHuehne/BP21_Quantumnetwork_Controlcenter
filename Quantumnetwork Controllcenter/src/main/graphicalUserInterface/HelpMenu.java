package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**This Menu will contain an instructional manual
 * 
 * @author Jonas Huehne
 * @deprecated Opening "Help" folder instead of in-GUI Solution
 */
public class HelpMenu extends JFrame {

	private static final long serialVersionUID = -6654948406963093431L;
	
	private final JPanel contentPanel = new JPanel();


	/**
	 * Create the dialog.
	 */
	public HelpMenu() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		setBounds(100, 100, 538, 559);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JLabel lblNewLabel = new JLabel("This window will contain information on how to use the program.");
			contentPanel.add(lblNewLabel);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton closeButton = new JButton("Close");
				closeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
						dispose();
					}
				});
				closeButton.setActionCommand("OK");
				buttonPane.add(closeButton);
				getRootPane().setDefaultButton(closeButton);
			}
		}
	}

}
