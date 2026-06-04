package com.hetero.app;

import java.util.logging.Logger;

import com.hetero.db.DatabaseManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * HeteroApp is the starting point of the whole program.
 *
 * When you run the program, JavaFX calls the start() method first.
 * From there we open the login screen. After the user logs in,
 * we switch to the main screen.
 *
 * We also make sure the database connection is properly closed
 * when the user closes the window.
 */
public class HeteroApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(HeteroApp.class.getName());

    // We keep one reference to the window so we can swap screens later
    private static Stage primaryStage;

    /**
     * JavaFX calls this automatically when the app launches.
     * We set up the database and show the login screen.
     *
     * @param stage the main window JavaFX gives us
     * @throws Exception if the login screen file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Make sure the database and its tables exist before anything else
        DatabaseManager.getInstance();

        showLogin();
        LOGGER.info("[App] Hetero started.");
    }

    /**
     * JavaFX calls this when the user closes the window.
     * We close the database connection here to avoid file corruption.
     */
    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
        LOGGER.info("[App] Hetero stopped.");
    }

    /**
     * Shows the login screen.
     * This is called at startup and again after the user logs out.
     *
     * @throws Exception if LoginView.fxml cannot be found or loaded
     */
    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            HeteroApp.class.getResource("/com/hetero/fxml/LoginView.fxml"));

        Scene scene = new Scene(loader.load(), 480, 560);
        ThemeManager.init(scene);

        primaryStage.setTitle("Hetero — Sign In");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Shows the main application screen after a successful login.
     * Called by LoginController once the username and password are confirmed.
     *
     * @throws Exception if MainLayout.fxml cannot be found or loaded
     */
    public static void showMain() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            HeteroApp.class.getResource("/com/hetero/fxml/MainLayout.fxml"));

        Scene scene = new Scene(loader.load(), 1100, 720);
        ThemeManager.init(scene);

        primaryStage.setTitle("Hetero — Task Management");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(860);
        primaryStage.setMinHeight(580);

        // BUG FIX: show() must be called after setScene() so the window
        // actually refreshes. Without this line the login screen stays
        // frozen after a successful sign-in.
        primaryStage.show();
    }

    /**
     * Standard Java entry point.
     * Calling launch() hands control over to JavaFX.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
