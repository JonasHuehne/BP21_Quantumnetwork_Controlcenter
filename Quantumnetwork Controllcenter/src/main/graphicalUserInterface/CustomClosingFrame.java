package graphicalUserInterface;

import javax.swing.JFrame;

import frame.QuantumnetworkControllcenter;

public class CustomClosingFrame extends JFrame {
	
	public CustomClosingFrame() {
		addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {

            	//Custom Window-Closing: Close all connections
            	System.out.println("Closing all Connections before Shutting down Application...");
            	QuantumnetworkControllcenter.guiWindow.shutdownUpdateService();
            	QuantumnetworkControllcenter.conMan.destroyAllConnectionEndpoints();
                super.windowClosing(e);
            }
        });
	}

}
