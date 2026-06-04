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
 * SettingsController handles the Settings screen.
 *
 * It has three sections:
 *   1. Account — shows who is logged in with a Sign Out button
 *   2. Appearance — a toggle button that switches dark/light mode
 *   3. About — version info about the app
 */
public class SettingsController {

    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());

    @FXML private Label        lblUser;   // Shows "Administrator (@admin)"
    @FXML private ToggleButton tglTheme; // Switches dark/light mode

    /**
     * JavaFX calls this after the FXML fields are injected.
     * We fill in the user label and set the toggle to the current theme.
     */
    @FXML
    public void initialize() {
        User signedInUser = SessionManager.getUser();

        if (signedInUser != null) {
            lblUser.setText(
                signedInUser.getDisplayName() + " (@" + signedInUser.getUsername() + ")");
        } else {
            lblUser.setText("Not signed in");
        }

        // Make the toggle button reflect whichever theme is currently active
        boolean isLight = ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT;
        tglTheme.setSelected(isLight);
        tglTheme.setText(isLight ? "Light Mode" : "Dark Mode");
    }

    /**
     * Called when the theme toggle button is clicked.
     * Tells ThemeManager to swap the stylesheet, then updates the button label.
     */
    @FXML
    private void onToggleTheme() {
        ThemeManager.toggle();
        boolean isNowLight = ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT;
        tglTheme.setText(isNowLight ? "Light Mode" : "Dark Mode");
        LOGGER.info("[Settings] Theme: " + ThemeManager.getCurrent());
    }

    /**
     * Called when the Sign Out button is clicked.
     * Clears the session and goes back to the login screen.
     */
    @FXML
    private void onLogout() {
        SessionManager.logout();
        LOGGER.info("[Settings] User signed out.");

        try {
            HeteroApp.showLogin();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Settings] Could not load login screen.", e);
        }
    }
}
