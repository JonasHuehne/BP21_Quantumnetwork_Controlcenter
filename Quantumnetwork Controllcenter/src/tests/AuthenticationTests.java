package tests;

import CommunicationList.Database;
import MessengerSystem.Authentication;
import MessengerSystem.MessageSystem;
import frame.QuantumnetworkControllcenter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * class for automated tests for the authentication
 * @author Sarah Schumann
 */
class AuthenticationTests {
    // IMPORTANT: only run tests one by one. There might be problems if they interleave,
    // as they use the same database and always add and delete the test data.
    // Additionally, if they fail, you might need to make everything in the test above the first delete line a comment,
    // run the test again, delete do uncomment the rest, and then run the test again.


    @Test
    void testSignatureKeyGeneration () {
        boolean result1 = Authentication.generateSignatureKeyPair();
        Assertions.assertTrue(result1);

        boolean result2 = Files.exists(Path.of(System.getProperty("user.dir")
                + File.separator + "SignatureKeys" + File.separator + "signature.key"));
        Assertions.assertTrue(result2);

        boolean result3 = Files.exists(Path.of(System.getProperty("user.dir")
                + File.separator + "SignatureKeys" + File.separator + "signature.pub"));
        Assertions.assertTrue(result3);

        boolean result4 = Authentication.deleteSignatureKeys();
        Assertions.assertTrue(result4);
    }

    @Test
    // relies on signature key generation in authentication class
    void testSign () {
        Authentication.generateSignatureKeyPair();

        String result = Authentication.sign("Hello");
        Assertions.assertNotNull(result);

        Authentication.deleteSignatureKeys();
    }

    @Test
    // only testable, if signing and signature key generation work
    void testVerifyTrue () {
        Authentication.generateSignatureKeyPair();
        Database.insert("self", "127.0.0.1", 2303, Authentication.readPublicKeyStringFromFile("signature"));

        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hello", signature, "self");
        Assertions.assertTrue(result);

        Database.delete("self");
        Authentication.deleteSignatureKeys();
    }

    @Test
    // only testable, if signing and signature key generation work
    void testVerifyFalse () {
        Authentication.generateSignatureKeyPair();

        Database.insert("self", "127.0.0.1", 2303, Authentication.readPublicKeyStringFromFile("signature"));
        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hallo", signature, "self");
        Assertions.assertFalse(result);

        Database.delete("self");
        Authentication.deleteSignatureKeys();
    }

    @Test
    // only testable, if signing and signature key generation work
    void testVerifyFalse2 () {
        Authentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                "1QIDAQAB";
        Database.insert("self", "127.0.0.1", 2303, Authentication.readPublicKeyStringFromFile("signature"));
        Database.insert("other", "128.0.0.1", 2505, otherPublicKeyString);

        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hello", signature, "other");
        Assertions.assertFalse(result);

        Database.delete("self");
        Database.delete("other");
        Authentication.deleteSignatureKeys();
    }

    @Test
    // only realistically testable if signature key generation, signing and sending of messages work
    void testLocalSendAuthenticatedMessage () throws IOException {
        Authentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                "1QIDAQAB";
        Database.insert("Alice", "127.0.0.1", 2303, Authentication.readPublicKeyStringFromFile("signature"));
        Database.insert("Bob", "127.0.0.1", 3303, otherPublicKeyString);

        QuantumnetworkControllcenter.initialize();
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Alice", 2303);
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Bob", 3303);

        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Bob").waitForConnection();
        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3303);

        MessageSystem.setActiveConnection("Bob");
        boolean result = MessageSystem.sendAuthenticatedMessage("Hello");
        Assertions.assertTrue(result);

        Database.delete("Alice");
        Database.delete("Bob");
        Authentication.deleteSignatureKeys();
    }

    @Test
    // only realistically testable if signature key generation, signing, verifying, sending and receiving of messages work
    void testLocalReceiveAuthenticatedMessage () throws IOException {
        Authentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                        "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                        "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                        "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                        "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                        "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                        "1QIDAQAB";
        Database.insert("Alice", "127.0.0.1", 2303, Authentication.readPublicKeyStringFromFile("signature"));
        Database.insert("Bob", "127.0.0.1", 3303, otherPublicKeyString);

        QuantumnetworkControllcenter.initialize();
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Alice", 2303);
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Bob", 3303);

        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Bob").waitForConnection();
        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3303);

        MessageSystem.setActiveConnection("Bob");
        MessageSystem.sendAuthenticatedMessage("Hello, how are you?");

        MessageSystem.setActiveConnection("Alice");
        String message = MessageSystem.readAuthenticatedMessage();
        Assertions.assertEquals("Hello, how are you?", message);

        Database.delete("Alice");
        Database.delete("Bob");
        Authentication.deleteSignatureKeys();
    }

    @Test
    // only realistically testable if signature key generation, signing, verifying, sending and receiving of messages work
    void testFalseLocalAuthenticatedMessage () throws IOException {
        Authentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                        "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                        "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                        "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                        "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                        "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                        "1QIDAQAB";
        Database.insert("Alice", "127.0.0.1", 2303, Authentication.readPublicKeyStringFromFile("signature"));
        Database.insert("Bob", "127.0.0.1", 3303, otherPublicKeyString);

        QuantumnetworkControllcenter.initialize();
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Alice", 2303);
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Bob", 3303);

        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Bob").waitForConnection();
        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3303);

        MessageSystem.setActiveConnection("Alice");
        MessageSystem.sendAuthenticatedMessage("Hello");

        MessageSystem.setActiveConnection("Bob");
        String message = MessageSystem.readAuthenticatedMessage();
        Assertions.assertNull(message);

        Database.delete("Alice");
        Database.delete("Bob");
        Authentication.deleteSignatureKeys();
    }

}
