package com.hetero.controller;

import com.hetero.app.SessionManager;
import com.hetero.db.DatabaseManager;
import com.hetero.model.Task;
import com.hetero.repository.ArrayListTaskRepo;
import com.hetero.repository.HashMapTaskRepo;
import com.hetero.repository.LinkedListTaskRepo;
import com.hetero.repository.TaskRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainLayoutController controls the outer shell of the app —
 * the sidebar, topbar, and the centre content area.
 *
 * Its two main jobs are:
 *
 *   1. Data structure switching — when the user picks a different mode
 *      in the ComboBox (HashMap, LinkedList, ArrayList), this controller
 *      creates a new repository, reloads all tasks from the database into
 *      it, and updates the benchmark label in the topbar.
 *
 *   2. Navigation — each sidebar button loads the corresponding FXML
 *      sub-view into the centre StackPane without restarting the whole app.
 *
 * The active repository is stored in a static field so that every
 * other controller can call getActiveRepository() to get it without
 * needing a direct reference to this controller.
 *
 * This is the Strategy Pattern in action — the repository type can
 * change at runtime while everything else stays the same.
 */
public class MainLayoutController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MainLayoutController.class.getName());

    // ComboBox option labels
    private static final String MODE_LABEL_HASHMAP    = "HashMap Mode";
    private static final String MODE_LABEL_LINKEDLIST = "LinkedList Mode";
    private static final String MODE_LABEL_ARRAYLIST  = "ArrayList Mode";

    // The currently active data structure — declared as the interface type
    // so polymorphism works (any of the three implementations can go here)
    private static TaskRepository activeRepository;

    // Tracks which sub-view is currently loaded so we can reload it after a mode switch
    private String currentViewName = "dashboard";

    // ── FXML-injected controls ────────────────────────────────────────────────

    @FXML private Label            lblViewTitle;   // Shows the name of the current view
    @FXML private Label            lblPerfMetric;  // Shows strategy name + benchmark time
    @FXML private Label            lblUserName;    // Shows the logged-in user's display name
    @FXML private ComboBox<String> cmbMode;        // Dropdown to pick the data structure
    @FXML private StackPane        contentArea;    // The centre panel where sub-views are loaded

    @FXML private Button btnDashboard;
    @FXML private Button btnAllTasks;
    @FXML private Button btnToday;
    @FXML private Button btnCategories;
    @FXML private Button btnSettings;

    // ── Initializable ─────────────────────────────────────────────────────────

    /**
     * JavaFX calls this automatically after all the @FXML fields are filled in.
     * We set up the ComboBox, activate the default HashMap strategy, and
     * load the Dashboard as the first view.
     *
     * @param location  not used
     * @param resources not used
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Show the logged-in user's name in the sidebar
        if (SessionManager.getUser() != null) {
            lblUserName.setText(SessionManager.getUser().getDisplayName());
        }

        // Fill the mode ComboBox with the three options
        cmbMode.setItems(FXCollections.observableArrayList(
            MODE_LABEL_HASHMAP, MODE_LABEL_LINKEDLIST, MODE_LABEL_ARRAYLIST));
        cmbMode.setValue(MODE_LABEL_HASHMAP);

        // Load all tasks into the HashMap and show the Dashboard
        activateRepositoryStrategy(MODE_LABEL_HASHMAP);
        setActiveNavigationButton(btnDashboard);
    }

    // ── Public accessor used by child controllers ─────────────────────────────

    /**
     * Returns the currently active repository.
     * Every sub-view controller calls this to perform CRUD operations.
     *
     * @return the active TaskRepository (never null after initialize runs)
     */
    public static TaskRepository getActiveRepository() {
        return activeRepository;
    }

    // ── Mode switching ────────────────────────────────────────────────────────

    /**
     * Called when the user picks a different option in the ComboBox.
     * Swaps the repository strategy and reloads the current view.
     */
    @FXML
    private void onModeChanged() {
        String selected = cmbMode.getValue();
        if (selected == null) return;
        activateRepositoryStrategy(selected);
        loadViewIntoContentArea(currentViewName); // Reload current view with new data structure
    }

    /**
     * Creates a new repository of the chosen type, loads all tasks from
     * the database into it, and updates the topbar performance label.
     *
     * @param modeLabel one of the three MODE_LABEL_* constants
     */
    private void activateRepositoryStrategy(String modeLabel) {
        // Create the right repository based on what the user selected
        TaskRepository newRepository = switch (modeLabel) {
            case MODE_LABEL_LINKEDLIST -> new LinkedListTaskRepo();
            case MODE_LABEL_ARRAYLIST  -> new ArrayListTaskRepo();
            default                    -> new HashMapTaskRepo();
        };

        // Time how long it takes to load all tasks from SQLite into the new collection
        long startTime      = System.nanoTime();
        List<Task> allTasks = DatabaseManager.getInstance().loadAll();
        newRepository.loadAll(allTasks);
        long elapsedNanoseconds = System.nanoTime() - startTime;

        // Publish the new repository so child controllers can use it
        activeRepository = newRepository;

        // Build the metric text shown in the topbar
        String metricText = String.format(
            "%s  ·  %d tasks  ·  %,d ns",
            newRepository.getStrategyName(),
            allTasks.size(),
            elapsedNanoseconds);

        System.out.println("[Benchmark] Strategy → " + metricText);
        LOGGER.info("[Strategy] Active: " + newRepository.getStrategyName());

        // runLater ensures we update the label on the JavaFX thread
        Platform.runLater(() -> lblPerfMetric.setText(metricText));
    }

    // ── Navigation handlers ───────────────────────────────────────────────────

    @FXML private void onNavDashboard()  { navigateTo("Dashboard",  "dashboard",  btnDashboard);  }
    @FXML private void onNavAllTasks()   { navigateTo("All Tasks",  "all-tasks",  btnAllTasks);   }
    @FXML private void onNavToday()      { navigateTo("Today",      "today",      btnToday);      }
    @FXML private void onNavCategories() { navigateTo("Categories", "categories", btnCategories); }
    @FXML private void onNavSettings()   { navigateTo("Settings",   "settings",   btnSettings);   }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Updates the title label, highlights the sidebar button, remembers
     * the current view name, and loads the FXML sub-view.
     *
     * @param titleText  what to show in the topbar title label
     * @param viewName   base file name of the FXML (e.g. "dashboard")
     * @param navButton  the sidebar button to highlight
     */
    private void navigateTo(String titleText, String viewName, Button navButton) {
        lblViewTitle.setText(titleText);
        currentViewName = viewName;
        setActiveNavigationButton(navButton);
        loadViewIntoContentArea(viewName);
    }

    /**
     * Loads the FXML file for the given view name into the centre StackPane.
     * If the file cannot be found, a placeholder message is shown instead
     * so the app does not crash.
     *
     * The file path follows the pattern:
     *   /com/hetero/fxml/<viewName>View.fxml
     *
     * @param viewName the base name without "View.fxml"
     */
    private void loadViewIntoContentArea(String viewName) {
        contentArea.getChildren().clear();
        String path = "/com/hetero/fxml/" + viewName + "View.fxml";

        try {
            URL resource = getClass().getResource(path);

            if (resource == null) {
                // FXML not found — show a placeholder so navigation still works
                Label placeholder = new Label("[ " + viewName + " — coming soon ]");
                placeholder.setStyle("-fx-text-fill:#4b5563;-fx-font-size:15px;");
                contentArea.getChildren().add(placeholder);
                return;
            }

            Node loadedView = new FXMLLoader(resource).load();
            contentArea.getChildren().add(loadedView);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Nav] Failed to load: " + path, e);

            // Show the error inline instead of crashing
            Label errorLabel = new Label("Error: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill:#f87171;-fx-font-size:13px;");
            contentArea.getChildren().add(errorLabel);
        }
    }

    /**
     * Removes the "active" highlight from all sidebar buttons and
     * applies it only to the one that was just clicked.
     *
     * @param activeButton the button to highlight
     */
    private void setActiveNavigationButton(Button activeButton) {
        List.of(btnDashboard, btnAllTasks, btnToday, btnCategories, btnSettings)
            .forEach(btn -> btn.getStyleClass().remove("active"));
        activeButton.getStyleClass().add("active");
    }

    /**
     * Lets child controllers update the topbar metric label after a timed operation.
     *
     * @param metricText the text to display
     */
    public void updatePerfMetric(String metricText) {
        Platform.runLater(() -> lblPerfMetric.setText(metricText));
    }
}
