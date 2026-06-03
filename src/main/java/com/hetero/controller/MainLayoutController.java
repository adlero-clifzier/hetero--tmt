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
 * Controller for {@code MainLayout.fxml} — the root application shell.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li><b>Strategy bootstrap:</b> Creates the initial {@link HashMapTaskRepo}
 *       on startup, hydrates it from SQLite, and stores it in the static
 *       {@code activeRepository} field.</li>
 *   <li><b>Strategy swap:</b> When the user changes the mode {@link ComboBox},
 *       instantiates the selected repository, reloads all data from SQLite,
 *       and refreshes the currently visible view.</li>
 *   <li><b>Navigation routing:</b> Each sidebar button calls {@link #navigateTo}
 *       which loads the corresponding FXML sub-view into the centre
 *       {@link StackPane} without reinitialising the outer shell.</li>
 *   <li><b>Performance display:</b> The topbar metric label is updated with
 *       the strategy name, task count, and load time after every strategy swap.</li>
 *   <li><b>Cross-controller data access:</b> The {@code static}
 *       {@link #getActiveRepository()} method is the single point of truth that
 *       all child controllers use to obtain the active {@link TaskRepository}
 *       without needing a direct reference to this controller.</li>
 * </ul>
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Inheritance / interface:</b>
 *       {@code implements Initializable} — JavaFX hook for post-injection setup.</li>
 *   <li><b>Polymorphism:</b>
 *       {@code activeRepository} is declared as the {@link TaskRepository} interface type;
 *       any of the three concrete implementations can be stored and used transparently.</li>
 *   <li><b>Java Collections:</b>
 *       {@link List} of {@link Task} from {@link DatabaseManager#loadAll};
 *       {@link FXCollections#observableArrayList} for the ComboBox items.</li>
 *   <li><b>Exception handling:</b>
 *       FXML loading inside {@link #loadViewIntoContentArea} is wrapped in
 *       {@code try/catch}; errors are displayed inline rather than crashing.</li>
 *   <li><b>Primitive data:</b>
 *       {@code long} timing delta; {@code int} task count in the metric string.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code activeRepository}, {@code currentViewName}, {@code elapsedNanoseconds},
 *       {@code performanceMetricText} — all clearly communicative.</li>
 * </ul>
 */
public class MainLayoutController implements Initializable {

    // ── Logger ────────────────────────────────────────────────────────────────

    /** Logger for navigation and strategy-swap events. */
    private static final Logger LOGGER = Logger.getLogger(MainLayoutController.class.getName());

    // ── Mode label constants ──────────────────────────────────────────────────

    /** ComboBox display string for the HashMap strategy. */
    private static final String MODE_LABEL_HASHMAP     = "HashMap Mode";

    /** ComboBox display string for the LinkedList strategy. */
    private static final String MODE_LABEL_LINKEDLIST  = "LinkedList Mode";

    /** ComboBox display string for the ArrayList strategy. */
    private static final String MODE_LABEL_ARRAYLIST   = "ArrayList Mode";

    // ── Shared repository — accessed by child controllers ─────────────────────

    /**
     * The currently active {@link TaskRepository} strategy.
     *
     * <p>Declared as the interface type ({@link TaskRepository}) to enable
     * runtime polymorphism — child controllers never need to know which
     * concrete implementation is currently active.
     *
     * <p>Static so that child controllers can call {@link #getActiveRepository()}
     * without holding a reference to this controller instance.
     */
    private static TaskRepository activeRepository;

    // ── Instance state ────────────────────────────────────────────────────────

    /**
     * Tracks the name of the currently loaded sub-view so that a strategy swap
     * can reload the same view with fresh data from the new repository.
     */
    private String currentViewName = "dashboard";

    // ── FXML-injected controls ────────────────────────────────────────────────

    /** Label showing the title of the currently active view (e.g. "All Tasks"). */
    @FXML private Label            lblViewTitle;

    /** Label showing the current strategy name, task count, and load time. */
    @FXML private Label            lblPerfMetric;

    /** Label showing the signed-in user's display name in the sidebar. */
    @FXML private Label            lblUserName;

    /** ComboBox for selecting the active data structure strategy at runtime. */
    @FXML private ComboBox<String> cmbMode;

    /** The centre container into which sub-view FXML nodes are loaded. */
    @FXML private StackPane        contentArea;

    // Sidebar navigation buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnAllTasks;
    @FXML private Button btnToday;
    @FXML private Button btnCategories;
    @FXML private Button btnSettings;

    // ── Initializable ─────────────────────────────────────────────────────────

    /**
     * Called by JavaFX after all {@code @FXML} fields have been injected.
     *
     * <p>Populates the mode ComboBox, activates the default HashMap strategy,
     * displays the signed-in user's name, marks Dashboard as the active nav item,
     * and loads the Dashboard sub-view.
     *
     * @param location  the URL of the FXML file (unused)
     * @param resources the resource bundle (unused)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Show the authenticated user's display name in the sidebar
        if (SessionManager.getUser() != null) {
            lblUserName.setText(SessionManager.getUser().getDisplayName());
        }

        // Populate and default the strategy selector ComboBox
        cmbMode.setItems(FXCollections.observableArrayList(
            MODE_LABEL_HASHMAP, MODE_LABEL_LINKEDLIST, MODE_LABEL_ARRAYLIST));
        cmbMode.setValue(MODE_LABEL_HASHMAP);

        // Bootstrap the default HashMap strategy and load the initial view
        activateRepositoryStrategy(MODE_LABEL_HASHMAP);
        setActiveNavigationButton(btnDashboard);
    }

    // ── Public static accessor ────────────────────────────────────────────────

    /**
     * Returns the currently active {@link TaskRepository} strategy.
     *
     * <p>The single point of truth for child controllers — they call this
     * method to get the repository without being coupled to this class by
     * a direct field reference.
     *
     * @return the active repository; never {@code null} after initialisation
     */
    public static TaskRepository getActiveRepository() {
        return activeRepository;
    }

    // ── Strategy management ───────────────────────────────────────────────────

    /**
     * Handles mode ComboBox selection changes.
     *
     * <p>Activates the selected repository strategy and reloads the current view
     * so that it immediately reflects data from the new collection.
     */
    @FXML
    private void onModeChanged() {
        String selectedMode = cmbMode.getValue();
        if (selectedMode == null) {
            return;
        }
        activateRepositoryStrategy(selectedMode);
        loadViewIntoContentArea(currentViewName); // Refresh with new strategy data
    }

    /**
     * Instantiates the repository matching the given mode label, loads all tasks
     * from SQLite into its in-memory collection, and updates the topbar metric label.
     *
     * <p>The total time measured includes both the {@link DatabaseManager#loadAll}
     * query and the repository's {@link TaskRepository#loadAll} hydration, giving
     * a realistic end-to-end load benchmark.
     *
     * @param modeLabel one of the {@code MODE_LABEL_*} constants
     */
    private void activateRepositoryStrategy(String modeLabel) {
        // Instantiate the correct repository implementation using a switch expression
        TaskRepository newRepository = switch (modeLabel) {
            case MODE_LABEL_LINKEDLIST -> new LinkedListTaskRepo();
            case MODE_LABEL_ARRAYLIST  -> new ArrayListTaskRepo();
            default                    -> new HashMapTaskRepo(); // Default: HashMap
        };

        // Time the full load: SQLite query + in-memory hydration
        long startTime     = System.nanoTime();
        List<Task> allTasks = DatabaseManager.getInstance().loadAll();
        newRepository.loadAll(allTasks);
        long elapsedNanoseconds = System.nanoTime() - startTime;

        // Publish the new repository for child controllers
        activeRepository = newRepository;

        // Build the performance metric string shown in the topbar
        String performanceMetricText = String.format(
            "%s  ·  %d tasks  ·  %,d ns",
            newRepository.getStrategyName(),
            allTasks.size(),
            elapsedNanoseconds);

        System.out.println("[Benchmark] Strategy swap → " + performanceMetricText);
        LOGGER.info("[Strategy] Active repository: " + newRepository.getStrategyName());

        // Update the UI label on the JavaFX Application Thread
        Platform.runLater(() -> lblPerfMetric.setText(performanceMetricText));
    }

    // ── Navigation handlers ───────────────────────────────────────────────────

    /** Navigates to the Dashboard view. */
    @FXML private void onNavDashboard() {
        navigateTo("Dashboard", "dashboard", btnDashboard);
    }

    /** Navigates to the All Tasks view. */
    @FXML private void onNavAllTasks() {
        navigateTo("All Tasks", "all-tasks", btnAllTasks);
    }

    /** Navigates to the Today view (tasks due today). */
    @FXML private void onNavToday() {
        navigateTo("Today", "today", btnToday);
    }

    /** Navigates to the Categories view. */
    @FXML private void onNavCategories() {
        navigateTo("Categories", "categories", btnCategories);
    }

    /** Navigates to the Settings view. */
    @FXML private void onNavSettings() {
        navigateTo("Settings", "settings", btnSettings);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Performs a full navigation action: updates the view title label,
     * highlights the active navigation button, records the current view name,
     * and loads the FXML sub-view into the centre {@link StackPane}.
     *
     * @param viewTitleText  the text to display in the topbar view-title label
     * @param viewFileName   the base name of the FXML file (e.g. "dashboard")
     * @param activeButton   the sidebar button to highlight as active
     */
    private void navigateTo(String viewTitleText, String viewFileName, Button activeButton) {
        lblViewTitle.setText(viewTitleText);
        currentViewName = viewFileName;
        setActiveNavigationButton(activeButton);
        loadViewIntoContentArea(viewFileName);
    }

    /**
     * Loads the named FXML sub-view into the centre {@link StackPane}.
     *
     * <p>The FXML resource is resolved from the classpath at
     * {@code /com/hetero/fxml/<viewFileName>View.fxml}.  If the resource does
     * not exist a "coming soon" placeholder is shown.  Load errors are caught,
     * logged, and shown as an inline error message rather than crashing the app.
     *
     * @param viewFileName the base file name without {@code View.fxml} suffix
     */
    private void loadViewIntoContentArea(String viewFileName) {
        contentArea.getChildren().clear();
        String fxmlResourcePath = "/com/hetero/fxml/" + viewFileName + "View.fxml";

        try {
            URL fxmlResourceUrl = getClass().getResource(fxmlResourcePath);

            if (fxmlResourceUrl == null) {
                // FXML not yet implemented — show a friendly placeholder
                Label placeholderLabel = new Label("[ " + viewFileName + " — coming soon ]");
                placeholderLabel.setStyle("-fx-text-fill:#4b5563;-fx-font-size:15px;");
                contentArea.getChildren().add(placeholderLabel);
                return;
            }

            Node loadedView = new FXMLLoader(fxmlResourceUrl).load();
            contentArea.getChildren().add(loadedView);

        } catch (Exception loadException) {
            LOGGER.log(Level.SEVERE,
                "[Nav] Failed to load view: " + fxmlResourcePath, loadException);

            Label errorLabel = new Label("Error loading view: " + loadException.getMessage());
            errorLabel.setStyle("-fx-text-fill:#f87171;-fx-font-size:13px;");
            contentArea.getChildren().add(errorLabel);
        }
    }

    /**
     * Removes the CSS {@code "active"} pseudo-class from all navigation buttons
     * and applies it exclusively to the selected button, giving it the accent
     * left-border highlight defined in the theme stylesheet.
     *
     * @param activeButton the button that should receive the active highlight
     */
    private void setActiveNavigationButton(Button activeButton) {
        List.of(btnDashboard, btnAllTasks, btnToday, btnCategories, btnSettings)
            .forEach(button -> button.getStyleClass().remove("active"));
        activeButton.getStyleClass().add("active");
    }

    /**
     * Updates the topbar performance metric label from an external caller.
     *
     * <p>Intended for child controllers that perform a timed CRUD operation and
     * want to surface the benchmark result in the topbar without navigating away.
     *
     * @param metricText the formatted metric string to display
     */
    public void updatePerfMetric(String metricText) {
        Platform.runLater(() -> lblPerfMetric.setText(metricText));
    }
}
