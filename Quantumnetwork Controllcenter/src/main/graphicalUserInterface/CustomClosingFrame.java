package graphicalUserInterface;

import javax.swing.JFrame;

import frame.QuantumnetworkControllcenter;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**This overwrites a method that is called when the window is closed with the X in the top right corner.
 * It causes all Connection Endpoints to shutdown in a network-friendly way.
 * @author Jonas Huehne
 *
 */
public class CustomClosingFrame extends JFrame {
	
	private static final long serialVersionUID = -2302659489326387423L;
	private static Log log = new Log(CustomClosingFrame.class.getName(), LogSensitivity.INFO);

	public CustomClosingFrame() {
		addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {

            	//Custom Window-Closing: Close all connections
            	log.logInfo("Closing all Connections before Shutting down Application...");
            	QuantumnetworkControllcenter.guiWindow.shutdownUpdateService();
            	QuantumnetworkControllcenter.conMan.destroyAllConnectionEndpoints();
                super.windowClosing(e);
            }
        });
	}

}
