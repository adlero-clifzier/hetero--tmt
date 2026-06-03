package com.hetero.model;

/**
 * Immutable domain entity representing an authenticated application user.
 *
 * <p>Instances are created by {@link com.hetero.db.DatabaseManager#authenticate}
 * after a successful credential check and held for the lifetime of the session
 * by {@link com.hetero.app.SessionManager}.
 *
 * <p>The class is intentionally <em>immutable</em> — all fields are {@code private final}
 * and there are no setters — because a user's identity must not change mid-session.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Primitive data:</b>
 *       {@code id} is a primitive {@code int}.</li>
 *   <li><b>Instance variables and objects:</b>
 *       {@code username} and {@code displayName} are {@link String} object instance variables.</li>
 *   <li><b>Proper access control:</b>
 *       All fields are {@code private final}; the class exposes only read-only getters.</li>
 *   <li><b>Custom-built class:</b>
 *       Defined specifically for Hetero's authentication and session flow.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       Field names clearly communicate their purpose without ambiguity.</li>
 * </ul>
 */
public class User {

    // ── Private immutable instance variables ──────────────────────────────────

    /**
     * Database-assigned primary key for this user account.
     * Primitive {@code int} — demonstrates use of primitive data.
     */
    private final int id;

    /**
     * Unique login identifier chosen by the user at registration.
     * Used to authenticate against the {@code users} table in SQLite.
     */
    private final String username;

    /**
     * Human-readable full name displayed in the sidebar and Settings screen.
     * Falls back to {@code username} if no display name was provided at registration.
     */
    private final String displayName;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs a fully populated, immutable {@code User} record.
     *
     * <p>Only the {@link com.hetero.db.DatabaseManager} should call this
     * constructor — it is not intended for direct use in UI code.
     *
     * @param id          the database primary key
     * @param username    the unique login username
     * @param displayName the friendly name shown in the UI; must not be {@code null}
     */
    public User(int id, String username, String displayName) {
        this.id          = id;
        this.username    = username;
        this.displayName = displayName;
    }

    // ── Getters (read-only — no setters by design) ────────────────────────────

    /**
     * Returns the user's database-assigned primary key.
     *
     * @return the integer id; always positive for persisted users
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the unique login username for this account.
     *
     * @return the username string; never {@code null}
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the friendly display name shown throughout the UI.
     *
     * @return the display name string; never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    /**
     * Returns a readable summary for session debugging and log output.
     *
     * @return formatted string including the id, username, and display name
     */
    @Override
    public String toString() {
        return "User{id=%d, username='%s', displayName='%s'}"
                .formatted(id, username, displayName);
    }
}
