package messengerSystem;

/**
 * Object to control the interaction between the verification method and the gui windows
 * @author Sarah Schumann
 */
public class SigKeyQueryInteractionObject {

    /**
     * Flags for controlling the interaction
     */
    private boolean continueVerify, abortVerify, discardMessage;

    /**
     * Constructor; sets the variables to the default values
     */
    public SigKeyQueryInteractionObject() {
        continueVerify = false;
        abortVerify = false;
        discardMessage = false;
    }

    /**
     * @return the value of continueVerify
     */
    public boolean isContinueVerify () {
        return continueVerify;
    }

    /**
     * @return the value of abortVerify
     */
    public boolean isAbortVerify () {
        return abortVerify;
    }

    /**
     * @return the value of discardMessage
     */
    public boolean isDiscardMessage () {
        return discardMessage;
    }

    /**
     * Sets the value of continueVerify.
     * Should be set to true, if there should be a valid key available now, and the verification can proceed.
     * @param continueVerify the value to set the variable to
     */
    public void setContinueVerify (boolean continueVerify) {
        this.continueVerify = continueVerify;
    }

    /**
     * Sets the value of abortVerify.
     * Should be set to true, if the verification should be aborted and the message read without authentication.
     * @param abortVerify the value to set the variable to
     */
    public void setAbortVerify (boolean abortVerify) {
        this.abortVerify = abortVerify;
    }

    /**
     * Sets the value of discardMessage.
     * Should be set to true, if the verification should be aborted and the message discarded.
     * @param discardMessage the value to set the variable to
     */
    public void setDiscardMessage (boolean discardMessage) {
        this.discardMessage = discardMessage;
    }
}
