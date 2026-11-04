public final class ActivityEntry {
    private final String time;
    private final String role;
    private final String username;
    private final String event;
    private final String details;

    // Stores one real request, file read, or completed save.
    public ActivityEntry(String time, String role, String username, String event, String details) {
        this.time = time;
        this.role = role;
        this.username = username;
        this.event = event;
        this.details = details;
    }

    // Returns when the action happened, in UTC.
    public String getTime() {
        return time;
    }

    // Returns the role that performed the action.
    public String getRole() {
        return role;
    }

    // Returns the username that performed the action.
    public String getUsername() {
        return username;
    }

    // Returns whether this was a request, login, read, or save.
    public String getEvent() {
        return event;
    }

    // Returns the action details without credentials or file contents.
    public String getDetails() {
        return details;
    }
}
