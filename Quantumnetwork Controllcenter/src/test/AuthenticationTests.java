
import communicationList.Contact;
import frame.Configuration;
import messengerSystem.MessageSystem;
import frame.QuantumnetworkControllcenter;
import messengerSystem.SHA256withRSAAuthentication;
import messengerSystem.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * class for automated tests for the authentication
 * @author Sarah Schumann
 */
class AuthenticationTests {

    private static String currentPath;
    private static SHA256withRSAAuthentication authentication;

    @BeforeAll
    static void setup() {
        QuantumnetworkControllcenter.initialize(new String[]{"127.0.0.1", "8303"});
        currentPath = Configuration.getBaseDirPath();
        authentication = new SHA256withRSAAuthentication();
    }

    @AfterEach
    void cleanUp() {
        ArrayList<Contact> entries = QuantumnetworkControllcenter.communicationList.queryAll();
        for (Contact e : entries) {
            QuantumnetworkControllcenter.communicationList.delete(e.getName());
        }

        authentication.deleteSignatureKeys();
    }

    @Nested
    class TestUtilityAndSignatureKeyGeneration {

        @Test
        void testSignatureKeyGeneration() {
            // test generation
            boolean result1 = authentication.generateSignatureKeyPair();
            Assertions.assertTrue(result1);
            boolean result2 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.key"));
            Assertions.assertTrue(result2);
            boolean result3 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.pub"));
            Assertions.assertTrue(result3);

            // test method behaviour while file name exists already depending on overwrite
            boolean result4 = authentication.generateSignatureKeyPair("signature", true, false, false);
            Assertions.assertFalse(result4);
            boolean result5 = authentication.generateSignatureKeyPair("signature", true, false, true);
            Assertions.assertTrue(result5);

            // test creation with all params as false
            boolean result6 = authentication.generateSignatureKeyPair("signatureTest", false, false, false);
            Assertions.assertTrue(result6);
            boolean result7 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.key"));
            Assertions.assertTrue(result7);
            boolean result8 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.pub"));
            Assertions.assertTrue(result8);

            // test correct deletion of current signature keys (meaning correct ones stayed as set in block above
            boolean result9 = authentication.deleteSignatureKeys();
            Assertions.assertTrue(result9);
            boolean result10 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.key"));
            Assertions.assertFalse(result10);
            boolean result11 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signature.pub"));
            Assertions.assertFalse(result11);

            // set the other key as private key, test that only this one gets deleted and the other with the same name but different extension stays
            boolean result12 = authentication.setPrivateKey("signatureTest.key");
            Assertions.assertTrue(result12);
            boolean result13 = authentication.deleteSignatureKeys();
            Assertions.assertTrue(result13);
            boolean result14 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.key"));
            Assertions.assertFalse(result14);
            boolean result15 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.pub"));
            Assertions.assertTrue(result15);

            // test deletion of just one specific signature key
            boolean result16 = authentication.deleteSignatureKey("signatureTest.pub");
            Assertions.assertTrue(result16);
            boolean result17 = Files.exists(Path.of(currentPath + "SignatureKeys" + File.separator + "signatureTest.pub"));
            Assertions.assertFalse(result17);

            // test setPrivateKey und setPublicKey with a nonexistent file
            boolean result18 = authentication.setPrivateKey("somethingThatIsNotThere.key");
            Assertions.assertFalse(result18);
            boolean result19 = authentication.setPublicKey("somethingThatIsNotThere.pub");
            Assertions.assertFalse(result19);
        }

