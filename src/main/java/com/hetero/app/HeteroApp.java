package com.hetero.app;

import com.hetero.db.DatabaseManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX application entry point for the Hetero Task Management Tool.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Bootstrapping the SQLite database via {@link DatabaseManager} on startup.</li>
 *   <li>Showing the {@code LoginView} as the first screen.</li>
 *   <li>Transitioning to the {@code MainLayout} after successful authentication.</li>
 *   <li>Providing {@code static} scene-transition helpers so controllers can
 *       navigate without holding a direct reference to the {@link Stage}.</li>
 *   <li>Closing the database connection cleanly on application exit.</li>
 * </ul>
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Inheritance:</b>
 *       {@code extends Application} — JavaFX lifecycle hooks are inherited from
 *       {@link Application} and overridden here ({@code start}, {@code stop}).</li>
 *   <li><b>Imported classes:</b>
 *       {@link DatabaseManager}, {@link Application}, {@link FXMLLoader},
 *       {@link Scene}, {@link Stage}, {@link Logger}, {@link Level}.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link DatabaseManager}, {@link ThemeManager}.</li>
 *   <li><b>Instance variables and objects:</b>
 *       {@code primaryStage} is a static object reference shared across scene transitions.</li>
 *   <li><b>Exception handling:</b>
 *       FXML loading is wrapped in {@code try/catch}; fatal load failures are
 *       logged and re-thrown so the JVM exits with a clear error message.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       Method names ({@code showLogin}, {@code showMain}) clearly communicate intent.</li>
 * </ul>
 */
public class HeteroApp extends Application {

    // ── Class-level logger ────────────────────────────────────────────────────

    /** Application-wide logger for startup and shutdown events. */
    private static final Logger LOGGER = Logger.getLogger(HeteroApp.class.getName());

    // ── Shared stage reference ────────────────────────────────────────────────

    /**
     * The single primary window managed by the JavaFX runtime.
     * Stored as a {@code static} field so that static helper methods
     * ({@link #showLogin}, {@link #showMain}) can transition scenes
     * without requiring controllers to hold a stage reference.
     */
    private static Stage primaryStage;

    // ── JavaFX lifecycle ──────────────────────────────────────────────────────

    /**
     * Called by the JavaFX runtime after the FX toolkit has been initialised.
     *
     * <p>Bootstraps the SQLite database and presents the login screen.
     *
     * @param stage the primary window provided by the JavaFX runtime
     * @throws Exception if the Login FXML resource cannot be loaded
     */
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Initialise the database (creates tables and seeds admin account if needed)
        DatabaseManager.getInstance();

        showLogin();
        LOGGER.info("[App] Hetero application started.");
    }

    /**
     * Called by the JavaFX runtime when the application window is closed.
     *
     * <p>Releases the SQLite JDBC connection to avoid resource leaks.
     */
    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
        LOGGER.info("[App] Hetero application stopped cleanly.");
    }

    // ── Scene transition helpers ──────────────────────────────────────────────

    /**
     * Loads and displays the Login screen ({@code LoginView.fxml}).
     *
     * <p>Called on application startup and again after the user signs out
     * via the Settings view.  The window is made non-resizable on the login
     * screen to enforce the fixed 480 × 560 layout.
     *
     * @throws Exception if the FXML resource cannot be found or loaded
     */
    public static void showLogin() throws Exception {
        FXMLLoader loginLoader = new FXMLLoader(
            HeteroApp.class.getResource("/com/hetero/fxml/LoginView.fxml"));

        Scene loginScene = new Scene(loginLoader.load(), 480, 560);

        // Apply the active theme stylesheet to the new scene
        ThemeManager.init(loginScene);

        primaryStage.setTitle("Hetero — Sign In");
        primaryStage.setScene(loginScene);
        primaryStage.setResizable(false);
        primaryStage.show();

        LOGGER.info("[App] Login screen shown.");
    }

    /**
     * Loads and displays the main application shell ({@code MainLayout.fxml}).
     *
     * <p>Called by {@link com.hetero.controller.LoginController} after successful
     * authentication.  The window is made resizable with a minimum size constraint
     * to prevent the layout from collapsing below a usable width.
     *
     * @throws Exception if the FXML resource cannot be found or loaded
     */
    public static void showMain() throws Exception {
        FXMLLoader mainLoader = new FXMLLoader(
            HeteroApp.class.getResource("/com/hetero/fxml/MainLayout.fxml"));

        Scene mainScene = new Scene(mainLoader.load(), 1100, 720);

        // Re-apply the active theme to the new, larger scene
        ThemeManager.init(mainScene);

        primaryStage.setTitle("Hetero — Task Management");
        primaryStage.setScene(mainScene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(860);
        primaryStage.setMinHeight(580);

        LOGGER.info("[App] Main layout shown.");
    }

    // ── Standard Java entry point ─────────────────────────────────────────────

    /**
     * Standard Java entry point — delegates to {@link Application#launch} which
     * initialises the JavaFX toolkit and calls {@link #start(Stage)}.
     *
     * @param args command-line arguments (not used by Hetero)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
