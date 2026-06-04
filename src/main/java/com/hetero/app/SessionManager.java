package com.hetero.app;

import com.hetero.model.User;

/**
 * SessionManager remembers who is currently logged in.
 *
 * It holds a single User object in a static variable.
 * Because it is static, any class in the app can check
 * who is logged in without needing a reference to this object.
 *
 * The constructor is private so nobody can create an instance —
 * it is meant to be used only through its static methods.
 */
public final class SessionManager {

    // The user who is currently logged in. Null means nobody is signed in.
    private static User currentUser;

    // Prevent instantiation — this class is only used via static methods
    private SessionManager() { }

    /**
     * Stores the logged-in user after a successful sign-in.
     *
     * @param authenticatedUser the user who just logged in
     */
    public static void login(User authenticatedUser) {
        currentUser = authenticatedUser;
    }

    /**
     * Clears the session when the user signs out.
     * After this, getUser() returns null.
     */
    public static void logout() {
        currentUser = null;
    }

    /**
     * Returns the currently logged-in user, or null if nobody is signed in.
     *
     * @return the current User, or null
     */
    public static User getUser() {
        return currentUser;
    }

    /**
     * Quick check — returns true if someone is signed in, false otherwise.
     *
     * @return true if a user is logged in
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
