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

public class SHA256withRSAAuthenticationGUI extends SHA256withRSAAuthentication {

    /**
     * Flags for the verification process
     */
    public static boolean continueVerify, abortVerify, discardMessage;

    /**
     * Logger for error handling
     */
    private static Log log = new Log(SHA256withRSAAuthentication.class.getName(), LogSensitivity.WARNING);

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
                continueVerify = false;
                abortVerify = false;
                discardMessage = false;
                boolean invalidKey = true;
                new CESignatureQueryDialog(sender);
                while (invalidKey) {
                    if (abortVerify) {
                        log.logWarning("Verification aborted, message will be shown unauthenticated.", new NoValidPublicKeyException(sender));
                        return true;
                    } else if (discardMessage) {
                        log.logError("Verification aborted, message will be discarded.", new NoValidPublicKeyException(sender));
                        return false;
                    } else if (continueVerify) {
                        PublicKey publicKey = getPublicKeyFromString(senderCE.getSigKey());
                        if (publicKey == null) {
                            new CESignatureQueryDialog(sender);
                            GenericWarningMessage noKeyWarning = new GenericWarningMessage("Invalid public key entered.");
                            noKeyWarning.setAlwaysOnTop(true);
                            continueVerify = false;
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
