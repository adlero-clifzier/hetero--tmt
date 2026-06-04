package com.hetero.controller;

import com.hetero.model.Task;
import com.hetero.repository.TaskRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * DashboardController fills in the dashboard screen.
 *
 * The dashboard shows four stat cards at the top:
 *   - Total tasks
 *   - Pending tasks
 *   - Completed tasks
 *   - Tasks due today
 *
 * Below the cards there are two lists:
 *   - Tasks due today
 *   - The 5 most recently added tasks
 *
 * This screen is read-only — users go to All Tasks to make changes.
 * All data comes from the active TaskRepository strategy.
 */
public class DashboardController implements Initializable {

    @FXML private Label         lblTotal;    // Total task count
    @FXML private Label         lblDone;     // Completed task count
    @FXML private Label         lblPending;  // Pending task count
    @FXML private Label         lblToday;    // Tasks due today count

    @FXML private ListView<Task> listToday;  // List of tasks due today
    @FXML private ListView<Task> listRecent; // List of 5 most recent tasks

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TaskRepository repo = MainLayoutController.getActiveRepository();

        List<Task> allTasks      = repo.findAll();
        List<Task> tasksDueToday = repo.findDueToday();

        // Count completed tasks using a stream — result is a primitive long
        long completedCount = allTasks.stream().filter(Task::isCompleted).count();

        // Fill in the four stat card labels
        lblTotal.setText(String.valueOf(allTasks.size()));
        lblDone.setText(String.valueOf(completedCount));
        lblPending.setText(String.valueOf(allTasks.size() - completedCount));
        lblToday.setText(String.valueOf(tasksDueToday.size()));

        // Fill the "Due Today" list
        listToday.getItems().setAll(tasksDueToday);
        listToday.setCellFactory(lv -> buildTaskCell());

        // Sort by id descending and take the top 5 for "Recent Tasks"
        List<Task> recentTasks = allTasks.stream()
            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
            .limit(5)
            .toList();

        listRecent.getItems().setAll(recentTasks);
        listRecent.setCellFactory(lv -> buildTaskCell());
    }

    /**
     * Creates a list cell that shows the task title on the left
     * and a colour-coded priority badge on the right.
     *
     * BUG FIX: Previously used hardcoded dark-mode hex colours.
     * Now uses CSS style classes so cells look correct in both
     * dark mode and light mode.
     *
     * @return a configured ListCell for Task objects
     */
    private ListCell<Task> buildTaskCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean isEmpty) {
                super.updateItem(task, isEmpty);

                if (isEmpty || task == null) {
                    setGraphic(null);
                    return;
                }

                // Title label grows to fill the available width
                Label titleLabel = new Label(task.getTitle());
                titleLabel.setStyle("-fx-font-size:13px;");
                HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

                // Priority badge — CSS class controls the colour so it
                // works correctly in both dark and light mode
                Label priorityBadge = new Label(task.getPriority().name());
                String badgeClass = switch (task.getPriority()) {
                    case CRITICAL -> "badge-critical";
                    case HIGH     -> "badge-high";
                    case MEDIUM   -> "badge-medium";
                    case LOW      -> "badge-low";
                    default       -> "badge-minimal";
                };
                priorityBadge.getStyleClass().add(badgeClass);
                priorityBadge.setStyle("-fx-font-size:11px;-fx-font-weight:bold;");

                HBox row = new HBox(10, titleLabel, priorityBadge);
                row.setStyle("-fx-alignment:CENTER-LEFT;");
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
            }
        };
    }
}
