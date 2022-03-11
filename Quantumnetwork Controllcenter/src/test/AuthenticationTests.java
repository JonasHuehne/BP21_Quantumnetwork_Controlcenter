
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import communicationList.Contact;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;

/**
 * class for automated tests for the authentication
 * @author Sarah Schumann
 */
class AuthenticationTests {

    private static String currentPath;

    @BeforeAll
    static void setup () {
        QuantumnetworkControllcenter.initialize(new String[]{"127.0.0.1", "8303"});
        currentPath = Configuration.getBaseDirPath();
    }

    @AfterEach
    void cleanUp () {
        ArrayList<Contact> entries = QuantumnetworkControllcenter.communicationList.queryAll();
        for (Contact e : entries) {
            QuantumnetworkControllcenter.communicationList.delete(e.getName());
        }

        SHA256withRSAAuthentication.deleteSignatureKeys();
    }

    @Nested
    class testUtilityAndSignatureKeyGeneration {

        @Test
        void testSignatureKeyGeneration() {
            // test generation
            boolean result1 = SHA256withRSAAuthentication.generateSignatureKeyPair();
            Assertions.assertTrue(result1);
            boolean result2 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.key"));
            Assertions.assertTrue(result2);
            boolean result3 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.pub"));
            Assertions.assertTrue(result3);

            // test method behaviour while file name exists already depending on overwrite
            boolean result4 = SHA256withRSAAuthentication.generateSignatureKeyPair("signature", true, false, false);
            Assertions.assertFalse(result4);
            boolean result5 = SHA256withRSAAuthentication.generateSignatureKeyPair("signature", true, false, true);
            Assertions.assertTrue(result5);

            // test creation with all params as false
            boolean result6 = SHA256withRSAAuthentication.generateSignatureKeyPair("signatureTest", false, false, false);
            Assertions.assertTrue(result6);
            boolean result7 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.key"));
            Assertions.assertTrue(result7);
            boolean result8 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.pub"));
            Assertions.assertTrue(result8);

            // test correct deletion of current signature keys (meaning correct ones stayed as set in block above
            boolean result9 = SHA256withRSAAuthentication.deleteSignatureKeys();
            Assertions.assertTrue(result9);
            boolean result10 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.key"));
            Assertions.assertFalse(result10);
            boolean result11 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.pub"));
            Assertions.assertFalse(result11);

            // set the other key as private key, test that only this one gets deleted and the other with the same name but different extension stays
            boolean result12 = SHA256withRSAAuthentication.setPrivateKey("signatureTest.key");
            Assertions.assertTrue(result12);
            boolean result13 = SHA256withRSAAuthentication.deleteSignatureKeys();
            Assertions.assertTrue(result13);
            boolean result14 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.key"));
            Assertions.assertFalse(result14);
            boolean result15 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.pub"));
            Assertions.assertTrue(result15);

            // test deletion of just one specific signature key
            boolean result16 = SHA256withRSAAuthentication.deleteSignatureKey("signatureTest.pub");
            Assertions.assertTrue(result16);
            boolean result17 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.pub"));
            Assertions.assertFalse(result17);

            // test setPrivateKey und setPublicKey with a nonexistent file
            boolean result18 = SHA256withRSAAuthentication.setPrivateKey("somethingThatIsNotThere.key");
            Assertions.assertFalse(result18);
            boolean result19 = SHA256withRSAAuthentication.setPublicKey("somethingThatIsNotThere.pub");
            Assertions.assertFalse(result19);
        }

