package redmineconnector.test;

import redmineconnector.util.SecurityUtils;
import static redmineconnector.test.SimpleTestRunner.*;

public class SecurityUtilsTest {

    public static void runTests(SimpleTestRunner runner) {
        System.out.println("\n=== SecurityUtils Tests ===");

        runner.run("testEncryptionDecryption", () -> {
            String original = "my-secret-key-123";
            String encrypted = SecurityUtils.encrypt(original);
            assertNotNull(encrypted, "Encrypted string should not be null");
            assertNotEquals(original, encrypted, "Encrypted string should be different from original");

            String decrypted = SecurityUtils.decrypt(encrypted);
            assertEquals(original, decrypted, "Decrypted string should match original");
        });

        runner.run("testEmptyAndNull", () -> {
            assertNull(SecurityUtils.encrypt(null), "Encrypting null should return null");
            assertEquals("", SecurityUtils.encrypt(""), "Encrypting empty should return empty");
            assertNull(SecurityUtils.decrypt(null), "Decrypting null should return null");
            assertEquals("", SecurityUtils.decrypt(""), "Decrypting empty should return empty");
        });

        runner.run("testInvalidDecryptionReturnsOriginal", () -> {
            String notEncrypted = "just-plain-text";
            String result = SecurityUtils.decrypt(notEncrypted);
            assertEquals(notEncrypted, result, "Invalid decryption should return original string");
        });

        runner.run("testSymmetry", () -> {
            String secret = "another-secret";
            String encrypted1 = SecurityUtils.encrypt(secret);
            String encrypted2 = SecurityUtils.encrypt(secret);
            assertEquals(encrypted1, encrypted2, "Encryption should be deterministic");
        });
    }
}
