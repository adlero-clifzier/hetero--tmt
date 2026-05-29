package com.hetero.model;

/** Authenticated user session holder. */
public class User {
    private final int id;
    private final String username;
    private final String displayName;

    public User(int id, String username, String displayName) {
        this.id = id; this.username = username; this.displayName = displayName;
    }
    public int    getId()          { return id; }
    public String getUsername()    { return username; }
    public String getDisplayName() { return displayName; }
}
