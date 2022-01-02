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
    // only testable, if signing works
    public void testVerifyTrue () {
        Database.insert("self", "127.0.0.0", 2303, Authentication.publicKeyString);
        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hello", signature, "self");
        Assertions.assertTrue(result);
        Database.delete("self");
    }

    @Test
    // only testable, if signing works
    public void testVerifyFalse () {
        Database.insert("self", "127.0.0.0", 2303, Authentication.publicKeyString);
        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hallo", signature, "self");
        Assertions.assertFalse(result);
        Database.delete("self");
    }

    @Test
    // only testable, if signing works
    public void testVerifyFalse2 () {
        String otherPublicKeyString =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5r12pr0ZBtvFj133y9Yz" +
                "UCmivnUycRU3T/TBFTiIV7Li7NN11RQ+RdOUzuNOB7A5tQIzkzNPJSOHC2ogxXnE" +
                "yG6ClQS/YQ6hGQ4BH/FMz8h3HWsA/d9rhL1csmz8xJeqCoK0djEph1qGkso/AyoK" +
                "LohV1zXgRM3EMV09ZgJAEktw6xxuzDtoLvDe7LMtYb/ahtdpYQMGSaHmUlEsC5Wk" +
                "hbZkxGgs0LZD1Tjk9zGQ2bHbfU1wR7XhMku0riIxk32pNNJ+E2VSGIK5UJIyjbHM" +
                "iX5wyzy+frpgvA4YyonXJJRs4dp6Jngy9BwYnCJjeHgcFdVtIqjYTEIcy3w4FsEX" +
                "1QIDAQAB";
        Database.insert("self", "127.0.0.0", 2303, Authentication.publicKeyString);
        Database.insert("other", "128.0.0.0", 2505, otherPublicKeyString);
        String signature = Authentication.sign("Hello");
        boolean result = Authentication.verify("Hello", signature, "other");
        Assertions.assertFalse(result);
        Database.delete("self");
        Database.delete("other");
    }

}
