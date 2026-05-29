package com.hetero.controller;

import com.hetero.app.SessionManager;
import com.hetero.db.DatabaseManager;
import com.hetero.model.Task;
import com.hetero.repository.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/** Root shell controller — strategy swap + navigation routing. */
public class MainLayoutController implements Initializable {

    @FXML private Label           lblViewTitle, lblPerfMetric, lblUserName;
    @FXML private ComboBox<String> cmbMode;
    @FXML private StackPane       contentArea;
    @FXML private Button          btnDashboard, btnAllTasks, btnToday, btnCategories, btnSettings;

    private static TaskRepository activeRepo;
    private String currentView = "dashboard";

    public static TaskRepository getActiveRepo() { return activeRepo; }

    private static final String MODE_HM = "HashMap Mode";
    private static final String MODE_LL = "LinkedList Mode";
    private static final String MODE_AL = "ArrayList Mode";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (SessionManager.getUser() != null)
            lblUserName.setText(SessionManager.getUser().getDisplayName());

        cmbMode.setItems(FXCollections.observableArrayList(MODE_HM, MODE_LL, MODE_AL));
        cmbMode.setValue(MODE_HM);
        activateStrategy(MODE_HM);
        setActiveNav(btnDashboard);
    }

    @FXML private void onModeChanged() {
        String sel = cmbMode.getValue();
        if (sel != null) { activateStrategy(sel); loadView(currentView); }
    }

    private void activateStrategy(String mode) {
        TaskRepository repo = switch (mode) {
            case MODE_LL -> new LinkedListTaskRepo();
            case MODE_AL -> new ArrayListTaskRepo();
            default      -> new HashMapTaskRepo();
        };
        long t0 = System.nanoTime();
        List<Task> all = DatabaseManager.getInstance().loadAll();
        repo.loadAll(all);
        long ns = System.nanoTime() - t0;
        activeRepo = repo;
        String metric = repo.getStrategyName() + " · " + all.size() + " tasks · " + String.format("%,d", ns) + " ns";
        System.out.println("[Benchmark] Strategy → " + metric);
        Platform.runLater(() -> lblPerfMetric.setText(metric));
    }

    @FXML private void onNavDashboard()  { nav("Dashboard",  "dashboard",  btnDashboard);  }
    @FXML private void onNavAllTasks()   { nav("All Tasks",  "all-tasks",  btnAllTasks);   }
    @FXML private void onNavToday()      { nav("Today",      "today",      btnToday);      }
    @FXML private void onNavCategories() { nav("Categories", "categories", btnCategories); }
    @FXML private void onNavSettings()   { nav("Settings",   "settings",   btnSettings);   }

    private void nav(String title, String view, Button btn) {
        lblViewTitle.setText(title);
        currentView = view;
        setActiveNav(btn);
        loadView(view);
    }

    private void loadView(String name) {
        contentArea.getChildren().clear();
        String path = "/com/hetero/fxml/" + name + "View.fxml";
        try {
            URL res = getClass().getResource(path);
            if (res == null) {
                Label ph = new Label("[ " + name + " — coming soon ]");
                ph.setStyle("-fx-text-fill:#4b5563;-fx-font-size:15px;");
                contentArea.getChildren().add(ph);
                return;
            }
            Node view = new FXMLLoader(res).load();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            Label err = new Label("Error: " + e.getMessage());
            err.setStyle("-fx-text-fill:#f87171;");
            contentArea.getChildren().add(err);
        }
    }

    private void setActiveNav(Button active) {
        List.of(btnDashboard, btnAllTasks, btnToday, btnCategories, btnSettings)
            .forEach(b -> b.getStyleClass().remove("active"));
        active.getStyleClass().add("active");
    }

    public void updatePerfMetric(String text) {
        Platform.runLater(() -> lblPerfMetric.setText(text));
    }
}
