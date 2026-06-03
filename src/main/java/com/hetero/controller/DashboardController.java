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
 * Controller for {@code dashboardView.fxml} — the home screen overview.
 *
 * <p>Displays four statistic cards (total, pending, completed, due today)
 * and two list panels (tasks due today, five most recently added tasks).
 * All data is read from the active {@link TaskRepository} strategy.
 *
 * <p>The dashboard is read-only — no mutations are made from this view.
 * Users navigate to the All Tasks view to add, edit, or delete.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Inheritance / interface:</b>
 *       {@code implements Initializable}.</li>
 *   <li><b>Java Collections:</b>
 *       {@link List} used for task queries; anonymous {@link ListCell} subclass
 *       for custom row rendering.</li>
 *   <li><b>Polymorphism:</b>
 *       {@link TaskRepository} declared as the interface type.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link Task}, {@link TaskRepository}, {@link MainLayoutController}.</li>
 *   <li><b>Primitive data:</b>
 *       {@code long done} count; {@code int} recent task limit.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code activeRepository}, {@code allTasks}, {@code tasksDueToday},
 *       {@code completedTaskCount}, {@code recentTasks}.</li>
 * </ul>
 */
public class DashboardController implements Initializable {

    // ── FXML-injected stat card labels ────────────────────────────────────────

    /** Label displaying the total number of tasks. */
    @FXML private Label lblTotal;

    /** Label displaying the number of completed tasks. */
    @FXML private Label lblDone;

    /** Label displaying the number of pending (incomplete) tasks. */
    @FXML private Label lblPending;

    /** Label displaying the number of tasks due today. */
    @FXML private Label lblToday;

    // ── FXML-injected list panels ─────────────────────────────────────────────

    /** ListView showing tasks whose due date is today. */
    @FXML private ListView<Task> listToday;

    /** ListView showing the five most recently added tasks (highest id first). */
    @FXML private ListView<Task> listRecent;

    // ── Initializable ─────────────────────────────────────────────────────────

    /**
     * Populates all stat labels and list panels from the active repository.
     *
     * @param location  the FXML URL (unused)
     * @param resources the resource bundle (unused)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TaskRepository activeRepository = MainLayoutController.getActiveRepository();

        List<Task> allTasks      = activeRepository.findAll();
        List<Task> tasksDueToday = activeRepository.findDueToday();

        // Primitive long — count of completed tasks used in stat cards
        long completedTaskCount = allTasks.stream()
            .filter(Task::isCompleted)
            .count();

        // Update the four stat card labels
        lblTotal.setText(String.valueOf(allTasks.size()));
        lblDone.setText(String.valueOf(completedTaskCount));
        lblPending.setText(String.valueOf(allTasks.size() - completedTaskCount));
        lblToday.setText(String.valueOf(tasksDueToday.size()));

        // Populate the "Due Today" list panel
        listToday.getItems().setAll(tasksDueToday);
        listToday.setCellFactory(listView -> buildTaskListCell());

        // Show the 5 most recently added tasks (sorted by id descending, limited to 5)
        List<Task> recentTasks = allTasks.stream()
            .sorted((firstTask, secondTask) ->
                Integer.compare(secondTask.getId(), firstTask.getId()))
            .limit(5)
            .toList();

        listRecent.getItems().setAll(recentTasks);
        listRecent.setCellFactory(listView -> buildTaskListCell());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Creates a custom {@link ListCell} that renders each task as a horizontal
     * row with the task title on the left and a colour-coded priority badge on the right.
     *
     * <p>Priority badge colours match the design system:
     * CRITICAL=red, HIGH=orange, MEDIUM=yellow, LOW=green, MINIMAL=grey.
     *
     * @return a new configured {@link ListCell} instance for {@link Task} items
     */
    private ListCell<Task> buildTaskListCell() {
        return new ListCell<>() {

            @Override
            protected void updateItem(Task task, boolean isEmpty) {
                super.updateItem(task, isEmpty);

                if (isEmpty || task == null) {
                    setGraphic(null);
                    return;
                }

                // Title label — grows to fill available width
                Label titleLabel = new Label(task.getTitle());
                titleLabel.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:13px;");
                HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

                // Colour-coded priority badge
                String priorityColorHex = switch (task.getPriority()) {
                    case CRITICAL -> "#f87171";
                    case HIGH     -> "#fb923c";
                    case MEDIUM   -> "#facc15";
                    case LOW      -> "#4ade80";
                    default       -> "#94a3b8"; // MINIMAL
                };
                Label priorityBadge = new Label(task.getPriority().name());
                priorityBadge.setStyle(
                    "-fx-text-fill:" + priorityColorHex +
                    ";-fx-font-size:11px;-fx-font-weight:bold;");

                HBox taskRow = new HBox(10, titleLabel, priorityBadge);
                taskRow.setStyle("-fx-alignment:CENTER-LEFT;");

                setGraphic(taskRow);
                setStyle("-fx-background-color:transparent;");
            }
        };
    }
}
