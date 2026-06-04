package com.hetero.model;

/**
 * User stores the information of whoever is currently logged in.
 *
 * Once a user logs in, we create one User object and keep it in
 * SessionManager for the rest of the session. Because we never
 * want this data to change mid-session, all fields are final
 * (they can only be set once in the constructor).
 *
 * This is an example of an immutable class — it has no setters.
 */
public class User {

    // The database row id for this account
    private final int id;

    // The login name chosen at registration (must be unique)
    private final String username;

    // The friendly name shown in the sidebar of the app
    private final String displayName;

    /**
     * Creates a User object with all three values.
     * Only DatabaseManager should call this directly.
     *
     * @param id          the database primary key
     * @param username    the unique login name
     * @param displayName the name shown in the UI
     */
    public User(int id, String username, String displayName) {
        this.id          = id;
        this.username    = username;
        this.displayName = displayName;
    }

    /** Returns the database id of this user. */
    public int getId() { return id; }

    /** Returns the login username. */
    public String getUsername() { return username; }

    /** Returns the display name shown in the app. */
    public String getDisplayName() { return displayName; }

    /**
     * Returns a short text summary of this user.
     * Helpful for debugging in the console.
     */
    @Override
    public String toString() {
        return "User{id=%d, username='%s', displayName='%s'}"
                .formatted(id, username, displayName);
    }
}
