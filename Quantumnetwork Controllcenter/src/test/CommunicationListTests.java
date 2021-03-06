
import java.io.IOException;
import java.util.ArrayList;

import messengerSystem.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import communicationList.CommunicationList;
import communicationList.Contact;
import communicationList.SQLiteCommunicationList;
import frame.Configuration;

/**
 * automated tests for interacting with the communication list db
 * @author Sarah Schumann
 */
class CommunicationListTests {

    CommunicationList db;

    @BeforeEach
    // only works if no problem with delete and queryAll
    void setup() {
        try {
            Configuration.findProperties();
            Configuration.createFolders();
        } catch (IOException e) {
            System.err.println("Error during test setup: " + e);
        }
        db = new SQLiteCommunicationList();
        ArrayList<Contact> entries = db.queryAll();
        for (Contact e : entries) {
            db.delete(e.getName());
        }
    }

    @AfterEach
    // only works if no problem with delete and queryAll
    void cleanUp() {
        ArrayList<Contact> entries = db.queryAll();
        for (Contact e : entries) {
            db.delete(e.getName());
        }
    }

    @Test
    void testInsertDelete() {
        boolean result1 = db.insert("Name1", "155.155.155.155", 5, "ABC");
        Assertions.assertTrue(result1);
        boolean result2 = db.insert("Name2", "166.166.166.166", 7, "");
        Assertions.assertTrue(result2);

        boolean result3 = db.delete("Name1");
        Assertions.assertTrue(result3);
        Contact result4 = db.query("Name2");
        Assertions.assertNotNull(result4);

        boolean result5 = db.delete("Name3");
        Assertions.assertTrue(result5);
        Contact result6 = db.query("Name2");
        Assertions.assertNotNull(result6);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> db.delete(null),
                "Input was null");

        boolean result7 = db.insert(null, "0.0.0.0", 5, "");
        Assertions.assertFalse(result7);
        int result8 = db.queryAll().size();
        Assertions.assertEquals(1, result8);

        boolean result9 = db.insert("Name4", null, 5, "");
        Assertions.assertFalse(result9);
        Contact result10 = db.query("Name4");
        Assertions.assertNull(result10);

