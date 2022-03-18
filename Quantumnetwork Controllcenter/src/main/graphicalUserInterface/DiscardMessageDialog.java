package graphicalUserInterface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import messengerSystem.SigKeyQueryInteractionObject;

/**
 * This is a dialog to ask the user whether to discard a message that could not be authenticated
 * because of a missing valid key
 *
 * @author Sarah Schumann
 */
public class DiscardMessageDialog extends JDialog {


	private static final long serialVersionUID = -2813240682818696371L;

    public DiscardMessageDialog(String connectionID, SigKeyQueryInteractionObject sigKeyQuery) {
    	setTitle("Discard Message?");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setVisible(true);

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
            JButton readButton = new JButton("Read");
            readButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    dispose();
                    sigKeyQuery.setAbortVerify(true);
                }
            });
            readButton.setActionCommand("Read");
            buttonPane.add(readButton);
        }
        {
            JButton discardButton = new JButton("Discard");
            discardButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    dispose();
                    sigKeyQuery.setDiscardMessage(true);
                }
            });
            discardButton.setActionCommand("Discard");
            buttonPane.add(discardButton);
            getRootPane().setDefaultButton(discardButton);
        }

        JTextPane warningTextField = new JTextPane();
        warningTextField.setBackground(UIManager.getColor("CheckBox.background"));
        warningTextField.setEditable(false);
        warningTextField.setFont(new Font("Tahoma", Font.PLAIN, 16));
        warningTextField.setText("The message could not be authenticated "
                + "because there is no valid public key set for connection to " + connectionID
                + ". Do you want to discard the message, or read it being treated as verified?");
        getContentPane().add(warningTextField, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                dispose();
                sigKeyQuery.setDiscardMessage(true);
            }
        });
    }
}
