package messengerSystem;

public interface Authentication {

    String sign (final String message);

    boolean verify (final String message, final String receivedSignature, final String sender);

}
