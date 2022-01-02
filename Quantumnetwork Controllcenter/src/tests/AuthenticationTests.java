package tests;

import CommunicationList.Database;
import MessengerSystem.Authentication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuthenticationTests {
    // IMPORTANT: only run tests one by one. There might be problems if they interleave,
    // as they use the same database and always add and delete the test data

    @Test
    public void testSign () {
        String result = Authentication.sign("Hello");
        Assertions.assertNotNull(result);
    }

    @Test
    // only testable, if testSign worked
    public void testVerifyTrue () {
        Database.insert("self", "127.0.0.0", 2303, Authentication.publicKeyString);
        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hello", signature, "self");
        Assertions.assertTrue(result);
        Database.delete("self");
    }

    @Test
    // only testable, if testSign worked
    public void testVerifyFalse () {
        Database.insert("self", "127.0.0.0", 2303, Authentication.publicKeyString);
        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hallo", signature, "self");
        Assertions.assertFalse(result);
        Database.delete("self");
    }

}
