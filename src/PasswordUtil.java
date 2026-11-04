import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
    private static final int ITERATIONS = 120000;

    // Keeps this helper from being used as a normal object.
    private PasswordUtil() {
    }

    // Saves a salted password hash so the text files do not contain passwords.
    public static String hash(String password) {
        if (password == null || password.length() < 6 || password.length() > 128) {
            throw new IllegalArgumentException("Passwords must have 6 to 128 characters.");
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt));
    }

    // Checks a password against the stored hash.
    public static boolean matches(String password, String savedHash) {
        if (password == null || password.length() > 128 || !isValidHash(savedHash)) {
            return false;
        }
        String[] parts = savedHash.split("\\$");
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        return MessageDigest.isEqual(expected, derive(password, salt));
    }

    // Rejects broken credential records when loading user files.
    public static boolean isValidHash(String hash) {
        if (hash == null) {
            return false;
        }
        String[] parts = hash.split("\\$", -1);
        try {
            return parts.length == 4 && parts[0].equals("pbkdf2")
                    && parts[1].equals(String.valueOf(ITERATIONS))
                    && Base64.getDecoder().decode(parts[2]).length == 16
                    && Base64.getDecoder().decode(parts[3]).length == 32;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // Runs Java's password hashing algorithm with the saved salt.
    private static byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing is unavailable.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
