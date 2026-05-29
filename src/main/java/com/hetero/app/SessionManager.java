package com.hetero.app;

import com.hetero.model.User;

/** Holds the currently authenticated user for the session. */
public final class SessionManager {
    private static User currentUser;
    private SessionManager() {}
    public static void  login(User u)  { currentUser = u; }
    public static void  logout()       { currentUser = null; }
    public static User  getUser()      { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
}
