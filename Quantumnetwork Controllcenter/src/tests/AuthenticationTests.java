package tests;

import communicationList.DbObject;
import messengerSystem.SHA256withRSAAuthentication;
import messengerSystem.MessageSystem;
import frame.QuantumnetworkControllcenter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * class for automated tests for the authentication
 * @author Sarah Schumann
 */
class AuthenticationTests {

    @BeforeEach
    void setup () {
        QuantumnetworkControllcenter.initialize();
    }

    @AfterEach
    void cleanUp () {
        ArrayList<DbObject> entries = QuantumnetworkControllcenter.communicationList.queryAll();
        for (DbObject e : entries) {
            QuantumnetworkControllcenter.communicationList.delete(e.getName());
        }

        SHA256withRSAAuthentication.deleteSignatureKeys();
    }

    @Test
    void testSignatureKeyGeneration () {
        boolean result1 = SHA256withRSAAuthentication.generateSignatureKeyPair();
        Assertions.assertTrue(result1);

        boolean result2 = Files.exists(Path.of(System.getProperty("user.dir")
                + File.separator + "SignatureKeys" + File.separator + "signature.key"));
        Assertions.assertTrue(result2);

        boolean result3 = Files.exists(Path.of(System.getProperty("user.dir")
                + File.separator + "SignatureKeys" + File.separator + "signature.pub"));
        Assertions.assertTrue(result3);

        boolean result4 = SHA256withRSAAuthentication.deleteSignatureKeys();
        Assertions.assertTrue(result4);
    }

    @Test
        // relies on signature key generation in authentication class
    void testSign () {
        SHA256withRSAAuthentication.generateSignatureKeyPair();

        String result = QuantumnetworkControllcenter.authentication.sign("Hello");
        Assertions.assertNotNull(result);
    }

    @Test
        // only testable, if signing and signature key generation work
    void testVerifyTrue () {
        SHA256withRSAAuthentication.generateSignatureKeyPair();
        QuantumnetworkControllcenter.communicationList.insert("self", "127.0.0.1", 2303, SHA256withRSAAuthentication.readPublicKeyStringFromFile("signature"));

        String signature = QuantumnetworkControllcenter.authentication.sign("Hello");
        boolean result = QuantumnetworkControllcenter.authentication.verify("Hello", signature, "self");
        Assertions.assertTrue(result);
    }

    @Test
        // only testable, if signing and signature key generation work
    void testVerifyFalse () {
        SHA256withRSAAuthentication.generateSignatureKeyPair();

        QuantumnetworkControllcenter.communicationList.insert("self", "127.0.0.1", 2303, SHA256withRSAAuthentication.readPublicKeyStringFromFile("signature"));
        String signature = QuantumnetworkControllcenter.authentication.sign("Hello");
        boolean result = QuantumnetworkControllcenter.authentication.verify("Hallo", signature, "self");
        Assertions.assertFalse(result);
    }

    @Test
        // only testable, if signing and signature key generation work
    void testVerifyFalse2 () {
        SHA256withRSAAuthentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                        "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                        "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                        "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                        "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                        "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                        "1QIDAQAB";
        QuantumnetworkControllcenter.communicationList.insert("self", "127.0.0.1", 2303, SHA256withRSAAuthentication.readPublicKeyStringFromFile("signature"));
        QuantumnetworkControllcenter.communicationList.insert("other", "128.0.0.1", 2505, otherPublicKeyString);

        String signature = QuantumnetworkControllcenter.authentication.sign("Hello");
        boolean result = QuantumnetworkControllcenter.authentication.verify("Hello", signature, "other");
        Assertions.assertFalse(result);
    }

    @Test
        // only realistically testable if signature key generation, signing and sending of messages work
    void testLocalSendAuthenticatedMessage () throws IOException {
        SHA256withRSAAuthentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                        "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                        "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                        "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                        "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                        "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                        "1QIDAQAB";
        QuantumnetworkControllcenter.communicationList.insert("Alice", "127.0.0.1", 2303, SHA256withRSAAuthentication.readPublicKeyStringFromFile("signature"));
        QuantumnetworkControllcenter.communicationList.insert("Bob", "127.0.0.1", 3303, otherPublicKeyString);

        QuantumnetworkControllcenter.initialize();
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Alice", 2303);
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Bob", 3303);

        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Bob").waitForConnection();
        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3303);

        boolean result = MessageSystem.sendAuthenticatedMessage("Bob", "Hello");
        Assertions.assertTrue(result);
    }

    @Test
        // only realistically testable if signature key generation, signing, verifying, sending and receiving of messages work
    void testLocalReceiveAuthenticatedMessage () throws IOException {
        SHA256withRSAAuthentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                        "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                        "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                        "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                        "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                        "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                        "1QIDAQAB";
        QuantumnetworkControllcenter.communicationList.insert("Alice", "127.0.0.1", 2303, SHA256withRSAAuthentication.readPublicKeyStringFromFile("signature"));
        QuantumnetworkControllcenter.communicationList.insert("Bob", "127.0.0.1", 3303, otherPublicKeyString);

        QuantumnetworkControllcenter.initialize();
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Alice", 2303);
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Bob", 3303);

        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Bob").waitForConnection();
        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3303);

        MessageSystem.sendAuthenticatedMessage("Bob", "Hello, how are you?");

        String message = MessageSystem.readAuthenticatedMessage("Alice");
        Assertions.assertEquals(message, "Hello, how are you?");
    }

    @Test
        // only realistically testable if signature key generation, signing, verifying, sending and receiving of messages work
    void testFalseLocalAuthenticatedMessage () throws IOException {
        SHA256withRSAAuthentication.generateSignatureKeyPair();
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                        "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                        "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                        "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                        "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                        "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                        "1QIDAQAB";
        QuantumnetworkControllcenter.communicationList.insert("Alice", "127.0.0.1", 2303, SHA256withRSAAuthentication.readPublicKeyStringFromFile("signature"));
        QuantumnetworkControllcenter.communicationList.insert("Bob", "127.0.0.1", 3303, otherPublicKeyString);

        QuantumnetworkControllcenter.initialize();
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Alice", 2303);
        QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Bob", 3303);

        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Bob").waitForConnection();
        QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3303);

        MessageSystem.sendAuthenticatedMessage("Alice", "Hello");

        String message = MessageSystem.readAuthenticatedMessage("Bob");
        Assertions.assertNull(message);
    }
}