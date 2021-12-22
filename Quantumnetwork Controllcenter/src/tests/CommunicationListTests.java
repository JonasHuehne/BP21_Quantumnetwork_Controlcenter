package tests;

import CommunicationList.Database;
import CommunicationList.DbObject;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class CommunicationListTests {

    @Test
    public void testInsertDelete() {
        Boolean result1 = Database.insert("Name1", "155.155.155.155", 5);
        assertTrue(result1);
        Boolean result2 = Database.delete("Name1");
        assertTrue(result2);
    }

    @Test
    public void testUpdate() {
        Database.insert("Name1", "155.155.155.155", 5);
        Boolean result1 = Database.updateName("Name1", "Name2");
        assertTrue(result1);
        Boolean result2 = Database.updateIP("Name2", "144.144.144.144");
        assertTrue(result2);
        Boolean result3 = Database.updatePort("Name2", 7);
        assertTrue(result3);
        Database.delete("Name2");
    }

    @Test
    public void testQuery() {
        Database.insert("Name1", "155.155.155.155", 5);
        Database.insert("Name2", "154.154.154.154", 7);
        Database.insert("Name3", "133.133.133.133", 2);
        DbObject testObject1 = Database.query("Name2");
        assertEquals(testObject1.getName(), "Name2");
        assertEquals(testObject1.getIpAddr(), "154.154.154.154");
        assertEquals(testObject1.getPort(), 7);
        DbObject testObject2 = Database.query("Name1");
        assertEquals(testObject2.getName(), "Name1");
        assertEquals(testObject2.getIpAddr(), "155.155.155.155");
        assertEquals(testObject2.getPort(), 5);
        Database.delete("Name1");
        Database.delete("Name2");
        Database.delete("Name3");
    }

    @Test
    public void testQueryAll() {
        Database.insert("Name1", "155.155.155.155", 5);
        Database.insert("Name2", "154.154.154.154", 7);
        Database.insert("Name3", "133.133.133.133", 2);
        ArrayList<DbObject> testList = Database.queryAll();
        assertEquals(testList.get(0).getName(), "Name1");
        assertEquals(testList.get(1).getPort(), 7);
        assertEquals(testList.get(2).getIpAddr(), "133.133.133.133");
        Database.delete("Name1");
        Database.delete("Name2");
        Database.delete("Name3");
    }

}
