package com.hetero.controller;

import com.hetero.app.HeteroApp;
import com.hetero.app.SessionManager;
import com.hetero.db.DatabaseManager;
import com.hetero.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoginController handles everything on the login screen.
 *
 * The login screen has two tabs:
 *   1. Sign In  — checks the username and password against the database
 *   2. Create Account — registers a new user account
 *
 * If login succeeds, we save the user in SessionManager and
 * call HeteroApp.showMain() to switch to the main screen.
 *
 * If login fails, we show an error message and play a shake animation
 * on the button so the user knows something went wrong.
 */
public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    // ── Sign In tab controls ───────────────────────────────────────────────────

    @FXML private TabPane       tabPane;       // Used to switch back to Sign In after registration
    @FXML private TextField     tfLoginUser;   // Username input on the Sign In tab
    @FXML private PasswordField pfLoginPass;   // Password input on the Sign In tab
    @FXML private Label         lblLoginError; // Error message shown on bad credentials
    @FXML private Button        btnLogin;      // The Sign In button (shakes on failure)

    // ── Create Account tab controls ────────────────────────────────────────────

    @FXML private TextField     tfRegUser;     // Chosen username
    @FXML private TextField     tfRegDisplay;  // Optional display name
    @FXML private PasswordField pfRegPass;     // Password
    @FXML private PasswordField pfRegConfirm;  // Confirm password
    @FXML private Label         lblRegError;   // Error message for invalid input
    @FXML private Label         lblRegSuccess; // Success message after registration

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Called when the Sign In button is clicked.
     *
     * Checks that both fields are filled, then asks the database to verify
     * the credentials. On success the app transitions to the main screen.
     * On failure an error message appears and the button shakes.
     */
    @FXML
    private void onLogin() {
        String enteredUsername = tfLoginUser.getText().trim();
        String enteredPassword = pfLoginPass.getText();

        // Both fields must be filled before we bother querying the database
        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            showError(lblLoginError, "Please fill in both fields.");
            return;
        }

        Optional<User> result =
            DatabaseManager.getInstance().authenticate(enteredUsername, enteredPassword);

        if (result.isPresent()) {
            // Login successful — save the user and load the main screen
            SessionManager.login(result.get());
            LOGGER.info("[Login] Signed in: " + result.get().getUsername());

            try {
                HeteroApp.showMain();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[Login] Could not load main screen.", e);
                showError(lblLoginError, "Application error — please restart.");
            }
        } else {
            // Wrong credentials
            showError(lblLoginError, "Invalid username or password.");
            playShake(btnLogin);
        }
    }

    /**
     * Called when the Create Account button is clicked.
     *
     * Validates all four fields, then asks the database to create the account.
     * If successful, switches to the Sign In tab with the username pre-filled.
     */
    @FXML
    private void onRegister() {
        String enteredUsername    = tfRegUser.getText().trim();
        String enteredDisplayName = tfRegDisplay.getText().trim();
        String enteredPassword    = pfRegPass.getText();
        String confirmPassword    = pfRegConfirm.getText();

        // Check required fields
        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            showError(lblRegError, "Username and password are required.");
            return;
        }

        // Check that both password fields match
        if (!enteredPassword.equals(confirmPassword)) {
            showError(lblRegError, "Passwords do not match.");
            return;
        }

        // Enforce minimum password length
        if (enteredPassword.length() < 4) {
            showError(lblRegError, "Password must be at least 4 characters.");
            return;
        }

        // Use the username as display name if the user left that field blank
        String resolvedDisplayName = enteredDisplayName.isEmpty()
                ? enteredUsername
                : enteredDisplayName;

        boolean success = DatabaseManager.getInstance()
            .register(enteredUsername, enteredPassword, resolvedDisplayName);

        if (success) {
            lblRegError.setVisible(false);
            lblRegSuccess.setText("Account created! You can now sign in.");
            lblRegSuccess.setVisible(true);
            LOGGER.info("[Login] Registered: " + enteredUsername);

            // Switch to Sign In tab and pre-fill the username for convenience
            tabPane.getSelectionModel().select(0);
            tfLoginUser.setText(enteredUsername);
        } else {
            showError(lblRegError, "That username is already taken.");
        }
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    /**
     * Shows an error message on the given label with a short fade-in animation.
     *
     * @param label   the label to display the message on
     * @param message the error text to show
     */
    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);

        FadeTransition fade = new FadeTransition(Duration.millis(300), label);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Plays a horizontal shake animation on the given button.
     * Used to signal that a login attempt was rejected.
     *
     * @param target the button to shake
     */
    private void playShake(Node target) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), target);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
}
