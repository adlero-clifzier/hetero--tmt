package com.hetero.controller;

import com.hetero.app.HeteroApp;
import com.hetero.app.SessionManager;
import com.hetero.app.ThemeManager;
import com.hetero.model.User;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for {@code settingsView.fxml}.
 *
 * <p>Provides three capabilities:
 * <ol>
 *   <li><b>Account display</b> — shows the signed-in user's display name and username.</li>
 *   <li><b>Theme toggle</b> — switches between dark and light mode via {@link ThemeManager}.</li>
 *   <li><b>Sign out</b> — clears the session via {@link SessionManager} and returns
 *       the user to the login screen.</li>
 * </ol>
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Instance variables and objects:</b>
 *       {@code lblUser} and {@code tglTheme} are private instance fields.</li>
 *   <li><b>Imported classes:</b>
 *       {@link HeteroApp}, {@link SessionManager}, {@link ThemeManager},
 *       {@link User}, {@link Label}, {@link ToggleButton}, {@link Logger}.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link HeteroApp}, {@link SessionManager}, {@link ThemeManager}, {@link User}.</li>
 *   <li><b>Exception handling:</b>
 *       The scene-transition call in {@link #onLogout} is wrapped in {@code try/catch};
 *       failures are logged rather than crashing the application.</li>
 *   <li><b>Proper access control:</b>
 *       FXML-injected fields are {@code private}; event handlers are
 *       {@code private} annotated with {@code @FXML}.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code signedInUser}, {@code userDisplayString}.</li>
 * </ul>
 */
public class SettingsController {

    // ── Logger ────────────────────────────────────────────────────────────────

    /** Logger for settings events (theme changes, logout). */
    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());

    // ── FXML-injected controls ────────────────────────────────────────────────

    /** Label showing the signed-in user's display name and username. */
    @FXML private Label        lblUser;

    /** Toggle button that switches between "Dark Mode" and "Light Mode". */
    @FXML private ToggleButton tglTheme;

    // ── FXML lifecycle ────────────────────────────────────────────────────────

    /**
     * Called by JavaFX after all {@code @FXML} fields are injected.
     *
     * <p>Populates the account label with the signed-in user's information
     * and sets the theme toggle to reflect the current theme.
     */
    @FXML
    public void initialize() {
        User signedInUser = SessionManager.getUser();

        if (signedInUser != null) {
            // Format: "Administrator (@admin)"
            String userDisplayString = signedInUser.getDisplayName()
                + " (@" + signedInUser.getUsername() + ")";
            lblUser.setText(userDisplayString);
        } else {
            lblUser.setText("Not signed in");
        }

        // Reflect the current theme state on the toggle button
        boolean isLightModeActive = ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT;
        tglTheme.setSelected(isLightModeActive);
        tglTheme.setText(isLightModeActive ? "Light Mode" : "Dark Mode");
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Handles the theme toggle button click.
     *
     * <p>Delegates to {@link ThemeManager#toggle()} which swaps the CSS stylesheet
     * on the active scene immediately, then updates the button label to reflect
     * the new theme state.
     */
    @FXML
    private void onToggleTheme() {
        ThemeManager.toggle();
        boolean isNowLight = ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT;
        tglTheme.setText(isNowLight ? "Light Mode" : "Dark Mode");
        LOGGER.info("[Settings] Theme switched to: " + ThemeManager.getCurrent());
    }

    /**
     * Handles the Sign Out button click.
     *
     * <p>Clears the active session via {@link SessionManager#logout}, then
     * calls {@link HeteroApp#showLogin()} to return to the login screen.
     * Any exception during the scene transition is caught and logged.
     */
    @FXML
    private void onLogout() {
        SessionManager.logout();
        LOGGER.info("[Settings] User signed out.");

        try {
            HeteroApp.showLogin();
        } catch (Exception sceneTransitionException) {
            LOGGER.log(Level.SEVERE,
                "[Settings] Failed to load login screen after logout.",
                sceneTransitionException);
        }
    }
}
