package tests;

import CommunicationList.Database;
import CommunicationList.DbObject;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * automated tests for interacting with the communication list db
 * @author Sarah Schumann
 */
class CommunicationListTests {
    // IMPORTANT: only run tests one by one. There will be problems if they interleave,
    // as they use the same database and always add and delete the test data.

    @AfterEach
    // only works if no problem with delete and queryAll
    void cleanUp() {
        ArrayList<DbObject> entries = Database.queryAll();
        for (DbObject e : entries) {
            Database.delete(e.getName());
        }
    }

    @Test
    void testInsertDelete() {
        boolean result1 = Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Assertions.assertTrue(result1);
        boolean result2 = Database.insert("Name2", "166.166.166.166", 7, "");
        Assertions.assertTrue(result2);

        boolean result3 = Database.delete("Name1");
        Assertions.assertTrue(result3);
        DbObject result4 = Database.query("Name2");
        Assertions.assertNotNull(result4);

        boolean result5 = Database.delete("Name3");
        Assertions.assertTrue(result5);
        DbObject result6 = Database.query("Name2");
        Assertions.assertNotNull(result6);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Database.delete(null),
                "Input was null");

        boolean result7 = Database.insert(null, "0.0.0.0", 5, "");
        Assertions.assertFalse(result7);
        int result8 = Database.queryAll().size();
        Assertions.assertEquals(1, result8);

        boolean result9 = Database.insert("Name4", null, 5, "");
        Assertions.assertFalse(result9);
        DbObject result10 = Database.query("Name4");
        Assertions.assertNull(result10);

