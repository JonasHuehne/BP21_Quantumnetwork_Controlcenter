package tests;

import CommunicationList.Database;
import CommunicationList.DbObject;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommunicationListTests {
    // IMPORTANT: only run tests one by one. There will be problems if they interleave,
    // as they use the same database and always add and delete the test data

    @Test
    public void testInsertDelete() {
        boolean result1 = Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Assertions.assertTrue(result1);
        boolean result2 = Database.delete("Name1");
        Assertions.assertTrue(result2);
    }

    @Test
    public void testUpdate() {
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
    public void testQuery() {
        Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Database.insert("Name2", "154.154.154.154", 7, "DEF");
        Database.insert("Name3", "133.133.133.133", 2, "GHI");
        DbObject testObject1 = Database.query("Name2");
        Assertions.assertEquals(testObject1.getName(), "Name2");
        Assertions.assertEquals(testObject1.getIpAddress(), "154.154.154.154");
        Assertions.assertEquals(testObject1.getPort(), 7);
        Assertions.assertEquals(testObject1.getSignatureKey(), "DEF");
        DbObject testObject2 = Database.query("Name1");
        Assertions.assertEquals(testObject2.getName(), "Name1");
        Assertions.assertEquals(testObject2.getIpAddress(), "155.155.155.155");
        Assertions.assertEquals(testObject2.getPort(), 5);
        Assertions.assertEquals(testObject2.getSignatureKey(), "ABC");
        Database.delete("Name1");
        Database.delete("Name2");
        Database.delete("Name3");
    }

    @Test
    public void testQueryAll() {
        Database.insert("Name1", "155.155.155.155", 5, "ABC");
        Database.insert("Name2", "154.154.154.154", 7, "DEF");
        Database.insert("Name3", "133.133.133.133", 2, "GHI");
        ArrayList<DbObject> testList = Database.queryAll();
        Assertions.assertEquals(testList.get(0).getName(), "Name1");
        Assertions.assertEquals(testList.get(1).getPort(), 7);
        Assertions.assertEquals(testList.get(2).getIpAddress(), "133.133.133.133");
        Assertions.assertEquals(testList.get(1).getSignatureKey(), "DEF");
        Database.delete("Name1");
        Database.delete("Name2");
        Database.delete("Name3");
    }

}
