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
 * Singleton gateway for all SQLite database interactions in the Hetero application.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Opening and maintaining the JDBC connection to {@code hetero.db}.</li>
 *   <li>Creating the {@code tasks} and {@code users} tables on first launch
 *       (idempotent via {@code CREATE TABLE IF NOT EXISTS}).</li>
 *   <li>Seeding a default {@code admin/admin} account when no users exist.</li>
 *   <li>Providing parameterised CRUD operations that protect against SQL injection.</li>
 *   <li>Mapping {@link ResultSet} rows back to {@link Task} and {@link User} objects.</li>
 * </ul>
 *
 * <p>The Singleton pattern guarantees exactly one connection is held per JVM process,
 * preventing resource leaks and concurrent-modification issues in SQLite.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Exception handling:</b> Every JDBC call is wrapped in a
 *       {@code try-with-resources} or {@code try/catch} block.  Fatal errors
 *       (connection failure, schema creation failure) throw a {@link RuntimeException}
 *       to fail fast; recoverable errors are logged and allow graceful degradation.</li>
 *   <li><b>Imported classes:</b> {@link Connection}, {@link DriverManager},
 *       {@link PreparedStatement}, {@link ResultSet}, {@link SQLException},
 *       {@link Statement}, {@link LocalDate}, {@link ArrayList}, {@link List},
 *       {@link Optional}, {@link Logger}, {@link Level}.</li>
 *   <li><b>Custom-built classes:</b> {@link Task}, {@link User}, {@link Priority}.</li>
 *   <li><b>Instance variables:</b> {@code databaseConnection} and {@code instance}.</li>
 *   <li><b>Primitive data:</b> {@code int} keys, {@code boolean} completed flag
 *       stored as {@code INTEGER 0/1} in SQLite.</li>
 *   <li><b>Access control:</b> Constructor is {@code private}; all state is
 *       accessed through the public {@code getInstance()} factory.</li>
 *   <li><b>Meaningful identifiers:</b> Variables like {@code generatedKeys},
 *       {@code taskRow}, {@code dueDateString} instead of single-letter names.</li>
 * </ul>
 */
public final class DatabaseManager {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Java standard logging facade — avoids a third-party logging dependency. */
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /** JDBC URL pointing to the local SQLite database file. */
    private static final String DATABASE_URL = "jdbc:sqlite:hetero.db";

    /** Default seed username created when the database is first initialised. */
    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    /** Default seed password — users should change this after first login. */
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";

    /** Default display name for the seeded admin account. */
    private static final String DEFAULT_ADMIN_DISPLAY_NAME = "Administrator";

    // ── Singleton state ───────────────────────────────────────────────────────

    /** The single application-wide instance of this manager. */
    private static DatabaseManager instance;

    /** The live JDBC connection to the SQLite file. */
    private Connection databaseConnection;

    // ── Singleton constructor ─────────────────────────────────────────────────

    /**
     * Private constructor enforces the Singleton pattern.
     * Opens the JDBC connection and initialises the schema on first call.
     *
     * @throws RuntimeException if the connection or schema initialisation fails
     */
    private DatabaseManager() {
        openConnection();
        initialiseSchema();
    }

    /**
     * Returns the application-wide singleton {@link DatabaseManager} instance,
     * creating it on the very first call.
     *
     * <p>Marked {@code synchronized} to be safe if multiple threads race during
     * application startup, though JavaFX typically initialises on a single thread.
     *
     * @return the shared {@link DatabaseManager}; never {@code null}
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ── Connection management ─────────────────────────────────────────────────

    /**
     * Opens the JDBC connection to the SQLite database file.
     *
     * <p>Auto-commit is enabled so every statement is immediately durable —
     * explicit transaction management is not required for single-user desktop use.
     *
     * @throws RuntimeException wrapping the {@link SQLException} if the connection fails
     */
    private void openConnection() {
        try {
            databaseConnection = DriverManager.getConnection(DATABASE_URL);
            databaseConnection.setAutoCommit(true);
            LOGGER.info("[DB] Connection opened: " + DATABASE_URL);
        } catch (SQLException sqlException) {
            // Fatal — the application cannot function without a database connection
            throw new RuntimeException("Cannot open SQLite database at: " + DATABASE_URL, sqlException);
        }
    }

    // ── Schema initialisation ─────────────────────────────────────────────────

