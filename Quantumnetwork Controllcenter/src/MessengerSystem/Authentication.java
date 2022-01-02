package MessengerSystem;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Authentication {

    // temporary plain text keys for development and tests
    private static final String privateKeyString =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC32llHgkxZPBdj" +
            "p9WsNz1FsYuY0y1/sjacFDTlOUiw4BxVwc7mp8ZZl4+MynyZ318ioaXshqbRPdKV" +
            "TpbkrweM7bPghF7ExuqQMqZXuxSN2pqPmV1gah82BiAKDTXS9MHGo571hklI6BgS" +
            "De3Hp/oCEpiTfB1FtAKXQ8sqaIR/KwRv8c7Pbnzn+oo5WKMHxPsprpXrgG4J5dij" +
            "KhUEFRYzkTWjAUfCqIHqarxcn+sHyl/hgGNjP0feFheq6edGYDSD8hggy4U5jDC3" +
            "YjW6bN2FucXE3kFgU1MBFppXx1qMuLGBaRq10U/LdORWh7l2lohi6cqofKqNEDJJ" +
            "zFTGPx4DAgMBAAECggEADqKqEsAzlbLr5bn5j3ECRqcAhtTgvVaRdFFDeK/bsoDU" +
            "oJsB/miCqKUg/0MrMhCPATG/BJofc26pcctExcWNI1HUzN2csatoBryRc1BoLXam" +
            "cSHzRfaMr39DU6mDjFyBhG+H+uGKuewH5oAHWqprg3i5fpwVtHPE9Qenh6O0UG8Y" +
            "ehDP6WrbrJqbb6OVnN+6ioOSDeH5K6okv4ek/1TYRCQdZ6ND14rJndTqcNSVfHtY" +
            "k/gwYyUlzC6BdAJc2ySotqWxLmqIoj9N1Y4RoeNuS2hiDqYjz5afIwsuAnxM6sWG" +
            "vYvDwsuO656rkzbvHmO6O8sZDGp7AwA85ttsoiD2EQKBgQDxQ914H+9SLN+Y8lKx" +
            "o3uip6y6Z0fYWSNOMUgp7wUVMckyymCiv604b0tcBV7cwNFapMAaYgYFqDaLm/ij" +
            "eTQU/gzPWhAp8NhTrlPer6X/vbfOEitx85er3crSwzS1BuUMOxAwC7IgN/9ADlEq" +
            "u3pMqqFWbh5mDa+uUFLm2quYaQKBgQDDFNrg2zrOs4jbrKWbAWSbxXk3vxqmMiSc" +
            "tw1q9fWsQ16hWwYMqF3pIB65nLyM4CEoNYAI2nsdJWt27s92RR/DsoeeDiSouk2x" +
            "6NJRWh7XKioJBK9zbmFT7lVlEEOTEZs9GEtoQdToRJ+i6eyLCOTzEfeklg4EPajZ" +
            "mPxsUqTViwKBgQCB3RG8sxPSm6zPWsAANgs6hh6HR0h4v6ItWIGLcMi/m8i8ugpC" +
            "EAJhOibKJWnmUTNfIwb0LIcpTF3vz8iJ1ZefJRAoHEZPDQCkKlWiq9EiUzA4j7cq" +
            "4v9k10FxKPmZ51gquTABbIo4pWuTQyGVasxtr/qf1y4tqHhDz6CP+mwa4QKBgGRu" +
            "vDC/LQt1iqPtwmSG1xELHVkB3epqLkcwRowmjJBfLrzLa9XgaFi43JiahMfSK7/T" +
            "q1LjiDZ9KVJVqCEvfSb3JrLMfTsQjydkwcJ4LyB+6J0z+E/pJ9pZ/UTGlsOEcPOH" +
            "KmtGWSIrEjSBUfJSZext95yYVCcCx4cfJ/Vspsf5AoGAGi2q0hbPvnjPlCfH90+C" +
            "HgtinT2wvAMGuyzkAXSZa8Z40KjmX2xyj6PdU9fjwVWkBaGkotMDGZTKbMGVMn3v" +
            "7GsDvWQxXiWgc7Q7Z3UT0fSLAS8rUqVBt3S2jhy8Fk/v3LrG2ACyHkysZ/Qu89Wq" +
            "6XSXtbgS25DXTFOCCU6UJPk=";
    private static final String publicKeyString =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt9pZR4JMWTwXY6fVrDc9" +
            "RbGLmNMtf7I2nBQ05TlIsOAcVcHO5qfGWZePjMp8md9fIqGl7Iam0T3SlU6W5K8H" +
            "jO2z4IRexMbqkDKmV7sUjdqaj5ldYGofNgYgCg010vTBxqOe9YZJSOgYEg3tx6f6" +
            "AhKYk3wdRbQCl0PLKmiEfysEb/HOz2585/qKOVijB8T7Ka6V64BuCeXYoyoVBBUW" +
            "M5E1owFHwqiB6mq8XJ/rB8pf4YBjYz9H3hYXqunnRmA0g/IYIMuFOYwwt2I1umzd" +
            "hbnFxN5BYFNTARaaV8dajLixgWkatdFPy3TkVoe5dpaIYunKqHyqjRAyScxUxj8e" +
            "AwIDAQAB";
    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    /**
     *
     * @param message the message to be signed with the private key
     * @return the signed message as a String; null if Error
     */
    public static String sign (String message) {
        try {
            // check if key already as key object, otherwise, use method to create from String
            if (publicKey == null) {
                getKeysFromString();
            }
            Signature signature = Signature.getInstance("SHA256withRSA");
            // for now: static private key for dev and tests
            signature.initSign(privateKey);
            // convert message from String to byte array
            byte[] msg = message.getBytes();
            signature.update(msg);
            byte[] sig = signature.sign();
            // convert signature into 'readable' string
            return new String(Base64.getEncoder().encode(sig));
        } catch (Exception e) {
            System.err.println("Error while signing: " + e.getMessage());
            return null;
        }
    }

    /**
     *
     * @param message the received signed message (only text without the signature)
     * @param receivedSignature the received signature as String
     * @param sender the sender of the message, needed to look up the public key in the communication list
     * @return true if the signature matches the message, false otherwise or if Error
     */
    public static boolean verify (String message, String receivedSignature, String sender) {
        try {
            // check if key already as key object, otherwise, use method to create from String
            if (privateKey == null) {
                getKeysFromString();
            }
            Signature signature = Signature.getInstance("SHA256withRSA");
            // for now: static public key for dev and tests
            signature.initVerify(publicKey);
            // convert message from String to byte array
            byte[] msg = message.getBytes();
            signature.update(msg);
            // convert receivedSignature to byte array
            byte[] recSig = Base64.getDecoder().decode(receivedSignature.getBytes());
            // return result of verification
            return signature.verify(recSig);
        } catch (Exception e) {
            System.err.println("Error while verifying: " + e.getMessage());
            return false;
        }
    }

    // used to generate a PrivateKey Object and a PublicKey from a String Object
    // not sure if needed later, depends on how it is saved in the db
    private static void getKeysFromString () throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString));
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString));
        privateKey = kf.generatePrivate(privateKeySpec);
        publicKey = kf.generatePublic(publicKeySpec);
    }
}
