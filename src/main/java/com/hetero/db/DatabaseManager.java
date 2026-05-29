package com.hetero.db;

import com.hetero.model.Priority;
import com.hetero.model.Task;
import com.hetero.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Singleton SQLite gateway — tasks + users tables. */
public final class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL = "jdbc:sqlite:hetero.db";
    private static DatabaseManager instance;
    private Connection conn;

    private DatabaseManager() { connect(); initSchema(); }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // ── Connection ────────────────────────────────────────────────────────────

    private void connect() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot open SQLite database", e);
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    private void initSchema() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                username     TEXT NOT NULL UNIQUE,
                password     TEXT NOT NULL,
                display_name TEXT
            );""";
        String tasks = """
            CREATE TABLE IF NOT EXISTS tasks (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                title        TEXT    NOT NULL,
                notes        TEXT,
                priority     TEXT    NOT NULL DEFAULT 'MEDIUM',
                category     TEXT    NOT NULL DEFAULT 'General',
                due_date     TEXT,
                is_completed INTEGER NOT NULL DEFAULT 0
            );""";
        try (Statement s = conn.createStatement()) {
            s.execute(users);
            s.execute(tasks);
            seedDefaultUser();
        } catch (SQLException e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }

    /** Creates a default admin/admin account if no users exist. */
    private void seedDefaultUser() {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "INSERT INTO users (username, password, display_name) VALUES ('admin','admin','Administrator')");
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Seed user failed", e);
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    /** Returns the User if credentials match, empty otherwise. */
    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT id, username, display_name FROM users WHERE username=? AND password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(
                new User(rs.getInt("id"), rs.getString("username"), rs.getString("display_name")));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Auth failed", e);
        }
        return Optional.empty();
    }

    /** Registers a new user. Returns false if username already taken. */
    public boolean register(String username, String password, String displayName) {
        String sql = "INSERT INTO users (username, password, display_name) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, password); ps.setString(3, displayName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false; // UNIQUE constraint violation
        }
    }

    // ── Tasks CRUD ────────────────────────────────────────────────────────────

    public List<Task> loadAll() {
        List<Task> list = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM tasks ORDER BY id")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOG.log(Level.SEVERE, "loadAll", e); }
        return list;
    }

    public int insert(Task t) {
        String sql = "INSERT INTO tasks (title,notes,priority,category,due_date,is_completed) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, t); ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) return k.getInt(1);
        } catch (SQLException e) { LOG.log(Level.SEVERE, "insert", e); }
        return -1;
    }

    public void update(Task t) {
        String sql = "UPDATE tasks SET title=?,notes=?,priority=?,category=?,due_date=?,is_completed=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, t); ps.setInt(7, t.getId()); ps.executeUpdate();
        } catch (SQLException e) { LOG.log(Level.SEVERE, "update", e); }
    }

    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { LOG.log(Level.SEVERE, "delete", e); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void bind(PreparedStatement ps, Task t) throws SQLException {
        ps.setString(1, t.getTitle());
        ps.setString(2, t.getNotes());
        ps.setString(3, t.getPriority().name());
        ps.setString(4, t.getCategory() != null ? t.getCategory() : "General");
        ps.setString(5, t.getDueDate() != null ? t.getDueDate().toString() : null);
        ps.setInt(6, t.isCompleted() ? 1 : 0);
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        String d = rs.getString("due_date");
        return new Task(rs.getInt("id"), rs.getString("title"), rs.getString("notes"),
                Priority.valueOf(rs.getString("priority")), rs.getString("category"),
                d != null ? LocalDate.parse(d) : null, rs.getInt("is_completed") == 1);
    }

    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException e) { LOG.log(Level.WARNING, "close", e); }
    }
}