    /**
     * Creates the {@code users} and {@code tasks} tables if they do not already exist.
     *
     * <p>Using {@code CREATE TABLE IF NOT EXISTS} makes this method safely idempotent —
     * it can be called on every startup without data loss or errors.
     *
     * @throws RuntimeException if the DDL statements fail to execute
     */
    private void initialiseSchema() {

        String createUsersTableSql = """
                CREATE TABLE IF NOT EXISTS users (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    username     TEXT    NOT NULL UNIQUE,
                    password     TEXT    NOT NULL,
                    display_name TEXT
                );
                """;

        String createTasksTableSql = """
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
            statement.execute(createUsersTableSql);
            statement.execute(createTasksTableSql);
            LOGGER.info("[DB] Schema initialised successfully.");
            seedDefaultAdminUser();
        } catch (SQLException sqlException) {
            // Fatal — the application cannot operate without the correct schema
            throw new RuntimeException("Schema initialisation failed", sqlException);
        }
    }

    /**
     * Inserts a default {@code admin/admin} account if the {@code users} table is empty.
     *
     * <p>This ensures the application is always accessible on first launch without
     * requiring the user to complete a registration step.  Users should change the
     * default credentials after their first login.
     */
    private void seedDefaultAdminUser() {
        String countUsersSql = "SELECT COUNT(*) FROM users";
        try (Statement statement  = databaseConnection.createStatement();
             ResultSet resultSet   = statement.executeQuery(countUsersSql)) {

            boolean tableIsEmpty = resultSet.next() && resultSet.getInt(1) == 0;

            if (tableIsEmpty) {
                String insertAdminSql =
                    "INSERT INTO users (username, password, display_name) VALUES (?,?,?)";

                try (PreparedStatement preparedStatement =
                         databaseConnection.prepareStatement(insertAdminSql)) {
                    preparedStatement.setString(1, DEFAULT_ADMIN_USERNAME);
                    preparedStatement.setString(2, DEFAULT_ADMIN_PASSWORD);
                    preparedStatement.setString(3, DEFAULT_ADMIN_DISPLAY_NAME);
                    preparedStatement.executeUpdate();
                    LOGGER.info("[DB] Default admin account seeded.");
                }
            }
        } catch (SQLException sqlException) {
            // Non-fatal — log and continue; user can still register manually
            LOGGER.log(Level.WARNING, "[DB] Failed to seed default admin user.", sqlException);
        }
    }

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Authenticates a user by checking the provided credentials against the
     * {@code users} table using a parameterised query to prevent SQL injection.
     *
     * @param username the login username entered by the user
     * @param password the plaintext password entered by the user
     * @return an {@link Optional} containing the matching {@link User}, or
     *         {@code Optional.empty()} if credentials are incorrect
     */
    public Optional<User> authenticate(String username, String password) {
        String selectUserSql =
            "SELECT id, username, display_name FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement preparedStatement =
                 databaseConnection.prepareStatement(selectUserSql)) {

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // Credentials matched — wrap in Optional and return the User object
                User authenticatedUser = new User(
                    resultSet.getInt("id"),
                    resultSet.getString("username"),
                    resultSet.getString("display_name")
                );
                return Optional.of(authenticatedUser);
            }

        } catch (SQLException sqlException) {
            LOGGER.log(Level.SEVERE, "[DB] Authentication query failed.", sqlException);
        }

        // No matching row found — return empty Optional (not an exception)
        return Optional.empty();
    }

    /**
     * Registers a new user account in the {@code users} table.
     *
     * <p>Returns {@code false} without throwing if the username is already taken
     * (SQLite UNIQUE constraint violation), so the caller can display a friendly
     * error message rather than catching an exception.
     *
     * @param username    the desired unique login name
     * @param password    the plaintext password; must be at least 4 characters (enforced by UI)
     * @param displayName the friendly name shown in the UI
     * @return {@code true} if the account was created; {@code false} if username is taken
     */
    public boolean register(String username, String password, String displayName) {
        String insertUserSql =
            "INSERT INTO users (username, password, display_name) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement =
                 databaseConnection.prepareStatement(insertUserSql)) {

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, displayName);
            preparedStatement.executeUpdate();

            LOGGER.info("[DB] New user registered: " + username);
            return true;

        } catch (SQLException sqlException) {
            // SQLite error code 19 = UNIQUE constraint violated — expected, not fatal
            LOGGER.log(Level.WARNING,
                "[DB] Registration failed for username '" + username + "' (possibly duplicate).",
                sqlException);
            return false;
        }
    }

    // ── Task CRUD operations ──────────────────────────────────────────────────

    /**
     * Loads every row from the {@code tasks} table and maps them to {@link Task} objects.
     *
     * <p>Results are ordered by ascending {@code id} to maintain a consistent
     * insertion-order presentation across all three repository strategies.
     *
     * @return a mutable {@link List} of all persisted tasks; empty if no tasks exist
     */
    public List<Task> loadAll() {
        List<Task> taskList = new ArrayList<>();
        String selectAllTasksSql = "SELECT * FROM tasks ORDER BY id ASC";

        try (Statement statement = databaseConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(selectAllTasksSql)) {

            while (resultSet.next()) {
                taskList.add(mapResultSetRowToTask(resultSet));
            }

        } catch (SQLException sqlException) {
            LOGGER.log(Level.SEVERE, "[DB] Failed to load all tasks.", sqlException);
        }

        return taskList;
    }

    /**
     * Inserts a new task row into the {@code tasks} table.
     *
     * <p>The {@code id} field of the provided task is ignored — SQLite assigns
     * the auto-incremented key, which is returned to the caller so the in-memory
     * collection can be updated with the authoritative id.
     *
     * @param taskToInsert the task to persist; {@code title} must not be null
     * @return the auto-generated primary key assigned by SQLite; {@code -1} on failure
     */
    public int insert(Task taskToInsert) {
        String insertTaskSql =
            "INSERT INTO tasks (title, notes, priority, category, due_date, is_completed) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement =
                 databaseConnection.prepareStatement(insertTaskSql, Statement.RETURN_GENERATED_KEYS)) {

            bindTaskFieldsToStatement(preparedStatement, taskToInsert);
            preparedStatement.executeUpdate();

            // Retrieve the auto-generated primary key SQLite assigned to this row
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1);
                LOGGER.fine("[DB] Task inserted with id=" + generatedId);
                return generatedId;
            }

        } catch (SQLException sqlException) {
            LOGGER.log(Level.SEVERE,
                "[DB] Failed to insert task: " + taskToInsert.getTitle(), sqlException);
        }

        return -1; // Signals a failed insert to the repository layer
    }

    /**
     * Updates all mutable fields of an existing task row matched by primary key.
     *
     * @param updatedTask the task carrying new field values; matched by {@code getId()}
     */
    public void update(Task updatedTask) {
        String updateTaskSql =
            "UPDATE tasks " +
            "SET title = ?, notes = ?, priority = ?, category = ?, due_date = ?, is_completed = ? " +
            "WHERE id = ?";

        try (PreparedStatement preparedStatement =
                 databaseConnection.prepareStatement(updateTaskSql)) {

            bindTaskFieldsToStatement(preparedStatement, updatedTask);
            preparedStatement.setInt(7, updatedTask.getId()); // WHERE clause parameter
            preparedStatement.executeUpdate();

        } catch (SQLException sqlException) {
            LOGGER.log(Level.SEVERE,
                "[DB] Failed to update task id=" + updatedTask.getId(), sqlException);
        }
    }

    /**
     * Deletes a single task row from the {@code tasks} table by primary key.
     *
     * @param taskId the primary key of the task to delete
     */
    public void delete(int taskId) {
        String deleteTaskSql = "DELETE FROM tasks WHERE id = ?";

        try (PreparedStatement preparedStatement =
                 databaseConnection.prepareStatement(deleteTaskSql)) {

            preparedStatement.setInt(1, taskId);
            preparedStatement.executeUpdate();
            LOGGER.fine("[DB] Task deleted, id=" + taskId);

        } catch (SQLException sqlException) {
            LOGGER.log(Level.SEVERE,
                "[DB] Failed to delete task id=" + taskId, sqlException);
        }
    }

    // ── Private mapping helpers ───────────────────────────────────────────────

    /**
     * Binds the first six positional parameters of a task-related
     * {@link PreparedStatement} from the given {@link Task} object.
     *
     * <p>Parameter positions:
     * <ol>
     *   <li>title</li>
     *   <li>notes</li>
     *   <li>priority (stored as enum name string)</li>
     *   <li>category (defaults to "General" if null)</li>
     *   <li>due_date (stored as ISO-8601 string, or SQL NULL)</li>
     *   <li>is_completed (stored as INTEGER 1 or 0)</li>
     * </ol>
     *
     * @param preparedStatement the statement to bind parameters into
     * @param taskToBind        the source task whose fields are being bound
     * @throws SQLException if any parameter binding operation fails
     */
    private void bindTaskFieldsToStatement(PreparedStatement preparedStatement,
                                           Task taskToBind) throws SQLException {
        preparedStatement.setString(1, taskToBind.getTitle());
        preparedStatement.setString(2, taskToBind.getNotes());
        preparedStatement.setString(3, taskToBind.getPriority().name());
        preparedStatement.setString(4,
            taskToBind.getCategory() != null ? taskToBind.getCategory() : "General");
        preparedStatement.setString(5,
            taskToBind.getDueDate() != null ? taskToBind.getDueDate().toString() : null);
        preparedStatement.setInt(6, taskToBind.isCompleted() ? 1 : 0);
    }

    /**
     * Maps a single row from a {@link ResultSet} to a fully populated {@link Task} object.
     *
     * <p>The {@code due_date} column is stored as an ISO-8601 text string in SQLite and
     * parsed back to a {@link LocalDate} here.  A {@code null} column value maps to a
     * {@code null} {@link LocalDate} reference on the {@link Task}.
     *
     * @param resultSet a {@link ResultSet} positioned at the row to map
     * @return a fully populated {@link Task} instance
     * @throws SQLException if any column retrieval operation fails
     */
    private Task mapResultSetRowToTask(ResultSet resultSet) throws SQLException {
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Closes the JDBC connection to SQLite.
     *
     * <p>Should be called from {@link com.hetero.app.HeteroApp#stop()} to ensure
     * the connection is cleanly released when the JavaFX application exits.
     */
    public void close() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
                LOGGER.info("[DB] Connection closed.");
            }
        } catch (SQLException sqlException) {
            LOGGER.log(Level.WARNING, "[DB] Error closing database connection.", sqlException);
        }
    }
}
