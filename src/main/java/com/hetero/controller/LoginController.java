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
 * Controller for {@code LoginView.fxml}.
 *
 * <p>Handles two distinct user flows on separate tabs:
 * <ol>
 *   <li><b>Sign In</b> — validates input, calls
 *       {@link DatabaseManager#authenticate}, stores the authenticated
 *       {@link User} in {@link SessionManager}, and transitions to the main layout.</li>
 *   <li><b>Create Account</b> — validates input, calls
 *       {@link DatabaseManager#register}, and switches the tab back to Sign In
 *       with the new username pre-filled.</li>
 * </ol>
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Instance variables and objects:</b>
 *       All {@code @FXML}-injected controls are private instance variables.</li>
 *   <li><b>Imported classes:</b>
 *       {@link FadeTransition}, {@link TranslateTransition}, {@link Optional},
 *       {@link DatabaseManager}, {@link SessionManager}, {@link User}, etc.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link HeteroApp}, {@link SessionManager}, {@link DatabaseManager}, {@link User}.</li>
 *   <li><b>Exception handling:</b>
 *       FXML-transition calls ({@link HeteroApp#showMain}) are wrapped in
 *       {@code try/catch}; errors are logged and surfaced to the user via the
 *       error label rather than crashing the application.</li>
 *   <li><b>Primitive data:</b>
 *       {@code boolean ok} from {@link DatabaseManager#register}; string length
 *       check {@code p.length() < 4} uses primitive {@code int} comparison.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       Variables like {@code enteredUsername}, {@code enteredPassword},
 *       {@code confirmPassword} — intent is unambiguous.</li>
 * </ul>
 */
public class LoginController {

    // ── Logger ────────────────────────────────────────────────────────────────

    /** Logger for authentication events and transition failures. */
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    // ── FXML-injected controls — Sign In tab ──────────────────────────────────

    /** The root tab pane; used to switch to the Sign In tab after registration. */
    @FXML private TabPane      tabPane;

    /** Username text field on the Sign In tab. */
    @FXML private TextField    tfLoginUser;

    /** Password field on the Sign In tab. */
    @FXML private PasswordField pfLoginPass;

    /** Error label displayed beneath the Sign In form when credentials are invalid. */
    @FXML private Label        lblLoginError;

    /** The Sign In button — shakes on failed login attempts. */
    @FXML private Button       btnLogin;

    // ── FXML-injected controls — Register tab ─────────────────────────────────

    /** Username field on the Create Account tab. */
    @FXML private TextField    tfRegUser;

    /** Optional display name field on the Create Account tab. */
    @FXML private TextField    tfRegDisplay;

    /** Password field on the Create Account tab. */
    @FXML private PasswordField pfRegPass;

    /** Confirm-password field on the Create Account tab. */
    @FXML private PasswordField pfRegConfirm;

    /** Error label displayed beneath the register form on validation failure. */
    @FXML private Label        lblRegError;

    /** Success label shown when a new account is created successfully. */
    @FXML private Label        lblRegSuccess;

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Handles the Sign In button click.
     *
     * <p>Validates that both fields are non-empty, then delegates to
     * {@link DatabaseManager#authenticate}. On success, stores the user in
     * {@link SessionManager} and transitions to the main layout.
     * On failure, displays an error and plays a shake animation on the button.
     */
    @FXML
    private void onLogin() {
        String enteredUsername = tfLoginUser.getText().trim();
        String enteredPassword = pfLoginPass.getText();

        // Basic input validation before hitting the database
        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            showErrorOnLabel(lblLoginError, "Please fill in both fields.");
            return;
        }

        Optional<User> authenticatedUser =
            DatabaseManager.getInstance().authenticate(enteredUsername, enteredPassword);

        if (authenticatedUser.isPresent()) {
            // Credentials valid — start the session and load the main shell
            SessionManager.login(authenticatedUser.get());
            LOGGER.info("[Login] User signed in: " + authenticatedUser.get().getUsername());

            try {
                HeteroApp.showMain();
            } catch (Exception sceneTransitionException) {
                LOGGER.log(Level.SEVERE,
                    "[Login] Failed to load main layout.", sceneTransitionException);
                showErrorOnLabel(lblLoginError, "Application error — please restart.");
            }

        } else {
            // No matching account found
            showErrorOnLabel(lblLoginError, "Invalid username or password.");
            playShakeAnimation(btnLogin);
        }
    }

    /**
     * Handles the Create Account button click.
     *
     * <p>Validates all fields (non-empty, matching passwords, minimum length),
     * then calls {@link DatabaseManager#register}. On success, switches to the
     * Sign In tab with the new username pre-filled.
     */
    @FXML
    private void onRegister() {
        String enteredUsername    = tfRegUser.getText().trim();
        String enteredDisplayName = tfRegDisplay.getText().trim();
        String enteredPassword    = pfRegPass.getText();
        String confirmPassword    = pfRegConfirm.getText();

        // ── Validation chain ──────────────────────────────────────────────────

        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            showErrorOnLabel(lblRegError, "Username and password are required.");
            return;
        }

        if (!enteredPassword.equals(confirmPassword)) {
            showErrorOnLabel(lblRegError, "Passwords do not match.");
            return;
        }

        if (enteredPassword.length() < 4) {
            showErrorOnLabel(lblRegError, "Password must be at least 4 characters.");
            return;
        }

        // Use username as display name if none provided
        String resolvedDisplayName = enteredDisplayName.isEmpty()
                ? enteredUsername
                : enteredDisplayName;

        // ── Persist the new account ────────────────────────────────────────────

        boolean registrationSucceeded =
            DatabaseManager.getInstance().register(
                enteredUsername, enteredPassword, resolvedDisplayName);

        if (registrationSucceeded) {
            lblRegError.setVisible(false);
            lblRegSuccess.setText("Account created! You can now sign in.");
            lblRegSuccess.setVisible(true);
            LOGGER.info("[Login] New account registered: " + enteredUsername);

            // Switch to Sign In tab and pre-fill the username for convenience
            tabPane.getSelectionModel().select(0);
            tfLoginUser.setText(enteredUsername);

        } else {
            showErrorOnLabel(lblRegError, "That username is already taken.");
        }
    }

    // ── Private UI helpers ────────────────────────────────────────────────────

    /**
     * Sets an error message on the given label, makes it visible, and fades it in.
     *
     * @param errorLabel the label to update and display
     * @param message    the error text to show
     */
    private void showErrorOnLabel(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        // Animate in with a short fade to draw the user's attention
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), errorLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Plays a horizontal shake animation on the given node to signal rejection.
     * Used on the Sign In button when authentication fails.
     *
     * @param targetNode the UI node to animate
     */
    private void playShakeAnimation(Node targetNode) {
        TranslateTransition shakeTransition =
            new TranslateTransition(Duration.millis(60), targetNode);
        shakeTransition.setByX(8);       // horizontal offset in pixels
        shakeTransition.setCycleCount(6); // oscillate 6 times
        shakeTransition.setAutoReverse(true);
        shakeTransition.play();
    }
}
