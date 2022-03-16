package messengerSystem;

import exceptions.NoValidPublicKeyException;
import graphicalUserInterface.CESignatureQueryDialog;
import graphicalUserInterface.GenericWarningMessage;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**
 * A subclass to handle the interaction between the gui and the authentication process where necessary
 *
 * @author Sarah Schumann, Sasha Petri
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
    @Override // synchronized is needed for wait(..) to work
    public synchronized boolean verify (final byte[] message, final byte[] receivedSignature,
                           final String sender) {
            String pubKeyString = Utils.getPkIfPossible(sender);
            if (pubKeyString == null) {
                SigKeyQueryInteractionObject skq = new SigKeyQueryInteractionObject();
                new CESignatureQueryDialog(sender, skq);
                while (!(skq.isAbortVerify() || skq.isContinueVerify() || skq.isDiscardMessage())) { 
                	try { // wait for a bit between each check to not eat up the CPU
						wait(100);
					} catch (InterruptedException e) {
						// this happens if the message waiting thread is interrupted
						// <Sasha> as far as I know this shouldn't happen, but if it does
						// the verification fails and the event is logged
						log.logError("Verification aborted due to interrupt, message will be discarded.", e);
	                    return false;
					}
                }
                if (skq.isContinueVerify()) { // User has selected "ok" after entering a key
                	if (Utils.getPkIfPossible(sender) == null) { // if key was null or empty, repeat prompt
                		GenericWarningMessage noKeyWarning = new GenericWarningMessage("Invalid public key entered.");
                        noKeyWarning.setAlwaysOnTop(true);
                        return this.verify(message, receivedSignature, sender);
                	} else { // if valid key was entered, verify
                		return super.verify(message, receivedSignature, sender);
                	}
                } else if(skq.isDiscardMessage()) { // user discards the message
                	log.logError("Verification aborted, message will be discarded.", new NoValidPublicKeyException(sender));
                    return false;
                } else if(skq.isAbortVerify()) { // user wants to read message even though unverified
                	log.logWarning("Verification aborted, message will be shown unauthenticated.", new NoValidPublicKeyException(sender));
                    return true;
                }
            }
            // if a pk exists, use it to verify
            return super.verify(message, receivedSignature, sender);
     }
       
}
    
