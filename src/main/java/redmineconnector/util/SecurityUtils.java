package redmineconnector.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility for symmetric encryption using AES.
 * The key is derived from machine-specific identifiers to prevent
 * simple configuration porting between different systems.
 */
public class SecurityUtils {

    private static final String ALGORITHM = "AES";
    private static SecretKeySpec secretKey;

    static {
        try {
            prepareKey();
        } catch (Exception e) {
            redmineconnector.util.LoggerUtil.logError("SecurityUtils",
                    "Failed to prepare encryption key", e);
        }
    }

    private static void prepareKey() throws Exception {
        // Derive a key from machine-specific info
        String machineInfo = System.getenv("COMPUTERNAME") +
                System.getenv("USERDOMAIN") +
                System.getProperty("user.name") +
                "RedmineConnectorSalt";

        byte[] key = machineInfo.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit
        secretKey = new SecretKeySpec(key, ALGORITHM);
    }

    /**
     * Encrypts a plain text string using AES.
     * 
     * @param strToEncrypt String to encrypt
     * @return Base64 encoded encrypted string
     */
    public static String encrypt(String strToEncrypt) {
        if (strToEncrypt == null || strToEncrypt.isEmpty())
            return strToEncrypt;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.err.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    /**
     * Decrypts a Base64 encoded encrypted string.
     * 
     * @param strToDecrypt Base64 encrypted string
     * @return Decrypted plain text
     */
    public static String decrypt(String strToDecrypt) {
        if (strToDecrypt == null || strToDecrypt.isEmpty())
            return strToDecrypt;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            // If it's not base64 or not encrypted correctly, return original
            // This allows migration from plain text keys
            return strToDecrypt;
        }
    }
}
