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
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JSeparator;
import java.awt.GridLayout;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;
import javax.swing.JTextPane;

/**This Menu will contain general information about this application
 * 
 * @author Jonas Huehne
 */
public class AboutPage extends JFrame {

	private final JPanel contentPanel = new JPanel();


	/**
	 * Create the dialog.
	 */
	public AboutPage() {
		setTitle("About");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		setBounds(100, 100, 538, 600);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			Box verticalBox = Box.createVerticalBox();
			contentPanel.add(verticalBox, BorderLayout.NORTH);
			{
				JLabel TitleLabel = new JLabel("Quantum Network Control Center");
				TitleLabel.setFont(new Font("Tahoma", Font.BOLD, 24));
				verticalBox.add(TitleLabel);
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
			{
				JLabel VersionLabel = new JLabel("Version 1.0");
				VersionLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
				verticalBox.add(VersionLabel);
			}
			{
				JSeparator separator = new JSeparator();
				verticalBox.add(separator);
			}
		}
		{
			JTextPane txtpnThisApplicationWas = new JTextPane();
			txtpnThisApplicationWas.setFont(new Font("Tahoma", Font.PLAIN, 18));
			txtpnThisApplicationWas.setText("This Application was developed as part of the\r\nB.Sc. Computer Science Bachelor-Project WS2021\r\nat TU Darmstadt.\r\n\r\nIt is available under the CC BY 4.0 License:\r\nhttps://creativecommons.org/licenses/by/4.0/\r\n\r\nDevelopers of the QNCC:\r\nLukas Dentler\r\nAron Hernandez\r\nJonas H\u00FChne\r\nSasha Petri\r\nSarah Schumann\r\n\r\nSupervision:\r\nPhilipp Kockerols\r\n\r\nClient:\r\nMaximilian Tippmann,\r\nInstitute for Applied Physics\r\nLaser and Quantum Optics");
			txtpnThisApplicationWas.setEditable(false);
			contentPanel.add(txtpnThisApplicationWas, BorderLayout.CENTER);
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
