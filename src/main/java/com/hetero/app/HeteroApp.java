package com.hetero.app;

import com.hetero.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX entry point — shows Login, then MainLayout on success. */
public class HeteroApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        DatabaseManager.getInstance();
        showLogin();
    }

    /** Loads the Login screen. */
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

    /** Called by LoginController on successful auth — loads the main shell. */
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
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) { launch(args); }
}
