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

/** Dashboard — stat cards + today's tasks + recent activity. */
public class DashboardController implements Initializable {

    @FXML private Label lblTotal, lblDone, lblPending, lblToday;
    @FXML private ListView<Task> listToday, listRecent;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TaskRepository repo = MainLayoutController.getActiveRepo();
        List<Task> all   = repo.findAll();
        List<Task> today = repo.findDueToday();
        long done        = all.stream().filter(Task::isCompleted).count();

        lblTotal.setText(String.valueOf(all.size()));
        lblDone.setText(String.valueOf(done));
        lblPending.setText(String.valueOf(all.size() - done));
        lblToday.setText(String.valueOf(today.size()));

        listToday.getItems().setAll(today);
        listToday.setCellFactory(lv -> taskCell());

        List<Task> recent = all.stream()
            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
            .limit(5).toList();
        listRecent.getItems().setAll(recent);
        listRecent.setCellFactory(lv -> taskCell());
    }

    private ListCell<Task> taskCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Task t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setGraphic(null); return; }

                Label title = new Label(t.getTitle());
                title.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:13px;");
                HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);

                String color = switch (t.getPriority()) {
                    case CRITICAL -> "#f87171";
                    case HIGH     -> "#fb923c";
                    case MEDIUM   -> "#facc15";
                    case LOW      -> "#4ade80";
                    default       -> "#94a3b8";
                };
                Label badge = new Label(t.getPriority().name());
                badge.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;-fx-font-weight:bold;");

                HBox row = new HBox(10, title, badge);
                row.setStyle("-fx-alignment:CENTER-LEFT;");
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
            }
        };
    }
}