        @Test
        void testReadFromFiles() {
            String result1 = Utils.readKeyStringFromFile("pkForTesting_1.pub");
            Assertions.assertTrue(result1.startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv3OWQBpJ2PS"));
            Assertions.assertTrue(result1.endsWith("UTjJ2iU25z1TLtiqNTivymVD2tuHiVqcu0aZh4U0BS6H8jwMqCaX+RZJQIDAQAB"));

            String result2 = Utils.readKeyStringFromFile("test_public_key.pem");
            Assertions.assertTrue(result2.startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt9pZR4JMWTwXY6fVrDc9"));
            Assertions.assertTrue(result2.endsWith("hbnFxN5BYFNTARaaV8dajLixgWkatdFPy3TkVoe5dpaIYunKqHyqjRAyScxUxj8eAwIDAQAB"));

            String result3 = Utils.readKeyStringFromFile("test_private_key.pem");
            Assertions.assertTrue(result3.startsWith("MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC32llHgkxZPBdj" +
                    "p9WsNz1FsYuY0y1/sjacFDTlOUiw4BxVwc7mp8ZZl4+MynyZ318ioaXshqbRPdKV"));
            Assertions.assertTrue(result3.endsWith("HgtinT2wvAMGuyzkAXSZa8Z40KjmX2xyj6PdU9fjwVWkBaGkotMDGZTKbMGVMn3v" +
                    "7GsDvWQxXiWgc7Q7Z3UT0fSLAS8rUqVBt3S2jhy8Fk/v3LrG2ACyHkysZ/Qu89Wq6XSXtbgS25DXTFOCCU6UJPk="));

            // should throw an error with wrong key file format
            String result4 = Utils.readKeyStringFromFile("nonExistentKeyFile.something");
            Assertions.assertNull(result4);
            // should throw an error for not finding the file
            String result5 = Utils.readKeyStringFromFile("nonExistentKeyFile.pub");
            Assertions.assertNull(result5);

            String result6 = Utils.readKeyStringFromFile("test_private_key2");
            Assertions.assertNotNull(result6);
            String result7 = Utils.readKeyStringFromFile("test_public_key2.txt");
            Assertions.assertNotNull(result7);
        }

        @Test
        void testExistsValidKeyPair() {
            authentication.setPrivateKey("test_private_key.pem");
            authentication.setPublicKey("test_public_key.pem");

            boolean result1 = authentication.existsValidKeyPair();
            Assertions.assertTrue(result1);

            authentication.setPublicKey("");

            boolean result2 = authentication.existsValidKeyPair();
            Assertions.assertFalse(result2);

            authentication.setPrivateKey("");

            boolean result3 = authentication.existsValidKeyPair();
            Assertions.assertFalse(result3);
        }
    }

    @Nested
    class TestSignAndVerify {

        @Test
            // relies on signature key generation in authentication class
        void testSign() {
            authentication.generateSignatureKeyPair();

            byte[] result1 = authentication.sign(MessageSystem.stringToByteArray("Hello"));
            Assertions.assertNotNull(result1);
            authentication.deleteSignatureKeys();

            authentication.setPrivateKey("test_private_key.pem");
            byte[] result2 = authentication.sign(MessageSystem.stringToByteArray("Hello"));
            Assertions.assertNotNull(result2);
            authentication.setPrivateKey("");
        }

        @Test
            // only testable, if signing and signature key generation work
        void testVerifyTrue() {
            authentication.generateSignatureKeyPair();
            QuantumnetworkControllcenter.communicationList.insert("self", "127.0.0.1", 2303, Utils.readKeyStringFromFile("signature.pub"));

            byte[] signature = authentication.sign(MessageSystem.stringToByteArray("Hello"));
            boolean result = authentication.verify(MessageSystem.stringToByteArray("Hello"), signature, "self");
            Assertions.assertTrue(result);
        }

        @Test
            // only testable, if signing and signature key generation work
        void testVerifyFalse() {
            authentication.generateSignatureKeyPair();

            QuantumnetworkControllcenter.communicationList.insert("self", "127.0.0.1", 2303, Utils.readKeyStringFromFile("signature.pub"));
            byte[] signature1 = authentication.sign(MessageSystem.stringToByteArray("Hello"));
            boolean result1 = authentication.verify(MessageSystem.stringToByteArray("Hallo"), signature1, "self");
            Assertions.assertFalse(result1);

            String otherPublicKeyString =
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                            "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                            "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                            "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                            "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                            "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                            "1QIDAQAB";
            QuantumnetworkControllcenter.communicationList.insert("testSelf", "127.0.0.1", 2303, Utils.readKeyStringFromFile("signature.pub"));
            QuantumnetworkControllcenter.communicationList.insert("testOther", "128.0.0.1", 2505, otherPublicKeyString);

            byte[] signature2 = authentication.sign(MessageSystem.stringToByteArray("Hello"));
            boolean result2 = authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, "testOther");
            Assertions.assertFalse(result2);

            boolean result3 = authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, null);
            Assertions.assertFalse(result3);

            QuantumnetworkControllcenter.communicationList.updateSignatureKey("testSelf", "");
            boolean result4 = authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, "testSelf");
            Assertions.assertFalse(result4);

            QuantumnetworkControllcenter.communicationList.updateSignatureKey("testSelf", null);
            boolean result5 = authentication.verify(MessageSystem.stringToByteArray("Hello"), signature2, "testSelf");
            Assertions.assertFalse(result5);
        }
    }
}
