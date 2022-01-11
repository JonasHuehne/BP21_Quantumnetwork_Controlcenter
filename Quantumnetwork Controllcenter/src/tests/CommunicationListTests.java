package tests;

import CommunicationList.Database;
import CommunicationList.DbObject;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * automated tests for interacting with the communication list db
 * @author Sarah Schumann
 */
class CommunicationListTests {
    // IMPORTANT: only run tests one by one. There will be problems if they interleave,
    // as they use the same database and always add and delete the test data.
    // Additionally, if they fail, you might need to make everything in the test above the first delete line a comment,
    // run the test again, delete do uncomment the rest, and then run the test again.

    @Test
    void testInsertDelete() {
        boolean result1 = Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Assertions.assertTrue(result1);
        boolean result2 = Database.delete("Name1");
        Assertions.assertTrue(result2);
    }

    @Test
    void testUpdate() {
        Database.insert("Name1", "155.155.155.155", 5, "ABC");
        boolean result1 = Database.updateName("Name1", "Name2");
        Assertions.assertTrue(result1);
        boolean result2 = Database.updateIP("Name2", "144.144.144.144");
        Assertions.assertTrue(result2);
        boolean result3 = Database.updatePort("Name2", 7);
        Assertions.assertTrue(result3);
        boolean result4 = Database.updateSignatureKey("name2", "DEF");
        Assertions.assertTrue(result4);
        Database.delete("Name2");
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
        Database.delete("Name1");
        Database.delete("Name2");
        Database.delete("Name3");
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
        Database.delete("Name1");
        Database.delete("Name2");
        Database.delete("Name3");
    }

}
