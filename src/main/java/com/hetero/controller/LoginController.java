package com.hetero.controller;

import com.hetero.app.HeteroApp;
import com.hetero.app.SessionManager;
import com.hetero.db.DatabaseManager;
import com.hetero.model.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Optional;

/** Handles login and registration on the LoginView. */
public class LoginController {

    @FXML private TabPane   tabPane;
    @FXML private TextField tfLoginUser;
    @FXML private PasswordField pfLoginPass;
    @FXML private Label     lblLoginError;
    @FXML private Button    btnLogin;

    @FXML private TextField tfRegUser;
    @FXML private TextField tfRegDisplay;
    @FXML private PasswordField pfRegPass;
    @FXML private PasswordField pfRegConfirm;
    @FXML private Label     lblRegError;
    @FXML private Label     lblRegSuccess;

    @FXML
    private void onLogin() {
        String u = tfLoginUser.getText().trim();
        String p = pfLoginPass.getText();
        if (u.isEmpty() || p.isEmpty()) { showError(lblLoginError, "Fill in all fields."); return; }

        Optional<User> user = DatabaseManager.getInstance().authenticate(u, p);
        if (user.isPresent()) {
            SessionManager.login(user.get());
            try { HeteroApp.showMain(); }
            catch (Exception e) { showError(lblLoginError, "Failed to load app."); }
        } else {
            showError(lblLoginError, "Invalid username or password.");
            shake(btnLogin);
        }
    }

    @FXML
    private void onRegister() {
        String u  = tfRegUser.getText().trim();
        String dn = tfRegDisplay.getText().trim();
        String p  = pfRegPass.getText();
        String c  = pfRegConfirm.getText();

        if (u.isEmpty() || p.isEmpty()) { showError(lblRegError, "Username and password required."); return; }
        if (!p.equals(c))               { showError(lblRegError, "Passwords do not match."); return; }
        if (p.length() < 4)             { showError(lblRegError, "Password must be ≥ 4 characters."); return; }

        boolean ok = DatabaseManager.getInstance().register(u, p, dn.isEmpty() ? u : dn);
        if (ok) {
            lblRegError.setVisible(false);
            lblRegSuccess.setText("Account created! You can now sign in.");
            lblRegSuccess.setVisible(true);
            tabPane.getSelectionModel().select(0);
            tfLoginUser.setText(u);
        } else {
            showError(lblRegError, "Username already taken.");
        }
    }

    private void showError(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), lbl);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void shake(javafx.scene.Node node) {
        javafx.animation.TranslateTransition tt =
            new javafx.animation.TranslateTransition(Duration.millis(60), node);
        tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true); tt.play();
    }
}
