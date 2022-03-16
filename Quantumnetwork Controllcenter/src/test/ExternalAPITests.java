import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.crypto.SecretKey;

import messengerSystem.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import communicationList.Contact;
import externalAPI.ExternalAPI;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionManager;

public class ExternalAPITests {

	private Path currentWorkingDir = Path.of(Configuration.getBaseDirPath());
	private Path externalPath = currentWorkingDir.resolve("externalAPI");
	
	private final static byte[] key = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};
	
	@Test
	public void exportKeyByteArrayTest() {
		byte[] bytes = ExternalAPI.exportKeyByteArray("42debugging42");

		assertArrayEquals(bytes, key);
	}
	
	@Test
	public void exportKeyStringTest() {
		String string = ExternalAPI.exportKeyString("42debugging42");
		String expected = "1234567891011121314151617181920212223242526272829303132";
		
		assertEquals(string, expected);
	}
	
	@Test
	public void exportKeySecretKeyTest() {
		SecretKey sk = ExternalAPI.exportKeySecretKey("42debugging42");
		byte[] keyBytes = sk.getEncoded();
		
		assertArrayEquals(keyBytes, key);
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"jpgTest.jpg", "odsTest.ods", "odtTest.odt", "pdfTest.pdf", "txtTest.txt", "zipTest.zip"})
	public void encryptionDecryptionTest(String fileName) throws IOException{
		Path inputFile = externalPath.resolve(fileName);
		byte[] inputBytes = Files.readAllBytes(inputFile);
		
		
		ExternalAPI.encryptFile("42debugging42", fileName);
		ExternalAPI.decryptFile("42debugging42", "encrypted_" + fileName);
		
		Path outputFile = externalPath.resolve("decrypted_encrypted_" + fileName);
		byte[] outputBytes = Files.readAllBytes(outputFile);
	
		
		assertNotNull(inputBytes);
		assertNotNull(outputBytes);
		assertArrayEquals(inputBytes, outputBytes);
	}
	
	@BeforeEach
    void setup () {
        QuantumnetworkControllcenter.initialize(null);
    }

    @AfterEach
    void cleanUp () {
        ArrayList<Contact> entries = QuantumnetworkControllcenter.communicationList.queryAll();
        for (Contact e : entries) {
            QuantumnetworkControllcenter.communicationList.delete(e.getName());
        }

        QuantumnetworkControllcenter.authentication.deleteSignatureKeys();
    }
    
    @Nested
    class testEncryptedMessage {

        @Test
        // only realistically testable if signature key generation, signing and sending of messages work
        void testLocalSendAuthenticatedMessage() throws IOException {
            QuantumnetworkControllcenter.authentication.generateSignatureKeyPair();
            String otherPublicKeyString =
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                            "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                            "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                            "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                            "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                            "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                            "1QIDAQAB";
            
            // Simulates the machine of our communication partner
            int ourServerPort = QuantumnetworkControllcenter.conMan.getLocalPort();
            ConnectionManager otherCM = new ConnectionManager("127.0.0.1", ourServerPort + 1);
            
            QuantumnetworkControllcenter.communicationList.insert("Alice", "127.0.0.1", 6603, Utils.readKeyStringFromFile("signature.pub"));
            QuantumnetworkControllcenter.communicationList.insert("42debugging42", "127.0.0.1", 6604, otherPublicKeyString);

            QuantumnetworkControllcenter.initialize(null);
            QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 6603);
            QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("42debugging42", 6604);

            QuantumnetworkControllcenter.conMan.getConnectionEndpoint("42debugging42").waitForConnection();
            QuantumnetworkControllcenter.conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 6604);

            boolean result = ExternalAPI.sendEncryptedTxtFile("42debugging42", "message.txt");
            assertTrue(result);
        }
        
        @Test
        // only realistically testable if signature key generation, signing, verifying, sending and receiving of messages work
        void testLocalReceiveAuthenticatedMessage() throws IOException {
            QuantumnetworkControllcenter.authentication.generateSignatureKeyPair();
            String otherPublicKeyString =
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                            "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                            "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                            "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                            "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                            "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                            "1QIDAQAB";
            QuantumnetworkControllcenter.communicationList.insert("41debugging41", "127.0.0.1", 9303, Utils.readKeyStringFromFile("signature.pub"));
            QuantumnetworkControllcenter.communicationList.insert("42debugging42", "127.0.0.1", 8303, otherPublicKeyString);

            QuantumnetworkControllcenter.initialize();
            QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("41debugging41", 9303);
            QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("42debugging42", 8303);

            QuantumnetworkControllcenter.conMan.getConnectionEndpoint("42debugging42").waitForConnection();
            QuantumnetworkControllcenter.conMan.getConnectionEndpoint("41debugging41").establishConnection("127.0.0.1", 8303);

            ExternalAPI.sendEncryptedTxtFile("42debugging42", "message.txt");
            
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu_MM_dd_HH_mm");
            LocalDateTime now = LocalDateTime.now();
            String currentDateTime = dateTimeFormatter.format(now);
    		System.out.println(currentDateTime);
            
    		Path received = externalPath.resolve(currentDateTime + "_" + "41debugging41" + ".txt");
    		
            ExternalAPI.receiveEncryptedTxtFile("41debugging41");
            
            Path sent = externalPath.resolve("message.txt");
    		
            byte[] inputBytes = Files.readAllBytes(sent);
            byte[] outputBytes = Files.readAllBytes(received);
            
            assertArrayEquals(inputBytes, outputBytes);
        }
    }
    
}