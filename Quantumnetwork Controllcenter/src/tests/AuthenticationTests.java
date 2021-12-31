package tests;

import MessengerSystem.Authentication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuthenticationTests {

    @Test
    public void testSign () {
        byte[] result = Authentication.sign("Hello");
        Assertions.assertNotNull(result);
    }

    @Test
    // only testable, if testSign worked
    public void testVerifyTrue () {
        byte[] signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hello", signature, "self");
        Assertions.assertTrue(result);
    }

    @Test
    // only testable, if testSign worked
    public void testVerifyFalse () {
        byte[] signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hallo", signature, "self");
        Assertions.assertFalse(result);
    }

}
