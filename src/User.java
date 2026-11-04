import java.util.Locale;

public abstract class User {
    private final String username;
    private String passwordHash;
    private String email;

    // Stores the information shared by all three user types.
    protected User(String username, String passwordHash, String email) {
        this.username = normalizeUsername(username);
        if (!PasswordUtil.isValidHash(passwordHash)) {
            throw new IllegalArgumentException("The saved password is not valid.");
        }
        this.passwordHash = passwordHash;
        setEmail(email);
    }

    // Checks the login password without exposing the stored hash.
    public boolean login(String password) {
        return PasswordUtil.matches(password, passwordHash);
    }

    // Returns the username used by orders and saved files.
    public String getUsername() {
        return username;
    }

    // Returns the user's email address.
    public String getEmail() {
        return email;
    }

    // Gives the file manager the hash to save.
    String getPasswordHash() {
        return passwordHash;
    }

    // Changes the stored hash after the new password is checked.
    void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Checks an email before storing it.
    final void setEmail(String email) {
        if (email == null || !email.trim().matches("[^\\s@|]+@[^\\s@|]+\\.[^\\s@|]+")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        this.email = email.trim();
    }

    // Makes usernames consistent between logins and saved records.
    public static String normalizeUsername(String username) {
        if (username == null || !username.trim().matches("[A-Za-z0-9_-]{3,30}")) {
            throw new IllegalArgumentException("Use 3 to 30 letters, numbers, underscores, or hyphens for usernames.");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    // Lets each user type choose its own dashboard name.
    public abstract String getRole();

    // Lets each user type show different terminal options.
    public abstract void viewOptions();
}