        @Test
        void testReadFromFiles() {
            String result1 = SHA256withRSAAuthentication.readKeyStringFromFile("pkForTesting_1.pub");
            Assertions.assertTrue(result1.startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv3OWQBpJ2PS"));
            Assertions.assertTrue(result1.endsWith("UTjJ2iU25z1TLtiqNTivymVD2tuHiVqcu0aZh4U0BS6H8jwMqCaX+RZJQIDAQAB"));

            String result2 = SHA256withRSAAuthentication.readKeyStringFromFile("test_public_key.pem");
            Assertions.assertTrue(result2.startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt9pZR4JMWTwXY6fVrDc9"));
            Assertions.assertTrue(result2.endsWith("hbnFxN5BYFNTARaaV8dajLixgWkatdFPy3TkVoe5dpaIYunKqHyqjRAyScxUxj8eAwIDAQAB"));

            String result3 = SHA256withRSAAuthentication.readKeyStringFromFile("test_private_key.pem");
            Assertions.assertTrue(result3.startsWith("MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC32llHgkxZPBdj" +
                    "p9WsNz1FsYuY0y1/sjacFDTlOUiw4BxVwc7mp8ZZl4+MynyZ318ioaXshqbRPdKV"));
            Assertions.assertTrue(result3.endsWith("HgtinT2wvAMGuyzkAXSZa8Z40KjmX2xyj6PdU9fjwVWkBaGkotMDGZTKbMGVMn3v" +
                    "7GsDvWQxXiWgc7Q7Z3UT0fSLAS8rUqVBt3S2jhy8Fk/v3LrG2ACyHkysZ/Qu89Wq6XSXtbgS25DXTFOCCU6UJPk="));

            // should throw an error with wrong key file format
            String result4 = SHA256withRSAAuthentication.readKeyStringFromFile("nonExistentKeyFile.something");
            Assertions.assertNull(result4);
            // should throw an error for not finding the file
            String result5 = SHA256withRSAAuthentication.readKeyStringFromFile("nonExistentKeyFile.pub");
            Assertions.assertNull(result5);

            String result6 = SHA256withRSAAuthentication.readKeyStringFromFile("test_private_key2");
            Assertions.assertNotNull(result6);
            String result7 = SHA256withRSAAuthentication.readKeyStringFromFile("test_public_key2.txt");
            Assertions.assertNotNull(result7);
        }
    }

    @Nested
    class testSignAndVerify {

        @Test
        // relies on signature key generation in authentication class
        void testSign() {
            SHA256withRSAAuthentication.generateSignatureKeyPair();

            byte[] result1 = QuantumnetworkControllcenter.authentication.sign(MessageSystem.stringToByteArray("Hello"));
            Assertions.assertNotNull(result1);
            SHA256withRSAAuthentication.deleteSignatureKeys();

            SHA256withRSAAuthentication.setPrivateKey("test_private_key.pem");
            byte[] result2 = QuantumnetworkControllcenter.authentication.sign(MessageSystem.stringToByteArray("Hello"));
            Assertions.assertNotNull(result2);
            SHA256withRSAAuthentication.setPrivateKey("");
        }

        @Test
        // only testable, if signing and signature key generation work
        void testVerifyTrue() {
            SHA256withRSAAuthentication.generateSignatureKeyPair();
            QuantumnetworkControllcenter.communicationList.insert("self", "127.0.0.1", 2303, SHA256withRSAAuthentication.readKeyStringFromFile("signature.pub"));

            byte[] signature = QuantumnetworkControllcenter.authentication.sign(MessageSystem.stringToByteArray("Hello"));
            boolean result = QuantumnetworkControllcenter.authentication.verify(MessageSystem.stringToByteArray("Hello"), signature, "self");
            Assertions.assertTrue(result);
        }

        @Test
        // only testable, if signing and signature key generation work
        void testVerifyFalse() {
            SHA256withRSAAuthentication.generateSignatureKeyPair();

            QuantumnetworkControllcenter.communicationList.insert("self", "127.0.0.1", 2303, SHA256withRSAAuthentication.readKeyStringFromFile("signature.pub"));
            byte[] signature1 = QuantumnetworkControllcenter.authentication.sign(MessageSystem.stringToByteArray("Hello"));
            boolean result1 = QuantumnetworkControllcenter.authentication.verify(MessageSystem.stringToByteArray("Hallo"), signature1, "self");
            Assertions.assertFalse(result1);

            String otherPublicKeyString =
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                            "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                            "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                            "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                            "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                            "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                            "1QIDAQAB";
            QuantumnetworkControllcenter.communicationList.insert("testSelf", "127.0.0.1", 2303, SHA256withRSAAuthentication.readKeyStringFromFile("signature.pub"));
            QuantumnetworkControllcenter.communicationList.insert("testOther", "128.0.0.1", 2505, otherPublicKeyString);

            byte[] signature2 = QuantumnetworkControllcenter.authentication.sign(MessageSystem.stringToByteArray("Hello"));
            boolean result2 = QuantumnetworkControllcenter.authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, "testOther");
            Assertions.assertFalse(result2);

            boolean result3 = QuantumnetworkControllcenter.authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, null);
            Assertions.assertFalse(result3);

            QuantumnetworkControllcenter.communicationList.updateSignatureKey("testSelf", "");
            boolean result4 = QuantumnetworkControllcenter.authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, "testSelf");
            Assertions.assertFalse(result4);

            QuantumnetworkControllcenter.communicationList.updateSignatureKey("testSelf", null);
            boolean result5 = QuantumnetworkControllcenter.authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, "testSelf");
            Assertions.assertFalse(result5);
        }
    }

 }