        boolean result11 = Database.insert("Name5", "2.2.2.2", 6, null);
        Assertions.assertTrue(result11);
        DbObject result12 = Database.query("Name5");
        Assertions.assertNull(result12.getSignatureKey());
    }

    @Test
    void testUpdate() {
        boolean result1 = Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Assertions.assertTrue(result1);

        boolean result2 = Database.updateName("Name1", "Name2");
        Assertions.assertTrue(result2);
        DbObject result3 = Database.query("Name1");
        Assertions.assertNull(result3);
        DbObject result4 = Database.query("Name2");
        Assertions.assertNotNull(result4);

        boolean result5 = Database.updateIP("Name2", "144.144.144.144");
        Assertions.assertTrue(result5);
        String result6 = Database.query("Name2").getIpAddress();
        Assertions.assertEquals("144.144.144.144", result6);

        boolean result7 = Database.updatePort("Name2", 7);
        Assertions.assertTrue(result7);
        int result8 = Database.query("Name2").getPort();
        Assertions.assertEquals(7, result8);

        boolean result9 = Database.updateSignatureKey("Name2", "DEF");
        Assertions.assertTrue(result9);
        String result10 = Database.query("Name2").getSignatureKey();
        Assertions.assertEquals("DEF", result10);

        boolean result11 = Database.updateName("Name2", null);
        Assertions.assertFalse(result11);
        DbObject result12 = Database.query("Name2");
        Assertions.assertNotNull(result12);

        boolean result13 = Database.updateIP("Name2", null);
        Assertions.assertFalse(result13);
        String result14 = Database.query("Name2").getIpAddress();
        Assertions.assertEquals("144.144.144.144", result14);
    }

    @Test
    void testQuery() {
        Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Database.insert("Name2", "154.154.154.154", 7, "DEF");
        Database.insert("Name3", "133.133.133.133", 2, "GHI");

        DbObject testObject1 = Database.query("Name2");
        Assertions.assertEquals("Name2", testObject1.getName());
        Assertions.assertEquals("154.154.154.154", testObject1.getIpAddress());
        Assertions.assertEquals(7, testObject1.getPort());
        Assertions.assertEquals("DEF", testObject1.getSignatureKey());

        DbObject testObject2 = Database.query("Name1");
        Assertions.assertEquals("Name1", testObject2.getName());
        Assertions.assertEquals("155.155.155.155", testObject2.getIpAddress());
        Assertions.assertEquals(5, testObject2.getPort());
        Assertions.assertEquals("ABC", testObject2.getSignatureKey());
    }

    @Test
    void testQueryAll() {
        Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Database.insert("Name2", "154.154.154.154", 7, "DEF");
        Database.insert("Name3", "133.133.133.133", 2, "GHI");

        ArrayList<DbObject> testList = Database.queryAll();
        Assertions.assertEquals("Name1", testList.get(0).getName());
        Assertions.assertEquals(7, testList.get(1).getPort());
        Assertions.assertEquals("133.133.133.133", testList.get(2).getIpAddress());
        Assertions.assertEquals("DEF", testList.get(1).getSignatureKey());
    }

    @Test
    void testFalseIP() {
        boolean result1 = Database.insert("Name1", "abc", 5, "ABC");
        Assertions.assertFalse(result1);
        DbObject result2 = Database.query("Name1");
        Assertions.assertNull(result2);

        boolean result3 = Database.insert("Name2", "1555.155.155.155", 5, "");
        Assertions.assertFalse(result3);
        DbObject result4 = Database.query("Name2");
        Assertions.assertNull(result4);

        boolean result5 = Database.insert("Name3", "155.155.555.155", 5, "");
        Assertions.assertFalse(result5);
        DbObject result6 = Database.query("Name3");
        Assertions.assertNull(result6);

        boolean result7 = Database.insert("Name4", "299.299.299.299", 5, "");
        Assertions.assertFalse(result7);
        DbObject result8 = Database.query("Name4");
        Assertions.assertNull(result8);

        boolean result9 = Database.insert("Name5", "1.1.256.1", 5, "");
        Assertions.assertFalse(result9);
        DbObject result10 = Database.query("Name5");
        Assertions.assertNull(result10);

        Database.insert("Name6", "155.155.155.155", 5, "");
        boolean result11 = Database.updateIP("Name6", "1.1.1.288");
        Assertions.assertFalse(result11);
        String result12 = Database.query("Name6").getIpAddress();
        Assertions.assertEquals("155.155.155.155", result12);
    }

    @Test
    void testCorrectIP() {
        boolean result1 = Database.insert("Name1", "5.5.5.5", 5, "");
        Assertions.assertTrue(result1);
        DbObject result2 = Database.query("Name1");
        Assertions.assertNotNull(result2);

        boolean result3 = Database.insert("Name2", "255.255.255.255", 5, "");
        Assertions.assertTrue(result3);
        DbObject result4 = Database.query("Name2");
        Assertions.assertNotNull(result4);

        boolean result5 = Database.insert("Name3", "0.0.0.0", 8, "");
        Assertions.assertTrue(result5);
        DbObject result6 = Database.query("Name3");
        Assertions.assertNotNull(result6);
    }

    @Test
    void testFalseName() {
        boolean result1 = Database.insert("Näme", "5.5.5.5", 5, "");
        Assertions.assertFalse(result1);
        DbObject result2 = Database.query("Näme");
        Assertions.assertNull(result2);

        Database.insert("Name1", "5.5.5.5", 5, "");
        boolean result3 = Database.updateName("Name1", "Nöme");
        Assertions.assertFalse(result3);
        DbObject result4 = Database.query("Nöme");
        DbObject result5 = Database.query("Name1");
        Assertions.assertNull(result4);
        Assertions.assertNotNull(result5);

        boolean result6 = Database.insert("Nameß", "5.5.5.5", 5, "");
        Assertions.assertFalse(result6);
        DbObject result7 = Database.query("Nameß");
        Assertions.assertNull(result7);

        boolean result8 = Database.insert("Name\\", "5.5.5.5", 5, "");
        Assertions.assertFalse(result8);
        DbObject result9 = Database.query("Name\\");
        Assertions.assertNull(result9);

        boolean result10 = Database.insert("Name 2", "5.5.5.5", 5, "");
        Assertions.assertFalse(result10);
        DbObject result11 = Database.query("Name 2");
        Assertions.assertNull(result11);
    }

    @Test
    void testCorrectName() {
        boolean result1 = Database.insert("Name_1", "5.5.5.5", 5, "");
        Assertions.assertTrue(result1);
        DbObject result2 = Database.query("Name_1");
        Assertions.assertNotNull(result2);

        boolean result3 = Database.insert("Name-2", "6.6.6.6", 6, "");
        Assertions.assertTrue(result3);
        DbObject result4 = Database.query("Name-2");
        Assertions.assertNotNull(result4);
    }

    @Test
    void testFalsePort() {
        boolean result1 = Database.insert("Name1", "5.5.5.5", -20, "");
        Assertions.assertFalse(result1);
        DbObject result2 = Database.query("Name1");
        Assertions.assertNull(result2);

        boolean result3 = Database.insert("Name2", "5.5.5.5", -1, "");
        Assertions.assertFalse(result3);
        DbObject result4 = Database.query("Name2");
        Assertions.assertNull(result4);

        boolean result5 = Database.insert("Name3", "5.5.5.5", 65536, "");
        Assertions.assertFalse(result5);
        DbObject result6 = Database.query("Name3");
        Assertions.assertNull(result6);

        Database.insert("Name4", "5.5.5.5", 4, "");
        boolean result7 = Database.updatePort("Name4", 70000);
        Assertions.assertFalse(result7);
        int result8 = Database.query("Name4").getPort();
        Assertions.assertEquals(4, result8);
    }

    @Test
    void testCorrectPort() {
        boolean result1 = Database.insert("Name1", "5.5.5.5", 0, "");
        Assertions.assertTrue(result1);
        int result2 = Database.query("Name1").getPort();
        Assertions.assertEquals(0, result2);

        boolean result3 = Database.updatePort("Name1", 65535);
        Assertions.assertTrue(result3);
        int result4 = Database.query("Name1").getPort();
        Assertions.assertEquals(65535, result4);
    }

    @Test
    void testFalseIpPortPair() {
        Database.insert("Name1", "5.5.5.5", 0, "");
        boolean result1 = Database.insert("Name2", "5.5.5.5", 0, "");
        Assertions.assertFalse(result1);
        DbObject result2 = Database.query("Name2");
        Assertions.assertNull(result2);

        Database.insert("Name3", "5.5.5.5", 3, "");
        boolean result3 = Database.updatePort("Name3", 0);
        Assertions.assertFalse(result3);
        int result4 = Database.query("Name3").getPort();
        Assertions.assertEquals(3, result4);

        Database.insert("Name4", "6.6.6.6", 0, "");
        boolean result5 = Database.updateIP("Name4", "5.5.5.5");
        Assertions.assertFalse(result5);
        String result6 = Database.query("Name4").getIpAddress();
        Assertions.assertEquals("6.6.6.6", result6);
    }

}
