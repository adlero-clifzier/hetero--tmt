package com.hetero.controller;

import com.hetero.app.HeteroApp;
import com.hetero.app.SessionManager;
import com.hetero.app.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

/** Settings view — theme toggle, user info, logout. */
public class SettingsController {

    @FXML private Label lblUser;
    @FXML private ToggleButton tglTheme;

    @FXML
    public void initialize() {
        if (SessionManager.getUser() != null)
            lblUser.setText(SessionManager.getUser().getDisplayName()
                + " (@" + SessionManager.getUser().getUsername() + ")");
        tglTheme.setSelected(ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT);
        tglTheme.setText(tglTheme.isSelected() ? "Light Mode" : "Dark Mode");
    }

    @FXML private void onToggleTheme() {
        ThemeManager.toggle();
        tglTheme.setText(ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT ? "Light Mode" : "Dark Mode");
    }

    @FXML private void onLogout() {
        SessionManager.logout();
        try { HeteroApp.showLogin(); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
