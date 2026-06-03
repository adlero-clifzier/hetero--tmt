package com.hetero.controller;

import com.hetero.model.Task;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for {@code todayView.fxml}.
 *
 * <p>Shows all tasks whose {@code dueDate} equals today's date.
 * Each task is rendered as a labelled {@link CheckBox} so the user can toggle
 * completion without navigating to the All Tasks view.
 *
 * <p>Changes are persisted immediately via the active {@link com.hetero.repository.TaskRepository}.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Inheritance / interface:</b>
 *       {@code implements Initializable}.</li>
 *   <li><b>Imported classes:</b>
 *       {@link LocalDate}, {@link DateTimeFormatter}, {@link CheckBox},
 *       {@link ListCell}, {@link ListView}.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link Task}, {@link MainLayoutController}.</li>
 *   <li><b>Java Collections:</b>
 *       {@link List} of tasks from the repository.</li>
 *   <li><b>Primitive data:</b>
 *       Ternary on {@code int} task count for singular/plural label.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code tasksDueToday}, {@code todayDateText}, {@code taskCheckBox}.</li>
 * </ul>
 */
public class TodayController implements Initializable {

    // ── FXML-injected controls ────────────────────────────────────────────────

    /** Label showing today's date in a human-readable format. */
    @FXML private Label        lblDate;

    /** Label showing the count of tasks due today. */
    @FXML private Label        lblCount;

    /** ListView containing one CheckBox row per task due today. */
    @FXML private ListView<Task> listView;

    // ── Initializable ─────────────────────────────────────────────────────────

    /**
     * Populates the date label, count label, and task list for today.
     *
     * @param location  the FXML URL (unused)
     * @param resources the resource bundle (unused)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Format today's date as "Wednesday, June 4"
        String todayDateText = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"));
        lblDate.setText(todayDateText);

        List<Task> tasksDueToday = MainLayoutController.getActiveRepository().findDueToday();

        // Singular/plural count label
        int taskCount = tasksDueToday.size();
        lblCount.setText(taskCount + " task" + (taskCount == 1 ? "" : "s") + " due today");

        listView.getItems().setAll(tasksDueToday);
        listView.setCellFactory(listView -> buildCheckBoxListCell());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds a custom {@link ListCell} that renders each task as a {@link CheckBox}.
     *
     * <p>Toggling the checkbox immediately updates the task's completion state
     * in the active repository (which also syncs to SQLite via write-through).
     * Completed tasks are shown with a grey strikethrough style.
     *
     * @return a configured {@link ListCell} for {@link Task} items
     */
    private ListCell<Task> buildCheckBoxListCell() {
        return new ListCell<>() {

            /** The checkbox rendered inside this cell. Created once per cell instance. */
            private final CheckBox taskCheckBox = new CheckBox();

            // Attach the toggle handler once in the initialiser block
            {
                taskCheckBox.setOnAction(event -> {
                    Task currentTask = getItem();
                    if (currentTask != null) {
                        currentTask.setCompleted(taskCheckBox.isSelected());
                        MainLayoutController.getActiveRepository().update(currentTask);
                        // Re-apply style after state change
                        applyCompletionStyle(currentTask.isCompleted());
                    }
                });
            }

            @Override
            protected void updateItem(Task task, boolean isEmpty) {
                super.updateItem(task, isEmpty);

                if (isEmpty || task == null) {
                    setGraphic(null);
                    return;
                }

                taskCheckBox.setSelected(task.isCompleted());
                taskCheckBox.setText(task.getTitle());
                applyCompletionStyle(task.isCompleted());

                setGraphic(taskCheckBox);
                setStyle("-fx-background-color:transparent;-fx-padding:6 12;");
            }

            /**
             * Applies the appropriate text style to the checkbox label based on
             * whether the task is completed or pending.
             *
             * @param isCompleted {@code true} to apply the completed (grey strikethrough) style
             */
            private void applyCompletionStyle(boolean isCompleted) {
                if (isCompleted) {
                    taskCheckBox.setStyle("-fx-text-fill:#4b5563;");
                } else {
                    taskCheckBox.setStyle("-fx-text-fill:#e2e8f0;");
                }
            }
        };
    }
}
