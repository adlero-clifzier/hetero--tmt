package com.hetero.app;

import com.hetero.model.User;

/**
 * Application-wide session registry that holds the currently authenticated user.
 *
 * <p>The Singleton-style static design is appropriate here because there is
 * exactly one active session per JVM process, and controllers across the
 * application need to access the current user without constructor injection.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Instance variables and objects:</b>
 *       {@code currentUser} is a static object reference of the custom {@link User} type.</li>
 *   <li><b>Custom-built classes:</b>
 *       Uses the custom {@link User} domain entity defined in {@code com.hetero.model}.</li>
 *   <li><b>Proper access control:</b>
 *       The constructor is {@code private} to prevent instantiation.
 *       The {@code currentUser} field is {@code private static} — accessible only
 *       through the controlled public API ({@link #login}, {@link #logout},
 *       {@link #getUser}, {@link #isLoggedIn}).</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code currentUser}, {@code authenticatedUser} — intent is clear.</li>
 * </ul>
 */
public final class SessionManager {

    // ── Session state ─────────────────────────────────────────────────────────

    /**
     * The currently authenticated user, or {@code null} when no session is active.
     * Set by {@link #login} and cleared by {@link #logout}.
     */
    private static User currentUser;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Private constructor prevents instantiation.
     * All members are static — this class is a pure utility registry.
     */
    private SessionManager() { }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Starts a session for the provided authenticated user.
     *
     * <p>Called by {@link com.hetero.controller.LoginController} immediately after
     * {@link com.hetero.db.DatabaseManager#authenticate} confirms valid credentials.
     *
     * @param authenticatedUser the user who has successfully logged in; must not be null
     */
    public static void login(User authenticatedUser) {
        currentUser = authenticatedUser;
    }

    /**
     * Ends the current session by clearing the stored user reference.
     *
     * <p>Called by {@link com.hetero.controller.SettingsController#onLogout}
     * before returning to the login screen.
     */
    public static void logout() {
        currentUser = null;
    }

    /**
     * Returns the currently authenticated user.
     *
     * @return the {@link User} who is signed in, or {@code null} if no session is active
     */
    public static User getUser() {
        return currentUser;
    }

    /**
     * Convenience check for whether a user session is currently active.
     *
     * @return {@code true} if a user is signed in; {@code false} otherwise
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
