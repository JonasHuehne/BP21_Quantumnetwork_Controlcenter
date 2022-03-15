package messengerSystem;

import communicationList.Contact;
import exceptions.NoValidPublicKeyException;
import frame.QuantumnetworkControllcenter;
import graphicalUserInterface.CESignatureQueryDialog;
import graphicalUserInterface.GenericWarningMessage;
import networkConnection.ConnectionEndpoint;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

import java.security.PublicKey;

/**
 * A subclass to handle the interaction between the gui and the authentication process where necessary
 *
 * @author Sarah Schumann
 */
public class SHA256withRSAAuthenticationGUI extends SHA256withRSAAuthentication {

    /**
     * Logger for error handling
     */
    private static Log log = new Log(SHA256withRSAAuthentication.class.getName(), LogSensitivity.WARNING);

    /**
     * Method to verify a message with a signature, given a message, the signature and the sender name
     * (takes the public key from the corresponding entry in the communication list or the CE)
     * (uses a gui window to ask for a public signature key if there is non)
     * @param message the received signed message (without the signature)
     * @param receivedSignature the received signature
     * @param sender the sender of the message, needed to look up the public key in the communication list
     * @return true if the signature matches the message, false otherwise or if Error
     */
    @Override
    public boolean verify (final byte[] message, final byte[] receivedSignature,
                           final String sender) {
        Contact senderEntry = QuantumnetworkControllcenter.communicationList.query(sender);
        if(senderEntry == null
                || senderEntry.getSignatureKey().equals(Utils.NO_KEY)) {
            ConnectionEndpoint senderCE = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(sender);
            if (senderCE == null) {
                log.logError("Error: No connection endpoint for " + sender + " found.", new RuntimeException());
                return false;
            }
            String pubKeyString = senderCE.getSigKey();
            if (pubKeyString.equals("")) {
                SigKeyQueryInteractionObject sigKeyQuery = new SigKeyQueryInteractionObject();
                boolean invalidKey = true;
                new CESignatureQueryDialog(sender, sigKeyQuery);
                while (invalidKey) {
                    if (sigKeyQuery.isAbortVerify()) {
                        log.logWarning("Verification aborted, message will be shown unauthenticated.", new NoValidPublicKeyException(sender));
                        return true;
                    } else if (sigKeyQuery.isDiscardMessage()) {
                        log.logError("Verification aborted, message will be discarded.", new NoValidPublicKeyException(sender));
                        return false;
                    } else if (sigKeyQuery.isContinueVerify()) {
                        PublicKey publicKey = getPublicKeyFromString(senderCE.getSigKey());
                        if (publicKey == null) {
                            new CESignatureQueryDialog(sender, sigKeyQuery);
                            GenericWarningMessage noKeyWarning = new GenericWarningMessage("Invalid public key entered.");
                            noKeyWarning.setAlwaysOnTop(true);
                            sigKeyQuery.setContinueVerify(false);
                        } else {
                            invalidKey = false;
                        }
                    }
                }
            }
        }
        return super.verify(message, receivedSignature, sender);
    }
}
