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
 * TodayController shows all tasks that are due today.
 *
 * Each task is shown as a checkbox so the user can quickly
 * mark it as done without going to the All Tasks screen.
 *
 * When a checkbox is toggled, the change is immediately saved
 * to the active repository (and through it to SQLite).
 */
public class TodayController implements Initializable {

    @FXML private Label          lblDate;    // Shows today's date e.g. "Thursday, June 5"
    @FXML private Label          lblCount;   // Shows "3 tasks due today"
    @FXML private ListView<Task> listView;   // The list of tasks due today

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Format today's date in a readable way
        String todayText = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"));
        lblDate.setText(todayText);

        List<Task> tasksDueToday = MainLayoutController.getActiveRepository().findDueToday();

        // Singular "task" vs plural "tasks"
        int count = tasksDueToday.size();
        lblCount.setText(count + " task" + (count == 1 ? "" : "s") + " due today");

        listView.getItems().setAll(tasksDueToday);
        listView.setCellFactory(lv -> buildCheckBoxCell());
    }

    /**
     * Builds a list cell that renders the task as a labelled checkbox.
     * Toggling the checkbox saves the completion change immediately.
     * Completed tasks appear greyed out to signal they are done.
     *
     * @return a configured ListCell for Task objects
     */
    private ListCell<Task> buildCheckBoxCell() {
        return new ListCell<>() {

            // One CheckBox per cell — created once and reused
            private final CheckBox taskCheckBox = new CheckBox();

            {
                // Save the change every time the checkbox is clicked
                taskCheckBox.setOnAction(e -> {
                    Task currentTask = getItem();
                    if (currentTask != null) {
                        currentTask.setCompleted(taskCheckBox.isSelected());
                        MainLayoutController.getActiveRepository().update(currentTask);
                        updateStyle(currentTask.isCompleted());
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
                updateStyle(task.isCompleted());

                setGraphic(taskCheckBox);
                setStyle("-fx-background-color:transparent;-fx-padding:6 12;");
            }

            /**
             * Changes the checkbox label colour based on completion status.
             * Grey = done, normal text colour = still pending.
             *
             * @param isCompleted true if the task is completed
             */
            private void updateStyle(boolean isCompleted) {
                taskCheckBox.setStyle(isCompleted
                    ? "-fx-text-fill:#6b7280;"   // Greyed out when done
                    : "-fx-font-size:13px;");     // Normal when pending
            }
        };
    }
}
