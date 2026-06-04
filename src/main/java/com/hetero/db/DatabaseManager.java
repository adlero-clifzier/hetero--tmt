package com.hetero.db;

import com.hetero.model.Priority;
import com.hetero.model.Task;
import com.hetero.model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DatabaseManager is the only class in the app that talks to SQLite.
 *
 * It uses the Singleton pattern — only one instance ever exists.
 * This prevents multiple connections to the same database file,
 * which could cause data corruption.
 *
 * On first launch it creates two tables:
 *   - "users"  for login accounts
 *   - "tasks"  for all task data
 *
 * It also inserts a default admin/admin account so the app
 * is always usable straight away.
 *
 * Every database call uses PreparedStatement instead of plain
 * string concatenation to prevent SQL injection attacks.
 *
 * All SQL errors are caught and logged — they never bubble up
 * and crash the application.
 */
public final class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    // The file-based SQLite database
    private static final String DATABASE_URL = "jdbc:sqlite:hetero.db";

    // Default credentials seeded when no users exist
    private static final String DEFAULT_ADMIN_USERNAME     = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD     = "admin";
    private static final String DEFAULT_ADMIN_DISPLAY_NAME = "Administrator";

    // The single instance (Singleton)
    private static DatabaseManager instance;

    // The live JDBC connection
    private Connection databaseConnection;

    /**
     * Private constructor — only called once by getInstance().
     * Opens the connection and sets up the tables.
     */
    private DatabaseManager() {
        openConnection();
        initialiseSchema();
    }

    /**
     * Returns the one shared instance of DatabaseManager.
     * Creates it on the first call, then reuses it every time after.
     *
     * @return the singleton DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Opens the JDBC connection to the SQLite file.
     * Throws a RuntimeException if it fails — the app cannot run without a database.
     */
    private void openConnection() {
        try {
            databaseConnection = DriverManager.getConnection(DATABASE_URL);
            databaseConnection.setAutoCommit(true); // Every statement saves immediately
            LOGGER.info("[DB] Connected: " + DATABASE_URL);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot open SQLite database: " + DATABASE_URL, e);
        }
    }

    /**
     * Creates the users and tasks tables if they do not already exist.
     * Safe to call on every startup — "IF NOT EXISTS" prevents duplicates.
     */
    private void initialiseSchema() {
        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    username     TEXT    NOT NULL UNIQUE,
                    password     TEXT    NOT NULL,
                    display_name TEXT
                );
                """;

        String createTasksTable = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    title        TEXT    NOT NULL,
                    notes        TEXT,
                    priority     TEXT    NOT NULL DEFAULT 'MEDIUM',
                    category     TEXT    NOT NULL DEFAULT 'General',
                    due_date     TEXT,
                    is_completed INTEGER NOT NULL DEFAULT 0
                );
                """;

        try (Statement statement = databaseConnection.createStatement()) {
            statement.execute(createUsersTable);
            statement.execute(createTasksTable);
            LOGGER.info("[DB] Schema ready.");
            seedDefaultAdminUser();
        } catch (SQLException e) {
            throw new RuntimeException("Schema setup failed", e);
        }
    }

    /**
     * Inserts an admin/admin account if no users exist yet.
     * This lets the app be used immediately after first launch.
     */
    private void seedDefaultAdminUser() {
        try (Statement statement = databaseConnection.createStatement();
             ResultSet resultSet  = statement.executeQuery("SELECT COUNT(*) FROM users")) {

            boolean noUsersExist = resultSet.next() && resultSet.getInt(1) == 0;

            if (noUsersExist) {
                String insertSql = "INSERT INTO users (username, password, display_name) VALUES (?,?,?)";
                try (PreparedStatement ps = databaseConnection.prepareStatement(insertSql)) {
                    ps.setString(1, DEFAULT_ADMIN_USERNAME);
                    ps.setString(2, DEFAULT_ADMIN_PASSWORD);
                    ps.setString(3, DEFAULT_ADMIN_DISPLAY_NAME);
                    ps.executeUpdate();
                    LOGGER.info("[DB] Default admin account created.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "[DB] Could not seed admin user.", e);
        }
    }

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Checks if the given username and password match a row in the users table.
     * Returns an Optional — empty means wrong credentials, present means success.
     *
     * @param username the login name entered by the user
     * @param password the password entered by the user
     * @return Optional with a User object if credentials are correct, empty otherwise
     */
    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT id, username, display_name FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement ps = databaseConnection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet resultSet = ps.executeQuery();

            if (resultSet.next()) {
                User authenticatedUser = new User(
                    resultSet.getInt("id"),
                    resultSet.getString("username"),
                    resultSet.getString("display_name")
                );
                return Optional.of(authenticatedUser);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[DB] Authentication failed.", e);
        }

        return Optional.empty();
    }

    /**
     * Creates a new user account in the database.
     * Returns false if the username is already taken (UNIQUE constraint).
     *
     * @param username    chosen login name
     * @param password    password (must be at least 4 characters — enforced by UI)
     * @param displayName name shown in the sidebar
     * @return true if the account was created, false if username is taken
     */
    public boolean register(String username, String password, String displayName) {
        String sql = "INSERT INTO users (username, password, display_name) VALUES (?, ?, ?)";

        try (PreparedStatement ps = databaseConnection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, displayName);
            ps.executeUpdate();
            LOGGER.info("[DB] Registered: " + username);
            return true;
        } catch (SQLException e) {
            // SQLite UNIQUE violation means the username is already taken
            LOGGER.log(Level.WARNING, "[DB] Registration failed for: " + username, e);
            return false;
        }
    }

    // ── Task CRUD ─────────────────────────────────────────────────────────────

    /**
     * Loads every row from the tasks table as a list of Task objects.
     * Results are sorted by id so the order is consistent.
     *
     * @return list of all saved tasks (empty list if none exist)
     */
    public List<Task> loadAll() {
        List<Task> taskList = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY id ASC";

        try (Statement statement = databaseConnection.createStatement();
             ResultSet resultSet  = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                taskList.add(mapRowToTask(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[DB] loadAll failed.", e);
        }

        return taskList;
    }

    /**
     * Saves a new task to the database and returns the auto-generated id.
     * Returns -1 if the insert failed.
     *
     * @param taskToInsert the task to save (id field is ignored)
     * @return the new database id, or -1 on failure
     */
    public int insert(Task taskToInsert) {
        String sql =
            "INSERT INTO tasks (title, notes, priority, category, due_date, is_completed) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps =
                 databaseConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindTaskToStatement(ps, taskToInsert);
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[DB] Insert failed: " + taskToInsert.getTitle(), e);
        }

        return -1;
    }

    /**
     * Updates an existing task row using the task's id to find the right row.
     *
     * @param updatedTask the task with new values (matched by id)
     */
    public void update(Task updatedTask) {
        String sql =
            "UPDATE tasks SET title=?, notes=?, priority=?, category=?, due_date=?, is_completed=? " +
            "WHERE id=?";

        try (PreparedStatement ps = databaseConnection.prepareStatement(sql)) {
            bindTaskToStatement(ps, updatedTask);
            ps.setInt(7, updatedTask.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[DB] Update failed for id=" + updatedTask.getId(), e);
        }
    }

    /**
     * Deletes a task row from the database by its id.
     *
     * @param taskId the id of the task to delete
     */
    public void delete(int taskId) {
        try (PreparedStatement ps =
                 databaseConnection.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[DB] Delete failed for id=" + taskId, e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Fills the first six parameters of a PreparedStatement from a Task object.
     * Used by both insert() and update() to avoid writing the same code twice.
     *
     * Parameter order: title, notes, priority, category, due_date, is_completed
     *
     * @param ps   the prepared statement to fill
     * @param task the task whose values should be used
     * @throws SQLException if a parameter binding fails
     */
    private void bindTaskToStatement(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getNotes() != null ? task.getNotes() : "");
        ps.setString(3, task.getPriority().name());
        ps.setString(4, task.getCategory() != null ? task.getCategory() : "General");
        ps.setString(5, task.getDueDate() != null ? task.getDueDate().toString() : null);
        ps.setInt(6, task.isCompleted() ? 1 : 0);
    }

    /**
     * Converts one row from a ResultSet into a Task object.
     * The due_date column is stored as text so we parse it back to LocalDate.
     *
     * @param resultSet a ResultSet positioned at the row to convert
     * @return a fully populated Task object
     * @throws SQLException if reading a column fails
     */
    private Task mapRowToTask(ResultSet resultSet) throws SQLException {
        String dueDateString = resultSet.getString("due_date");
        LocalDate parsedDueDate = (dueDateString != null) ? LocalDate.parse(dueDateString) : null;

        return new Task(
            resultSet.getInt("id"),
            resultSet.getString("title"),
            resultSet.getString("notes"),
            Priority.valueOf(resultSet.getString("priority")),
            resultSet.getString("category"),
            parsedDueDate,
            resultSet.getInt("is_completed") == 1
        );
    }

    /**
     * Closes the database connection.
     * Called by HeteroApp.stop() when the window is closed.
     */
    public void close() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
                LOGGER.info("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "[DB] Error closing connection.", e);
        }
    }
}