        boolean result11 = db.insert("Name5", "2.2.2.2", 6, null);
        Assertions.assertTrue(result11);
        Contact result12 = db.query("Name5");
        Assertions.assertNull(result12.getSignatureKey());
    }

    @Test
    void testUpdate() {
        boolean result1 = db.insert("Name1", "155.155.155.155", 5, "ABC");
        Assertions.assertTrue(result1);

        boolean result2 = db.updateName("Name1", "Name2");
        Assertions.assertTrue(result2);
        Contact result3 = db.query("Name1");
        Assertions.assertNull(result3);
        Contact result4 = db.query("Name2");
        Assertions.assertNotNull(result4);

        boolean result5 = db.updateIP("Name2", "144.144.144.144");
        Assertions.assertTrue(result5);
        String result6 = db.query("Name2").getIpAddress();
        Assertions.assertEquals("144.144.144.144", result6);

        boolean result7 = db.updatePort("Name2", 7);
        Assertions.assertTrue(result7);
        int result8 = db.query("Name2").getPort();
        Assertions.assertEquals(7, result8);

        boolean result9 = db.updateSignatureKey("Name2", "DEF");
        Assertions.assertTrue(result9);
        String result10 = db.query("Name2").getSignatureKey();
        Assertions.assertEquals("DEF", result10);

        boolean result11 = db.updateName("Name2", null);
        Assertions.assertFalse(result11);
        Contact result12 = db.query("Name2");
        Assertions.assertNotNull(result12);

        boolean result13 = db.updateIP("Name2", null);
        Assertions.assertFalse(result13);
        String result14 = db.query("Name2").getIpAddress();
        Assertions.assertEquals("144.144.144.144", result14);
    }

    @Test
    void testQuery() {
        db.insert("Name1", "155.155.155.155", 5, "ABC");
        db.insert("Name2", "154.154.154.154", 7, "DEF");
        db.insert("Name3", "133.133.133.133", 2, "GHI");

        Contact testObject1 = db.query("Name2");
        Assertions.assertEquals("Name2", testObject1.getName());
        Assertions.assertEquals("154.154.154.154", testObject1.getIpAddress());
        Assertions.assertEquals(7, testObject1.getPort());
        Assertions.assertEquals("DEF", testObject1.getSignatureKey());

        Contact testObject2 = db.query("Name1");
        Assertions.assertEquals("Name1", testObject2.getName());
        Assertions.assertEquals("155.155.155.155", testObject2.getIpAddress());
        Assertions.assertEquals(5, testObject2.getPort());
        Assertions.assertEquals("ABC", testObject2.getSignatureKey());

        Contact testObject3 = db.query("133.133.133.133", 2);
        Assertions.assertEquals("Name3", testObject3.getName());
        Assertions.assertEquals("133.133.133.133", testObject3.getIpAddress());
        Assertions.assertEquals(2, testObject3.getPort());
        Assertions.assertEquals("GHI", testObject3.getSignatureKey());

        Contact testObject4 = db.query("Namee2");
        Assertions.assertNull(testObject4);
        Contact testObject5 = db.query("Name2", 5);
        Assertions.assertNull(testObject5);
    }

    @Test
    void testQueryAll() {
        db.insert("Name1", "155.155.155.155", 5, "ABC");
        db.insert("Name2", "154.154.154.154", 7, "DEF");
        db.insert("Name3", "133.133.133.133", 2, "GHI");

        ArrayList<Contact> testList = db.queryAll();
        Assertions.assertEquals("Name1", testList.get(0).getName());
        Assertions.assertEquals(7, testList.get(1).getPort());
        Assertions.assertEquals("133.133.133.133", testList.get(2).getIpAddress());
        Assertions.assertEquals("DEF", testList.get(1).getSignatureKey());
    }

    @Test
    void testToString() {
        db.insert("test1", "12.12.12.12", 5, null);
        String result1 = db.query("test1").toString();
        Assertions.assertEquals("Name: test1, IP Address: 12.12.12.12, Port: 5", result1);

        db.updateSignatureKey("test1", "");
        String result2 = db.query("test1").toString();
        Assertions.assertEquals("Name: test1, IP Address: 12.12.12.12, Port: 5", result2);

        db.updateSignatureKey("test1", Utils.readKeyStringFromFile("pkForTesting_1.pub"));
        String result3 = db.query("test1").toString();
        // expects 7 letters of the public key, check needs to be changed if variable in Contact is changed
        Assertions.assertEquals("Name: test1, IP Address: 12.12.12.12, Port: 5, Public Key: MIIBIjA...", result3);

        db.updateIP("test1", "5.5.5.5");
        db.updatePort("test1", 900);
        db.updateName("test1", "test2");
        String result4 = db.query("test2").toString();
        Assertions.assertEquals("Name: test2, IP Address: 5.5.5.5, Port: 900, Public Key: MIIBIjA...", result4);
    }

    @Test
    void testFalseIP() {
        boolean result1 = db.insert("Name1", "abc", 5, "ABC");
        Assertions.assertFalse(result1);
        Contact result2 = db.query("Name1");
        Assertions.assertNull(result2);

        boolean result3 = db.insert("Name2", "1555.155.155.155", 5, "");
        Assertions.assertFalse(result3);
        Contact result4 = db.query("Name2");
        Assertions.assertNull(result4);

        boolean result5 = db.insert("Name3", "155.155.555.155", 5, "");
        Assertions.assertFalse(result5);
        Contact result6 = db.query("Name3");
        Assertions.assertNull(result6);

        boolean result7 = db.insert("Name4", "299.299.299.299", 5, "");
        Assertions.assertFalse(result7);
        Contact result8 = db.query("Name4");
        Assertions.assertNull(result8);

        boolean result9 = db.insert("Name5", "1.1.256.1", 5, "");
        Assertions.assertFalse(result9);
        Contact result10 = db.query("Name5");
        Assertions.assertNull(result10);

        db.insert("Name6", "155.155.155.155", 5, "");
        boolean result11 = db.updateIP("Name6", "1.1.1.288");
        Assertions.assertFalse(result11);
        String result12 = db.query("Name6").getIpAddress();
        Assertions.assertEquals("155.155.155.155", result12);
    }

    @Test
    void testCorrectIP() {
        boolean result1 = db.insert("Name1", "5.5.5.5", 5, "");
        Assertions.assertTrue(result1);
        Contact result2 = db.query("Name1");
        Assertions.assertNotNull(result2);

        boolean result3 = db.insert("Name2", "255.255.255.255", 5, "");
        Assertions.assertTrue(result3);
        Contact result4 = db.query("Name2");
        Assertions.assertNotNull(result4);

        boolean result5 = db.insert("Name3", "0.0.0.0", 8, "");
        Assertions.assertTrue(result5);
        Contact result6 = db.query("Name3");
        Assertions.assertNotNull(result6);
    }

    @Test
    void testFalseName() {
        boolean result1 = db.insert("N??me", "5.5.5.5", 5, "");
        Assertions.assertFalse(result1);
        Contact result2 = db.query("N??me");
        Assertions.assertNull(result2);

        db.insert("Name1", "5.5.5.5", 5, "");
        boolean result3 = db.updateName("Name1", "N??me");
        Assertions.assertFalse(result3);
        Contact result4 = db.query("N??me");
        Contact result5 = db.query("Name1");
        Assertions.assertNull(result4);
        Assertions.assertNotNull(result5);

        boolean result6 = db.insert("Name??", "5.5.5.5", 5, "");
        Assertions.assertFalse(result6);
        Contact result7 = db.query("Name??");
        Assertions.assertNull(result7);

        boolean result8 = db.insert("Name\\", "5.5.5.5", 5, "");
        Assertions.assertFalse(result8);
        Contact result9 = db.query("Name\\");
        Assertions.assertNull(result9);

        boolean result10 = db.insert("Name 2", "5.5.5.5", 5, "");
        Assertions.assertFalse(result10);
        Contact result11 = db.query("Name 2");
        Assertions.assertNull(result11);
    }

    @Test
    void testCorrectName() {
        boolean result1 = db.insert("Name_1", "5.5.5.5", 5, "");
        Assertions.assertTrue(result1);
        Contact result2 = db.query("Name_1");
        Assertions.assertNotNull(result2);

        boolean result3 = db.insert("Name-2", "6.6.6.6", 6, "");
        Assertions.assertTrue(result3);
        Contact result4 = db.query("Name-2");
        Assertions.assertNotNull(result4);
    }

    @Test
    void testFalsePort() {
        boolean result1 = db.insert("Name1", "5.5.5.5", -20, "");
        Assertions.assertFalse(result1);
        Contact result2 = db.query("Name1");
        Assertions.assertNull(result2);

        boolean result3 = db.insert("Name2", "5.5.5.5", -1, "");
        Assertions.assertFalse(result3);
        Contact result4 = db.query("Name2");
        Assertions.assertNull(result4);

        boolean result5 = db.insert("Name3", "5.5.5.5", 65536, "");
        Assertions.assertFalse(result5);
        Contact result6 = db.query("Name3");
        Assertions.assertNull(result6);

        db.insert("Name4", "5.5.5.5", 4, "");
        boolean result7 = db.updatePort("Name4", 70000);
        Assertions.assertFalse(result7);
        int result8 = db.query("Name4").getPort();
        Assertions.assertEquals(4, result8);
    }

    @Test
    void testCorrectPort() {
        boolean result1 = db.insert("Name1", "5.5.5.5", 0, "");
        Assertions.assertTrue(result1);
        int result2 = db.query("Name1").getPort();
        Assertions.assertEquals(0, result2);

        boolean result3 = db.updatePort("Name1", 65535);
        Assertions.assertTrue(result3);
        int result4 = db.query("Name1").getPort();
        Assertions.assertEquals(65535, result4);
    }

    @Test
    void testFalseIpPortPair() {
        db.insert("Name1", "5.5.5.5", 0, "");
        boolean result1 = db.insert("Name2", "5.5.5.5", 0, "");
        Assertions.assertFalse(result1);
        Contact result2 = db.query("Name2");
        Assertions.assertNull(result2);

        db.insert("Name3", "5.5.5.5", 3, "");
        boolean result3 = db.updatePort("Name3", 0);
        Assertions.assertFalse(result3);
        int result4 = db.query("Name3").getPort();
        Assertions.assertEquals(3, result4);

        db.insert("Name4", "6.6.6.6", 0, "");
        boolean result5 = db.updateIP("Name4", "5.5.5.5");
        Assertions.assertFalse(result5);
        String result6 = db.query("Name4").getIpAddress();
        Assertions.assertEquals("6.6.6.6", result6);
    }

    @Test
    void testIPV6() {
    	//Test correct insert
        Assertions.assertTrue(db.insert("ipv6Test1", "2001:0db8:85a3:08d3:1319:8a2e:0370:7344", 2000, "sig1"));
        Assertions.assertTrue(db.insert("ipv6Test2", "2a02:0908:2614:b920:8d95:487e:8fec:ba99", 3000, "sig2"));
        
        
        //Test abridged insert
        Assertions.assertTrue(db.insert("ipv6Test3", "2001:db8:85a3:8d3:1319:8a2e:370:7344", 2200, "sig3"));
        Assertions.assertTrue(db.insert("ipv6Test4", "2a02:908:2614:b920:8d95:487e:8fec:ba99", 3200, "sig4"));
        
        
        //Test wrong insert
        Assertions.assertFalse(db.insert("ipv6Test5", "2001.0db8.85a3.08d3.1319.8a2e.0370.7344", 2600, "sig5"));
        Assertions.assertFalse(db.insert("ipv6Test6", "2a02:0908:2614:b920:8d95:487e.8fec.ba99", 3000, "sig6"));
        
        
        //Test wrong insert 2
        Assertions.assertFalse(db.insert("ipv6Test7", "2001:0db(:(%a3:08d3:131):8a2e:0?70:7344", 200, "sig7"));
        Assertions.assertFalse(db.insert("ipv6Test8", "155:155:155:155", 3000, "sig8"));
        
        //Test wrong insert 3
        Assertions.assertFalse(db.insert("ipv6Test9", "20501:0db8:85a3:08d3:1319:8a2e:0370:7344", 2600, "sig9"));
        
        //Test ::
        Assertions.assertTrue(db.insert("ipv6Test10", "2a02:0908:2614::8d95:487e:8fec:ba99", 3000, "sig10"));

        //Test delete and query
        Assertions.assertTrue(db.delete("ipv6Test3"));
        Assertions.assertNotNull(db.query("ipv6Test4"));
    }
}
